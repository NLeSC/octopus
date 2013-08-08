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
package nl.esciencecenter.octopus.adaptors.scripting;

import java.net.URI;
import java.util.Map;

import nl.esciencecenter.octopus.jobs.Scheduler;

/**
 * @author Niels Drost
 *
 */
public class FakeScriptingScheduler implements Scheduler {

    public FakeScriptingScheduler() {
        
    }
    
    @Override
    public String getAdaptorName() {
        return null;
    }

    @Override
    public URI getUri() {
        return null;
    }

    @Override
    public Map<String, String> getProperties() {
        return null;
    }

    @Override
    public String[] getQueueNames() {
        return null;
    }

    @Override
    public boolean supportsInteractive() {
        return false;
    }

    @Override
    public boolean supportsBatch() {
        return false;
    }

    @Override
    public boolean isOnline() {
        return false;
    }

}
