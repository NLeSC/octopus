
# Xenon

[![Build Status](https://travis-ci.org/NLeSC/Xenon.svg?branch=develop)](https://travis-ci.org/NLeSC/Xenon)
[![Build status Windows](https://ci.appveyor.com/api/projects/status/h4l4wn158db23kuf?svg=true)](https://ci.appveyor.com/project/NLeSC/xenon/branch/master)
[![codecov.io](https://codecov.io/github/NLeSC/Xenon/coverage.svg?branch=master)](https://codecov.io/github/NLeSC/Xenon?branch=master)
[![SonarQube Coverage](https://sonarqube.com/api/badges/measure?key=nlesc%3AXenon&metric=coverage)](https://sonarqube.com/component_measures/domain/Coverage?id=nlesc%3AXenon)
[![GitHub license](https://img.shields.io/badge/license-Apache--2.0%20-blue.svg)](https://github.com/NLeSC/Xenon/blob/master/LICENSE)
[![Download](https://jitpack.io/v/NLeSC/Xenon.svg)](https://jitpack.io/#NLeSC/Xenon)
[![DOI](https://zenodo.org/badge/9236864.svg)](https://zenodo.org/badge/latestdoi/9236864)

Copyright 2013 The Netherlands eScience Center

## What problem does Xenon solve?

Many applications use remote storage and compute resources. To do so, they need
to include code to interact with the scheduling systems and file transfer
protocols used on those remote machines.

Unfortunately, many different scheduler systems and file transfer protocols
exist, often with completely different programming interfaces. This makes it
hard for applications to switch to a different system or support multiple
remote systems simultaneously. 

Xenon solves this problem by providing a single programming interface to many
different types of remote resources, allowing applications to switch without
changing a single line of code.

![Xenon abstraction](https://github.com/nlesc/xenon/raw/master/docs/images/readme-xenon-abstraction.svg.png "Xenon abstraction")

## How does Xenon work?

Xenon is an abstraction layer that sits between your application and the remote
resource it uses. Xenon is written in Java, but is also accessible from other
languages (e.g. Python) through its gRPC interface.

![Xenon API](https://github.com/nlesc/xenon/raw/master/docs/images/readme-xenon-api.svg.png "Xenon API")

## Adding Xenon as a dependency to your project

Follow the instructions from [jitpack.io](https://jitpack.io/#NLeSC/Xenon/2.0.0-rc2) to include Xenon as a 
dependency for Gradle, Maven, SBT, or Leiningen projects, e.g. Gradle:

```gradle
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

and 

```gradle
	dependencies {
	        compile 'com.github.NLeSC:Xenon:2.0.0-rc2'
	}

```

Or for a Maven project,

```maven
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```

and


```maven
	<dependency>
	    <groupId>com.github.NLeSC</groupId>
	    <artifactId>Xenon</artifactId>
	    <version>2.0.0-rc2</version>
	</dependency>
```


Simple examples
---------------

Here are some examples of basic operations you can perform with Xenon: 

#### Copying a file from a local filesystem to a remote filesystem

```java
import nl.esciencecenter.xenon.XenonException;
import nl.esciencecenter.xenon.credentials.PasswordCredential;
import nl.esciencecenter.xenon.filesystems.CopyMode;
import nl.esciencecenter.xenon.filesystems.CopyStatus;
import nl.esciencecenter.xenon.filesystems.FileSystem;
import nl.esciencecenter.xenon.filesystems.Path;

public class CopyFileLocalToSftpAbsolutePaths {

    public static void main(String[] args) throws Exception {

        // Use the file system adaptors to create file system representations; the remote file system
        // requires credentials, so we need to create those too.
        //
        // Assume the remote system is actually just a Docker container (e.g.
        // https://hub.docker.com/r/nlesc/xenon-ssh/), accessible via
        // port 10022 on localhost
        String location = "localhost:10022";
        String username = "xenon";
        char[] password = "javagat".toCharArray();
        PasswordCredential credential = new PasswordCredential(username, password);
        FileSystem localFileSystem = FileSystem.create("file");
        FileSystem remoteFileSystem = FileSystem.create("sftp", location, credential);

        // create Paths for the source and destination files, using absolute paths
        Path sourceFile = new Path("/etc/passwd");
        Path destFile = new Path("/tmp/password");

        // create the destination file only if the destination path doesn't exist yet
        CopyMode mode = CopyMode.CREATE;
        boolean recursive = false;

        // perform the copy and wait 1000 ms for the successful or otherwise
        // completion of the operation
        String copyId = localFileSystem.copy(sourceFile, remoteFileSystem, destFile, mode, recursive);
        long timeoutMilliSecs = 1000;
        CopyStatus copyStatus = localFileSystem.waitUntilDone(copyId, timeoutMilliSecs);

        // throw any exceptions
        XenonException copyException = copyStatus.getException();
        if (copyException != null) {
          throw copyException;
        }
    }
}
```

#### Submitting a job

The following code performs a wordcount of a file residing on a remote machine: 

```java 
import nl.esciencecenter.xenon.credentials.PasswordCredential;
import nl.esciencecenter.xenon.schedulers.JobDescription;
import nl.esciencecenter.xenon.schedulers.JobStatus;
import nl.esciencecenter.xenon.schedulers.Scheduler;

public class SlurmSubmitWordCountJob {

    public static void main(String[] args) throws Exception {

        // Assume the remote system is actually just a Docker container (e.g.
        // https://hub.docker.com/r/nlesc/xenon-slurm/), accessible to user 'xenon' via
        // port 10022 on localhost, using password 'javagat'
        String location = "localhost:10022";
        String username = "xenon";
        char[] password = "javagat".toCharArray();
        PasswordCredential credential = new PasswordCredential(username, password);

        // create the SLURM scheduler representation
        Scheduler scheduler = Scheduler.create("slurm", location, credential);

        JobDescription description = new JobDescription();
        description.setExecutable("/usr/bin/wc");
        description.setArguments("-l", "/etc/passwd");
        description.setStdout("/tmp/wc.stdout.txt");

        // submit the job
        String jobId = scheduler.submitBatchJob(description);

        long WAIT_INDEFINITELY = 0;
        JobStatus jobStatus = scheduler.waitUntilDone(jobId, WAIT_INDEFINITELY);

        // print any exceptions
        Exception jobException = jobStatus.getException();
        if (jobException != null)  {
          throw jobException;
        }

    }
}
```

The output of the job will be written to ``/tmp/wc.stdout.txt`` file in the ``nlesc/xenon-slurm`` Docker container.

Supported middleware
--------------------

Xenon currently supports the following file access mechanisms:

- ``file`` (local file manipulation)
- ``ftp``
- ``sftp``
- ``webdav``
- ``s3``

Xenon currently supports the following job submission mechanisms:

- ``local``
- ``ssh``
- ``gridengine``
- ``slurm``
- ``torque``  

Planned extensions include: 

- Swift
- HDFS (almost done)
- YARN
- GridFTP
- glite
- Azure-Batch
- Amazon-Batch

Documentation
-------------

Xenon's JavaDoc is available online at <https://jitpack.io/com/github/NLeSC/Xenon/master-SNAPSHOT/javadoc/index.html>.

Legal
------------------------

The Xenon library is copyrighted by the Netherlands eScience Center and released
under the Apache License, Version 2.0. A copy of the license may be obtained
from [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0).

Xenon uses several third-party libraries that have their own (permissive, open 
source) licenses. See the file [legal/README.md](legal/README.md) for an overview.

