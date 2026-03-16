# Kafka Streams: блокировка пользователей и цензура сообщений

Система выполняет две основные функции:

1. **Блокировка пользователей**  
   Пользователь может заблокировать другого пользователя.  
   Если получатель заблокировал отправителя, сообщение не должно попасть в итоговый выходной топик.

2. **Цензура запрещённых слов**  
   В системе есть список запрещённых слов, который можно динамически изменять.  
   Все входящие сообщения, которые не были отброшены по блокировке, проходят через цензуру.  
   Запрещённые слова в тексте заменяются символами `*`.

---

## Используемые технологии

- Java 11
- Apache Kafka
- Kafka Streams
- Docker Compose
- Kafka UI
- ksqlDB

---

## Инфраструктура проекта

Инфраструктура разворачивается через `docker-compose.yml` и включает:

- 3 Kafka broker в режиме KRaft
- Schema Registry
- Kafka UI
- ksqlDB server
- ksqlDB CLI
- сервис автоматического создания Kafka-топиков

---

## Kafka-топики проекта

В проекте используются следующие топики:

- `messages` — входящие сообщения пользователей
- `filtered_messages` — сообщения после фильтрации и цензуры
- `blocked_users` — события блокировки и разблокировки пользователей
- `banned_words` — события добавления и удаления запрещённых слов

---

## Логика работы приложения

### 1. Блокировка пользователей

В топик `blocked_users` отправляются события в формате:

```text
user|blockedUser|action
```

Примеры:

```text
bob|alice|BLOCK
bob|alice|UNBLOCK
```

Логика:
- `bob|alice|BLOCK` означает, что пользователь `bob` заблокировал пользователя `alice`
- `bob|alice|UNBLOCK` означает, что пользователь `bob` снял блокировку с `alice`

Список блокировок хранится в **persistent state store** Kafka Streams.

Если получатель заблокировал отправителя, сообщение отбрасывается и **не записывается** в `filtered_messages`.

---

### 2. Цензура запрещённых слов

В топик `banned_words` отправляются события в формате:

```text
word|action
```

Примеры:

```text
spam|ADD
spam|REMOVE
badword|ADD
```

Логика:
- `ADD` — добавить слово в список запрещённых
- `REMOVE` — удалить слово из списка запрещённых

Список запрещённых слов также хранится в **persistent state store** Kafka Streams.

Если слово активно в store, то при обработке сообщения оно маскируется звёздочками.

Пример:

Исходное сообщение:

```text
msg-3|charlie|bob|badword should be hidden
```

После цензуры:

```text
msg-3|charlie|bob|******* should be hidden
```

---

## Формат сообщений

### Сообщения пользователей (`messages`)

Формат:

```text
messageId|sender|receiver|text
```

Пример:

```text
msg-1|alice|bob|hello bob
```

Описание полей:
- `messageId` — идентификатор сообщения
- `sender` — отправитель
- `receiver` — получатель
- `text` — текст сообщения

---

### События блокировки (`blocked_users`)

Формат:

```text
user|blockedUser|action
```

Пример:

```text
bob|alice|BLOCK
```

---

### События запрещённых слов (`banned_words`)

Формат:

```text
word|action
```

Пример:

```text
badword|ADD
```

---

## Структура решения

Для выполнения задания используются следующие классы.

### Основной класс приложения

#### `ModerationStreamsApp`
Главный класс Kafka Streams приложения.

Он выполняет:
- чтение событий блокировки из `blocked_users`
- чтение событий запрещённых слов из `banned_words`
- чтение входящих сообщений из `messages`
- фильтрацию сообщений по списку блокировок
- цензуру текста сообщений
- запись результата в `filtered_messages`

Этот класс является **основным классом запуска для задания**.

---

### Конфигурация

#### `AppConfig`
Содержит:
- адреса bootstrap servers
- названия Kafka-топиков
- имя Kafka Streams приложения
- имена state store

---

### Модели данных

#### `ChatMessage`
Модель сообщения пользователя.

