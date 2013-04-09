package nl.esciencecenter.octopus.adaptors.local;

import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nl.esciencecenter.octopus.ImmutableTypedProperties;
import nl.esciencecenter.octopus.engine.OctopusEngine;
import nl.esciencecenter.octopus.engine.jobs.JobImplementation;
import nl.esciencecenter.octopus.engine.jobs.JobsAdaptor;
import nl.esciencecenter.octopus.engine.jobs.SchedulerImplementation;
import nl.esciencecenter.octopus.exceptions.BadParameterException;
import nl.esciencecenter.octopus.exceptions.OctopusException;
import nl.esciencecenter.octopus.jobs.Job;
import nl.esciencecenter.octopus.jobs.JobDescription;
import nl.esciencecenter.octopus.jobs.JobStatus;
import nl.esciencecenter.octopus.jobs.Scheduler;
import nl.esciencecenter.octopus.security.Credentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalJobs implements JobsAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(LocalJobs.class);
    
    private final OctopusEngine octopusEngine;

    private final LinkedList<LocalJobExecutor> singleQ;

    private final LinkedList<LocalJobExecutor> multiQ;

    private final LinkedList<LocalJobExecutor> unlimitedQ;

    private final ExecutorService singleExecutor;

    private final ExecutorService multiExecutor;

    private final ExecutorService unlimitedExecutor;
    
    private final int maxQSize;
    
    private final LocalAdaptor localAdaptor;
    
    private final ImmutableTypedProperties properties;

    private static int jobID = 0;

    private static synchronized int getNextJobID() { 
    	return jobID++;
    }
    
    //private final LocalScheduler scheduler;

    public LocalJobs(ImmutableTypedProperties properties, LocalAdaptor localAdaptor, OctopusEngine octopusEngine)
            throws OctopusException {
    	
        this.octopusEngine = octopusEngine;
        this.localAdaptor = localAdaptor;
        this.properties = properties;

        singleQ = new LinkedList<LocalJobExecutor>();
        multiQ = new LinkedList<LocalJobExecutor>();
        unlimitedQ = new LinkedList<LocalJobExecutor>();

        unlimitedExecutor = Executors.newCachedThreadPool();
        singleExecutor = Executors.newSingleThreadExecutor();

        int processors = Runtime.getRuntime().availableProcessors();
        int multiQThreads = properties.getIntProperty(LocalAdaptor.LOCAL_MULTIQ_MAX_JOBS, processors);
        multiExecutor = Executors.newFixedThreadPool(multiQThreads);
        
        maxQSize = properties.getIntProperty(LocalAdaptor.LOCAL_Q_HISTORY_SIZE, LocalAdaptor.DEFAULT_LOCAL_Q_HISTORY_SIZE);
        
        if (maxQSize < 0 && maxQSize != -1) {
            throw new BadParameterException("max q size cannot be negative (excluding -1 for unlimited)", "local", null);
        }
        
        // this.scheduler = new LocalScheduler(properties, octopusEngine);
    }

    @Override
    public Scheduler newScheduler(ImmutableTypedProperties properties, Credentials credentials, URI location)
            throws OctopusException {

    	localAdaptor.checkURI(location);

        if (location.getPath() != null && location.getPath().length() > 0) {
            throw new OctopusException("Non-empty path in a local scheduler URI is not allowed", "local", location);
        }

    	// FIXME: This simply returns a new SchedulerImplementation, but ignores properties and credentials completely.    	
        return new SchedulerImplementation(location, properties, credentials, "local");
    }

    public void end() {
    	singleExecutor.shutdownNow();
    	multiExecutor.shutdownNow();
    	unlimitedExecutor.shutdownNow();
    }

    @Override
	public String[] getQueueNames(Scheduler scheduler) throws OctopusException {
        return new String[] { "single", "multi", "unlimited" };
    }
    
    //remove finished jobs from a q until the maximum number of jobs limit is met.
    private synchronized void purgeQ(LinkedList<LocalJobExecutor> q) {
        if (maxQSize == -1) {
            return;
        }
        
        //how many jobs do we need to remove
        int purgeCount = q.size() - maxQSize; 
        
        if (purgeCount <= 0) {
            return;
        }
        
        Iterator<LocalJobExecutor> iterator = q.iterator();
        
        while(iterator.hasNext() && purgeCount > 0) {
            if (iterator.next().isDone()) {
                iterator.remove();
                purgeCount--;
            }
        }
    }

    private Job[] getJobs(LocalJobExecutor [] executors) { 
    
    	LocalJobExecutor [] tmp = singleQ.toArray(new LocalJobExecutor[0]);
		
		Job[] result = new Job[tmp.length];
		
		for (int i=0;i<tmp.length;i++) { 
			result[i] = tmp[i].getJob();
		}
		
		return result;
    }
    
    @Override
    public synchronized Job[] getJobs(Scheduler scheduler, String queueName) throws OctopusException {
        
    	if (queueName == null || queueName.equals("single")) {
            return getJobs(singleQ.toArray(new LocalJobExecutor[0]));
        } else if (queueName.equals("multi")) {
            return getJobs(multiQ.toArray(new LocalJobExecutor[0]));
        } else if (queueName.equals("unlimited")) {
            return getJobs(unlimitedQ.toArray(new LocalJobExecutor[0]));
        } else {
            throw new BadParameterException("queue \"" + queueName + "\" does not exist", "local", null);
        }
    }

	@Override
	public JobStatus getJobStatus(Job job) throws OctopusException {
		
		
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JobStatus[] getJobStatuses(Job... jobs) throws OctopusException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void cancelJob(Job job) throws OctopusException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void cancelJobs(Job... jobs) throws OctopusException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Job submitJob(Scheduler scheduler, JobDescription description) throws OctopusException {
		
		Job result = new JobImplementation(description, scheduler, "localjob-" + getNextJobID());
		
		LocalJobExecutor executor = new LocalJobExecutor(result, octopusEngine);

		String queueName = description.getQueueName();

		if (queueName == null || queueName.equals("single")) {
			singleQ.add(executor);
			singleExecutor.execute(executor);
		} else if (queueName.equals("multi")) {
			multiQ.add(executor);
			multiExecutor.execute(executor);
		} else if (queueName.equals("unlimited")) {
			unlimitedQ.add(executor);
			unlimitedExecutor.execute(executor);
		} else {
			throw new BadParameterException("queue \"" + queueName + "\" does not exist", "local", null);
		}

		 //purge jobs from q if needed (will not actually cancel execution of jobs)
		 purgeQ(singleQ);
		 purgeQ(multiQ);
		 purgeQ(unlimitedQ);

		 return result;
	}

	@Override
	public Job[] submitJobs(Scheduler scheduler, JobDescription... descriptions) throws OctopusException {

		Job[] result = new Job[descriptions.length]; 
		
		for (int i=0;i<descriptions.length;i++) { 
			result[i] = submitJob(scheduler, descriptions[i]);
		}
		
		return result;
	}

}