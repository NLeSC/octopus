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

package nl.esciencecenter.xenon.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.XenonRuntimeException;
import nl.esciencecenter.xenon.IncompatibleVersionException;
import nl.esciencecenter.xenon.InvalidCredentialException;
import nl.esciencecenter.xenon.InvalidLocationException;
import nl.esciencecenter.xenon.InvalidPropertyException;
import nl.esciencecenter.xenon.NoSuchXenonException;
import nl.esciencecenter.xenon.UnknownPropertyException;
import nl.esciencecenter.xenon.adaptors.ssh.ConnectionLostException;
import nl.esciencecenter.xenon.adaptors.ssh.EndOfFileException;
import nl.esciencecenter.xenon.adaptors.ssh.NotConnectedException;
import nl.esciencecenter.xenon.adaptors.ssh.PermissionDeniedException;
import nl.esciencecenter.xenon.adaptors.ssh.UnsupportedIOOperationException;
import nl.esciencecenter.xenon.engine.PropertyTypeException;
import nl.esciencecenter.xenon.engine.util.BadParameterException;
import nl.esciencecenter.xenon.engine.util.CommandNotFoundException;
import nl.esciencecenter.xenon.files.AttributeNotSupportedException;
import nl.esciencecenter.xenon.files.DirectoryNotEmptyException;
import nl.esciencecenter.xenon.files.IllegalSourcePathException;
import nl.esciencecenter.xenon.files.IllegalTargetPathException;
import nl.esciencecenter.xenon.files.InvalidCopyOptionsException;
import nl.esciencecenter.xenon.files.InvalidOpenOptionsException;
import nl.esciencecenter.xenon.files.InvalidResumeTargetException;
import nl.esciencecenter.xenon.files.NoSuchCopyException;
import nl.esciencecenter.xenon.files.NoSuchPathException;
import nl.esciencecenter.xenon.files.PathAlreadyExistsException;
import nl.esciencecenter.xenon.jobs.IncompleteJobDescriptionException;
import nl.esciencecenter.xenon.jobs.InvalidJobDescriptionException;
import nl.esciencecenter.xenon.jobs.JobCanceledException;
import nl.esciencecenter.xenon.jobs.NoSuchJobException;
import nl.esciencecenter.xenon.jobs.NoSuchQueueException;
import nl.esciencecenter.xenon.jobs.NoSuchSchedulerException;
import nl.esciencecenter.xenon.jobs.UnsupportedJobDescriptionException;

import org.junit.Test;

/**
 * @author Jason Maassen <J.Maassen@esciencecenter.nl>
 * 
 */
public class ExceptionsTest {

    private void testException(Exception e, String name, String message, Throwable cause) {

        if (name == null) {
            assertEquals(message, e.getMessage());
        } else {
            assertEquals(name + " adaptor: " + message, e.getMessage());
        }
        assertTrue(e.getCause() == cause);
    }

    private void testException(Exception e, Throwable cause) {
        testException(e, "name", "message", cause);
    }

    private void testException(Exception e) {
        testException(e, "name", "message", null);
    }

    @Test
    public void testXenonException1() throws Exception {
        testException(new XenonException("name", "message"));
    }

    @Test
    public void testXenonException2() throws Exception {
        Throwable t = new Throwable();
        testException(new XenonException("name", "message", t), t);
    }

    @Test
    public void testXenonException3() throws Exception {
        testException(new XenonException(null, "message"), null, "message", null);
    }

    @Test
    public void testXenonRuntimeException1() throws Exception {
        testException(new XenonRuntimeException("name", "message"));
    }

    @Test
    public void testXenonRuntimeException2() throws Exception {
        Throwable t = new Throwable();
        testException(new XenonRuntimeException("name", "message", t), t);
    }

    @Test
    public void testXenonIOException3() throws Exception {
        testException(new XenonException(null, "message"), null, "message", null);
    }

    @Test
    public void testXenonIOException1() throws Exception {
        testException(new XenonException("name", "message"));
    }

    @Test
    public void testXenonIOException2() throws Exception {
        Throwable t = new Throwable();
        testException(new XenonException("name", "message", t), t);
    }

    @Test
    public void testXenonRuntimeException3() throws Exception {
        testException(new XenonRuntimeException(null, "message"), null, "message", null);
    }

    @Test
    public void testAttributeNotSupportedException1() throws Exception {
        testException(new AttributeNotSupportedException("name", "message"));
    }

    @Test
    public void testAttributeNotSupportedException2() throws Exception {
        Throwable t = new Throwable();
        testException(new AttributeNotSupportedException("name", "message", t), t);
    }

    @Test
    public void testBadParameterException1() throws Exception {
        testException(new BadParameterException("name", "message"));
    }

