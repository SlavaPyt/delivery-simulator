package ru.leti.delivery.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Статус заказа и допустимые переходы между статусами.
 *
 * <p>Основной путь: {@code NEW → CONFIRMED → (IN_DELIVERY | READY_FOR_PICKUP) → COMPLETED}.
 * Отменить заказ можно до его завершения. Статусы {@code COMPLETED} и {@code CANCELLED} конечные.
 */
public enum OrderStatus {
  NEW("Новый"),
  CONFIRMED("Подтверждён"),
  IN_DELIVERY("В доставке"),
  READY_FOR_PICKUP("Готов к выдаче"),
  COMPLETED("Выполнен"),
  CANCELLED("Отменён");

  private final String title;

  OrderStatus(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  /** Статусы, в которые заказ можно перевести из текущего. */
  public Set<OrderStatus> getAllowedNext() {
    return switch (this) {
      case NEW -> EnumSet.of(CONFIRMED, CANCELLED);
      case CONFIRMED -> EnumSet.of(IN_DELIVERY, READY_FOR_PICKUP, CANCELLED);
      case IN_DELIVERY, READY_FOR_PICKUP -> EnumSet.of(COMPLETED, CANCELLED);
      case COMPLETED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
    };
  }

  public boolean canTransitionTo(OrderStatus next) {
    return getAllowedNext().contains(next);
  }

  /** Конечный статус: заказ завершён или отменён и больше не меняется. */
  public boolean isFinal() {
    return getAllowedNext().isEmpty();
  }
}
