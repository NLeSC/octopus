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

package nl.esciencecenter.xenon.engine.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.engine.files.CopyImplementation;
import nl.esciencecenter.xenon.engine.files.CopyStatusImplementation;
import nl.esciencecenter.xenon.files.Copy;
import nl.esciencecenter.xenon.files.CopyOption;
import nl.esciencecenter.xenon.files.CopyStatus;
import nl.esciencecenter.xenon.files.FileAttributes;
import nl.esciencecenter.xenon.files.Files;
import nl.esciencecenter.xenon.files.IllegalSourcePathException;
import nl.esciencecenter.xenon.files.IllegalTargetPathException;
import nl.esciencecenter.xenon.files.InvalidResumeTargetException;
import nl.esciencecenter.xenon.files.NoSuchCopyException;
import nl.esciencecenter.xenon.files.NoSuchPathException;
import nl.esciencecenter.xenon.files.OpenOption;
import nl.esciencecenter.xenon.files.Path;
import nl.esciencecenter.xenon.files.PathAlreadyExistsException;
import nl.esciencecenter.xenon.files.RelativePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CopyEngine is responsible for performing the asynchronous copy operations.
 * 
 * @author Jason Maassen <J.Maassen@esciencecenter.nl>
 * @version 1.0
 * @since 1.0
 */
public final class CopyEngine {

    class CopyThread extends Thread {
        public void run() {
            CopyInfo ac = dequeue();

            while (ac != null) {
                startCopy(ac);
                ac = dequeue();
            }
        }
    }

    /** A logger for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(CopyEngine.class);

    /** The name of this unit */
    private static final String NAME = "CopyEngine";

    /** The polling delay */
    private static final int POLLING_DELAY = 1000;

    /** The default buffer size */
    private static final int BUFFER_SIZE = 4 * 1024;

    /** A Files used to access the files that need to be copied. */
    private final Files owner;

    /** Pending copies */
    private Deque<CopyInfo> pending = new LinkedList<>();

    /** Finished copies */
    private Map<String, CopyInfo> finished = new LinkedHashMap<>();

    /** Running copy */
    private CopyInfo running;

    /** Current Copy ID */
    private long nextID = 0;

    /** Should we terminate ? */
    private boolean done = false;

    public CopyEngine(Files owner) {
        this.owner = owner;
        new CopyThread().start();
    }

    private void close(Closeable c) {

        try {
            if (c != null) {
                c.close();
            }
        } catch (IOException e) {
            // ignored
        }
    }

    private void streamCopy(InputStream in, OutputStream out, CopyInfo ac) throws IOException {

        long total = 0;

        byte[] buffer = new byte[BUFFER_SIZE];

        if (ac.isCancelled()) {
            LOGGER.debug("Copy killed by user!");
            ac.setException(new IOException("Copy killed by user"));
            return;
        }

        int size = in.read(buffer);

        while (size > 0) {
            out.write(buffer, 0, size);
            total += size;

            ac.setBytesCopied(total);

            if (ac.isCancelled()) {
                LOGGER.debug("Copy killed by user!");
                ac.setException(new IOException("Copy killed by user"));
                return;
            }

            size = in.read(buffer);
        }
    }

    private void append(Path source, long fromOffset, Path target, CopyInfo ac) throws XenonException {

        // We need to append some bytes from source to target. 
        LOGGER.debug("Appending from {} to {} starting at {}", source, target, fromOffset);

        InputStream in = owner.newInputStream(source);
        OutputStream out = owner.newOutputStream(target, OpenOption.OPEN, OpenOption.APPEND);

        long skipped = 0;

        try {

            while (skipped < fromOffset) {
                long tmp = in.skip(fromOffset);

                if (tmp <= 0) {
                    throw new XenonException(NAME, "Failed to seek file " + source + " to " + fromOffset);
                }

                skipped += tmp;
            }

            streamCopy(in, out, ac);
        } catch (IOException e) {
            throw new XenonException(NAME, "Failed to copy " + source + ":" + fromOffset + " to target " + target, e);
        } finally {
            close(in);
            close(out);
        }
    }

