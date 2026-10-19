package ru.leti.delivery.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.leti.delivery.model.Courier.CourierStatus;
import ru.leti.delivery.model.Order;
import ru.leti.delivery.model.OrderStatus;
import ru.leti.delivery.model.delivery.ExpressDelivery;
import ru.leti.delivery.model.delivery.PickupDelivery;
import ru.leti.delivery.model.delivery.StandardDelivery;
import ru.leti.delivery.service.CourierService;
import ru.leti.delivery.service.OrderService;

/** Сквозные тесты: сценарии работы пользователя с меню от ввода до вывода. */
class ConsoleAppTest {

  /** Создание заказа №1: клиент, адрес и одна позиция на 1000 руб. */
  private static final List<String> CREATE_ORDER =
      List.of(
          "1", "Анна Смирнова", "+7 (999) 123-45-67", "ул. Ленина, 1", "Пицца", "2", "500", "нет");

  private OrderService orderService;
  private CourierService courierService;

  @BeforeEach
  void setUp() {
    courierService = new CourierService();
    courierService.addCourier("Пётр Курьеров", "+7 (911) 111-22-33");
    courierService.addCourier("Олег Быстров", "+7 (922) 222-33-44");
    courierService.addCourier("Мария Скорова", "+7 (933) 333-44-55");
    orderService =
        new OrderService(
            List.of(new StandardDelivery(), new ExpressDelivery(), new PickupDelivery()));
  }

  @SafeVarargs
  private String run(List<String>... parts) {
    StringBuilder script = new StringBuilder();
    for (List<String> part : parts) {
      part.forEach(line -> script.append(line).append('\n'));
    }
    ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
    ConsoleInput input =
        new ConsoleInput(
            new ByteArrayInputStream(script.toString().getBytes(StandardCharsets.UTF_8)),
            out,
            true);
    new ConsoleApp(orderService, courierService, input, new ConsolePrinter(out)).run();
    return outBytes.toString(StandardCharsets.UTF_8);
  }

  private static void assertContains(String output, String expected) {
    assertTrue(output.contains(expected), "В выводе нет «" + expected + "»:\n" + output);
  }

  @Test
  void courierDeliveryScenarioFromCreationToCompletion() {
    String output =
        run(
            CREATE_ORDER,
            List.of("5", "1", "1"),
            List.of("7", "1", "1"),
            List.of("6", "1", "1"),
            List.of("7", "1", "1"),
            List.of("7", "1", "1"),
            List.of("0"));

    assertContains(output, "Готово: создан заказ №1");
    assertContains(output, "для заказа №1 выбран способ «Стандартная доставка курьером»");
    assertContains(output, "курьер Пётр Курьеров назначен на заказ №1");
    assertContains(output, "статус заказа №1 изменён на «В доставке»");
    assertContains(output, "статус заказа №1 изменён на «Выполнен»");
    assertContains(output, "Работа программы завершена.");

    Order order = orderService.getOrder(1);
    assertEquals(OrderStatus.COMPLETED, order.getStatus());
    assertEquals("Пётр Курьеров", order.getCourier().orElseThrow().getName());
    assertTrue(courierService.getAllCouriers().get(0).isAvailable());
  }

  @Test
  void pickupScenarioNeedsNoCourier() {
    String output =
        run(
            CREATE_ORDER,
            List.of("5", "1", "3"),
            List.of("6", "1"),
            List.of("7", "1", "1"),
            List.of("7", "1", "1"),
            List.of("7", "1", "1"),
            List.of("0"));

    assertContains(output, "Для способа «Самовывоз» курьер не требуется, назначать некого.");
    assertContains(output, "статус заказа №1 изменён на «Готов к выдаче»");
    assertContains(output, "Пункт выдачи:");
    assertFalse(
        output.contains("В доставке"), "Для самовывоза не должно быть статуса «В доставке»");
    assertEquals(OrderStatus.COMPLETED, orderService.getOrder(1).getStatus());
  }

  @Test
  void showsDeliveryCostAndTimeForEveryMethod() {
    String output = run(CREATE_ORDER, List.of("5", "1", "2", "0"));

    assertContains(output, "1. Стандартная доставка курьером: 250.00 руб., срок до 2 сут.");
    assertContains(output, "2. Экспресс-доставка: 550.00 руб., срок до 2 ч");
    assertContains(output, "3. Самовывоз: 0.00 руб., срок до 1 ч");
    assertContains(output, "Итого:               1550.00 руб.");
  }