    @Test
    public void testBadParameterException2() throws Exception {
        Throwable t = new Throwable();
        testException(new BadParameterException("name", "message", t), t);
    }

    @Test
    public void testCommandNotFoundException1() throws Exception {
        testException(new CommandNotFoundException("name", "message"));
    }

    @Test
    public void testCommandNotFoundException2() throws Exception {
        Throwable t = new Throwable();
        testException(new CommandNotFoundException("name", "message", t), t);
    }

    @Test
    public void testConnectionLostException1() throws Exception {
        testException(new ConnectionLostException("name", "message"));
    }

    @Test
    public void testConnectionLostException2() throws Exception {
        Throwable t = new Throwable();
        testException(new ConnectionLostException("name", "message", t), t);
    }

    @Test
    public void testDirectoryIteratorException1() throws Exception {
        testException(new InvalidCopyOptionsException("name", "message"));
    }

    @Test
    public void testDirectoryIteratorException2() throws Exception {
        Throwable t = new Throwable();
        testException(new InvalidCopyOptionsException("name", "message", t), t);
    }

    @Test
    public void testDirectoryNotEmptyException1() throws Exception {
        testException(new DirectoryNotEmptyException("name", "message"));
    }

    @Test
    public void testDirectoryNotEmptyException2() throws Exception {
        Throwable t = new Throwable();
        testException(new DirectoryNotEmptyException("name", "message", t), t);
    }

    @Test
    public void testEndOfFileException1() throws Exception {
        testException(new EndOfFileException("name", "message"));
    }

    @Test
    public void testEndOfFileException2() throws Exception {
        Throwable t = new Throwable();
        testException(new EndOfFileException("name", "message", t), t);
    }

    @Test
    public void testFileAlreadyExistsException1() throws Exception {
        testException(new PathAlreadyExistsException("name", "message"));
    }

    @Test
    public void testFileAlreadyExistsException2() throws Exception {
        Throwable t = new Throwable();
        testException(new PathAlreadyExistsException("name", "message", t), t);
    }

    @Test
    public void testIllegalSourcePathException1() throws Exception {
        testException(new IllegalSourcePathException("name", "message"));
    }

    @Test
    public void testIllegalSourcePathException2() throws Exception {
        Throwable t = new Throwable();
        testException(new IllegalSourcePathException("name", "message", t), t);
    }

    @Test
    public void testIllegalTargetPathException1() throws Exception {
        testException(new IllegalTargetPathException("name", "message"));
    }

    @Test
    public void testIllegalTargetPathException2() throws Exception {
        Throwable t = new Throwable();
        testException(new IllegalTargetPathException("name", "message", t), t);
    }

    @Test
    public void testIncompatibleVersionException1() throws Exception {
        testException(new IncompatibleVersionException("name", "message"));
    }

    @Test
    public void testIncompatibleVersionException2() throws Exception {
        Throwable t = new Throwable();
        testException(new IncompatibleVersionException("name", "message", t), t);
    }

    @Test
    public void testIncompleteJobDescriptionException1() throws Exception {
        testException(new IncompleteJobDescriptionException("name", "message"));
    }

    @Test
    public void testIncompleteJobDescriptionException2() throws Exception {
        Throwable t = new Throwable();
        testException(new IncompleteJobDescriptionException("name", "message", t), t);
    }

    @Test
    public void testInvalidCredentialException1() throws Exception {
        testException(new InvalidCredentialException("name", "message"));
    }

    @Test
    public void testInvalidCredentialException2() throws Exception {
        Throwable t = new Throwable();
        testException(new InvalidCredentialException("name", "message", t), t);
    }

    @Test
    public void testInvalidDataException1() throws Exception {
        testException(new InvalidResumeTargetException("name", "message"));
    }

    @Test
    public void testInvalidDataException2() throws Exception {
        Throwable t = new Throwable();
        testException(new InvalidResumeTargetException("name", "message", t), t);
    }

    @Test
    public void testInvalidJobDescriptionException1() throws Exception {
        testException(new InvalidJobDescriptionException("name", "message"));
    }

    @Test
    public void testInvalidJobDescriptionException2() throws Exception {
        Throwable t = new Throwable();
        testException(new InvalidJobDescriptionException("name", "message", t), t);
    }

    @Test
    public void testInvalidLocationException1() throws Exception {
        testException(new InvalidLocationException("name", "message"));
    }

    @Test
    public void testInvalidLocationException2() throws Exception {
        Throwable t = new Throwable();
        testException(new InvalidLocationException("name", "message", t), t);
    }

    @Test
    public void testInvalidOpenOptionsException1() throws Exception {
        testException(new InvalidOpenOptionsException("name", "message"));
    }

