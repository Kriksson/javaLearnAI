package learning.task021;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CompilerPluginTest {
    @Test
    void pomConfiguresCompilerPluginWithJavaReleaseProperty() throws Exception {
        Document document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(Path.of("pom.xml").toFile());

        Element compilerPlugin = findCompilerPlugin(document);
        assertNotNull(compilerPlugin, "Добавь maven-compiler-plugin в build/plugins");
        assertEquals("3.13.0", childValue(compilerPlugin, "version"));

        Element configuration = childElement(compilerPlugin, "configuration");
        assertNotNull(configuration, "Добавь configuration для compiler plugin");
        assertEquals("${maven.compiler.release}", childValue(configuration, "release"));
        assertEquals("21", propertyValue(document, "maven.compiler.release"));
    }

    private Element findCompilerPlugin(Document document) {
        NodeList plugins = document.getElementsByTagName("plugin");
        for (int index = 0; index < plugins.getLength(); index++) {
            Element plugin = (Element) plugins.item(index);
            if ("maven-compiler-plugin".equals(childValue(plugin, "artifactId"))) {
                return plugin;
            }
        }
        return null;
    }

    private String propertyValue(Document document, String name) {
        NodeList properties = document.getElementsByTagName(name);
        return properties.item(0).getTextContent().trim();
    }

    private String childValue(Element parent, String name) {
        Element child = childElement(parent, name);
        return child == null ? null : child.getTextContent().trim();
    }

    private Element childElement(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element element && name.equals(element.getTagName())) {
                return element;
            }
        }
        return null;
    }
}
