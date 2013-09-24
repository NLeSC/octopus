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
package nl.esciencecenter.xenon.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;

import nl.esciencecenter.xenon.Xenon;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.XenonFactory;
import nl.esciencecenter.xenon.credentials.Credential;
import nl.esciencecenter.xenon.files.DirectoryStream;
import nl.esciencecenter.xenon.files.FileSystem;
import nl.esciencecenter.xenon.files.Files;
import nl.esciencecenter.xenon.files.OpenOption;
import nl.esciencecenter.xenon.files.Path;
import nl.esciencecenter.xenon.files.PathAttributesPair;
import nl.esciencecenter.xenon.files.RelativePath;
import nl.esciencecenter.xenon.util.Utils;

/**
 * Abstract FileSystem tests. This class runs a set of test scenarios on the (remote) filesystem. This is one abstract test class
 * which can be used for all FileSystem adaptors.
 * 
 * @author Piter T. de Boer
 */
abstract public class AbstractFileTests {

    /**
     * Singleton Engine for all tests
     */
    protected static Xenon xenon = null;

    protected static Files getFiles() throws XenonException {

        // class synchronization:
        synchronized (AbstractFileTests.class) {

            // init xenon singleton instance: 
            if (xenon == null) {
                xenon = XenonFactory.newXenon(null);
            }

            return xenon.files();
        }
    }

    // todo logging
    public static void debugPrintf(String format, Object... args) {
        System.out.printf("DEBUG:" + format, args);
    }

    // todo logging
    public static void infoPrintf(String format, Object... args) {
        System.out.printf("INFO:" + format, args);
    }

    // todo logging
    public static void errorPrintf(String format, Object... args) {
        System.err.printf("ERROR:" + format, args);
    }

    protected static int uniqueIdcounter = 1;

    // ========
    // Instance 
    // ======== 

    /**
     * The FileSystem instance to run integration tests on.
     */
    protected FileSystem fileSystem = null;

    /**
     * Get actual FileSystem implementation to run test on. Test this before other tests:
     */
    protected FileSystem getFileSystem() throws Exception {

        // Use singleton for all tests. Could create new Filesystem instance per test.  
        synchronized (this) {
            if (fileSystem == null) {
                URI uri = getTestLocation();
                fileSystem = getFiles().newFileSystem(uri.getScheme(), uri.getAuthority(), getCredentials(), null);
            }

            return fileSystem;
        }
    }

    /**
     * Return credentials for this FileSystem if needed for the integration tests.
     * 
     * @return Xenon Credential for the FileSystem to be tested.
     * @throws XenonException
     */
    abstract Credential getCredentials() throws XenonException;

    /**
     * Return test location. Subclasses need to override this.
     * 
     * @return the test location as URI
     */
    abstract public java.net.URI getTestLocation() throws Exception;

    // =========================================
    // Helper Methods for the integration tests.  
    // =========================================

    /**
     * Helper method to return current test dir. This directory must exist and must be writable.
     */
    protected Path getTestDir() throws Exception {
        FileSystem fs = getFileSystem();
        String testPath = this.getTestLocation().getPath();
        return getFiles().newPath(fs, new RelativePath(testPath));
    }

    protected Path createSubdir(Path parentDirPath, String subDir) throws XenonException {
        Path absPath = Utils.resolveWithRoot(xenon.files(), parentDirPath, subDir);
        infoPrintf("createSubdir: '%s' -> '%s'\n", subDir, absPath);
        getFiles().createDirectory(absPath);
        return absPath;
    }

    /**
     * Create new and unique sub-directory for testing purposes. To avoid previous failed tests to interfere with current test
     * run, an unique directory has to be created each time. It is recommend after each successful test run to delete the test
     * directory and its contents.
     * 
     * @param parentDirPath
     *            - parent directory to create (sub) directory in.
     * @param dirPrefix
     *            - prefix of the sub-directory. An unique number will be append to this name,
     * @return AbsolutPath of new created directory
     */
    protected Path createUniqueTestSubdir(Path parentDirPath, String dirPrefix) throws XenonException,
            XenonException {
        do {
            int myid = uniqueIdcounter++;
            Path absPath = Utils.resolveWithRoot(xenon.files(), parentDirPath, dirPrefix + "." + myid);

            if (getFiles().exists(absPath) == false) {
                infoPrintf("createUniqueTestSubdir: '%s'+%d => '%s'\n", dirPrefix, myid, absPath);
                getFiles().createDirectory(absPath);
                return absPath;
            }

        } while (true);
    }

