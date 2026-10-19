package ru.leti.delivery.console;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import ru.leti.delivery.model.Courier;
import ru.leti.delivery.model.DeliveryMethod;
import ru.leti.delivery.model.Order;
import ru.leti.delivery.model.OrderItem;
import ru.leti.delivery.service.DeliveryQuote;

/**
 * Вывод информации на консоль.
 *
 * <p>Класс отвечает только за форматирование и печать; он ничего не читает и не изменяет в заказах.
 */
public class ConsolePrinter {

  private static final int WIDTH = 78;
  private static final String ORDER_ROW = "%-4s %-16s %-14s %-29s %-14s %s";
  private static final int AMOUNT_COLUMN_EXTRA = 8;
  private static final String DETAIL_ROW = "  %-20s %s";

  private final PrintStream out;

  public ConsolePrinter(PrintStream out) {
    this.out = out;
  }

  public void printWelcome() {
    out.println("=".repeat(WIDTH));
    out.println("  КОНСОЛЬНЫЙ СИМУЛЯТОР СЛУЖБЫ ДОСТАВКИ");
    out.println("=".repeat(WIDTH));
    out.println("Подсказка: на любом вопросе можно ввести «отмена», чтобы прервать операцию.");
  }

  public void printMenu(List<String> titles) {
    out.println();
    out.println("--- Главное меню " + "-".repeat(WIDTH - 17));
    for (int i = 0; i < titles.size(); i++) {
      out.printf("%2d. %s%n", i + 1, titles.get(i));
    }
    out.printf("%2d. %s%n", 0, "Выход");
  }

  public void printInfo(String message) {
    out.println(message);
  }

  public void printSuccess(String message) {
    out.println("Готово: " + message);
  }

  public void printError(String message) {
    out.println("Ошибка: " + message);
  }

  /** Печатает нумерованный список вариантов, из которых пользователь выбирает один. */
  public void printChoices(String header, List<String> options) {
    out.println(header);
    for (int i = 0; i < options.size(); i++) {
      out.printf("%2d. %s%n", i + 1, options.get(i));
    }
  }

  public void printOrderTable(List<Order> orders) {
    if (orders.isEmpty()) {
      out.println("Заказов пока нет.");
      return;
    }
    String header =
        String.format(ORDER_ROW, "№", "Клиент", "Статус", "Доставка", "Курьер", "Сумма")
            .stripTrailing();
    out.println(header);
    out.println("-".repeat(header.length() + AMOUNT_COLUMN_EXTRA));
    for (Order order : orders) {
      out.println(
          String.format(
              ORDER_ROW,
              order.getId(),
              fit(order.getCustomer().getName(), 16),
              fit(order.getStatus().getTitle(), 14),
              fit(order.getDeliveryMethod().map(DeliveryMethod::getTitle).orElse("не выбрана"), 29),
              fit(courierSummary(order), 14),
              formatMoney(order.getTotalCost())));
    }
  }

  public void printOrderDetails(Order order) {
    out.println();
    out.println("Заказ №" + order.getId());
    out.printf(DETAIL_ROW + "%n", "Статус:", order.getStatus().getTitle());
    out.printf(DETAIL_ROW + "%n", "Клиент:", order.getCustomer());
    out.printf(DETAIL_ROW + "%n", "Получение:", order.getDestination());
    out.println("  Позиции:");
    List<OrderItem> items = order.getItems();
    for (int i = 0; i < items.size(); i++) {
      out.printf("    %d. %s%n", i + 1, formatItem(items.get(i)));
    }
    out.printf(DETAIL_ROW + "%n", "Стоимость товаров:", formatMoney(order.getItemsTotal()));
    out.printf(
        DETAIL_ROW + "%n",
        "Способ доставки:",
        order.getDeliveryMethod().map(DeliveryMethod::getTitle).orElse("не выбран"));
    out.printf(
        DETAIL_ROW + "%n",
        "Стоимость доставки:",
        order.getDeliveryMethod().isPresent() ? formatMoney(order.getDeliveryCost()) : "—");
    out.printf(
        DETAIL_ROW + "%n",
        "Срок доставки:",
        order.getEstimatedTime().map(ConsolePrinter::formatDuration).orElse("—"));
    out.printf(DETAIL_ROW + "%n", "Курьер:", courierDetails(order));
    out.printf(DETAIL_ROW + "%n", "Итого:", formatMoney(order.getTotalCost()));
  }

  public void printDeliveryQuotes(List<DeliveryQuote> quotes) {
    out.println("Способы доставки:");
    for (int i = 0; i < quotes.size(); i++) {
      DeliveryQuote quote = quotes.get(i);
      out.printf(
          "%2d. %s: %s, срок %s, курьер %s%n",
          i + 1,
          quote.method().getTitle(),
          formatMoney(quote.cost()),
          formatDuration(quote.estimatedTime()),
          quote.requiresCourier() ? "нужен" : "не нужен");
    }
  }

  public void printCouriers(List<Courier> couriers) {
    if (couriers.isEmpty()) {
      out.println("Курьеров пока нет.");
      return;
    }
    out.println("Курьеры:");
    for (int i = 0; i < couriers.size(); i++) {
      out.printf("%2d. %s%n", i + 1, couriers.get(i));
    }
  }

  public static String formatMoney(BigDecimal amount) {
    return amount.setScale(2, RoundingMode.HALF_UP).toPlainString() + " руб.";
  }

  /** Форматирует срок как верхнюю границу: «до 2 ч», «до 2 сут.». */
  public static String formatDuration(Duration duration) {
    long minutes = duration.toMinutes();
    if (minutes >= 24 * 60 && minutes % (24 * 60) == 0) {
      return "до " + minutes / (24 * 60) + " сут.";
    }
    if (minutes >= 60 && minutes % 60 == 0) {
      return "до " + minutes / 60 + " ч";
    }
    return "до " + minutes + " мин";
  }

  private static String formatItem(OrderItem item) {
    return item.getProductName()
        + " × "
        + item.getQuantity()
        + " по "
        + formatMoney(item.getUnitPrice())
        + " = "
        + formatMoney(item.getTotal());
  }

  private static String courierSummary(Order order) {
    if (order.getCourier().isPresent()) {
      return order.getCourier().get().getName();
    }
    return order
        .getDeliveryMethod()
        .map(method -> method.requiresCourier() ? "не назначен" : "не требуется")
        .orElse("—");
  }

  private static String courierDetails(Order order) {
    if (order.getCourier().isPresent()) {
      Courier courier = order.getCourier().get();
      return courier.getName() + ", тел. " + courier.getPhone();
    }
    return courierSummary(order);
  }

  private static String fit(String text, int width) {
    return text.length() <= width ? text : text.substring(0, width - 1) + "…";
  }
}
