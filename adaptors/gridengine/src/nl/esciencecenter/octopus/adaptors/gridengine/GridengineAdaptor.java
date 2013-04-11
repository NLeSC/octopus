package nl.esciencecenter.octopus.adaptors.gridengine;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import nl.esciencecenter.octopus.credentials.Credentials;
import nl.esciencecenter.octopus.engine.Adaptor;
import nl.esciencecenter.octopus.engine.OctopusEngine;
import nl.esciencecenter.octopus.engine.OctopusProperties;
import nl.esciencecenter.octopus.exceptions.OctopusException;
import nl.esciencecenter.octopus.files.Files;

public class GridengineAdaptor extends Adaptor {

    public static final String ADAPTOR_NAME = "gridengine";

    private static final String ADAPTOR_DESCRIPTION =
            "The SGE Adaptor submits jobs to a (Sun/Ocacle/Univa) Grid Engine scheduler. This adaptor uses either the local "
                    + "or the ssh adaptor to gain access to the scheduler machine.";

    private static final String[] ADAPTOR_SCHEME = new String[] { "ge", "sge" };

    public static final String PROPERTY_PREFIX = OctopusEngine.ADAPTORS + ADAPTOR_NAME + ".";

    public static final String IGNORE_VERSION_PROPERTY = PROPERTY_PREFIX + "ignore.version";

    /** List of {NAME, DESCRIPTION, DEFAULT_VALUE} for properties. */
    private static final String[][] validPropertiesList =
            new String[][] { { IGNORE_VERSION_PROPERTY, "false",
                    "Boolean: If true, the version check is skipped. WARNING: it is not recommended to use this setting in production environments" }, };

    private final GridEngineJobs jobsAdaptor;

    public GridengineAdaptor(OctopusProperties properties, OctopusEngine octopusEngine) throws OctopusException {
        super(octopusEngine, ADAPTOR_NAME, ADAPTOR_DESCRIPTION, ADAPTOR_SCHEME, validPropertiesList, properties);

        this.jobsAdaptor = new GridEngineJobs(properties, octopusEngine);
    }

    void checkURI(URI location) throws OctopusException {
        if (!supports(location.getScheme())) {
            throw new OctopusException(ADAPTOR_NAME, "Adaptor does not support scheme" + location.getScheme());
        }
    }

    @Override
    public String getName() {
        return ADAPTOR_NAME;
    }

    @Override
    public String getDescription() {
        return "The SGE Adaptor submits jobs to a (Sun/Ocacle/Univa) Grid Engine scheduler. This adaptor uses either the local "
                + "or the ssh adaptor to gain access to the scheduler machine.";
    }

    @Override
    public Map<String, String> getSupportedProperties() {
        return new HashMap<String, String>();
    }

    @Override
    public GridEngineJobs jobsAdaptor() {
        return jobsAdaptor;
    }

    @Override
    public void end() {
        jobsAdaptor.end();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public Files filesAdaptor() throws OctopusException {
        throw new OctopusException(ADAPTOR_NAME, "Adaptor does not support files.");
    }

    @Override
    public Credentials credentialsAdaptor() throws OctopusException {
        throw new OctopusException(ADAPTOR_NAME, "Adaptor does not support credentials.");
    }

}