    /**
     * Create new and unique test file.
     * 
     * @param parentDirPath
     *            - parent directory to create file into.
     * @param filePrefix
     *            - filePrefix to use as filename. An unique number will be added to the fileName.
     * @param createFile
     *            - actually create (empty) file on (remote) file system.
     * @return new Path, which points to existing file if createFile was true.
     * @throws XenonException
     * @throws XenonException
     */
    protected Path createUniqueTestFile(Path parentDirPath, String filePrefix, boolean createFile)
            throws XenonException {

        do {
            int myid = uniqueIdcounter++;
            Path absPath = Utils.resolveWithRoot(xenon.files(), parentDirPath, filePrefix + "." + myid);

            if (getFiles().exists(absPath) == false) {

                infoPrintf("createUniqueTestFile: '%s'+%d => '%s'\n", filePrefix, myid, absPath);
                if (createFile) {
                    getFiles().createFile(absPath);
                }
                return absPath;
            }

        } while (true);
    }

    protected Path createFile(Path parentDirPath, String subFile) throws XenonException {
        Path absPath = Utils.resolveWithRoot(xenon.files(), parentDirPath, subFile);
        getFiles().createFile(absPath);
        return absPath;
    }

    protected void deletePaths(Path[] paths, boolean assertDeletion) throws XenonException {

        for (Path path : paths) {
            getFiles().delete(path);
            if (assertDeletion)
                assertFalse("After Files().delete(), the path may not exist:" + path, getFiles().exists(path));
        }
    }

    // ========================
    // SetUp Integration Tests
    // ========================

    @org.junit.Before
    public void checkTestSetup() throws Exception {
        // Basic Sanity checks of the test environment; 
        // Typically Exceptions should be thrown here if the call fails. 
        URI uri = getTestLocation();
        assertNotNull("Setup: Can't do tests on a NULL location", uri);
        assertNotNull("Setup: The Files() interface is NULL", getFiles());
        assertNotNull("Setup: Actual FileSystem to run tests on is NULL", getFileSystem());
    }

    // ========================
    // Actual integration tests  
    // ========================

    @org.junit.Test
    public void testGetTestDir() throws Exception {
        Path path = getTestDir();
        assertNotNull("TestPath returned NULL", path);
        assertNotNull("Actual path element of Path may not be NULL", path);

        infoPrintf("Test location path scheme      =%s\n", path.getFileSystem().getScheme());
        infoPrintf("Test location path location    =%s\n", path.getFileSystem().getLocation());
        infoPrintf("Test location path          =%s\n", path.getRelativePath().getAbsolutePath());
        infoPrintf("Test location toString()    =%s\n", path.toString());
        infoPrintf("Test location getFileName() =%s\n", path.getRelativePath().getFileName());

        assertTrue("Root test location must exists (won't create here):" + path, getFiles().exists(path));
    }

    /**
     * Test creation and delete of file in one test. If this test fails, all other test will fail to!
     */
    @org.junit.Test
    public void testCreateDeleteEmptyFile() throws Exception {
        Path filePath = Utils.resolveWithRoot(xenon.files(), getTestDir(), "testFile01");

        Files files = getFiles();

        // Previous test run could have gone wrong. Indicate here that a previous test run failed. 
        boolean preExisting = files.exists(filePath);
        if (preExisting) {
            try {
                // try to delete first ! 
                files.delete(filePath);
            } catch (Exception e) {

            }
            assertFalse(
                    "exists(): Can't test createFile is previous test file already exists. File should now be deleted, please run test again.",
                    preExisting);
        }

        files.createFile(filePath);
        // enforce ? 
        boolean exists = files.exists(filePath);
        assertTrue("exist(): After createFile() exists() reports false.", exists);

        files.delete(filePath);
        assertTrue("delet(): After delete, method exist() return true.", exists);
    }

    /**
     * Test creation and deletion of directory in one test. If this test fails, all other test will fail to!
     */
    @org.junit.Test
    public void testCreateDeleteEmptySubdir() throws Exception {

        Files files = getFiles();

        Path dirPath = Utils.resolveWithRoot(xenon.files(), getTestDir(), "testSubdir01");
        assertFalse("Previous test directory already exists. Please clean test location.:" + dirPath,
                files.exists(dirPath));

        getFiles().createDirectory(dirPath);
        // test both ? 
        boolean exists = files.exists(dirPath);
        exists = files.exists(dirPath);
        assertTrue("After createDirectory(), method exists() reports false for path:" + dirPath, exists);

        assertDirIsEmpty(files, dirPath);

        files.delete(dirPath);
        exists = files.exists(dirPath);
        assertFalse("After delete() on directory, method exists() reports false.", exists);
    }