Используется для:
- чтения входящих строк из топика `messages`
- преобразования строки в объект
- обратной сборки объекта в строку

---

#### `BlockEvent`
Модель события блокировки пользователя.

Используется для:
- чтения событий из топика `blocked_users`
- определения, нужно добавить или удалить блокировку

---

#### `BannedWordEvent`
Модель события запрещённого слова.

Используется для:
- чтения событий из топика `banned_words`
- определения, нужно добавить или удалить слово из списка запрещённых

---

### Обработка блокировок

#### `BlockEventProcessor`
Обрабатывает события из `blocked_users` и обновляет persistent state store блокировок.

Логика:
- при `BLOCK` создаёт запись в store
- при `UNBLOCK` удаляет запись из store

Ключ в store формируется как:

```text
receiver|sender
```

Например:

```text
bob|alice
```

Это означает, что получатель `bob` заблокировал отправителя `alice`.

---

#### `BlockFilterTransformer`
Проверяет каждое сообщение из `messages`.

Если для пары `receiver|sender` найдена блокировка в store, сообщение отбрасывается.  
Если блокировки нет, сообщение передаётся дальше на следующий этап обработки.

---

### Обработка запрещённых слов

#### `BannedWordEventProcessor`
Обрабатывает события из `banned_words` и обновляет persistent state store запрещённых слов.

Логика:
- при `ADD` слово добавляется в store
- при `REMOVE` слово удаляется из store

---

#### `CensorshipTransformer`
Проверяет текст сообщения на наличие запрещённых слов.

Если запрещённые слова найдены:
- они заменяются на символы `*`
- сообщение в изменённом виде передаётся дальше в выходной топик

Если запрещённых слов нет, сообщение передаётся без изменений.

---

### Тестовые producer-классы

#### `ChatMessageProducer`
Тестовый producer, который отправляет сообщения в топик `messages`.

Используется для проверки основной логики фильтрации и цензуры.

---

#### `BlockedUsersProducer`
Тестовый producer, который отправляет события блокировки в топик `blocked_users`.

Используется для проверки логики блокировки пользователей.

---

#### `BannedWordsProducer`
Тестовый producer, который отправляет события запрещённых слов в топик `banned_words`.

Используется для проверки логики цензуры.

---

## Какие файлы относятся к этому заданию

Для текущего задания используются следующие файлы:

### Инфраструктура
- `docker-compose.yml`

### Основной Kafka Streams класс
- `src/main/java/ru/practice/kafka/ModerationStreamsApp.java`

### Конфигурация
- `src/main/java/ru/practice/kafka/AppConfig.java`

### Модели
- `src/main/java/ru/practice/kafka/ChatMessage.java`
- `src/main/java/ru/practice/kafka/BlockEvent.java`
- `src/main/java/ru/practice/kafka/BannedWordEvent.java`

### Логика блокировок
- `src/main/java/ru/practice/kafka/BlockEventProcessor.java`
- `src/main/java/ru/practice/kafka/BlockFilterTransformer.java`

### Логика цензуры
- `src/main/java/ru/practice/kafka/BannedWordEventProcessor.java`
- `src/main/java/ru/practice/kafka/CensorshipTransformer.java`

### Тестовые producers
- `src/main/java/ru/practice/kafka/ChatMessageProducer.java`
- `src/main/java/ru/practice/kafka/BlockedUsersProducer.java`
- `src/main/java/ru/practice/kafka/BannedWordsProducer.java`

---

## Какие файлы можно не использовать в этом задании

В проекте могут оставаться старые учебные классы из предыдущих тем, например:

- `ProducerApp`
- `SingleMessageConsumer`
- `BatchMessageConsumer`
- `SimpleKafkaStreamsExample`
- `StreamProcessorExample`
- `ReplicationConfigExample`
- `BlockFilterStreamsApp`

Они могут остаться в проекте, но **не являются основной частью текущего задания**.

Для сдачи и запуска нужно использовать именно:

```text
ru.practice.kafka.ModerationStreamsApp
```

