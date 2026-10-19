package ru.leti.delivery.model;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Общий контракт способа доставки.
 *
 * <p>Каждая реализация сама определяет стоимость, срок и необходимость курьера, поэтому код
 * оформления заказа не зависит от конкретного способа и не содержит проверок типа доставки.
 */
public interface DeliveryMethod {

  /** Название способа доставки для показа пользователю. */
  String getTitle();

  /** Стоимость доставки заказа, товары в котором стоят {@code itemsTotal}. */
  BigDecimal calculateCost(BigDecimal itemsTotal);

  /** Ориентировочный срок: через сколько времени заказ будет доставлен или готов к выдаче. */
  Duration estimateTime();

  /** Нужно ли назначать на заказ курьера. */
  boolean requiresCourier();

  /** Статус, в который заказ переходит после подтверждения при передаче в доставку. */
  OrderStatus getDispatchStatus();

  /** Описание места получения заказа; {@code customerAddress} — адрес, указанный клиентом. */
  String describeDestination(String customerAddress);
}
