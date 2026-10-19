package ru.leti.delivery.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Позиция заказа: товар, его количество и цена за единицу. Объект неизменяемый. */
public final class OrderItem {

  private final String productName;
  private final int quantity;
  private final BigDecimal unitPrice;

  public OrderItem(String productName, int quantity, BigDecimal unitPrice) {
    this.productName = Validation.requireText(productName, "Название товара");
    this.quantity = Validation.requirePositive(quantity, "Количество");
    this.unitPrice =
        Validation.requirePositiveMoney(unitPrice, "Цена").setScale(2, RoundingMode.HALF_UP);
  }

  public String getProductName() {
    return productName;
  }

  public int getQuantity() {
    return quantity;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  /** Стоимость позиции: цена за единицу, умноженная на количество. */
  public BigDecimal getTotal() {
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
  }

  @Override
  public String toString() {
    return productName + " × " + quantity + " по " + unitPrice + " руб. = " + getTotal() + " руб.";
  }
}
