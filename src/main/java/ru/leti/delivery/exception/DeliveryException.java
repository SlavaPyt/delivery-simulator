package ru.leti.delivery.exception;

/** Базовое исключение приложения. Сообщение исключения предназначено для показа пользователю. */
public class DeliveryException extends RuntimeException {

  public DeliveryException(String message) {
    super(message);
  }
}
