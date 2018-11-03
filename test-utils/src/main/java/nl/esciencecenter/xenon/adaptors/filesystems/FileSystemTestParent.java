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
package nl.esciencecenter.xenon.adaptors.filesystems;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.adaptors.NotConnectedException;
import nl.esciencecenter.xenon.filesystems.CopyMode;
import nl.esciencecenter.xenon.filesystems.CopyStatus;
import nl.esciencecenter.xenon.filesystems.DirectoryNotEmptyException;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.InvalidPathException;
import nl.esciencecenter.xenon.filesystems.NoSuchCopyException;
import nl.esciencecenter.xenon.filesystems.NoSuchPathException;
import nl.esciencecenter.xenon.filesystems.Path;
import nl.esciencecenter.xenon.filesystems.PathAlreadyExistsException;
import nl.esciencecenter.xenon.filesystems.PathAttributes;
import nl.esciencecenter.xenon.filesystems.PosixFilePermission;
import nl.esciencecenter.xenon.utils.LocalFileSystemUtils;
import nl.esciencecenter.xenon.utils.OutputReader;

public abstract class FileSystemTestParent extends FileSystemTestInfrastructure {

    @Test
    public void exists_fileDoesExist_fileExists() throws XenonException {
        Path path = locationConfig.getExistingPath();
        assertTrue(path.toString(), fileSystem.exists(path));
    }

    @Test
    public void readSymbolicLink_linkToExistingFile_targetMatches() throws XenonException {
        assumeTrue("Does not support reading of symlinks", description.canReadSymboliclinks());
        Map.Entry<Path, Path> linkTarget = locationConfig.getSymbolicLinksToExistingFile();
        Path target = fileSystem.readSymbolicLink(linkTarget.getKey());
        Path expectedTarget = linkTarget.getValue();
        assertThat(target, is(expectedTarget));
    }

    // Tests to create directories

    @Test
    public void test_exists_ok() throws Exception {
        assertTrue(fileSystem.exists(locationConfig.getExistingPath()));
    }

    @Test
    public void test_exists_testdir_ok() throws Exception {
        assertTrue(fileSystem.exists(locationConfig.getWritableTestDir()));
    }

