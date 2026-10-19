package ru.leti.delivery.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import ru.leti.delivery.exception.InvalidOperationException;
import ru.leti.delivery.exception.ValidationException;

/**
 * Заказ службы доставки.
 *
 * <p>Заказ состоит из клиента, позиций и выбранного способа доставки (композиция). Его состояние
 * изменяется только через методы, которые проверяют допустимость операции: добавление позиций,
 * выбор способа доставки, назначение курьера и смена статуса.
 */
public class Order {

  private final int id;
  private final Customer customer;
  private final String address;
  private final List<OrderItem> items = new ArrayList<>();
  private DeliveryMethod deliveryMethod;
  private Courier courier;
  private OrderStatus status = OrderStatus.NEW;

  public Order(int id, Customer customer, String address) {
    this.id = Validation.requirePositive(id, "Номер заказа");
    if (customer == null) {
      throw new ValidationException("Клиент не указан");
    }
    this.customer = customer;
    this.address = Validation.requireText(address, "Адрес доставки");
  }

  public int getId() {
    return id;
  }

  public Customer getCustomer() {
    return customer;
  }

  public String getAddress() {
    return address;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public List<OrderItem> getItems() {
    return Collections.unmodifiableList(items);
  }

  public Optional<DeliveryMethod> getDeliveryMethod() {
    return Optional.ofNullable(deliveryMethod);
  }

  public Optional<Courier> getCourier() {
    return Optional.ofNullable(courier);
  }

  /** Стоимость всех позиций без учёта доставки. */
  public BigDecimal getItemsTotal() {
    return items.stream().map(OrderItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Стоимость доставки; пока способ не выбран, равна нулю. */
  public BigDecimal getDeliveryCost() {
    return deliveryMethod == null
        ? BigDecimal.ZERO.setScale(2)
        : deliveryMethod.calculateCost(getItemsTotal());
  }

  /** Итоговая стоимость: позиции плюс доставка. */
  public BigDecimal getTotalCost() {
    return getItemsTotal().add(getDeliveryCost());
  }

  /** Ориентировочный срок доставки по выбранному способу. */
  public Optional<Duration> getEstimatedTime() {
    return getDeliveryMethod().map(DeliveryMethod::estimateTime);
  }

  /** Место получения заказа: адрес клиента или пункт выдачи, если выбран самовывоз. */
  public String getDestination() {
    return deliveryMethod == null ? address : deliveryMethod.describeDestination(address);
  }

  /**
   * Статусы, в которые заказ можно перевести из текущего с учётом способа доставки: например, при
   * самовывозе нет статуса «В доставке».
   */
  public List<OrderStatus> getAvailableTransitions() {
    return status.getAllowedNext().stream()
        .filter(next -> !next.isDispatch() || next == deliveryMethod.getDispatchStatus())
        .toList();
  }

  /** Добавляет позицию; состав заказа можно менять, пока он не подтверждён. */
  public void addItem(OrderItem item) {
    if (item == null) {
      throw new ValidationException("Позиция заказа не указана");
    }
    if (status != OrderStatus.NEW) {
      throw new InvalidOperationException(
          "Добавлять позиции можно только в новый заказ, а заказ №"
              + id
              + " имеет статус «"
              + status.getTitle()
              + "»");
    }
    items.add(item);
  }

  /**
   * Выбирает способ доставки до передачи заказа в доставку. Если новый способ не требует курьера,
   * ранее назначенный курьер освобождается.
   */
  public void chooseDeliveryMethod(DeliveryMethod method) {
    if (method == null) {
      throw new ValidationException("Способ доставки не указан");
    }
    if (status != OrderStatus.NEW && status != OrderStatus.CONFIRMED) {
      throw new InvalidOperationException(
          "Способ доставки нельзя изменить: заказ имеет статус «" + status.getTitle() + "»");
    }
    this.deliveryMethod = method;
    if (!method.requiresCourier()) {
      releaseCourier();
      this.courier = null;
    }
  }

  /**
   * Назначает курьера. Возможно только для способа доставки, который требует курьера, и до передачи
   * заказа в доставку. Ранее назначенный курьер при замене освобождается.
   */
  public void assignCourier(Courier newCourier) {
    if (newCourier == null) {
      throw new ValidationException("Курьер не указан");
    }
    if (deliveryMethod == null) {
      throw new InvalidOperationException("Сначала выберите способ доставки");
    }
    if (!deliveryMethod.requiresCourier()) {
      throw new InvalidOperationException(
          "Для способа «" + deliveryMethod.getTitle() + "» курьер не требуется");
    }
    if (status != OrderStatus.NEW && status != OrderStatus.CONFIRMED) {
      throw new InvalidOperationException(
          "Курьера нельзя назначить: заказ имеет статус «" + status.getTitle() + "»");
    }
    if (newCourier == courier) {
      return;
    }
    newCourier.occupy();
    releaseCourier();
    this.courier = newCourier;
  }

  /** Переводит заказ в новый статус, если переход допустим и выполнены его условия. */
  public void changeStatus(OrderStatus next) {
    if (next == null) {
      throw new ValidationException("Новый статус не указан");
    }
    if (!status.canTransitionTo(next)) {
      throw new InvalidOperationException(describeForbiddenTransition(next));
    }
    checkPreconditions(next);
    this.status = next;
    if (next.isFinal()) {
      releaseCourier();
    }
  }

  private String describeForbiddenTransition(OrderStatus next) {
    if (status.isFinal()) {
      return "Заказ №" + id + " уже «" + status.getTitle() + "»: его статус больше не меняется";
    }
    String allowed =
        status.getAllowedNext().stream()
            .map(OrderStatus::getTitle)
            .collect(Collectors.joining(", "));
    return "Недопустимый переход статуса: «"
        + status.getTitle()
        + "» → «"
        + next.getTitle()
        + "». Допустимо: "
        + allowed;
  }

  private void checkPreconditions(OrderStatus next) {
    switch (next) {
      case CONFIRMED -> {
        if (items.isEmpty()) {
          throw new InvalidOperationException("Нельзя подтвердить заказ без позиций");
        }
        if (deliveryMethod == null) {
          throw new InvalidOperationException(
              "Нельзя подтвердить заказ: не выбран способ доставки");
        }
      }
      case IN_DELIVERY, READY_FOR_PICKUP -> {
        if (next != deliveryMethod.getDispatchStatus()) {
          throw new InvalidOperationException(
              "Для способа «"
                  + deliveryMethod.getTitle()
                  + "» нельзя установить статус «"
                  + next.getTitle()
                  + "»: после подтверждения заказ получает статус «"
                  + deliveryMethod.getDispatchStatus().getTitle()
                  + "»");
        }
        if (deliveryMethod.requiresCourier() && courier == null) {
          throw new InvalidOperationException(
              "Нельзя передать заказ в доставку: не назначен курьер");
        }
      }
      default -> {}
    }
  }

  private void releaseCourier() {
    if (courier != null) {
      courier.release();
    }
  }

  @Override
  public String toString() {
    return "Заказ №" + id + " (" + status.getTitle() + ")";
  }
}
