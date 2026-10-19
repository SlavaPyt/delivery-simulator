package ru.leti.delivery.exception;

/** Операция недопустима в текущем состоянии объекта, например недопустимый переход статуса. */
public class InvalidOperationException extends DeliveryException {

  public InvalidOperationException(String message) {
    super(message);
  }
}
