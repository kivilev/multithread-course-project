# order-orchestrator

Сервис заказов и асинхронная оркестрация оплаты. При создании заказа в БД появляется задача; фоновый poller забирает её и выполняет цепочку вызовов к payment/warehouse стабам.

## API

### `POST /order`

Создаёт заказ и задачу на оплату. Возвращает `201 Created` с телом `OrderDto`.

**Request** (`OrderCreateRequestDto`):

| Поле | Тип | Описание |
|------|-----|----------|
| `address` | string | Адрес доставки |
| `clientEstimate` | decimal | Оценочная сумма от клиента |
| `customerId` | long | Идентификатор покупателя |

**Response** (`OrderDto`): `id`, `address`, `customerId`, `paymentStatus`, `clientEstimate`, `finalAmount`, `authorizedAmount`, `capturedAmount`, `failureReason`, `failureCode`.

### `GET /order/{id}`

Возвращает заказ по UUID или `404 Not Found`.

## Статусы оплаты (`paymentStatus`)

| Статус | Описание |
|--------|----------|
| `NEW` | Заказ создан, оплата ещё не запускалась |
| `AUTHORIZED` | Промежуточный статус внутри обработки (холд прошёл) |
| `SUCCEED_PAID` | Capture успешен |
| `AUTHORIZATION_FAILED` | Банк/шлюз отклонил authorize |
| `PRICE_CHANGED_FAILED` | Финальная цена склада выше авторизованной суммы |
| `CAPTURE_FAILED` | Ошибка при списании |

## Оркестрация задач

Компоненты:

- **`TaskPoller`** — `@Scheduled` опрос БД, `SELECT ... FOR UPDATE SKIP LOCKED`, перевод задач в `IN_PROGRESS`.
- **`TaskDispatcher`** — асинхронный запуск в thread pool, обработка результата и retry.
- **`TaskProcessor`** — бизнес-логика шагов `AUTH → REPRICE → CAPTURE`.

Шаги задачи (`TaskStep`): `NEW` → `AUTH` → `REPRICE` → `CAPTURE`.

Статусы задачи (`TaskStatus`): `NEW`, `IN_PROGRESS`, `SUCCEEDED`, `FAILED_RETRYABLE`, `FAILED_NON_RETRYABLE`.

## Схема БД

**`orders`** — заказ и результат оплаты (`status`, суммы, `failure_reason`).

**`tasks`** — одна задача на заказ (`order_id` unique), поля `status`, `step`, `attempts`, `next_attempt_at` (retry), `locked_until` (lease), `result`.

Миграции: Liquibase, `classpath:/db/changelog/changelog-master.yaml`.

## Конфигурация (env)

| Переменная | Описание | По умолчанию |
|------------|----------|--------------|
| `SPRING_DATASOURCE_URL` | JDBC Postgres | `jdbc:postgresql://localhost:5432/orders` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Учётные данные БД | `user` / `pass` |
| `STUB_BASE_URL` | URL payment/warehouse стаба | `http://localhost:8081/` |
| `STUB_BASE_READ_TIMEOUT` | Read timeout HTTP-клиента | `5s` |
| `STUB_BASE_CONNECT_TIMEOUT` | Connect timeout HTTP-клиента | `5s` |
| `TASK_EXEC_POLL_INTERVAL_MS` | Интервал poller'а, мс | `10000` |
| `TASK_EXEC_POLL_BATCH_SIZE` | Размер батча poller'а | `10` |

Параметры dispatcher'а задаются в `application.yml` (`task-execution.dispatcher.*`): `retry-delay`, `max-attempts`, `thread-pool-size`.

## Сборка и запуск

```bash
# сборка jar
./gradlew :order-orchestrator:bootJar

# локальный запуск (нужны Postgres и payment-stub)
./gradlew :order-orchestrator:bootRun
```

## Docker

- Dockerfile в корне модуля.
- Инфраструктура для IDE: `order-orchestrator/docker-compose.dev.yaml` (Postgres + payment-stub).
- Полный стек: `infra/docker-compose.dev.yaml`.
