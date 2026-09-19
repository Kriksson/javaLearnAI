package learning.task002;

public class Solution {
    public static String createPlayerTag(String nickname, String characterClass) {
        nickname = nickname.trim();
        characterClass = characterClass.toUpperCase().trim();
        if (!nickname.isEmpty() && !characterClass.isEmpty()) {
            return String.format("[%s] %s", characterClass, nickname);
        }
        return "Некорректные данные";
    }
}
