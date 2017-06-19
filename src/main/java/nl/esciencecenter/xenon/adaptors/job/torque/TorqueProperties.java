/**
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
package nl.esciencecenter.xenon.adaptors.job.torque;

import java.util.EnumSet;

import nl.esciencecenter.xenon.XenonPropertyDescription;
import nl.esciencecenter.xenon.XenonPropertyDescription.Component;
import nl.esciencecenter.xenon.XenonPropertyDescription.Type;
import nl.esciencecenter.xenon.engine.XenonEngine;
import nl.esciencecenter.xenon.engine.XenonPropertyDescriptionImplementation;
import nl.esciencecenter.xenon.engine.util.ImmutableArray;

/**
 * Adaptor for TORQUE batch system.
 * 
 * @version 1.0
 * @since 1.0
 */
public class TorqueProperties {

    /** The name of this adaptor */
    public static final String ADAPTOR_NAME = "torque";

    /** The prefix used by all properties related to this adaptor */
    public static final String PREFIX = XenonEngine.ADAPTORS_PREFIX + ADAPTOR_NAME + ".";

    /** The schemes supported by this adaptor */
    public static final ImmutableArray<String> ADAPTOR_SCHEMES = new ImmutableArray<>("torque");
    
    /** The locations supported by this adaptor */
    public static final ImmutableArray<String> ADAPTOR_LOCATIONS = new ImmutableArray<>("(locations supported by local)", 
            "(locations supported by ssh)");
    
    /** Should the grid engine version on the target machine be ignored ? */
    public static final String IGNORE_VERSION_PROPERTY = PREFIX + "ignore.version";

    /** Timeout for waiting for the accounting info of a job to appear */
    public static final String ACCOUNTING_GRACE_TIME_PROPERTY = PREFIX + "accounting.grace.time";

    /** Polling delay for jobs started by this adaptor. */
    public static final String POLL_DELAY_PROPERTY = PREFIX + "poll.delay";

    /** Human readable description of this adaptor */
    public static final String ADAPTOR_DESCRIPTION = "The Torque Adaptor submits jobs to a TORQUE batch system."
            + " This adaptor uses either the local or the ssh adaptor to gain access to the scheduler machine.";

    /** List of all properties supported by this adaptor */
    public static final ImmutableArray<XenonPropertyDescription> VALID_PROPERTIES = 
            new ImmutableArray<XenonPropertyDescription>(
        new XenonPropertyDescriptionImplementation(IGNORE_VERSION_PROPERTY, Type.BOOLEAN, EnumSet.of(Component.SCHEDULER),
                "false", "Skip version check is skipped when connecting to remote machines. "
                        + "WARNING: it is not recommended to use this setting in production environments!"),
        new XenonPropertyDescriptionImplementation(ACCOUNTING_GRACE_TIME_PROPERTY, Type.LONG, EnumSet.of(Component.SCHEDULER),
                "60000", "Number of milliseconds a job is allowed to take going from the queue to the accinfo output."),

        new XenonPropertyDescriptionImplementation(POLL_DELAY_PROPERTY, Type.LONG, EnumSet.of(Component.SCHEDULER), "1000",
                "Number of milliseconds between polling the status of a job."));
}