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

package nl.esciencecenter.xenon.examples.files;

import nl.esciencecenter.xenon.Xenon;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.XenonFactory;
import nl.esciencecenter.xenon.files.Files;
import nl.esciencecenter.xenon.files.Path;
import nl.esciencecenter.xenon.util.Utils;

/**
 * An example of how to check if a local file exists.
 * 
 * This example is hard coded to use the local file system. A more generic example is shown in {@link FileExists}. 
 * 
 * This example assumes the user provides a path to check.
 * 
 * @author Jason Maassen <J.Maassen@esciencecenter.nl>
 * @version 1.0
 * @since 1.0
 */
public class LocalFileExists {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Example required an absolute file path as a parameter!");
            System.exit(1);
        }

        // This should be a valid local path!
        String filename = args[0];

        try {
            // We create a new Xenon using the XenonFactory (without providing any properties).
            Xenon xenon = XenonFactory.newXenon(null);

            // Next, we retrieve the Files interfaces
            Files files = xenon.files();

            // We now create an Path representing the local file
            Path path = Utils.fromLocalPath(files, filename);

            // Check if the file exists 
            if (files.exists(path)) {
                System.out.println("File " + filename + " exists!");
            } else {
                System.out.println("File " + filename + " does not exist!");
            }

            // If we are done we need to close the FileSystem ad the credential
            files.close(path.getFileSystem());

            // Finally, we end Xenon to release all resources 
            XenonFactory.endXenon(xenon);

        } catch (XenonException e) {
            System.out.println("LocalFileExists example failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
