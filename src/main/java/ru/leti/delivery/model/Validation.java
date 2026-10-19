package ru.leti.delivery.model;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import ru.leti.delivery.exception.ValidationException;

/** Общие проверки входных данных для классов модели. */
final class Validation {

  private static final int MAX_TEXT_LENGTH = 200;
  private static final int MIN_PHONE_DIGITS = 10;
  private static final int MAX_PHONE_DIGITS = 15;
  private static final Pattern PERSON_NAME = Pattern.compile("\\p{L}[\\p{L} .'-]*");
  private static final Pattern PHONE_CHARACTERS = Pattern.compile("\\+?[0-9 ()-]+");

  private Validation() {}

  /** Проверяет, что строка не пуста и не слишком длинна; возвращает её без крайних пробелов. */
  static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new ValidationException(fieldName + " не может быть пустым");
    }
    String trimmed = value.trim();
    if (trimmed.length() > MAX_TEXT_LENGTH) {
      throw new ValidationException(
          fieldName + " слишком длинное: не более " + MAX_TEXT_LENGTH + " символов");
    }
    return trimmed;
  }

  /** Имя человека: начинается с буквы, далее буквы, пробелы, точки, дефисы и апострофы. */
  static String requirePersonName(String value) {
    String name = requireText(value, "Имя");
    if (!PERSON_NAME.matcher(name).matches()) {
      throw new ValidationException("Имя должно начинаться с буквы и не содержать цифр и символов");
    }
    return name;
  }

  /** Телефон: необязательный «+», затем цифры, пробелы, скобки и дефисы; от 10 до 15 цифр. */
  static String requirePhone(String value) {
    String phone = requireText(value, "Телефон");
    long digits = phone.chars().filter(Character::isDigit).count();
    if (!PHONE_CHARACTERS.matcher(phone).matches()
        || digits < MIN_PHONE_DIGITS
        || digits > MAX_PHONE_DIGITS) {
      throw new ValidationException(
          "Телефон указан неверно: ожидается от "
              + MIN_PHONE_DIGITS
              + " до "
              + MAX_PHONE_DIGITS
              + " цифр, например +7 (999) 123-45-67");
    }
    return phone;
  }

  static int requirePositive(int value, String fieldName) {
    if (value <= 0) {
      throw new ValidationException(fieldName + " должно быть положительным числом");
    }
    return value;
  }

  static BigDecimal requireNonNegativeMoney(BigDecimal value, String fieldName) {
    if (value == null) {
      throw new ValidationException(fieldName + " не задана");
    }
    if (value.signum() < 0) {
      throw new ValidationException(fieldName + " не может быть отрицательной");
    }
    return value;
  }
}
