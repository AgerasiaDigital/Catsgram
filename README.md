# 🐱 Catsgram

Социальная сеть для публикации фотографий котиков. REST API на Spring Boot с хранением данных в PostgreSQL.

---

## Стек технологий

- **Java 21**
- **Spring Boot 3.2.2** (Web, JDBC)
- **PostgreSQL 16**
- **Lombok**
- **Docker / Docker Compose**
- **Maven** (+ Checkstyle)

---

## Структура проекта

```
src/main/java/ru/yandex/practicum/catsgram/
├── controller/       # REST-контроллеры и обработчик ошибок
├── dal/              # Репозитории (BaseRepository + конкретные)
│   └── mappers/      # RowMapper-ы для маппинга строк БД
├── dto/              # DTO для запросов и ответов
├── exception/        # Кастомные исключения
├── mapper/           # Маппер между моделями и DTO
├── model/            # Доменные модели
└── service/          # Бизнес-логика
```

---

## API

### Пользователи `/users`

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/users` | Получить всех пользователей |
| `GET` | `/users/{userId}` | Получить пользователя по ID |
| `POST` | `/users` | Создать пользователя |

**Тело запроса `POST /users`:**
```json
{
  "username": "ivan",
  "email": "ivan@example.com",
  "password": "secret"
}
```

---

### Посты `/posts`

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/posts` | Получить список постов |
| `GET` | `/posts/{postId}` | Получить пост по ID |
| `POST` | `/posts` | Создать пост |
| `PUT` | `/posts` | Обновить пост |

**Query-параметры `GET /posts`:**

| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `from` | `0` | Смещение выборки |
| `size` | `10` | Размер выборки |
| `sort` | `desc` | Порядок сортировки (`asc` / `desc`) |

---

### Изображения

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/posts/{postId}/images` | Получить изображения поста |
| `POST` | `/posts/{postId}/images` | Загрузить изображения (multipart) |
| `GET` | `/images/{imageId}` | Скачать изображение |

---

## Запуск

### Требования

- Docker и Docker Compose
- Java 21 + Maven (для локальной сборки)

### 1. Запуск базы данных

```bash
cd testcompose
docker compose up -d
```

Это поднимет PostgreSQL и автоматически создаст схему БД (таблицы `users`, `posts`, `image_storage`).

### 2. Сборка приложения

```bash
mvn clean package -DskipTests
```

### 3. Запуск приложения

```bash
java -jar target/catsgram-1.0-SNAPSHOT.jar
```

Приложение будет доступно на `http://localhost:8080`.

### 4. Запуск через Docker (многоэтапная сборка)

```bash
mvn clean package -DskipTests
docker build -t catsgram .
docker run -p 8080:8080 catsgram
```

---

## Конфигурация

Настройки задаются в `src/main/resources/application.properties`:

```properties
server.port=8080

# Директория для хранения загружаемых изображений
catsgram.image-directory=C:/catsgram/images

# Лимиты на размер загружаемых файлов
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# База данных
spring.datasource.url=jdbc:postgresql://localhost:5432/catsgram
spring.datasource.username=dbuser
spring.datasource.password=12345
```

---

## Схема базы данных

```sql
users (
  id                BIGINT PRIMARY KEY,
  username          VARCHAR(40),
  email             VARCHAR(255),
  password          VARCHAR(40),
  registration_date TIMESTAMP WITH TIME ZONE
)

posts (
  id          BIGINT PRIMARY KEY,
  author_id   BIGINT REFERENCES users(id),
  description TEXT,
  post_date   TIMESTAMP WITH TIME ZONE
)

image_storage (
  id            BIGINT PRIMARY KEY,
  original_name VARCHAR(255),
  file_path     VARCHAR(1024),
  post_id       BIGINT REFERENCES posts(id)
)
```

---

## Обработка ошибок

Все ошибки возвращаются в формате:

```json
{
  "error": "Описание ошибки"
}
```

| Исключение | HTTP-статус |
|------------|-------------|
| `NotFoundException` | `404 Not Found` |
| `DuplicatedDataException` | `409 Conflict` |
| `ConditionsNotMetException` | `422 Unprocessable Entity` |
| `ParameterNotValidException` | `400 Bad Request` |
| Прочие | `500 Internal Server Error` |
