package ru.leti.delivery.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.leti.delivery.exception.ValidationException;
import ru.leti.delivery.model.Validation;

class ConsoleInputTest {

  private final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
  private final PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);

  private ConsoleInput inputOf(String text) {
    return new ConsoleInput(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), out);
  }

  private String output() {
    return outBytes.toString(StandardCharsets.UTF_8);
  }

  @Test
  void readsTrimmedLineAfterPrompt() {
    ConsoleInput input = inputOf("  Иван  \n");

    assertEquals("Иван", input.readLine("Имя"));
    assertTrue(output().startsWith("Имя: "));
  }

  @Test
  void repeatsPromptUntilValueIsValid() {
    ConsoleInput input = inputOf("\n123\nАнна\n");

    String name = input.read("Имя", Validation::requirePersonName);

    assertEquals("Анна", name);
    assertEquals(2, output().split("Ошибка:", -1).length - 1);
  }

  @Test
  void readsIntegerWithinRange() {
    ConsoleInput input = inputOf("abc\n10\n0\n3\n");

    assertEquals(3, input.readInt("Пункт", 1, 5));
    assertTrue(output().contains("Введите целое число от 1 до 5"));
    assertTrue(output().contains("Число должно быть от 1 до 5"));
  }

  @Test
  void readsPositivePriceWithCommaOrDot() {
    assertEquals(new BigDecimal("450.50"), inputOf("450,50\n").readPrice("Цена"));
    assertEquals(new BigDecimal("450"), inputOf("450\n").readPrice("Цена"));
  }

  @Test
  void rejectsZeroNegativeAndMalformedPrice() {
    ConsoleInput input = inputOf("0\n-5\n1e5\n10.999\nабв\n99.90\n");

    assertEquals(new BigDecimal("99.90"), input.readPrice("Цена"));
    assertEquals(5, output().split("Ошибка:", -1).length - 1);
  }

  @Test
  void confirmUnderstandsYesAndNoInEitherLanguage() {
    ConsoleInput input = inputOf("ДА\nнет\ny\nN\nвозможно\nд\n");

    assertTrue(input.confirm("Ещё?"));
    assertFalse(input.confirm("Ещё?"));
    assertTrue(input.confirm("Ещё?"));
    assertFalse(input.confirm("Ещё?"));
    assertTrue(input.confirm("Ещё?"));
    assertTrue(output().contains("Ответьте «да» или «нет»"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"отмена", "ОТМЕНА", " cancel "})
  void cancelCommandInterruptsOperation(String command) {
    ConsoleInput input = inputOf(command + "\n");

    assertThrows(
        OperationCancelledException.class, () -> input.read("Имя", Validation::requirePersonName));
  }

  @Test
  void endOfInputIsReportedAsClosedInput() {
    ConsoleInput input = inputOf("");

    assertThrows(InputClosedException.class, () -> input.readLine("Имя"));
  }

  @Test
  void endOfInputInsideRetryLoopStopsReading() {
    ConsoleInput input = inputOf("123\n");

    assertThrows(
        InputClosedException.class, () -> input.read("Имя", Validation::requirePersonName));
  }

  @Test
  void parseIntRejectsOverflowAndSymbols() {
    assertThrows(ValidationException.class, () -> ConsoleInput.parseInt("99999999999", 1, 10));
    assertThrows(ValidationException.class, () -> ConsoleInput.parseInt("1.5", 1, 10));
    assertThrows(ValidationException.class, () -> ConsoleInput.parseInt("", 1, 10));
    assertEquals(7, ConsoleInput.parseInt("+7", 1, 10));
  }

  @Test
  void echoesInputWhenEnabled() {
    ConsoleInput input =
        new ConsoleInput(
            new ByteArrayInputStream("Анна\n".getBytes(StandardCharsets.UTF_8)), out, true);

    input.readLine("Имя");

    assertEquals("Имя: Анна" + System.lineSeparator(), output());
  }
}
