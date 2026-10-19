package ru.leti.delivery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.leti.delivery.exception.InvalidOperationException;
import ru.leti.delivery.exception.NotFoundException;
import ru.leti.delivery.exception.ValidationException;
import ru.leti.delivery.model.Courier;
import ru.leti.delivery.model.Courier.CourierStatus;
import ru.leti.delivery.model.DeliveryMethod;
import ru.leti.delivery.model.Order;
import ru.leti.delivery.model.OrderItem;
import ru.leti.delivery.model.OrderStatus;
import ru.leti.delivery.model.delivery.ExpressDelivery;
import ru.leti.delivery.model.delivery.PickupDelivery;
import ru.leti.delivery.model.delivery.StandardDelivery;

class OrderServiceTest {

  private final DeliveryMethod standard = new StandardDelivery();
  private final DeliveryMethod express = new ExpressDelivery();
  private final DeliveryMethod pickup = new PickupDelivery();

  private OrderService service;
  private Courier courier;

  @BeforeEach
  void setUp() {
    service = new OrderService(List.of(standard, express, pickup));
    courier = new CourierService().addCourier("Пётр Курьеров", "+7 (911) 000-11-22");
  }

  private Order createOrder() {
    return service.createOrder(
        "Анна Смирнова",
        "+7 (999) 123-45-67",
        "ул. Ленина, 1",
        List.of(new OrderItem("Пицца", 2, new BigDecimal("500.00"))));
  }

  @Test
  void assignsSequentialOrderNumbers() {
    Order first = createOrder();
    Order second = createOrder();

    assertEquals(1, first.getId());
    assertEquals(2, second.getId());
    assertEquals(List.of(first, second), service.getAllOrders());
  }

  @Test
  void orderCannotBeCreatedWithoutItems() {
    assertThrows(
        ValidationException.class,
        () -> service.createOrder("Анна", "89991234567", "ул. Ленина, 1", List.of()));
    assertTrue(service.getAllOrders().isEmpty());
  }

  @Test
  void invalidCustomerDataDoesNotConsumeOrderNumberOrCreateOrder() {
    List<OrderItem> items = List.of(new OrderItem("Пицца", 1, BigDecimal.TEN));

    assertThrows(
        ValidationException.class,
        () -> service.createOrder("Анна", "не телефон", "ул. Ленина, 1", items));

    assertTrue(service.getAllOrders().isEmpty());
    assertEquals(1, createOrder().getId());
  }

  @Test
  void findsOrderById() {
    Order order = createOrder();

    assertSame(order, service.getOrder(order.getId()));
    assertThrows(NotFoundException.class, () -> service.getOrder(99));
  }

  @Test
  void addsItemToExistingOrder() {
    Order order = createOrder();

    service.addItem(order.getId(), new OrderItem("Сок", 1, new BigDecimal("100.00")));

    assertEquals(2, order.getItems().size());
    assertEquals(new BigDecimal("1100.00"), order.getItemsTotal());
  }

  @Test
  void quotesAllDeliveryMethodsForOrder() {
    Order order = createOrder();

    List<DeliveryQuote> quotes = service.quoteDelivery(order.getId());

    assertEquals(3, quotes.size());
    assertEquals(new BigDecimal("250.00"), quotes.get(0).cost());
    assertEquals(Duration.ofHours(48), quotes.get(0).estimatedTime());
    assertEquals(new BigDecimal("550.00"), quotes.get(1).cost());
    assertEquals(new BigDecimal("0.00"), quotes.get(2).cost());
    assertTrue(quotes.get(0).requiresCourier());
    assertFalse(quotes.get(2).requiresCourier());
  }

  @Test
  void choosingDeliveryMethodReturnsQuoteAndStoresMethod() {
    Order order = createOrder();

    DeliveryQuote quote = service.chooseDeliveryMethod(order.getId(), express);

    assertEquals(new BigDecimal("550.00"), quote.cost());
    assertSame(express, order.getDeliveryMethod().orElseThrow());
    assertEquals(new BigDecimal("1550.00"), order.getTotalCost());
  }

  @Test
  void fullCourierScenario() {
    Order order = createOrder();
    service.chooseDeliveryMethod(order.getId(), standard);
    service.changeStatus(order.getId(), OrderStatus.CONFIRMED);
    service.assignCourier(order.getId(), courier);
    service.changeStatus(order.getId(), OrderStatus.IN_DELIVERY);
    service.changeStatus(order.getId(), OrderStatus.COMPLETED);

    assertEquals(OrderStatus.COMPLETED, order.getStatus());
    assertTrue(courier.isAvailable());
  }

  @Test
  void businessRulesAreEnforcedThroughService() {
    Order order = createOrder();

    assertThrows(
        InvalidOperationException.class,
        () -> service.changeStatus(order.getId(), OrderStatus.CONFIRMED));
    assertThrows(
        InvalidOperationException.class, () -> service.assignCourier(order.getId(), courier));
    assertEquals(CourierStatus.AVAILABLE, courier.getStatus());
  }

  @Test
  void operationsOnUnknownOrderFail() {
    assertThrows(NotFoundException.class, () -> service.quoteDelivery(5));
    assertThrows(NotFoundException.class, () -> service.chooseDeliveryMethod(5, standard));
    assertThrows(NotFoundException.class, () -> service.assignCourier(5, courier));
    assertThrows(NotFoundException.class, () -> service.changeStatus(5, OrderStatus.CONFIRMED));
  }

  @Test
  void requiresAtLeastOneDeliveryMethod() {
    assertThrows(ValidationException.class, () -> new OrderService(List.of()));
    assertThrows(ValidationException.class, () -> new OrderService(null));
  }

  @Test
  void returnedOrderListIsReadOnly() {
    createOrder();

    assertThrows(UnsupportedOperationException.class, () -> service.getAllOrders().clear());
  }
}
