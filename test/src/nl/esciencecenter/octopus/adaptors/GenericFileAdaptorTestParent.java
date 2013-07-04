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

package nl.esciencecenter.octopus.adaptors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
//import java.nio.ByteBuffer;
//import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import nl.esciencecenter.octopus.Octopus;
import nl.esciencecenter.octopus.OctopusFactory;
import nl.esciencecenter.octopus.credentials.Credential;
import nl.esciencecenter.octopus.credentials.Credentials;
import nl.esciencecenter.octopus.engine.files.PathAttributesPairImplementation;
import nl.esciencecenter.octopus.files.AbsolutePath;
import nl.esciencecenter.octopus.files.Copy;
import nl.esciencecenter.octopus.files.CopyOption;
import nl.esciencecenter.octopus.files.CopyStatus;
import nl.esciencecenter.octopus.files.DirectoryStream;
import nl.esciencecenter.octopus.files.FileAttributes;
import nl.esciencecenter.octopus.files.FileSystem;
import nl.esciencecenter.octopus.files.Files;
import nl.esciencecenter.octopus.files.OpenOption;
import nl.esciencecenter.octopus.files.PathAttributesPair;
import nl.esciencecenter.octopus.files.PosixFilePermission;
import nl.esciencecenter.octopus.files.RelativePath;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * @author Jason Maassen <J.Maassen@esciencecenter.nl>
 * 
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class GenericFileAdaptorTestParent {

    protected static String TEST_ROOT;

    public static FileTestConfig config;

    protected Octopus octopus;
    protected Files files;
    protected Credentials credentials;

    protected AbsolutePath testDir;

    private long counter = 0;

    // MUST be invoked by a @BeforeClass method of the subclass! 
    public static void prepareClass(FileTestConfig testConfig) throws Exception {
        config = testConfig;
        TEST_ROOT = "octopus_test_" + config.getAdaptorName() + "_" + System.currentTimeMillis();
    }

    // MUST be invoked by a @AfterClass method of the subclass! 
    public static void cleanupClass() throws Exception {

        System.err.println("GenericFileAdaptorTest.cleanupClass() attempting to remove: " + TEST_ROOT);

        Octopus octopus = OctopusFactory.newOctopus(null);

        Files files = octopus.files();
        Credentials credentials = octopus.credentials();

        FileSystem filesystem = config.getTestFileSystem(files, credentials);

        AbsolutePath root = filesystem.getEntryPath().resolve(new RelativePath(TEST_ROOT));

        if (files.exists(root)) {
            files.delete(root);
        }

        OctopusFactory.endOctopus(octopus);
    }

    protected void prepare() throws Exception {
        octopus = OctopusFactory.newOctopus(null);
        files = octopus.files();
        credentials = octopus.credentials();
    }

    protected void cleanup() throws Exception {
        OctopusFactory.endOctopus(octopus);
        files = null;
        octopus = null;
    }

    // Various util functions ------------------------------------------------------------

    class AllTrue implements DirectoryStream.Filter {
        @Override
        public boolean accept(AbsolutePath entry) {
            return true;
        }
    }

    class AllFalse implements DirectoryStream.Filter {
        @Override
        public boolean accept(AbsolutePath entry) {
            return false;
        }
    }

    class Select implements DirectoryStream.Filter {

        private Set<AbsolutePath> set;

        public Select(Set<AbsolutePath> set) {
            this.set = set;
        }

        @Override
        public boolean accept(AbsolutePath entry) {
            return set.contains(entry);
        }
    }

    private void throwUnexpected(String name, Exception e) throws Exception {
        cleanup();
        throw new Exception(name + " throws unexpected Exception!", e);
    }

    private void throwExpected(String name) throws Exception {
        cleanup();
        throw new Exception(name + " did NOT throw Exception which was expected!");
    }

    private void throwWrong(String name, String expected, String result) throws Exception {
        cleanup();
        throw new Exception(name + " produced wrong result! Expected: " + expected + " but got: " + result);
    }

    private void throwUnexpectedElement(String name, String element) throws Exception {
        cleanup();
        throw new Exception(name + " produced unexpected element: " + element);
    }

    //    private void throwMissingElement(String name, String element) throws Exception { 
    //        cleanup();
    //        throw new Exception(name + " did NOT produce element: " + element);        
    //    }

    private void throwMissingElements(String name, Collection elements) throws Exception {
        cleanup();
        throw new Exception(name + " did NOT produce elements: " + elements);
    }

    private void close(Closeable c) {

        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (Exception e) {
            // ignore
        }
    }

    // Depends on: AbsolutePath.resolve, RelativePath, exists
    private AbsolutePath createNewTestDirName(AbsolutePath root) throws Exception {

        AbsolutePath dir = root.resolve(new RelativePath("dir" + counter));
        counter++;

        if (files.exists(dir)) {
            throw new Exception("Generated test dir already exists! " + dir.getPath());
        }

        return dir;
    }

    // Depends on: [createNewTestDirName], createDirectory, exists
    private AbsolutePath createTestDir(AbsolutePath root) throws Exception {

        AbsolutePath dir = createNewTestDirName(root);

        files.createDirectory(dir);

        if (!files.exists(dir)) {
            throw new Exception("Failed to generate test dir! " + dir.getPath());
        }

        return dir;
    }

    // Depends on: [createTestDir]
    private void prepareTestDir(FileSystem fs, String testName) throws Exception {

        if (testDir != null) {
            return;
        }

        AbsolutePath entry = fs.getEntryPath();
        testDir = entry.resolve(new RelativePath(TEST_ROOT, testName));

        if (!files.exists(testDir)) {
            files.createDirectories(testDir);
        }
    }

    // Depends on: AbsolutePath.resolve, RelativePath, exists 
    private AbsolutePath createNewTestFileName(AbsolutePath root) throws Exception {

        AbsolutePath file = root.resolve(new RelativePath("file" + counter));
        counter++;

        if (files.exists(file)) {
            throw new Exception("Generated NEW test file already exists! " + file.getPath());
        }

        return file;
    }

    // Depends on: newOutputStream
    private void writeData(AbsolutePath testFile, byte[] data) throws Exception {

        OutputStream out = files.newOutputStream(testFile, OpenOption.OPEN, OpenOption.TRUNCATE, OpenOption.WRITE);
        if (data != null) {
            out.write(data);
        }
        out.close();
    }

    // Depends on: [createNewTestFileName], createFile, [writeData]
    private AbsolutePath createTestFile(AbsolutePath root, byte[] data) throws Exception {

        AbsolutePath file = createNewTestFileName(root);

        files.createFile(file);

        if (data != null && data.length > 0) {
            writeData(file, data);
        }

        return file;
    }

    // Depends on: exists, isDirectory, delete
    private void deleteTestFile(AbsolutePath file) throws Exception {

        if (!files.exists(file)) {
            throw new Exception("Cannot delete non-existing file: " + file);
        }

        if (files.isDirectory(file)) {
            throw new Exception("Cannot delete directory: " + file);
        }

        files.delete(file);
    }

    // Depends on: exists, isDirectory, delete
    private void deleteTestDir(AbsolutePath dir) throws Exception {

        if (!files.exists(dir)) {
            throw new Exception("Cannot delete non-existing dir: " + dir);
        }

        if (!files.isDirectory(dir)) {
            throw new Exception("Cannot delete file: " + dir);
        }

        files.delete(dir);
    }

    private byte[] readFully(InputStream in) throws Exception {

        byte[] buffer = new byte[1024];

        int offset = 0;
        int read = in.read(buffer, offset, buffer.length - offset);

        while (read != -1) {

            offset += read;

            if (offset == buffer.length) {
                buffer = Arrays.copyOf(buffer, buffer.length * 2);
            }

            read = in.read(buffer, offset, buffer.length - offset);
        }

        close(in);

        return Arrays.copyOf(buffer, offset);
    }

    //    private byte [] readFully(SeekableByteChannel channel) throws Exception { 
    //        
    //        ByteBuffer buffer = ByteBuffer.allocate(1024);
    //        
    //        int read = channel.read(buffer);
    //        
    //        while (read != -1) { 
    //            
    //            System.err.println("READ from channel " + read);
    //            
    //            if (buffer.position() == buffer.limit()) {                 
    //                ByteBuffer tmp = ByteBuffer.allocate(buffer.limit()*2);
    //                buffer.flip();
    //                tmp.put(buffer);
    //                buffer = tmp;
    //            }
    //                        
    //            read = channel.read(buffer);
    //        }
    //        
    //        close(channel);
    //        
    //        buffer.flip();
    //        byte [] tmp = new byte[buffer.remaining()];
    //        buffer.get(tmp);
    //        
    //        System.err.println("Returning byte[" + tmp.length + "]");
    //        
    //        return tmp;  
    //    }

    // The test start here.

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST newFileSystem 
    //
    // Possible parameters: 
    //   URI         - correct URI / wrong user / wrong location / wrong path
    //   Credentials - default / null / value
    //   Properties  - null / empty / set right / set wrong
    // 
    // Total combinations: 4 + 2 + 3 = 9
    // 
    // Depends on: newFileSystem, close

    private void test00_newFileSystem(URI uri, Credential c, Properties p, boolean mustFail) throws Exception {

        try {
            FileSystem fs = files.newFileSystem(uri, c, p);
            files.close(fs);
        } catch (Exception e) {
            if (mustFail) {
                // exception was expected.
                return;
            }

            // exception was not expected
            throwUnexpected("test00_newFileSystem", e);
        }

        if (mustFail) {
            // expected an exception!
            throwExpected("test00_newFileSystem");
        }
    }

    @org.junit.Test
    public void test00_newFileSystem() throws Exception {

        prepare();

        // test with null URI and null credentials
        test00_newFileSystem(null, null, null, true);

        // test with correct URI with default credential and without properties
        test00_newFileSystem(config.getCorrectURI(), config.getDefaultCredential(credentials), null, false);

        // test with correct URI with default credential and without properties
        test00_newFileSystem(config.getCorrectURIWithPath(), config.getDefaultCredential(credentials), null, false);

        // test with correct URI with default credential and without properties
        test00_newFileSystem(config.getCorrectURIWithPath(), config.getDefaultCredential(credentials), null, false);

        // test with wrong URI user with default credential and without properties
        if (config.supportURIUser()) {
            test00_newFileSystem(config.getURIWrongUser(), config.getDefaultCredential(credentials), null, true);
        }

        // test with wrong URI location with default credential and without properties
        if (config.supportURILocation()) {
            test00_newFileSystem(config.getURIWrongLocation(), config.getDefaultCredential(credentials), null, true);
        }

        // test with wrong URI path with default credential and without properties
        test00_newFileSystem(config.getURIWrongPath(), config.getDefaultCredential(credentials), null, true);

        // test with correct URI without credential and without properties
        boolean allowNull = config.supportNullCredential();
        test00_newFileSystem(config.getCorrectURI(), null, null, !allowNull);

        // test with correct URI with non-default credential and without properties
        if (config.supportNonDefaultCredential()) {
            test00_newFileSystem(config.getCorrectURI(), config.getNonDefaultCredential(credentials), null, false);
        }

        // test with correct URI with default credential and with empty properties
        test00_newFileSystem(config.getCorrectURI(), config.getDefaultCredential(credentials), new Properties(), false);

        // test with correct URI with default credential and with correct properties
        if (config.supportsProperties()) {
            test00_newFileSystem(config.getCorrectURI(), config.getDefaultCredential(credentials), config.getCorrectProperties(),
                    false);

            // test with correct URI with default credential and with wrong properties
            // test00_newFileSystem(config.getCorrectURI(), config.getDefaultCredential(credentials), getIncorrectProperties(), 
            // true);
        }

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST isOpen
    // 
    // Possible parameters: 
    // 
    // FileSystem - null / open FS / closed FS
    // 
    // Total combinations : 3
    // 
    // Depends on: [getTestFileSystem], close, isOpen

    private void test01_isOpen(FileSystem fs, boolean expected, boolean mustFail) throws Exception {

        boolean result = false;

        try {
            result = files.isOpen(fs);
        } catch (Exception e) {
            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test01_isOpen", e);
        }

        if (mustFail) {
            throwExpected("test01_isOpen");
        }

        if (result != expected) {
            throwWrong("test01_isOpen", "" + expected, "" + result);
        }
    }

    @org.junit.Test
    public void test01_isOpen() throws Exception {

        prepare();

        // test with null filesystem 
        test01_isOpen(null, false, true);

        FileSystem fs = config.getTestFileSystem(files, credentials);

        // test with correct open filesystem
        test01_isOpen(fs, true, false);

        if (config.supportsClose()) {
            files.close(fs);

            // test with correct closed filesystem
            test01_isOpen(fs, false, false);
        }

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST close
    // 
    // Possible parameters: 
    // 
    // FileSystem - null / open FS / closed FS
    // 
    // Total combinations : 3
    // 
    // Depends on: [getTestFileSystem], close

    private void test02_close(FileSystem fs, boolean mustFail) throws Exception {

        try {
            files.close(fs);
        } catch (Exception e) {
            if (mustFail) {
                // expected
                return;
            }
            throwUnexpected("test02_close", e);
        }

        if (mustFail) {
            throwExpected("test02_close");
        }
    }

    @org.junit.Test
    public void test02_close() throws Exception {

        prepare();

        // test with null filesystem 
        test02_close(null, true);

        if (config.supportsClose()) {

            FileSystem fs = config.getTestFileSystem(files, credentials);

            // test with correct open filesystem
            test02_close(fs, false);

            // test with correct closed filesystem
            test02_close(fs, true);
        }

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST newPath
    // 
    // Possible parameters: 
    //
    // FileSystem - null / correct 
    // RelativePath - null / empty / value
    //
    // Total combinations : 2
    // 
    // Depends on: [getTestFileSystem], FileSystem.getEntryPath(), AbsolutePath.getPath(), RelativePath, close

    private void test03_newPath(FileSystem fs, RelativePath path, String expected, boolean mustFail) throws Exception {

        String result = null;

        try {
            result = files.newPath(fs, path).getPath();
        } catch (Exception e) {
            if (mustFail) {
                // expected exception
                return;
            }

            throwUnexpected("test03_newPath", e);
        }

        if (mustFail) {
            throwExpected("test03_newPath");
        }

        if (!result.equals(expected)) {
            throwWrong("test03_newPath", expected, result);
        }
    }

    @org.junit.Test
    public void test03_newPath() throws Exception {

        prepare();

        FileSystem fs = config.getTestFileSystem(files, credentials);
        String root = "/";

        // test with null filesystem and null relative path 
        test03_newPath(null, null, null, true);

        // test with correct filesystem and null relative path 
        test03_newPath(fs, null, null, true);

        // test with correct filesystem and empty relative path 
        test03_newPath(fs, new RelativePath(), root, false);

        // test with correct filesystem and relativepath with value
        test03_newPath(fs, new RelativePath("test"), root + "test", false);

        files.close(fs);

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: createDirectory
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing dir / existing dir / existing file / non-exising parent / closed filesystem    
    // 
    // Total combinations : 5
    // 
    // Depends on: [getTestFileSystem], FileSystem.getEntryPath(), [createNewTestDirName], [createTestFile], 
    //             createDirectory, [deleteTestDir], [deleteTestFile], [closeTestFileSystem]

    private void test04_createDirectory(AbsolutePath path, boolean mustFail) throws Exception {

        try {
            files.createDirectory(path);
        } catch (Exception e) {

            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test04_createDirectory", e);
        }

        if (mustFail) {
            throwExpected("test04_createDirectory");
        }
    }

    @org.junit.Test
    public void test04_createDirectory() throws Exception {

        prepare();

        // test with null
        test04_createDirectory(null, true);

        FileSystem fs = config.getTestFileSystem(files, credentials);

        AbsolutePath entry = fs.getEntryPath();
        AbsolutePath root = entry.resolve(new RelativePath(TEST_ROOT));

        // test with non-existing dir
        test04_createDirectory(root, false);

        // test with existing dir
        test04_createDirectory(root, true);

        // test with existing file 
        AbsolutePath file0 = createTestFile(root, null);
        test04_createDirectory(file0, true);
        deleteTestFile(file0);

        // test with non-existent parent dir
        AbsolutePath parent = createNewTestDirName(root);
        AbsolutePath dir0 = createNewTestDirName(parent);
        test04_createDirectory(dir0, true);

        // cleanup 
        deleteTestDir(root);

        if (config.supportsClose()) {
            // test with closed fs
            config.closeTestFileSystem(files, fs);
            test04_createDirectory(root, true);
        }

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: createDirectories
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing dir / existing dir / dir with existing parents / dir with non existing parents / 
    //               dir where last parent is file / closed filesystem    
    // 
    // Total combinations : 7
    // 
    // Depends on: [getTestFileSystem], FileSystem.getEntryPath(), [createNewTestDirName], createDirectories, 
    //             [deleteTestDir], [createTestFile], [deleteTestFile], [deleteTestDir], [closeTestFileSystem]

    private void test05_createDirectories(AbsolutePath path, boolean mustFail) throws Exception {

        try {
            files.createDirectories(path);

            assert (files.exists(path));
            assert (files.isDirectory(path));

        } catch (Exception e) {

            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test05_createDirectories", e);
        }

        if (mustFail) {
            throwExpected("createDirectory");
        }
    }

    @org.junit.Test
    public void test05_createDirectories() throws Exception {

        prepare();

        // test with null
        test05_createDirectories(null, true);

        FileSystem fs = config.getTestFileSystem(files, credentials);

        AbsolutePath entry = fs.getEntryPath();
        AbsolutePath root = entry.resolve(new RelativePath(TEST_ROOT, "test05_createDirectories"));

        // test with non-existing dir
        test05_createDirectories(root, false);

        // test with existing dir
        test05_createDirectories(root, true);

        // dir with existing parents 
        AbsolutePath dir0 = createNewTestDirName(root);
        test05_createDirectories(dir0, false);
        deleteTestDir(dir0);

        // dir with non-existing parents 
        AbsolutePath dir1 = createNewTestDirName(dir0);
        test05_createDirectories(dir1, false);

        // dir where last parent is file 
        AbsolutePath file0 = createTestFile(dir0, null);
        AbsolutePath dir2 = createNewTestDirName(file0);
        test05_createDirectories(dir2, true);

        // cleanup 
        deleteTestDir(dir1);
        deleteTestFile(file0);
        deleteTestDir(dir0);
        deleteTestDir(root);

        if (config.supportsClose()) {
            // test with closed fs
            config.closeTestFileSystem(files, fs);
            test05_createDirectories(root, true);
        }

        cleanup();
    }

    // From this point on we can use prepareTestDir 

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: isDirectory
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing file / existing file / existing dir / closed filesystem
    // 
    // Total combinations : 4
    // 
    // Depends on: [getTestFileSystem], [createTestDir], [createNewTestFileName], [createTestFile], [deleteTestFile] 
    //             [closeTestFileSystem]

    private void test06_isDirectory(AbsolutePath path, boolean expected, boolean mustFail) throws Exception {

        boolean result = false;

        try {
            result = files.isDirectory(path);
        } catch (Exception e) {

            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test06_isDirectory", e);
        }

        if (mustFail) {
            throwExpected("test06_isDirectory");
        }

        if (result != expected) {
            throwWrong("test06_isDirectory", "" + expected, "" + result);
        }
    }

    @org.junit.Test
    public void test06_isDirectory() throws Exception {

        prepare();

        // prepare
        FileSystem fs = config.getTestFileSystem(files, credentials);
        prepareTestDir(fs, "test06_isDirectory");

        // test with null        
        test06_isDirectory(null, false, true);

        // test with non-existing file
        AbsolutePath file0 = createNewTestFileName(testDir);
        test06_isDirectory(file0, false, false);

        // test with existing file
        AbsolutePath file1 = createTestFile(testDir, null);
        test06_isDirectory(file1, false, false);
        deleteTestFile(file1);

        // test with existing dir
        test06_isDirectory(testDir, true, false);

        // cleanup        
        deleteTestDir(testDir);
        config.closeTestFileSystem(files, fs);

        if (config.supportsClose()) {
            // test with closed filesystem
            test06_isDirectory(testDir, true, true);
        }

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: createFile
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing file / existing file / existing dir / non-existing parent / closed filesystem    
    // 
    // Total combinations : 6
    // 
    // Depends on: [getTestFileSystem], [createTestDir], [createNewTestFileName], createFile, delete, [deleteTestDir] 
    //             [closeTestFileSystem]

    private void test07_createFile(AbsolutePath path, boolean mustFail) throws Exception {

        try {
            files.createFile(path);
        } catch (Exception e) {

            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test07_createFile", e);
        }

        if (mustFail) {
            throwExpected("test07_createFile");
        }
    }

    @org.junit.Test
    public void test07_createFile() throws Exception {

        prepare();

        // prepare
        FileSystem fs = config.getTestFileSystem(files, credentials);
        prepareTestDir(fs, "test07_createFile");

        // test with null        
        test07_createFile(null, true);

        // test with non-existing file
        AbsolutePath file0 = createNewTestFileName(testDir);
        test07_createFile(file0, false);

        // test with existing file
        test07_createFile(file0, true);

        // test with existing dir
        test07_createFile(testDir, true);

        AbsolutePath tmp = createNewTestDirName(testDir);
        AbsolutePath file1 = createNewTestFileName(tmp);

        // test with non-existing parent
        test07_createFile(file1, true);

        // cleanup 
        files.delete(file0);
        deleteTestDir(testDir);
        config.closeTestFileSystem(files, fs);

        if (config.supportsClose()) {
            // test with closed filesystem
            test07_createFile(file0, true);
        }

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: exists
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing file / existing file   
    // 
    // Total combinations : 3 
    // 
    // Depends on: [getTestFileSystem], [createTestDir], [createNewTestFileName], [createTestFile], [deleteTestFile], 
    //             [closeTestFileSystem], exists  

    private void test08_exists(AbsolutePath path, boolean expected, boolean mustFail) throws Exception {

        boolean result = false;

        try {
            result = files.exists(path);
        } catch (Exception e) {

            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test08_exists", e);
        }

        if (mustFail) {
            throwExpected("test08_exists");
        }

        if (result != expected) {
            throwWrong("test08_exists", "" + expected, "" + result);
        }
    }

    @org.junit.Test
    public void test08_exists() throws Exception {

        prepare();

        // prepare
        FileSystem fs = config.getTestFileSystem(files, credentials);
        prepareTestDir(fs, "test08_exists");

        // test with null
        test08_exists(null, false, true);

        // test with non-existing file
        AbsolutePath file0 = createNewTestFileName(testDir);
        test08_exists(file0, false, false);

        // test with existing file
        AbsolutePath file1 = createTestFile(testDir, null);
        test08_exists(file1, true, false);
        deleteTestFile(file1);

        // cleanup
        deleteTestDir(testDir);
        config.closeTestFileSystem(files, fs);

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: delete
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing file / existing file / existing empty dir / existing non-empty dir / 
    //              existing non-writable file / closed filesystem    
    // 
    // Total combinations : 7
    // 
    // Depends on: [getTestFileSystem], [createTestDir], [createNewTestFileName], delete, [deleteTestFile], [deleteTestDir] 
    //             [closeTestFileSystem]

    private void test09_delete(AbsolutePath path, boolean mustFail) throws Exception {

        try {
            files.delete(path);
        } catch (Exception e) {

            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test09_delete", e);
        }

        if (files.exists(path)) {
            throwWrong("test09_delete", "no file", "a file");
        }

        if (mustFail) {
            throwExpected("test09_delete");
        }
    }

    @org.junit.Test
    public void test09_delete() throws Exception {

        prepare();

        // test with null
        test09_delete(null, true);

        FileSystem fs = config.getTestFileSystem(files, credentials);
        prepareTestDir(fs, "test09_delete");

        // test with non-existing file
        AbsolutePath file0 = createNewTestFileName(testDir);
        test09_delete(file0, true);

        // test with existing file
        AbsolutePath file1 = createTestFile(testDir, null);
        test09_delete(file1, false);

        // test with existing empty dir 
        AbsolutePath dir0 = createTestDir(testDir);
        test09_delete(dir0, false);

        // test with existing non-empty dir
        AbsolutePath dir1 = createTestDir(testDir);
        AbsolutePath file2 = createTestFile(dir1, null);
        test09_delete(dir1, true);

        // test with non-writable file 
        //        AbsolutePath file3 = createTestFile(testDir, null);
        //        files.setPosixFilePermissions(file3, new HashSet<PosixFilePermission>());

        //      System.err.println("Attempting to delete: " + file3.getPath() + " " + files.getAttributes(file3));

        //        test09_delete(file3, true);

        // cleanup
        deleteTestFile(file2);
        deleteTestDir(dir1);
        deleteTestDir(testDir);

        if (config.supportsClose()) {
            // test with closed fs
            config.closeTestFileSystem(files, fs);
            test09_delete(testDir, true);
        }

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: size
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing file / existing file size 0 / existing file size N / file from closed FS  
    // 
    // Total combinations : 5
    // 
    // Depends on: [getTestFileSystem], [createTestDir], [createNewTestFileName], [createTestFile], [deleteTestFile], 
    //             [deleteTestDir], [closeTestFileSystem], size, close  

    private void test10_size(AbsolutePath path, long expected, boolean mustFail) throws Exception {

        long result = -1;

        try {
            result = files.size(path);
        } catch (Exception e) {

            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test10_size", e);
        }

        if (mustFail) {
            throwExpected("test10_size");
        }

        if (result != expected) {
            throwWrong("test10_size", "" + expected, "" + result);
        }
    }

    @org.junit.Test
    public void test10_size() throws Exception {

        prepare();

        // test with null parameter 
        test10_size(null, -1, true);

        FileSystem fs = config.getTestFileSystem(files, credentials);
        prepareTestDir(fs, "test10_size");

        // test with non existing file
        AbsolutePath file1 = createNewTestFileName(testDir);
        test10_size(file1, -1, true);

        // test with existing empty file
        AbsolutePath file2 = createTestFile(testDir, new byte[0]);
        test10_size(file2, 0, false);
        deleteTestFile(file2);

        // test with existing filled file
        AbsolutePath file3 = createTestFile(testDir, new byte[13]);
        test10_size(file3, 13, false);
        deleteTestFile(file3);

        // test with dir
        AbsolutePath dir0 = createTestDir(testDir);
        test10_size(dir0, 0, false);
        deleteTestDir(dir0);
        deleteTestDir(testDir);

        // test with closed filesystem
        if (config.supportsClose()) {
            config.closeTestFileSystem(files, fs);
            test10_size(file1, 0, true);
        }

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: newDirectoryStream 
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing dir / existing empty dir / existing non-empty dir / existing dir with subdirs / 
    //              existing file / closed filesystem    
    // 
    // Total combinations : 7
    // 
    // Depends on: [getTestFileSystem], [createTestDir], [createNewTestDirName], [createTestFile], newDirectoryStream,   
    //             [deleteTestDir], , [deleteTestFile], [deleteTestDir], [closeTestFileSystem]

    private void test11_newDirectoryStream(AbsolutePath root, Set<AbsolutePath> expected, boolean mustFail) throws Exception {

        Set<AbsolutePath> tmp = new HashSet<AbsolutePath>();

        if (expected != null) {
            tmp.addAll(expected);
        }

        DirectoryStream<AbsolutePath> in = null;

        try {
            in = files.newDirectoryStream(root);
        } catch (Exception e) {

            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test11_newDirectoryStream", e);
        }

        if (mustFail) {
            close(in);
            throwExpected("test11_newDirectoryStream");
        }

        for (AbsolutePath p : in) {

            if (tmp.contains(p)) {
                tmp.remove(p);
            } else {
                close(in);
                throwUnexpectedElement("test11_newDirectoryStream", p.getPath());
            }
        }

        close(in);

        if (tmp.size() > 0) {
            throwMissingElements("test11_newDirectoryStream", tmp);
        }
    }

    @org.junit.Test
    public void test11_newDirectoryStream() throws Exception {

        prepare();

        // test with null
        test11_newDirectoryStream(null, null, true);

        FileSystem fs = config.getTestFileSystem(files, credentials);
        prepareTestDir(fs, "test11_newDirectoryStream");

        // test with empty dir 
        test11_newDirectoryStream(testDir, null, false);

        // test with non-existing dir
        AbsolutePath dir0 = createNewTestDirName(testDir);
        test11_newDirectoryStream(dir0, null, true);

        // test with exising file
        AbsolutePath file0 = createTestFile(testDir, null);
        test11_newDirectoryStream(file0, null, true);

        // test with non-empty dir
        AbsolutePath file1 = createTestFile(testDir, null);
        AbsolutePath file2 = createTestFile(testDir, null);
        AbsolutePath file3 = createTestFile(testDir, null);

        Set<AbsolutePath> tmp = new HashSet<AbsolutePath>();
        tmp.add(file0);
        tmp.add(file1);
        tmp.add(file2);
        tmp.add(file3);

        test11_newDirectoryStream(testDir, tmp, false);

        // test with subdirs 
        AbsolutePath dir1 = createTestDir(testDir);
        AbsolutePath file4 = createTestFile(dir1, null);

        tmp.add(dir1);

        test11_newDirectoryStream(testDir, tmp, false);

        deleteTestFile(file4);
        deleteTestDir(dir1);
        deleteTestFile(file3);
        deleteTestFile(file2);
        deleteTestFile(file1);
        deleteTestFile(file0);
        deleteTestDir(testDir);

        if (config.supportsClose()) {
            // test with closed fs
            config.closeTestFileSystem(files, fs);
            test11_newDirectoryStream(testDir, null, true);
        }

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: newDirectoryStream with filter 
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing dir / existing empty dir / existing non-empty dir / existing dir with subdirs / 
    //              existing file / closed filesystem    
    // 
    // directoryStreams.Filter null / filter returns all / filter returns none / filter selects one. 

    // Total combinations : 7 + 8
    // 
    // Depends on: [getTestFileSystem], FileSystem.getEntryPath(), [createNewTestDirName], createDirectories, 
    //             [deleteTestDir], [createTestFile], [deleteTestFile], [deleteTestDir], [closeTestFileSystem]

    public void test12_newDirectoryStream(AbsolutePath root, DirectoryStream.Filter filter, Set<AbsolutePath> expected,
            boolean mustFail) throws Exception {

        Set<AbsolutePath> tmp = new HashSet<AbsolutePath>();

        if (expected != null) {
            tmp.addAll(expected);
        }

        DirectoryStream<AbsolutePath> in = null;

        try {
            in = files.newDirectoryStream(root, filter);
        } catch (Exception e) {

            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test12_newDirectoryStream_with_filter", e);
        }

        if (mustFail) {
            close(in);
            throwExpected("test12_newDirectoryStream_with_filter");
        }

        Iterator<AbsolutePath> itt = in.iterator();

        while (itt.hasNext()) {

            AbsolutePath p = itt.next();

            if (p == null) {
                throwUnexpectedElement("test12_newDirectoryStream_with_filter", null);
            }

            if (tmp.contains(p)) {
                tmp.remove(p);
            } else {
                close(in);
                throwUnexpectedElement("test12_newDirectoryStream_with_filter", p.getPath());
            }
        }

        close(in);

        if (tmp.size() > 0) {
            throwMissingElements("test12_newDirectoryStream_with_filter", tmp);
        }

        // close(in); // double close should result in exception
    }

    @org.junit.Test
    public void test12_newDirectoryStream_with_filter() throws Exception {

        prepare();

        // test with null 
        test12_newDirectoryStream(null, null, null, true);

        FileSystem fs = config.getTestFileSystem(files, credentials);
        prepareTestDir(fs, "test12_newDirectoryStream_with_filter");

        // test with empty dir + null filter 
        test12_newDirectoryStream(testDir, null, null, true);

        // test with empty dir + true filter 
        test12_newDirectoryStream(testDir, new AllTrue(), null, false);

        // test with empty dir + false filter 
        test12_newDirectoryStream(testDir, new AllTrue(), null, false);

        // test with non-existing dir
        AbsolutePath dir0 = createNewTestDirName(testDir);
        test12_newDirectoryStream(dir0, new AllTrue(), null, true);

        // test with existing file
        AbsolutePath file0 = createTestFile(testDir, null);
        test12_newDirectoryStream(file0, new AllTrue(), null, true);

        // test with non-empty dir and allTrue
        AbsolutePath file1 = createTestFile(testDir, null);
        AbsolutePath file2 = createTestFile(testDir, null);
        AbsolutePath file3 = createTestFile(testDir, null);

        Set<AbsolutePath> tmp = new HashSet<AbsolutePath>();
        tmp.add(file0);
        tmp.add(file1);
        tmp.add(file2);
        tmp.add(file3);

        test12_newDirectoryStream(testDir, new AllTrue(), tmp, false);

        // test with non-empty dir and allFalse
        test12_newDirectoryStream(testDir, new AllFalse(), null, false);

        tmp.remove(file3);

        // test with non-empty dir and select        
        test12_newDirectoryStream(testDir, new Select(tmp), tmp, false);

        // test with subdirs 
        AbsolutePath dir1 = createTestDir(testDir);
        AbsolutePath file4 = createTestFile(dir1, null);

        test12_newDirectoryStream(testDir, new Select(tmp), tmp, false);

        deleteTestFile(file4);
        deleteTestDir(dir1);
        deleteTestFile(file3);
        deleteTestFile(file2);
        deleteTestFile(file1);
        deleteTestFile(file0);
        deleteTestDir(testDir);

        if (config.supportsClose()) {
            // test with closed fs
            config.closeTestFileSystem(files, fs);
            test12_newDirectoryStream(testDir, new AllTrue(), null, true);
        }

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: getAttributes 
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing file / existing empty file / existing non-empty file / existing dir / existing link (!) 
    //              closed filesystem    
    // 
    // Total combinations : 7
    // 
    // Depends on: [getTestFileSystem], FileSystem.getEntryPath(), [createNewTestDirName], createDirectories, 
    //             [deleteTestDir], [createTestFile], [deleteTestFile], [deleteTestDir], [closeTestFileSystem]

    private void test13_getAttributes(AbsolutePath path, boolean isDirectory, long size, boolean mustFail) throws Exception {

        FileAttributes result = null;

        try {
            result = files.getAttributes(path);
        } catch (Exception e) {

            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test13_getFileAttributes", e);
        }

        if (mustFail) {
            throwExpected("test13_getFileAttributes");
        }

        if (result.isDirectory() && !isDirectory) {
            throwWrong("test13_getfileAttributes", "<not directory>", "<directory>");
        }

        if (size >= 0 && result.size() != size) {
            throwWrong("test13_getfileAttributes", "size=" + size, "size=" + result.size());
        }

        System.err.println("File " + path.getPath() + " has attributes: " + result.isReadable() + " " + result.isWritable() + " "
                + result.isExecutable() + " " + result.isSymbolicLink() + " " + result.isDirectory() + " "
                + result.isRegularFile() + " " + result.isHidden() + " " + result.isOther() + " " + result.lastAccessTime() + " "
                + result.lastModifiedTime() + " " + result.group() + " " + result.owner() + " " + result.permissions());
    }

    @org.junit.Test
    public void test13_getAttributes() throws Exception {

        prepare();

        // test with null
        test13_getAttributes(null, false, -1, true);

        FileSystem fs = config.getTestFileSystem(files, credentials);
        prepareTestDir(fs, "test13_getAttributes");

        // test with non-existing file
        AbsolutePath file0 = createNewTestFileName(testDir);
        test13_getAttributes(file0, false, -1, true);

        // test with existing empty file
        AbsolutePath file1 = createTestFile(testDir, null);
        test13_getAttributes(file1, false, 0, false);

        // test with existing non-empty file
        AbsolutePath file2 = createTestFile(testDir, new byte[] { 1, 2, 3 });
        test13_getAttributes(file2, false, 3, false);

        // test with existing dir 
        AbsolutePath dir0 = createTestDir(testDir);
        test13_getAttributes(dir0, true, -1, false);

        // TODO: test with link!

        deleteTestDir(dir0);
        deleteTestFile(file2);
        deleteTestFile(file1);
        deleteTestDir(testDir);

        if (config.supportsClose()) {
            // test with closed fs
            config.closeTestFileSystem(files, fs);
            test13_getAttributes(testDir, false, -1, true);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: setPosixFilePermissions 
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing file / existing file / existing dir / existing link (!) / closed filesystem
    // Set<PosixFilePermission> null / empty set / [various correct set] 
    // 
    // Total combinations : N
    // 
    // Depends on: [getTestFileSystem], FileSystem.getEntryPath(), [createNewTestDirName], createDirectories, 
    //             [deleteTestDir], [createTestFile], [deleteTestFile], [deleteTestDir], [closeTestFileSystem]

    private void test14_setPosixFilePermissions(AbsolutePath path, Set<PosixFilePermission> permissions, boolean mustFail)
            throws Exception {

        try {
            files.setPosixFilePermissions(path, permissions);
        } catch (Exception e) {
            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test14_setPosixFilePermissions", e);
        }

        if (mustFail) {
            throwExpected("test14_setPosixFilePermissions");
        }

        // Check result
        FileAttributes attributes = files.getAttributes(path);
        Set<PosixFilePermission> tmp = attributes.permissions();

        if (!permissions.equals(tmp)) {
            throwWrong("test14_setPosixFilePermissions", permissions.toString(), tmp.toString());
        }
    }

    @org.junit.Test
    public void test14_setPosixFilePermissions() throws Exception {

        prepare();

        // test with null, null
        test14_setPosixFilePermissions(null, null, true);

        FileSystem fs = config.getTestFileSystem(files, credentials);
        prepareTestDir(fs, "test14_setPosixFilePermissions");

        // test with existing file, null set
        AbsolutePath file0 = createTestFile(testDir, null);
        test14_setPosixFilePermissions(file0, null, true);

        // test with existing file, empty set 
        Set<PosixFilePermission> permissions = new HashSet<PosixFilePermission>();
        test14_setPosixFilePermissions(file0, permissions, false);

        // test with existing file, non-empty set 
        permissions.add(PosixFilePermission.OWNER_EXECUTE);
        permissions.add(PosixFilePermission.OWNER_READ);
        permissions.add(PosixFilePermission.OWNER_WRITE);
        test14_setPosixFilePermissions(file0, permissions, false);

        permissions.add(PosixFilePermission.OTHERS_READ);
        test14_setPosixFilePermissions(file0, permissions, false);

        permissions.add(PosixFilePermission.GROUP_READ);
        test14_setPosixFilePermissions(file0, permissions, false);

        // test with non-existing file
        AbsolutePath file1 = createNewTestFileName(testDir);
        test14_setPosixFilePermissions(file1, permissions, true);

        // test with existing dir 
        AbsolutePath dir0 = createTestDir(testDir);

        permissions.add(PosixFilePermission.OWNER_EXECUTE);
        permissions.add(PosixFilePermission.OWNER_READ);
        permissions.add(PosixFilePermission.OWNER_WRITE);
        test14_setPosixFilePermissions(dir0, permissions, false);

        permissions.add(PosixFilePermission.OTHERS_READ);
        test14_setPosixFilePermissions(dir0, permissions, false);

        permissions.add(PosixFilePermission.GROUP_READ);
        test14_setPosixFilePermissions(dir0, permissions, false);

        deleteTestDir(dir0);
        deleteTestFile(file0);
        deleteTestDir(testDir);

        if (config.supportsClose()) {
            // test with closed fs
            config.closeTestFileSystem(files, fs);
            test14_setPosixFilePermissions(file0, permissions, true);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: newAttributesDirectoryStream 
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing dir / existing empty dir / existing non-empty dir / existing dir with subdirs / 
    //              existing file / closed filesystem    
    // 
    // Total combinations : 7
    // 
    // Depends on: [getTestFileSystem], [createTestDir], [createNewTestDirName], [createTestFile], newDirectoryStream,   
    //             [deleteTestDir], , [deleteTestFile], [deleteTestDir], [closeTestFileSystem]

    private void test15_newAttributesDirectoryStream(AbsolutePath root, Set<PathAttributesPair> expected, boolean mustFail)
            throws Exception {

        Set<PathAttributesPair> tmp = new HashSet<PathAttributesPair>();

        if (expected != null) {
            tmp.addAll(expected);
        }

        DirectoryStream<PathAttributesPair> in = null;

        try {
            in = files.newAttributesDirectoryStream(root);
        } catch (Exception e) {

            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test15_newAttributesDirectoryStream", e);
        }

        if (mustFail) {
            close(in);
            throwExpected("test15_newAttributesDirectoryStream");
        }

        System.err.println("Comparing PathAttributesPairs:");

        for (PathAttributesPair p : in) {

            System.err.println("Got input " + p.path().getPath() + " " + p.attributes());

            PathAttributesPair found = null;

            for (PathAttributesPair x : tmp) {

                System.err.println("  Comparing to " + x.path().getPath() + " " + x.attributes());

                if (x.path().equals(p.path()) && x.attributes().equals(p.attributes())) {
                    System.err.println("Found!");
                    found = x;
                    break;
                }
            }

            System.err.println("  Found = " + found);

            if (found != null) {
                tmp.remove(found);
            } else {
                System.err.println("NOT Found!");
                close(in);
                throwUnexpectedElement("test15_newAttributesDirectoryStream", p.path().getPath());

            }

            //            if (tmp.contains(p)) {
            //                System.err.println("Found!");
            //                tmp.remove(p);
            //            } else {
            //                System.err.println("NOT Found!");
            //                
            //                close(in);
            //                throwUnexpectedElement("newAttributesDirectoryStream", p.path().getPath());
            //            }
        }

        close(in);

        if (tmp.size() > 0) {
            throwMissingElements("test15_newAttributesDirectoryStream", tmp);
        }
    }

    @org.junit.Test
    public void test15_newAttrributesDirectoryStream() throws Exception {

        prepare();

        // test with null
        test15_newAttributesDirectoryStream(null, null, true);

        FileSystem fs = config.getTestFileSystem(files, credentials);
        prepareTestDir(fs, "test15_newAttrributesDirectoryStream");

        // test with empty dir 
        test15_newAttributesDirectoryStream(testDir, null, false);

        // test with non-existing dir
        AbsolutePath dir0 = createNewTestDirName(testDir);
        test15_newAttributesDirectoryStream(dir0, null, true);

        // test with exising file
        AbsolutePath file0 = createTestFile(testDir, null);
        test15_newAttributesDirectoryStream(file0, null, true);

        // test with non-empty dir
        AbsolutePath file1 = createTestFile(testDir, null);
        AbsolutePath file2 = createTestFile(testDir, null);
        AbsolutePath file3 = createTestFile(testDir, null);

        Set<PathAttributesPair> result = new HashSet<PathAttributesPair>();
        result.add(new PathAttributesPairImplementation(file0, files.getAttributes(file0)));
        result.add(new PathAttributesPairImplementation(file1, files.getAttributes(file1)));
        result.add(new PathAttributesPairImplementation(file2, files.getAttributes(file2)));
        result.add(new PathAttributesPairImplementation(file3, files.getAttributes(file3)));

        test15_newAttributesDirectoryStream(testDir, result, false);

        // test with subdirs 
        AbsolutePath dir1 = createTestDir(testDir);
        AbsolutePath file4 = createTestFile(dir1, null);

        result.add(new PathAttributesPairImplementation(dir1, files.getAttributes(dir1)));

        test15_newAttributesDirectoryStream(testDir, result, false);

        deleteTestFile(file4);
        deleteTestDir(dir1);
        deleteTestFile(file3);
        deleteTestFile(file2);
        deleteTestFile(file1);
        deleteTestFile(file0);
        deleteTestDir(testDir);

        if (config.supportsClose()) {
            // test with closed fs
            config.closeTestFileSystem(files, fs);
            test15_newAttributesDirectoryStream(testDir, null, true);
        }

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: newAttributesDirectoryStream with filter 
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing dir / existing empty dir / existing non-empty dir / existing dir with subdirs / 
    //              existing file / closed filesystem    
    // 
    // directoryStreams.Filter null / filter returns all / filter returns none / filter selects one. 

    // Total combinations : 7 + 8
    // 
    // Depends on: [getTestFileSystem], FileSystem.getEntryPath(), [createNewTestDirName], createDirectories, 
    //             [deleteTestDir], [createTestFile], [deleteTestFile], [deleteTestDir], [closeTestFileSystem]

    public void test16_newAttributesDirectoryStream(AbsolutePath root, DirectoryStream.Filter filter,
            Set<PathAttributesPair> expected, boolean mustFail) throws Exception {

        Set<PathAttributesPair> tmp = new HashSet<PathAttributesPair>();

        if (expected != null) {
            tmp.addAll(expected);
        }

        DirectoryStream<PathAttributesPair> in = null;

        try {
            in = files.newAttributesDirectoryStream(root, filter);
        } catch (Exception e) {

            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test16_newAttributesDirectoryDirectoryStream_with_filter", e);
        }

        if (mustFail) {
            close(in);
            throwExpected("test16_newAttributesDirectoryDirectoryStream_with_filter");
        }

        for (PathAttributesPair p : in) {

            System.err.println("Got input " + p.path().getPath() + " " + p.attributes());

            PathAttributesPair found = null;

            for (PathAttributesPair x : tmp) {

                System.err.println("  Comparing to " + x.path().getPath() + " " + x.attributes());

                if (x.path().equals(p.path()) && x.attributes().equals(p.attributes())) {
                    System.err.println("Found!");
                    found = x;
                    break;
                }
            }

            System.err.println("  Found = " + found);

            if (found != null) {
                tmp.remove(found);
            } else {
                System.err.println("NOT Found!");
                close(in);
                throwUnexpectedElement("test16_newAttributesDirectoryStream_with_filter", p.path().getPath());

            }

            //            if (tmp.contains(p)) {
            //                System.err.println("Found!");
            //                tmp.remove(p);
            //            } else {
            //                System.err.println("NOT Found!");
            //                
            //                close(in);
            //                throwUnexpectedElement("newAttributesDirectoryStream", p.path().getPath());
            //            }
        }

        close(in);

        if (tmp.size() > 0) {
            throwMissingElements("test16_newAttributesDirectoryDirectoryStream_with_filter", tmp);
        }
    }

    @org.junit.Test
    public void test15_newAttributesDirectoryStream_with_filter() throws Exception {

        prepare();

        // test with null 
        test16_newAttributesDirectoryStream(null, null, null, true);

        FileSystem fs = config.getTestFileSystem(files, credentials);
        prepareTestDir(fs, "test15_newAttributesDirectoryStream_with_filter");

        // test with empty dir + null filter 
        test16_newAttributesDirectoryStream(testDir, null, null, true);

        // test with empty dir + true filter 
        test16_newAttributesDirectoryStream(testDir, new AllTrue(), null, false);

        // test with empty dir + false filter 
        test16_newAttributesDirectoryStream(testDir, new AllTrue(), null, false);

        // test with non-existing dir
        AbsolutePath dir0 = createNewTestDirName(testDir);
        test16_newAttributesDirectoryStream(dir0, new AllTrue(), null, true);

        // test with existing file
        AbsolutePath file0 = createTestFile(testDir, null);
        test16_newAttributesDirectoryStream(file0, new AllTrue(), null, true);

        // test with non-empty dir and allTrue
        AbsolutePath file1 = createTestFile(testDir, null);
        AbsolutePath file2 = createTestFile(testDir, null);
        AbsolutePath file3 = createTestFile(testDir, null);

        Set<PathAttributesPair> result = new HashSet<PathAttributesPair>();
        result.add(new PathAttributesPairImplementation(file0, files.getAttributes(file0)));
        result.add(new PathAttributesPairImplementation(file1, files.getAttributes(file1)));
        result.add(new PathAttributesPairImplementation(file2, files.getAttributes(file2)));
        result.add(new PathAttributesPairImplementation(file3, files.getAttributes(file3)));

        test16_newAttributesDirectoryStream(testDir, new AllTrue(), result, false);

        // test with non-empty dir and allFalse
        test16_newAttributesDirectoryStream(testDir, new AllFalse(), null, false);

        // test with subdirs  
        AbsolutePath dir1 = createTestDir(testDir);
        AbsolutePath file4 = createTestFile(dir1, null);

        result.add(new PathAttributesPairImplementation(dir1, files.getAttributes(dir1)));
        test16_newAttributesDirectoryStream(testDir, new AllTrue(), result, false);

        // test with non-empty dir and select        
        Set<AbsolutePath> tmp = new HashSet<AbsolutePath>();
        tmp.add(file0);
        tmp.add(file1);
        tmp.add(file2);

        result = new HashSet<PathAttributesPair>();
        result.add(new PathAttributesPairImplementation(file0, files.getAttributes(file0)));
        result.add(new PathAttributesPairImplementation(file1, files.getAttributes(file1)));
        result.add(new PathAttributesPairImplementation(file2, files.getAttributes(file2)));

        test16_newAttributesDirectoryStream(testDir, new Select(tmp), result, false);

        deleteTestFile(file4);
        deleteTestDir(dir1);
        deleteTestFile(file3);
        deleteTestFile(file2);
        deleteTestFile(file1);
        deleteTestFile(file0);
        deleteTestDir(testDir);

        if (config.supportsClose()) {
            // test with closed fs
            config.closeTestFileSystem(files, fs);
            test16_newAttributesDirectoryStream(testDir, new AllTrue(), null, true);
        }

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: newInputStream 
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing file / existing empty file / existing non-empty file / existing dir / closed filesystem    
    // 
    // Total combinations : 6
    // 
    // Depends on: 

    public void test20_newInputStream(AbsolutePath file, byte[] expected, boolean mustFail) throws Exception {

        InputStream in = null;

        try {
            in = files.newInputStream(file);
        } catch (Exception e) {

            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test20_newInputStream", e);
        }

        if (mustFail) {
            close(in);
            throwExpected("test20_newInputStream");
        }

        byte[] data = readFully(in);

        if (expected == null) {
            if (data.length != 0) {
                throwWrong("test20_newInputStream", "zero bytes", data.length + " bytes");
            }
            return;
        }

        if (expected.length != data.length) {
            throwWrong("test20_newInputStream", expected.length + " bytes", data.length + " bytes");
        }

        if (!Arrays.equals(expected, data)) {
            throwWrong("test20_newInputStream", Arrays.toString(expected), Arrays.toString(data));
        }
    }

    @org.junit.Test
    public void test20_newInputStream() throws Exception {

        byte[] data = "Hello World".getBytes();

        prepare();

        // test with null
        test20_newInputStream(null, null, true);

        FileSystem fs = config.getTestFileSystem(files, credentials);
        prepareTestDir(fs, "test20_newInputStream");

        // test with non-existing file
        AbsolutePath file0 = createNewTestFileName(testDir);
        test20_newInputStream(file0, null, true);

        // test with existing empty file
        AbsolutePath file1 = createTestFile(testDir, null);
        test20_newInputStream(file1, null, false);

        // test with existing non-empty file
        AbsolutePath file2 = createTestFile(testDir, data);
        test20_newInputStream(file2, data, false);

        // test with existing dir 
        AbsolutePath dir0 = createTestDir(testDir);
        test20_newInputStream(dir0, null, true);

        // cleanup
        deleteTestFile(file1);
        deleteTestFile(file2);
        deleteTestDir(dir0);
        deleteTestDir(testDir);

        if (config.supportsClose()) {
            // test with closed fs
            config.closeTestFileSystem(files, fs);
            test20_newInputStream(file2, data, true);
        }

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: newOuputStream 
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing file / existing empty file / existing non-empty file / existing dir / closed filesystem
    // OpenOption null / CREATE / OPEN / OPEN_OR_CREATE / READ / TRUNCATE / READ / WRITE + combinations
    // 
    // Total combinations : N
    // 
    // Depends on: 

    public void test21_newOutputStream(AbsolutePath path, OpenOption[] options, byte[] data, byte[] expected, boolean mustFail)
            throws Exception {

        OutputStream out = null;

        try {
            out = files.newOutputStream(path, options);
        } catch (Exception e) {

            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test21_newOutputStream", e);
        }

        if (mustFail) {
            close(out);
            throwExpected("test21_newOutputStream");
        }

        out.write(data);
        close(out);

        InputStream in = files.newInputStream(path);

        byte[] tmp = readFully(in);

        if (expected == null) {
            if (data.length != 0) {
                throwWrong("test21_newOutputStream", "zero bytes", tmp.length + " bytes");
            }
            return;
        }

        if (expected.length != tmp.length) {
            throwWrong("test21_newOutputStream", expected.length + " bytes", tmp.length + " bytes");
        }

        if (!Arrays.equals(expected, tmp)) {
            throwWrong("test21_newOutputStream", Arrays.toString(expected), Arrays.toString(tmp));
        }
    }

    @org.junit.Test
    public void test21_newOutputStream() throws Exception {

        byte[] data = "Hello World".getBytes();
        byte[] data2 = "Hello WorldHello World".getBytes();

        prepare();

        // test with null
        test21_newOutputStream(null, null, null, null, true);

        FileSystem fs = config.getTestFileSystem(files, credentials);
        prepareTestDir(fs, "test21_newOuputStream");

        // test with existing file and null options
        AbsolutePath file0 = createTestFile(testDir, null);
        test21_newOutputStream(file0, null, null, null, true);

        // test with existing file and empty options
        test21_newOutputStream(file0, new OpenOption[0], null, null, true);

        // test with existing file and CREATE option
        test21_newOutputStream(file0, new OpenOption[] { OpenOption.CREATE }, null, null, true);

        // test with existing file and OPEN option
        test21_newOutputStream(file0, new OpenOption[] { OpenOption.OPEN }, null, null, true);

        // test with existing file and OPEN_OR_CREATE option
        test21_newOutputStream(file0, new OpenOption[] { OpenOption.OPEN_OR_CREATE }, null, null, true);

        // test with existing file and APPEND option
        test21_newOutputStream(file0, new OpenOption[] { OpenOption.APPEND }, null, null, true);

        // test with existing file and TRUNCATE option
        test21_newOutputStream(file0, new OpenOption[] { OpenOption.TRUNCATE }, null, null, true);

        // test with existing file and READ option
        test21_newOutputStream(file0, new OpenOption[] { OpenOption.READ }, null, null, true);

        // test with existing file and WRITE option
        test21_newOutputStream(file0, new OpenOption[] { OpenOption.WRITE }, null, null, true);

        // test with existing file and CREATE + APPEND option
        test21_newOutputStream(file0, new OpenOption[] { OpenOption.CREATE, OpenOption.APPEND }, null, null, true);

        // test with existing file and CREATE + APPEND + READ option
        test21_newOutputStream(file0, new OpenOption[] { OpenOption.CREATE, OpenOption.APPEND, OpenOption.READ }, null, null,
                true);

        // test with existing file and OPEN_OR_CREATE + APPEND option
        test21_newOutputStream(file0, new OpenOption[] { OpenOption.OPEN_OR_CREATE, OpenOption.APPEND }, data, data, false);

        // test with existing file and OPEN + APPEND option
        test21_newOutputStream(file0, new OpenOption[] { OpenOption.OPEN, OpenOption.APPEND }, data, data2, false);

        // test with existing file and OPEN_OR_CREATE + APPEND + WRITE option
        test21_newOutputStream(file0, new OpenOption[] { OpenOption.OPEN, OpenOption.TRUNCATE, OpenOption.WRITE }, data, data,
                false);

        // test with existing file and CREATE + TRUNCATE option
        test21_newOutputStream(file0, new OpenOption[] { OpenOption.CREATE, OpenOption.TRUNCATE }, null, null, true);

        // test with existing file and OPEN_OR_CREATE + TRUNCATE option
        test21_newOutputStream(file0, new OpenOption[] { OpenOption.OPEN_OR_CREATE, OpenOption.TRUNCATE }, data, data, false);

        // test with existing file and OPEN + TRUNCATE option
        test21_newOutputStream(file0, new OpenOption[] { OpenOption.OPEN, OpenOption.TRUNCATE }, data, data, false);

        deleteTestFile(file0);

        // test with non-existing and CREATE + APPEND option
        AbsolutePath file1 = createNewTestFileName(testDir);
        test21_newOutputStream(file1, new OpenOption[] { OpenOption.CREATE, OpenOption.APPEND }, data, data, false);
        deleteTestFile(file1);

        // test with non-existing and OPEN_OR_CREATE + APPEND option
        AbsolutePath file2 = createNewTestFileName(testDir);
        test21_newOutputStream(file2, new OpenOption[] { OpenOption.OPEN_OR_CREATE, OpenOption.APPEND }, data, data, false);
        deleteTestFile(file2);

        // test with non-existing and OPEN + APPEND option
        AbsolutePath file3 = createNewTestFileName(testDir);
        test21_newOutputStream(file3, new OpenOption[] { OpenOption.OPEN, OpenOption.APPEND }, null, null, true);

        // test with exising dir
        AbsolutePath dir0 = createTestDir(testDir);

        test21_newOutputStream(dir0, new OpenOption[] { OpenOption.CREATE, OpenOption.APPEND }, null, null, true);
        test21_newOutputStream(dir0, new OpenOption[] { OpenOption.OPEN_OR_CREATE, OpenOption.APPEND }, null, null, true);
        test21_newOutputStream(dir0, new OpenOption[] { OpenOption.OPEN, OpenOption.APPEND }, null, null, true);

        deleteTestDir(dir0);

        // test with conflicting options
        AbsolutePath file4 = createTestFile(testDir, null);

        test21_newOutputStream(file4, new OpenOption[] { OpenOption.CREATE, OpenOption.OPEN, OpenOption.APPEND }, null, null,
                true);
        test21_newOutputStream(file4, new OpenOption[] { OpenOption.OPEN, OpenOption.TRUNCATE, OpenOption.APPEND }, null, null,
                true);
        test21_newOutputStream(file4, new OpenOption[] { OpenOption.OPEN, OpenOption.APPEND, OpenOption.READ }, null, null, true);

        deleteTestFile(file4);

        // test with non-existing and CREATE option
        AbsolutePath file5 = createNewTestFileName(testDir);
        test21_newOutputStream(file5, new OpenOption[] { OpenOption.CREATE, OpenOption.APPEND }, data, data, false);
        deleteTestFile(file5);

        deleteTestDir(testDir);

        if (config.supportsClose()) {
            // test with closed fs
            config.closeTestFileSystem(files, fs);
            test21_newOutputStream(file0, new OpenOption[] { OpenOption.OPEN_OR_CREATE, OpenOption.APPEND }, null, null, true);
        }

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: newByteChannel 
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing file / existing empty file / existing non-empty file / existing dir / closed filesystem
    // OpenOption null / CREATE / OPEN / OPEN_OR_CREATE / READ / TRUNCATE / READ / WRITE + combinations
    // 
    // Total combinations : N
    // 
    // Depends on: 

    //    public void test22_newByteChannel(AbsolutePath path, OpenOption [] options, byte [] toWrite, byte [] toRead, 
    //            boolean mustFail) throws Exception {
    //
    //        if (!config.supportsNewByteChannel()) {
    //            return;
    //        }
    //        
    //        SeekableByteChannel channel = null;
    //        
    //        try { 
    //            channel = files.newByteChannel(path, options);
    //        } catch (Exception e) { 
    //
    //            if (mustFail) { 
    //                // expected
    //                return;
    //            } 
    //            
    //            throwUnexpected("test22_newByteChannel", e);
    //        }
    //        
    //        if (mustFail) {             
    //            close(channel);            
    //            throwExpected("test22_newByteChannel");
    //        }
    //        
    //        if (toWrite != null) { 
    //            channel.write(ByteBuffer.wrap(toWrite));
    //        }
    //
    //        if (toRead != null) {
    //            
    //            channel.position(0);
    //            
    //            byte [] tmp = readFully(channel);
    //        
    //            if (toRead.length != tmp.length) { 
    //                throwWrong("test22_newByteChannel", toRead.length + " bytes", tmp.length + " bytes");            
    //            }
    //        
    //            if (!Arrays.equals(toRead, tmp)) { 
    //                throwWrong("test22_newByteChannel", Arrays.toString(toRead), Arrays.toString(tmp));
    //            }
    //        }
    //        
    //        close(channel);
    //    }

    //    @org.junit.Test
    //    public void test21_newByteChannel() throws Exception { 
    //
    //        if (!config.supportsNewByteChannel()) {
    //            return;
    //        }
    //        
    //        byte [] data = "Hello World".getBytes();
    //        byte [] data2 = "Hello WorldHello World".getBytes();
    //        
    //        prepare();
    //
    //        // test with null
    //        test22_newByteChannel(null, null, null, null, true);
    //        
    //        FileSystem fs =  config.getTestFileSystem(files, credentials);
    //        prepareTestDir(fs, "test22_newByteChannel");
    //        
    //        // test with existing file and null options
    //        AbsolutePath file0 = createTestFile(testDir, null);
    //        test22_newByteChannel(file0, null, null, null, true);
    //        
    //        // test with existing file and empty options
    //        test22_newByteChannel(file0, new OpenOption[0],  null, null, true);
    //        
    //        // test with existing file and CREATE option
    //        test22_newByteChannel(file0, new OpenOption [] { OpenOption.CREATE }, null, null, true);
    //
    //        // test with existing file and OPEN option
    //        test22_newByteChannel(file0, new OpenOption [] { OpenOption.OPEN }, null, null, true);
    //
    //        // test with existing file and OPEN_OR_CREATE option
    //        test22_newByteChannel(file0, new OpenOption [] { OpenOption.OPEN_OR_CREATE }, null, null, true);
    //
    //        // test with existing file and APPEND option
    //        test22_newByteChannel(file0, new OpenOption [] { OpenOption.APPEND }, null, null, true);
    //
    //        // test with existing file and TRUNCATE option
    //        test22_newByteChannel(file0, new OpenOption [] { OpenOption.TRUNCATE }, null, null, true);
    //        
    //        // test with existing file and READ option
    //        test22_newByteChannel(file0, new OpenOption [] { OpenOption.READ }, null, null, true);
    //        
    //        // test with existing file and WRITE option
    //        test22_newByteChannel(file0, new OpenOption [] { OpenOption.WRITE }, null, null, true);
    //        
    //        // test with existing file and CREATE + APPEND option
    //        test22_newByteChannel(file0, new OpenOption [] { OpenOption.CREATE, OpenOption.APPEND }, null, null, true);
    //
    //        // test with existing file and OPEN + READ + APPEND option
    //        test22_newByteChannel(file0, new OpenOption [] { OpenOption.OPEN, OpenOption.READ, OpenOption.APPEND }, null, null, true);
    //        
    //        // test with existing file and OPEN + READ option
    //        AbsolutePath file1 = createTestFile(testDir, data);
    //        test22_newByteChannel(file1, new OpenOption [] { OpenOption.OPEN, OpenOption.READ }, null, data, false);
    //
    //        // Test with existing file and OPEN + APPEND + READ + WRITE 
    //        test22_newByteChannel(file1, new OpenOption [] { OpenOption.OPEN, OpenOption.WRITE, OpenOption.READ }, data, data, false);
    //        
    //        // Test with existing file and OPEN + APPEND + READ + WRITE 
    //        test22_newByteChannel(file1, new OpenOption [] { OpenOption.OPEN, OpenOption.APPEND, OpenOption.WRITE, OpenOption.READ }, null, null, true);
    //        
    //        // test with existing file and OPEN + WRITE without APPEND option
    //        test22_newByteChannel(file1, new OpenOption [] { OpenOption.OPEN, OpenOption.WRITE }, null, null, true);
    //        
    //        // test with existing file and CREATE + WRITE + APPEND 
    //        test22_newByteChannel(file1, new OpenOption [] { OpenOption.CREATE, OpenOption.WRITE, OpenOption.APPEND }, null, null, true);
    //        
    //        deleteTestFile(file1);
    //        
    //        // test with non-existing file and CREATE + WRITE + APPEND
    //        AbsolutePath file2 = createNewTestFileName(testDir);
    //        test22_newByteChannel(file2, new OpenOption [] { OpenOption.CREATE, OpenOption.WRITE, OpenOption.APPEND }, data, null, false);
    //        test22_newByteChannel(file2, new OpenOption [] { OpenOption.OPEN, OpenOption.READ }, null, data, false);
    //        deleteTestFile(file2);
    //        
    //        // test with non-existing file and OPEN + READ
    //        AbsolutePath file3 = createNewTestFileName(testDir);
    //        test22_newByteChannel(file3, new OpenOption [] { OpenOption.OPEN, OpenOption.READ }, null, null, true);
    //        
    //        // test with non-existing file and OPEN_OR_CREATE + WRITE + READ + APPEND
    //        AbsolutePath file4 = createNewTestFileName(testDir);
    //        test22_newByteChannel(file4, new OpenOption [] { OpenOption.OPEN_OR_CREATE, OpenOption.WRITE, OpenOption.READ }, data, data, false);
    //
    //        // test with existing file and OPEN_OR_CREATE + WRITE + READ + APPEND
    //        test22_newByteChannel(file4, new OpenOption [] { OpenOption.OPEN_OR_CREATE, OpenOption.WRITE, OpenOption.APPEND }, data, 
    //                null, false);
    //        test22_newByteChannel(file4, new OpenOption [] { OpenOption.OPEN, OpenOption.READ, }, null, data2, false);
    //        
    //        deleteTestFile(file0);
    //        deleteTestFile(file4);
    //        
    //        deleteTestDir(testDir);
    //        
    //        if (config.supportsClose()) { 
    //            // test with closed fs
    //            config.closeTestFileSystem(files,fs);
    //            test22_newByteChannel(file0, new OpenOption [] { OpenOption.OPEN_OR_CREATE, OpenOption.APPEND, OpenOption.READ }, 
    //                    null, null, true);             
    //        }
    //
    //        cleanup();
    //    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: copy (synchronous) 
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing file / existing empty file / existing non-empty file / existing dir / closed filesystem
    // CopyOptions  null / CREATE / REPLACE / IGNORE / APPEND / RESUME / VERIFY / ASYNCHRONOUS 
    // 
    // Total combinations : N
    // 
    // Depends on: 

    private void test23_copy(AbsolutePath source, AbsolutePath target, CopyOption[] options, byte[] expected, boolean mustFail)
            throws Exception {

        Copy copy;

        try {
            copy = files.copy(source, target, options);
        } catch (Exception e) {

            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test23_copy", e);
        }

        if (mustFail) {
            throwExpected("test23_copy");
        }

        if (expected != null) {
            byte[] tmp = readFully(files.newInputStream(target));

            if (!Arrays.equals(expected, tmp)) {
                throwWrong("test23_copy", Arrays.toString(expected), Arrays.toString(tmp));
            }
        }
    }

    @org.junit.Test
    public void test23_copy() throws Exception {

        byte[] data = "Hello World!".getBytes();
        byte[] data2 = "Goodbye World!".getBytes();
        byte[] data3 = "Hello World!Goodbye World!".getBytes();
        byte[] data4 = "Hello World!Hello World!".getBytes();
        byte[] data5 = "Hello World!Hello World!Hello World!".getBytes();

        prepare();

        // test with null
        test23_copy(null, null, null, null, true);

        FileSystem fs = config.getTestFileSystem(files, credentials);
        prepareTestDir(fs, "test23_copy");

        AbsolutePath file0 = createTestFile(testDir, data);

        // test without target
        test23_copy(file0, null, new CopyOption[] { CopyOption.CREATE }, null, true);

        // test without source
        test23_copy(null, file0, new CopyOption[] { CopyOption.CREATE }, null, true);

        AbsolutePath file1 = createNewTestFileName(testDir);
        AbsolutePath file2 = createNewTestFileName(testDir);
        AbsolutePath file3 = createNewTestFileName(testDir);

        AbsolutePath file4 = createTestFile(testDir, data2);
        AbsolutePath file5 = createTestFile(testDir, data3);

        AbsolutePath dir0 = createTestDir(testDir);
        AbsolutePath dir1 = createNewTestDirName(testDir);

        AbsolutePath file6 = createNewTestFileName(dir1);

        // test copy with non-existing source
        test23_copy(file1, file2, new CopyOption[0], null, true);

        // test copy with dir source 
        test23_copy(dir0, file1, new CopyOption[] { CopyOption.CREATE }, null, true);

        // test copy with non-existing target
        test23_copy(file0, file1, new CopyOption[] { CopyOption.IGNORE, CopyOption.CREATE }, null, true);
        test23_copy(file0, file1, new CopyOption[] { CopyOption.CREATE, CopyOption.IGNORE }, null, true);
        test23_copy(file0, file1, new CopyOption[] { CopyOption.CREATE, CopyOption.REPLACE }, null, true);
        test23_copy(file0, file1, new CopyOption[] { CopyOption.CREATE, CopyOption.RESUME }, null, true);
        test23_copy(file0, file1, new CopyOption[] { CopyOption.CREATE, CopyOption.APPEND }, null, true);

        // test copy with non-existing target
        test23_copy(file0, file1, new CopyOption[] { CopyOption.CREATE }, data, false);
        test23_copy(file0, file2, new CopyOption[] { CopyOption.CREATE, CopyOption.CREATE }, data, false);

        // test copy with non-existing target with non-existing parent
        test23_copy(file0, file6, new CopyOption[] { CopyOption.CREATE }, null, true);

        // test copy with existing target 
        test23_copy(file0, file1, new CopyOption[0], null, true);
        test23_copy(file0, file1, new CopyOption[] { CopyOption.CREATE }, null, true);

        // test copy with same target as source
        test23_copy(file0, file0, new CopyOption[] { CopyOption.CREATE }, data, false);

        // test ignore with existing target 
        test23_copy(file4, file1, new CopyOption[] { CopyOption.IGNORE }, data, false);
        test23_copy(file4, file1, new CopyOption[] { CopyOption.IGNORE, CopyOption.IGNORE }, data, false);

        // test resume with existing target 
        test23_copy(file4, file1, new CopyOption[] { CopyOption.RESUME, CopyOption.VERIFY }, null, true);
        test23_copy(file1, file5, new CopyOption[] { CopyOption.RESUME }, null, true);
        test23_copy(file5, file1, new CopyOption[] { CopyOption.RESUME, CopyOption.VERIFY }, data3, false);
        test23_copy(file5, file1, new CopyOption[] { CopyOption.RESUME }, data3, false);
        test23_copy(file5, file2, new CopyOption[] { CopyOption.RESUME, CopyOption.RESUME }, data3, false);
        test23_copy(file4, file1, new CopyOption[] { CopyOption.RESUME, CopyOption.VERIFY }, null, true);

        // test resume with non-existing source 
        test23_copy(file3, file1, new CopyOption[] { CopyOption.RESUME, CopyOption.VERIFY }, null, true);

        // test resume with non-exising target
        test23_copy(file5, file3, new CopyOption[] { CopyOption.RESUME, CopyOption.VERIFY }, null, true);

        // test resume with dir source 
        test23_copy(dir0, file1, new CopyOption[] { CopyOption.RESUME, CopyOption.VERIFY }, null, true);

        // test resume with dir target
        test23_copy(file5, dir0, new CopyOption[] { CopyOption.RESUME, CopyOption.VERIFY }, null, true);

        // test resume with same dir and target
        test23_copy(file5, file5, new CopyOption[] { CopyOption.RESUME, CopyOption.VERIFY }, data3, false);

        // test replace with existing target 
        test23_copy(file0, file1, new CopyOption[] { CopyOption.REPLACE }, data, false);
        test23_copy(file0, file1, new CopyOption[] { CopyOption.REPLACE, CopyOption.REPLACE }, data, false);
        test23_copy(file0, file1, new CopyOption[] { CopyOption.REPLACE, CopyOption.VERIFY }, null, true);

        // test append with existing target 
        test23_copy(file0, file1, new CopyOption[] { CopyOption.APPEND }, data4, false);
        test23_copy(file0, file1, new CopyOption[] { CopyOption.APPEND, CopyOption.APPEND }, data5, false);

        // test append with non-existing source 
        test23_copy(file3, file1, new CopyOption[] { CopyOption.APPEND }, null, true);

        // test append with non-existing target 
        test23_copy(file0, file3, new CopyOption[] { CopyOption.APPEND }, null, true);

        // test append with dir source 
        test23_copy(dir0, file1, new CopyOption[] { CopyOption.APPEND }, null, true);

        // test append with dir target 
        test23_copy(file0, dir0, new CopyOption[] { CopyOption.APPEND }, null, true);

        // test append with source equals target 
        test23_copy(file0, file0, new CopyOption[] { CopyOption.APPEND }, null, true);

        // test with source equals target and empty option 
        test23_copy(file0, file0, new CopyOption[] { null }, null, true);

        deleteTestDir(dir0);
        deleteTestFile(file5);
        deleteTestFile(file4);
        deleteTestFile(file2);
        deleteTestFile(file1);
        deleteTestFile(file0);
        deleteTestDir(testDir);

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: copy (asynchronous) 
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing file / existing empty file / existing non-empty file / existing dir / closed filesystem
    // CopyOptions  null / CREATE / REPLACE / IGNORE / APPEND / RESUME / VERIFY / ASYNCHRONOUS 
    // 
    // Total combinations : N
    // 
    // Depends on: 

    @org.junit.Test
    public void test24_copy_async() throws Exception {

        byte[] data = "Hello World!".getBytes();

        prepare();

        FileSystem fs = config.getTestFileSystem(files, credentials);
        prepareTestDir(fs, "test24_copy_async");
        AbsolutePath file0 = createTestFile(testDir, data);
        AbsolutePath file1 = createNewTestFileName(testDir);

        // Test the async copy
        Copy copy = files.copy(file0, file1, new CopyOption[] { CopyOption.CREATE, CopyOption.ASYNCHRONOUS });
        CopyStatus status = files.getCopyStatus(copy);

        while (!status.isDone()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignored
            }

            status = files.getCopyStatus(copy);
        }

        // Test the cancel
        copy = files.copy(file0, file1, new CopyOption[] { CopyOption.REPLACE, CopyOption.ASYNCHRONOUS });
        status = files.cancelCopy(copy);

        deleteTestFile(file1);
        deleteTestFile(file0);
        deleteTestDir(testDir);

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: copy (synchronous) 
    // 
    // Possible parameters:
    //
    // AbsolutePath null / non-existing file / existing empty file / existing non-empty file / existing dir / closed filesystem
    // CopyOptions  null / CREATE / REPLACE / IGNORE / APPEND / RESUME / VERIFY / ASYNCHRONOUS 
    // 
    // Total combinations : N
    // 
    // Depends on: 

    @org.junit.Test
    public void test25_getLocalCWDFileSystem() throws Exception {

        if (config.supportsLocalCWDFileSystem()) {

            prepare();

            try {
                files.getLocalCWDFileSystem();
            } catch (Exception e) {
                throwUnexpected("test25_getLocalCWDFileSystem", e);
            }

            cleanup();
        }
    }

    @org.junit.Test
    public void test26_getLocalHomeFileSystem() throws Exception {

        if (config.supportsLocalHomeFileSystem()) {
            prepare();

            try {
                files.getLocalHomeFileSystem();
            } catch (Exception e) {
                throwUnexpected("test26_getLocalHomeFileSystem", e);
            }

            cleanup();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: move 
    // 
    // Possible parameters:
    //
    // source null / non-existing file / existing file / existing dir
    // target null / non-existing file / existing file / non-existing parent dir / existing dir 
    // +  closed filesystem
    // 
    // Total combinations : 
    // 
    // Depends on: 

    private void test27_move(AbsolutePath source, AbsolutePath target, boolean mustFail) throws Exception {

        try {
            files.move(source, target);
        } catch (Exception e) {

            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test27_move", e);
        }

        if (mustFail) {
            throwExpected("test27_move");
        }

        if (source.normalize().equals(target.normalize())) {
            // source == target, so the move did nothing. 
            return;
        }

        // make sure the source no longer exists, and the target does exist
        if (files.exists(source)) {
            throwWrong("test27_move", "no source file", "source file");
        }

        if (!files.exists(target)) {
            throwWrong("test27_move", "target file", "no target file");
        }
    }

    @org.junit.Test
    public void test27_move() throws Exception {

        prepare();

        test27_move(null, null, true);

        FileSystem fs = config.getTestFileSystem(files, credentials);
        prepareTestDir(fs, "test27_move");

        // test with non-existing source
        AbsolutePath file0 = createNewTestFileName(testDir);
        AbsolutePath file1 = createNewTestFileName(testDir);
        test27_move(file0, file1, true);

        // test with existing source, non-existing target
        AbsolutePath file2 = createTestFile(testDir, null);
        test27_move(file2, file0, false);

        // test with existing source and target
        AbsolutePath file3 = createTestFile(testDir, null);
        test27_move(file3, file0, true);

        // test file existing source, and target with non-existing parent
        AbsolutePath dir0 = createNewTestDirName(testDir);
        AbsolutePath file4 = createNewTestFileName(dir0);

        test27_move(file0, file4, true);

        // test with source equals target
        test27_move(file0, file0, false);

        deleteTestFile(file0);
        deleteTestFile(file3);

        // test with existing dir
        AbsolutePath dir1 = createTestDir(testDir);
        test27_move(dir1, file1, false);

        deleteTestDir(file1);
        deleteTestDir(testDir);

        cleanup();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // TEST: readSymbolicLink 
    // 
    // Possible parameters:
    //
    // link null / non-existing file / existing file / existing dir / existing link / broken link / closed filesystem
    // 
    // Total combinations : 7
    // 
    // Depends on: 

    private void test28_readSymbolicLink(AbsolutePath link, AbsolutePath expected, boolean mustFail) throws Exception {

        AbsolutePath target = null;

        try {
            target = files.readSymbolicLink(link);
        } catch (Exception e) {

            if (mustFail) {
                // expected
                return;
            }

            throwUnexpected("test28_readSymboliclink", e);
        }

        if (mustFail) {
            throwExpected("test28_readSymbolicLink");
        }

        // make sure the target is what was expected 
        if (expected != null && !target.equals(expected)) {
            throwWrong("test28_readSymbolicLink", expected.getPath(), target.getPath());
        }
    }

    @org.junit.Test
    public void test28_readSymbolicLink() throws Exception {

        prepare();

        // test with null
        test28_readSymbolicLink(null, null, true);

        FileSystem fs = config.getTestFileSystem(files, credentials);
        prepareTestDir(fs, "test28_readSybmolicLink");

        // test with non-exising file
        AbsolutePath file0 = createNewTestFileName(testDir);
        test28_readSymbolicLink(file0, null, true);

        // test with existing file
        AbsolutePath file1 = createTestFile(testDir, null);
        test28_readSymbolicLink(file1, null, true);
        deleteTestFile(file1);

        // test with existing dir
        AbsolutePath dir0 = createTestDir(testDir);
        test28_readSymbolicLink(dir0, null, true);

        deleteTestDir(dir0);
        deleteTestDir(testDir);

        if (config.supportsClose()) {
            files.close(fs);
            // TODO
        }

        cleanup();
    }

    @org.junit.Test
    public void test29_readSymbolicLink() throws Exception {

        prepare();

        FileSystem fs = config.getTestFileSystem(files, credentials);

        // Use external test dir with is assumed to be in fs.getEntryPath().resolve("octopus_test/links");
        AbsolutePath root = fs.getEntryPath().resolve(new RelativePath("octopus_test/links"));

        if (!files.exists(root)) {
            throw new Exception("Cannot find symbolic link test dir at " + root.getPath());
        }

        // prepare the test files 
        AbsolutePath file0 = root.resolve(new RelativePath("file0")); // exists
        AbsolutePath file1 = root.resolve(new RelativePath("file1")); // exists
        AbsolutePath file2 = root.resolve(new RelativePath("file2")); // does not exist

        // prepare the test links 
        AbsolutePath link0 = root.resolve(new RelativePath("link0")); // points to file0 (contains text) 
        AbsolutePath link1 = root.resolve(new RelativePath("link1")); // points to file1 (is empty)
        AbsolutePath link2 = root.resolve(new RelativePath("link2")); // points to non-existing file2 
        AbsolutePath link3 = root.resolve(new RelativePath("link3")); // points to link0 which points to file0 (contains text)
        AbsolutePath link4 = root.resolve(new RelativePath("link4")); // points to link2 which points to non-existing file2 
        AbsolutePath link5 = root.resolve(new RelativePath("link5")); // points to link6 (circular)  
        AbsolutePath link6 = root.resolve(new RelativePath("link6")); // points to link5 (circular)

        // link0 should point to file0
        test28_readSymbolicLink(link0, file0, false);

        // link1 should point to file1
        test28_readSymbolicLink(link1, file1, false);

        // link2 should point to file2 which fails
        test28_readSymbolicLink(link2, file2, false);

        // link3 should point to link0 which points to file0
        test28_readSymbolicLink(link3, link0, false);

        // link4 should point to link2 which points to file2
        test28_readSymbolicLink(link4, link2, false);

        // link5 should point to link6 which points to link5
        test28_readSymbolicLink(link5, link6, false);

        // link6 should point to link5 which points to link6
        test28_readSymbolicLink(link6, link5, false);

        cleanup();
    }

    @org.junit.Test
    public void test30_isSymbolicLink() throws Exception {

        prepare();

        FileSystem fs = config.getTestFileSystem(files, credentials);

        // Use external test dir with is assumed to be in fs.getEntryPath().resolve("octopus_test/links");
        AbsolutePath root = fs.getEntryPath().resolve(new RelativePath("octopus_test/links"));

        if (!files.exists(root)) {
            throw new Exception("Cannot find symbolic link test dir at " + root.getPath());
        }

        // prepare the test files
        boolean v = files.isSymbolicLink(root.resolve(new RelativePath("file0")));
        assertFalse(v);

        v = files.isSymbolicLink(root.resolve(new RelativePath("link0")));
        assertTrue(v);

        v = files.isSymbolicLink(root.resolve(new RelativePath("file2")));
        assertFalse(v);

        cleanup();
    }

    @org.junit.Test
    public void test31_newDirectoryStreamWithBrokenLinks() throws Exception {

        prepare();

        FileSystem fs = config.getTestFileSystem(files, credentials);

        // Use external test dir with is assumed to be in fs.getEntryPath().resolve("octopus_test/links");
        AbsolutePath root = fs.getEntryPath().resolve(new RelativePath("octopus_test/links"));

        if (!files.exists(root)) {
            throw new Exception("Cannot find symbolic link test dir at " + root.getPath());
        }

        // prepare the test files 
        AbsolutePath file0 = root.resolve(new RelativePath("file0")); // exists
        AbsolutePath file1 = root.resolve(new RelativePath("file1")); // exists

        // prepare the test links 
        AbsolutePath link0 = root.resolve(new RelativePath("link0")); // points to file0 (contains text) 
        AbsolutePath link1 = root.resolve(new RelativePath("link1")); // points to file1 (is empty)
        AbsolutePath link2 = root.resolve(new RelativePath("link2")); // points to non-existing file2 
        AbsolutePath link3 = root.resolve(new RelativePath("link3")); // points to link0 which points to file0 (contains text)
        AbsolutePath link4 = root.resolve(new RelativePath("link4")); // points to link2 which points to non-existing file2 
        AbsolutePath link5 = root.resolve(new RelativePath("link5")); // points to link6 (circular)  
        AbsolutePath link6 = root.resolve(new RelativePath("link6")); // points to link5 (circular)

        Set<AbsolutePath> tmp = new HashSet<AbsolutePath>();
        tmp.add(file0);
        tmp.add(file1);
        tmp.add(link0);
        tmp.add(link1);
        tmp.add(link2);
        tmp.add(link3);
        tmp.add(link4);
        tmp.add(link5);
        tmp.add(link6);

        test11_newDirectoryStream(root, tmp, false);

        cleanup();
    }

    @org.junit.Test
    public void test32_newAttributesDirectoryStreamWithBrokenLinks() throws Exception {

        prepare();

        FileSystem fs = config.getTestFileSystem(files, credentials);

        // Use external test dir with is assumed to be in fs.getEntryPath().resolve("octopus_test/links");
        AbsolutePath root = fs.getEntryPath().resolve(new RelativePath("octopus_test/links"));

        if (!files.exists(root)) {
            throw new Exception("Cannot find symbolic link test dir at " + root.getPath());
        }

        // prepare the test files 
        AbsolutePath file0 = root.resolve(new RelativePath("file0")); // exists
        AbsolutePath file1 = root.resolve(new RelativePath("file1")); // exists

        // prepare the test links 
        AbsolutePath link0 = root.resolve(new RelativePath("link0")); // points to file0 (contains text) 
        AbsolutePath link1 = root.resolve(new RelativePath("link1")); // points to file1 (is empty)
        AbsolutePath link2 = root.resolve(new RelativePath("link2")); // points to non-existing file2 
        AbsolutePath link3 = root.resolve(new RelativePath("link3")); // points to link0 which points to file0 (contains text)
        AbsolutePath link4 = root.resolve(new RelativePath("link4")); // points to link2 which points to non-existing file2 
        AbsolutePath link5 = root.resolve(new RelativePath("link5")); // points to link6 (circular)  
        AbsolutePath link6 = root.resolve(new RelativePath("link6")); // points to link5 (circular)

        Set<PathAttributesPair> tmp = new HashSet<PathAttributesPair>();
        tmp.add(new PathAttributesPairImplementation(file0, files.getAttributes(file0)));
        tmp.add(new PathAttributesPairImplementation(file1, files.getAttributes(file1)));
        tmp.add(new PathAttributesPairImplementation(link0, files.getAttributes(link0)));
        tmp.add(new PathAttributesPairImplementation(link1, files.getAttributes(link1)));
        tmp.add(new PathAttributesPairImplementation(link2, files.getAttributes(link2)));
        tmp.add(new PathAttributesPairImplementation(link3, files.getAttributes(link3)));
        tmp.add(new PathAttributesPairImplementation(link4, files.getAttributes(link4)));
        tmp.add(new PathAttributesPairImplementation(link5, files.getAttributes(link5)));
        tmp.add(new PathAttributesPairImplementation(link6, files.getAttributes(link6)));

        test15_newAttributesDirectoryStream(root, tmp, false);

        cleanup();
    }

    /*        
    public AbsolutePath readSymbolicLink(AbsolutePath link) throws OctopusIOException;

    public boolean isSymbolicLink(AbsolutePath path) throws OctopusIOException;
    
     
    */

}
