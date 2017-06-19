/**
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
package nl.esciencecenter.xenon.adaptors.file.sftp;

import java.util.List;

import org.apache.sshd.client.subsystem.sftp.SftpClient;

import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.adaptors.file.util.DirectoryStreamBase;
import nl.esciencecenter.xenon.engine.files.PathAttributesPairImplementation;
import nl.esciencecenter.xenon.files.Path;
import nl.esciencecenter.xenon.files.PathAttributesPair;

class SftpDirectoryAttributeStream extends DirectoryStreamBase<SftpClient.DirEntry, PathAttributesPair> {

    public SftpDirectoryAttributeStream(Path dir, nl.esciencecenter.xenon.files.DirectoryStream.Filter filter,
            List<SftpClient.DirEntry> listing) throws XenonException {
        super(dir, filter, listing);
    }

    @Override
    protected PathAttributesPair getStreamElementFromEntry(SftpClient.DirEntry entry, Path entryPath) {
        SftpFileAttributes attributes = new SftpFileAttributes(entry.getAttributes(), entryPath);
        return new PathAttributesPairImplementation(entryPath, attributes);
    }

    @Override
    protected String getFileNameFromEntry(SftpClient.DirEntry entry, Path parentPath) {
        return entry.getFilename();
    }

}