package ru.leti.delivery.console;

/** Поток ввода закрыт (например, нажато Ctrl+D), читать команды больше не из чего. */
public class InputClosedException extends RuntimeException {

  public InputClosedException() {
    super("Ввод завершён");
  }
}
