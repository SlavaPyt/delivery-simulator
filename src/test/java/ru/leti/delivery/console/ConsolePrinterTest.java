package ru.leti.delivery.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.leti.delivery.model.Courier;
import ru.leti.delivery.model.Customer;
import ru.leti.delivery.model.Order;
import ru.leti.delivery.model.OrderItem;
import ru.leti.delivery.model.delivery.PickupDelivery;
import ru.leti.delivery.model.delivery.StandardDelivery;
import ru.leti.delivery.service.DeliveryQuote;

class ConsolePrinterTest {

  private final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
  private final ConsolePrinter printer =
      new ConsolePrinter(new PrintStream(outBytes, true, StandardCharsets.UTF_8));
  private Order order;

  @BeforeEach
  void setUp() {
    order = new Order(7, new Customer("Анна Смирнова", "+7 (999) 123-45-67"), "ул. Ленина, 1");
    order.addItem(new OrderItem("Пицца", 2, new BigDecimal("500")));
  }

  private String output() {
    return outBytes.toString(StandardCharsets.UTF_8);
  }

  private static void assertRow(String text, String label, String value) {
    assertTrue(
        Pattern.compile(Pattern.quote(label) + "\\s+" + Pattern.quote(value)).matcher(text).find(),
        "Не найдена строка «" + label + " " + value + "» в выводе:\n" + text);
  }

  @Test
  void formatsMoneyWithKopecks() {
    assertEquals("1250.00 руб.", ConsolePrinter.formatMoney(new BigDecimal("1250")));
    assertEquals("10.01 руб.", ConsolePrinter.formatMoney(new BigDecimal("10.005")));
  }

  @Test
  void formatsDurationAsUpperBound() {
    assertEquals("до 2 сут.", ConsolePrinter.formatDuration(Duration.ofHours(48)));
    assertEquals("до 1 сут.", ConsolePrinter.formatDuration(Duration.ofHours(24)));
    assertEquals("до 2 ч", ConsolePrinter.formatDuration(Duration.ofHours(2)));
    assertEquals("до 36 ч", ConsolePrinter.formatDuration(Duration.ofHours(36)));
    assertEquals("до 45 мин", ConsolePrinter.formatDuration(Duration.ofMinutes(45)));
  }

  @Test
  void printsMenuWithExitItem() {
    printer.printMenu(List.of("Создать заказ", "Список заказов"));

    assertTrue(output().contains(" 1. Создать заказ"));
    assertTrue(output().contains(" 2. Список заказов"));
    assertTrue(output().contains(" 0. Выход"));
  }

  @Test
  void printsEmptyListMessages() {
    printer.printOrderTable(List.of());
    printer.printCouriers(List.of());

    assertTrue(output().contains("Заказов пока нет."));
    assertTrue(output().contains("Курьеров пока нет."));
  }

  @Test
  void printsOrderTableRow() {
    order.chooseDeliveryMethod(new StandardDelivery());

    printer.printOrderTable(List.of(order));

    String text = output();
    assertTrue(text.contains("Клиент"));
    assertTrue(text.contains("Анна Смирнова"));
    assertTrue(text.contains("Новый"));
    assertTrue(text.contains("Стандартная доставка курьером"));
    assertTrue(text.contains("не назначен"));
    assertTrue(text.contains("1250.00 руб."));
  }

  @Test
  void printsOrderDetailsForCourierDelivery() {
    order.chooseDeliveryMethod(new StandardDelivery());
    order.assignCourier(new Courier("Пётр Курьеров", "+7 (911) 000-11-22"));

    printer.printOrderDetails(order);

    String text = output();
    assertTrue(text.contains("Заказ №7"));
    assertTrue(text.contains("Пицца × 2 по 500.00 руб. = 1000.00 руб."));
    assertRow(text, "Стоимость доставки:", "250.00 руб.");
    assertTrue(text.contains("до 2 сут."));
    assertTrue(text.contains("Пётр Курьеров, тел. +7 (911) 000-11-22"));
    assertRow(text, "Итого:", "1250.00 руб.");
  }

  @Test
  void printsOrderDetailsForPickup() {
    order.chooseDeliveryMethod(new PickupDelivery());

    printer.printOrderDetails(order);

    String text = output();
    assertTrue(text.contains("Пункт выдачи"));
    assertTrue(text.contains("не требуется"));
    assertRow(text, "Итого:", "1000.00 руб.");
  }

  @Test
  void printsOrderDetailsBeforeDeliveryMethodIsChosen() {
    printer.printOrderDetails(order);

    String text = output();
    assertTrue(text.contains("не выбран"));
    assertTrue(text.contains("ул. Ленина, 1"));
  }

  @Test
  void printsDeliveryQuotes() {
    StandardDelivery standard = new StandardDelivery();
    PickupDelivery pickup = new PickupDelivery();

    printer.printDeliveryQuotes(
        List.of(
            new DeliveryQuote(standard, new BigDecimal("250.00"), standard.estimateTime()),
            new DeliveryQuote(pickup, new BigDecimal("0.00"), pickup.estimateTime())));

    String text = output();
    assertTrue(text.contains("1. Стандартная доставка курьером: 250.00 руб., срок до 2 сут."));
    assertTrue(text.contains("курьер нужен"));
    assertTrue(text.contains("2. Самовывоз: 0.00 руб., срок до 1 ч, курьер не нужен"));
  }

  @Test
  void truncatesTooLongNamesInTable() {
    Order longName =
        new Order(
            1,
            new Customer("Александр Константинович Ужасно-Длиннофамильный", "89991234567"),
            "ул. Ленина, 1");

    printer.printOrderTable(List.of(longName));

    assertTrue(output().contains("…"));
  }
}