    public void assertDirIsEmpty(Files files, Path dirPath) throws Exception {

        DirectoryStream<Path> dirStream = files.newDirectoryStream(dirPath);
        Iterator<Path> iterator = dirStream.iterator();
        assertFalse("Method hasNext() from empty directory iterator must return false.", iterator.hasNext());
    }

    @org.junit.Test
    public void testgetFileSystemEntryPath() throws Exception {

        FileSystem fs = getFileSystem();

        // just test whether it works: 
        Path relEntryPath = fs.getEntryPath();
        assertNotNull("Entry Path may not be null.", relEntryPath);
    }

    @org.junit.Test
    public void testResolveRootPath() throws Exception {

        FileSystem fs = getFileSystem();

        // resolve "/", for current filesystems this must equal to "/" ? 
        Path rootPath = getFiles().newPath(fs, new RelativePath("/"));
        assertEquals("Absolute path of resolved path '/' must equal to '/'.", "/", rootPath);
    }

    @org.junit.Test
    public void testNewDirectoryStreamTestDir() throws Exception {

        Path path = getTestDir();
        DirectoryStream<Path> dirStream = getFiles().newDirectoryStream(path);
        Iterator<Path> iterator = dirStream.iterator();

        // Just test whether it works and directory is readable (other tests will fail if this doesn't work). 
        while (iterator.hasNext()) {
            Path pathEl = iterator.next();
            infoPrintf(" -(Path)Path     =%s:'%s'\n", pathEl.getFileSystem().getScheme(), pathEl);
            infoPrintf(" -(Path)getPath()=%s:'%s'\n", pathEl.getFileSystem().getLocation(), pathEl);
        }
    }

    @org.junit.Test
    public void testNewDirectoryAttributesStreamTestDir() throws Exception {

        Path path = getTestDir();
        DirectoryStream<PathAttributesPair> dirStream = getFiles().newAttributesDirectoryStream(path);
        Iterator<PathAttributesPair> iterator = dirStream.iterator();

        // Just test whether it works and directory is readable (other tests will fail if this doesn't work). 
        while (iterator.hasNext()) {
            PathAttributesPair pathEl = iterator.next();
            infoPrintf(" -(PathAttributesPair)path='%s'\n", pathEl.path());
        }
    }

    @org.junit.Test
    public void testCreateListAndDelete3Subdirs() throws Exception {

        // PRE:
        Files files = getFiles();
        Path testDirPath = createUniqueTestSubdir(getTestDir(), "testSubdir3");
        assertDirIsEmpty(files, testDirPath);

        // TEST: 
        Path dir1 = createSubdir(testDirPath, "subDir1");
        Path dir2 = createSubdir(testDirPath, "subDir2");
        Path dir3 = createSubdir(testDirPath, "subDir3");

        DirectoryStream<Path> dirStream = getFiles().newDirectoryStream(testDirPath);
        Iterator<Path> iterator = dirStream.iterator();

        int count = 0;

        while (iterator.hasNext()) {
            Path pathEl = iterator.next();
            infoPrintf(" -(Path)Path     =%s:'%s'\n", pathEl.getFileSystem().getScheme(), pathEl);
            infoPrintf(" -(Path)getPath()=%s:'%s'\n", pathEl.getFileSystem().getLocation(), pathEl);
            count++;
        }

        infoPrintf("Directory has:%d entries\n", count);
        assertEquals("Directory must have 3 sub directories\n", 3, count);

        // POST: 
        deletePaths(new Path[] { dir1, dir2, dir3, testDirPath }, true);

    }

    @org.junit.Test
    public void testCreateListAndDelete3FilesWithAttributes() throws Exception {

        // PRE: 
        Files files = getFiles();
        Path testDirPath = createUniqueTestSubdir(getTestDir(), "testSubdir4");
        assertDirIsEmpty(files, testDirPath);

        // TEST: 
        Path file1 = createFile(testDirPath, "file1");
        Path file2 = createFile(testDirPath, "file2");
        Path file3 = createFile(testDirPath, "file3");

        DirectoryStream<PathAttributesPair> dirStream = getFiles().newAttributesDirectoryStream(testDirPath);
        Iterator<PathAttributesPair> iterator = dirStream.iterator();

        // Regression test: this has failed before. Issue #91
        int count = 0;

        while (iterator.hasNext()) {
            PathAttributesPair el = iterator.next();
            Path path = el.path();
            infoPrintf(" -(Path)Path     =%s:'%s'\n", path.getFileSystem().getScheme(), path);
            infoPrintf(" -(Path)getPath()=%s:'%s'\n", path.getFileSystem().getLocation(), path);
            count++;
        }

        infoPrintf("Directory has:%d entries\n", count);
        assertEquals("Directory must have 3 file entries\n", 3, count);

        // POST: 
        deletePaths(new Path[] { file1, file2, file3, testDirPath }, true);
    }

