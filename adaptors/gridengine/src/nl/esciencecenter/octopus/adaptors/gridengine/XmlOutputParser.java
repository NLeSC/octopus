package nl.esciencecenter.octopus.adaptors.gridengine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import nl.esciencecenter.octopus.engine.OctopusProperties;
import nl.esciencecenter.octopus.exceptions.OctopusException;
import nl.esciencecenter.octopus.exceptions.OctopusIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlOutputParser {

    private static final Logger logger = LoggerFactory.getLogger(XmlOutputParser.class);

    //string in the xmlns:xsd tag of qstat -xml
    public static String SGE62_SCHEMA_ATTRIBUTE = "xmlns:xsd";

    public static String SGE62_SCHEMA_VALUE =
            "http://gridengine.sunsource.net/source/browse/*checkout*/gridengine/source/dist/util/resources/schemas/qstat/qstat.xsd?revision=1.11";

    private final DocumentBuilder documentBuilder;

    private final boolean ignoreVersion;
    
    XmlOutputParser(OctopusProperties properties) throws OctopusIOException {
        this(properties.getBooleanProperty(GridengineAdaptor.IGNORE_VERSION_PROPERTY));
    }

    XmlOutputParser(boolean ignoreVersion) throws OctopusIOException {
        this.ignoreVersion = ignoreVersion;

        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new OctopusIOException(GridengineAdaptor.ADAPTOR_NAME, "could not create parser for qstat xml files", e);
        }
    }

    void checkVersion(Document document) throws IncompatibleServerException {

        Element de = document.getDocumentElement();

        if (de == null || !de.hasAttribute(SGE62_SCHEMA_ATTRIBUTE) || de.getAttribute(SGE62_SCHEMA_ATTRIBUTE) == null) {

            if (ignoreVersion) {
                logger.warn("cannot determine version, version attribute not found. Ignoring as requested by "
                        + GridengineAdaptor.IGNORE_VERSION_PROPERTY);
            } else {

                throw new IncompatibleServerException(GridengineAdaptor.ADAPTOR_NAME,
                        "cannot determine version, version attribute not found. Use the "
                                + GridengineAdaptor.IGNORE_VERSION_PROPERTY + " property to ignore this error");
            }
        }

        String schemaValue = de.getAttribute(SGE62_SCHEMA_ATTRIBUTE);

        logger.debug("found schema value " + schemaValue);

        //schemaValue == null checked above
        if (!SGE62_SCHEMA_VALUE.equals(schemaValue)) {
            if (ignoreVersion) {
                logger.warn("cannot determine version, version attribute not found. Ignoring as requested by "
                        + GridengineAdaptor.IGNORE_VERSION_PROPERTY);
            } else {

                throw new IncompatibleServerException(GridengineAdaptor.ADAPTOR_NAME, "schema version reported by server ("
                        + schemaValue + ") incompatible with adaptor. Use the " + GridengineAdaptor.IGNORE_VERSION_PROPERTY
                        + " property to ignore this error");
            }
        }

    }

    /**
     * Debugging version of checkVersion function
     * @param file the file to check
     * @throws OctopusException if the version is incorrect
     * @throws OctopusIOException if the file cannot be read or parsed
     */
    void checkVersion(File file) throws OctopusException, OctopusIOException {
        try {
            Document result = documentBuilder.parse(file);
            result.normalize();

            checkVersion(result);
        } catch (SAXException | IOException e) {
            throw new OctopusIOException(GridengineAdaptor.ADAPTOR_NAME, "could not parse qstat xml file", e);
        }
    }

    private Document parseDocument(InputStream in) throws OctopusException, OctopusIOException {
        try {
            Document result = documentBuilder.parse(in);
            result.normalize();

            checkVersion(result);

            return result;
        } catch (SAXException | IOException e) {
            throw new OctopusIOException(GridengineAdaptor.ADAPTOR_NAME, "could not parse qstat xml file", e);
        }
    }

    /**
     * Parses queue info from "qstat -g c -xml"
     * 
     * @param in
     *            the stream to get the xml data from
     * @return a list containing all queue names found
     * @throws OctopusIOException
     *             if the file could not be parsed
     * @throws OctopusException
     *             if the server version is not compatible with this adaptor
     * @throws Exception
     */
    Map<String, Map<String, String>> parseQueueInfos(InputStream in) throws OctopusIOException, OctopusException {
        Document document = parseDocument(in);

        Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();

        logger.debug("root node of xml file: " + document.getDocumentElement().getNodeName());
        NodeList nodes = document.getElementsByTagName("cluster_queue_summary");

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                NodeList tagNodes = element.getChildNodes();

                Map<String, String> queueInfo = new HashMap<String, String>();

                //fetch tags from the list of tag nodes. Ignores empty values
                for (int j = 0; j < tagNodes.getLength(); j++) {
                    Node tagNode = tagNodes.item(j);
                    if (tagNode.getNodeType() == Node.ELEMENT_NODE) {
                        String key = tagNode.getNodeName();
                        if (key != null && key.length() > 0) {
                            NodeList children = tagNode.getChildNodes();
                            if (children.getLength() > 0) {
                                String value = tagNode.getChildNodes().item(0).getNodeValue();
                                queueInfo.put(key, value);
                            }
                        }
                    }
                }

                String queueName = queueInfo.get("name");

                if (queueName == null || queueName.length() == 0) {

                    throw new OctopusIOException(GridengineAdaptor.ADAPTOR_NAME, "found queue in queue list with no name");
                }

                result.put(queueName, queueInfo);
            }
        }

        if (result.size() == 0) {
            throw new OctopusIOException(GridengineAdaptor.ADAPTOR_NAME, "server seems to have no queues");
        }

        return result;
    }
    
    /**
     * Parses job info from "qstat -xml"
     * 
     * @param in
     *            the stream to get the xml data from
     * @return a list containing all queue names found
     * @throws OctopusIOException
     *             if the file could not be parsed
     * @throws OctopusException
     *             if the server version is not compatible with this adaptor
     * @throws Exception
     */
    Map<String, Map<String, String>> parseJobInfos(InputStream in) throws OctopusIOException, OctopusException {
        Document document = parseDocument(in);

        Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();

        logger.debug("root node of xml file: " + document.getDocumentElement().getNodeName());
        NodeList nodes = document.getElementsByTagName("job_list");

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                NodeList tagNodes = element.getChildNodes();

                Map<String, String> jobInfo = new HashMap<String, String>();

                //fetch tags from the list of tag nodes. Ignores empty values
                for (int j = 0; j < tagNodes.getLength(); j++) {
                    Node tagNode = tagNodes.item(j);
                    if (tagNode.getNodeType() == Node.ELEMENT_NODE) {
                        String key = tagNode.getNodeName();
                        if (key != null && key.length() > 0) {
                            NodeList children = tagNode.getChildNodes();
                            if (children.getLength() > 0) {
                                String value = tagNode.getChildNodes().item(0).getNodeValue();
                                jobInfo.put(key, value);
                            }
                        }
                    }
                }
                
                String state = element.getAttribute("state");

                if (state != null && state.length() > 0) {
                    jobInfo.put("state", state);
                }

                String jobID = jobInfo.get("JB_job_number");

                if (jobID == null || jobID.length() == 0) {
                    throw new OctopusIOException(GridengineAdaptor.ADAPTOR_NAME, "found job in queue with no job number");
                }

                result.put(jobID, jobInfo);
            }
        }

        if (result.size() == 0) {
            throw new OctopusIOException(GridengineAdaptor.ADAPTOR_NAME, "server seems to have no queues");
        }

        return result;
    }
}
