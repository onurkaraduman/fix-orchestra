package io.fixprotocol.orchestra.quickfix;

import com.sun.org.apache.xerces.internal.dom.DeferredElementImpl;
import io.fixprotocol._2016.fixrepository.*;
import org.purl.dc.terms.ElementOrRefinementContainer;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

            orchestraFileGenerator.generate(inputFile, outputDir, true);
        } else {
            orchestraFileGenerator.useage();
        }

    }

    int componentCounter = 1111111;
    private final Map<String, ComponentType> componentsMap = new HashMap<>();
    private final Map<String, GroupType> groupsMap = new HashMap<>();
    private final Map<String, FieldType> fieldsMap = new HashMap<>();

    public void generate(File inputFile, File outputDir, boolean generateDataType) throws ParserConfigurationException, SAXException, IOException, JAXBException {
        Repository repository = new Repository();
        Document document = getDocument(inputFile);

        addFields(document.getDocumentElement(), repository, generateDataType);
        addComponents(document.getDocumentElement(), repository);
        addMessages(document.getDocumentElement(), repository);
        repository.setMetadata(new ElementOrRefinementContainer());
        repository.setVersion(findVersion(document));

        File repositoryFile = getRepositoryFilePath(outputDir, document, ".xml");
        outputDir.mkdirs();

        JAXBContext context = JAXBContext.newInstance(Repository.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(repository, repositoryFile);
    }

    private File getRepositoryFilePath(File outputDir, Document root, String extension) {

        StringBuilder sb = new StringBuilder();
        sb.append("Repository");
        sb.append(findVersion(root));
        sb.append(extension);
        return new File(outputDir, sb.toString());
    }


    private String findVersion(Document root) {
        String minor = root.getChildNodes().item(0).getAttributes().getNamedItem("minor").getTextContent();
        String major = root.getChildNodes().item(0).getAttributes().getNamedItem("major").getTextContent();

        StringBuilder sb = new StringBuilder();
        if (minor.equals("1") && major.equals("1")) {
            sb.append("FIXT");
        } else {
            sb.append("FIX");
        }
        sb.append(".");
        sb.append(major);
        sb.append(".");
        sb.append(minor);
        return sb.toString();
    }

    private void useage() {
        System.out.format("useage: java %s <input-file> <output-dir>", this.getClass().getName());
    }

    private Document getDocument(File file) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    private void addFields(Element element, Repository repository, boolean generateDataType) {

        if (repository.getFields() == null) {
            repository.setFields(new Fields());
        }

        Node fieldsNode = element.getElementsByTagName("fields").item(0);
        if (fieldsNode == null) {
            return;
        }
        if (repository.getCodeSets() == null) {
            repository.setCodeSets(new CodeSets());
        }
        if (repository.getDatatypes() == null) {
            repository.setDatatypes(new Datatypes());
        }

        Set<String> dataTypeNameSet = new HashSet<>();

        NodeList fieldNodeList = fieldsNode.getChildNodes();
        for (int i = 0; i < fieldNodeList.getLength(); i++) {
            Node item = fieldNodeList.item(i);
            if (item instanceof DeferredElementImpl) {
                FieldType fieldType = new FieldType();
                fieldType.setName(((Element) item).getAttribute("name"));
                fieldType.setType(((Element) item).getAttribute("type"));
                fieldType.setId(new BigInteger(((Element) item).getAttribute("number")));

                dataTypeNameSet.add(((Element) item).getAttribute("type"));

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
                if (!codeSetType.getCode().isEmpty()) {
                    String codeSetName = ((Element) item).getAttribute("name") + "CodeSet";
                    fieldType.setType(codeSetName);

                    codeSetType.setId(fieldType.getId());
                    codeSetType.setName(codeSetName);
                    codeSetType.setType(((Element) item).getAttribute("type"));

                    fieldType.setCodeSet(codeSetType);
                    repository.getCodeSets().getCodeSet().add(codeSetType);
                }
                fieldsMap.put(fieldType.getName(), fieldType);
                repository.getFields().getField().add(fieldType);
            }
        }
        for (String dataTypeName : dataTypeNameSet) {
            Datatype datatype = new Datatype();
            datatype.setName(dataTypeName);
            repository.getDatatypes().getDatatype().add(datatype);
        }
    }

    private void addComponents(Element element, Repository repository) {
        if (repository.getComponents() == null) {
            repository.setComponents(new Components());
        }

        Node componentsNode = element.getElementsByTagName("components").item(0);
        if (componentsNode == null) {
            return;
        }

        NodeList componentNodeList = componentsNode.getChildNodes();
        for (int i = 0; i < componentNodeList.getLength(); i++) {
            Node item = componentNodeList.item(i);
            if (item instanceof DeferredElementImpl) {
                ComponentType componentType = new ComponentType();
                componentType.setName(((Element) item).getAttribute("name"));
                componentType.setId(getComponentId(componentType.getName()));

                componentsMap.put(componentType.getName(), componentType);

                NodeList childNodes = item.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    Node childItem = childNodes.item(j);
                    if (childItem instanceof DeferredElementImpl) {
                        if ("field".equals(childItem.getNodeName())) {
                            FieldRefType fieldRefType = new FieldRefType();
                            fieldRefType.setName(((Element) childItem).getAttribute("name"));
                            fieldRefType.setPresence(getPresence(((Element) childItem).getAttribute("required")));
                            fieldRefType.setId(getFieldId(fieldRefType.getName()));
                            componentType.getComponentRefOrGroupRefOrFieldRef().add(fieldRefType);
                        } else if ("group".equals(childItem.getNodeName())) {
                            GroupType groupType = createGroupType(null, childItem, repository);

                            GroupRefType groupRefType = new GroupRefType();
                            groupRefType.setName(groupType.getName());
                            groupRefType.setPresence(getPresence(((Element) childItem).getAttribute("required")));
                            groupRefType.setId(getGroupId(groupRefType.getName()));

                            componentType.getComponentRefOrGroupRefOrFieldRef().add(groupRefType);
                        }
                    }
                }
                repository.getComponents().getComponentOrGroup().add(componentType);
            }
        }
        if (!repository.getComponents().getComponentOrGroup().isEmpty()) {
            repository.setHasComponents(true);
        }
    }

    private void addMessages(Element element, Repository repository) {
        if (repository.getMessages() == null) {
            repository.setMessages(new Messages());
        }

        Node messagesNode = element.getElementsByTagName("messages").item(0);
        if (messagesNode == null) {
            return;
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
                            fieldRefType.setPresence(getPresence(((Element) childItem).getAttribute("required")));
                            fieldRefType.setId(fieldsMap.get(fieldRefType.getName()).getId());
                            structure.getComponentOrComponentRefOrGroup().add(fieldRefType);
                        } else if ("component".equals(childItem.getNodeName())) {
                            ComponentRefType componentRefType = new ComponentRefType();
                            componentRefType.setName(((Element) childItem).getAttribute("name"));
                            componentRefType.setPresence(getPresence(((Element) childItem).getAttribute("required")));
                            componentRefType.setId(getComponentId(componentRefType.getName()));
                            structure.getComponentOrComponentRefOrGroup().add(componentRefType);
                        } else if ("group".equals(childItem.getNodeName())) {
                            GroupType groupType = createGroupType(null, childItem, repository);

                            GroupRefType groupRefType = new GroupRefType();
                            groupRefType.setName(groupType.getName());
                            groupRefType.setPresence(getPresence(((Element) childItem).getAttribute("required")));
                            groupRefType.setId(getGroupId(groupRefType.getName()));

                            structure.getComponentOrComponentRefOrGroup().add(groupRefType);
                        }
                    }
                }
                messageType.setStructure(structure);
                messageType.setCategory(getMessageCategory(((Element) fieldNodeList.item(i)).getAttribute("msgcat")));
                repository.getMessages().getMessage().add(messageType);
            }
        }
    }

    private GroupType createGroupType(GroupType groupType, Node element, Repository repository) {
        if ("field".equals(element.getNodeName())) {
            FieldRefType fieldRefType = new FieldRefType();
            fieldRefType.setName(((Element) element).getAttribute("name"));
            fieldRefType.setPresence(getPresence(((Element) element).getAttribute("required")));
            fieldRefType.setId(getFieldId(fieldRefType.getName()));
            groupType.getComponentRefOrGroupRefOrFieldRef().add(fieldRefType);
        } else if ("group".equals(element.getNodeName())) {
            if (groupType == null) {
                groupType = new GroupType();
                groupType.setNumInGroupName(((Element) element).getAttribute("name"));
                groupType.setName(((Element) element).getAttribute("name"));
                groupType.setId(getGroupId(groupType.getName()));
                groupType.setNumInGroupId(getNumInGroupId(groupType.getNumInGroupName()));
            } else {
                GroupRefType groupRefType = new GroupRefType();
                groupRefType.setName(((Element) element).getAttribute("name"));
                groupRefType.setPresence(getPresence(((Element) element).getAttribute("required")));
                groupRefType.setId(getGroupId(groupRefType.getName()));

                groupType.getComponentRefOrGroupRefOrFieldRef().add(groupRefType);

                groupType = new GroupType();
                groupType.setNumInGroupName(((Element) element).getAttribute("name"));
                groupType.setName(((Element) element).getAttribute("name"));
                groupType.setId(getGroupId(groupType.getName()));
                groupType.setNumInGroupId(getNumInGroupId(groupType.getName()));
            }

            NodeList childNodes = element.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childItem = childNodes.item(i);
                if (childItem instanceof DeferredElementImpl) {
                    createGroupType(groupType, childItem, repository);
                }
            }
            if (repository.getComponents() == null) {
                repository.setComponents(new Components());
            }
            groupsMap.put(groupType.getName(), groupType);
            repository.getComponents().getComponentOrGroup().add(groupType);
        } else if ("component".equals(element.getNodeName())) {
            ComponentRefType componentRefType = new ComponentRefType();
            componentRefType.setName(((Element) element).getAttribute("name"));
            componentRefType.setPresence(getPresence(((Element) element).getAttribute("required")));
            componentRefType.setId(getComponentId(componentRefType.getName()));
            groupType.getComponentRefOrGroupRefOrFieldRef().add(componentRefType);
        }
        return groupType;
    }


    private PresenceT getPresence(String value) {
        return ("Y").equals(value) ? PresenceT.REQUIRED : PresenceT.OPTIONAL;
    }

    private String getMessageCategory(String msgcat) {
        if (msgcat != null && msgcat.equals("admin")) {
            return "Session";
        } else {
            return "Common";
        }
    }

    private BigInteger getGroupId(String name) {
        if (groupsMap.get(name) == null) {
            GroupType groupType = new GroupType();
            groupType.setName(name);
            groupType.setId(BigInteger.valueOf(componentCounter++));
            groupsMap.put(name, groupType);
        }
        return groupsMap.get(name).getId();
    }

    private BigInteger getComponentId(String name) {
        if (componentsMap.get(name) == null) {
            ComponentType componentType = new ComponentType();
            componentType.setId(BigInteger.valueOf(componentCounter++));
            componentsMap.put(name, componentType);
        }
        return componentsMap.get(name).getId();
    }

    private BigInteger getFieldId(String name) {
        return fieldsMap.get(name).getId();
    }

    private BigInteger getNumInGroupId(String name) {
        return fieldsMap.get(name).getId();
    }
}