package ru.leti.delivery.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.leti.delivery.exception.InvalidOperationException;
import ru.leti.delivery.exception.ValidationException;
import ru.leti.delivery.model.Courier.CourierStatus;
import ru.leti.delivery.model.delivery.ExpressDelivery;
import ru.leti.delivery.model.delivery.PickupDelivery;
import ru.leti.delivery.model.delivery.StandardDelivery;

class OrderTest {

  private Order order;
  private Courier courier;

  @BeforeEach
  void setUp() {
    Customer customer = new Customer("Анна Смирнова", "+7 (999) 123-45-67");
    order = new Order(1, customer, "ул. Ленина, 1");
    courier = new Courier("Пётр Курьеров", "+7 (911) 000-11-22");
  }

  private void addItem(String name, int quantity, String price) {
    order.addItem(new OrderItem(name, quantity, new BigDecimal(price)));
  }

  private void prepareConfirmedCourierOrder() {
    addItem("Пицца", 2, "500.00");
    order.chooseDeliveryMethod(new StandardDelivery());
    order.changeStatus(OrderStatus.CONFIRMED);
  }

  @Test
  void newOrderIsEmptyAndHasNewStatus() {
    assertEquals(OrderStatus.NEW, order.getStatus());
    assertTrue(order.getItems().isEmpty());
    assertTrue(order.getDeliveryMethod().isEmpty());
    assertTrue(order.getCourier().isEmpty());
    assertEquals(new BigDecimal("0.00"), order.getDeliveryCost());
  }

  @Test
  void validatesConstructorArguments() {
    Customer customer = new Customer("Иван", "89991234567");

    assertThrows(ValidationException.class, () -> new Order(0, customer, "ул. Ленина, 1"));
    assertThrows(ValidationException.class, () -> new Order(1, null, "ул. Ленина, 1"));
    assertThrows(ValidationException.class, () -> new Order(1, customer, "  "));
  }

