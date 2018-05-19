package io.fixprotocol.orchestra.quickfix;

import com.sun.org.apache.xerces.internal.dom.DeferredElementImpl;
import io.fixprotocol._2016.fixrepository.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Generates repository2016 file from fix dictionary
 *
 * @author Onur Karaduman
 */
public class OrchestraFileGenerator {

    /**
     * Runs a OrchestraFileGenerator with command line arguments
     *
     * @param args command line arguments. The first argument is the name of a FIX dictionary file. An
     *             optional second argument is the target directory for generated files. It defaults to
     *             directory "repository".
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws JAXBException
     */
    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, JAXBException {
        OrchestraFileGenerator orchestraFileGenerator = new OrchestraFileGenerator();
        if (args.length >= 1) {
            File inputFile = new File(args[0]);
            File outputDir;
            if (args.length >= 2) {
                outputDir = new File(args[1]);
            } else {
                outputDir = new File("repository");
            }
            orchestraFileGenerator.generate(inputFile, outputDir);
        } else {
            orchestraFileGenerator.useage();
        }

    }

    public void generate(File inputFile, File outputDir) throws ParserConfigurationException, SAXException, IOException, JAXBException {
        Repository repository = new Repository();
        Document document = getDocument(inputFile);

        repository.setFields(generateFields(document.getDocumentElement()));
        repository.setMessages(generateMessages(document.getDocumentElement()));
        repository.setComponents(generateComponents(document.getDocumentElement()));

        File repositoryFile = getRepositoryFilePath(outputDir, document, ".xml");
        outputDir.mkdirs();

        JAXBContext context = JAXBContext.newInstance(Repository.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(repository, repositoryFile);
    }

    private File getRepositoryFilePath(File outputDir, Document root, String extension) {
        String minor = root.getChildNodes().item(0).getAttributes().getNamedItem("minor").getTextContent();
        String major = root.getChildNodes().item(0).getAttributes().getNamedItem("major").getTextContent();

        StringBuilder sb = new StringBuilder();
        sb.append("FixRepository");
        sb.append(major);
        sb.append(minor);
        sb.append(extension);
        return new File(outputDir, sb.toString());
    }


    private void useage() {
        System.out.format("useage: java %s <input-file> <output-dir>", this.getClass().getName());
    }

    private Document getDocument(File file) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    private Fields generateFields(Element element) {
        Fields fields = new Fields();
        Node fieldsNode = element.getElementsByTagName("fields").item(0);
        if (fieldsNode == null) {
            return null;
        }
        NodeList fieldNodeList = fieldsNode.getChildNodes();
        for (int i = 0; i < fieldNodeList.getLength(); i++) {
            Node item = fieldNodeList.item(i);
            if (item instanceof DeferredElementImpl) {
                FieldType fieldType = new FieldType();
                fieldType.setName(((Element) item).getAttribute("name"));
                fieldType.setType(((Element) item).getAttribute("type"));
                fieldType.setId(new BigInteger(((Element) item).getAttribute("number")));
                NodeList childNodes = fieldNodeList.item(i).getChildNodes();
                CodeSetType codeSetType = new CodeSetType();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    Node childItem = childNodes.item(j);
                    if (childItem instanceof DeferredElementImpl) {
                        CodeType codeType = new CodeType();
                        codeType.setName(((Element) childItem).getAttribute("description"));
                        codeType.setValue(((Element) childItem).getAttribute("enum"));
                        codeSetType.getCode().add(codeType);
                    }
                }
                fieldType.setCodeSet(codeSetType);
                fields.getField().add(fieldType);
            }
        }
        return fields;
    }

