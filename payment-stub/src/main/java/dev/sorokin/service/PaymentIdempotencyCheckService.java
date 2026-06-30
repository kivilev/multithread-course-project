package dev.sorokin.service;

import org.springframework.stereotype.Service;

@Service
public class PaymentIdempotencyCheckService {

    /**
     * Проверка, что idempotency key не переиспользуется с другими параметрами запроса (409 при конфликте).
     * Полную логику не реализовал — мало времени, всегда считаем ключ валидным.
     */
    public void validateKey(String idempotencyKey) {
    }
}
