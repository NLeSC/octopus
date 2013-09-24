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
package nl.esciencecenter.xenon;

import java.util.Map;

import nl.esciencecenter.xenon.credentials.Credentials;
import nl.esciencecenter.xenon.files.Files;
import nl.esciencecenter.xenon.jobs.Jobs;

/**
 * Main Xenon interface.
 * 
 * This interface provides an access point to all packages of Xenon and several utility functions.
 * 
 * @author Jason Maassen <J.Maassen@esciencecenter.nl>
 * @version 1.0
 * @since 1.0
 */
public interface Xenon {

    /**
     * Retrieve the <code>Files</code> interface.
     * 
     * @return a reference to the Files interface.
     */
    Files files();

    /**
     * Retrieve the <code>Jobs</code> interface.
     * 
     * @return a reference to the Files package interface.
     */
    Jobs jobs();

    /**
     * Retrieve the <code>Credentials</code> package interface.
     * 
     * @return a reference to the Credentials package interface.
     */
    Credentials credentials();

    /**
     * Returns the properties that where used to create this Xenon.
     * 
     * @return the properties used to create this Xenon.
     */
    Map<String, String> getProperties();

    /**
     * Returns information about the specified adaptor.
     * 
     * @param adaptorName
     *            the adaptor for which to return the information.            
     * @return an AdaptorInfo containing information about the specified adaptor.
     * @throws XenonException
     *             if the adaptor does not exist, or no information could be retrieved.
     */
    AdaptorStatus getAdaptorStatus(String adaptorName) throws XenonException;

    /**
     * Returns information on all adaptors available to this Xenon.
     * 
     * @return information on all adaptors.
     */
    AdaptorStatus[] getAdaptorStatuses();

}
