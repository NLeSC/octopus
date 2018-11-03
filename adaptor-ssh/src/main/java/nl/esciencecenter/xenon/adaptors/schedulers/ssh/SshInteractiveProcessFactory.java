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

import static nl.esciencecenter.xenon.adaptors.schedulers.ssh.SshSchedulerAdaptor.ADAPTOR_NAME;

import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.adaptors.schedulers.InteractiveProcess;
import nl.esciencecenter.xenon.adaptors.schedulers.InteractiveProcessFactory;
import nl.esciencecenter.xenon.adaptors.schedulers.SchedulerClosedException;
import nl.esciencecenter.xenon.adaptors.shared.ssh.SSHConnection;
import nl.esciencecenter.xenon.schedulers.JobDescription;

public class SshInteractiveProcessFactory implements InteractiveProcessFactory {

    private final SSHConnection connection;

    protected SshInteractiveProcessFactory(SSHConnection session) {

        if (session == null) {
            throw new IllegalArgumentException("Session may not be null");
        }

        this.connection = session;
    }

    @Override
    public InteractiveProcess createInteractiveProcess(JobDescription description, String workdir, String jobIdentifier, long timeoutInMillis)
            throws XenonException {

        if (connection.isClosed()) {
            throw new SchedulerClosedException(ADAPTOR_NAME, "Scheduler is closed");
        }

        return new SshInteractiveProcess(connection.getSession(), description, jobIdentifier, timeoutInMillis);
    }

    @Override
    public void close() throws XenonException {

        if (connection.isClosed()) {
            throw new SchedulerClosedException(ADAPTOR_NAME, "Scheduler already closed");
        }

        try {
            connection.close();
        } catch (Exception e) {
            throw new XenonException(ADAPTOR_NAME, "Scheduler failed to close", e);
        }
    }

    @Override
    public boolean isOpen() throws XenonException {
        return connection.isOpen();
    }
}
