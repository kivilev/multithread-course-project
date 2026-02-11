# Order Payment System

Шаблон для ДЗ по асинхронной оплате заказов (AsyncRunner 2049). Содержит общий модуль с DTO, оркестратор заказов и стабы внешних сервисов.

## Архитектура
- `common-libs` — общие модели/DTO для платежного шлюза и ценового сервиса.
- `order-orchestrator` — API для заказов + каркас оркестрации платежа (бизнес-логика пишется студентами).
- `payment-stub` — HTTP-стабы платежного шлюза и пересчета цен с настраиваемым поведением.
- `infra/docker-compose.dev.yaml` — общий compose для Postgres + стабы + оркестратор.
- `order-orchestrator/docker-compose.dev.yaml` — compose для Postgres + стабы при запуске через Spring Docker Compose plugin.

## Требования
- Docker Desktop (Compose v2).
- JDK 21 нужен только для локального запуска без Docker.

## Быстрый старт (Docker, Windows и macOS)
1. Убедись, что Docker Desktop запущен.
2. Запусти весь стек:
   ```bash
   docker compose -f infra/docker-compose.dev.yaml up --build
   ```
3. По умолчанию: оркестратор на `http://localhost:8080`, стаб на `http://localhost:8081`, Postgres на `localhost:5432`.

## Сборка образов вручную (необязательно)
macOS/Linux:
```bash
bash ./infra/build-stub-image.dev.sh
bash ./infra/build-orchestrator-image.dev.sh
```

Windows PowerShell:
```powershell
.\infra\build-stub-image.dev.ps1
.\infra\build-orchestrator-image.dev.ps1
```

Если политика PowerShell блокирует запуск скриптов:
```powershell
powershell -ExecutionPolicy Bypass -File .\infra\build-stub-image.dev.ps1
```

## Локальный запуск сервисов без Docker
macOS/Linux:
```bash
./gradlew :order-orchestrator:bootRun
./gradlew :payment-stub:bootRun
```

Windows PowerShell:
```powershell
.\gradlew.bat :order-orchestrator:bootRun
.\gradlew.bat :payment-stub:bootRun
```

Для локального запуска оркестратора можно поднять только инфраструктуру:
```bash
docker compose -f order-orchestrator/docker-compose.dev.yaml up --build
```

## Swagger/UI
- Оркестратор (если включен SpringDoc): `http://localhost:8080/swagger-ui/index.html`
- Payment Stub: `http://localhost:8081/swagger-ui/index.html`

## Compose команды
- Старт общего стека (с пересборкой):
  ```bash
  docker compose -f infra/docker-compose.dev.yaml up --build
  ```
- Остановка с удалением контейнеров/сетей (тома остаются):
  ```bash
  docker compose -f infra/docker-compose.dev.yaml down
  ```
- Полное удаление с томами:
  ```bash
  docker compose -f infra/docker-compose.dev.yaml down -v
  ```
- Просмотр логов:
  ```bash
  docker compose -f infra/docker-compose.dev.yaml logs -f
  ```
