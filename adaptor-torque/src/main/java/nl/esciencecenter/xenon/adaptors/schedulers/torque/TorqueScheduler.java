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
package nl.esciencecenter.xenon.adaptors.schedulers.torque;

import static nl.esciencecenter.xenon.adaptors.schedulers.torque.TorqueSchedulerAdaptor.ACCOUNTING_GRACE_TIME_PROPERTY;
import static nl.esciencecenter.xenon.adaptors.schedulers.torque.TorqueSchedulerAdaptor.ADAPTOR_NAME;
import static nl.esciencecenter.xenon.adaptors.schedulers.torque.TorqueSchedulerAdaptor.POLL_DELAY_PROPERTY;
import static nl.esciencecenter.xenon.adaptors.schedulers.torque.TorqueUtils.JOB_OPTION_JOB_SCRIPT;
import static nl.esciencecenter.xenon.adaptors.schedulers.torque.TorqueUtils.QUEUE_INFO_NAME;
import static nl.esciencecenter.xenon.adaptors.schedulers.torque.TorqueUtils.getJobStatusFromQstatInfo;
import static nl.esciencecenter.xenon.adaptors.schedulers.torque.TorqueUtils.verifyJobDescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.esciencecenter.xenon.UnsupportedOperationException;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.XenonPropertyDescription;
import nl.esciencecenter.xenon.adaptors.schedulers.JobCanceledException;
import nl.esciencecenter.xenon.adaptors.schedulers.JobStatusImplementation;
import nl.esciencecenter.xenon.adaptors.schedulers.QueueStatusImplementation;
import nl.esciencecenter.xenon.adaptors.schedulers.RemoteCommandRunner;
import nl.esciencecenter.xenon.adaptors.schedulers.ScriptingParser;
import nl.esciencecenter.xenon.adaptors.schedulers.ScriptingScheduler;
import nl.esciencecenter.xenon.credentials.Credential;
import nl.esciencecenter.xenon.filesystems.Path;
import nl.esciencecenter.xenon.schedulers.JobDescription;
import nl.esciencecenter.xenon.schedulers.JobStatus;
import nl.esciencecenter.xenon.schedulers.NoSuchJobException;
import nl.esciencecenter.xenon.schedulers.NoSuchQueueException;
import nl.esciencecenter.xenon.schedulers.QueueStatus;
import nl.esciencecenter.xenon.schedulers.Streams;

/**
 * Interface to the TORQUE command line tools. Will run commands to submit/list/cancel jobs and get the status of queues.
 *
 */
