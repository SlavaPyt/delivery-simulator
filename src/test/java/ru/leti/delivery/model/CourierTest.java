package ru.leti.delivery.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.leti.delivery.exception.InvalidOperationException;
import ru.leti.delivery.exception.ValidationException;
import ru.leti.delivery.model.Courier.CourierStatus;

class CourierTest {

  private Courier courier;

  @BeforeEach
  void setUp() {
    courier = new Courier("Пётр Курьеров", "+7 (911) 000-11-22");
  }

  @Test
  void newCourierIsAvailable() {
    assertEquals(CourierStatus.AVAILABLE, courier.getStatus());
    assertTrue(courier.isAvailable());
  }

  @Test
  void validatesNameAndPhone() {
    assertThrows(ValidationException.class, () -> new Courier("", "89991234567"));
    assertThrows(ValidationException.class, () -> new Courier("Пётр", "abc"));
  }

  @Test
  void availableCourierCanGoOnBreakAndOffline() {
    courier.setStatus(CourierStatus.ON_BREAK);
    assertEquals(CourierStatus.ON_BREAK, courier.getStatus());

    courier.setStatus(CourierStatus.OFFLINE);
    assertEquals(CourierStatus.OFFLINE, courier.getStatus());

    courier.setStatus(CourierStatus.AVAILABLE);
    assertTrue(courier.isAvailable());
  }

  @Test
  void busyStatusCannotBeSetManually() {
    assertThrows(InvalidOperationException.class, () -> courier.setStatus(CourierStatus.BUSY));
  }

  @Test
  void occupiedCourierCannotChangeStatusUntilReleased() {
    courier.occupy();
    assertEquals(CourierStatus.BUSY, courier.getStatus());
    assertThrows(InvalidOperationException.class, () -> courier.setStatus(CourierStatus.OFFLINE));

    courier.release();
    assertTrue(courier.isAvailable());
  }

  @Test
  void onlyAvailableCourierCanBeOccupied() {
    courier.setStatus(CourierStatus.ON_BREAK);
    assertThrows(InvalidOperationException.class, courier::occupy);
    assertFalse(courier.isAvailable());
  }

  @Test
  void releaseDoesNotChangeNonBusyCourier() {
    courier.setStatus(CourierStatus.OFFLINE);
    courier.release();
    assertEquals(CourierStatus.OFFLINE, courier.getStatus());
  }
}
