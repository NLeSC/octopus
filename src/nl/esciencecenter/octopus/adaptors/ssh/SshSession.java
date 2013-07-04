/*
 * Copyright 2013 Netherlands eScience Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.esciencecenter.octopus.adaptors.ssh;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

import nl.esciencecenter.octopus.credentials.Credential;
import nl.esciencecenter.octopus.engine.OctopusProperties;
import nl.esciencecenter.octopus.engine.credentials.CertificateCredentialImplementation;
import nl.esciencecenter.octopus.engine.credentials.CredentialImplementation;
import nl.esciencecenter.octopus.engine.credentials.PasswordCredentialImplementation;
import nl.esciencecenter.octopus.exceptions.BadParameterException;
import nl.esciencecenter.octopus.exceptions.InvalidCredentialException;
import nl.esciencecenter.octopus.exceptions.OctopusException;
import nl.esciencecenter.octopus.exceptions.OctopusIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * @author Jason Maassen <J.Maassen@esciencecenter.nl>
 * 
 */
class SshSession {

    private static final Logger logger = LoggerFactory.getLogger(SshSession.class);

    private static final int MAX_OPEN_CHANNELS = 7;

    class SessionInfo {

        final int ID;
        final Session session;
        ChannelSftp sftpChannel;
        int openChannels = 0;

        SessionInfo(Session session, int ID) {
            this.session = session;
            this.ID = ID;
        }

        boolean incOpenChannels(String info) {

            if (openChannels == MAX_OPEN_CHANNELS) {
                return false;
            }

            openChannels++;
            logger.debug("SSHSESSION-{}: ++Open channels: {} {}", ID, openChannels, info);
            return true;
        }

        void decOpenChannels(String info) {
            openChannels--;
            logger.debug("SSHSESSION-{}: --Open channels: {} {}", ID, openChannels, info);
        }

        ChannelSftp getSftpChannelFromCache() {
            ChannelSftp channel = sftpChannel;
            sftpChannel = null;
            return channel;
        }

        boolean putSftpChannelInCache(ChannelSftp channel) {
            if (sftpChannel != null) {
                return false;
            }

            sftpChannel = channel;
            return true;
        }

        void releaseExecChannel(ChannelExec channel) {
            logger.debug("SSHSESSION-{}: Releasing EXEC channel", ID);
            channel.disconnect();
            decOpenChannels("EXEC");
        }

        void failedExecChannel(ChannelExec channel) {
            logger.debug("SSHSESSION-{}: Releasing FAILED EXEC channel", ID);
            channel.disconnect();
            decOpenChannels("FAILED EXEC");
        }

        void releaseSftpChannel(ChannelSftp channel) {
            logger.debug("SSHSESSION-{}: Releasing SFTP channel", ID);

            if (!putSftpChannelInCache(channel)) {
                channel.disconnect();
                decOpenChannels("SFTP");
            }
        }

        void failedSftpChannel(ChannelSftp channel) {
            logger.debug("SSHSESSION-{}: Releasing FAILED SFTP channel", ID);
            channel.disconnect();
            decOpenChannels("FAILED SFTP");
        }

        void disconnect() {
            if (sftpChannel != null) {
                sftpChannel.disconnect();
            }

            session.disconnect();
        }

        ChannelExec getExecChannel() throws OctopusIOException {

            if (openChannels == MAX_OPEN_CHANNELS) {
                return null;
            }

            ChannelExec channel = null;

            try {
                logger.debug("SSHSESSION-{}: Creating EXEC channel {}", ID, openChannels);
                channel = (ChannelExec) session.openChannel("exec");
            } catch (JSchException e) {
                logger.debug("SSHSESSION-{}: Failed to create EXEC channel {}", ID, openChannels, e);
                throw new OctopusIOException(SshAdaptor.ADAPTOR_NAME, e.getMessage(), e);
            }

            incOpenChannels("EXEC");
            return channel;
        }

        ChannelSftp getSftpChannel() throws OctopusIOException {

            ChannelSftp channel = getSftpChannelFromCache();

            if (channel != null) {
                logger.debug("SSHSESSION-{}: Reusing SFTP channel {}", ID, openChannels);
                return channel;
            }

            if (openChannels == MAX_OPEN_CHANNELS) {
                return null;
            }

            try {
                logger.debug("SSHSESSION-{}: Creating SFTP channel {}", ID, openChannels);
                channel = (ChannelSftp) session.openChannel("sftp");
                channel.connect();
            } catch (JSchException e) {
                logger.debug("SSHSESSION-{}: Failed to create SFTP channel {}", ID, openChannels, e);
                throw new OctopusIOException(SshAdaptor.ADAPTOR_NAME, e.getMessage(), e);
            }

            incOpenChannels("SFTP");
            return channel;
        }
    }