public class TorqueScheduler extends ScriptingScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TorqueScheduler.class);

    private final long accountingGraceTime;

    /**
     * Map with the last seen time of jobs. There is a delay between jobs disappearing from the qstat queue output. Instead of throwing an exception, we allow
     * for a certain grace time. Jobs will report the status "pending" during this time. Typical delays are in the order of seconds.
     */
    private final Map<String, Long> lastSeenMap;

    // list of jobs we have killed before they even started. These will not end up in qstat, so we keep them here.
    private final Set<String> deletedJobs;

    private final TorqueXmlParser parser;

    private final String[] queueNames;

    TorqueScheduler(String uniqueID, String location, Credential credential, XenonPropertyDescription[] valid, Map<String, String> prop) throws XenonException {

        super(uniqueID, ADAPTOR_NAME, location, credential, prop, valid, POLL_DELAY_PROPERTY);

        accountingGraceTime = properties.getLongProperty(ACCOUNTING_GRACE_TIME_PROPERTY);

        parser = new TorqueXmlParser();

        lastSeenMap = new HashMap<>(30);
        deletedJobs = new HashSet<>(10);

        queueNames = queryQueueNames();
    }

    /* Query the queue names of the TORQUE batch system. */
    private String[] queryQueueNames() throws XenonException {
        Set<String> queueNameSet = queryQueues().keySet();
        return queueNameSet.toArray(new String[queueNameSet.size()]);
    }

    @Override
    public String[] getQueueNames() {
        return queueNames.clone();
    }

    @Override
    public String getDefaultQueueName() {
        return null;
    }

    private synchronized void updateJobsSeenMap(Set<String> identifiers) {
        long currentTime = System.currentTimeMillis();

        for (String identifier : identifiers) {
            lastSeenMap.put(identifier, currentTime);
        }

        long expiredTime = currentTime + accountingGraceTime;

        Iterator<Entry<String, Long>> iterator = lastSeenMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<String, Long> entry = iterator.next();

            if (entry.getValue() > expiredTime) {
                iterator.remove();
            }
        }

    }

    private synchronized boolean haveRecentlySeen(String identifier) {
        if (!lastSeenMap.containsKey(identifier)) {
            return false;
        }

        return (lastSeenMap.get(identifier) + accountingGraceTime) > System.currentTimeMillis();
    }

    private synchronized void addDeletedJob(String jobIdentifier) {
        deletedJobs.add(jobIdentifier);
    }

    /*
     * Note: Works exactly once per job.
     */
    private synchronized boolean jobWasDeleted(String jobIdentifier) {
        return deletedJobs.remove(jobIdentifier);
    }

    private void jobsFromStatus(String statusOutput, List<String> result) throws XenonException {
        Map<String, Map<String, String>> status = parser.parseJobInfos(statusOutput);
        updateJobsSeenMap(status.keySet());
        result.addAll(status.keySet());
    }

    @Override
    public String[] getJobs(String... queueNames) throws XenonException {
        List<String> result = new ArrayList<>(1500);

        if (queueNames == null || queueNames.length == 0) {
            String statusOutput = runCheckedCommand(null, "qstat", "-x").trim();

            jobsFromStatus(statusOutput, result);
        } else {
            for (String queueName : queueNames) {
                RemoteCommandRunner runner = runCommand(null, "qstat", "-x", queueName);

                if (runner.success()) {
                    jobsFromStatus(runner.getStdout(), result);
                } else if (runner.getExitCode() == 172) {
                    // slurm returns "172" as the exit code if there is something wrong with the queue, ignore
                    LOGGER.warn("Failed to get queue status for queue {}", runner);
                    throw new NoSuchQueueException(ADAPTOR_NAME, "Failed to get queue status for queue \"" + queueName + "\": " + runner);
                } else {
                    throw new XenonException(ADAPTOR_NAME, "Failed to get queue status for queue \"" + queueName + "\": " + runner);
                }
            }
        }

        return result.toArray(new String[result.size()]);
    }

    @Override
    public QueueStatus getQueueStatus(String queueName) throws XenonException {

        assertNonNullOrEmpty(queueName, "Queue name may not be null or empty");

        Map<String, Map<String, String>> allMap = queryQueues(queueName);

        Map<String, String> map = allMap.get(queueName);

        if (map == null || map.isEmpty()) {
            throw new NoSuchQueueException(ADAPTOR_NAME, "Cannot get status of queue \"" + queueName + "\" from server, perhaps it does not exist?");
        }

        return new QueueStatusImplementation(this, queueName, null, map);
    }

    @Override
    public QueueStatus[] getQueueStatuses(String... queueNames) throws XenonException {

        if (queueNames == null) {
            throw new IllegalArgumentException("Queue names may not be null");
        }

        Map<String, Map<String, String>> allMap = queryQueues(queueNames);

        if (queueNames.length == 0) {
            queueNames = allMap.keySet().toArray(new String[allMap.size()]);
        }

        return getQueueStatuses(allMap, queueNames);
    }

    protected Map<String, Map<String, String>> queryQueues(String... queueNames) throws XenonException {

        if (queueNames == null) {
            throw new IllegalArgumentException("Queue names cannot be null");
        }

        String output;
        if (queueNames.length == 0) {
            output = runCheckedCommand(null, "qstat", "-Qf");
        } else {

            ArrayList<String> args = new ArrayList<>();
            args.add("-Qf");

            for (String name : queueNames) {
                if (name != null) {
                    args.add(name);
                }
            }
            // String[] args = new String[1 + queueNames.length];
            // args[0] = "-Qf";
            // System.arraycopy(queueNames, 0, args, 1, queueNames.length);

            RemoteCommandRunner runner = runCommand(null, "qstat", args.toArray(new String[args.size()]));

            if (runner.success()) {
                output = runner.getStdout();
            } else {
                Map<String, Map<String, String>> badResult = new HashMap<>(2);
                for (String name : queueNames) {
                    badResult.put(name, null);
                }
                return badResult;
            }
        }
        String[] lines = ScriptingParser.NEWLINE_REGEX.split(output);

        Map<String, Map<String, String>> result = new HashMap<>(10);

        Map<String, String> currentQueueMap = null;
        for (String line : lines) {
            Matcher queueNameMatcher = QUEUE_INFO_NAME.matcher(line);
            if (queueNameMatcher.find()) {
                if (currentQueueMap != null) {
                    result.put(currentQueueMap.get("qname"), currentQueueMap);
                }
                currentQueueMap = new HashMap<>(lines.length);
                currentQueueMap.put("qname", queueNameMatcher.group(1));
            } else {
                String[] keyVal = ScriptingParser.EQUALS_REGEX.split(line, 2);
                if (keyVal.length == 2) {
                    if (currentQueueMap == null) {
                        throw new XenonException(ADAPTOR_NAME, "qstat does not follow syntax.");
                    }
                    currentQueueMap.put(keyVal[0], keyVal[1]);
                } // else: empty line
            }
        }
        if (currentQueueMap != null) {
            result.put(currentQueueMap.get("qname"), currentQueueMap);
        }

        return result;
    }

    @Override
    public String submitBatchJob(JobDescription description) throws XenonException {

        String output;

        Path workdir = getWorkingDirectory();

        verifyJobDescription(description);

        checkQueue(queueNames, description.getQueueName());

        // check for option that overrides job script completely.
        String customScriptFile = description.getJobOptions().get(JOB_OPTION_JOB_SCRIPT);

        if (customScriptFile == null) {
            checkWorkingDirectory(description.getWorkingDirectory());
            String jobScript = TorqueUtils.generate(description, workdir);

            output = runCheckedCommand(jobScript, "qsub");
        } else {
            // the user gave us a job script. Pass it to qsub as-is

            // convert to absolute path if needed
            if (!customScriptFile.startsWith("/")) {
                Path scriptFile = workdir.resolve(customScriptFile);
                customScriptFile = scriptFile.toString();
            }

            output = runCheckedCommand(null, "qsub", customScriptFile);
        }

        String identifier = ScriptingParser.parseJobIDFromLine(output, ADAPTOR_NAME, "");

        updateJobsSeenMap(Collections.singleton(identifier));

        if (!description.getJobOptions().containsKey(JOB_OPTION_JOB_SCRIPT)) {
            String[] idParts = identifier.split("\\.");
            try {
                long idNumber = Long.parseLong(idParts[0]);
                description.setStderr("xenon.e" + idNumber);
                description.setStdout("xenon.o" + idNumber);
            } catch (NumberFormatException ex) {
                LOGGER.warn("Standard out and standard err could not be set from Job ID {0}", identifier);
            }
        }

        return identifier;
    }

    @Override
    public Streams submitInteractiveJob(JobDescription description) throws XenonException {
        throw new UnsupportedOperationException(ADAPTOR_NAME, "Interactive jobs not supported");
    }

    @Override
    public JobStatus cancelJob(String jobIdentifier) throws XenonException {

        assertNonNullOrEmpty(jobIdentifier, "Job identifier cannot be null or empty");

        RemoteCommandRunner runner = runCommand(null, "qdel", jobIdentifier);
        if (runner.success()) {
            // deleted or already finished
            addDeletedJob(jobIdentifier);
        } else if (runner.getExitCode() == 170) {
            // job was already finished.
        } else {
            throw new XenonException(ADAPTOR_NAME, "could not run command qdel for job \"" + jobIdentifier + "\". Exit code = " + runner.getExitCode()
                    + " Output: " + runner.getStdout() + " Error output: " + runner.getStderr());
        }

        return getJobStatus(jobIdentifier);
    }

    private Map<String, Map<String, String>> getQstatInfo() throws XenonException {
        RemoteCommandRunner runner = runCommand(null, "qstat", "-x");

        if (!runner.success()) {
            LOGGER.debug("failed to get job status {}", runner);
            return new HashMap<>(0);
        }

        Map<String, Map<String, String>> result = parser.parseJobInfos(runner.getStdout());

        // mark jobs we found as seen, in case they disappear from the queue
        updateJobsSeenMap(result.keySet());

        return result;
    }

    /**
     * Get job status. First checks given qstat info map, but also runs additional qacct and qdel commands if needed.
     *
     * @param qstatInfo
     *            the info to get the job status from.
     * @param job
     *            the job to get the status for.
     * @return the JobStatus of the job.
     * @throws XenonException
     *             in case the info is not valid.
     * @throws XenonException
     *             in case an additional command fails to run.
     */
    private JobStatus getJobStatus(Map<String, Map<String, String>> qstatInfo, String job) throws XenonException {
        if (job == null) {
            return null;
        }

        JobStatus status = getJobStatusFromQstatInfo(qstatInfo, job);

        if (status != null && status.hasException()) {
            status = cancelJob(job);
        }

        if (status == null) {
            if (jobWasDeleted(job)) {
                XenonException exception = new JobCanceledException(ADAPTOR_NAME, "Job " + job + " deleted by user");
                status = new JobStatusImplementation(job, "killed", null, exception, false, true, null);
            } else if (haveRecentlySeen(job)) {
                status = new JobStatusImplementation(job, "unknown", null, null, false, true, new HashMap<String, String>(0));
            }
        } else if (status.isDone() && jobWasDeleted(job)) {
            XenonException exception = new JobCanceledException(ADAPTOR_NAME, "Job " + job + " deleted by user");
            status = new JobStatusImplementation(job, "killed", status.getExitCode(), exception, false, true, status.getSchedulerSpecificInformation());
        }

        return status;
    }

    @Override
    public JobStatus getJobStatus(String jobIdentifier) throws XenonException {

        assertNonNullOrEmpty(jobIdentifier, "Job identifier cannot be null or empty");

        Map<String, Map<String, String>> info = getQstatInfo();

        JobStatus result = getJobStatus(info, jobIdentifier);

        // this job really does not exist. throw an exception
        if (result == null) {
            throw new NoSuchJobException(ADAPTOR_NAME, "Job " + jobIdentifier + " not found on server");
        }

        return result;
    }

    @Override
    public JobStatus[] getJobStatuses(String... jobs) throws XenonException {
        Map<String, Map<String, String>> info = getQstatInfo();

        JobStatus[] result = new JobStatus[jobs.length];

        for (int i = 0; i < result.length; i++) {
            if (jobs[i] == null) {
                result[i] = null;
            } else {
                result[i] = getJobStatus(info, jobs[i]);

                // this job really does not exist. set it to an error state.
                if (result[i] == null) {
                    XenonException exception = new NoSuchJobException(ADAPTOR_NAME, "Job " + jobs[i] + " not found on server");
                    result[i] = new JobStatusImplementation(jobs[i], null, null, exception, false, false, null);
                }
            }
        }
        return result;
    }
}