    @org.junit.Test
    public void testCreateListAndDelete3SubdirsWithAttributes() throws Exception {

        // PRE:
        Files files = getFiles();
        Path testDirPath = createUniqueTestSubdir(getTestDir(), "testSubdir5");
        assertDirIsEmpty(files, testDirPath);

        // TEST: 
        Path dir1 = createSubdir(testDirPath, "subDir1");
        Path dir2 = createSubdir(testDirPath, "subDir2");
        Path dir3 = createSubdir(testDirPath, "subDir3");

        DirectoryStream<PathAttributesPair> dirStream = getFiles().newAttributesDirectoryStream(testDirPath);
        Iterator<PathAttributesPair> iterator = dirStream.iterator();

        int count = 0;

        while (iterator.hasNext()) {
            PathAttributesPair el = iterator.next();
            Path path = el.path();
            infoPrintf(" -(Path)Path     =%s:'%s'\n", path.getFileSystem().getScheme(), path);
            infoPrintf(" -(Path)getPath()=%s:'%s'\n", path.getFileSystem().getLocation(), path);
            count++;
        }

        infoPrintf("Directory has:%d entries\n", count);
        assertEquals("Directory must have 3 sub directories\n", 3, count);

        // POST: 
        deletePaths(new Path[] { dir1, dir2, dir3, testDirPath }, true);
    }

    // ===================================
    // Test Stream Read and Write Methods 
    // ===================================

    /**
     * Test write and read back 0 bytes.
     */
    @org.junit.Test
    public void testStreamWriteAndReadNillBytes() throws Exception {

        // empty array: 
        byte nilBytes[] = new byte[0];
        testStreamWriteAndReadBytes(nilBytes, 0);
    }

    /**
     * Test write and read back 256 bytes to a NEW file.
     */
    @org.junit.Test
    public void testStreamWriteAndRead256Bytes() throws Exception {

        // one byte: 
        byte oneByte[] = new byte[1];
        oneByte[0] = 13;
        testStreamWriteAndReadBytes(oneByte, 1);

        // 256 bytes 
        int n = 256;
        byte bytes[] = new byte[n];

        for (int i = 0; i < n; i++) {
            bytes[i] = (byte) (n % 256);
        }
        testStreamWriteAndReadBytes(bytes, 256);
    }

    /**
     * Helper method to write a series of bytes.
     */
    protected void testStreamWriteAndReadBytes(byte bytes[], int numBytes) throws Exception {

        // PRE: 
        Path testFilePath = createUniqueTestFile(getTestDir(), "testStreaReadWriteFile03", true);
        assertTrue("Test file doesn't exists:" + testFilePath, getFiles().exists(testFilePath));

        // TEST: 
        java.io.OutputStream outps = getFiles().newOutputStream(testFilePath, OpenOption.CREATE);

        outps.write(bytes);

        try {
            outps.close();
        } catch (IOException e) {
            debugPrintf("IOException when closing test file:%s\n", e);
        }

        InputStream inps = getFiles().newInputStream(testFilePath);

        byte readBytes[] = new byte[numBytes];
        int totalRead = 0;

        while (totalRead < numBytes) { // read loop: 

            int numRead = inps.read(readBytes, totalRead, (numBytes - totalRead));
            if (numRead >= 0) {
                totalRead += numRead;
            } else {
                throw new IOException("Got EOF when reading from testFile:" + testFilePath);
            }
        }

        // readBytes[100]=13; // test fault insertion here. 

        for (int i = 0; i < numBytes; i++) {
            assertEquals("Byte at #" + i + " does not equal orginal value.", bytes[i], readBytes[i]);
        }

        try {
            inps.close();
        } catch (IOException e) {
            debugPrintf("IOException when closing test file:%s\n", e);
        }

        // POST: 
        deletePaths(new Path[] { testFilePath }, true);
    }

}
