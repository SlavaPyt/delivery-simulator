package ru.leti.delivery.model.delivery;

import java.math.BigDecimal;
import java.time.Duration;

/** Стандартная доставка курьером: фиксированная цена, бесплатно для крупных заказов. */
public class StandardDelivery extends CourierDelivery {

  static final BigDecimal BASE_COST = new BigDecimal("250.00");
  static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("3000.00");
  private static final Duration ESTIMATED_TIME = Duration.ofHours(48);

  @Override
  public String getTitle() {
    return "Стандартная доставка курьером";
  }

  @Override
  public BigDecimal calculateCost(BigDecimal itemsTotal) {
    if (itemsTotal.compareTo(FREE_DELIVERY_THRESHOLD) >= 0) {
      return BigDecimal.ZERO.setScale(2);
    }
    return BASE_COST;
  }

  @Override
  public Duration estimateTime() {
    return ESTIMATED_TIME;
  }
}
