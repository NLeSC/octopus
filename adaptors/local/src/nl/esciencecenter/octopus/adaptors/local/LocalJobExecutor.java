package nl.esciencecenter.octopus.adaptors.local;

import java.io.IOException;

import nl.esciencecenter.octopus.engine.OctopusEngine;
import nl.esciencecenter.octopus.exceptions.BadParameterException;
import nl.esciencecenter.octopus.exceptions.OctopusException;
import nl.esciencecenter.octopus.jobs.Job;
import nl.esciencecenter.octopus.jobs.JobDescription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalJobExecutor implements Runnable {

    protected static Logger logger = LoggerFactory.getLogger(LocalJobExecutor.class);
    
    private final OctopusEngine engine;

    private final Job job;
    
    private int exitStatus;

    private Thread thread = null;

    private boolean killed = false;
    
    private boolean done = false;

    private String state = "INITIAL";
    
    private Exception error;
    
    public LocalJobExecutor(Job job, OctopusEngine engine) throws BadParameterException {

    	this.engine = engine;
    	this.job = job;

        if (job.getJobDescription().getProcessesPerNode() <= 0) {
            throw new BadParameterException("number of processes cannot be negative or 0", "local", null);
        }
        
        if (job.getJobDescription().getNodeCount() != 1) {
            throw new BadParameterException("number of nodes must be 1", "local", null);
        }

        // thread will be started by local scheduler
    }

    public synchronized int getExitStatus() throws OctopusException {
        if (!isDone()) {
            throw new OctopusException("Cannot get state, job not done yet", "local", null);
        }
        return exitStatus;
    }

    private synchronized void setExitStatus(int exitStatus) {
        this.exitStatus = exitStatus;
    }

    public synchronized void kill() throws OctopusException {
        killed = true;

        if (thread != null) {
            thread.interrupt();
        }
    }
    
    private synchronized void updateState(String state) {
        this.state = state;
    }
    
    private synchronized void setError(Exception e) { 
    	error = e;
    }
	
	public synchronized boolean isDone() { 
		return done;
	}
    
	public Job getJob() {
		return job;
	}

	public synchronized String getState() { 
		return state;
	}
	
	public synchronized Exception getError() { 
		return error;
	}
	
    @Override
    public void run() {
        
    	try {
          
            synchronized (this) {
                if (killed) {
                	updateState("KILLED");
                	throw new IOException("Job killed");
                }
                this.thread = Thread.currentThread();
            }

            updateState("INITIAL");

            if (Thread.currentThread().isInterrupted()) {
            	updateState("KILLED");
                throw new IOException("Job killed");
            }

            JobDescription description = job.getJobDescription();
            
            ParallelProcess parallelProcess = new ParallelProcess(description.getProcessesPerNode(),
                    description.getExecutable(), description.getArguments(), description.getEnvironment(),
                    description.getWorkingDirectory(), description.getStdin(), description.getStdout(), description.getStderr(), 
                    engine);

            updateState("RUNNING");

            try {
                setExitStatus(parallelProcess.waitFor());
            } catch (InterruptedException e) {
                parallelProcess.destroy();
            }

        } catch (IOException e) {
        	setError(e);
            updateState("ERROR");
        }
    }
}
