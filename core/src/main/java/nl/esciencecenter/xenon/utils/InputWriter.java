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
package nl.esciencecenter.xenon.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple input writer that uses a daemon thread to write from an {@link java.lang.String} to an {@link java.io.OutputStream}.
 * Once the end of the string is reached, the destination stream will be closed.
 */
public final class InputWriter extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(InputWriter.class);

    private final String content;

    private final OutputStream destination;

    // written all content or got exception.
    private boolean finished = false;

    /**
     * Create a new InputWriter that writes <code>content</code> to the <code>destination</code>.
     *
     * @param content the data to write to the destination.
     * @param destination the destination to write to.
     */
    public InputWriter(String content, OutputStream destination) {

        if (destination == null) {
            throw new IllegalArgumentException("Destination may not be null");
        }

        this.content = content;
        this.destination = destination;

        setDaemon(true);
        setName("Input Writer");
        start();
    }

    private synchronized void setFinished() {
        finished = true;
        notifyAll();
    }

    /**
     * Poll if the InputWriter has finished writing.
     *
     * @return
     *          if the InputWriter has finished writing.
     */
    public synchronized boolean isFinished() {
        return finished;
    }

    /**
     * Wait until the InputWriter has finished writing.
     */
    public synchronized void waitUntilFinished() {
        while (!finished) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Entry point for the Daemon thread.
     */
    @Override
    public void run() {
        try {
            if (content != null) {
                destination.write(content.getBytes(Charset.defaultCharset()));
            }
        } catch (IOException e) {
            LOGGER.error("Cannot write content to stream", e);
        } finally {
            try {
                destination.close();
            } catch (IOException e) {
                LOGGER.error("Cannot close input stream", e);
            }
            setFinished();
        }
    }
}
