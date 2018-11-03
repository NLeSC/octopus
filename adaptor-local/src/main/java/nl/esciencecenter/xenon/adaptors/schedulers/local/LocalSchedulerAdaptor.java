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
package nl.esciencecenter.xenon.adaptors.schedulers.local;

import java.util.Map;

import nl.esciencecenter.xenon.InvalidCredentialException;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.XenonPropertyDescription;
import nl.esciencecenter.xenon.XenonPropertyDescription.Type;
import nl.esciencecenter.xenon.adaptors.XenonProperties;
import nl.esciencecenter.xenon.adaptors.schedulers.JobQueueScheduler;
import nl.esciencecenter.xenon.adaptors.schedulers.SchedulerAdaptor;
import nl.esciencecenter.xenon.credentials.Credential;
import nl.esciencecenter.xenon.credentials.DefaultCredential;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.schedulers.Scheduler;

/**
 * LocalFiles implements an Xenon <code>Jobs</code> adaptor for local job operations.
 *
 * @see nl.esciencecenter.xenon.schedulers.Scheduler
 *
 * @version 1.0
 * @since 1.0
 */
public class LocalSchedulerAdaptor extends SchedulerAdaptor {

    /** Name of the local adaptor is defined in the engine. */
    public static final String ADAPTOR_NAME = "local";

    /** Local properties start with this prefix. */
    public static final String PREFIX = SchedulerAdaptor.ADAPTORS_PREFIX + "local.";

    /** Description of the adaptor */
    public static final String ADAPTOR_DESCRIPTION = "The local jobs adaptor implements all functionality by emulating a local queue.";

    /** Local queue properties start with this prefix. */
    public static final String QUEUE = PREFIX + "queue.";

    /** Property for maximum history length for finished jobs */
    public static final String MAX_HISTORY = QUEUE + "historySize";

    /** Property for maximum history length for finished jobs */
    public static final String POLLING_DELAY = QUEUE + "pollingDelay";

    /** Local multi queue properties start with this prefix. */
    public static final String MULTIQ = QUEUE + "multi.";

    /** Property for the maximum number of concurrent jobs in the multi queue. */
    public static final String MULTIQ_MAX_CONCURRENT = MULTIQ + "maxConcurrentJobs";

    /** Local queue information start with this prefix. */
    public static final String INFO = PREFIX + "info.";

    /** Local job information start with this prefix. */
    public static final String JOBS = INFO + "jobs.";

    /** How many jobs have been submitted locally. */
    public static final String SUBMITTED = JOBS + "submitted";

    /** The locations supported by the adaptor */
    private static final String[] ADAPTOR_LOCATIONS = new String[] { "[/workdir]" };

    /** The properties supported by this adaptor */
    private static final XenonPropertyDescription[] VALID_PROPERTIES = new XenonPropertyDescription[] {
            new XenonPropertyDescription(POLLING_DELAY, Type.LONG, "1000", "The polling delay for monitoring running jobs (in milliseconds)."),
            new XenonPropertyDescription(MULTIQ_MAX_CONCURRENT, Type.INTEGER, "4", "The maximum number of concurrent jobs in the multiq.") };

    public LocalSchedulerAdaptor() {
        super(ADAPTOR_NAME, ADAPTOR_DESCRIPTION, ADAPTOR_LOCATIONS, VALID_PROPERTIES);
    }

    @Override
    public boolean isEmbedded() {
        // The local scheduler is embedded
        return true;
    }

    @Override
    public boolean supportsInteractive() {
        // The local scheduler supports interactive jobs
        return true;
    }

    @Override
    public Scheduler createScheduler(String location, Credential credential, Map<String, String> properties) throws XenonException {

        XenonProperties xp = new XenonProperties(VALID_PROPERTIES, properties);

        if (credential != null && !(credential instanceof DefaultCredential)) {
            throw new InvalidCredentialException(ADAPTOR_NAME, "Local scheduler does not support this credential!");
        }

        FileSystem filesystem = FileSystem.create("file", location, credential, properties);

        int processors = Runtime.getRuntime().availableProcessors();
        int multiQThreads = xp.getIntegerProperty(MULTIQ_MAX_CONCURRENT, processors);
        long pollingDelay = xp.getLongProperty(POLLING_DELAY);

        return new JobQueueScheduler(getNewUniqueID(), ADAPTOR_NAME, location == null ? "" : location, new DefaultCredential(),
                new LocalInteractiveProcessFactory(), filesystem, filesystem.getWorkingDirectory(), multiQThreads, pollingDelay, 0L, xp);
    }
}
