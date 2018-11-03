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
package nl.esciencecenter.xenon.filesystems;

/**
 * CopyOption is an enumeration containing all possible options for copying a file.
 *
 * Note that the <code>CREATE</code>, <code>REPLACE</code> and <code>IGNORE</code> options are mutually exclusive.
 */
public enum CopyMode {

    /**
     * Copy to a new destination file, failing if the file already exists.
     */
    CREATE,

    /**
     * If the source and destination are a regular file, the destination file will be replaced. If the destination exists and is not a file and the source is a
     * file, then the destination will be (recursively) deleted before copying.
     *
     *
     * If the source and destination are directories then additional files in the destination directory are <b>not</b> touched.
     *
     */
    REPLACE,

    /**
     * Skip the copy if the destination file if it already exists.
     */
    IGNORE
}
