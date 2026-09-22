package learning.task028;

public record LootDrop(String itemName, int quantity) {
    public LootDrop(String itemName, int quantity) {
        if (itemName == null) throw new IllegalArgumentException();
        itemName = itemName.trim();
        if (itemName.isEmpty()) throw new IllegalArgumentException();
        if (quantity < 1) throw new IllegalArgumentException();
        this.itemName = itemName;
        this.quantity = quantity;
    }
}
