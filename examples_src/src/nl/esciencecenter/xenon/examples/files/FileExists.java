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

import java.net.URI;
import java.net.URISyntaxException;

import nl.esciencecenter.xenon.Xenon;
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.XenonFactory;
import nl.esciencecenter.xenon.credentials.Credential;
import nl.esciencecenter.xenon.credentials.Credentials;
import nl.esciencecenter.xenon.files.FileSystem;
import nl.esciencecenter.xenon.files.Files;
import nl.esciencecenter.xenon.files.Path;
import nl.esciencecenter.xenon.files.RelativePath;

/**
 * An example of how to check if a file exists.
 * 
 * This example assumes the user provides a URI with the location of the files to check on the command line.
 * 
 * @author Jason Maassen <J.Maassen@esciencecenter.nl>
 * @version 1.0
 * @since 1.0
 */
public class FileExists {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Example requires a URI as a parameter!");
            System.exit(1);
        }

        try {
            // We first turn the user provided argument into a URI.
            URI uri = new URI(args[0]);
        
            // We create a new Xenon using the XenonFactory (without providing any properties).
            Xenon xenon = XenonFactory.newXenon(null);

            // Next, we retrieve the Files and Credentials interfaces
            Files files = xenon.files();
            Credentials credentials = xenon.credentials();

            // Next we create a FileSystem  
            Credential c = credentials.getDefaultCredential(uri.getScheme());
            FileSystem fs = files.newFileSystem(uri.getScheme(), uri.getAuthority(), c, null);

            // We now create an Path representing the file
            Path path = files.newPath(fs, new RelativePath(uri.getPath()));

            // Check if the file exists 
            if (files.exists(path)) {
                System.out.println("File " + uri + " exists!");
            } else {
                System.out.println("File " + uri + " does not exist!");
            }

            // If we are done we need to close the FileSystem ad the credential
            files.close(fs);
            credentials.close(c);

            // Finally, we end Xenon to release all resources 
            XenonFactory.endXenon(xenon);

        } catch (URISyntaxException | XenonException e) {
            System.out.println("FileExists example failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
