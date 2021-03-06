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
package nl.esciencecenter.xenon.adaptors;

import nl.esciencecenter.xenon.credentials.CertificateCredential;
import nl.esciencecenter.xenon.credentials.Credential;
import nl.esciencecenter.xenon.credentials.DefaultCredential;
import nl.esciencecenter.xenon.credentials.PasswordCredential;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

public class Utils {
    public static Map<String,String> buildProperties(String prefix) {
        Map<String,String> properties = new HashMap<>();
        for (String propName : System.getProperties().stringPropertyNames()) {
            if (propName.startsWith(prefix)) {
                properties.put(propName, System.getProperty(propName));
            }
        }
        return properties;
    }

    public static Credential buildCredential() {
        String username = System.getProperty("xenon.username");
        assertNotNull("liveTest expects 'xenon.username' system property", username);
        String password = System.getProperty("xenon.password");
        if (password != null) {
            return new PasswordCredential(username, password.toCharArray());
        }
        String certfile = System.getProperty("xenon.certfile");
        if (certfile != null) {
            String passphrase = System.getProperty("xenon.passphrase");
            if (passphrase == null) {
                return new CertificateCredential(username, certfile, new char[0]);
            }
            return new CertificateCredential(username, certfile, passphrase.toCharArray());
        }
        return new DefaultCredential(username);
    }
}
