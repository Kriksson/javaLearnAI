package learning.task001;

import java.util.Scanner;

public class Solution {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        String name = scan.nextLine();
        String wClass = scan.nextLine();
        int level = Integer.parseInt(scan.nextLine());
        System.out.printf("Игрок: %s%nКласс: %s%nУровень: %d%nГотов к приключению!", name, wClass, level);
    }
}
