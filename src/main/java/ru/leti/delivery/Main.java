package ru.leti.delivery;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import ru.leti.delivery.console.ConsoleApp;
import ru.leti.delivery.console.ConsoleInput;
import ru.leti.delivery.console.ConsolePrinter;
import ru.leti.delivery.model.delivery.ExpressDelivery;
import ru.leti.delivery.model.delivery.PickupDelivery;
import ru.leti.delivery.model.delivery.StandardDelivery;
import ru.leti.delivery.service.CourierService;
import ru.leti.delivery.service.OrderService;

/** Точка входа: собирает компоненты приложения и запускает консольное меню. */
public final class Main {

  private Main() {}

  public static void main(String[] args) {
    PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);

    CourierService courierService = new CourierService();
    courierService.addCourier("Пётр Курьеров", "+7 (911) 111-22-33");
    courierService.addCourier("Олег Быстров", "+7 (922) 222-33-44");
    courierService.addCourier("Мария Скорова", "+7 (933) 333-44-55");

    OrderService orderService =
        new OrderService(
            List.of(new StandardDelivery(), new ExpressDelivery(), new PickupDelivery()));

    boolean interactive = System.console() != null;
    ConsoleInput input = new ConsoleInput(System.in, out, !interactive);
    new ConsoleApp(orderService, courierService, input, new ConsolePrinter(out)).run();
  }
}
