package io.fixprotocol.orchestra.quickfix;

import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class OrchestraFileGeneratorTest {

    @Test
    public void generate() throws ParserConfigurationException, JAXBException, SAXException, IOException {
        String[] args = new String[]{Thread.currentThread().getContextClassLoader().getResource("FIX50SP1.xml").getFile(), "target/repository"};
        OrchestraFileGenerator.main(args);
    }
}