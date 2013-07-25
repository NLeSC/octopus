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

package nl.esciencecenter.octopus.examples.files;

import java.net.URI;

import nl.esciencecenter.octopus.Octopus;
import nl.esciencecenter.octopus.OctopusFactory;
import nl.esciencecenter.octopus.exceptions.NoSuchFileException;
import nl.esciencecenter.octopus.files.AbsolutePath;
import nl.esciencecenter.octopus.files.FileAttributes;
import nl.esciencecenter.octopus.files.FileSystem;
import nl.esciencecenter.octopus.files.Files;
import nl.esciencecenter.octopus.files.RelativePath;
import nl.esciencecenter.octopus.util.URIUtils;

/**
 * A simple example of how to check file attributes. 
 * 
 * This example assumes the user provides a URI on the command line to check. 
 * 
 * @author Jason Maassen <J.Maassen@esciencecenter.nl>
 * @version 1.0
 * @since 1.0
 */
public class ShowFileAttributes {

    public static void main(String [] args) { 
        
        if (args.length != 1) { 
            System.out.println("Example requires an URI a parameter!");
            System.exit(1);
        }
                
        try {
            // We first turn the user provided argument into a URI.
            URI uri = new URI(args[0]);
            
            // Next, extract the parts from the URI we need to access the FileSystem.
            URI fsURI = URIUtils.getFileSystemURI(uri);
            
            // Also get the (absolute) path to the file.
            String filepath = uri.getPath();
            
            // We create a new octopus using the OctopusFactory (without providing any properties).
            Octopus octopus = OctopusFactory.newOctopus(null);

            // Next, we retrieve the Files and Credentials interfaces
            Files files = octopus.files();
            
            // Next we create a FileSystem 
            FileSystem fs = files.newFileSystem(fsURI, null, null);
            
            // We now create an AbsolutePath representing the file.
            AbsolutePath path = files.newPath(fs, new RelativePath(filepath));
            
            try { 
                // Retrieve the attributes of the file
                FileAttributes attributes = files.getAttributes(path);
                
                System.out.println("File " + uri + " exists and has the following attributes:");
                System.out.println("  isDirectory: " + attributes.isDirectory());
                System.out.println("  isSymbolicLink: " + attributes.isSymbolicLink());
                System.out.println("  size: " + attributes.size());
                System.out.println("  owner: " + attributes.owner());
                System.out.println("  group: " + attributes.group());
                System.out.println("  permissions: " + attributes.permissions());
                
            } catch (NoSuchFileException e) { 
                System.out.println("File " + uri + " does not exist!");
            
            } catch (Exception e) {
                System.out.println("Failed to retrieve attributes of " + uri + " " + e.getMessage());
                e.printStackTrace();
            }
            
            // If we are done we need to close the FileSystem
            files.close(fs);
            
            // Finally, we end octopus to release all resources 
            OctopusFactory.endOctopus(octopus);

        } catch (Exception e) { 
            System.out.println("CreatingFileSystem example failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}