package ru.leti.delivery.model;

/** Клиент службы доставки. */
public class Customer extends Person {

  public Customer(String name, String phone) {
    super(name, phone);
  }

  @Override
  public String toString() {
    return getName() + ", тел. " + getPhone();
  }
}
