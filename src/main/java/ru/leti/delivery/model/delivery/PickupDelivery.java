package ru.leti.delivery.model.delivery;

import java.math.BigDecimal;
import java.time.Duration;
import ru.leti.delivery.model.DeliveryMethod;
import ru.leti.delivery.model.OrderStatus;

/** Самовывоз из пункта выдачи: бесплатно, курьер не нужен. */
public class PickupDelivery implements DeliveryMethod {

  static final String DEFAULT_PICKUP_POINT = "Санкт-Петербург, ул. Профессора Попова, д. 5";
  private static final Duration ESTIMATED_TIME = Duration.ofHours(1);

  private final String pickupPoint;

  public PickupDelivery() {
    this(DEFAULT_PICKUP_POINT);
  }

  public PickupDelivery(String pickupPoint) {
    this.pickupPoint = pickupPoint;
  }

  @Override
  public String getTitle() {
    return "Самовывоз";
  }

  @Override
  public BigDecimal calculateCost(BigDecimal itemsTotal) {
    return BigDecimal.ZERO.setScale(2);
  }

  @Override
  public Duration estimateTime() {
    return ESTIMATED_TIME;
  }

  @Override
  public boolean requiresCourier() {
    return false;
  }

  @Override
  public OrderStatus getDispatchStatus() {
    return OrderStatus.READY_FOR_PICKUP;
  }

  /** При самовывозе адрес клиента не используется: заказ забирают из пункта выдачи. */
  @Override
  public String describeDestination(String customerAddress) {
    return "Пункт выдачи: " + pickupPoint;
  }

  @Override
  public String toString() {
    return getTitle();
  }
}