    @Test
    public void test_exists_notExistsDir() throws Exception {
        assertFalse(fileSystem.exists(new Path("/foobar")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_createDirectory_null_throw() throws Exception {
        fileSystem.createDirectory(null);
    }

    @Test
    public void test_createDirectory_nonExisting_noThrow() throws Exception {
        generateAndCreateTestDir();
        assertTrue(fileSystem.exists(testDir));
    }

    @Test(expected = PathAlreadyExistsException.class)
    public void createDirectory_existing_throw() throws Exception {
        generateAndCreateTestDir();
        fileSystem.createDirectory(testDir);
    }

    @Test(expected = PathAlreadyExistsException.class)
    public void test_createDirectory_existingFile_throw() throws Exception {
        generateAndCreateTestDir();

        String file = generateTestFileName();

        Path tmp = testDir.resolve(file);
        fileSystem.createFile(tmp);

        // Should fail!
        fileSystem.createDirectory(tmp);
    }

    @Test(expected = NoSuchPathException.class)
    public void test_createDirectory_nonExistingParent_throw() throws Exception {
        generateAndCreateTestDir();
        fileSystem.createDirectory(testDir.resolve(new Path(true, "aap", "noot")));
    }

    // @Test(expected=XenonException.class)
    // public void createDirectory_closedFileSystemIfSupported_throw() throws
    // Exception {
    // assumeFalse(description.isConnectionless());
    // fileSystem.close();
    // generateAndCreateTestDir();
    // }

    @Test(expected = IllegalArgumentException.class)
    public void test_createFile_null_throwsException() throws Exception {
        fileSystem.createFile(null);
    }

    @Test
    public void test_createFile_nonExistingFile() throws Exception {
        generateAndCreateTestDir();
        Path file = testDir.resolve(generateTestFileName());
        fileSystem.createFile(file);
    }

    @Test(expected = PathAlreadyExistsException.class)
    public void test_createFile_existingFile_throwsException() throws Exception {
        generateAndCreateTestDir();
        Path file = testDir.resolve(generateTestFileName());
        fileSystem.createFile(file);
        fileSystem.createFile(file);
    }

    @Test(expected = PathAlreadyExistsException.class)
    public void test_createFile_existingDir_throwsException() throws Exception {
        generateAndCreateTestDir();
        fileSystem.createFile(testDir);
    }

    @Test(expected = NoSuchPathException.class)
    public void test_createFile_nonExistingParent_throwsException() throws Exception {
        generateAndCreateTestDir();
        Path dir = testDir.resolve(generateTestDirName());
        Path file = dir.resolve(generateTestFileName());
        fileSystem.createFile(file);
    }

    @Test
    public void test_createFile_empty() throws Exception {
        generateAndCreateTestDir();
        Path file = testDir.resolve(generateTestFileName());
        fileSystem.createFile(file);
        assertTrue("File does not exist!", fileSystem.exists(file));
    }

    @Test
    public void test_createFile_withData_sizeUnknown() throws Exception {
        assumeTrue(!description.needsSizeBeforehand());
        byte[] data = "Hello World\n".getBytes();

        generateAndCreateTestDir();
        Path file = testDir.resolve(generateTestFileName());

        OutputStream out = fileSystem.writeToFile(file);
        out.write(data);
        out.close();

        ensureUpToDate(file, data, 5000);
    }

    @Test
    public void test_createFile_withData_sizeKnown() throws Exception {

        byte[] data = "Hello World\n".getBytes();

        generateAndCreateTestDir();
        Path file = testDir.resolve(generateTestFileName());

        OutputStream out = fileSystem.writeToFile(file, data.length);
        out.write(data);
        out.close();

        ensureUpToDate(file, data, 5000);
    }

    @Test
    public void test_readFromFile() throws Exception {

        Path file = locationConfig.getExistingPath();

        OutputReader reader = new OutputReader(fileSystem.readFromFile(file));

        reader.waitUntilFinished();

        // System.out.println("READ: " + reader.getResultAsString());

        String expected;

        if (fileSystem.getAdaptorName().equals("file") && LocalFileSystemUtils.isWindows()) {
            // We are testing locally on windows and therefore need to us a different newline.
            expected = "Hello World \r\n";
        } else {
            // We assume testing on linux or mac
            expected = "Hello World\n";
        }

        assertEquals(expected, reader.getResultAsString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_exists_null_throwsException() throws Exception {
        fileSystem.exists(null);
    }

    @Test
    public void test_exists_nonExistent_returnFalse() throws Exception {
        generateAndCreateTestDir();

        // test with non-existing file
        Path file0 = createNewTestFileName(testDir);
        assertFalse(fileSystem.exists(file0));
    }

    @Test
    public void test_exists_existingFile_returnTrue() throws Exception {
        generateAndCreateTestDir();

        Path file0 = createNewTestFileName(testDir);
        fileSystem.createFile(file0);
        assertTrue(fileSystem.exists(file0));
    }

    @Test
    public void test_exists_existingRelativeFile_returnTrue() throws Exception {
        generateAndCreateTestDir();
        Path file0 = createNewTestFileName(testDir);
        fileSystem.createFile(file0);

        // System.out.println("FILE0 " + file0);
        // System.out.println("CONFIG " + locationConfig);
        // System.out.println("WORKDIR " + locationConfig.getExpectedWorkingDirectory());

        Path relFile0 = locationConfig.getExpectedWorkingDirectory().relativize(file0);

        assertTrue(fileSystem.exists(relFile0));
    }

    @Test
    public void test_exists_existingDir_returnTrue() throws Exception {
        generateAndCreateTestDir();

        createNewTestFileName(testDir);
        assertTrue(fileSystem.exists(testDir));
    }

    @Test
    public void test_exists_existingRelativeDir_returnTrue() throws Exception {
        generateAndCreateTestDir();

        createNewTestFileName(testDir);

        Path relDir = locationConfig.getExpectedWorkingDirectory().relativize(testDir);

        assertTrue(fileSystem.exists(relDir));
    }

    @Test
    public void test_exists_existingSymbolicLink_returnTrue() throws Exception {
        assumeTrue(description.canCreateSymboliclinks());

        generateAndCreateTestDir();

        Path file0 = createNewTestFileName(testDir);
        Path link = createNewTestFileName(testDir);
        fileSystem.createFile(file0);
        fileSystem.createSymbolicLink(link, file0);
        assertTrue(fileSystem.exists(link));
    }

    @Test
    @Ignore("Dunno how to create other?")
    public void test_exists_existingOther_returnTrue() throws Exception {

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_delete_nonRec_null_throwsException() throws Exception {
        fileSystem.delete(null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_delete_rec_null_throwsException() throws Exception {
        fileSystem.delete(null, true);
    }

    @Test(expected = NoSuchPathException.class)
    public void test_delete_nonExistentFile_throwsException() throws Exception {
        generateAndCreateTestDir();
        Path nonExistent = createNewTestFileName(testDir);
        fileSystem.delete(nonExistent, false);
    }

    @Test
    public void test_delete_symlink() throws Exception {
        assumeTrue(description.canCreateSymboliclinks());
        generateAndCreateTestDir();
        Path file = createNewTestFileName(testDir);
        Path link = createNewTestFileName(testDir);
        fileSystem.createFile(file);
        fileSystem.createSymbolicLink(link, file);
        fileSystem.delete(link, false);
        assertTrue("Target not deleted", fileSystem.exists(file));
        assertFalse("Link deleted", fileSystem.exists(link));
    }

    @Test
    public void test_delete_dangling_symlink() throws Exception {
        assumeTrue(description.canCreateSymboliclinks());
        generateAndCreateTestDir();
        Path file = createNewTestFileName(testDir);
        Path link = createNewTestFileName(testDir);
        fileSystem.createFile(file);
        fileSystem.createSymbolicLink(link, file);
        fileSystem.delete(file, false);

        fileSystem.delete(link, false);

        assertFalse("Target deleted", fileSystem.exists(file));
        assertFalse("Link deleted", fileSystem.exists(link));
    }

    @Test
    public void test_delete_existingFile() throws Exception {
        generateAndCreateTestDir();
        Path file = createNewTestFileName(testDir);
        fileSystem.createFile(file);
        fileSystem.delete(file, false);
        assertFalse(fileSystem.exists(file));
    }

    @Test
    public void test_delete_existingEmptyDir() throws Exception {
        generateAndCreateTestDir();
        fileSystem.delete(testDir, false);
        assertFalse(fileSystem.exists(testDir));
    }

    @Test(expected = DirectoryNotEmptyException.class)
    public void test_delete_existingNonEmptyDir_throwsException() throws Exception {
        generateAndCreateTestDir();
        Path file = createNewTestFileName(testDir);
        Path file2 = createNewTestFileName(testDir);
        fileSystem.createFile(file);
        fileSystem.createFile(file2);
        fileSystem.delete(testDir, false);
    }

    @Test
    public void test_delete_Rec_existingNonEmptyDir_throwsException() throws Exception {
        generateAndCreateTestDir();
        Path file = createNewTestFileName(testDir);
        Path file2 = createNewTestFileName(testDir);
        fileSystem.createFile(file);
        fileSystem.createFile(file2);
        fileSystem.delete(testDir, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_list_nullPath_throwsException() throws Exception {
        fileSystem.list(null, true);
    }

    @Test(expected = NoSuchPathException.class)
    public void test_list_nonExistentDir_throwsException() throws Exception {
        generateAndCreateTestDir();
        Path nonExistent = createNewTestFileName(testDir);
        fileSystem.list(nonExistent, false);
    }

    @Test(expected = InvalidPathException.class)
    public void test_list_file_throwsException() throws Exception {
        generateAndCreateTestDir();
        Path file = createTestFile(testDir, null);
        fileSystem.list(file, false);
    }

    @Test(expected = InvalidPathException.class)
    public void test_list_symlinkToFile_throwsException() throws Exception {
        assumeTrue(description.canCreateSymboliclinks());
        generateAndCreateTestDir();
        Path file = createTestFile(testDir, null);
        Path link = createNewTestFileName(testDir);
        fileSystem.createSymbolicLink(link, file);
        fileSystem.list(link, false);
    }

    @Test(expected = InvalidPathException.class)
    public void test_list_symlinkToDir_throwsException() throws Exception {
        assumeTrue(description.canCreateSymboliclinks());
        generateAndCreateTestDir();
        Path link = createNewTestFileName(testDir);
        fileSystem.createSymbolicLink(link, testDir);
        fileSystem.list(link, false);
    }

    @Test
    public void test_list_canIterateTwice() throws Exception {
        generateAndCreateTestDir();
        byte[] data = "Hello World!".getBytes();
        byte[] data2 = "Party people!".getBytes();
        byte[] data3 = "yes | rm -rf ".getBytes();
        byte[] data4 = "Use Xenon!".getBytes();

        Path source = createTestSubDir(testDir);
        createTestFile(source, data);

        Path testSubDir = createTestSubDir(source);
        Path testSubDir2 = createTestSubDir(source);
        createTestFile(testSubDir2, data2);
        createTestFile(testSubDir, data3);

        Path testSubSub = createTestSubDir(testSubDir);
        createTestFile(testSubSub, data4);
        createTestSubDir(testDir);

        Iterable<PathAttributes> pa = fileSystem.list(testDir, true);
        Iterator<PathAttributes> it1 = pa.iterator();
        Iterator<PathAttributes> it2 = pa.iterator();
        Set<PathAttributes> set1 = new HashSet<>();
        while (it1.hasNext()) {
            set1.add(it1.next());
        }
        Set<PathAttributes> set2 = new HashSet<>();
        while (it2.hasNext()) {
            set2.add(it2.next());
        }
        assertEquals(set1, set2);
    }

    @Test
    public void test_list_existingEmptyDir() throws Exception {
        generateAndCreateTestDir();
        Set<PathAttributes> emptySet = new HashSet<>();
        Set<PathAttributes> res = listSet(testDir, false);
        assertListSetEqual(res, emptySet);
    }

    @Test
    public void test_list_existingFile() throws Exception {
        generateAndCreateTestDir();
        Path file = createNewTestFileName(testDir);
        Set<PathAttributes> expected = new HashSet<>();
        fileSystem.createFile(file);
        expected.add(fileSystem.getAttributes(file));
        Set<PathAttributes> res = listSet(testDir, false);
        assertListSetEqual(res, expected);
    }

    @Test
    public void test_list_nonEmptyDir_correctListing() throws Exception {
        generateAndCreateTestDir();
        Path file0 = createTestFile(testDir, null);
        Path file1 = createTestFile(testDir, null);
        Path file2 = createTestFile(testDir, null);
        Path file3 = createTestFile(testDir, null);
        Set<PathAttributes> expected = new HashSet<>(6);
        expected.add(fileSystem.getAttributes(file0));
        expected.add(fileSystem.getAttributes(file1));
        expected.add(fileSystem.getAttributes(file2));
        expected.add(fileSystem.getAttributes(file3));

        Set<PathAttributes> res = listSet(testDir, false);
        assertListSetEqual(res, expected);
    }

    @Test
    public void test_list_withSubDirs_onlyListTopDirContents() throws Exception {
        generateAndCreateTestDir();
        Path file0 = createTestFile(testDir, null);
        Path file1 = createTestFile(testDir, null);
        Path file2 = createTestFile(testDir, null);
        Path file3 = createTestFile(testDir, null);
        Path subDir = createTestSubDir(testDir);
        createTestFile(subDir, null);
        Set<PathAttributes> expected = new HashSet<>(7);
        expected.add(fileSystem.getAttributes(file0));
        expected.add(fileSystem.getAttributes(file1));
        expected.add(fileSystem.getAttributes(file2));
        expected.add(fileSystem.getAttributes(file3));
        expected.add(fileSystem.getAttributes(subDir));

        Set<PathAttributes> res = listSet(testDir, false);
        assertListSetEqual(res, expected);

    }

    @Test
    public void test_list_recursive_withSubDirs_listAll() throws Exception {
        generateAndCreateTestDir();

        Path file0 = createTestFile(testDir, null);
        Path file1 = createTestFile(testDir, null);
        Path file2 = createTestFile(testDir, null);
        Path file3 = createTestFile(testDir, null);
        Path subDir = createTestSubDir(testDir);
        Path irrelevantFileInSubDir = createTestFile(subDir, null);
        Set<PathAttributes> expected = new HashSet<>(7);
        expected.add(fileSystem.getAttributes(file0));
        expected.add(fileSystem.getAttributes(file1));
        expected.add(fileSystem.getAttributes(file2));
        expected.add(fileSystem.getAttributes(file3));
        expected.add(fileSystem.getAttributes(subDir));
        expected.add(fileSystem.getAttributes(irrelevantFileInSubDir));

        Set<PathAttributes> res = listSet(testDir, true);
        assertListSetEqual(res, expected);

    }

    @Test
    public void test_list_recursive() throws Exception {
        byte[] data = "Hello World!".getBytes();
        byte[] data2 = "Party people!".getBytes();
        byte[] data3 = "yes | rm -rf ".getBytes();
        byte[] data4 = "Use Xenon!".getBytes();

        generateAndCreateTestDir();

        Set<PathAttributes> expected = new HashSet<>();

        Path source = createTestSubDir(testDir);
        expected.add(fileSystem.getAttributes(source));

        Path file0 = createTestFile(source, data);
        expected.add(fileSystem.getAttributes(file0));

        Path testSubDir = createTestSubDir(source);
        expected.add(fileSystem.getAttributes(testSubDir));

        Path testSubDir2 = createTestSubDir(source);
        expected.add(fileSystem.getAttributes(testSubDir2));

        Path file1 = createTestFile(testSubDir2, data2);
        expected.add(fileSystem.getAttributes(file1));

        Path file2 = createTestFile(testSubDir, data3);
        expected.add(fileSystem.getAttributes(file2));

        Path testSubSub = createTestSubDir(testSubDir);
        expected.add(fileSystem.getAttributes(testSubSub));

        Path file3 = createTestFile(testSubSub, data4);
        expected.add(fileSystem.getAttributes(file3));

        assertListSetEqual(listSet(testDir, true), expected);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_getAttributes_nullPath_throwsException() throws Exception {
        fileSystem.getAttributes(null);
    }

    @Test(expected = NoSuchPathException.class)
    public void test_getAttributes_nonExistingFile_throwsException() throws Exception {
        generateAndCreateTestDir();
        // test with non-existing file
        Path file0 = createNewTestFileName(testDir);
        fileSystem.getAttributes(file0);
    }

    private void assertPathAttributesConsistent(Path path, boolean isDirectory, long size, long currentTime) throws Exception {

        PathAttributes result = fileSystem.getAttributes(path);

        if (result.isDirectory() && !isDirectory) {
            throwWrong("test_getfileAttributes", "<not directory>", "<directory>");
        }

        if (size >= 0 && result.getSize() != size) {
            throwWrong("test_getfileAttributes", "size=" + size, "size=" + result.getSize());
        }

        if (!isWithinMargin(currentTime, result.getLastModifiedTime()) && result.getLastModifiedTime() != 0) {
            throwWrong("test_getfileAttributes", "lastModifiedTime=" + currentTime, "lastModifiedTime=" + result.getLastModifiedTime());

        }
        if (!isWithinMargin(currentTime, result.getCreationTime()) && result.getCreationTime() != result.getLastModifiedTime()) {
            throwWrong("test_getfileAttributes", "creationTime=" + currentTime, "creationTime=" + result.getCreationTime());
        }

        if (!isWithinMargin(currentTime, result.getLastAccessTime()) && result.getLastAccessTime() != result.getLastModifiedTime()) {
            throwWrong("test13_getfileAttributes", "lastAccessTime=" + currentTime, "lastAccessTime=" + result.getLastAccessTime());
        }

    }

    /*
     *
     *
     *
     *
     *
     * /** Tests whether two times (in milliseconds) are within a mild margin of one another. The margin is large enough to be able to cope with servers in
     * other timezones and similar, expected, sources of discrepancy between times.
     *
     * @param time1
     *
     * @param time2
     *
     * @return
     */
    private boolean isWithinMargin(long time1, long time2) {
        final int millisecondsPerSecond = 1000;
        final int secondsPerHour = 3600;
        final long margin = 60 * secondsPerHour * millisecondsPerSecond;
        return Math.abs(time1 - time2) < margin;
    }

    @Test
    public void test_getAttributes_emptyFile() throws Exception {
        long currentTime = System.currentTimeMillis();
        generateAndCreateTestDir();
        // test with non-existing file
        Path file0 = createNewTestFileName(testDir);
        fileSystem.createFile(file0);
        assertPathAttributesConsistent(file0, false, 0, currentTime);
    }

    @Test
    public void test_getAttributes_nonEmptyFile() throws Exception {
        long currentTime = System.currentTimeMillis();
        generateAndCreateTestDir();
        Path nonEmptyFile = createTestFile(testDir, new byte[] { 1, 2, 3 });

        assertPathAttributesConsistent(nonEmptyFile, false, 3, currentTime);
    }

    @Test
    public void test_getAttributes_existingDir() throws Exception {
        long currentTime = System.currentTimeMillis();
        generateAndCreateTestDir();

        assertPathAttributesConsistent(testDir, true, -1, currentTime);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_setPosixFilePermissions_nullPath_throwsException() throws Exception {
        assumeTrue(description.supportsSettingPosixPermissions());
        fileSystem.setPosixFilePermissions(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_setPosixFilePermissions_existingFileNullSet_throwsException() throws Exception {
        assumeTrue(description.supportsSettingPosixPermissions());
        generateAndCreateTestDir();
        Path existingFile = createTestFile(testDir, new byte[] { 1, 2, 3 });
        fileSystem.setPosixFilePermissions(existingFile, null);
    }

    @Test
    public void test_setPosixFilePermissions_existingFileZeroPermissions() throws Exception {
        assumeTrue(description.supportsSettingPosixPermissions());
        generateAndCreateTestDir();
        Path existingFile = createTestFile(testDir, new byte[] { 1, 2, 3 });
        Set<PosixFilePermission> emptyPermissions = EnumSet.noneOf(PosixFilePermission.class);
        try {
            assertPermissionsSetIsGet(existingFile, emptyPermissions);
        } finally {
            // Set the permissions to write again before we can remove it
            fileSystem.setPosixFilePermissions(existingFile, getVariousPosixPermissions());
        }
    }

    private void assertPermissionsSetIsGet(Path path, Set<PosixFilePermission> permissions) throws XenonException {
        assumeTrue(description.supportsSettingPosixPermissions());
        fileSystem.setPosixFilePermissions(path, permissions);
        Set<PosixFilePermission> got = fileSystem.getAttributes(path).getPermissions();
        assertEquals(permissions, got);
    }

    @Test
    public void test_setPosixFilePermissions_existingFileFewPermissions() throws Exception {
        assumeTrue(description.supportsSettingPosixPermissions());
        generateAndCreateTestDir();
        Path existingFile = createTestFile(testDir, new byte[] { 1, 2, 3 });
        Set<PosixFilePermission> permissions = EnumSet.of(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
        try {
            assertPermissionsSetIsGet(existingFile, permissions);
        } finally {
            // Set the permissions to write again before we can remove it
            fileSystem.setPosixFilePermissions(existingFile, getVariousPosixPermissions());
        }

    }

    @Test
    public void test_setPosixFilePermissions_existingFileMorePermissions() throws Exception {
        assumeTrue(description.supportsSettingPosixPermissions());
        generateAndCreateTestDir();
        Path existingFile = createTestFile(testDir, new byte[] { 1, 2, 3 });
        Set<PosixFilePermission> permissions = getVariousPosixPermissions();
        try {
            assertPermissionsSetIsGet(existingFile, permissions);
        } finally {
            // Set the permissions to write again before we can remove it
            fileSystem.setPosixFilePermissions(existingFile, getVariousPosixPermissions());
        }
    }

    @Test(expected = NoSuchPathException.class)
    public void test_setPosixFilePermissions_nonExistingFile_throwsException() throws Exception {
        assumeTrue(description.supportsSettingPosixPermissions());
        generateAndCreateTestDir();
        Path nonExistingFile = createNewTestFileName(testDir);
        Set<PosixFilePermission> permissions = getVariousPosixPermissions();
        fileSystem.setPosixFilePermissions(nonExistingFile, permissions);
    }

    @Test
    public void test_setPosixFilePermissions_existingDir() throws Exception {
        assumeTrue(description.supportsSettingPosixPermissions());
        generateAndCreateTestDir();
        Set<PosixFilePermission> permissions = getVariousPosixPermissions();
        assertPermissionsSetIsGet(testDir, permissions);
    }

    @Test
    public void test_readPosixFilePermissions_existingFile() throws Exception {
        assumeTrue(description.supportsReadingPosixPermissions());
        PathAttributes p = fileSystem.getAttributes(locationConfig.getExistingPath());
        assertNotNull(p.getPermissions());
    }

    private Set<PosixFilePermission> getVariousPosixPermissions() {
        return EnumSet.of(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OTHERS_READ,
                PosixFilePermission.GROUP_READ);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_readFromFile_null_throwsException() throws Exception {
        fileSystem.readFromFile(null);
    }

    @Test(expected = NoSuchPathException.class)
    public void test_readFromFile_nonExistingFile_throwsException() throws Exception {
        generateAndCreateTestDir();
        Path nonExistingFile = createNewTestFileName(testDir);
        fileSystem.readFromFile(nonExistingFile);
    }

    @Test
    public void test_readFromFile_existingEmptyFile() throws Exception {
        generateAndCreateTestDir();
        Path file1 = createTestFile(testDir, null);
        assertReadsExpected(file1, null);
    }

    @Test
    public void test_readFromFile_existingNonEmptyFile() throws Exception {
        byte[] data = "Hello World".getBytes();
        generateAndCreateTestDir();
        Path file2 = createTestFile(testDir, data);

        assertReadsExpected(file2, data);

    }

    @Test(expected = InvalidPathException.class)
    public void test_readFromFile_existingDir_throw() throws Exception {
        generateAndCreateTestDir();
        fileSystem.readFromFile(testDir);
    }

    @Test
    public void test_readFromFile_DoubleClose() throws Exception {

        // See what happens when we close an in input stream twice and then
        // reopen the stream. This failed
        // on the SSH adaptor due to a bug in the sftp channel cache.

        byte[] data = "Hello World".getBytes();
        generateAndCreateTestDir();
        Path file = createTestFile(testDir, data);

        InputStream in = null;

        try {
            in = fileSystem.readFromFile(file);
        } catch (Exception e) {
            // should not fail
            throwUnexpected("test_readFromFile_DoubleClose", e);
        }

        try {
            // should not fail
            in.close();
        } catch (Exception e) {
            throwUnexpected("test_readFromFile_DoubleClose", e);
        }

        try {
            in.close();
        } catch (Exception e) {
            // should fail
        }

        try {
            in = fileSystem.readFromFile(file);
        } catch (Exception e) {
            // should not fail
            throwUnexpected("test_readFromFile_DoubleClose", e);
        }

        try {
            in.close();
        } catch (Exception e) {
            // should not fail
            throwUnexpected("test20b_newInputStreamDoubleClose", e);
        }

    }

    // TODO: not connected exceptions test for every function
    // TODO: not test destination parent exception for every function
    @Test(expected = NotConnectedException.class)
    public void test_readFromFile_closed_throwsException() throws Exception {
        assumeFalse(description.isConnectionless());
        fileSystem.close();
        fileSystem.readFromFile(locationConfig.getExistingPath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_writeToFile_nullPath_throwsException() throws Exception {
        fileSystem.writeToFile(null, 0);
    }

    @Test(expected = PathAlreadyExistsException.class)
    public void test_writeToFile_existingFile_throwsException() throws Exception {
        generateAndCreateTestDir();
        Path file = createTestFile(testDir, null);

        fileSystem.writeToFile(file, 0);
    }

    @Test(expected = PathAlreadyExistsException.class)
    public void test_writeToFile_existingDir_throwsException() throws Exception {
        generateAndCreateTestDir();
        fileSystem.writeToFile(testDir, 0);
    }

    @Test(expected = PathAlreadyExistsException.class)
    public void test_writeToFile_symlinkToFile_throwsException() throws Exception {
        assumeTrue(description.canCreateSymboliclinks());
        generateAndCreateTestDir();
        Path file = createTestFile(testDir, null);
        Path link = createNewTestFileName(testDir);
        fileSystem.createSymbolicLink(link, file);
        fileSystem.writeToFile(link, 0);
    }

    // No need for more tests for write: we already check that data written =
    // data read when testing read (data is written before
    // using write)

    @Test(expected = IllegalArgumentException.class)
    public void test_appendToFile_nullPath_throwsException() throws Exception {
        assumeTrue(description.canAppend());
        fileSystem.appendToFile(null);
    }

    @Test
    public void test_appendToFile_append_nothing() throws Exception {
        assumeTrue(description.canAppend());
        generateAndCreateTestDir();
        byte[] data = "Hello world!".getBytes();
        Path file = createTestFile(testDir, data);
        OutputStream out = fileSystem.appendToFile(file);
        out.close();
        assertContents(file, data);
    }

    @Test
    public void test_appendToFile_append() throws Exception {
        assumeTrue(description.canAppend());
        generateAndCreateTestDir();
        String a = "Hello world! ";
        String b = "Pary people!";

        Path file = createTestFile(testDir, a.getBytes());
        OutputStream out = fileSystem.appendToFile(file);
        out.write(b.getBytes());
        out.close();
        assertContents(file, (a + b).getBytes());
    }

    @Test(expected = NoSuchPathException.class)
    public void test_appendToFile_fileDoesNotExist_throwsException() throws Exception {
        assumeTrue(description.canAppend());
        generateAndCreateTestDir();

        Path file = createNewTestFileName(testDir);
        fileSystem.appendToFile(file);
    }

    @Test(expected = InvalidPathException.class)
    public void test_appendToFile_isDirectory_throwsException() throws Exception {
        assumeTrue(description.canAppend());
        generateAndCreateTestDir();
        Path p = createTestSubDir(testDir);
        fileSystem.appendToFile(p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_copy_null_throwsException() throws Exception {
        fileSystem.copy(null, null, null, null, false);

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_copy_nullTarget_throwsException() throws Exception {
        generateAndCreateTestDir();
        Path file = createNewTestFileName(testDir);
        fileSystem.copy(file, null, null, null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_copy_nullTargetPath_throwsException() throws Exception {
        generateAndCreateTestDir();
        Path file = createNewTestFileName(testDir);
        fileSystem.copy(file, fileSystem, null, null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_copy_nullMode_throwsException() throws Exception {
        generateAndCreateTestDir();
        Path file = createTestFile(testDir, "bla".getBytes());
        Path file2 = createNewTestFileName(testDir);
        fileSystem.copy(file, fileSystem, file2, null, false);
    }

    @Test(expected = NoSuchPathException.class)
    public void test_copy_nonExistingSource_throwsException() throws Throwable {
        generateAndCreateTestDir();
        Path nonExistingSource = createNewTestFileName(testDir);
        Path target = createNewTestFileName(testDir);
        copySync(nonExistingSource, target, CopyMode.CREATE, true);
    }

    @Test(expected = InvalidPathException.class)
    public void test_copy_directoryAsSource_throwsException() throws Throwable {
        generateAndCreateTestDir();
        Path target = createNewTestFileName(testDir);
        Path dir0 = createTestSubDir(testDir);

        copySync(dir0, target, CopyMode.CREATE, false);
    }

    @Test
    public void test_copy_nonExistingTarget_copiedFile() throws Throwable {
        byte[] data = "Hello World!".getBytes();
        generateAndCreateTestDir();
        Path file0 = createTestFile(testDir, data);
        Path file1 = createNewTestFileName(testDir);
        copySync(file0, file1, CopyMode.CREATE, false);
        assertSameContents(file0, file1);
    }

    @Test(expected = PathAlreadyExistsException.class)
    public void test_copy_existingTarget_throwsException() throws Throwable {
        byte[] data = "Hello World!".getBytes();
        generateAndCreateTestDir();
        Path file0 = createTestFile(testDir, data);
        Path file1 = createTestFile(testDir, null);
        copySync(file0, file1, CopyMode.CREATE, false);
    }

    @Test
    public void test_copy_existingTarget_replace() throws Throwable {
        byte[] data = "Hello World!".getBytes();
        byte[] data2 = "Something else!".getBytes();
        generateAndCreateTestDir();
        Path file0 = createTestFile(testDir, data);
        Path file1 = createTestFile(testDir, data2);
        copySync(file0, file1, CopyMode.REPLACE, false);
        assertSameContents(file0, file1);
    }

    @Test
    public void test_copy_existingTarget_ignore() throws Throwable {
        byte[] data = "Hello World!".getBytes();
        byte[] data2 = "Something else!".getBytes();
        generateAndCreateTestDir();
        Path file0 = createTestFile(testDir, data);
        Path file1 = createTestFile(testDir, data2);
        copySync(file0, file1, CopyMode.IGNORE, false);
        assertContents(file1, data2);
    }

    @Test
    public void test_copy() throws Throwable {
        byte[] data = "Hello World!".getBytes();
        generateAndCreateTestDir();
        Path file0 = createTestFile(testDir, data);
        Path file1 = createNewTestFileName(testDir);
        copySync(file0, file1, CopyMode.CREATE, false);
        assertSameContents(file0, file1);
    }

    @Test
    public void test_copy_rec() throws Throwable {
        byte[] data = "Hello World!".getBytes();
        byte[] data2 = "Party people!".getBytes();
        byte[] data3 = "yes | rm -rf ".getBytes();
        byte[] data4 = "Use Xenon!".getBytes();
        generateAndCreateTestDir();

        Path source = createTestSubDir(testDir);
        createTestFile(source, data);
        Path testSubDir = createTestSubDir(source);

        Path testSubDir2 = createTestSubDir(source);
        createTestFile(testSubDir2, data2);
        createTestFile(testSubDir, data3);

        Path testSubSub = createTestSubDir(testSubDir);
        createTestFile(testSubSub, data4);

        Path target = createTestSubDirName(testDir);
        copySync(source, target, CopyMode.CREATE, true);
        assertSameContentsDir(source, target);
    }

    @Test(expected = PathAlreadyExistsException.class)
    public void test_copy_rec_create() throws Throwable {
        byte[] data = "Hello World!".getBytes();
        byte[] data2 = "Party people!".getBytes();
        byte[] data3 = "yes | rm -rf ".getBytes();
        byte[] data4 = "Use Xenon!".getBytes();
        generateAndCreateTestDir();
        Path target = createTestSubDir(testDir);
        Path source = createTestSubDir(testDir);
        Path file = createTestFile(target, data3);
        Path file2 = createTestFile(target, data3);
        Path file3 = createTestFile(target, data3);
        Path file4 = createTestFile(target, data4);

        Path srcFile = createNamedTestFile(source.resolve(file.getFileName()), data);
        Path srcFile2 = createNamedTestFile(source.resolve(file2.getFileName()), data2);

        copySync(source, target, CopyMode.CREATE, true);
    }

    @Test
    public void test_copy_rec_replace() throws Throwable {
        byte[] data = "Hello World!".getBytes();
        byte[] data2 = "Party people!".getBytes();
        byte[] data3 = "yes | rm -rf ".getBytes();
        byte[] data4 = "Use Xenon!".getBytes();
        generateAndCreateTestDir();
        Path target = createTestSubDir(testDir);
        Path subtarget = createTestSubDir(target);
        Path source = createTestSubDir(testDir);
        Path subsource = source.resolve(subtarget.getFileName());
        fileSystem.createDirectory(subsource);
        Path file = createTestFile(subtarget, data3);
        Path file2 = createTestFile(subtarget, data3);
        Path file3 = createTestFile(subtarget, data3);
        Path file4 = createTestFile(subtarget, data4);

        Path srcFile = createNamedTestFile(subsource.resolve(file.getFileName()), data);
        Path srcFile2 = createNamedTestFile(subsource.resolve(file2.getFileName()), data2);

        copySync(source, target, CopyMode.REPLACE, true);
        assertContents(file, data);
        assertContents(file2, data2);
        assertContents(file3, data3);
        assertContents(file4, data4);
    }

    @Test
    public void test_copy_rec_ignore() throws Throwable {
        byte[] data = "Hello World!".getBytes();
        byte[] data2 = "Party people!".getBytes();
        byte[] data3 = "yes | rm -rf ".getBytes();
        byte[] data4 = "Use Xenon!".getBytes();
        generateAndCreateTestDir();
        Path target = createTestSubDir(testDir);
        Path subtarget = createTestSubDir(target);
        Path source = createTestSubDir(testDir);
        Path subsource = source.resolve(subtarget.getFileName());
        fileSystem.createDirectory(subsource);
        Path file = createTestFile(subtarget, data3);
        Path file2 = createTestFile(subtarget, data3);
        Path file3 = createTestFile(subtarget, data3);
        Path file4 = createTestFile(subtarget, data4);

        Path srcFile = createNamedTestFile(subsource.resolve(file.getFileName()), data);
        Path srcFile2 = createNamedTestFile(subsource.resolve(file2.getFileName()), data2);

        copySync(source, target, CopyMode.IGNORE, true);
        assertContents(file, data3);
        assertContents(file2, data3);
        assertContents(file3, data3);
        assertContents(file4, data4);
    }

    @Test(expected = PathAlreadyExistsException.class)
    public void test_copy_target_directory_source_file_create() throws Throwable {
        byte[] data = "Hello World!".getBytes();
        byte[] data2 = "Party people!".getBytes();
        byte[] data3 = "yes | rm -rf ".getBytes();
        byte[] data4 = "Use Xenon!".getBytes();
        generateAndCreateTestDir();
        Path target = createTestSubDir(testDir);
        Path subtarget = createTestSubDir(target);
        Path file = createTestFile(subtarget, data3);
        Path file2 = createTestFile(subtarget, data3);
        Path file3 = createTestFile(subtarget, data3);
        Path file4 = createTestFile(subtarget, data4);

        Path source = createTestFile(testDir, data);

        copySync(source, target, CopyMode.CREATE, true);

    }

    @Test
    public void test_copy_target_directory_source_file_replace() throws Throwable {
        byte[] data = "Hello World!".getBytes();
        byte[] data2 = "Party people!".getBytes();
        byte[] data3 = "yes | rm -rf ".getBytes();
        byte[] data4 = "Use Xenon!".getBytes();
        generateAndCreateTestDir();

        // Create a source file
        Path source = createTestFile(testDir, data);

        // Create a target directory with contents
        Path target = createTestSubDir(testDir);
        Path subtarget = createTestSubDir(target);
        Path file = createTestFile(subtarget, data3);
        Path file2 = createTestFile(subtarget, data3);
        Path file3 = createTestFile(subtarget, data3);
        Path file4 = createTestFile(subtarget, data4);

        // Copy source to target, replacing the directory with a file.
        copySync(source, target, CopyMode.REPLACE, true);

        // Check if target is now a file with correct content.
        assertContents(target, data);
    }

    @Test
    public void test_copy_target_directory_source_file_ignore() throws Throwable {
        byte[] data = "Hello World!".getBytes();
        byte[] data2 = "Party people!".getBytes();
        byte[] data3 = "yes | rm -rf ".getBytes();
        byte[] data4 = "Use Xenon!".getBytes();
        generateAndCreateTestDir();
        Path target = createTestSubDir(testDir);
        Path subtarget = createTestSubDir(target);
        Path file = createTestFile(subtarget, data);
        Path file2 = createTestFile(subtarget, data2);
        Path file3 = createTestFile(subtarget, data3);
        Path file4 = createTestFile(subtarget, data4);

        Path source = createTestFile(testDir, data);

        copySync(source, target, CopyMode.IGNORE, true);

        assertContents(file, data);
        assertContents(file2, data2);
        assertContents(file3, data3);
        assertContents(file4, data4);
    }

    @Test(expected = NoSuchCopyException.class)
    public void test_getStatus_noSuchCopy_throwsException() throws Exception {
        fileSystem.getStatus("it would be a huge coincidence if this string would be linked to a copy status");
    }

    @Test
    public void test_getStatus_copyDone() throws Exception {
        byte[] data = "Hello World!".getBytes();
        generateAndCreateTestDir();
        Path file0 = createTestFile(testDir, data);
        Path file1 = createNewTestFileName(testDir);
        String copyId = fileSystem.copy(file0, fileSystem, file1, CopyMode.CREATE, false);

        CopyStatus status = fileSystem.waitUntilDone(copyId, 1000);
        assert (status.isDone());

    }

    @Test
    public void test_getStatus_bytesCorrect() throws Exception {
        byte[] data = "Hello World!".getBytes();
        generateAndCreateTestDir();
        Path file0 = createTestFile(testDir, data);
        Path file1 = createNewTestFileName(testDir);
        String copyId = fileSystem.copy(file0, fileSystem, file1, CopyMode.CREATE, false);
        CopyStatus status = fileSystem.waitUntilDone(copyId, 1000);
        assertEquals(status.bytesCopied(), data.length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_rename_nullSource_throwsException() throws Exception {
        assumeTrue(description.supportsRename());
        fileSystem.rename(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_rename_nullTarget_throwsException() throws Exception {
        assumeTrue(description.supportsRename());
        generateAndCreateTestDir();
        Path file0 = createTestFile(testDir, null);
        fileSystem.rename(file0, null);
    }

    @Test(expected = NoSuchPathException.class)
    public void test_rename_nonExistingSource() throws Exception {
        assumeTrue(description.supportsRename());
        generateAndCreateTestDir();
        Path nonExistingFile = createNewTestFileName(testDir);
        Path nonExistingFile2 = createNewTestFileName(testDir);
        fileSystem.rename(nonExistingFile, nonExistingFile2);
    }

    private void assertRenameCorrect(Path a, Path b, byte[] data) throws Exception {
        assertFalse(fileSystem.exists(a));
        assertContents(b, data);
    }

    @Test
    public void test_rename_existingSourceNonExistingTarget_success() throws Exception {
        assumeTrue(description.supportsRename());
        generateAndCreateTestDir();
        Path nonExistingFile = createNewTestFileName(testDir);
        byte[] data = "Hello everybody!".getBytes();
        Path existingFile = createTestFile(testDir, data);
        fileSystem.rename(existingFile, nonExistingFile);
        assertRenameCorrect(existingFile, nonExistingFile, data);
    }

    @Test(expected = PathAlreadyExistsException.class)
    public void test_rename_existingSourceAndTarget_throw() throws Exception {
        assumeTrue(description.supportsRename());
        generateAndCreateTestDir();
        Path existingFile1 = createTestFile(testDir, null);
        Path existingFile2 = createTestFile(testDir, null);
        fileSystem.rename(existingFile1, existingFile2);
    }

    @Test(expected = NoSuchPathException.class)
    public void test_rename_existingSourceNonExistingTargetParent_throw() throws Exception {
        assumeTrue(description.supportsRename());
        generateAndCreateTestDir();
        Path subDir = createTestSubDirName(testDir);
        Path source = createTestFile(testDir, null);
        Path target = createNewTestFileName(subDir);
        fileSystem.rename(source, target);
    }

    @Test
    public void test_rename_sourceEqualsTarget_noThrow() throws Exception {
        assumeTrue(description.supportsRename());
        generateAndCreateTestDir();
        Path existingFile = createTestFile(testDir, "test data".getBytes());
        fileSystem.rename(existingFile, existingFile);
    }

    @Test
    public void test_rename_existingDirectoryNonExistingFile() throws Exception {
        assumeTrue(description.supportsRename());
        generateAndCreateTestDir();

        Path subDir = createTestSubDir(testDir);

        byte[] adata = "data".getBytes();
        byte[] bdata = "content".getBytes();
        Path a = createTestFile(subDir, adata);
        Path b = createTestFile(subDir, bdata);

        Path subDir2 = createTestSubDirName(testDir);
        fileSystem.rename(subDir, subDir2);

        Thread.sleep(1000);

        assertTrue(fileSystem.exists(subDir2));
        assertFalse(fileSystem.exists(subDir));

        assertContents(subDir2.resolve(a.getFileName()), adata);
        assertContents(subDir2.resolve(b.getFileName()), bdata);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_readSymbolicLink_null_throwsException() throws Exception {
        assumeTrue(description.canReadSymboliclinks());
        fileSystem.readSymbolicLink(null);
    }

    @Test(expected = InvalidPathException.class)
    public void test_readSymbolicLink_noLink_throwsException() throws Exception {
        assumeTrue(description.canReadSymboliclinks());
        generateAndCreateTestDir();
        Path p = createTestFile(testDir, "not a link".getBytes());
        fileSystem.readSymbolicLink(p);
    }

    @Test(expected = InvalidPathException.class)
    public void test_readSymbolicLink_noLinkDir_throwsException() throws Exception {
        assumeTrue(description.canReadSymboliclinks());
        generateAndCreateTestDir();
        fileSystem.readSymbolicLink(testDir);
    }

    @Test
    public void test29_readSymbolicLink() throws Exception {
        assumeTrue(description.canReadSymboliclinks());
        Map.Entry<Path, Path> map = locationConfig.getSymbolicLinksToExistingFile();
        Path res = fileSystem.readSymbolicLink(map.getKey());
        assertEquals(res, map.getValue());
    }

    @Test
    public void test_multipleFileSystemsOpenSimultaneously() throws Exception {
        FileSystem fs1 = setupFileSystem();
        FileSystem fs2 = setupFileSystem();
        assert (fs2.isOpen());
        assert (fs1.isOpen());

        // Close them both. We should get no exceptions.
        fs1.close();
        fs2.close();
    }

    @Test
    public void test_getWorkingDirectory() {
        Path expected = locationConfig.getExpectedWorkingDirectory();
        assumeNotNull(expected);

        Path result = fileSystem.getWorkingDirectory();

        assertEquals("Working dir not equal: " + expected + " != " + result, expected, result);
    }

    // TODO: Symbolic links in a cycle tests
    // TODO: Test asynchronous exceptions
    // TODO: Test create symbolic link
    // TODO: Cancel copy
    // TODO: WaitUntilDone

}
