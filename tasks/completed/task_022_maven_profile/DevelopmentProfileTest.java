package learning.task022;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DevelopmentProfileTest {
    @Test
    void pomDefinesDevelopmentProfileWithEnvironmentProperty() throws Exception {
        Document document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(Path.of("pom.xml").toFile());

        Element profile = findProfile(document, "development");
        assertNotNull(profile, "Добавь профиль с id development");

        Element properties = childElement(profile, "properties");
        assertNotNull(properties, "Добавь properties в профиль development");
        assertEquals("dev", childValue(properties, "app.environment"));
    }

    private Element findProfile(Document document, String profileId) {
        NodeList profiles = document.getElementsByTagName("profile");
        for (int index = 0; index < profiles.getLength(); index++) {
            Element profile = (Element) profiles.item(index);
            if (profileId.equals(childValue(profile, "id"))) {
                return profile;
            }
        }
        return null;
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
