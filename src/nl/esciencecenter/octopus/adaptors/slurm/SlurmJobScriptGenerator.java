package nl.esciencecenter.octopus.adaptors.slurm;

import java.util.Formatter;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import nl.esciencecenter.octopus.engine.util.CommandLineUtils;
import nl.esciencecenter.octopus.exceptions.OctopusException;
import nl.esciencecenter.octopus.files.AbsolutePath;
import nl.esciencecenter.octopus.files.RelativePath;
import nl.esciencecenter.octopus.jobs.JobDescription;

@SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE", justification = "Script generated is a Unix script.")
public final class SlurmJobScriptGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SlurmJobScriptGenerator.class);
    
    private SlurmJobScriptGenerator() {
        //DO NOT USE
    }

    //Stop sonar from complaining about %n vs \n. As these scripts get copied to a unix machine, the newline must be a \n
    
    public static String generate(JobDescription description, AbsolutePath fsEntryPath) throws OctopusException {
        StringBuilder stringBuilder = new StringBuilder();
        Formatter script = new Formatter(stringBuilder, Locale.US);

        script.format("#!/bin/sh\n");

        //set name of job to octopus
        script.format("#SBATCH --job-name octopus\n");

        //set working directory
        if (description.getWorkingDirectory() != null) {
            if (description.getWorkingDirectory().startsWith("/")) {
                script.format("#SBATCH --workdir=%s\n", description.getWorkingDirectory());
            } else {
                //make relative path absolute
                AbsolutePath workingDirectory = fsEntryPath.resolve(new RelativePath(description.getWorkingDirectory()));
                script.format("#SBATCH --workdir=%s\n", workingDirectory.getPath());
            }
        }

        if (description.getQueueName() != null) {
            script.format("#SBATCH --partition=%s\n", description.getQueueName());
        }

        //number of nodes
        script.format("#SBATCH --nodes=%d\n", description.getNodeCount());

        //number of processer per node
        script.format("#SBATCH --ntasks-per-node=%d\n", description.getProcessesPerNode());

        //resulting number of tasks (redundant)
        //script.format("#SBATCH --ntasks=%d\n", description.getNodeCount() * description.getProcessesPerNode());

        //add maximum runtime
        script.format("#SBATCH --time=%d\n", description.getMaxTime());

        if (description.getStdin() != null) {
            script.format("#SBATCH --input=%s\n", description.getStdin());
        }

        if (description.getStdout() == null) {
            script.format("#SBATCH --output=/dev/null\n");
        } else {
            script.format("#SBATCH --output=%s\n", description.getStdout());
        }

        if (description.getStderr() == null) {
            script.format("#SBATCH --error=/dev/null\n");
        } else {
            script.format("#SBATCH --error=%s\n", description.getStderr());
        }

        if (description.getEnvironment() != null) {
            for (Map.Entry<String, String> entry : description.getEnvironment().entrySet()) {
                script.format("export %s=\"%s\"\n", entry.getKey(), entry.getValue());
            }
        }

        script.format("\n");

        //run commands through srun
        script.format("srun ");

        script.format("%s", description.getExecutable());

        for (String argument : description.getArguments()) {
            script.format(" %s", CommandLineUtils.protectAgainstShellMetas(argument));
        }
        script.format("\n");

        script.close();

        LOGGER.debug("Created job script:\n{}", stringBuilder);

        return stringBuilder.toString();
    }
}