---

## Инструкция по запуску

### 1. Поднять инфраструктуру

```bash
docker compose up -d
```

---

### 2. Проверить, что контейнеры запущены

```bash
docker compose ps
```

Ожидается, что будут доступны:
- `kafka-0`
- `kafka-1`
- `kafka-2`
- `schema-registry`
- `kafka-ui`
- `ksqldb-server`

---

### 3. Проверить созданные топики

```bash
docker compose exec kafka-0 /opt/bitnami/kafka/bin/kafka-topics.sh   --bootstrap-server kafka-0:9092   --list
```

Ожидается, что в списке будут:
- `messages`
- `filtered_messages`
- `blocked_users`
- `banned_words`

---

### 4. Собрать проект

```bash
mvn -DskipTests clean package
```

Ожидаемый результат:

```text
[INFO] BUILD SUCCESS
```

---

### 5. Запустить основное приложение Kafka Streams

```bash
mvn -q -DskipTests exec:java -Dexec.mainClass="ru.practice.kafka.ModerationStreamsApp"
```

Ожидаемый вывод:

```text
ModerationStreamsApp started.
```

---

## Инструкция по тестированию

Ниже приведён базовый сценарий проверки задания.

---

### Шаг 1. Отправить запрещённые слова

```bash
mvn -q -DskipTests exec:java -Dexec.mainClass="ru.practice.kafka.BannedWordsProducer"
```

Тестовые данные, которые будут отправлены:

```text
spam|ADD
badword|ADD
```

Ожидаемый смысл:
- слово `spam` добавляется в список запрещённых
- слово `badword` добавляется в список запрещённых

---

### Шаг 2. Отправить события блокировки

```bash
mvn -q -DskipTests exec:java -Dexec.mainClass="ru.practice.kafka.BlockedUsersProducer"
```

Тестовые данные, которые будут отправлены:

```text
bob|alice|BLOCK
alice|david|UNBLOCK
```

Ожидаемый смысл:
- `bob` блокирует `alice`
- `alice` снимает блокировку с `david`

---

### Шаг 3. Отправить тестовые сообщения

```bash
mvn -q -DskipTests exec:java -Dexec.mainClass="ru.practice.kafka.ChatMessageProducer"
```

Тестовые данные, которые будут отправлены:

```text
msg-1|alice|bob|hello bob
msg-2|alice|bob|this is spam text
msg-3|charlie|bob|badword should be hidden
msg-4|david|alice|normal message for alice
```

---

## Ожидаемый результат обработки

В итоговый топик `filtered_messages` должны попасть только сообщения, которые:
- не были отброшены по блокировке;
- были обработаны цензурой.

Ожидаемый результат:

```text
msg-3|charlie|bob|******* should be hidden
msg-4|david|alice|normal message for alice
```

### Почему именно такой результат

#### Сообщение 1
```text
msg-1|alice|bob|hello bob
```

Не проходит, потому что `bob` заблокировал `alice`.

---

#### Сообщение 2
```text
msg-2|alice|bob|this is spam text
```

Тоже не проходит, потому что `bob` заблокировал `alice`.

Даже несмотря на наличие запрещённого слова `spam`, сообщение не дойдёт до этапа итоговой записи, так как оно отсекается раньше на этапе блокировки.

---

#### Сообщение 3
```text
msg-3|charlie|bob|badword should be hidden
```

Проходит по блокировке, потому что `bob` не блокировал `charlie`.

После этого применяется цензура:
- `badword` найдено в списке запрещённых слов
- слово заменяется на `*******`

Итог:

```text
msg-3|charlie|bob|******* should be hidden
```

---

#### Сообщение 4
```text
msg-4|david|alice|normal message for alice
```

Проходит по блокировке и не содержит запрещённых слов, поэтому записывается без изменений.

---

## Проверка результата в Kafka

### Проверка топика `filtered_messages`

```bash
docker compose exec kafka-0 /opt/bitnami/kafka/bin/kafka-console-consumer.sh   --bootstrap-server kafka-0:9092   --topic filtered_messages   --from-beginning
```

