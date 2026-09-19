package learning.topic02.task03;

public class Solution {
    public static String normalizeToken(String value) {
        return value.trim().toUpperCase().replace(" ", "_");
    }

    public static String createCharacterCard(String data) {

        int first = data.indexOf('|');
        int second = data.lastIndexOf('|');

        boolean exactlyTwo = first >= 0
                && first != second
                && data.indexOf('|', first + 1) == second;

        if (first != second &&  exactlyTwo) {
            String name = normalizeToken(data.substring(0, first));
            String card = normalizeToken(data.substring(first + 1, second));
            String level = data.substring(second + 1, data.length()).trim();
            if (name.isEmpty() || level.isEmpty() || card.isEmpty()) return "Некорректные данные";
            return String.format("[%s] %s (уровень %s)",
                    card, name, level);
        } else {
            return "Некорректные данные";
        }
    }
}
