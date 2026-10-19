package ru.leti.delivery.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import ru.leti.delivery.exception.ValidationException;

class OrderItemTest {

  @Test
  void calculatesTotalAsPriceTimesQuantity() {
    OrderItem item = new OrderItem("Пицца", 3, new BigDecimal("450.50"));

    assertEquals(new BigDecimal("1351.50"), item.getTotal());
  }

  @Test
  void roundsPriceToKopecks() {
    OrderItem item = new OrderItem("Конфета", 1, new BigDecimal("10.005"));

    assertEquals(new BigDecimal("10.01"), item.getUnitPrice());
  }

  @Test
  void trimsProductName() {
    assertEquals("Суши", new OrderItem("  Суши ", 1, BigDecimal.TEN).getProductName());
  }

  @Test
  void rejectsBlankProductName() {
    assertThrows(ValidationException.class, () -> new OrderItem(" ", 1, BigDecimal.TEN));
    assertThrows(ValidationException.class, () -> new OrderItem(null, 1, BigDecimal.TEN));
  }

  @Test
  void rejectsNonPositiveQuantity() {
    assertThrows(ValidationException.class, () -> new OrderItem("Суши", 0, BigDecimal.TEN));
    assertThrows(ValidationException.class, () -> new OrderItem("Суши", -2, BigDecimal.TEN));
  }

  @Test
  void rejectsMissingOrNonPositivePrice() {
    assertThrows(ValidationException.class, () -> new OrderItem("Суши", 1, null));
    assertThrows(ValidationException.class, () -> new OrderItem("Суши", 1, BigDecimal.ZERO));
    assertThrows(ValidationException.class, () -> new OrderItem("Суши", 1, new BigDecimal("-5")));
  }
}
