Xenon 2.3.0
-----------

This is release 2.3.0 of Xenon. 

Notable changes compared to v2.2.0:
-----------------------------------

- added an HDFS filesystem adaptor 
- fixed bug in GridEngineSchedulers for complex configurations of number of slots per node
- various code cleanups, etc.

Notable changes compared to v2.1.0:
-----------------------------------

- extended CredentialMap to retrieve all keys 
- removed logback config from jar 
- fixed bug in handling workdir of Local and TorqueSchedulers
- many small bugfixes, additional tests, etc.


Notable changes compared to v2.0.0:
-----------------------------------

- added getCredential to Scheduler and FileSystem 
- fixed a bug in equals of CredentialMap
- added proper check of supported credential types in adaptors 
- many small bugfixes, additional tests, etc.

Notable changes compared to v1.2.3:
-----------------------------------

- complete overhaul of public API, which should increase ease-of-use significantly. 
- complete overhaul of integration test framework, which should improve performance and make it easier to test against different versions of the same middleware.
- complete overhaul of implementation, which should make implementing adaptors much more straightforward.
- replaced Jsch with Apache SSHD in the SSH and SFTP adaptors
- replaced Apache Jackrabbit with Sardine in the Webdav adaptor. 
- added an S3 filesystem adaptor. 

Notable changes compared to v1.2.2:
-----------------------------------

- fixed various issues flagged by sonarqube

Notable changes compared to v1.2.1:
-----------------------------------

- fixed bug in the copy engine that would ignore a copy if source and destination had exactly the same path (even when on different machines).
- added timeout overflow detection in Jobs.waitUntilDone and Jobs.waitUntilRunning.
- added SonarQube code for quality analysis and coverage
- we have a new logo!

Notable changes compared to v1.2.0:
-----------------------------------

- fixed nasty inconsistency in adaptor implementations of waiting for jobs to start or finish.

Notable changes between v1.2.0 and v1.1.0:
------------------------------------------

- added support for WebDAV file access.
- added OSX testing in Travis
- added support for Slurm version 15.08.6
- fixed several bugs related to Windows local file system semantics
- many small bugfixes, additional tests, etc. 


Notable changes between v1.1.0 and v1.0.0:
------------------------------------------
 
- added support for FTP file access.
- added support for Torque resource manager.
- added support for Slurm versions 2.6.0, 14.03.0 and 14.11.9-Bull.1.0.
- added option to specify resources in sge adaptor.
- added support for SSH agent and agent proxies
- added a -lot- of unit and integration tests
- javadoc is java 8 compliant.
- the adaptor documentation is now part of the javadoc.
- Xenon releases are now available in jCenter.
- switched from and ant to gradle based build system, this is reflected in the directory structure 
- split unit and integration tests
- added docker support for integration tests
- now using travis-ci for continous integration 
- now using PMD and codacy for code quality
- now using codecov for unit and integration test coverage
- moved examples and tutorial to a separate repository https://github.com/NLeSC/Xenon-examples


What's missing:
---------------
	
The GridFTP adaptor is not considered stable yet. It is not part of this release.

There is no adaptor writing documentation at the moment, nor is the Javadoc complete for the internals methods of the adaptor implementations.

It should be made easier to inspect at runtime which adaptors are available and what properties they support.

We can always use more adaptors, e.g, for SWIFT, HDFS, YARN, Azure-Batch, etc. These are planned for 2.1.0 or later.

We can always use more interfaces, e.g. for clouds (to start and stop VMs). This is planned for 3.0.0.



