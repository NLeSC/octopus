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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import nl.esciencecenter.xenon.schedulers.Streams;

/**
 * Streams is a container for the standard input, output and error streams of a job.
 *
 * Note that these standard streams are only available for interactive jobs.
 */
public class StreamsImplementation implements Streams {

    private final String jobIdentifier;
    private final InputStream stdout;
    private final InputStream stderr;
    private final OutputStream stdin;

    /**
     * Create a Streams containing the job and its standard streams.
     *
     * @param jobIdentifier
     *            the identifier of the job.
     * @param stdout
     *            the standard output stream.
     * @param stdin
     *            the standard input stream.
     * @param stderr
     *            the standard error stream.
     */
    public StreamsImplementation(String jobIdentifier, InputStream stdout, OutputStream stdin, InputStream stderr) {
        this.jobIdentifier = jobIdentifier;
        this.stdout = stdout;
        this.stdin = stdin;
        this.stderr = stderr;
    }

    /**
     * Get the identifier of the job for which this Streams was created.
     *
     * @return the identifier of the ob.
     */
    public String getJobIdentifier() {
        return jobIdentifier;
    }

    /**
     * Returns the standard output stream of job.
     *
     * @return the standard output stream of job.
     */
    public InputStream getStdout() {
        return stdout;
    }

    /**
     * Returns the standard error stream of job.
     *
     * @return the standard error stream of job.
     */
    public InputStream getStderr() {
        return stderr;
    }

    /**
     * Returns the standard input stream of job.
     *
     * @return the standard input stream of this job.
     */
    public OutputStream getStdin() {
        return stdin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StreamsImplementation that = (StreamsImplementation) o;
        return Objects.equals(jobIdentifier, that.jobIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobIdentifier);
    }
}
