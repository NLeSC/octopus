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
package nl.esciencecenter.octopus.engine.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import nl.esciencecenter.octopus.exceptions.CommandNotFoundException;
import nl.esciencecenter.octopus.exceptions.OctopusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a command. Constructor waits for command to finish.
 * 
 * @author Niels Drost
 * 
 */
public class CommandRunner {

    protected static Logger logger = LoggerFactory.getLogger(CommandRunner.class);

    private final int exitCode;

    private final OutputReader out;

    private final OutputReader err;
    
    // determine location of exe file using path, will return given location if
    // not found in path
    private static String getExeFile(String exe) {
        String path = System.getenv("PATH");

        if (path != null) {
            for (String pathElement : path.split(File.pathSeparator)) {
                if (!pathElement.isEmpty()) {
                    String candidateLocation = pathElement + File.separator + exe;
                    File file = new File(candidateLocation);
                    if (file.canExecute()) {
                        return candidateLocation;
                    }
                } else {
                    // special case empty path element
                    File f = new File(exe);
                    if (f.canExecute()) {
                        return exe;
                    }
                }
            }
        }
        return exe;
    }

    public CommandRunner(String... command) throws OctopusException {
        this(null, null, command);
    }

    public CommandRunner(String stdin, File workingDir, String... command) throws CommandNotFoundException {
        if (command.length == 0) {
            throw new ArrayIndexOutOfBoundsException("runCommand: command array has length 0");
        }

        // expand command using path
        command[0] = getExeFile(command[0]);
        if (logger.isDebugEnabled()) {
            logger.debug("CommandRunner running: " + Arrays.toString(command));
        }
        ProcessBuilder builder = new ProcessBuilder(command);
        if (workingDir != null) {
            builder.directory(workingDir);
        }
        java.lang.Process p = null;
        try {
            p = builder.start();
        } catch (IOException e) {
            throw new CommandNotFoundException(getClass().getName(), "CommandRunner cannot run command "
                    + Arrays.toString(command), e);
        }

        if (stdin == null) {
            stdin = "";
        }
        
        //write given content to stdin of process
        new InputWriter(stdin, p.getOutputStream());

        // we must always read the output and error streams to avoid deadlocks
        out = new OutputReader(p.getInputStream());
        err = new OutputReader(p.getErrorStream());

        int exitCode = 0;
        try {
            exitCode = p.waitFor();

            out.waitUntilFinished();
            err.waitUntilFinished();

            if (logger.isDebugEnabled()) {
                logger.debug("CommandRunner out: " + out.getResult() + "\n" + "CommandRunner err: " + err.getResult());
            }

        } catch (InterruptedException e) {
            // IGNORE
        }
        this.exitCode = exitCode;
    }

    public String getStdout() {
        return out.getResult();
    }

    public String getStderr() {
        return err.getResult();
    }

    public int getExitCode() {
        return exitCode;
    }

}
