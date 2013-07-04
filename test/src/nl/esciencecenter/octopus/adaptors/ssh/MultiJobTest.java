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

package nl.esciencecenter.octopus.adaptors.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Properties;

import org.junit.BeforeClass;

import static org.junit.Assert.assertTrue;

import nl.esciencecenter.octopus.Octopus;
import nl.esciencecenter.octopus.OctopusFactory;
import nl.esciencecenter.octopus.credentials.Credentials;
import nl.esciencecenter.octopus.files.AbsolutePath;
import nl.esciencecenter.octopus.files.FileSystem;
import nl.esciencecenter.octopus.files.Files;
import nl.esciencecenter.octopus.files.RelativePath;
import nl.esciencecenter.octopus.jobs.Job;
import nl.esciencecenter.octopus.jobs.JobDescription;
import nl.esciencecenter.octopus.jobs.JobStatus;
import nl.esciencecenter.octopus.jobs.Jobs;
import nl.esciencecenter.octopus.jobs.Scheduler;

/**
 * @author Jason Maassen <J.Maassen@esciencecenter.nl>
 * 
 */
public class MultiJobTest {

    private String readFully(InputStream in) throws IOException {

        byte[] buffer = new byte[1024];

        int offset = 0;

        int tmp = in.read(buffer, 0, buffer.length - offset);

        while (tmp != -1) {

            offset += tmp;

            if (offset == buffer.length) {
                buffer = Arrays.copyOf(buffer, buffer.length * 2);
            }

            tmp = in.read(buffer, offset, buffer.length - offset);
        }

        in.close();
        return new String(buffer, 0, offset);
    }

    private void submitToQueueWithPolling(String testName, String queueName, int jobCount) throws Exception {

        System.err.println("STARTING TEST submitToQueueWithPolling(" + testName + ", " + queueName + ", " + jobCount + ")");

        String TEST_ROOT = "octopus_test_SSH_" + System.currentTimeMillis();
        URI correctURI = new URI("ssh://test@localhost");
        URI correctFSURI = new URI("sftp://test@localhost");

        Octopus octopus = OctopusFactory.newOctopus(null);
        Files files = octopus.files();
        Jobs jobs = octopus.jobs();
        Credentials credentials = octopus.credentials();

        FileSystem filesystem = files.newFileSystem(correctFSURI, credentials.getDefaultCredential("sftp"), new Properties());
        Scheduler scheduler = jobs.newScheduler(correctURI, credentials.getDefaultCredential("ssh"), new Properties());

        String workingDir = TEST_ROOT + "/" + testName;

        AbsolutePath root = filesystem.getEntryPath().resolve(new RelativePath(workingDir));
        files.createDirectories(root);

        AbsolutePath[] out = new AbsolutePath[jobCount];
        AbsolutePath[] err = new AbsolutePath[jobCount];

        Job[] j = new Job[jobCount];

        for (int i = 0; i < j.length; i++) {

            out[i] = root.resolve(new RelativePath("stdout" + i + ".txt"));
            err[i] = root.resolve(new RelativePath("stderr" + i + ".txt"));

            JobDescription description = new JobDescription();
            description.setExecutable("/bin/sleep");
            description.setArguments("1");
            description.setWorkingDirectory(workingDir);

            description.setQueueName(queueName);
            description.setInteractive(false);
            description.setStdin(null);
            description.setStdout("stdout" + i + ".txt");
            description.setStderr("stderr" + i + ".txt");

            j[i] = jobs.submitJob(scheduler, description);
        }

        // Bit hard to determine realistic deadline here ?
        long deadline = System.currentTimeMillis() + (60 * jobCount * 1000);

        boolean done = false;

        while (!done) {
            JobStatus[] status = jobs.getJobStatuses(j);

            int count = 0;

            for (int i = 0; i < j.length; i++) {
                if (j[i] != null) {
                    if (status[i].isDone()) {
                        if (status[i].hasException()) {
                            System.err.println("Job " + i + " failed!");
                            throw new Exception("Job " + i + " failed", status[i].getException());
                        }

                        System.err.println("Job " + i + " done.");
                        j[i] = null;
                    } else {
                        count++;
                    }
                }
            }

            if (count == 0) {
                done = true;
            } else {
                Thread.sleep(1000);

                long now = System.currentTimeMillis();

                if (now > deadline) {
                    throw new Exception("Job exceeded deadline!");
                }
            }
        }

        for (int i = 0; i < j.length; i++) {

            String tmpout = readFully(files.newInputStream(out[i]));
            String tmperr = readFully(files.newInputStream(err[i]));

            assertTrue(tmpout != null);
            assertTrue(tmpout.length() == 0);

            assertTrue(tmperr != null);
            assertTrue(tmperr.length() == 0);

            files.delete(out[i]);
            files.delete(err[i]);
        }

        jobs.close(scheduler);
        files.delete(root);
        files.close(filesystem);

        OctopusFactory.endOctopus(octopus);
    }

    @org.junit.Test
    public void test33b_testMultiBatchJobSubmitWithPolling() throws Exception {
        submitToQueueWithPolling("test33a_unlimited", "unlimited", 100);
    }
}
