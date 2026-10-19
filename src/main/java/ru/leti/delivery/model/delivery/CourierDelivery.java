package ru.leti.delivery.model.delivery;

import ru.leti.delivery.model.DeliveryMethod;
import ru.leti.delivery.model.OrderStatus;

/** Общее поведение способов доставки, при которых заказ привозит курьер по адресу клиента. */
abstract class CourierDelivery implements DeliveryMethod {

  @Override
  public boolean requiresCourier() {
    return true;
  }

  @Override
  public OrderStatus getDispatchStatus() {
    return OrderStatus.IN_DELIVERY;
  }

  @Override
  public String describeDestination(String customerAddress) {
    return customerAddress;
  }

  @Override
  public String toString() {
    return getTitle();
  }
}
