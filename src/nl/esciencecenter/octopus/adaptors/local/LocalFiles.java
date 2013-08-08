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
package nl.esciencecenter.octopus.adaptors.local;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import nl.esciencecenter.octopus.OctopusPropertyDescription.Level;
import nl.esciencecenter.octopus.credentials.Credential;
import nl.esciencecenter.octopus.engine.OctopusProperties;
import nl.esciencecenter.octopus.engine.files.AbsolutePathImplementation;
import nl.esciencecenter.octopus.engine.files.CopyImplementation;
import nl.esciencecenter.octopus.engine.files.FileSystemImplementation;
import nl.esciencecenter.octopus.engine.files.FilesEngine;
import nl.esciencecenter.octopus.engine.util.CopyEngine;
import nl.esciencecenter.octopus.engine.util.CopyInfo;
import nl.esciencecenter.octopus.engine.util.OpenOptions;
import nl.esciencecenter.octopus.exceptions.FileAlreadyExistsException;
import nl.esciencecenter.octopus.exceptions.InvalidOpenOptionsException;
import nl.esciencecenter.octopus.exceptions.NoSuchFileException;
import nl.esciencecenter.octopus.exceptions.OctopusException;
import nl.esciencecenter.octopus.exceptions.OctopusIOException;
import nl.esciencecenter.octopus.exceptions.UnsupportedOperationException;
import nl.esciencecenter.octopus.files.AbsolutePath;
import nl.esciencecenter.octopus.files.Copy;
import nl.esciencecenter.octopus.files.CopyOption;
import nl.esciencecenter.octopus.files.CopyStatus;
import nl.esciencecenter.octopus.files.DirectoryStream;
import nl.esciencecenter.octopus.files.FileAttributes;
import nl.esciencecenter.octopus.files.FileSystem;
import nl.esciencecenter.octopus.files.OpenOption;
import nl.esciencecenter.octopus.files.PathAttributesPair;
import nl.esciencecenter.octopus.files.PosixFilePermission;
import nl.esciencecenter.octopus.files.RelativePath;

/**
 * LocalFiles implements an Octopus <code>Files</code> adaptor for local file operations.
 * 
 * @see nl.esciencecenter.octopus.files.Files
 * 
 * @author Jason Maassen <J.Maassen@esciencecenter.nl>
 * @version 1.0
 * @since 1.0
 */
public class LocalFiles implements nl.esciencecenter.octopus.files.Files {

    /** The parent adaptor */
    private final LocalAdaptor localAdaptor;

    /** The copy engine */
    private final CopyEngine copyEngine;

    /** The next ID for a FileSystem */
    private static int fsID = 0;

    private static synchronized int getNextFsID() {
        return fsID++;
    }

    private final FileSystem cwd;
    private final FileSystem home;

    public LocalFiles(LocalAdaptor localAdaptor, CopyEngine copyEngine) throws OctopusException {
        this.localAdaptor = localAdaptor;
        this.copyEngine = copyEngine;

        cwd = new FileSystemImplementation(LocalAdaptor.ADAPTOR_NAME, "localfs-" + getNextFsID(), LocalUtils.getLocalFileURI(),
                new RelativePath(LocalUtils.getCWD()), null, null);

        home = new FileSystemImplementation(LocalAdaptor.ADAPTOR_NAME, "localfs-" + getNextFsID(), LocalUtils.getLocalFileURI(),
                new RelativePath(LocalUtils.getHome()), null, null);
    }

