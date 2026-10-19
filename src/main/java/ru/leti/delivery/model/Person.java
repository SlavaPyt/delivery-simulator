package ru.leti.delivery.model;

/** Человек, участвующий в доставке: у него есть имя и контактный телефон. */
public abstract class Person {

  private String name;
  private String phone;

  protected Person(String name, String phone) {
    this.name = Validation.requirePersonName(name);
    this.phone = Validation.requirePhone(phone);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = Validation.requirePersonName(name);
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = Validation.requirePhone(phone);
  }
}
