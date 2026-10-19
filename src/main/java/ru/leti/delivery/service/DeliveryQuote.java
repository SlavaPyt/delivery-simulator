package ru.leti.delivery.service;

import java.math.BigDecimal;
import java.time.Duration;
import ru.leti.delivery.model.DeliveryMethod;

/** Расчёт доставки конкретного заказа выбранным способом: стоимость и ориентировочный срок. */
public record DeliveryQuote(DeliveryMethod method, BigDecimal cost, Duration estimatedTime) {

  public boolean requiresCourier() {
    return method.requiresCourier();
  }
}
