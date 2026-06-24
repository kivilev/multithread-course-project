package dev.sorokin.domain;

import dev.sorokin.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus implements EnumUtils.IntEnum {
    NEW(0), //заказ только создан, оплата ещё не запускалась;
    AUTHORIZED(1), // платеж прошел авторизацию
    SUCCEED_PAID(2), // заказ успешно оплачен (capture прошёл).

    AUTHORIZATION_FAILED(101), // авторизация карты не прошла (банк/шлюз отказал, денег не хватило и т.п.);
    PRICE_CHANGED_FAILED(102),// финальная сумма после пересчёта склада оказалась больше, чем авторизованная, мы решили не списывать;
    CAPTURE_FAILED(103); // проблема с оплатой

    private final int id;

    @Override
    public int getCode() {
        return id;
    }
}
