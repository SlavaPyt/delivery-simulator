package ru.leti.delivery.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.leti.delivery.model.OrderStatus.CANCELLED;
import static ru.leti.delivery.model.OrderStatus.COMPLETED;
import static ru.leti.delivery.model.OrderStatus.CONFIRMED;
import static ru.leti.delivery.model.OrderStatus.IN_DELIVERY;
import static ru.leti.delivery.model.OrderStatus.NEW;
import static ru.leti.delivery.model.OrderStatus.READY_FOR_PICKUP;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OrderStatusTest {

  @Test
  void definesAllowedTransitions() {
    assertEquals(Set.of(CONFIRMED, CANCELLED), NEW.getAllowedNext());
    assertEquals(Set.of(IN_DELIVERY, READY_FOR_PICKUP, CANCELLED), CONFIRMED.getAllowedNext());
    assertEquals(Set.of(COMPLETED, CANCELLED), IN_DELIVERY.getAllowedNext());
    assertEquals(Set.of(COMPLETED, CANCELLED), READY_FOR_PICKUP.getAllowedNext());
  }

  @Test
  void finalStatusesHaveNoTransitions() {
    assertTrue(COMPLETED.isFinal());
    assertTrue(CANCELLED.isFinal());
    for (OrderStatus status : EnumSet.of(COMPLETED, CANCELLED)) {
      for (OrderStatus other : OrderStatus.values()) {
        assertFalse(status.canTransitionTo(other));
      }
    }
  }

  @Test
  void cannotSkipOrGoBackwards() {
    assertFalse(NEW.canTransitionTo(IN_DELIVERY));
    assertFalse(NEW.canTransitionTo(COMPLETED));
    assertFalse(CONFIRMED.canTransitionTo(NEW));
    assertFalse(IN_DELIVERY.canTransitionTo(CONFIRMED));
    assertFalse(READY_FOR_PICKUP.canTransitionTo(IN_DELIVERY));
  }

  @Test
  void nonFinalStatusesAreNotFinal() {
    for (OrderStatus status : EnumSet.of(NEW, CONFIRMED, IN_DELIVERY, READY_FOR_PICKUP)) {
      assertFalse(status.isFinal());
    }
  }
}