    /**
     * Move or rename an existing source path to a non-existing target path.
     * 
     * The parent of the target path (e.g. <code>target.getParent</code>) must exist.
     * 
     * If the source is a link, the link itself will be moved, not the path to which it refers. If the source is a directory, it
     * will be renamed to the target. This implies that a moving a directory between physical locations may fail.
     * 
     * @param source
     *            the existing source path.
     * @param target
     *            the non existing target path.
     * @return the target path.
     * 
     * @throws NoSuchFileException
     *             If the source file does not exist or the target parent directory does not exist.
     * @throws FileAlreadyExistsException
     *             If the target file already exists.
     * @throws OctopusIOException
     *             If the move failed.
     */
    @Override
    public AbsolutePath move(AbsolutePath source, AbsolutePath target) throws OctopusIOException {

        if (!exists(source)) {
            throw new NoSuchFileException(LocalAdaptor.ADAPTOR_NAME, "Source " + source.getPath() + " does not exist!");
        }

        if (source.normalize().equals(target.normalize())) {
            return target;
        }

        if (exists(target)) {
            throw new FileAlreadyExistsException(LocalAdaptor.ADAPTOR_NAME, "Target " + target.getPath() + " already exists!");
        }

        if (!exists(target.getParent())) {
            throw new NoSuchFileException(LocalAdaptor.ADAPTOR_NAME, "Target directory " + target.getParent().getPath()
                    + " does not exist!");
        }

        LocalUtils.move(source, target);

        return target;
    }

    @Override
    public AbsolutePath readSymbolicLink(AbsolutePath link) throws OctopusIOException {

        try {
            java.nio.file.Path path = LocalUtils.javaPath(link);
            java.nio.file.Path target = Files.readSymbolicLink(path);

            AbsolutePath parent = link.getParent();

            if (parent == null || target.isAbsolute()) {
                return new AbsolutePathImplementation(link.getFileSystem(), new RelativePath(target.toString()));
            }

            return parent.resolve(new RelativePath(target.toString()));
        } catch (IOException e) {
            throw new OctopusIOException(LocalAdaptor.ADAPTOR_NAME, "Failed to read symbolic link.", e);
        }
    }

    @Override
    public DirectoryStream<AbsolutePath> newDirectoryStream(AbsolutePath dir, DirectoryStream.Filter filter)
            throws OctopusIOException {

        FileAttributes att = getAttributes(dir);

        if (!att.isDirectory()) {
            throw new OctopusIOException(LocalAdaptor.ADAPTOR_NAME, "File is not a directory.");
        }

        if (filter == null) {
            throw new OctopusIOException(LocalAdaptor.ADAPTOR_NAME, "Filter is null.");
        }

        return new LocalDirectoryStream(dir, filter);
    }

    @Override
    public DirectoryStream<PathAttributesPair> newAttributesDirectoryStream(AbsolutePath dir, DirectoryStream.Filter filter)

    throws OctopusIOException {

        FileAttributes att = getAttributes(dir);

        if (!att.isDirectory()) {
            throw new OctopusIOException(LocalAdaptor.ADAPTOR_NAME, "File is not a directory.");
        }

        if (filter == null) {
            throw new OctopusIOException(LocalAdaptor.ADAPTOR_NAME, "Filter is null.");
        }

        return new LocalDirectoryAttributeStream(this, new LocalDirectoryStream(dir, filter));
    }

    @Override
    public InputStream newInputStream(AbsolutePath path) throws OctopusIOException {

        if (!exists(path)) {
            throw new NoSuchFileException(LocalAdaptor.ADAPTOR_NAME, "File " + path.getPath() + " does not exist!");
        }

        FileAttributes att = getAttributes(path);

        if (att.isDirectory()) {
            throw new OctopusIOException(LocalAdaptor.ADAPTOR_NAME, "Path " + path.getPath() + " is a directory!");
        }

        return LocalUtils.newInputStream(path);
    }

    @Override
    public OutputStream newOutputStream(AbsolutePath path, OpenOption... options) throws OctopusIOException {

        OpenOptions tmp = OpenOptions.processOptions(LocalAdaptor.ADAPTOR_NAME, options);

        if (tmp.getReadMode() != null) {
            throw new InvalidOpenOptionsException(LocalAdaptor.ADAPTOR_NAME, "Disallowed open option: READ");
        }

        if (tmp.getAppendMode() == null) {
            throw new InvalidOpenOptionsException(LocalAdaptor.ADAPTOR_NAME, "No append mode provided!");
        }

        if (tmp.getWriteMode() == null) {
            tmp.setWriteMode(OpenOption.WRITE);
        }

        if (tmp.getOpenMode() == OpenOption.CREATE) {
            if (exists(path)) {
                throw new FileAlreadyExistsException(LocalAdaptor.ADAPTOR_NAME, "File already exists: " + path.getPath());
            }
        } else if (tmp.getOpenMode() == OpenOption.OPEN) {
            if (!exists(path)) {
                throw new NoSuchFileException(LocalAdaptor.ADAPTOR_NAME, "File does not exist: " + path.getPath());
            }
        }

        try {
            return Files.newOutputStream(LocalUtils.javaPath(path), LocalUtils.javaOpenOptions(options));
        } catch (IOException e) {
            throw new OctopusIOException(LocalAdaptor.ADAPTOR_NAME, "Failed to create OutputStream.", e);
        }
    }

