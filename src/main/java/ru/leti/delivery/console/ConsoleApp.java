package ru.leti.delivery.console;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import ru.leti.delivery.exception.DeliveryException;
import ru.leti.delivery.model.Courier;
import ru.leti.delivery.model.Courier.CourierStatus;
import ru.leti.delivery.model.DeliveryMethod;
import ru.leti.delivery.model.Order;
import ru.leti.delivery.model.OrderItem;
import ru.leti.delivery.model.OrderStatus;
import ru.leti.delivery.model.Validation;
import ru.leti.delivery.service.CourierService;
import ru.leti.delivery.service.DeliveryQuote;
import ru.leti.delivery.service.OrderService;

/**
 * Консольное меню приложения: связывает ввод, сервисы и вывод.
 *
 * <p>Класс только организует диалог с пользователем. Данные он читает через {@link ConsoleInput},
 * печатает через {@link ConsolePrinter}, а все операции над заказами и курьерами выполняют сервисы;
 * допустимость операций проверяет модель.
 */
public class ConsoleApp {

  private record MenuItem(String title, Runnable action) {}

  private final OrderService orderService;
  private final CourierService courierService;
  private final ConsoleInput input;
  private final ConsolePrinter printer;
  private final List<MenuItem> menu;

  public ConsoleApp(
      OrderService orderService,
      CourierService courierService,
      ConsoleInput input,
      ConsolePrinter printer) {
    this.orderService = orderService;
    this.courierService = courierService;
    this.input = input;
    this.printer = printer;
    this.menu =
        List.of(
            new MenuItem("Создать новый заказ", this::createOrder),
            new MenuItem("Добавить позицию в заказ", this::addItem),
            new MenuItem("Показать список заказов", this::showOrders),
            new MenuItem("Показать информацию о заказе", this::showOrder),
            new MenuItem("Выбрать способ доставки", this::chooseDeliveryMethod),
            new MenuItem("Назначить курьера", this::assignCourier),
            new MenuItem("Изменить статус заказа", this::changeOrderStatus),
            new MenuItem("Показать курьеров", this::showCouriers),
            new MenuItem("Добавить курьера", this::addCourier),
            new MenuItem("Изменить статус курьера", this::changeCourierStatus));
  }

  /** Запускает главный цикл меню; работа завершается командой «0» или концом ввода. */
  public void run() {
    printer.printWelcome();
    List<String> titles = menu.stream().map(MenuItem::title).toList();
    boolean running = true;
    while (running) {
      printer.printMenu(titles);
      try {
        int choice = input.readInt("Выберите пункт меню", 0, menu.size());
        if (choice == 0) {
          running = false;
        } else {
          menu.get(choice - 1).action().run();
        }
      } catch (OperationCancelledException e) {
        printer.printInfo("Операция отменена.");
      } catch (DeliveryException e) {
        printer.printError(e.getMessage());
      } catch (InputClosedException e) {
        running = false;
      }
    }
    printer.printInfo("Работа программы завершена.");
  }

  private void createOrder() {
    printer.printInfo("Создание заказа.");
    String name = input.read("Имя клиента", Validation::requirePersonName);
    String phone = input.read("Телефон клиента", Validation::requirePhone);
    String address =
        input.read("Адрес доставки", line -> Validation.requireText(line, "Адрес доставки"));
    List<OrderItem> items = new ArrayList<>();
    do {
      printer.printInfo("Позиция №" + (items.size() + 1));
      items.add(readItem());
    } while (input.confirm("Добавить ещё одну позицию?"));

    Order order = orderService.createOrder(name, phone, address, items);
    printer.printSuccess("создан заказ №" + order.getId());
    printer.printOrderDetails(order);
    printer.printInfo("Следующий шаг: «Выбрать способ доставки».");
  }

  private void addItem() {
    Optional<Order> order = selectOrder();
    if (order.isEmpty()) {
      return;
    }
    OrderItem item = readItem();
    orderService.addItem(order.get().getId(), item);
    printer.printSuccess("позиция добавлена в заказ №" + order.get().getId());
    printer.printOrderDetails(order.get());
  }

  private void showOrders() {
    printer.printOrderTable(orderService.getAllOrders());
  }

  private void showOrder() {
    selectOrder().ifPresent(printer::printOrderDetails);
  }

