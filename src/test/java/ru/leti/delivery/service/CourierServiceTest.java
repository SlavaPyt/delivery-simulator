package ru.leti.delivery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.leti.delivery.exception.ValidationException;
import ru.leti.delivery.model.Courier;
import ru.leti.delivery.model.Courier.CourierStatus;

class CourierServiceTest {

  private CourierService service;
  private Courier first;
  private Courier second;

  @BeforeEach
  void setUp() {
    service = new CourierService();
    first = service.addCourier("Пётр Курьеров", "+7 (911) 000-11-22");
    second = service.addCourier("Олег Быстров", "+7 (922) 333-44-55");
  }

  @Test
  void keepsRegisteredCouriersInOrder() {
    assertEquals(List.of(first, second), service.getAllCouriers());
  }

  @Test
  void availableCouriersExcludeThoseOnBreakOrOffline() {
    service.changeStatus(first, CourierStatus.ON_BREAK);

    assertEquals(List.of(second), service.getAvailableCouriers());

    service.changeStatus(second, CourierStatus.OFFLINE);

    assertEquals(List.of(), service.getAvailableCouriers());
  }

  @Test
  void rejectsInvalidCourierData() {
    assertThrows(ValidationException.class, () -> service.addCourier("", "89991234567"));
    assertThrows(ValidationException.class, () -> service.addCourier("Иван", "12"));
    assertEquals(2, service.getAllCouriers().size());
  }

  @Test
  void rejectsMissingCourier() {
    assertThrows(
        ValidationException.class, () -> service.changeStatus(null, CourierStatus.OFFLINE));
  }
}