    private int readFully(InputStream in, byte[] buffer) throws IOException {

        int offset = 0;

        while (offset < buffer.length) {
            int tmp = in.read(buffer, offset, buffer.length - offset);

            if (tmp <= 0) {
                break;
            }

            offset += tmp;
        }

        if (offset == 0) {
            return -1;
        }

        return offset;
    }

    private boolean compareHead(CopyInfo ac, Path target, Path source) throws XenonException, IOException {

        LOGGER.debug("Compare head of {} to {}", target, source);

        byte[] buf1 = new byte[BUFFER_SIZE];
        byte[] buf2 = new byte[BUFFER_SIZE];

        InputStream in1 = owner.newInputStream(target);
        InputStream in2 = owner.newInputStream(source);

        try {
            while (true) {

                if (ac.isCancelled()) {
                    throw new XenonException(NAME, "Copy killed by user");
                }

                int size1 = readFully(in1, buf1);
                int size2 = readFully(in2, buf2);

                if (size1 > size2) {
                    return false;
                }

                if (size1 < 0) {
                    return true;
                }

                for (int i = 0; i < size1; i++) {
                    if (buf1[i] != buf2[i]) {
                        return false;
                    }
                }
            }
        } finally {
            close(in1);
            close(in2);
        }
    }

    private void doResume(CopyInfo ac) throws XenonException {

        if (ac.isCancelled()) {
            ac.setException(new IOException("Copy killed by user"));
            return;
        }

        CopyImplementation copy = ac.getCopy();
        
        Path source = copy.getSource();
        Path target = copy.getTarget();

        LOGGER.debug("Resume copy from {} to {} verify={}", source, target, ac.mustVerify());

        if (!owner.exists(source)) {
            throw new NoSuchPathException(NAME, "Source " + source + " does not exist!");
        }

        FileAttributes sourceAtt = owner.getAttributes(source);

        if (sourceAtt.isDirectory()) {
            throw new IllegalSourcePathException(NAME, "Source " + source + " is a directory");
        }

        if (sourceAtt.isSymbolicLink()) {
            throw new IllegalSourcePathException(NAME, "Source " + source + " is a link");
        }

        if (!owner.exists(target)) {
            throw new NoSuchPathException(NAME, "Target " + target + " does not exist!");
        }

        FileAttributes targetAtt = owner.getAttributes(target);

        if (targetAtt.isDirectory()) {
            throw new IllegalTargetPathException(NAME, "Target " + target + " is a directory");
        }

        if (targetAtt.isSymbolicLink()) {
            throw new IllegalTargetPathException(NAME, "Target " + target + " is a link");
        }

        RelativePath sourceName = source.getRelativePath().normalize();
        RelativePath targetName = target.getRelativePath().normalize();
        
        if (sourceName.equals(targetName)) {
            return;
        }

        if (ac.mustVerify()) {
            if (ac.isCancelled()) {
                ac.setException(new IOException("Copy killed by user"));
                return;
            }

            // check if the data in target corresponds to the head of source.
            try {
                if (!compareHead(ac, target, source)) {
                    throw new InvalidResumeTargetException(NAME, "Data in target " + target + " does not match source " + source);
                }
            } catch (IOException e) {
                throw new XenonException(NAME, "Failed to compare " + source + " to " + target, e);
            }
        }

        long targetSize = targetAtt.size();
        long sourceSize = sourceAtt.size();

        LOGGER.debug("Resuming copy from {} to {} ? ", source, target, (sourceSize < targetSize));

        // If target is larger than source, they cannot be the same file.
        if (targetSize > sourceSize) {
            throw new InvalidResumeTargetException(NAME, "Data in target " + target + " does not match " + source + " "
                    + source);
        }

        // If target is the same size as source we are done.
        if (targetSize == sourceSize) {
            ac.setBytesToCopy(0);
            ac.setBytesCopied(0);
            return;
        }

        ac.setBytesToCopy(sourceSize - targetSize);

        // Now append source (from index targetSize) to target.
        append(source, targetSize, target, ac);
    }

