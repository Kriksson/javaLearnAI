package learning.task018;

public final class NicknameValidator {
    private NicknameValidator() {
        // TODO: запрети создание объектов утилитного класса
    }
//
//    public static boolean isValid(String nickname) {
//        if (nickname == null) return false;
//        nickname = nickname.trim();
//        if (nickname.length() < 3 ||  nickname.length() > 12) return false;
//        return nickname.matches("^[A-Za-z][A-Za-z0-9_]*");
//    }

    public static boolean isValid(String nickname) {
        if (nickname == null) return false;
        nickname = nickname.trim();
        if (nickname.length() < 3 ||  nickname.length() > 12) return false;
        char[] chars = nickname.toCharArray();
        if (!(chars[0] >= 'a' && chars[0] <= 'z') && !(chars[0] >= 'A' && chars[0] <= 'Z')) return false;
        for (int i = 1; i < chars.length; i++) {
            if (chars[i] != '_' && !(chars[i] >= 'a' && chars[i] <= 'z') && !(chars[i] >= 'A' && chars[i] <= 'Z')
                    && !(chars[i] >= '0' && chars[i] <= '9')) return false;
        }
        return true;
    }
}
