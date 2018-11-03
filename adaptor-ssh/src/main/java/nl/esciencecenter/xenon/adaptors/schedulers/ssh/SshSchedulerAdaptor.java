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
package nl.esciencecenter.xenon.adaptors.schedulers.ssh;

import java.util.Map;

import org.apache.sshd.client.SshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.XenonPropertyDescription;
import nl.esciencecenter.xenon.XenonPropertyDescription.Type;
import nl.esciencecenter.xenon.adaptors.XenonProperties;
import nl.esciencecenter.xenon.adaptors.filesystems.sftp.SftpFileAdaptor;
import nl.esciencecenter.xenon.adaptors.schedulers.JobQueueScheduler;
import nl.esciencecenter.xenon.adaptors.schedulers.SchedulerAdaptor;
import nl.esciencecenter.xenon.adaptors.shared.ssh.SSHConnection;
import nl.esciencecenter.xenon.adaptors.shared.ssh.SSHUtil;
import nl.esciencecenter.xenon.credentials.Credential;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.schedulers.Scheduler;

public class SshSchedulerAdaptor extends SchedulerAdaptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshSchedulerAdaptor.class);

    /** The name of this adaptor */
    public static final String ADAPTOR_NAME = "ssh";

    /** The default SSH port */
    public static final int DEFAULT_PORT = 22;

    /** A description of this adaptor */
    public static final String ADAPTOR_DESCRIPTION = "The SSH job adaptor implements all functionality to start jobs on ssh servers.";

    /** All our own properties start with this prefix. */
    public static final String PREFIX = SchedulerAdaptor.ADAPTORS_PREFIX + "ssh.";

    /** Enable strict host key checking. */
    public static final String STRICT_HOST_KEY_CHECKING = PREFIX + "strictHostKeyChecking";

    /** Enable the use of an ssh-agent */
    public static final String AGENT = PREFIX + "agent";

    /** Enable the use of ssh-agent-forwarding */
    public static final String AGENT_FORWARDING = PREFIX + "agentForwarding";

    /** Load the known_hosts file by default. */
    public static final String LOAD_STANDARD_KNOWN_HOSTS = PREFIX + "loadKnownHosts";

    /** Load the OpenSSH config file by default. */
    public static final String LOAD_SSH_CONFIG = PREFIX + "loadSshConfig";

    /** OpenSSH config filename. */
    public static final String SSH_CONFIG_FILE = PREFIX + "sshConfigFile";

    /** Enable strict host key checking. */
    public static final String AUTOMATICALLY_ADD_HOST_KEY = PREFIX + "autoAddHostKey";

    /** Add gateway to access machine. */
    public static final String GATEWAY = PREFIX + "gateway";

    /** Add gateway to access machine. */
    public static final String TIMEOUT = PREFIX + "timeout";

    /** All our own queue properties start with this prefix. */
    public static final String QUEUE = PREFIX + "queue.";

    /** Maximum history length for finished jobs */
    public static final String MAX_HISTORY = QUEUE + "historySize";

    /** Property for maximum history length for finished jobs */
    public static final String POLLING_DELAY = QUEUE + "pollingDelay";

    /** Local multi queue properties start with this prefix. */
    public static final String MULTIQ = QUEUE + "multi.";

    /**
     * Property for the maximum number of concurrent jobs in the multi queue.
     */
    public static final String MULTIQ_MAX_CONCURRENT = MULTIQ + "maxConcurrentJobs";

    /** Ssh adaptor information start with this prefix. */
    public static final String INFO = PREFIX + "info.";

    /** Ssh job information start with this prefix. */
    public static final String JOBS = INFO + "jobs.";

    /** How many jobs have been submitted using this adaptor. */
    public static final String SUBMITTED = JOBS + "submitted";

    /** The locations supported by this adaptor */
    private static final String[] ADAPTOR_LOCATIONS = new String[] { "host[:port][/workdir][ via:otherhost[:port]]*" };

    /** List of properties supported by this SSH adaptor */
    private static final XenonPropertyDescription[] VALID_PROPERTIES = new XenonPropertyDescription[] {
            new XenonPropertyDescription(STRICT_HOST_KEY_CHECKING, Type.BOOLEAN, "true", "Enable strict host key checking."),
            new XenonPropertyDescription(LOAD_STANDARD_KNOWN_HOSTS, Type.BOOLEAN, "true", "Load the standard known_hosts file."),
            new XenonPropertyDescription(LOAD_SSH_CONFIG, Type.BOOLEAN, "true", "Load the OpenSSH config file."),
            /* new XenonPropertyDescription(SSH_CONFIG_FILE, Type.STRING, null, "OpenSSH config filename."), */
            new XenonPropertyDescription(AGENT, Type.BOOLEAN, "false", "Use a (local) ssh-agent."),
            new XenonPropertyDescription(AGENT_FORWARDING, Type.BOOLEAN, "false", "Use ssh-agent forwarding"),
            new XenonPropertyDescription(TIMEOUT, Type.LONG, "10000", "The timeout for the connection setup and authetication (in milliseconds)."),
            new XenonPropertyDescription(POLLING_DELAY, Type.LONG, "1000", "The polling delay for monitoring running jobs (in milliseconds)."),
            new XenonPropertyDescription(MULTIQ_MAX_CONCURRENT, Type.INTEGER, "4", "The maximum number of concurrent jobs in the multiq..") };

    public SshSchedulerAdaptor() {
        super(ADAPTOR_NAME, ADAPTOR_DESCRIPTION, ADAPTOR_LOCATIONS, VALID_PROPERTIES);
    }

    @Override
    public boolean isEmbedded() {
        // The SSH scheduler is embedded
        return true;
    }

    @Override
    public boolean supportsInteractive() {
        // The SSH scheduler supports interactive jobs
        return true;
    }

    @Override
    public Scheduler createScheduler(String location, Credential credential, Map<String, String> properties) throws XenonException {

        LOGGER.debug("new SSH scheduler location = {} credential = {} properties = {}", location, credential, properties);

        XenonProperties xp = new XenonProperties(VALID_PROPERTIES, properties);

        boolean loadKnownHosts = xp.getBooleanProperty(LOAD_STANDARD_KNOWN_HOSTS);
        boolean loadSSHConfig = xp.getBooleanProperty(LOAD_SSH_CONFIG);
        boolean strictHostCheck = xp.getBooleanProperty(STRICT_HOST_KEY_CHECKING);
        boolean useSSHAgent = xp.getBooleanProperty(AGENT);
        boolean useAgentForwarding = xp.getBooleanProperty(AGENT_FORWARDING);

        SshClient client = SSHUtil.createSSHClient(loadKnownHosts, loadSSHConfig, strictHostCheck, useSSHAgent, useAgentForwarding);

        long timeout = xp.getLongProperty(TIMEOUT);

        SSHConnection connection = SSHUtil.connect(ADAPTOR_NAME, client, location, credential, 0, timeout);

        // We must convert the relevant SSH properties to SFTP here.
        Map<String, String> sftpProperties = SSHUtil.translateProperties(properties, SshSchedulerAdaptor.PREFIX,
                FileSystem.getAdaptorDescription("sftp").getSupportedProperties(), SftpFileAdaptor.PREFIX);

        // Create a file system that point to the same location as the
        // scheduler.
        FileSystem fs = FileSystem.create("sftp", location, credential, sftpProperties);

        long pollingDelay = xp.getLongProperty(POLLING_DELAY);
        int multiQThreads = xp.getIntegerProperty(MULTIQ_MAX_CONCURRENT);

        return new JobQueueScheduler(getNewUniqueID(), ADAPTOR_NAME, location, credential, new SshInteractiveProcessFactory(connection), fs,
                fs.getWorkingDirectory(), multiQThreads, pollingDelay, timeout, xp);
    }
}