    @Override
    public FileAttributes getAttributes(AbsolutePath path) throws OctopusIOException {
        return new LocalFileAttributes(path);
    }

    @Override
    public boolean exists(AbsolutePath path) throws OctopusIOException {
        return Files.exists(LocalUtils.javaPath(path));
    }

    @Override
    public void setPosixFilePermissions(AbsolutePath path, Set<PosixFilePermission> permissions) throws OctopusIOException {

        if (!exists(path)) {
            throw new NoSuchFileException(LocalAdaptor.ADAPTOR_NAME, "File " + path.getPath() + " does not exist!");
        }

        if (permissions == null) {
            throw new OctopusIOException(LocalAdaptor.ADAPTOR_NAME, "Permissions is null!");
        }

        LocalUtils.setPosixFilePermissions(path, permissions);
    }

    @Override
    public FileSystem newFileSystem(URI location, Credential credential, Map<String, String> properties) throws OctopusException,
            OctopusIOException {

        localAdaptor.checkURI(location);
        localAdaptor.checkCredential(credential);

        String path = location.getPath();

        if (path != null && !path.equals("/")) {
            throw new OctopusException(LocalAdaptor.ADAPTOR_NAME, "Cannot create local file system with path!");
        }

        OctopusProperties p = new OctopusProperties(localAdaptor.getSupportedProperties(Level.FILESYSTEM), properties);

        return new FileSystemImplementation(LocalAdaptor.ADAPTOR_NAME, "localfs-" + getNextFsID(), location, new RelativePath(
                LocalUtils.getCWD()), credential, p);
    }

    @Override
    public AbsolutePath newPath(FileSystem filesystem, RelativePath location) throws OctopusException, OctopusIOException {
        return new AbsolutePathImplementation(filesystem, location);
    }

    @Override
    public void close(FileSystem filesystem) throws OctopusException, OctopusIOException {
        // ignored!
    }

    @Override
    public boolean isOpen(FileSystem filesystem) throws OctopusException, OctopusIOException {
        return true;
    }

    @Override
    public AbsolutePath createDirectories(AbsolutePath dir) throws OctopusIOException {

        if (exists(dir)) {
            throw new FileAlreadyExistsException(LocalAdaptor.ADAPTOR_NAME, "Directory " + dir.getPath() + " already exists!");
        }

        Iterator<AbsolutePath> itt = dir.iterator();

        while (itt.hasNext()) {
            AbsolutePath path = itt.next();

            if (!exists(path)) {
                createDirectory(path);
            }
        }

        return dir;
    }

    @Override
    public AbsolutePath createDirectory(AbsolutePath dir) throws OctopusIOException {

        if (exists(dir)) {
            throw new FileAlreadyExistsException(LocalAdaptor.ADAPTOR_NAME, "Directory " + dir.getPath() + " already exists!");
        }

        if (!exists(dir.getParent())) {
            throw new OctopusIOException(LocalAdaptor.ADAPTOR_NAME, "Parent directory " + dir.getParent() + " does not exist!");
        }

        try {
            java.nio.file.Files.createDirectory(LocalUtils.javaPath(dir));
        } catch (IOException e) {
            throw new OctopusIOException(LocalAdaptor.ADAPTOR_NAME, "Failed to create directory " + dir.getPath(), e);
        }

        return dir;
    }

