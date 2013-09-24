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

package nl.esciencecenter.xenon.adaptors.ssh;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import nl.esciencecenter.xenon.adaptors.FileTestConfig;
import nl.esciencecenter.xenon.credentials.Credential;
import nl.esciencecenter.xenon.credentials.Credentials;
import nl.esciencecenter.xenon.files.FileSystem;
import nl.esciencecenter.xenon.files.Files;
import nl.esciencecenter.xenon.files.Path;

/**
 * @author Jason Maassen <J.Maassen@esciencecenter.nl>
 * 
 */
public class SSHFileTestConfig extends FileTestConfig {

    private String username;
    private char[] passwd;

    private String scheme = "sftp";
    private String correctLocation;
    private String wrongLocation;
    private String correctLocationWrongUser;
    
    public SSHFileTestConfig(String configfile) throws Exception {

        super("ssh", configfile);

        String location = getPropertyOrFail(p, "test.ssh.location");

        username = getPropertyOrFail(p, "test.ssh.user");
        passwd = getPropertyOrFail(p, "test.ssh.password").toCharArray();
        
        correctLocation = username + "@" + location;
        wrongLocation = username + "@doesnotexist.com";
        correctLocationWrongUser = "incorrect@" + location;
    }

    private String getPropertyOrFail(Properties p, String property) throws Exception {

        String tmp = p.getProperty(property);

        if (tmp == null) {
            throw new Exception("Failed to retireve property " + property);
        }

        return tmp;
    }

    @Override
    public boolean supportLocation() {
        return true;
    }

    @Override
    public boolean supportUser() {
        return true;
    }

    @Override
    public boolean supportsCredentials() {
        return true;
    }

    @Override
    public Credential getDefaultCredential(Credentials credentials) throws Exception {
        return credentials.getDefaultCredential("ssh");
    }

    @Override
    public Credential getPasswordCredential(Credentials credentials) throws Exception {
        return credentials.newPasswordCredential("ssh", username, passwd, new HashMap<String, String>());
    }

    @Override
    public Credential getInvalidCredential(Credentials credentials) throws Exception {
        return credentials.newPasswordCredential("ssh", username, "wrongpassword".toCharArray(), new HashMap<String, String>());
    }

    @Override
    public boolean supportNonDefaultCredential() {
        return true;
    }

    @Override
    public Credential getNonDefaultCredential(Credentials credentials) throws Exception {
        return getPasswordCredential(credentials);
    }

    @Override
    public boolean supportNullCredential() {
        return true;
    }

    @Override
    public boolean supportsClose() {
        return true;
    }

    @Override
    public Map<String, String> getDefaultProperties() throws Exception {
        return null;
    }

    @Override
    public FileSystem getTestFileSystem(Files files, Credentials credentials) throws Exception {
        return files.newFileSystem("sftp", correctLocation, getDefaultCredential(credentials), null);
    }

    @Override
    public String getScheme() throws Exception {
        return scheme;
    }

    @Override
    public String getCorrectLocation() throws Exception {
        return correctLocation;
    }

    @Override
    public String getWrongLocation() throws Exception {
        return wrongLocation;
    }

    @Override
    public String getCorrectLocationWithUser() throws Exception {
        return correctLocation;
    }

    @Override
    public String getCorrectLocationWithWrongUser() throws Exception {
        return correctLocationWrongUser;
    }

    @Override
    public boolean supportsPosixPermissions() {
        // Assumes an SSH connection to a posix machine!
        return true;
    }

    @Override
    public boolean supportsSymboliclinks() {
        // Assumes an SSH connection to a posix machine!
        return true;
    }

    @Override
    public Path getWorkingDir(Files files, Credentials credentials) throws Exception {
        return files.newFileSystem("sftp", correctLocation, getDefaultCredential(credentials), null).getEntryPath();
    }
}
