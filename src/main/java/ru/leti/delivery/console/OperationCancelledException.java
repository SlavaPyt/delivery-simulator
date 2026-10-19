package ru.leti.delivery.console;

/** Пользователь отменил текущую операцию, введя команду отмены. */
public class OperationCancelledException extends RuntimeException {

  public OperationCancelledException() {
    super("Операция отменена");
  }
}
