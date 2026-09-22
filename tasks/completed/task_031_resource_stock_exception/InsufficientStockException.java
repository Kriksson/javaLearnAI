package learning.task031;

public class InsufficientStockException extends Exception {
    public InsufficientStockException(String resourceName, int requested, int available) {
        super(String.format("Было запрошено %s x%d, доступно x%d", resourceName, requested, available));
    }
}
