package ru.leti.delivery.model.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.leti.delivery.model.DeliveryMethod;
import ru.leti.delivery.model.OrderStatus;

class DeliveryMethodsTest {

  private static final String ADDRESS = "ул. Ленина, 1";

  private final DeliveryMethod standard = new StandardDelivery();
  private final DeliveryMethod express = new ExpressDelivery();
  private final DeliveryMethod pickup = new PickupDelivery();

  @Test
  void standardDeliveryHasFixedCostBelowThreshold() {
    assertEquals(new BigDecimal("250.00"), standard.calculateCost(new BigDecimal("1000.00")));
    assertEquals(new BigDecimal("250.00"), standard.calculateCost(new BigDecimal("2999.99")));
  }

  @Test
  void standardDeliveryIsFreeFromThreshold() {
    assertEquals(new BigDecimal("0.00"), standard.calculateCost(new BigDecimal("3000.00")));
    assertEquals(new BigDecimal("0.00"), standard.calculateCost(new BigDecimal("10000.00")));
  }

  @Test
  void expressDeliveryAddsPercentageToBaseCost() {
    assertEquals(new BigDecimal("550.00"), express.calculateCost(new BigDecimal("1000.00")));
    assertEquals(new BigDecimal("500.03"), express.calculateCost(new BigDecimal("0.55")));
  }

  @Test
  void expressIsMoreExpensiveAndFasterThanStandard() {
    BigDecimal itemsTotal = new BigDecimal("1500.00");

    assertTrue(express.calculateCost(itemsTotal).compareTo(standard.calculateCost(itemsTotal)) > 0);
    assertTrue(express.estimateTime().compareTo(standard.estimateTime()) < 0);
  }

  @Test
  void pickupIsFree() {
    assertEquals(new BigDecimal("0.00"), pickup.calculateCost(new BigDecimal("1.00")));
    assertEquals(new BigDecimal("0.00"), pickup.calculateCost(new BigDecimal("99999.00")));
  }

  @Test
  void estimatesTime() {
    assertEquals(Duration.ofHours(48), standard.estimateTime());
    assertEquals(Duration.ofHours(2), express.estimateTime());
    assertEquals(Duration.ofHours(1), pickup.estimateTime());
  }

  @Test
  void onlyCourierMethodsRequireCourier() {
    assertTrue(standard.requiresCourier());
    assertTrue(express.requiresCourier());
    assertFalse(pickup.requiresCourier());
  }

  @Test
  void dispatchStatusDependsOnMethod() {
    assertEquals(OrderStatus.IN_DELIVERY, standard.getDispatchStatus());
    assertEquals(OrderStatus.IN_DELIVERY, express.getDispatchStatus());
    assertEquals(OrderStatus.READY_FOR_PICKUP, pickup.getDispatchStatus());
  }

  @Test
  void destinationIsCustomerAddressOrPickupPoint() {
    assertEquals(ADDRESS, standard.describeDestination(ADDRESS));
    assertEquals(ADDRESS, express.describeDestination(ADDRESS));
    assertEquals(
        "Пункт выдачи: " + PickupDelivery.DEFAULT_PICKUP_POINT,
        pickup.describeDestination(ADDRESS));
  }

  @Test
  void allMethodsAreUsedThroughCommonContract() {
    List<DeliveryMethod> methods = List.of(standard, express, pickup);

    for (DeliveryMethod method : methods) {
      assertFalse(method.getTitle().isBlank());
      assertTrue(method.calculateCost(new BigDecimal("100.00")).signum() >= 0);
      assertTrue(method.estimateTime().toMinutes() > 0);
    }
  }
}
