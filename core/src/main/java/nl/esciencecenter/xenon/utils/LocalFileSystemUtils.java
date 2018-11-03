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

import java.io.File;

import nl.esciencecenter.xenon.InvalidLocationException;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.filesystems.FileSystem;

public class LocalFileSystemUtils {

    private static final String OS_PROPERTY = "os.name";
    private static final String ADAPTOR_NAME = "local";

    private LocalFileSystemUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Returns if we are currently running on Windows.
     *
     * @return if we are currently running on Window.
     */
    public static boolean isWindows() {
        String os = System.getProperty(OS_PROPERTY);
        return (os != null && os.startsWith("Windows"));
    }

    /**
     * Returns if we are currently running on OSX.
     *
     * @return if we are currently running on OSX.
     */
    public static boolean isOSX() {
        String os = System.getProperty(OS_PROPERTY);
        return (os != null && os.equals("Mac OS X"));
    }

    /**
     * Returns if we are currently running on Linux.
     *
     * @return if we are currently running on Linux.
     */
    public static boolean isLinux() {
        String os = System.getProperty(OS_PROPERTY);
        return (os != null && os.equals("Linux"));
    }

    /**
     * Check if <code>root</code> only contains a valid Windows root element such as "C:".
     *
     * If <code>root</code> is <code>null</code> or empty, <code>false</code> will be returned. If <code>root</code> contains more than just a root element,
     * <code>false</code> will be returned.
     *
     * @param root
     *            The root to check.
     * @return If <code>root</code> only contains a valid Windows root element.
     */
    public static boolean isWindowsRoot(String root) {
        if (root == null) {
            return false;
        }

        if (root.length() == 2 && root.endsWith(":") && Character.isLetter(root.charAt(0))) {
            return true;
        }

        return (root.length() == 3 && root.charAt(1) == ':' && Character.isLetter(root.charAt(0)) && root.charAt(2) == '\\');
    }

    /**
     * Check if <code>root</code> only contains a valid Linux root element, which is "/".
     *
     * If <code>root</code> is <code>null</code> or empty, <code>false</code> will be returned. If <code>root</code> contains more than just a root element,
     * <code>false</code> will be returned.
     *
     * @param root
     *            The root to check.
     * @return If <code>root</code> only contains a valid Linux root element.
     */
    public static boolean isLinuxRoot(String root) {
        return (root != null && root.equals("/"));
    }

    /**
     * Check if <code>root</code> contains a valid OSX root element, which is "/".
     *
     * If <code>root</code> is <code>null</code> or empty, <code>false</code> will be returned. If <code>root</code> contains more than just a root element,
     * <code>false</code> will be returned.
     *
     * @param root
     *            The root to check.
     * @return If <code>root</code> only contains a valid OSX root element.
     */
    public static boolean isOSXRoot(String root) {
        return (root != null && root.equals("/"));
    }

    /**
     * Check if <code>root</code> contains a locally valid root element, such as "C:" on Windows or "/" on Linux and OSX.
     *
     * If <code>root</code> is <code>null</code> or empty, <code>false</code> will be returned. If <code>root</code> contains more than just a root element,
     * <code>false</code> will be returned.
     *
     * Note that the result of this method depends on the OS the application is running on.
     *
     * @param root
     *            The root to check.
     * @return If <code>root</code> only contains a valid OSX root element.
     */
    public static boolean isLocalRoot(String root) {
        if (isWindows()) {
            return isWindowsRoot(root);
        }

        return isLinuxRoot(root);
    }

    /**
     * Checks if the provide path starts with a valid Linux root, that is "/".
     *
     * @param path
     *            The path to check.
     * @return If the provide path starts with a valid Linux root.
     */
    public static boolean startsWithLinuxRoot(String path) {
        return path != null && path.startsWith("/");

    }

    /**
     * Checks if the provide path starts with a valid Windows root, for example "C:".
     *
     * @param path
     *            The path to check.
     * @return If the provide path starts with a valid Windows root.
     */
    public static boolean startWithWindowsRoot(String path) {
        return path != null && path.length() >= 2 && path.charAt(1) == ':' && Character.isLetter(path.charAt(0));
    }

    /**
     * Checks if the provide path starts with a valid root, such as "/" or "C:".
     *
     * @param path
     *            The path to check.
     * @return If the provide path starts with a valid root.
     */
    public static boolean startWithRoot(String path) {
        return startsWithLinuxRoot(path) || startWithWindowsRoot(path);
    }

