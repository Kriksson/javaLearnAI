package learning.task018;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class NicknameValidatorTest {
    @ParameterizedTest
    @ValueSource(strings = {"mage", "ss19", "DMitrs2"})
    void isValidNickname(String nickname) {
        Assertions.assertTrue(NicknameValidator.isValid(nickname));
    }
    @ParameterizedTest
    @ValueSource(strings = {"1ss", "smf s", "sk2-s", "  ab ", "Héro"})
    void isInvalidNickname(String nickname) {
        Assertions.assertFalse(NicknameValidator.isValid(nickname));
    }

    @ParameterizedTest
    @NullSource
    void isValidNicknameWithNull(String nickname) {
        Assertions.assertFalse(NicknameValidator.isValid(nickname));
    }

}