    private void doAppend(CopyInfo ac) throws XenonException {

        if (ac.isCancelled()) {
            ac.setException(new IOException("Copy killed by user"));
            return;
        }

        CopyImplementation copy = ac.getCopy();
        
        Path source = copy.getSource();
        Path target = copy.getTarget();
        
        LOGGER.debug("Append from {} to {} verify={}", source, target);

        if (!owner.exists(source)) {
            throw new NoSuchPathException(NAME, "Source " + source + " does not exist!");
        }

        FileAttributes sourceAtt = owner.getAttributes(source);

        if (sourceAtt.isDirectory()) {
            throw new IllegalSourcePathException(NAME, "Source " + source + " is a directory");
        }

        if (!owner.exists(target)) {
            throw new NoSuchPathException(NAME, "Target " + target + " does not exist!");
        }

        FileAttributes targetAtt = owner.getAttributes(target);

        if (targetAtt.isDirectory()) {
            throw new IllegalSourcePathException(NAME, "Target " + target + " is a directory");
        }
        
        RelativePath sourceName = source.getRelativePath().normalize();
        RelativePath targetName = target.getRelativePath().normalize();
        
        if (sourceName.equals(targetName)) {
            throw new IllegalTargetPathException(NAME, "Can not append a file to itself (source " + source + " equals target " 
                    + target + ")");
        }

        ac.setBytesToCopy(sourceAtt.size());
        append(source, 0, target, ac);
    }

    private void doCopy(CopyInfo ac) throws XenonException {

        if (ac.isCancelled()) {
            ac.setException(new IOException("Copy killed by user"));
            return;
        }

        CopyImplementation copy = ac.getCopy();
        
        Path source = copy.getSource();
        Path target = copy.getTarget();

        boolean replace = (ac.getMode() == CopyOption.REPLACE);
        boolean ignore = (ac.getMode() == CopyOption.IGNORE);

        LOGGER.debug("Copy from {} to {} replace={} ignore={}", source, target, replace, ignore);

        if (!owner.exists(source)) {
            throw new NoSuchPathException(NAME, "Source " + source + " does not exist!");
        }

        FileAttributes sourceAtt = owner.getAttributes(source);

        if (sourceAtt.isDirectory()) {
            throw new IllegalSourcePathException(NAME, "Source " + source + " is a directory");
        }

        RelativePath sourceName = source.getRelativePath().normalize();
        RelativePath targetName = target.getRelativePath().normalize();
        
        if (sourceName.equals(targetName)) {
            return;
        }

        if (owner.exists(target)) {
            if (ignore) {
                return;
            }
            if (!replace) {
                throw new PathAlreadyExistsException(NAME, "Target " + target + " already exists!");
            }
        }

        RelativePath parentName = target.getRelativePath().getParent();
        Path parent = owner.newPath(target.getFileSystem(), parentName);
        
        if (!owner.exists(parent)) {
            throw new NoSuchPathException(NAME, "Target directory " + parent + " does not exist!");
        }

        ac.setBytesToCopy(sourceAtt.size());

        InputStream in = null;
        OutputStream out = null;

        try {
            in = owner.newInputStream(source);

            if (replace) {
                out = owner.newOutputStream(target, OpenOption.OPEN_OR_CREATE, OpenOption.TRUNCATE);
            } else {
                out = owner.newOutputStream(target, OpenOption.CREATE, OpenOption.APPEND);
            }

            streamCopy(in, out, ac);

        } catch (IOException e) {
            throw new XenonException(NAME, "Failed to copy " + source + " to " + target, e);
        } finally {
            close(in);
            close(out);
        }
    }

    private void startCopy(CopyInfo info) {

        LOGGER.debug("Start copy: {}", info);

        try {
            
            CopyOption mode = info.getMode(); 
            
            switch (mode) {
            case CREATE:
            case REPLACE:
            case IGNORE:
                doCopy(info);
                break;
            case APPEND:
                doAppend(info);
                break;
            case RESUME:
                doResume(info);
                break;
            default:
                throw new XenonException(NAME, "INTERNAL ERROR: Failed to recognise copy mode! (" + mode + " "
                        + info.mustVerify() + ")");
            }
        } catch (Exception e) {
            info.setException(e);
        }

        LOGGER.debug("Finished copy: {}", info);
    }

