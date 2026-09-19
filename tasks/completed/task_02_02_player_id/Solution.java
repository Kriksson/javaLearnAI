package learning.topic02.task02;

public class Solution {
    public static String createGameCode(String gameTitle) {
        gameTitle = gameTitle.toUpperCase().trim();
        if (gameTitle.isEmpty()) {
            return "";
        }
        if (gameTitle.length() <= 3){
            return gameTitle;
        } else {
            return gameTitle.substring(0,3);
        }
    }

    public static String createPlayerId(String nickname, String gameTitle) {
        gameTitle = gameTitle.toUpperCase().trim();
        nickname = nickname.trim().replaceAll(" ","_").toUpperCase();
        if (gameTitle.isEmpty() || nickname.isEmpty()) {
            return "Некорректные данные";
        }
        return String.format("%s-%s", createGameCode(gameTitle), nickname);
    }
}