  private void chooseDeliveryMethod() {
    Optional<Order> selected = selectOrder();
    if (selected.isEmpty()) {
      return;
    }
    Order order = selected.get();
    List<DeliveryQuote> quotes = orderService.quoteDelivery(order.getId());
    printer.printDeliveryQuotes(quotes);
    int choice = input.readInt("Выберите способ доставки", 1, quotes.size());

    DeliveryQuote quote =
        orderService.chooseDeliveryMethod(order.getId(), quotes.get(choice - 1).method());
    printer.printSuccess(
        "для заказа №"
            + order.getId()
            + " выбран способ «"
            + quote.method().getTitle()
            + "»: стоимость доставки "
            + ConsolePrinter.formatMoney(quote.cost())
            + ", срок "
            + ConsolePrinter.formatDuration(quote.estimatedTime()));
    printer.printInfo(
        quote.requiresCourier()
            ? "Для этого способа нужен курьер: используйте «Назначить курьера»."
            : "Для этого способа курьер не требуется.");
    printer.printOrderDetails(order);
  }

  private void assignCourier() {
    Optional<Order> selected = selectOrder();
    if (selected.isEmpty()) {
      return;
    }
    Order order = selected.get();
    Optional<DeliveryMethod> method = order.getDeliveryMethod();
    if (method.isEmpty()) {
      printer.printError("сначала выберите способ доставки для заказа №" + order.getId());
      return;
    }
    if (!method.get().requiresCourier()) {
      printer.printInfo(
          "Для способа «" + method.get().getTitle() + "» курьер не требуется, назначать некого.");
      return;
    }
    List<Courier> available = courierService.getAvailableCouriers();
    if (available.isEmpty()) {
      printer.printError("нет свободных курьеров, попробуйте позже");
      return;
    }
    printer.printCouriers(available);
    Courier courier = available.get(input.readInt("Выберите курьера", 1, available.size()) - 1);

    orderService.assignCourier(order.getId(), courier);
    printer.printSuccess("курьер " + courier.getName() + " назначен на заказ №" + order.getId());
    printer.printOrderDetails(order);
  }

  private void changeOrderStatus() {
    Optional<Order> selected = selectOrder();
    if (selected.isEmpty()) {
      return;
    }
    Order order = selected.get();
    List<OrderStatus> options = order.getAvailableTransitions();
    if (options.isEmpty()) {
      printer.printInfo(
          "Заказ №"
              + order.getId()
              + " уже «"
              + order.getStatus().getTitle()
              + "»: статус больше не меняется.");
      printer.printOrderDetails(order);
      return;
    }
    printer.printInfo(
        "Текущий статус заказа №" + order.getId() + ": " + order.getStatus().getTitle());
    printer.printChoices(
        "Можно перевести в статус:", options.stream().map(OrderStatus::getTitle).toList());
    OrderStatus next = options.get(input.readInt("Выберите новый статус", 1, options.size()) - 1);

    orderService.changeStatus(order.getId(), next);
    printer.printSuccess(
        "статус заказа №" + order.getId() + " изменён на «" + next.getTitle() + "»");
    printer.printOrderDetails(order);
  }

  private void showCouriers() {
    printer.printCouriers(courierService.getAllCouriers());
  }

  private void addCourier() {
    String name = input.read("Имя курьера", Validation::requirePersonName);
    String phone = input.read("Телефон курьера", Validation::requirePhone);
    Courier courier = courierService.addCourier(name, phone);
    printer.printSuccess("курьер добавлен: " + courier);
  }

  private void changeCourierStatus() {
    List<Courier> couriers = courierService.getAllCouriers();
    if (couriers.isEmpty()) {
      printer.printInfo("Курьеров пока нет.");
      return;
    }
    printer.printCouriers(couriers);
    Courier courier = couriers.get(input.readInt("Выберите курьера", 1, couriers.size()) - 1);

    List<CourierStatus> statuses =
        Arrays.stream(CourierStatus.values())
            .filter(status -> status != CourierStatus.BUSY)
            .toList();
    printer.printChoices(
        "Новый статус (статус «Занят» выставляется автоматически при назначении на заказ):",
        statuses.stream().map(CourierStatus::getTitle).toList());
    CourierStatus status = statuses.get(input.readInt("Выберите статус", 1, statuses.size()) - 1);

    courierService.changeStatus(courier, status);
    printer.printSuccess("курьер " + courier.getName() + ": " + courier.getStatus().getTitle());
  }

  private OrderItem readItem() {
    String product =
        input.read("Название товара", line -> Validation.requireText(line, "Название товара"));
    int quantity = input.readInt("Количество", 1, 999);
    BigDecimal price = input.readPrice("Цена за единицу, руб.");
    return new OrderItem(product, quantity, price);
  }

  /** Показывает заказы и просит выбрать один из них; пусто, если заказов ещё нет. */
  private Optional<Order> selectOrder() {
    if (orderService.getAllOrders().isEmpty()) {
      printer.printInfo("Заказов пока нет. Сначала создайте заказ.");
      return Optional.empty();
    }
    printer.printOrderTable(orderService.getAllOrders());
    Order order =
        input.read(
            "Номер заказа",
            line -> orderService.getOrder(ConsoleInput.parseInt(line, 1, Integer.MAX_VALUE)));
    return Optional.of(order);
  }
}
