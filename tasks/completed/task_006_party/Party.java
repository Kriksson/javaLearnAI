package learning.task006;

import java.util.ArrayList;
import java.util.List;

public class Party {

    private final String name;
    private final int maxSize;

    private final List<GameCharacter> characters = new ArrayList<>();

    public Party(String name, int maxSize) {
        name = name.trim();
        if (name.isEmpty()) throw new IllegalArgumentException();
        if (maxSize <= 0) throw new IllegalArgumentException();
        this.name = name;
        this.maxSize = maxSize;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return characters.size();
    }

    public boolean addMember(GameCharacter character) {
        if (characters.size() < maxSize) {
            boolean alreadyAdded = false;
            for (GameCharacter gc : characters) {
                if (gc.getNickname().equalsIgnoreCase(character.getNickname())) {
                    alreadyAdded = true;
                }
            }
            if (!alreadyAdded) {
                characters.add(character);
                return true;
            }
        }
        return false;
    }

    public GameCharacter findMember(String nickname) {
        for (GameCharacter character : characters) {
            if (character.getNickname().toLowerCase().trim().equals(nickname.toLowerCase().trim())) {
                return character;
            }
        }
        return null;
    }

    public GameCharacter getStrongestMember() {
        int maxLevel =  0;
        for (GameCharacter character : characters) {
            if (character.getLevel() > maxLevel) {
                maxLevel = character.getLevel();
            }
        }
        for (GameCharacter character : characters) {
            if (character.getLevel() == maxLevel) {
                return character;
            }
        }
        return null;
    }

    public double getAverageLevel() {
        int sumLevel = 0;
        if (characters.size() <= 0) return (double) 0;
        for (GameCharacter character : characters) {
            sumLevel += character.getLevel();
        }
        return (double) sumLevel / (double) characters.size();
    }
}
