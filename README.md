# Aurora

**Aurora** — это телеграм-бот для организации Random Coffee встреч, оснащённый модулями техподдержки, сбора статистики, рассылки сообщений и алгоритмом сопоставления анкет на базе Apache Lucene.

## Содержание

- [Установка](#установка)
- [Конфигурация](#конфигурация)
- [Запуск](#запуск)
- [Альтернативный запуск с Docker](#альтернативный-запуск-с-docker)
- [Команды Telegram-Бота](#команды-telegram-бота)
- [Автоматические скрипты](#автоматические-скрипты)
- [Структура проекта](#структура-проекта)
- [CI](#ci)
- [Лицензия](#лицензия)
- [Контакты](#контакты)

## Установка

Для корректной работы Aurora вам понадобятся следующие компоненты:

- [Java 21](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
- [Maven 3.9.8](https://maven.apache.org/download.cgi)
- [PostgreSQL](https://www.postgresql.org/download/)

## Конфигурация

Перед запуском приложения настройте переменные среды в файле `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/YOUR_DB_NAME
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD

# Telegram API Configuration
telegram.bot.name=YOUR_BOT_NAME
telegram.bot.token=YOUR_BOT_TOKEN

# Special User ID
special.user.id=YOUR_SPECIAL_USER_ID
```

Замените `YOUR_DB_NAME`, `YOUR_DB_USERNAME`, `YOUR_DB_PASSWORD`, `YOUR_BOT_NAME`, `YOUR_BOT_TOKEN` на соответствующие значения. `YOUR_SPECIAL_USER_ID` должен быть ID человека, который будет добавлен в выборку при нечётном количестве анкет, чтобы количество всегда было чётным и у всех была пара.

## Запуск

Для запуска проекта выполните следующие шаги:

1. Склонируйте репозиторий:

    ```bash
    git clone https://github.com/Daniil-Tiunchyk/Aurora-Telegram-Bot
    cd aurora-telegram-bot
    ```

2. Установите зависимости и соберите проект:

    ```bash
    mvn clean install
    ```

3. Запустите приложение:

    ```bash
    mvn spring-boot:run
    ```

## Альтернативный запуск с Docker

Для удобства развертывания проект также включает Dockerfile. Убедитесь, что вы настроили переменные среды в `application.properties` перед созданием Docker-образа.

1. Постройте Docker-образ:

    ```bash
    docker build -t aurora-telegram-bot .
    ```

2. Запустите контейнер:

    ```bash
    docker run -d aurora-telegram-bot
    ```

## Команды Telegram-Бота

**Команды пользователя:**

- **`/start`**: Инициализация работы с ботом и начало взаимодействия.
- **`/restart`**: Перезапуск процесса заполнения анкеты.
- **`/profile`**: Просмотр текущей анкеты пользователем.
- **`/help`**: Получение справочной информации о функционале бота и доступных командах.
- **`/support`**: Отправка запроса в техническую поддержку.

**Команды администратора:**

- **`/admin`**: Отображение всех доступных команд с кратким описанием.
- **`/list_admins`**: Получение списка администраторов.
- **`/promote`**: Повышение пользователя до администратора.

## Автоматические скрипты

Aurora использует три ключевых скрипта, работающих по расписанию, для автоматизации задач:

### ProfileMatchingTask

Скрипт `ProfileMatchingTask.java` отвечает за подбор пользователей на основе их анкет. Алгоритм:

1. Извлечение анкет из базы данных.
2. Векторизация анкет с использованием Apache Lucene.
3. Сравнение векторов и сортировка анкет.
4. Сопоставление пользователей с наибольшим совпадением интересов.
5. Случайное распределение оставшихся анкет.
6. Соединение оставшейся анкеты с администратором, если количество анкет нечётное.

### DailyMessageTask

Скрипт `DailyMessageTask.java` ежедневно отправляет сообщения всем пользователям. Алгоритм:

1. Заранее подготовленные сообщения помещаются в таблицу `DailyMessage`.
2. В заданное время скрипт проверяет наличие неотправленных сообщений.
3. Рассылка сообщений всем пользователям.

### ProfileStatisticsTask

Скрипт `ProfileStatisticsTask.java` собирает ежедневную статистику использования бота. Алгоритм:

1. Подсчёт общего количества пользователей.
2. Подсчёт количества анкет в базе данных.
3. Сохранение собранных данных для анализа.

## Структура проекта

```plaintext
aurora-telegram-bot/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/
│   │   │       └── example/
│   │   │           ├── commands/
│   │   │           │   ├── AdminCommand.java
│   │   │           │   ├── AdminsListCommand.java
│   │   │           │   ├── HelpCommand.java
│   │   │           │   ├── ProfileCommand.java
│   │   │           │   ├── PromoteCommand.java
│   │   │           │   ├── RestartCommand.java
│   │   │           │   ├── StartCommand.java
│   │   │           │   └── SupportCommand.java
│   │   │           ├── enums/
│   │   │           │   └── DialogMode.java
│   │   │           ├── interfaces/
│   │   │           │   └── BotCommandHandler.java
│   │   │           ├── models/
│   │   │           │   ├── SupportRequest.java
│   │   │           │   └── UserInfo.java
│   │   │           ├── modules/
│   │   │           │   ├── profile_matching/
│   │   │           │   │   ├── ProfileMatchingTask.java
│   │   │           │   │   └── TextSimilarity.java
│   │   │           │   ├── regular_messages/
│   │   │           │   │   ├── DailyMessage.java
│   │   │           │   │   ├── DailyMessageRepository.java
│   │   │           │   │   ├── DailyMessageService.java
│   │   │           │   │   └── DailyMessageTask.java
│   │   │           │   ├── statistics/
│   │   │           │   │   ├── ProfileStatistics.java
│   │   │           │   │   ├── ProfileStatisticsRepository.java
│   │   │           │   │   ├── ProfileStatisticsService.java
│   │   │           │   │   └── ProfileStatisticsTask.java
│   │   │           ├── repositories/
│   │   │           │   ├── SupportRequestRepository.java
│   │   │           │   └── UserInfoRepository.java
│   │   │           ├── services/
│   │   │           │   ├── SupportRequestService.java
│   │   │           │   └── UserInfoService.java
│   │   │           ├── AuroraBotApplication.java
│   │   │           ├── AuroraBot.java
│   │   │           └── MultiSessionTelegramBot.java
│   │   ├── resources/
│   │   │   ├── images/
│   │   │   │   └── name.jpg
│   │   │   ├── messages/
│   │   │   │   ├── help.txt
│   │   │   │   ├── info.txt
│   │   │   │   └── start.txt
│   │   │   └── application.properties
├── pom.xml
```

## CI

Проект включает в себя настройку CI, которая находится в файле `Aurora-Telegram-Bot/.github/workflows/java-ci.yml`. Этот файл конфигурирует автоматические проверки и сборку проекта с использованием GitHub Actions.

## Лицензия

Этот проект распространяется под лицензией Apache License 2.0. Подробности можно найти в файле `LICENSE`.

## Контакты

1. Telegram: [https://t.me/yet_another_name](https://t.me/yet_another_name)
2. LinkedIn: [https://www.linkedin.com/in/daniil-tiunchyk/](https://www.linkedin.com/in/daniil-tiunchyk/)
3. Gmail: [fcad.td@gmail.com](mailto:fcad.td@gmail.com)
