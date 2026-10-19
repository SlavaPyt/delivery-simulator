package ru.leti.delivery.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.leti.delivery.exception.NotFoundException;
import ru.leti.delivery.exception.ValidationException;
import ru.leti.delivery.model.Courier;
import ru.leti.delivery.model.Customer;
import ru.leti.delivery.model.DeliveryMethod;
import ru.leti.delivery.model.Order;
import ru.leti.delivery.model.OrderItem;
import ru.leti.delivery.model.OrderStatus;

/**
 * Управление заказами: создание, поиск и операции над ними.
 *
 * <p>Сервис не знает о конкретных способах доставки: список доступных способов передаётся при
 * создании, поэтому новый способ добавляется без изменения этого класса. Бизнес-правила операций
 * проверяет сам {@link Order}.
 */
public class OrderService {

  private final Map<Integer, Order> orders = new LinkedHashMap<>();
  private final List<DeliveryMethod> deliveryMethods;
  private int nextOrderId = 1;

  public OrderService(List<DeliveryMethod> deliveryMethods) {
    if (deliveryMethods == null || deliveryMethods.isEmpty()) {
      throw new ValidationException("Должен быть доступен хотя бы один способ доставки");
    }
    this.deliveryMethods = List.copyOf(deliveryMethods);
  }

  /** Создаёт заказ с присвоением следующего номера. Заказ должен содержать хотя бы одну позицию. */
  public Order createOrder(
      String customerName, String customerPhone, String address, List<OrderItem> items) {
    if (items == null || items.isEmpty()) {
      throw new ValidationException("Заказ должен содержать хотя бы одну позицию");
    }
    Order order = new Order(nextOrderId, new Customer(customerName, customerPhone), address);
    items.forEach(order::addItem);
    orders.put(order.getId(), order);
    nextOrderId++;
    return order;
  }

  public Order getOrder(int orderId) {
    Order order = orders.get(orderId);
    if (order == null) {
      throw new NotFoundException("Заказ №" + orderId + " не найден");
    }
    return order;
  }

  public List<Order> getAllOrders() {
    return Collections.unmodifiableList(new ArrayList<>(orders.values()));
  }

  public List<DeliveryMethod> getDeliveryMethods() {
    return deliveryMethods;
  }

  public void addItem(int orderId, OrderItem item) {
    getOrder(orderId).addItem(item);
  }

  /** Рассчитывает стоимость и срок доставки заказа каждым из доступных способов. */
  public List<DeliveryQuote> quoteDelivery(int orderId) {
    Order order = getOrder(orderId);
    return deliveryMethods.stream().map(method -> quote(order, method)).toList();
  }

  /** Назначает заказу способ доставки и возвращает рассчитанные стоимость и срок. */
  public DeliveryQuote chooseDeliveryMethod(int orderId, DeliveryMethod method) {
    Order order = getOrder(orderId);
    order.chooseDeliveryMethod(method);
    return quote(order, method);
  }

  public void assignCourier(int orderId, Courier courier) {
    getOrder(orderId).assignCourier(courier);
  }

  public void changeStatus(int orderId, OrderStatus status) {
    getOrder(orderId).changeStatus(status);
  }

  private static DeliveryQuote quote(Order order, DeliveryMethod method) {
    return new DeliveryQuote(
        method, method.calculateCost(order.getItemsTotal()), method.estimateTime());
  }
}
