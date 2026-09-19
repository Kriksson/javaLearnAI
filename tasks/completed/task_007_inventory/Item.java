package learning.task007;

import java.util.Objects;

public class Item {
    private final String name;
    private final ItemType type;
    private final int price;

    public Item(String name, ItemType type, int price) {
        name = name.trim();
        if (name.isEmpty() || type == null || price < 0) {
            throw new IllegalArgumentException("Некорректные данные предмета");
        }
        this.name = name;
        this.type = type;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public ItemType getType() {
        return type;
    }

    public int getPrice() {
        return price;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Item item)) return false;
        return price == item.price && name.equals(item.name) && type == item.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, price);
    }
}