Ожидаемый результат:

```text
msg-3|charlie|bob|******* should be hidden
msg-4|david|alice|normal message for alice
```

---

### Проверка топика `blocked_users`

```bash
docker compose exec kafka-0 /opt/bitnami/kafka/bin/kafka-console-consumer.sh   --bootstrap-server kafka-0:9092   --topic blocked_users   --from-beginning
```

Ожидаемый результат:

```text
bob|alice|BLOCK
alice|david|UNBLOCK
```

---

### Проверка топика `banned_words`

```bash
docker compose exec kafka-0 /opt/bitnami/kafka/bin/kafka-console-consumer.sh   --bootstrap-server kafka-0:9092   --topic banned_words   --from-beginning
```

Ожидаемый результат:

```text
spam|ADD
badword|ADD
```

---

### Проверка топика `messages`

```bash
docker compose exec kafka-0 /opt/bitnami/kafka/bin/kafka-console-consumer.sh   --bootstrap-server kafka-0:9092   --topic messages   --from-beginning
```

Ожидаемый результат:

```text
msg-1|alice|bob|hello bob
msg-2|alice|bob|this is spam text
msg-3|charlie|bob|badword should be hidden
msg-4|david|alice|normal message for alice
```

---

## Дополнительные сценарии проверки

### 1. Проверка разблокировки пользователя

Можно вручную отправить в топик `blocked_users` событие:

```text
bob|alice|UNBLOCK
```

После этого сообщения от `alice` к `bob` должны снова проходить.

Пример ручной отправки:

```bash
docker compose exec -T kafka-0 /opt/bitnami/kafka/bin/kafka-console-producer.sh   --bootstrap-server kafka-0:9092   --topic blocked_users
```

После запуска команды ввести:

```text
bob|alice|UNBLOCK
```

Затем можно отправить новое сообщение:

```bash
docker compose exec -T kafka-0 /opt/bitnami/kafka/bin/kafka-console-producer.sh   --bootstrap-server kafka-0:9092   --topic messages
```

И ввести:

```text
msg-100|alice|bob|hello after unblock
```

Ожидаемый результат:
- сообщение больше не отбрасывается по блокировке
- оно попадёт в `filtered_messages`

---

### 2. Проверка удаления запрещённого слова

Можно вручную отправить в топик `banned_words` событие:

```text
badword|REMOVE
```

После этого слово `badword` не должно маскироваться в новых сообщениях.

Пример ручной отправки:

```bash
docker compose exec -T kafka-0 /opt/bitnami/kafka/bin/kafka-console-producer.sh   --bootstrap-server kafka-0:9092   --topic banned_words
```

После запуска команды ввести:

```text
badword|REMOVE
```

Затем можно отправить новое сообщение:

```bash
docker compose exec -T kafka-0 /opt/bitnami/kafka/bin/kafka-console-producer.sh   --bootstrap-server kafka-0:9092   --topic messages
```

И ввести:

```text
msg-200|charlie|bob|badword is visible again
```

Ожидаемый результат:
- сообщение пройдёт
- слово `badword` уже не будет заменяться на `*`

---

## Что демонстрирует проект

Данный проект демонстрирует:

- использование Kafka Streams для потоковой обработки сообщений
- использование **persistent state store** для хранения состояния
- динамическое обновление состояния из Kafka-топиков
- фильтрацию сообщений по бизнес-правилам
- цензуру текста сообщений
- развёртывание Kafka-инфраструктуры через Docker Compose

---

## Итог

В рамках задания реализована учебная система обработки сообщений, которая:
- хранит списки блокировок пользователей в persistent state store
- хранит список запрещённых слов в persistent state store
- отбрасывает сообщения от заблокированных отправителей
- маскирует запрещённые слова в тексте сообщений
- записывает итоговый результат в отдельный Kafka-топик `filtered_messages`

Основной класс для запуска задания:

```text
ru.practice.kafka.ModerationStreamsApp
```
