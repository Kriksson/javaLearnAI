# Задание 29 — Корзина покупок и `BigDecimal`

**Сложность:** мини-проект на Java Collections

## Закрепляем

- `record` как ключ `Map`;
- накопление количества товаров;
- неизменяемый снимок коллекции;
- проверку обычных и ошибочных сценариев через unit-тесты.

## Новое понятие: `BigDecimal`

Для денег не используют `double`: дробные числа в двоичной системе могут
храниться с погрешностью. `BigDecimal` хранит десятичное значение точно.

Для сравнения `BigDecimal` используй `compareTo`:

```java
price.compareTo(BigDecimal.ZERO) <= 0
```

означает «цена нулевая или отрицательная».

## Задача

Реализуй корзину игровых покупок.

### `Product`

Создай record:

```java
public record Product(String sku, BigDecimal price) { }
```

Правила:

- `sku` не `null`, после `trim()` не пустой и сохраняется без пробелов по краям;
- `price` не `null` и строго больше `BigDecimal.ZERO`;
- при нарушении — `IllegalArgumentException`.

### `ShoppingCart`

Реализуй методы:

```java
public void add(Product product, int quantity)
public boolean removeOne(Product product)
public int quantityOf(Product product)
public int distinctProducts()
public BigDecimal total()
public Map<Product, Integer> items()
```

Правила:

- `add` увеличивает количество товара; `product == null` или `quantity <= 0`
  приводят к `IllegalArgumentException`;
- `removeOne` уменьшает количество на один. Если товара нет, возвращает `false`.
  Если после уменьшения количество стало нулём, удали товар из корзины;
- `quantityOf` для отсутствующего товара возвращает `0`, но `null` недопустим;
- `total` — сумма `price × quantity` для всех товаров. Для пустой корзины —
  `BigDecimal.ZERO`;
- `items()` возвращает неизменяемый снимок, а не внутреннюю `Map`.

## Пример

```text
add(potion за 1.25, 2)
add(sword за 10.00, 1)
total() = 12.50
removeOne(potion)
total() = 11.25
```

## Файлы

- `src/main/java/learning/task029/Product.java`;
- `src/main/java/learning/task029/ShoppingCart.java`;
- `src/test/java/learning/task029/ShoppingCartTest.java`.

Запуск: `bash ./mvnw test`.
