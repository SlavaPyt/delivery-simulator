package ru.leti.delivery.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ru.leti.delivery.exception.ValidationException;
import ru.leti.delivery.model.Courier;
import ru.leti.delivery.model.Courier.CourierStatus;

/** Реестр курьеров службы доставки. Данные хранятся только в памяти. */
public class CourierService {

  private final List<Courier> couriers = new ArrayList<>();

  public Courier addCourier(String name, String phone) {
    Courier courier = new Courier(name, phone);
    couriers.add(courier);
    return courier;
  }

  public List<Courier> getAllCouriers() {
    return Collections.unmodifiableList(couriers);
  }

  public List<Courier> getAvailableCouriers() {
    return couriers.stream().filter(Courier::isAvailable).toList();
  }

  public void changeStatus(Courier courier, CourierStatus status) {
    if (courier == null) {
      throw new ValidationException("Курьер не указан");
    }
    courier.setStatus(status);
  }
}
