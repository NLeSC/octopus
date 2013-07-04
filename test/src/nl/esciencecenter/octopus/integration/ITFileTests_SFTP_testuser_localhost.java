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
package nl.esciencecenter.octopus.integration;

import java.net.URI;

import nl.esciencecenter.octopus.credentials.Credential;
import nl.esciencecenter.octopus.credentials.Credentials;
import nl.esciencecenter.octopus.exceptions.OctopusException;

public class ITFileTests_SFTP_testuser_localhost extends AbstractFileTests {

    public String getTestUser() {

        //run tests on localhost explicit with 'testuser' account!   
        return "testuser";
    }

    public java.net.URI getTestLocation() throws Exception {

        String user = getTestUser();
        return new URI("sftp://" + user + "@localhost/tmp/testuser");
    }

    public Credential getCredentials() throws OctopusException {

        // use home user which runs the test:
        String userHome = System.getProperty("user.home");
        Credentials creds = octopus.credentials();
        String testUser = getTestUser();
        Credential cred =
                creds.newCertificateCredential("ssh", null, userHome + "/.ssh/id_rsa", userHome + "/.ssh/id_rsa.pub", testUser,
                        null);

        return cred;
    }

    // ===
    // Sftp Specific tests here: 
    // === 

}
