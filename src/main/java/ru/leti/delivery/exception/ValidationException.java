package ru.leti.delivery.exception;

/** Переданы некорректные данные: пустое имя, неверный телефон, отрицательная цена и т. п. */
public class ValidationException extends DeliveryException {

  public ValidationException(String message) {
    super(message);
  }
}
