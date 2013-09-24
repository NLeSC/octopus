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

package nl.esciencecenter.xenon.examples.credentials;

import nl.esciencecenter.xenon.Xenon;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.XenonFactory;
import nl.esciencecenter.xenon.credentials.Credential;
import nl.esciencecenter.xenon.credentials.Credentials;

/**
 * A simple example of how to create credentials.
 * 
 * @author Jason Maassen <J.Maassen@esciencecenter.nl>
 * @version 1.0
 * @since 1.0
 */
public class CreatingCredential {

    public static void main(String[] args) {
        try {
            // First, we create a new Xenon using the XenonFactory (without providing any properties).
            Xenon xenon = XenonFactory.newXenon(null);

            // Next, we retrieve the Credentials API
            Credentials credentials = xenon.credentials();

            // We can now retrieve the default credential for a certain scheme
            Credential credential1 = credentials.getDefaultCredential("ssh");

            // We can also create other types of credentials
            Credential credential2 = credentials.newPasswordCredential("ssh", "username", "password".toCharArray(), null);

            // Close the credentials once we are done.
            credentials.close(credential1);
            credentials.close(credential2);

            // Finally, we end Xenon to release all resources 
            XenonFactory.endXenon(xenon);

        } catch (XenonException e) {
            System.out.println("CreatingCredential example failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
