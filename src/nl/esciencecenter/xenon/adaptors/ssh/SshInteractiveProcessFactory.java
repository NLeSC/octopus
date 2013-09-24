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

import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.engine.jobs.JobImplementation;
import nl.esciencecenter.xenon.engine.util.InteractiveProcess;
import nl.esciencecenter.xenon.engine.util.InteractiveProcessFactory;

/**
 * @author Jason Maassen <J.Maassen@esciencecenter.nl>
 * 
 */
public class SshInteractiveProcessFactory implements InteractiveProcessFactory {

    private final SshMultiplexedSession session;

    public SshInteractiveProcessFactory(SshMultiplexedSession session) {
        this.session = session;
    }

    @Override
    public InteractiveProcess createInteractiveProcess(JobImplementation job) throws XenonException {
        return new SshInteractiveProcess(session, job);
    }
}