  @Test
  void itemsListCannotBeModifiedFromOutside() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> order.getItems().add(new OrderItem("Суши", 1, BigDecimal.TEN)));
  }

  @Test
  void calculatesTotals() {
    addItem("Пицца", 2, "500.00");
    addItem("Сок", 1, "120.50");
    order.chooseDeliveryMethod(new StandardDelivery());

    assertEquals(new BigDecimal("1120.50"), order.getItemsTotal());
    assertEquals(new BigDecimal("250.00"), order.getDeliveryCost());
    assertEquals(new BigDecimal("1370.50"), order.getTotalCost());
    assertEquals(Duration.ofHours(48), order.getEstimatedTime().orElseThrow());
  }

  @Test
  void deliveryCostFollowsItemsTotal() {
    order.chooseDeliveryMethod(new StandardDelivery());
    addItem("Холодильник", 1, "2500.00");
    assertEquals(new BigDecimal("250.00"), order.getDeliveryCost());

    addItem("Чайник", 1, "600.00");
    assertEquals(new BigDecimal("0.00"), order.getDeliveryCost());
  }

  @Test
  void itemsCanBeAddedOnlyToNewOrder() {
    prepareConfirmedCourierOrder();

    assertThrows(InvalidOperationException.class, () -> addItem("Сок", 1, "100.00"));
    assertEquals(1, order.getItems().size());
  }

  @Test
  void deliveryMethodMustBeChosenBeforeConfirmation() {
    addItem("Пицца", 1, "500.00");

    assertThrows(InvalidOperationException.class, () -> order.changeStatus(OrderStatus.CONFIRMED));
    assertEquals(OrderStatus.NEW, order.getStatus());
  }

  @Test
  void emptyOrderCannotBeConfirmed() {
    order.chooseDeliveryMethod(new PickupDelivery());

    assertThrows(InvalidOperationException.class, () -> order.changeStatus(OrderStatus.CONFIRMED));
  }

  @Test
  void courierCannotBeAssignedBeforeDeliveryMethodIsChosen() {
    assertThrows(InvalidOperationException.class, () -> order.assignCourier(courier));
    assertTrue(courier.isAvailable());
  }

  @Test
  void courierIsNotNeededForPickup() {
    order.chooseDeliveryMethod(new PickupDelivery());

    assertThrows(InvalidOperationException.class, () -> order.assignCourier(courier));
    assertTrue(courier.isAvailable());
  }

  @Test
  void assignedCourierBecomesBusy() {
    order.chooseDeliveryMethod(new ExpressDelivery());

    order.assignCourier(courier);

    assertSame(courier, order.getCourier().orElseThrow());
    assertEquals(CourierStatus.BUSY, courier.getStatus());
  }

  @Test
  void unavailableCourierCannotBeAssigned() {
    order.chooseDeliveryMethod(new StandardDelivery());
    courier.setStatus(CourierStatus.ON_BREAK);

    assertThrows(InvalidOperationException.class, () -> order.assignCourier(courier));
    assertTrue(order.getCourier().isEmpty());
  }

  @Test
  void busyCourierCannotTakeSecondOrder() {
    order.chooseDeliveryMethod(new StandardDelivery());
    order.assignCourier(courier);
    Order other = new Order(2, new Customer("Иван", "89991234567"), "ул. Мира, 5");
    other.chooseDeliveryMethod(new StandardDelivery());

    assertThrows(InvalidOperationException.class, () -> other.assignCourier(courier));
  }

  @Test
  void replacingCourierFreesPreviousOne() {
    Courier second = new Courier("Олег Быстров", "+7 (922) 333-44-55");
    order.chooseDeliveryMethod(new StandardDelivery());
    order.assignCourier(courier);

    order.assignCourier(second);

    assertTrue(courier.isAvailable());
    assertEquals(CourierStatus.BUSY, second.getStatus());
    assertSame(second, order.getCourier().orElseThrow());
  }

  @Test
  void failedReplacementKeepsCurrentCourier() {
    Courier second = new Courier("Олег Быстров", "+7 (922) 333-44-55");
    second.setStatus(CourierStatus.OFFLINE);
    order.chooseDeliveryMethod(new StandardDelivery());
    order.assignCourier(courier);

    assertThrows(InvalidOperationException.class, () -> order.assignCourier(second));

    assertSame(courier, order.getCourier().orElseThrow());
    assertEquals(CourierStatus.BUSY, courier.getStatus());
  }

  @Test
  void switchingToPickupReleasesCourier() {
    order.chooseDeliveryMethod(new StandardDelivery());
    order.assignCourier(courier);

    order.chooseDeliveryMethod(new PickupDelivery());

    assertTrue(courier.isAvailable());
    assertTrue(order.getCourier().isEmpty());
  }

  @Test
  void courierDeliveryLifecycle() {
    prepareConfirmedCourierOrder();
    order.assignCourier(courier);

    order.changeStatus(OrderStatus.IN_DELIVERY);
    assertEquals(CourierStatus.BUSY, courier.getStatus());

    order.changeStatus(OrderStatus.COMPLETED);
    assertEquals(OrderStatus.COMPLETED, order.getStatus());
    assertTrue(courier.isAvailable());
  }

  @Test
  void pickupLifecycleDoesNotNeedCourier() {
    addItem("Книга", 1, "800.00");
    order.chooseDeliveryMethod(new PickupDelivery());
    order.changeStatus(OrderStatus.CONFIRMED);

    order.changeStatus(OrderStatus.READY_FOR_PICKUP);
    order.changeStatus(OrderStatus.COMPLETED);

    assertEquals(OrderStatus.COMPLETED, order.getStatus());
    assertTrue(order.getDestination().startsWith("Пункт выдачи"));
  }

  @Test
  void courierOrderCannotGoToDeliveryWithoutCourier() {
    prepareConfirmedCourierOrder();

    assertThrows(
        InvalidOperationException.class, () -> order.changeStatus(OrderStatus.IN_DELIVERY));
    assertEquals(OrderStatus.CONFIRMED, order.getStatus());
  }

  @Test
  void dispatchStatusMustMatchDeliveryMethod() {
    prepareConfirmedCourierOrder();
    order.assignCourier(courier);
    assertThrows(
        InvalidOperationException.class, () -> order.changeStatus(OrderStatus.READY_FOR_PICKUP));

    Order pickupOrder = new Order(2, new Customer("Иван", "89991234567"), "ул. Мира, 5");
    pickupOrder.addItem(new OrderItem("Книга", 1, BigDecimal.TEN));
    pickupOrder.chooseDeliveryMethod(new PickupDelivery());
    pickupOrder.changeStatus(OrderStatus.CONFIRMED);
    assertThrows(
        InvalidOperationException.class, () -> pickupOrder.changeStatus(OrderStatus.IN_DELIVERY));
  }

  @Test
  void rejectsStatusTransitionsThatSkipSteps() {
    assertThrows(
        InvalidOperationException.class, () -> order.changeStatus(OrderStatus.IN_DELIVERY));
    assertThrows(InvalidOperationException.class, () -> order.changeStatus(OrderStatus.COMPLETED));
    assertEquals(OrderStatus.NEW, order.getStatus());
  }

  @Test
  void cancelledOrderReleasesCourierAndStaysFinal() {
    prepareConfirmedCourierOrder();
    order.assignCourier(courier);

    order.changeStatus(OrderStatus.CANCELLED);

    assertEquals(OrderStatus.CANCELLED, order.getStatus());
    assertTrue(courier.isAvailable());
    assertThrows(InvalidOperationException.class, () -> order.changeStatus(OrderStatus.NEW));
    assertThrows(InvalidOperationException.class, () -> order.changeStatus(OrderStatus.CONFIRMED));
  }

  @Test
  void deliveryMethodAndCourierCannotChangeAfterDispatch() {
    prepareConfirmedCourierOrder();
    order.assignCourier(courier);
    order.changeStatus(OrderStatus.IN_DELIVERY);
    Courier other = new Courier("Олег Быстров", "+7 (922) 333-44-55");

    assertThrows(
        InvalidOperationException.class, () -> order.chooseDeliveryMethod(new ExpressDelivery()));
    assertThrows(InvalidOperationException.class, () -> order.assignCourier(other));
    assertFalse(other.getStatus() == CourierStatus.BUSY);
  }

  @Test
  void destinationIsCustomerAddressForCourierDelivery() {
    assertEquals("ул. Ленина, 1", order.getDestination());

    order.chooseDeliveryMethod(new ExpressDelivery());

    assertEquals("ул. Ленина, 1", order.getDestination());
  }
}
