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
import java.util.HashMap;
import java.util.Properties;

import nl.esciencecenter.octopus.credentials.Credential;
import nl.esciencecenter.octopus.engine.OctopusEngine;
import nl.esciencecenter.octopus.engine.OctopusProperties;
import nl.esciencecenter.octopus.engine.jobs.JobStatusImplementation;
import nl.esciencecenter.octopus.engine.jobs.SchedulerImplementation;
import nl.esciencecenter.octopus.engine.util.JobQueues;
import nl.esciencecenter.octopus.exceptions.BadParameterException;
import nl.esciencecenter.octopus.exceptions.NoSuchSchedulerException;
import nl.esciencecenter.octopus.exceptions.OctopusException;
import nl.esciencecenter.octopus.exceptions.OctopusIOException;
import nl.esciencecenter.octopus.exceptions.UnsupportedJobDescriptionException;
import nl.esciencecenter.octopus.files.FileSystem;
import nl.esciencecenter.octopus.jobs.Job;
import nl.esciencecenter.octopus.jobs.JobDescription;
import nl.esciencecenter.octopus.jobs.JobStatus;
import nl.esciencecenter.octopus.jobs.Jobs;
import nl.esciencecenter.octopus.jobs.QueueStatus;
import nl.esciencecenter.octopus.jobs.Scheduler;
import nl.esciencecenter.octopus.jobs.Streams;

public class SshJobs implements Jobs {

    //private static final Logger logger = LoggerFactory.getLogger(SshJobs.class);

    private static int currentID = 1;

    private static synchronized String getNewUniqueID() {
        String res = "ssh" + currentID;
        currentID++;
        return res;
    }

    /**
     * Used to store all state attached to a scheduler. This way, SchedulerImplementation is immutable.
     */
    class SchedulerInfo {

        final SchedulerImplementation impl;
        final SshSession session;
        final FileSystem filesystem;
        final JobQueues jobQueues;

        SchedulerInfo(SchedulerImplementation impl, FileSystem fs, SshSession session, JobQueues jobQueues) {
            this.impl = impl;
            this.filesystem = fs;
            this.session = session;
            this.jobQueues = jobQueues;
        }
    }

    private final OctopusEngine octopusEngine;

    private final SshAdaptor adaptor;

    private final OctopusProperties properties;

    private final int pollingDelay;
    private final int multiQThreads;

    private HashMap<String, SchedulerInfo> schedulers = new HashMap<String, SchedulerInfo>();

    public SshJobs(OctopusProperties properties, SshAdaptor sshAdaptor, OctopusEngine octopusEngine) throws OctopusException {
        this.octopusEngine = octopusEngine;
        this.adaptor = sshAdaptor;
        this.properties = properties;

        multiQThreads = properties.getIntProperty(SshAdaptor.MULTIQ_MAX_CONCURRENT, 1);
        pollingDelay = properties.getIntProperty(SshAdaptor.POLLING_DELAY);

        if (multiQThreads <= 1) {
            throw new BadParameterException(SshAdaptor.ADAPTOR_NAME,
                    "Number of slots for the multi queue cannot be smaller than one!");
        }

        if (pollingDelay < 100 || pollingDelay > 60000) {
            throw new BadParameterException(SshAdaptor.ADAPTOR_NAME, "Polling delay must be between 100 and 60000!");
        }
    }

    @Override
    public Scheduler newScheduler(URI location, Credential credential, Properties properties) throws OctopusException,
            OctopusIOException {

        //adaptor.checkURI(location);
        adaptor.checkPath(location, "scheduler");

        // FIXME: Why can't we add scheduler specific properties ?
        if (properties != null && properties.size() > 0) {
            throw new OctopusException(SshAdaptor.ADAPTOR_NAME, "Cannot create ssh scheduler with additional properties!");
        }

        String uniqueID = getNewUniqueID();

        SshSession session = adaptor.createNewSession(location, credential, this.properties);

        SchedulerImplementation scheduler =
                new SchedulerImplementation(SshAdaptor.ADAPTOR_NAME, uniqueID, location, new String[] { "single", "multi",
                        "unlimited" }, credential, new OctopusProperties(properties), true, true, true);

        SshInteractiveProcessFactory factory = new SshInteractiveProcessFactory(session);

        // Create a file system that uses the same SSH session as the scheduler.
        SshFiles files = (SshFiles) adaptor.filesAdaptor();
        FileSystem fs = files.newFileSystem(session, location, credential, this.properties);

        JobQueues jobQueues =
                new JobQueues(SshAdaptor.ADAPTOR_NAME, octopusEngine, scheduler, fs, factory, multiQThreads, pollingDelay);

        synchronized (this) {
            schedulers.put(uniqueID, new SchedulerInfo(scheduler, fs, session, jobQueues));
        }

        return scheduler;
    }

