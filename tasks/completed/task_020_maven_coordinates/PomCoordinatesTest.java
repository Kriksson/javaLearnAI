package learning.task020;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PomCoordinatesTest {
    @Test
    void pomUsesLearningProjectCoordinates() throws Exception {
        Document document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(Path.of("pom.xml").toFile());
        Element project = document.getDocumentElement();

        assertEquals("ru.kriksson.javacourse", valueOf(project, "groupId"));
        assertEquals("java-learning", valueOf(project, "artifactId"));
        assertEquals("1.0.0", valueOf(project, "version"));
    }

    private String valueOf(Element project, String tagName) {
        NodeList elements = project.getElementsByTagName(tagName);
        return elements.item(0).getTextContent().trim();
    }
}
