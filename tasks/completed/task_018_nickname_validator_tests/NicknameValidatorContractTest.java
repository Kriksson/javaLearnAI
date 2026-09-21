package learning.task018;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class NicknameValidatorContractTest {
    @Test
    void acceptsValidNicknamesAndTrimsSpaces() {
        assertTrue(NicknameValidator.isValid("Hero"));
        assertTrue(NicknameValidator.isValid("mage_7"));
        assertTrue(NicknameValidator.isValid("A12"));
        assertTrue(NicknameValidator.isValid("  Hero_42  "));
        assertTrue(NicknameValidator.isValid("Abcdefghijkl"));
    }

    @Test
    void rejectsNullInvalidCharactersAndWrongLength() {
        assertFalse(NicknameValidator.isValid(null));
        assertFalse(NicknameValidator.isValid("  "));
        assertFalse(NicknameValidator.isValid("ab"));
        assertFalse(NicknameValidator.isValid("Abcdefghijklm"));
        assertFalse(NicknameValidator.isValid("42Hero"));
        assertFalse(NicknameValidator.isValid("Hero-name"));
        assertFalse(NicknameValidator.isValid("Hero!"));
        assertFalse(NicknameValidator.isValid("Héro"));
    }

    @Test
    void classAndMethodHaveUtilityShape() throws NoSuchMethodException {
        assertTrue(Modifier.isFinal(NicknameValidator.class.getModifiers()));
        assertTrue(Modifier.isStatic(NicknameValidator.class
                .getMethod("isValid", String.class).getModifiers()));

        Constructor<NicknameValidator> constructor = NicknameValidator.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    }
}
