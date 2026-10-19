package ru.leti.delivery;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import ru.leti.delivery.console.ConsoleApp;
import ru.leti.delivery.console.ConsoleInput;
import ru.leti.delivery.console.ConsolePrinter;
import ru.leti.delivery.model.delivery.ExpressDelivery;
import ru.leti.delivery.model.delivery.PickupDelivery;
import ru.leti.delivery.model.delivery.StandardDelivery;
import ru.leti.delivery.service.CourierService;
import ru.leti.delivery.service.OrderService;

/**
 * Точка входа: собирает компоненты приложения и запускает консольное меню.
 *
 * <p>Ключ {@code --echo} включает повтор введённых строк в выводе. Он нужен при вводе из файла или
 * канала, чтобы запись сеанса получалась читаемой.
 */
public final class Main {

  private static final String ECHO_FLAG = "--echo";

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

    boolean echoInput = Arrays.asList(args).contains(ECHO_FLAG);
    ConsoleInput input = new ConsoleInput(System.in, out, echoInput);
    new ConsoleApp(orderService, courierService, input, new ConsolePrinter(out)).run();
  }
}
