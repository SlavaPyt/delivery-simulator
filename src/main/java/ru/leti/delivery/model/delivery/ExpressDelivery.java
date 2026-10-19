package ru.leti.delivery.model.delivery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/** Экспресс-доставка курьером: дороже стандартной, зато быстрее. */
public class ExpressDelivery extends CourierDelivery {

  static final BigDecimal BASE_COST = new BigDecimal("500.00");
  static final BigDecimal RATE = new BigDecimal("0.05");
  private static final Duration ESTIMATED_TIME = Duration.ofHours(2);

  @Override
  public String getTitle() {
    return "Экспресс-доставка";
  }

  /** Базовая цена плюс 5% от стоимости товаров. */
  @Override
  public BigDecimal calculateCost(BigDecimal itemsTotal) {
    return BASE_COST.add(itemsTotal.multiply(RATE)).setScale(2, RoundingMode.HALF_UP);
  }

  @Override
  public Duration estimateTime() {
    return ESTIMATED_TIME;
  }
}