    private final SshAdaptor adaptor;
    private final JSch jsch;

    private final URI location;
    private final OctopusProperties properties;

    private Credential credential;
    private String user;
    private String host;
    private int port;

    private int nextSessionID = 0;

    private ArrayList<SessionInfo> sessions = new ArrayList<>();

    //    private Session session;
    //    private ChannelSftp sftpChannel;
    //    private int openChannels = 0;

    static class Robot implements UserInfo {

        private final boolean yesNo;

        Robot(boolean yesyNo) {
            this.yesNo = yesyNo;
        }

        @Override
        public String getPassphrase() {
            return null;
        }

        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public boolean promptPassphrase(String message) {
            return false;
        }

        @Override
        public boolean promptPassword(String message) {
            return false;
        }

        @Override
        public boolean promptYesNo(String message) {
            return yesNo;
        }

        @Override
        public void showMessage(String message) {
            // ignored
        }
    }

    SshSession(SshAdaptor adaptor, JSch jsch, URI location, Credential credential, OctopusProperties properties)
            throws OctopusException {

        this.adaptor = adaptor;
        this.jsch = jsch;
        this.location = location;
        this.properties = properties;
        this.credential = credential;

        user = location.getUserInfo();
        host = location.getHost();
        port = location.getPort();

        if (credential == null) {
            credential = adaptor.credentialsAdaptor().getDefaultCredential("ssh");
        }

        if (credential instanceof CertificateCredentialImplementation) {
            CertificateCredentialImplementation certificate = (CertificateCredentialImplementation) credential;
            try {
                jsch.addIdentity(certificate.getKeyfile(), Arrays.toString(certificate.getPassword()));
            } catch (JSchException e) {
                throw new InvalidCredentialException(SshAdaptor.ADAPTOR_NAME, "Could not read private key file.", e);
            }
        } else if (credential instanceof PasswordCredentialImplementation) {
            // handled per session
        } else {
            throw new InvalidCredentialException(SshAdaptor.ADAPTOR_NAME, "Unknown credential type.");
        }

        if (port <= 0) {
            port = SshAdaptor.DEFAULT_PORT;
        }

        if (host == null) {
            host = "localhost";
        }

        String credentialUserName = ((CredentialImplementation) credential).getUsername();

        //        if (user != null && credentialUserName != null && !user.equals(credentialUserName)) {
        //            throw new BadParameterException(SshAdaptor.ADAPTOR_NAME,
        //                    "If a user name is given in the URI, it must match the one in the credential");
        //        }

        if (user == null) {
            user = credentialUserName;
        }

        if (user == null) {
            throw new BadParameterException(SshAdaptor.ADAPTOR_NAME, "No user name given. Specify it in URI or credential.");
        }

        createSession();
    }

    private SessionInfo storeSession(Session s) {
        SessionInfo info = new SessionInfo(s, nextSessionID++);
        sessions.add(info);
        return info;
    }

    private SessionInfo findSession(Channel c) throws OctopusIOException {
        try {
            return findSession(c.getSession());
        } catch (JSchException e) {
            throw new OctopusIOException(SshAdaptor.ADAPTOR_NAME, "Failed to retrieve Session from SSH Channel!", e);
        }
    }

    private SessionInfo findSession(Session s) throws OctopusIOException {

        for (int i = 0; i < sessions.size(); i++) {
            SessionInfo info = sessions.get(i);

            if (info != null && info.session == s) {
                return info;
            }
        }

        throw new OctopusIOException(SshAdaptor.ADAPTOR_NAME, "SSH Session not found!");
    }

