package nl.esciencecenter.octopus.engine;

import java.util.Properties;
import java.util.Vector;

import nl.esciencecenter.octopus.AdaptorInfo;
import nl.esciencecenter.octopus.OctopusProperties;
import nl.esciencecenter.octopus.Octopus;
import nl.esciencecenter.octopus.credentials.Credentials;
import nl.esciencecenter.octopus.engine.credentials.CredentialsEngine;
import nl.esciencecenter.octopus.engine.files.FilesEngine;
import nl.esciencecenter.octopus.engine.jobs.JobsEngine;
import nl.esciencecenter.octopus.exceptions.OctopusException;
import nl.esciencecenter.octopus.exceptions.OctopusRuntimeException;
import nl.esciencecenter.octopus.files.Files;
import nl.esciencecenter.octopus.jobs.Jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of Octopus Interface class. Redirects calls to adaptors, and the FilesEngine
 * 
 * @author Niels Drost
 */
public class OctopusEngine implements Octopus {

    /** All our own properties start with this prefix. */
    public static final String PREFIX = "octopus.";

    /** All our own queue properties start with this prefix. */
    public static final String ADAPTORS = PREFIX + "adaptors.";

    /** All our own queue properties start with this prefix. */
    public static final String LOAD = ADAPTORS + "load";

    /** List of {NAME, DESCRIPTION, DEFAULT_VALUE} for properties. */
    private static final String[][] VALID_PROPERTIES = new String[][] {
            { LOAD, null, "List: comma separated list of the adaptors to load." }};
    
    private static final Logger logger = LoggerFactory.getLogger(OctopusEngine.class);

    // list of all octopusEngines, so we can end them all in one go.
    private static final Vector<OctopusEngine> octopusEngines = new Vector<OctopusEngine>();

    public static Octopus newEngine(Properties properties) throws OctopusException {
        OctopusEngine result = new OctopusEngine(properties);

        octopusEngines.add(result);

        return result;
    }

    public static void endEngines() {
        for (OctopusEngine octopusEngine : octopusEngines) {
            octopusEngine.end();
        }
    }

    private boolean ended = false;

    private OctopusProperties defaultProperties;

    private final FilesEngine filesEngine;

    private final JobsEngine jobsEngine;

    private final CredentialsEngine credentialsEngine;

    private final Adaptor[] adaptors;

    /**
     * Constructs a OctopusEngine instance.
     * 
     * @param credentials
     *            the credentials to use. Will NOT be copied.
     * @param properties
     *            the properties to use. Will NOT be copied.
     * @throws OctopusException
     */
    private OctopusEngine(Properties properties) throws OctopusException {
      
        defaultProperties = new OctopusProperties(VALID_PROPERTIES, properties);
        
        adaptors = AdaptorLoader.loadAdaptors(defaultProperties, this);

        filesEngine = new FilesEngine(this);

        jobsEngine = new JobsEngine(this);

        credentialsEngine = new CredentialsEngine(this);

        logger.info("Octopus engine initialized with adaptors: " + adaptors);
    }
    
    public synchronized OctopusProperties getCombinedProperties(Properties properties) {
        OctopusProperties result = new OctopusProperties(defaultProperties, properties);

        return result;
    }

    // ************** Octopus Interface Implementation ***************\\

    @Override
    public AdaptorInfo[] getAdaptorInfos() {
        return adaptors.clone();
    }

    @Override
    public AdaptorInfo getAdaptorInfo(String adaptorName) throws OctopusException {
        return getAdaptor(adaptorName);
    }

    /**
     * Return the adaptor that provides functionality for the given scheme.
     * 
     * @param scheme
     *            the scheme for which to get the adaptor
     * @return the adaptor
     */
    public Adaptor getAdaptorFor(String scheme) throws OctopusException {
        
        for (Adaptor adaptor : adaptors) {
            if (adaptor.supports(scheme)) {
                return adaptor;
            }
        }
        throw new OctopusException("cannot find adaptor for scheme " + scheme, null, null);
    }

    public Adaptor getAdaptor(String name) throws OctopusException {
        for (Adaptor adaptor : adaptors) {
            if (adaptor.getName().equals(name)) {
                return adaptor;
            }
        }

        throw new OctopusException("could not find adaptor named " + name, null, null);
    }

    public Adaptor[] getAdaptors() {
        return adaptors;
    }

    @Override
    public synchronized Properties getDefaultProperties() {
        return defaultProperties;
    }

    @Override
    public synchronized void setDefaultProperties(Properties properties) {
        
        synchronized (this) {
            defaultProperties = new OctopusProperties(VALID_PROPERTIES, properties);
        }
        
        // FIXME: Should check properties here!
    }

    @Override
    public Files files() {
        return filesEngine;
    }

    @Override
    public Jobs jobs() {
        return jobsEngine;
    }

    @Override
    public Credentials credentials() {
        return credentialsEngine;
    }

    @Override
    public void end() {
        synchronized (this) {
            if (ended) {
                return;
            }

            ended = true;
        }

        for (Adaptor adaptor : adaptors) {
            adaptor.end();
        }
    }
}