  @Test
  void invalidInputGetsClearMessagesAndIsAskedAgain() {
    String output =
        run(
            List.of("abc", "99", "-1"),
            List.of("1", "", "Анна1", "Анна Смирнова"),
            List.of("абв", "+7 (999) 123-45-67"),
            List.of("", "ул. Ленина, 1"),
            List.of("", "Пицца", "0", "1", "0", "abc", "100"),
            List.of("может быть", "нет"),
            List.of("4", "42", "1"),
            List.of("0"));

    assertContains(output, "Введите целое число от 0 до 10");
    assertContains(output, "Число должно быть от 0 до 10");
    assertContains(output, "Ошибка: Имя не может быть пустым");
    assertContains(output, "Имя должно начинаться с буквы");
    assertContains(output, "Ошибка: Телефон указан неверно");
    assertContains(output, "Ошибка: Адрес доставки не может быть пустым");
    assertContains(output, "Ошибка: Название товара не может быть пустым");
    assertContains(output, "Число должно быть от 1 до 999");
    assertContains(output, "Ошибка: Цена должна быть больше нуля");
    assertContains(output, "Введите сумму в рублях");
    assertContains(output, "Ответьте «да» или «нет»");
    assertContains(output, "Ошибка: Заказ №42 не найден");
    assertEquals(1, orderService.getAllOrders().size());
  }

  @Test
  void domainRuleViolationsAreShownAsMessages() {
    String output = run(CREATE_ORDER, List.of("7", "1", "1"), List.of("6", "1"), List.of("0"));

    assertContains(output, "Ошибка: Нельзя подтвердить заказ: не выбран способ доставки");
    assertContains(output, "Ошибка: сначала выберите способ доставки для заказа №1");
    assertEquals(OrderStatus.NEW, orderService.getOrder(1).getStatus());
  }

  @Test
  void itemsCannotBeAddedAfterConfirmation() {
    String output =
        run(
            CREATE_ORDER,
            List.of("5", "1", "1"),
            List.of("7", "1", "1"),
            List.of("2", "1", "Сок", "1", "100"),
            List.of("0"));

    assertContains(output, "Ошибка: Добавлять позиции можно только в новый заказ");
    assertEquals(1, orderService.getOrder(1).getItems().size());
  }

  @Test
  void itemCanBeAddedToNewOrder() {
    String output = run(CREATE_ORDER, List.of("2", "1", "Сок", "3", "99,90"), List.of("0"));

    assertContains(output, "Готово: позиция добавлена в заказ №1");
    assertEquals(2, orderService.getOrder(1).getItems().size());
  }

  @Test
  void commandCancelAbortsOperationWithoutSideEffects() {
    String output = run(List.of("1", "Анна", "отмена"), List.of("3"), List.of("0"));

    assertContains(output, "Операция отменена.");
    assertContains(output, "Заказов пока нет.");
    assertTrue(orderService.getAllOrders().isEmpty());
  }

  @Test
  void programStopsGracefullyWhenInputEnds() {
    String output = run(List.of("1", "Анна"));

    assertContains(output, "Работа программы завершена.");
  }

  @Test
  void operationsOnEmptyOrderListExplainWhatToDo() {
    String output = run(List.of("4", "5", "6", "7", "2", "0"));

    assertEquals(5, output.split("Заказов пока нет. Сначала создайте заказ.", -1).length - 1);
  }

  @Test
  void courierStatusChangesAffectAssignment() {
    String output =
        run(
            CREATE_ORDER,
            List.of("5", "1", "1"),
            List.of("10", "1", "2"),
            List.of("6", "1"),
            List.of("0"));

    assertContains(output, "Готово: курьер Пётр Курьеров: Не на смене");
    assertEquals(CourierStatus.OFFLINE, courierService.getAllCouriers().get(0).getStatus());
    int assignmentList = output.lastIndexOf("Курьеры:");
    assertFalse(output.substring(assignmentList).contains("Пётр Курьеров"));
  }

  @Test
  void reportsWhenNoCourierIsAvailable() {
    String output =
        run(
            CREATE_ORDER,
            List.of("5", "1", "1"),
            List.of("10", "1", "2", "10", "2", "2", "10", "3", "2"),
            List.of("6", "1"),
            List.of("0"));

    assertContains(output, "Ошибка: нет свободных курьеров");
  }

  @Test
  void busyCourierStatusCannotBeChangedManually() {
    String output =
        run(
            CREATE_ORDER,
            List.of("5", "1", "1"),
            List.of("6", "1", "1"),
            List.of("10", "1", "3"),
            List.of("0"));

    assertContains(output, "Ошибка: Курьер Пётр Курьеров выполняет заказ");
    assertEquals(CourierStatus.BUSY, courierService.getAllCouriers().get(0).getStatus());
  }

  @Test
  void courierCanBeAdded() {
    String output = run(List.of("9", "Иван Новиков", "+7 (944) 444-55-66"), List.of("8", "0"));

    assertContains(output, "Готово: курьер добавлен: Иван Новиков");
    assertContains(output, "4. Иван Новиков, тел. +7 (944) 444-55-66 (Свободен)");
    assertEquals(4, courierService.getAllCouriers().size());
  }

  @Test
  void orderListShowsAllOrders() {
    String output = run(CREATE_ORDER, CREATE_ORDER, List.of("3", "0"));

    assertContains(output, "Готово: создан заказ №2");
    assertEquals(2, orderService.getAllOrders().size());
    assertContains(output, "Клиент");
  }
}