    private SessionInfo createSession() throws OctopusException {

        logger.debug("SSHSESSION: Creating new session to " + user + "@" + host + ":" + port);

        Session session = null;

        try {
            session = jsch.getSession(user, host, port);
        } catch (JSchException e) {
            throw new OctopusException(SshAdaptor.ADAPTOR_NAME, "Failed to create SSH session!", e);
        }

        if (credential instanceof PasswordCredentialImplementation) {
            PasswordCredentialImplementation passwordCredential = (PasswordCredentialImplementation) credential;
            session.setPassword(new String(passwordCredential.getPassword()));
        }

        if (properties.getBooleanProperty(SshAdaptor.STRICT_HOST_KEY_CHECKING)) {
            logger.debug("SSHSESSION: Strict host key checking enabled");

            if (properties.getBooleanProperty(SshAdaptor.AUTOMATICALLY_ADD_HOST_KEY)) {
                logger.debug("SSHSESSION: Automatically add host key to known_hosts");
                session.setConfig("StrictHostKeyChecking", "ask");
                session.setUserInfo(new Robot(true));
            } else {
                session.setConfig("StrictHostKeyChecking", "yes");
            }
        } else {
            logger.debug("SSHSESSION: Strict host key checking disabled");
            session.setConfig("StrictHostKeyChecking", "no");
        }

        if (properties.getBooleanProperty(SshAdaptor.LOAD_STANDARD_KNOWN_HOSTS)) {
            String knownHosts = System.getProperty("user.home") + "/.ssh/known_hosts";
            logger.debug("SSHSESSION: Setting ssh known hosts file to: " + knownHosts);
            setKnownHostsFile(knownHosts);
        }

        try {
            session.connect();
        } catch (JSchException e) {
            throw new OctopusException(SshAdaptor.ADAPTOR_NAME, e.getMessage(), e);
        }

        return storeSession(session);
    }

    private void setKnownHostsFile(String knownHostsFile) throws OctopusException {
        try {
            jsch.setKnownHosts(knownHostsFile);
        } catch (JSchException e) {
            throw new OctopusException(SshAdaptor.ADAPTOR_NAME, "Could not set known_hosts file", e);
        }

        if (logger.isDebugEnabled()) {
            HostKeyRepository hkr = jsch.getHostKeyRepository();
            HostKey[] hks = hkr.getHostKey();
            if (hks != null) {
                logger.debug("SSHSESSION: Host keys in " + hkr.getKnownHostsRepositoryID());
                for (HostKey hk : hks) {
                    logger.debug(hk.getHost() + " " + hk.getType() + " " + hk.getFingerPrint(jsch));
                }
                logger.debug("");
            } else {
                logger.debug("SSHSESSION: No keys in " + knownHostsFile);
            }
        }
    }

    /**
     * Get a new exec channel. The channel is not connected yet, because the input and output streams should be set before
     * connecting.
     * 
     * @param session
     *            The authenticated session.
     * @return the channel
     * @throws OctopusIOException
     */
    synchronized ChannelExec getExecChannel() throws OctopusIOException {

        for (int i = 0; i < sessions.size(); i++) {
            SessionInfo s = sessions.get(i);

            ChannelExec channel = s.getExecChannel();

            if (channel != null) {
                return channel;
            }
        }

        try {
            SessionInfo s = createSession();
            return s.getExecChannel();
        } catch (OctopusException e) {
            throw new OctopusIOException(SshAdaptor.ADAPTOR_NAME, "Failed to create new SSH session!", e);
        }
    }

    synchronized void releaseExecChannel(ChannelExec channel) throws OctopusIOException {
        findSession(channel).releaseExecChannel(channel);
    }

    synchronized void failedExecChannel(ChannelExec channel) throws OctopusIOException {
        findSession(channel).failedExecChannel(channel);
    }

    /**
     * Get a connected channel for doing sftp operations.
     * 
     * @param session
     *            The authenticated session.
     * @return the channel
     * @throws OctopusIOException
     */
    synchronized ChannelSftp getSftpChannel() throws OctopusIOException {

        for (int i = 0; i < sessions.size(); i++) {
            SessionInfo s = sessions.get(i);

            ChannelSftp channel = s.getSftpChannel();

            if (channel != null) {
                return channel;
            }
        }

        try {
            SessionInfo s = createSession();
            return s.getSftpChannel();
        } catch (OctopusException e) {
            throw new OctopusIOException(SshAdaptor.ADAPTOR_NAME, "Failed to create new SSH session!", e);
        }
    }

    synchronized void releaseSftpChannel(ChannelSftp channel) throws OctopusIOException {
        findSession(channel).releaseSftpChannel(channel);
    }

    synchronized void failedSftpChannel(ChannelSftp channel) throws OctopusIOException {
        findSession(channel).failedSftpChannel(channel);
    }

    synchronized void disconnect() {

        while (sessions.size() > 0) {
            SessionInfo s = sessions.remove(0);

            if (s != null) {
                s.disconnect();
            }
        }
    }
}
