# Запуск проекта

Пошаговое руководство по запуску **ysci-java-project** (социальная платформа на Spring Boot 3.2 / Java 17) на Linux, macOS и Windows.

**English version:** [RUNNING.md](RUNNING.md)

---

## Обзор

Рекомендуемый способ запуска — **Docker Compose** через обёрточные скрипты. Они поднимают четыре сервиса:

| Сервис | Назначение | Порт на хосте |
|--------|------------|---------------|
| **nginx** | Обратный прокси (HTTP + WebSocket) | **8081** |
| **app** | Spring Boot приложение | внутренний 8080 |
| **mysql** | База данных MySQL 8 | внутренний 3306 |
| **redis** | Кэш и ограничение частоты запросов | внутренний 6379 |

**URL после запуска:**

- Приложение: [http://localhost:8081](http://localhost:8081)
- Проверка GCS: [http://localhost:8081/api/health/gcs](http://localhost:8081/api/health/gcs)

**Скрипты запуска по платформам:**

| Платформа | Скрипт | Примечание |
|-----------|--------|------------|
| Linux / macOS | `./run.sh` | Bash-скрипт |
| Windows PowerShell | `.\run.ps1` | Рекомендуется на Windows |
| Windows CMD | `run.cmd` | Делегирует выполнение в `run.ps1` |

При старте скрипты автоматически:

1. Проверяют наличие Docker и `docker compose`
2. Находят учётные данные Google Cloud Application Default Credentials (ADC)
3. Генерируют или обновляют `.env` на основе значений из `.env.example`
4. Запускают `docker compose up -d --build`
5. Ожидают ответ health-endpoint с доступным GCS

---

## Требования

Установите **до первого запуска**:

| Компонент | Нужен для | Установка |
|-----------|-----------|-----------|
| **Docker** + **Compose v2** | Все платформы | [Docker Desktop](https://www.docker.com/products/docker-desktop/) или [Docker Engine](https://docs.docker.com/engine/install/) |
| **Google Cloud SDK** (`gcloud`) | Загрузка файлов в GCS (включено по умолчанию) | [Установка gcloud](https://cloud.google.com/sdk/docs/install) |
| **Git** | Клонирование репозитория | [git-scm.com](https://git-scm.com/) |
| **Ключ OpenRouter API** | AI-ассистент (опционально) | [openrouter.ai/keys](https://openrouter.ai/keys) — см. [AI_ASSISTANT_SETUP.md](AI_ASSISTANT_SETUP.md) |

**Опционально (локальная разработка без Docker):**

- Java 17 (JDK)
- Redis на `localhost:6379`

---

## Однократная настройка GCS

Google Cloud Storage **включён по умолчанию** (`GCS_ENABLED=true`). Для успешного старта приложению нужны валидные учётные данные.

### 1. Установка и аутентификация gcloud

Выполните один раз на своей машине:

```bash
gcloud auth login
gcloud auth application-default login
gcloud config set project project-a4b9287b-070f-4f18-b48
```

Замените ID проекта, если используете другой GCS-проект (см. `GCS_PROJECT_ID` в `.env.example`).

### 2. Расположение файла ADC по платформам

Скрипты запуска автоматически определяют файл Application Default Credentials:

| Платформа | Путь по умолчанию |
|-----------|-------------------|
| Linux / macOS | `~/.config/gcloud/application_default_credentials.json` |
| Windows | `%APPDATA%\gcloud\application_default_credentials.json` |

Для указания своего пути задайте переменную `GCS_CREDENTIALS_PATH` перед запуском скрипта.

### 3. Проверка доступа к bucket (опционально)

```bash
gcloud storage ls gs://java_project_bucket/
```

Дополнительные варианты (JSON сервисного аккаунта, GKE) — в [GCS_SETUP.md](GCS_SETUP.md).

---

## Быстрый старт по платформам

### Linux

#### Шаг 1 — Установка Docker

Следуйте официальному руководству для вашего дистрибутива:

- [Установка Docker Engine на Linux](https://docs.docker.com/engine/install/)

Убедитесь, что пользователь может запускать Docker без `sudo` (добавьте пользователя в группу `docker`), затем перезайдите в сессию.

Проверка:

```bash
docker --version
docker compose version
```

#### Шаг 2 — Установка Google Cloud SDK

```bash
# Пример для Debian/Ubuntu — для других дистрибутивов см. официальную документацию
curl https://sdk.cloud.google.com | bash
exec -l $SHELL
gcloud init
```

Выполните [однократную настройку GCS](#однократная-настройка-gcs) выше.

#### Шаг 3 — Клонирование и переход в проект

```bash
git clone <repository-url> ysci-java-project
cd ysci-java-project
```

#### Шаг 4 — Права на выполнение скрипта

```bash
chmod +x run.sh
```

#### Шаг 5 — (Опционально) Настройка окружения

Скопируйте пример, если хотите задать значения до первого запуска:

```bash
cp .env.example .env
# Отредактируйте .env — добавьте OPENROUTER_API_KEY для AI-функций
```

Скрипт пересоздаст `.env` при старте, сохраняя ваши значения где возможно.

#### Шаг 6 — Запуск проекта

```bash
./run.sh
```

Явная команда:

```bash
./run.sh up
```

Дождитесь сообщения `Project is running.` и JSON health с `"available": true`.

#### Шаг 7 — Открытие приложения

Откройте [http://localhost:8081](http://localhost:8081) в браузере. Зарегистрируйте пользователя на `/register`.

---

### macOS

#### Шаг 1 — Установка Docker Desktop

Скачайте и установите [Docker Desktop for Mac](https://www.docker.com/products/docker-desktop/).

Запустите Docker Desktop и дождитесь статуса «Docker is running».

Проверка в Terminal:

```bash
docker --version
docker compose version
```

#### Шаг 2 — Установка Google Cloud SDK

**Вариант A — Homebrew:**

```bash
brew install --cask google-cloud-sdk
gcloud init
```

**Вариант B — Официальный установщик:**

Скачайте с [cloud.google.com/sdk/docs/install](https://cloud.google.com/sdk/docs/install).

Выполните [однократную настройку GCS](#однократная-настройка-gcs) выше.

#### Шаг 3 — Клонирование и переход в проект

```bash
git clone <repository-url> ysci-java-project
cd ysci-java-project
```

#### Шаг 4 — Права на выполнение скрипта

```bash
chmod +x run.sh
```

#### Шаг 5 — (Опционально) Настройка окружения

```bash
cp .env.example .env
# Отредактируйте .env — добавьте OPENROUTER_API_KEY для AI-функций
```

#### Шаг 6 — Запуск проекта

```bash
./run.sh
```

#### Шаг 7 — Открытие приложения

Откройте [http://localhost:8081](http://localhost:8081) в браузере.

---

### Windows

#### Шаг 1 — Установка Docker Desktop

Скачайте [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/).

- При запросе включите бэкенд **WSL 2** (рекомендуется)
- Запустите Docker Desktop и дождитесь его готовности

Проверка в PowerShell:

```powershell
docker --version
docker compose version
```

#### Шаг 2 — Установка Google Cloud SDK

Скачайте установщик с [cloud.google.com/sdk/docs/install](https://cloud.google.com/sdk/docs/install).

В PowerShell или CMD:

```powershell
gcloud auth login
gcloud auth application-default login
gcloud config set project project-a4b9287b-070f-4f18-b48
```

Файл ADC будет создан по пути:

```
%APPDATA%\gcloud\application_default_credentials.json
```

#### Шаг 3 — Клонирование и переход в проект

```powershell
git clone <repository-url> ysci-java-project
cd ysci-java-project
```

#### Шаг 4 — (Опционально) Настройка окружения

```powershell
copy .env.example .env
# Отредактируйте .env — добавьте OPENROUTER_API_KEY для AI-функций
```

#### Шаг 5 — Запуск проекта

**PowerShell (рекомендуется):**

```powershell
.\run.ps1
```

**Командная строка (CMD):**

```cmd
run.cmd
```

Или явно:

```powershell
.\run.ps1 up
```

```cmd
run.cmd up
```

> **Примечание:** `run.cmd` вызывает PowerShell с `-ExecutionPolicy Bypass`, поэтому менять политику выполнения вручную не нужно.

#### Шаг 6 — Открытие приложения

Откройте [http://localhost:8081](http://localhost:8081) в браузере.

---

## Переменные окружения

Переменные описаны в [`.env.example`](.env.example). Скрипты запуска **пересоздают `.env` при каждом старте**, сохраняя существующие значения из файла или окружения.

| Переменная | Значение по умолчанию | Описание |
|------------|----------------------|----------|
| `GCS_CREDENTIALS_PATH` | Автоопределение пути ADC | Путь к JSON Application Default Credentials |
| `GCS_ENABLED` | `true` | Включить Google Cloud Storage для загрузки медиа |
| `GCS_BUCKET` | `java_project_bucket` | Имя GCS bucket |
| `GCS_PROJECT_ID` | `project-a4b9287b-070f-4f18-b48` | ID проекта Google Cloud |
| `JWT_SECRET` | `dev-docker-jwt-secret-change-in-production-min-256-bits!!` | Секрет для JWT (смените в production) |
| `OPENROUTER_API_KEY` | *(пусто)* | Ключ API для AI-ассистента — см. [AI_ASSISTANT_SETUP.md](AI_ASSISTANT_SETUP.md) |
| `OPENROUTER_MODEL` | `google/gemma-2-9b-it:free` | Имя модели OpenRouter |

Переопределение перед стартом (Linux/macOS):

```bash
export OPENROUTER_API_KEY=your_key_here
./run.sh
```

Windows PowerShell:

```powershell
$env:OPENROUTER_API_KEY = "your_key_here"
.\run.ps1
```

---

## Ежедневные команды

| Действие | Linux / macOS | Windows PowerShell | Windows CMD |
|----------|---------------|--------------------|-------------|
| Запуск (по умолчанию) | `./run.sh` | `.\run.ps1` | `run.cmd` |
| Запуск | `./run.sh up` | `.\run.ps1 up` | `run.cmd up` |
| Остановка | `./run.sh stop` | `.\run.ps1 stop` | `run.cmd stop` |
| Перезапуск | `./run.sh restart` | `.\run.ps1 restart` | `run.cmd restart` |
| Статус | `./run.sh status` | `.\run.ps1 status` | `run.cmd status` |
| Логи (app) | `./run.sh logs` | `.\run.ps1 logs` | `run.cmd logs` |
| Справка | `./run.sh help` | `.\run.ps1 help` | `run.cmd help` |

Нажмите `Ctrl+C` для выхода из режима просмотра логов.

---

## Чеклист проверки

После успешного выполнения `./run.sh` или `.\run.ps1`:

### 1. Health-endpoint

```bash
curl http://localhost:8081/api/health/gcs
```

Ожидаемый ответ (поля могут отличаться):

```json
{
  "available": true,
  "bucket": "java_project_bucket",
  "bucketAccessible": true,
  "projectId": "project-a4b9287b-070f-4f18-b48",
  "message": "GCS ready - uploads will go to Google Cloud Storage"
}
```

### 2. Веб-интерфейс

- Откройте [http://localhost:8081](http://localhost:8081)
- Перейдите на `/register` и создайте аккаунт
- Войдите и убедитесь, что лента загружается

### 3. Статус контейнеров

```bash
./run.sh status
```

Все сервисы (`app`, `nginx`, `mysql`, `redis`) должны быть в состоянии running.

### 4. AI-ассистент (опционально)

Если задан `OPENROUTER_API_KEY`, откройте `/assistant` и отправьте тестовое сообщение. Без ключа приложение работает в режиме local fallback с заглушкой.

---

## Устранение неполадок

### Docker не установлен / демон не запущен

**Симптом:** `[ERROR] Docker is not installed.` или `Docker daemon is not running.`

**Решение:**

- Запустите Docker Desktop (macOS / Windows) или сервис Docker (Linux)
- Проверьте: `docker info`

### Файл учётных данных GCS не найден

**Симптом:** `[ERROR] GCS credentials file not found: ...`

**Решение:**

```bash
gcloud auth login
gcloud auth application-default login
gcloud config set project project-a4b9287b-070f-4f18-b48
```

Затем снова запустите скрипт.

### Путь к учётным данным GCS — это каталог

**Симптом:** `GCS credentials path is a directory (broken Docker mount)`

Возникает, когда Docker создал каталог вместо монтирования файла учётных данных.

**Решение на Linux/macOS:**

```bash
docker run --rm -v "${HOME}/.config/gcloud:/gcloud" alpine rm -rf /gcloud/application_default_credentials.json
gcloud auth application-default login
```

**Решение на Windows (PowerShell):**

```powershell
Remove-Item -Recurse -Force "$env:APPDATA\gcloud\application_default_credentials.json"
gcloud auth application-default login
```

### Приложение запущено, но GCS недоступен

**Симптом:** Health возвращает `"available": false` или старт завершается с предупреждением GCS.

**Решение:**

1. Проверьте логи: `./run.sh logs` или `.\run.ps1 logs`
2. Убедитесь, что bucket существует и доступен: `gcloud storage ls gs://java_project_bucket/`
3. Проверьте `GCS_PROJECT_ID` и `GCS_BUCKET` в `.env`

### Загруженные изображения возвращают 403

**Симптом:** Изображения загружаются, но не отображаются в браузере.

**Решение:** Сделайте bucket публично читаемым (только для разработки):

```bash
gcloud storage buckets add-iam-policy-binding gs://java_project_bucket \
  --member=allUsers --role=roles/storage.objectViewer
```

### Порт 8081 уже занят

**Симптом:** nginx не может привязать порт 8081.

**Решение:**

1. Найдите и остановите процесс, занимающий порт
2. Или измените порт хоста в `docker-compose.yml` в секции `nginx.ports` (например, `"8082:80"`) и используйте `http://localhost:8082`

### AI не отвечает / сообщение fallback

**Симптом:** Ассистент сообщает о режиме «local fallback mode».

**Решение:** Задайте `OPENROUTER_API_KEY` в `.env` или окружении, затем перезапустите:

```bash
./run.sh restart
```

Подробности — в [AI_ASSISTANT_SETUP.md](AI_ASSISTANT_SETUP.md).

### Нет доступа к bucket (только предупреждение)

**Симптом:** `[WARN] Cannot access bucket gs://java_project_bucket/`

Приложение может стартовать, если учётные данные валидны внутри контейнера. Настройте IAM-права вашего Google-аккаунта на bucket, если загрузки не работают.

---

## Альтернатива: локальная разработка без Docker

Для быстрой разработки бэкенда без MySQL/GCS/nginx:

### Требования

- Java 17 JDK
- Redis на `localhost:6379`

### Запуск

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Профиль `local` ([`application-local.yml`](src/main/resources/application-local.yml)) использует:

- **H2 in-memory** (режим совместимости с MySQL) вместо MySQL
- **Локальный Redis** на порту 6379
- Миграции Flyway **отключены** (схему управляет Hibernate)

Приложение доступно на [http://localhost:8080](http://localhost:8080) (без nginx).

### Ограничения

| Функция | Режим Docker | Локальный режим |
|---------|--------------|-----------------|
| MySQL + миграции Flyway | Да | Нет (H2 in-memory) |
| Загрузка в GCS | Да (по умолчанию) | Отключена без настройки |
| nginx reverse proxy | Да | Нет |
| WebSocket через nginx | Да | Напрямую к app |
| Production-подобная среда | Да | Нет |

Переменные окружения для локального запуска:

```bash
export JWT_SECRET=change-me-to-a-very-long-secret-key-at-least-256-bits-long-for-dev
export OPENROUTER_API_KEY=your_key_here   # опционально
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

---

## Связанная документация

- [GCS_SETUP.md](GCS_SETUP.md) — учётные данные GCS, сервисные аккаунты, устранение неполадок
- [AI_ASSISTANT_SETUP.md](AI_ASSISTANT_SETUP.md) — OpenRouter API, AI-функции, лимиты
- [.env.example](.env.example) — шаблон переменных окружения