    private Components generateComponents(Element element) {
        Components components = new Components();
        Node componentsNode = element.getElementsByTagName("components").item(0);
        if (componentsNode == null) {
            return null;
        }
        NodeList componentNodeList = componentsNode.getChildNodes();
        for (int i = 0; i < componentNodeList.getLength(); i++) {
            Node item = componentNodeList.item(i);
            if (item instanceof DeferredElementImpl) {
                ComponentType componentType = new ComponentType();
                componentType.setName(((Element) item).getAttribute("name"));
                NodeList childNodes = item.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    Node childItem = childNodes.item(j);
                    if (childItem instanceof DeferredElementImpl) {
                        if ("field".equals(childItem.getNodeName())) {
                            FieldRefType fieldRefType = new FieldRefType();
                            fieldRefType.setName(((Element) childItem).getAttribute("name"));
                            String required = ((Element) childItem).getAttribute("required");
                            fieldRefType.setPresence(Boolean.valueOf(required) ? PresenceT.REQUIRED : PresenceT.OPTIONAL);
                            componentType.getComponentRefOrGroupRefOrFieldRef().add(fieldRefType);
                        } else if ("group".equals(childItem.getNodeName())) {
                            GroupRefType groupRefType = createGroupRefType(null, childItem);
                            componentType.getComponentRefOrGroupRefOrFieldRef().add(groupRefType);
                        }
                    }
                }
                components.getComponentOrGroup().add(componentType);
            }
        }
        return components;
    }

    private Messages generateMessages(Element element) {
        Messages messages = new Messages();
        Node messagesNode = element.getElementsByTagName("messages").item(0);
        if (messagesNode == null) {
            return null;
        }
        NodeList fieldNodeList = messagesNode.getChildNodes();
        for (int i = 0; i < fieldNodeList.getLength(); i++) {
            Node item = fieldNodeList.item(i);
            if (item instanceof DeferredElementImpl) {
                MessageType messageType = new MessageType();
                messageType.setName(((Element) fieldNodeList.item(i)).getAttribute("name"));
                messageType.setMsgType(((Element) fieldNodeList.item(i)).getAttribute("msgtype"));
                NodeList childNodes = fieldNodeList.item(i).getChildNodes();
                MessageType.Structure structure = new MessageType.Structure();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    Node childItem = childNodes.item(j);
                    if (childItem instanceof DeferredElementImpl) {
                        if ("field".equals(childItem.getNodeName())) {
                            FieldRefType fieldRefType = new FieldRefType();
                            fieldRefType.setName(((Element) childItem).getAttribute("name"));
                            String required = ((Element) childItem).getAttribute("required");
                            fieldRefType.setPresence(Boolean.valueOf(required) ? PresenceT.REQUIRED : PresenceT.OPTIONAL);
                            structure.getComponentOrComponentRefOrGroup().add(fieldRefType);
                        } else if ("component".equals(childItem.getNodeName())) {
                            ComponentRefType componentRefType = new ComponentRefType();
                            componentRefType.setName(((Element) childItem).getAttribute("name"));
                            String required = ((Element) childItem).getAttribute("required");
                            componentRefType.setPresence(Boolean.valueOf(required) ? PresenceT.REQUIRED : PresenceT.OPTIONAL);
                            structure.getComponentOrComponentRefOrGroup().add(componentRefType);
                        } else if ("group".equals(childItem.getNodeName())) {
                            GroupRefType groupRefType = createGroupRefType(null, childItem);
                            structure.getComponentOrComponentRefOrGroup().add(groupRefType);
                        }
                    }
                }
                messageType.setStructure(structure);
                messages.getMessage().add(messageType);
            }
        }
        return messages;
    }

    private GroupRefType createGroupRefType(GroupRefType groupRefTypeParam, Node element) {
        if ("field".equals(element.getNodeName())) {
            FieldRefType fieldRefType = new FieldRefType();
            fieldRefType.setName(((Element) element).getAttribute("name"));
            String required = ((Element) element).getAttribute("required");
            fieldRefType.setPresence(Boolean.valueOf(required) ? PresenceT.REQUIRED : PresenceT.OPTIONAL);
            BlockAssignmentType blockAssignmentType = new BlockAssignmentType();
            blockAssignmentType.getComponentRefOrGroupRefOrFieldRef().add(fieldRefType);
            groupRefTypeParam.getBlockAssignment().add(blockAssignmentType);
        } else if ("group".equals(element.getNodeName())) {
            GroupRefType groupRefType = new GroupRefType();
            groupRefType.setName(((Element) element).getAttribute("name"));
            String required = ((Element) element).getAttribute("required");
            groupRefType.setPresence(Boolean.valueOf(required) ? PresenceT.REQUIRED : PresenceT.OPTIONAL);
            NodeList childNodes = element.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childItem = childNodes.item(i);
                if (childItem instanceof DeferredElementImpl) {
                    createGroupRefType(groupRefType, childItem);
                }
            }
            if (groupRefTypeParam != null) {
                BlockAssignmentType blockAssignmentType = new BlockAssignmentType();
                blockAssignmentType.getComponentRefOrGroupRefOrFieldRef().add(groupRefType);
                groupRefTypeParam.getBlockAssignment().add(blockAssignmentType);
            } else {
                groupRefTypeParam = groupRefType;
            }
        } else if ("component".equals(element.getNodeName())) {
            ComponentRefType componentRefType = new ComponentRefType();
            componentRefType.setName(((Element) element).getAttribute("name"));
            String required = ((Element) element).getAttribute("required");
            componentRefType.setPresence(Boolean.valueOf(required) ? PresenceT.REQUIRED : PresenceT.OPTIONAL);
            BlockAssignmentType blockAssignmentType = new BlockAssignmentType();
            blockAssignmentType.getComponentRefOrGroupRefOrFieldRef().add(componentRefType);
            groupRefTypeParam.getBlockAssignment().add(blockAssignmentType);
        }
        return groupRefTypeParam;
    }
}
