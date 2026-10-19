package ru.leti.delivery.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.leti.delivery.exception.ValidationException;

class CustomerTest {

  @Test
  void createsCustomerAndTrimsName() {
    Customer customer = new Customer("  Анна Смирнова ", "+7 (999) 123-45-67");

    assertEquals("Анна Смирнова", customer.getName());
    assertEquals("+7 (999) 123-45-67", customer.getPhone());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "123", "Иван1", "@Иван"})
  void rejectsInvalidName(String name) {
    assertThrows(ValidationException.class, () -> new Customer(name, "89991234567"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {"12345", "abcdefghijk", "+7 999 12", "8999123456789012345", "89991234567x"})
  void rejectsInvalidPhone(String phone) {
    assertThrows(ValidationException.class, () -> new Customer("Иван", phone));
  }

  @Test
  void settersValidateNewValues() {
    Customer customer = new Customer("Иван", "89991234567");

    assertThrows(ValidationException.class, () -> customer.setName(" "));
    assertThrows(ValidationException.class, () -> customer.setPhone("1"));

    customer.setName("Пётр Иванов-Петров");
    customer.setPhone("+79990001122");
    assertEquals("Пётр Иванов-Петров", customer.getName());
    assertEquals("+79990001122", customer.getPhone());
  }
}
