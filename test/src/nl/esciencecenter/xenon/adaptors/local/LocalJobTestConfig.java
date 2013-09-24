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

package nl.esciencecenter.xenon.adaptors.local;

import java.util.Map;

import nl.esciencecenter.xenon.adaptors.JobTestConfig;
import nl.esciencecenter.xenon.credentials.Credential;
import nl.esciencecenter.xenon.credentials.Credentials;
import nl.esciencecenter.xenon.files.Files;
import nl.esciencecenter.xenon.files.Path;
import nl.esciencecenter.xenon.jobs.Jobs;
import nl.esciencecenter.xenon.jobs.Scheduler;
import nl.esciencecenter.xenon.util.Utils;

/**
 * @author Jason Maassen <J.Maassen@esciencecenter.nl>
 * 
 */
public class LocalJobTestConfig extends JobTestConfig {

    private final String scheme;
    private final String correctLocation;
    private final String wrongLocation;
    
    public LocalJobTestConfig() throws Exception {
        super("local", null);

        scheme = "local";
        correctLocation = "/";
        wrongLocation = "/aap";
    }

    @Override
    public Scheduler getDefaultScheduler(Jobs jobs, Credentials credentials) throws Exception {
        return jobs.newScheduler("local", null, null, null);
    }

    @Override
    public Path getWorkingDir(Files files, Credentials credentials) throws Exception {
        return Utils.getLocalCWD(files);
    }

    @Override
    public String getInvalidQueueName() throws Exception {
        return "aap";
    }

    public boolean supportLocation() {
        return true;
    }

    @Override
    public Credential getDefaultCredential(Credentials c) throws Exception {
        return null;
    }

    @Override
    public Map<String, String> getDefaultProperties() throws Exception {
        return null;
    }

    @Override
    public boolean supportsStatusAfterDone() {
        return false;
    }

    @Override
    public long getQueueWaitTime() {
        return 1000;
    }

    @Override
    public long getUpdateTime() {
        return 3000;
    }

    @Override
    public boolean supportsParallelJobs() {
        return false;
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
        return null;
    }

    @Override
    public String getCorrectLocationWithWrongUser() throws Exception {
        return null;
    }

    @Override
    public boolean supportsNullLocation() {
        return true;
    }
    
    @Override
    public boolean targetIsWindows() {
        return Utils.isWindows();
    }

}