    /**
     * Return the locally valid root element of an <code>String</code> representation of an absolute path.
     *
     * Examples of a root elements are "/" or "C:". If the provided path does not contain a locally valid root element, an exception will be thrown. For
     * example, providing "/user/local" will return "/" on Linux or OSX, but throw an exception on Windows; providing "C:\test" will return "C:" on Windows but
     * throw an exception on Linux or OSX.
     *
     * If the provided string is <code>null</code> or empty, the default root element for this OS will be returned, i.e,. "/" on Linux or OSX and "C:" on
     * windows.
     *
     * @param p
     *            The absolute path for which to determine the root element.
     * @return The locally valid root element.
     * @throws InvalidLocationException
     *             If the provided <code>path</code> is not absolute, or does not contain a locally valid root.
     */
    public static String getLocalRoot(String p) throws InvalidLocationException {

        String path = p;

        if (isWindows()) {
            if (path == null || path.isEmpty()) {
                return "C:";
            }

            if (path.charAt(0) == '/') {
                path = path.substring(1);
            }
            if (path.length() >= 2 && (path.charAt(1) == ':') && Character.isLetter(path.charAt(0))) {
                return path.substring(0, 2).toUpperCase();
            }

            throw new InvalidLocationException(ADAPTOR_NAME, "Path does not include drive name! " + path);
        }

        if (path == null || path.isEmpty() || path.charAt(0) == '/') {
            return "/";
        }

        throw new InvalidLocationException(ADAPTOR_NAME, "Path is not absolute! " + path);
    }

    /**
     * Expand the tilde in a String representation of a path by the users home directory, as provided by the <code>user.home</code> property.
     *
     * The tilde will only be replaced if it is the first character in the path and either the only character in the path or directly followed by the local
     * separator character or directly followed by the local user name (as provided by the <code>user.name</code> property).
     *
     * For example, if <code>user.name</code> is set to "john" and <code>user.home</code> is set to "/home/john", then "~" will be expanded to "/home/john",
     * "~/foo" or "~john/foo" will be expanded to "/home/john/foo".
     *
     * However, in paths like "/foo/bar", "~matt/foo" or "/foo/~" the tilde will not be expanded, and the provided path is returned unchanged. This includes a
     * path that is <code>null</code>.
     *
     * If the <code>user.home</code> property is not set, the tilde will not be expanded and the provided path will be returned unchanged. If the
     * <code>user.name</code> property is not set, the combined tilde-username expansion will not be performed.
     *
     * @param path
     *            the path in which to replace the tilde (if possible).
     * @return the path with the tilde replaced by the user home property, or the unchanged path if replacement was not triggered.
     */
    public static String expandTilde(String path) {

        if (path == null) {
            return null;
        }

        String home = System.getProperty("user.home");

        if (home == null || home.isEmpty()) {
            return path;
        }

        if (!path.startsWith("~")) {
            return path;
        }

        if (path.length() == 1 || path.startsWith("~" + getLocalSeparator())) {
            return getLocalRootlessPath(home) + path.substring(1);
        }

        String user = System.getProperty("user.name");

        if (user == null || user.isEmpty()) {
            return path;
        }

        if (path.startsWith("~" + user)) {
            return getLocalRootlessPath(home) + path.substring(1 + user.length());
        }

        return path;
    }

    /**
     * Return the local root less path of an absolute path.
     *
     * @param path
     *            The absolute path from which to remove the root element.
     * @return The path without the root element.
     */
    public static String getLocalRootlessPath(String path) {
        if (path.length() >= 2 && (path.charAt(1) == ':') && Character.isLetter(path.charAt(0))) {
            return path.substring(2);
        }
        return path;
    }

    /**
     * Returns the local file system path separator character.
     *
     * @return The local file system path separator character.
     */
    public static char getLocalSeparator() {
        return File.separatorChar;
    }

    /**
     * Returns all local FileSystems.
     *
     * This method detects all local file system roots, and returns one or more <code>FileSystems</code> representing each of these roots.
     *
     * @return all local FileSystems.
     *
     * @throws XenonException
     *             If the creation of the FileSystem failed.
     */
    public static FileSystem[] getLocalFileSystems() throws XenonException {

        File[] roots = File.listRoots();

        FileSystem[] result = new FileSystem[roots.length];

        for (int i = 0; i < result.length; i++) {
            result[i] = FileSystem.create("file", getLocalRoot(roots[i].getPath()));
        }

        return result;
    }
}
