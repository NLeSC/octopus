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
package nl.esciencecenter.xenon.adaptors.schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.XenonPropertyDescription;
import nl.esciencecenter.xenon.adaptors.XenonProperties;
import nl.esciencecenter.xenon.schedulers.IncompleteJobDescriptionException;
import nl.esciencecenter.xenon.schedulers.InvalidJobDescriptionException;
import nl.esciencecenter.xenon.schedulers.JobDescription;
import nl.esciencecenter.xenon.schedulers.Scheduler;

public class ScriptingUtils {

    private ScriptingUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isLocal(String location) {
        return (location == null || location.length() == 0 || location.startsWith("local://"));
    }

    public static boolean isSSH(String location) {
        return (location != null && location.startsWith("ssh://"));
    }

    public static XenonPropertyDescription[] mergeValidProperties(XenonPropertyDescription[]... prop) {

        if (prop == null || prop.length == 0) {
            return new XenonPropertyDescription[0];
        }

        ArrayList<XenonPropertyDescription> tmp = new ArrayList<>();

        for (XenonPropertyDescription[] pa : prop) {
            if (pa != null) {
                tmp.addAll(Arrays.asList(pa));
            }
        }

        return tmp.toArray(new XenonPropertyDescription[tmp.size()]);
    }

    public static XenonProperties getProperties(XenonPropertyDescription[] validProperties, String location, Map<String, String> properties)
            throws XenonException {

        if (isLocal(location)) {
            return new XenonProperties(mergeValidProperties(validProperties, Scheduler.getAdaptorDescription("local").getSupportedProperties()), properties);
        } else {
            return new XenonProperties(mergeValidProperties(validProperties, Scheduler.getAdaptorDescription("ssh").getSupportedProperties()), properties);
        }
    }

    /**
     * Do some checks on a job description.
     *
     * @param description
     *            the job description to check
     * @param adaptorName
     *            the name of the adaptor. Used when an exception is thrown
     * @throws IncompleteJobDescriptionException
     *             if the description is missing a mandatory value.
     * @throws InvalidJobDescriptionException
     *             if the description contains illegal values.
     */
    public static void verifyJobDescription(JobDescription description, String adaptorName) throws XenonException {
        String executable = description.getExecutable();

        if (executable == null) {
            throw new IncompleteJobDescriptionException(adaptorName, "Executable missing in JobDescription!");
        }

        int nodeCount = description.getNodeCount();

        if (nodeCount < 1) {
            throw new InvalidJobDescriptionException(adaptorName, "Illegal node count: " + nodeCount);
        }

        int processesPerNode = description.getProcessesPerNode();

        if (processesPerNode < 1) {
            throw new InvalidJobDescriptionException(adaptorName, "Illegal processes per node count: " + processesPerNode);
        }

        int maxTime = description.getMaxRuntime();

        if (maxTime <= 0) {
            throw new InvalidJobDescriptionException(adaptorName, "Illegal maximum runtime: " + maxTime);
        }
    }

    public static void verifyJobOptions(Map<String, String> options, String[] validOptions, String adaptorName) throws InvalidJobDescriptionException {

        // check if all given job options are valid
        for (String option : options.keySet()) {
            boolean found = false;
            for (String validOption : validOptions) {
                if (validOption.equals(option)) {
                    found = true;
                }
            }
            if (!found) {
                throw new InvalidJobDescriptionException(adaptorName, "Given Job option \"" + option + "\" not supported");
            }
        }
    }

    /**
     * Check if the info map for a job exists, contains the expected job ID, and contains the given additional fields
     *
     * @param jobInfo
     *            the info map to check.
     * @param jobIdentifier
     *            the unique identifier of the job.
     * @param adaptorName
     *            name of the current adaptor for error reporting.
     * @param jobIDField
     *            the field which contains the job id.
     * @param additionalFields
     *            any additional fields to check the presence of.
     * @throws XenonException
     *             if any fields are missing or incorrect
     */
    public static void verifyJobInfo(Map<String, String> jobInfo, String jobIdentifier, String adaptorName, String jobIDField, String... additionalFields)
            throws XenonException {
        if (jobInfo == null) {
            // redundant check, calling functions usually already check for this and return null.
            throw new XenonException(adaptorName, "Job " + jobIdentifier + " not found in job info");
        }

        String jobID = jobInfo.get(jobIDField);

        if (jobID == null) {
            throw new XenonException(adaptorName, "Invalid job info. Info does not contain job id");
        }

        if (!jobID.equals(jobIdentifier)) {
            throw new XenonException(adaptorName, "Invalid job info. Found job id \"" + jobID + "\" does not match " + jobIdentifier);
        }

        for (String field : additionalFields) {
            if (!jobInfo.containsKey(field)) {
                throw new XenonException(adaptorName, "Invalid job info. Info does not contain mandatory field \"" + field + "\"");
            }
        }
    }

}
