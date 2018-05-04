package io.fixprotocol.orchestra.docgen;

import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;

public class DocGeneratorTest {

    @Test
    public void generate() throws JAXBException, IOException {
        String repositoryPath = this.getClass().getClassLoader().getResource("NYSEPillarBinaryPhase2.xml").getPath();
        String targetPath = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
        DocGenerator.main(new String[]{repositoryPath, targetPath + "/doc"});
    }
}