    @Override
    public AbsolutePath createFile(AbsolutePath path) throws OctopusIOException {

        if (exists(path)) {
            throw new FileAlreadyExistsException(LocalAdaptor.ADAPTOR_NAME, "File " + path.getPath() + " already exists!");
        }

        if (!exists(path.getParent())) {
            throw new OctopusIOException(LocalAdaptor.ADAPTOR_NAME, "Parent directory " + path.getParent() + " does not exist!");
        }

        LocalUtils.createFile(path);

        return path;
    }

    @Override
    public void delete(AbsolutePath path) throws OctopusIOException {
        LocalUtils.delete(path);
    }

    @Override
    public DirectoryStream<AbsolutePath> newDirectoryStream(AbsolutePath dir) throws OctopusIOException {
        return newDirectoryStream(dir, FilesEngine.ACCEPT_ALL_FILTER);
    }

    @Override
    public DirectoryStream<PathAttributesPair> newAttributesDirectoryStream(AbsolutePath dir) throws OctopusIOException {
        return newAttributesDirectoryStream(dir, FilesEngine.ACCEPT_ALL_FILTER);
    }

    @Override
    public FileSystem getLocalCWDFileSystem() throws OctopusException {
        return cwd;
    }

    @Override
    public FileSystem getLocalHomeFileSystem() throws OctopusException {
        return home;
    }

    @Override
    public Copy copy(AbsolutePath source, AbsolutePath target, CopyOption... options) throws OctopusIOException,
            UnsupportedOperationException {

        boolean async = false;
        boolean verify = false;

        CopyOption mode = null;

        for (CopyOption opt : options) {
            switch (opt) {
            case CREATE:
                if (mode != null && mode != opt) {
                    throw new UnsupportedOperationException(LocalAdaptor.ADAPTOR_NAME, "Conflicting copy options: " + mode
                            + " and CREATE");
                }

                mode = opt;
                break;
            case REPLACE:
                if (mode != null && mode != opt) {
                    throw new UnsupportedOperationException(LocalAdaptor.ADAPTOR_NAME, "Conflicting copy options: " + mode
                            + " and REPLACE");
                }

                mode = opt;
                break;
            case APPEND:
                if (mode != null && mode != opt) {
                    throw new UnsupportedOperationException(LocalAdaptor.ADAPTOR_NAME, "Conflicting copy options: " + mode
                            + " and APPEND");
                }

                mode = opt;
                break;
            case RESUME:
                if (mode != null && mode != opt) {
                    throw new UnsupportedOperationException(LocalAdaptor.ADAPTOR_NAME, "Conflicting copy options: " + mode
                            + " and RESUME");
                }

                mode = opt;
                break;
            case IGNORE:
                if (mode != null && mode != opt) {
                    throw new UnsupportedOperationException(LocalAdaptor.ADAPTOR_NAME, "Conflicting copy options: " + mode
                            + " and RESUME");
                }

                mode = opt;
                break;
            case VERIFY:
                verify = true;
                break;
            case ASYNCHRONOUS:
                async = true;
                break;
            }
        }

        if (mode == null) {
            mode = CopyOption.CREATE;
        }

        if (verify && mode != CopyOption.RESUME) {
            throw new UnsupportedOperationException(LocalAdaptor.ADAPTOR_NAME, "Conflicting copy options: " + mode
                    + " and VERIFY");
        }

        CopyImplementation copy = 
                new CopyImplementation(LocalAdaptor.ADAPTOR_NAME, copyEngine.getNextID("LOCAL_COPY_"), source, target);
        
        CopyInfo info = new CopyInfo(copy, mode, verify);
        copyEngine.copy(info, async);

        if (async) {
            return copy;
        } else {

            Exception e = info.getException();

            if (e != null) {
                throw new OctopusIOException(LocalAdaptor.ADAPTOR_NAME, "Copy failed!", e);
            }

            return null;
        }

    }

    @Override
    public CopyStatus getCopyStatus(Copy copy) throws OctopusException, OctopusIOException {
        return copyEngine.getStatus(copy);
    }

    @Override
    public CopyStatus cancelCopy(Copy copy) throws OctopusException, OctopusIOException {
        return copyEngine.cancel(copy);
    }
}
