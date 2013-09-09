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
package nl.esciencecenter.octopus;

import java.util.Map;

import nl.esciencecenter.octopus.engine.OctopusEngine;
import nl.esciencecenter.octopus.exceptions.OctopusException;
import nl.esciencecenter.octopus.exceptions.OctopusIOException;

/**
 * OctopusFactory is used to create and end Octopus instances.
 * 
 * @author Jason Maassen <J.Maassen@esciencecenter.nl>
 * @version 1.0
 * @since 1.0
 */
public final class OctopusFactory {

    /**
     * Constructor of OctopusFactory should never be used.
     */
    private OctopusFactory() {
        // DO NOT USE
    }

    /**
     * Create a new Octopus using the given properties.
     * 
     * The properties provided will be passed to the octopus and its adaptors on creation time. Note that an
     * {@link OctopusException} will be thrown if properties contains any unknown keys.
     * 
     * @param properties
     *            (optional) properties used to configure the newly created octopus.
     * 
     * @return a new Octopus instance.
     * 
     * @throws UnknownPropertyException
     *             If an unknown property was passed.
     * @throws InvalidPropertyException
     *             If a known property was passed with an illegal value.
     * @throws OctopusException
     *             If the Octopus failed initialize.
     * @throws OctopusIOException
     *             If an I/O error occurred.  
     */
    public static Octopus newOctopus(Map<String, String> properties) throws OctopusException, OctopusIOException {
        return OctopusEngine.newOctopus(properties);
    }

    /**
     * Ends an Octopus.
     * 
     * Ending an Octopus will automatically close local resources, such as <code>Schedulers</code>, <code>FileSystems</code> and 
     * <code>Credentials</code>. In addition, all non online <code>Jobs</code> it has creates will be killed (for example jobs 
     * that run locally).
     * 
     * @param octopus
     *            the octopus to end.
     * 
     * @throws NoSuchOctopusException
     *             If the Octopus was not found
     * @throws OctopusException
     *             If the Octopus failed to end.
     */
    public static void endOctopus(Octopus octopus) throws OctopusException {
        OctopusEngine.closeOctopus(octopus);
    }

    /**
     * End all Octopus created by this factory.
     * 
     * All exceptions throw during endAll are ignored.
     */
    public static void endAll() {
        OctopusEngine.endAll();
    }
}