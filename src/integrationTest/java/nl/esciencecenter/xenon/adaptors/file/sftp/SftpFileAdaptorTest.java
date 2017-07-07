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
package nl.esciencecenter.xenon.adaptors.file.sftp;

import nl.esciencecenter.xenon.adaptors.GenericFileAdaptorTestParent;
import nl.esciencecenter.xenon.adaptors.filesystems.sftp.SftpFileTestConfig;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * 
 */
public class SftpFileAdaptorTest extends GenericFileAdaptorTestParent {

    @BeforeClass
    public static void prepareSSHFileAdaptorTest() throws Exception {
        GenericFileAdaptorTestParent.prepareClass(new SftpFileTestConfig(null));
    }

    @AfterClass
    public static void cleanupSSHFileAdaptorTest() throws Exception {
        GenericFileAdaptorTestParent.cleanupClass();
    }
}