    @Test
    public void testInvalidOpenOptionsException2() throws Exception {
        Throwable t = new Throwable();
        testException(new InvalidOpenOptionsException("name", "message", t), t);
    }

    @Test
    public void testInvalidPropertyException1() throws Exception {
        testException(new InvalidPropertyException("name", "message"));
    }

    @Test
    public void testInvalidPropertyException2() throws Exception {
        Throwable t = new Throwable();
        testException(new InvalidPropertyException("name", "message", t), t);
    }

    @Test
    public void testNoSuchCopyException1() throws Exception {
        testException(new NoSuchCopyException("name", "message"));
    }

    @Test
    public void testNoSuchCopyException2() throws Exception {
        Throwable t = new Throwable();
        testException(new NoSuchCopyException("name", "message", t), t);
    }

    @Test
    public void testNoSuchFileException1() throws Exception {
        testException(new NoSuchPathException("name", "message"));
    }

    @Test
    public void testNoSuchFileException2() throws Exception {
        Throwable t = new Throwable();
        testException(new NoSuchPathException("name", "message", t), t);
    }

    @Test
    public void testNoSuchJobException1() throws Exception {
        testException(new NoSuchJobException("name", "message"));
    }

    @Test
    public void testNoSuchJobException2() throws Exception {
        Throwable t = new Throwable();
        testException(new NoSuchJobException("name", "message", t), t);
    }

    @Test
    public void testNoSuchXenonException1() throws Exception {
        testException(new NoSuchXenonException("name", "message"));
    }

    @Test
    public void testNoSuchXenonException2() throws Exception {
        Throwable t = new Throwable();
        testException(new NoSuchXenonException("name", "message", t), t);
    }

    @Test
    public void testNoSuchQueueException1() throws Exception {
        testException(new NoSuchQueueException("name", "message"));
    }

    @Test
    public void testNoSuchQueueException2() throws Exception {
        Throwable t = new Throwable();
        testException(new NoSuchQueueException("name", "message", t), t);
    }

    @Test
    public void testNoSuchSchedulerException1() throws Exception {
        testException(new NoSuchSchedulerException("name", "message"));
    }

    @Test
    public void testNoSuchSchedulerException2() throws Exception {
        Throwable t = new Throwable();
        testException(new NoSuchSchedulerException("name", "message", t), t);
    }

    @Test
    public void testNotConnectedException1() throws Exception {
        testException(new NotConnectedException("name", "message"));
    }

    @Test
    public void testNotConnectedException2() throws Exception {
        Throwable t = new Throwable();
        testException(new NotConnectedException("name", "message", t), t);
    }

    @Test
    public void testPermissionDeniedException1() throws Exception {
        testException(new PermissionDeniedException("name", "message"));
    }

    @Test
    public void testPermissionDeniedException2() throws Exception {
        Throwable t = new Throwable();
        testException(new PermissionDeniedException("name", "message", t), t);
    }

    @Test
    public void testUnknownPropertyException1() throws Exception {
        testException(new UnknownPropertyException("name", "message"));
    }

    @Test
    public void testUnknownPropertyException2() throws Exception {
        Throwable t = new Throwable();
        testException(new UnknownPropertyException("name", "message", t), t);
    }

    @Test
    public void testUnsupportedIOOperationException1() throws Exception {
        testException(new UnsupportedIOOperationException("name", "message"));
    }

    @Test
    public void testUnsupportedIOOperationException2() throws Exception {
        Throwable t = new Throwable();
        testException(new UnsupportedIOOperationException("name", "message", t), t);
    }

    @Test
    public void testUnsupportedJobDescriptionException1() throws Exception {
        testException(new UnsupportedJobDescriptionException("name", "message"));
    }

    @Test
    public void testUnsupportedJobDescriptionException2() throws Exception {
        Throwable t = new Throwable();
        testException(new UnsupportedJobDescriptionException("name", "message", t), t);
    }

    @Test
    public void testUnsupportedOperationException1() throws Exception {
        testException(new InvalidCopyOptionsException("name", "message"));
    }

    @Test
    public void testUnsupportedOperationException2() throws Exception {
        Throwable t = new Throwable();
        testException(new InvalidCopyOptionsException("name", "message", t), t);
    }

    @Test
    public void testJobCanceledException1() throws Exception {
        testException(new JobCanceledException("name", "message"));
    }

    @Test
    public void testJobCanceledException2() throws Exception {
        Throwable t = new Throwable();
        testException(new JobCanceledException("name", "message", t), t);
    }

    @Test
    public void testPropertyTypeException1() throws Exception {
        testException(new PropertyTypeException("name", "message"));
    }
    
    @Test
    public void testPropertyTypeException2() throws Exception {
        Throwable t = new Throwable();
        testException(new PropertyTypeException("name", "message", t), t);
    }

    
}
