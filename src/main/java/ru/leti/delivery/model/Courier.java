package ru.leti.delivery.model;

import ru.leti.delivery.exception.InvalidOperationException;

/**
 * Курьер службы доставки.
 *
 * <p>Статус «занят» курьер получает только при назначении на заказ и теряет его, когда заказ
 * завершён или отменён. Остальные статусы курьер может менять сам через {@link
 * #setStatus(CourierStatus)}.
 */
public class Courier extends Person {

  public enum CourierStatus {
    BUSY("Занят"),
    ON_BREAK("На перерыве"),
    OFFLINE("Не на смене"),
    AVAILABLE("Свободен");

    private final String title;

    CourierStatus(String title) {
      this.title = title;
    }

    public String getTitle() {
      return title;
    }
  }

  private CourierStatus status;

  public Courier(String name, String phone) {
    super(name, phone);
    this.status = CourierStatus.AVAILABLE;
  }

  public CourierStatus getStatus() {
    return status;
  }

  public boolean isAvailable() {
    return status == CourierStatus.AVAILABLE;
  }

  /** Меняет статус вручную (перерыв, конец смены и т. п.), если курьер не занят заказом. */
  public void setStatus(CourierStatus newStatus) {
    if (newStatus == null) {
      throw new InvalidOperationException("Статус курьера не задан");
    }
    if (newStatus == status) {
      return;
    }
    if (status == CourierStatus.BUSY) {
      throw new InvalidOperationException(
          "Курьер " + getName() + " выполняет заказ: статус можно изменить после его завершения");
    }
    if (newStatus == CourierStatus.BUSY) {
      throw new InvalidOperationException(
          "Статус «Занят» устанавливается автоматически при назначении курьера на заказ");
    }
    this.status = newStatus;
  }

  /** Назначает курьера на заказ: доступно только свободному курьеру. */
  void occupy() {
    if (!isAvailable()) {
      throw new InvalidOperationException(
          "Курьер " + getName() + " не может принять заказ: статус «" + status.getTitle() + "»");
    }
    this.status = CourierStatus.BUSY;
  }

  /** Освобождает курьера после завершения или отмены заказа. */
  void release() {
    if (status == CourierStatus.BUSY) {
      this.status = CourierStatus.AVAILABLE;
    }
  }

  @Override
  public String toString() {
    return getName() + ", тел. " + getPhone() + " (" + status.getTitle() + ")";
  }
}
