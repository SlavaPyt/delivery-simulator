package ru.leti.delivery.exception;

/** Запрошенный объект (например, заказ с указанным номером) не найден. */
public class NotFoundException extends DeliveryException {

  public NotFoundException(String message) {
    super(message);
  }
}
