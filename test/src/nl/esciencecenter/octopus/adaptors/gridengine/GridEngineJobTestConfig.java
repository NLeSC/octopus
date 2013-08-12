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

package nl.esciencecenter.octopus.adaptors.gridengine;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import nl.esciencecenter.octopus.adaptors.JobTestConfig;
import nl.esciencecenter.octopus.credentials.Credential;
import nl.esciencecenter.octopus.credentials.Credentials;
import nl.esciencecenter.octopus.files.FileSystem;
import nl.esciencecenter.octopus.files.Files;
import nl.esciencecenter.octopus.jobs.Jobs;
import nl.esciencecenter.octopus.jobs.Scheduler;

/**
 * @author Jason Maassen <J.Maassen@esciencecenter.nl>
 * @author Niels Drost <N.Drost@esciencecenter.nl>
 * 
 */
public class GridEngineJobTestConfig extends JobTestConfig {

    private String username;
    private char[] passwd;

    private URI correctURI;
    private URI correctURIWithPath;
    private URI correctFSURI;

    private URI wrongUserURI;
    private URI wrongLocationURI;
    private URI wrongPathURI;

    private String defaultQueue;
    private String[] queues;

    private long queueWaitTime;
    private long updateTime;

    private String parallelEnvironment;

    public GridEngineJobTestConfig(String configfile) throws Exception {

        super("gridengine");

        if (configfile == null) {
            configfile = System.getProperty("test.config");
        }

        if (configfile == null) {
            configfile = System.getProperty("user.home") + File.separator + "octopus.test.properties";
        }

        Properties p = new Properties();
        p.load(new FileInputStream(configfile));

        username = getPropertyOrFail(p, "test.gridengine.user");
        passwd = getPropertyOrFail(p, "test.gridengine.password").toCharArray();

        String location = getPropertyOrFail(p, "test.gridengine.location");

        String wrongUser = getPropertyOrFail(p, "test.gridengine.user.wrong");
        String wrongLocation = getPropertyOrFail(p, "test.gridengine.location.wrong");

        defaultQueue = getPropertyOrFail(p, "test.gridengine.default.queue");
        String queueList = getPropertyOrFail(p, "test.gridengine.queues");
        queues = queueList.split("\\s*,\\s*");

        queueWaitTime = Long.parseLong(getPropertyOrFail(p, "test.gridengine.queue.wait.time"));
        updateTime = Long.parseLong(getPropertyOrFail(p, "test.gridengine.update.time"));

        parallelEnvironment = p.getProperty("test.gridengine.parallel.environment");

        correctURI = new URI("ge://" + username + "@" + location);
        correctFSURI = new URI("sftp://" + username + "@" + location);
        correctURIWithPath = new URI("ge://" + username + "@" + location + "/");
        wrongUserURI = new URI("ge://" + wrongUser + "@" + location);
        wrongLocationURI = new URI("ge://" + username + "@" + wrongLocation);
        wrongPathURI = new URI("ge://" + username + "@" + location + "/aap/noot");
    }

    private String getPropertyOrFail(Properties p, String property) throws Exception {

        String tmp = p.getProperty(property);

        if (tmp == null) {
            throw new Exception("Failed to retrieve property " + property);
        }

        return tmp;
    }

    @Override
    public URI getCorrectURI() throws Exception {
        return correctURI;
    }

    @Override
    public URI getCorrectURIWithPath() throws Exception {
        return correctURIWithPath;
    }

    @Override
    public boolean supportURILocation() {
        return true;
    }

    @Override
    public URI getURIWrongLocation() throws Exception {
        return wrongLocationURI;
    }

    @Override
    public URI getURIWrongPath() throws Exception {
        return wrongPathURI;
    }

    @Override
    public boolean supportURIUser() {
        return true;
    }

    @Override
    public URI getURIWrongUser() throws Exception {
        return wrongUserURI;
    }

    @Override
    public boolean supportsCredentials() {
        return true;
    }

    @Override
    public Credential getDefaultCredential(Credentials credentials) throws Exception {
        return credentials.getDefaultCredential("ge");
    }

    @Override
    public Credential getPasswordCredential(Credentials credentials) throws Exception {
        return credentials.newPasswordCredential("ge", username, passwd, new HashMap<String, String>());
    }

    @Override
    public Credential getInvalidCredential(Credentials credentials) throws Exception {
        return credentials.newPasswordCredential("ge", username, "wrongpassword".toCharArray(), new HashMap<String, String>());
    }

    @Override
    public boolean supportNonDefaultCredential() {
        return false;
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

    public String getDefaultQueueName() {
        return defaultQueue;
    }

    public String[] getQueueNames() {
        return queues;
    }

    @Override
    public Scheduler getDefaultScheduler(Jobs jobs, Credentials credentials) throws Exception {
        return jobs.newScheduler(correctURI, getDefaultCredential(credentials), getDefaultProperties());
    }

    @Override
    public FileSystem getDefaultFileSystem(Files files, Credentials credentials) throws Exception {
        return files.newFileSystem(correctFSURI, getDefaultCredential(credentials), null);
    }

    @Override
    public String getInvalidQueueName() throws Exception {
        return "aap";
    }

    public boolean supportsProperties() throws Exception {
        return true;
    }

    @Override
    public Map<String, String> getDefaultProperties() throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        result.put("octopus.adaptors.gridengine.poll.delay", "100");
        return result;
    }

    public Map<String, String> getUnknownProperties() throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        result.put("octopus.adaptors.gridengine.unknown.property", "some.value");
        return result;
    }

    public Map<String, String>[] getInvalidProperties() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, String>[] result = new Map[1];

        result[0] = new HashMap<String, String>();

        result[0].put("octopus.adaptors.gridengine.poll.delay", "AAP");
        return result;
    }

    public Map<String, String> getCorrectProperties() throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        result.put("octopus.adaptors.gridengine.poll.delay", "100");
        return result;
    }

    @Override
    public boolean supportsStatusAfterDone() {
        return true;
    }

    @Override
    public long getQueueWaitTime() {
        return queueWaitTime;
    }

    @Override
    public long getUpdateTime() {
        return updateTime;
    }

    @Override
    public boolean supportsParallelJobs() {
        return true;
    }

    public String getParallelEnvironment() {
        return parallelEnvironment;
    }
}
