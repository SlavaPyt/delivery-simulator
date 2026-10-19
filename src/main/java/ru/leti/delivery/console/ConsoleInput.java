package ru.leti.delivery.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import ru.leti.delivery.exception.DeliveryException;
import ru.leti.delivery.exception.ValidationException;
import ru.leti.delivery.model.Validation;

/**
 * Чтение данных, введённых пользователем с консоли.
 *
 * <p>Класс отвечает только за ввод: показывает приглашение, разбирает строку и при ошибке сообщает
 * о ней и запрашивает значение заново. Бизнес-правила он не содержит — правила проверки передаются
 * в виде функций-разборщиков.
 */
public class ConsoleInput {

  private static final Set<String> CANCEL_COMMANDS = Set.of("отмена", "cancel");
  private static final Set<String> YES_ANSWERS = Set.of("да", "д", "y", "yes");
  private static final Set<String> NO_ANSWERS = Set.of("нет", "н", "n", "no");
  private static final Pattern INTEGER = Pattern.compile("[+-]?\\d{1,9}");
  private static final Pattern MONEY = Pattern.compile("\\d{1,9}([.,]\\d{1,2})?");

  private final BufferedReader reader;
  private final PrintStream out;

  public ConsoleInput(InputStream in, PrintStream out) {
    this.reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    this.out = out;
  }

  /**
   * Читает строку после приглашения. Команда отмены прерывает текущую операцию.
   *
   * @throws OperationCancelledException если пользователь ввёл команду отмены
   * @throws InputClosedException если поток ввода закончился
   */
  public String readLine(String prompt) {
    out.print(prompt + ": ");
    out.flush();
    String line;
    try {
      line = reader.readLine();
    } catch (IOException e) {
      throw new InputClosedException();
    }
    if (line == null) {
      out.println();
      throw new InputClosedException();
    }
    String trimmed = line.trim();
    if (CANCEL_COMMANDS.contains(trimmed.toLowerCase(Locale.ROOT))) {
      throw new OperationCancelledException();
    }
    return trimmed;
  }

  /**
   * Читает значение, пока разборщик не примет введённую строку. Ошибку разборщика (исключение
   * приложения) выводит пользователю и запрашивает ввод повторно.
   */
  public <T> T read(String prompt, Function<String, T> parser) {
    while (true) {
      String line = readLine(prompt);
      try {
        return parser.apply(line);
      } catch (DeliveryException e) {
        out.println("Ошибка: " + e.getMessage());
      }
    }
  }

  public int readInt(String prompt, int min, int max) {
    return read(prompt, line -> parseInt(line, min, max));
  }

  /** Читает положительную цену в рублях, например {@code 450} или {@code 450,50}. */
  public BigDecimal readPrice(String prompt) {
    return read(prompt, line -> Validation.requirePositiveMoney(parseMoney(line), "Цена"));
  }

  /** Задаёт вопрос, на который отвечают «да» или «нет». */
  public boolean confirm(String question) {
    return read(question + " (да/нет)", ConsoleInput::parseYesNo);
  }

  static int parseInt(String line, int min, int max) {
    if (!INTEGER.matcher(line).matches()) {
      throw new ValidationException("Введите целое число от " + min + " до " + max);
    }
    int value = Integer.parseInt(line);
    if (value < min || value > max) {
      throw new ValidationException("Число должно быть от " + min + " до " + max);
    }
    return value;
  }

  static BigDecimal parseMoney(String line) {
    if (!MONEY.matcher(line).matches()) {
      throw new ValidationException(
          "Введите сумму в рублях, например 450 или 450.50 (не более двух знаков после запятой)");
    }
    return new BigDecimal(line.replace(',', '.'));
  }

  static boolean parseYesNo(String line) {
    String answer = line.toLowerCase(Locale.ROOT);
    if (YES_ANSWERS.contains(answer)) {
      return true;
    }
    if (NO_ANSWERS.contains(answer)) {
      return false;
    }
    throw new ValidationException("Ответьте «да» или «нет»");
  }
}