    public void copy(CopyInfo info) {
        if (info.isAsync()) {
            enqueue(info);
        } else {
            startCopy(info);
        }
    }

    public synchronized void done() {
        LOGGER.debug("Sending CopyEngine termination signal");
        done = true;
        notifyAll();
    }

    private synchronized void enqueue(CopyInfo info) {

        LOGGER.debug("CopyEngine queueing copy: {}", info);

        pending.addLast(info);
        notifyAll();
    }

    private synchronized CopyInfo dequeue() {

        LOGGER.debug("CopyEngine dequeueing copy");

        if (running != null) {
            finished.put(running.getUniqueID(), running);
            running = null;
            notifyAll();
        }

        while (!done && pending.size() == 0) {
            try {
                wait(POLLING_DELAY);
            } catch (InterruptedException e) {
                // ignore ?
            }
        }

        if (done) {
            LOGGER.debug("CopyEngine received termination signal with {} pending copies.", pending.size());
            return null;
        }

        running = pending.removeFirst();
        return running;
    }

    private synchronized void waitUntilCancelled(String copyID) {

        LOGGER.debug("Waiting until copy {} is cancelled.", copyID);

        while (running != null && running.hasID(copyID)) {
            try {
                wait(POLLING_DELAY);
            } catch (InterruptedException e) {
                // ignored.
            }
        }
    }

    public synchronized CopyStatus cancel(Copy copy) throws NoSuchCopyException {

        if (!(copy instanceof CopyImplementation)) {
            throw new NoSuchCopyException(NAME, "No such copy!");
        }

        CopyImplementation tmp = (CopyImplementation) copy;

        String copyID = tmp.getUniqueID();

        LOGGER.debug("Attempting to cancel copy {}.", copyID);

        if (running != null && running.hasID(copyID)) {
            LOGGER.debug("Canceled copy {} is running,", copyID);
            running.cancel();

            // We should now wait until the running copy is indeed cancelled. Otherwise, we are in an inconsistent state when 
            // we return.            
            waitUntilCancelled(copyID);
        }

        CopyInfo ac = finished.remove(copyID);

        if (ac != null) {
            LOGGER.debug("Canceled copy {} was already finished.", copyID);
            // Already finished
            return new CopyStatusImplementation(copy, "DONE", false, true, ac.getBytesToCopy(), ac.getBytesCopied(),
                    ac.getException());
        }

        Iterator<CopyInfo> it = pending.iterator();

        while (it.hasNext()) {

            CopyInfo c = it.next();

            if (c.hasID(copyID)) {
                it.remove();
                LOGGER.debug("Canceled copy {} was queued.", copyID);
                return new CopyStatusImplementation(copy, "KILLED", false, false, c.getBytesToCopy(), 0, new IOException(
                        "Copy killed by user"));
            }
        }

        throw new NoSuchCopyException(NAME, "No such copy " + copyID);
    }

    public synchronized CopyStatus getStatus(Copy copy) throws NoSuchCopyException {

        if (!(copy instanceof CopyImplementation)) {
            throw new NoSuchCopyException(NAME, "No such copy!");
        }

        CopyImplementation tmp = (CopyImplementation) copy;

        String copyID = tmp.getUniqueID();

        LOGGER.debug("Retrieving status of copy {}.", copyID);

        String state = null;
        CopyInfo ac = null;
        boolean isRunning = false;
        boolean isDone = false;

        if (running != null && running.hasID(copyID)) {
            state = "RUNNING";
            ac = running;
            isRunning = true;
        }

        if (ac == null) {
            ac = finished.remove(copyID);

            if (ac != null) {
                state = "DONE";
                isDone = true;
            }
        }

        if (ac == null) {
            for (CopyInfo c : pending) {
                if (c.hasID(copyID)) {
                    ac = c;
                    state = "PENDING";
                    break;
                }
            }
        }

        if (ac == null) {
            throw new NoSuchCopyException(NAME, "No such copy " + copyID);
        }

        return new CopyStatusImplementation(copy, state, isRunning, isDone, ac.getBytesToCopy(), ac.getBytesCopied(),
                ac.getException());
    }

    public synchronized String getNextID(String prefix) {
        return prefix + nextID++;
    }
}
