package learning.task019;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class RewardCalculatorTest {
    @Test
    void calculatesRewardForPartialProgress() {
        assertEquals(25, RewardCalculator.calculate(100, 1, 4, false));
        assertEquals(66, RewardCalculator.calculate(100, 2, 3, false));
        assertEquals(0, RewardCalculator.calculate(100, 0, 4, false));
    }

    @Test
    void addsPremiumBonusOnlyForCompletedQuest() {
        assertEquals(120, RewardCalculator.calculate(100, 3, 3, true));
        assertEquals(121, RewardCalculator.calculate(101, 3, 3, true));
        assertEquals(25, RewardCalculator.calculate(100, 1, 4, true));
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> RewardCalculator.calculate(-1, 1, 1, false));
        assertThrows(IllegalArgumentException.class, () -> RewardCalculator.calculate(100, -1, 1, false));
        assertThrows(IllegalArgumentException.class, () -> RewardCalculator.calculate(100, 1, 0, false));
        assertThrows(IllegalArgumentException.class, () -> RewardCalculator.calculate(100, 2, 1, false));
    }

    @Test
    void usesUtilityClassShape() throws NoSuchMethodException {
        assertTrue(Modifier.isFinal(RewardCalculator.class.getModifiers()));
        Constructor<RewardCalculator> constructor = RewardCalculator.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    }
}