    @Override
    public Scheduler getLocalScheduler() throws OctopusException, OctopusIOException {
        throw new OctopusException(getClass().getName(), "getLocalScheduler not supported!");
    }

    private JobQueues getJobQueue(Scheduler scheduler) throws OctopusException {

        if (!(scheduler instanceof SchedulerImplementation)) {
            throw new NoSuchSchedulerException(SshAdaptor.ADAPTOR_NAME, "Illegal scheduler type.");
        }

        SchedulerImplementation s = (SchedulerImplementation) scheduler;

        SchedulerInfo info = schedulers.get(s.getUniqueID());

        if (info == null) {
            throw new NoSuchSchedulerException(SshAdaptor.ADAPTOR_NAME, "Cannot find scheduler: " + s.getUniqueID());
        }

        return info.jobQueues;
    }

    @Override
    public Job[] getJobs(Scheduler scheduler, String... queueNames) throws OctopusException, OctopusIOException {
        return getJobQueue(scheduler).getJobs(queueNames);
    }

    @Override
    public Job submitJob(Scheduler scheduler, JobDescription description) throws OctopusException {

        if (description.getEnvironment().size() != 0) {
            throw new UnsupportedJobDescriptionException(SshAdaptor.ADAPTOR_NAME, "Environment variables not supported!");
        }

        return getJobQueue(scheduler).submitJob(description);
    }

    @Override
    public JobStatus getJobStatus(Job job) throws OctopusException {
        return getJobQueue(job.getScheduler()).getJobStatus(job);
    }

    @Override
    public JobStatus[] getJobStatuses(Job... jobs) {
        JobStatus[] result = new JobStatus[jobs.length];

        for (int i = 0; i < jobs.length; i++) {
            try {
                if (jobs[i] != null) {
                    result[i] = getJobStatus(jobs[i]);
                } else {
                    result[i] = null;
                }
            } catch (OctopusException e) {
                result[i] = new JobStatusImplementation(jobs[i], null, null, e, false, false, null);
            }
        }

        return result;
    }

    @Override
    public JobStatus waitUntilDone(Job job, long timeout) throws OctopusException, OctopusIOException {
        return getJobQueue(job.getScheduler()).waitUntilDone(job, timeout);
    }

    @Override
    public JobStatus waitUntilRunning(Job job, long timeout) throws OctopusException, OctopusIOException {
        return getJobQueue(job.getScheduler()).waitUntilRunning(job, timeout);
    }

    @Override
    public JobStatus cancelJob(Job job) throws OctopusException {
        return getJobQueue(job.getScheduler()).cancelJob(job);
    }

    public void end() {
        // FIXME!
        // singleExecutor.shutdownNow();
    }

    @Override
    public QueueStatus getQueueStatus(Scheduler scheduler, String queueName) throws OctopusException {
        return getJobQueue(scheduler).getQueueStatus(scheduler, queueName);
    }

    @Override
    public QueueStatus[] getQueueStatuses(Scheduler scheduler, String... queueNames) throws OctopusException {
        return getJobQueue(scheduler).getQueueStatuses(scheduler, queueNames);
    }

    @Override
    public void close(Scheduler scheduler) throws OctopusException, OctopusIOException {

        if (!(scheduler instanceof SchedulerImplementation)) {
            throw new OctopusException(SshAdaptor.ADAPTOR_NAME, "Illegal scheduler type.");
        }

        SchedulerImplementation s = (SchedulerImplementation) scheduler;

        SchedulerInfo info = null;

        synchronized (this) {
            info = schedulers.remove(s.getUniqueID());

            if (info == null) {
                throw new NoSuchSchedulerException(SshAdaptor.ADAPTOR_NAME, "Cannot find scheduler: " + s.getUniqueID());
            }
        }

        info.jobQueues.end();
        info.session.disconnect();
    }

    @Override
    public boolean isOpen(Scheduler scheduler) throws OctopusException, OctopusIOException {

        if (!(scheduler instanceof SchedulerImplementation)) {
            throw new OctopusException(SshAdaptor.ADAPTOR_NAME, "Illegal scheduler type.");
        }

        return schedulers.containsKey(((SchedulerImplementation) scheduler).getUniqueID());
    }

    @Override
    public String getDefaultQueueName(Scheduler scheduler) throws OctopusException, OctopusIOException {
        return getJobQueue(scheduler).getDefaultQueueName(scheduler);
    }

    @Override
    public Streams getStreams(Job job) throws OctopusException {
        return getJobQueue(job.getScheduler()).getStreams(job);
    }

}
