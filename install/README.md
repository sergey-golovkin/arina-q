#Установка структуры в СУБД

Для установки структуры в СУБД необходимо: 

###1. Установить и настроить необходимую СУБД.

>Для СУБД ORACLE также требуется выполнить:
```
GRANT EXECUTE ON SYS.DBMS_LOCK TO SYSTEM WITH GRANT OPTION;
GRANT EXECUTE ON SYS.DBMS_CRYPTO TO SYSTEM WITH GRANT OPTION;
```

###2. Установить структуру с СУБД.
Для этого необходимо запустить менеджер очереди
```java
java -cp * arina.q.datasource.Manager
```

и последовательно ответить на вопросы выводимые на консоль:

Пример установки arina-q в СУБД PostgreSql:

```text
Choose operation (install|drop): install
Enter driver class name (FQN): arina.q.datasource.PostgreSql                                         
Enter server name or ip: localhost
Enter server port: 5432
Enter database name: postgres
Enter dba login: postgres 
Enter dba password: Manager_2019
Enter system id: POSTGRESQL
Enter system storage: pg_default
Enter system password: PWD
```

Всё, структура в СУБД создана.


###3. Настройка Apache Camel

В конфигурационный файл вставить:
```xml
<bean id="dsQ.{SYSID}"
    class="arina.q.datasource.Oracle|OracleACO|MsSql|PostgreSql|FireBird|MySql|MariaDb"
    destroy-method="close"
    p:server="{SERVER}"
    p:database="{SID|SERVICE_NAME|DB_NAME|DB_PATH}"
    p:password="{PASSWORD}"
    p:system="{SYSID}"
    [p:port="{PORT}"]
    [p:perfAlertThresholdGet="0"]
    [p:perfAlertThresholdCommit="0"]
    [p:perfAlertThresholdProcessMessage="0"]
    [p:perfAlertThresholdAll="0"]
/>
```              
Пример для установки arina-q в СУБД PostgreSql:

```xml
<bean id="dsQ.POSTGRESQL"
    class="arina.q.datasource.PostgreSql"
    destroy-method="close"
    p:server="localhost"
    p:database="postgres"
    p:password="Manager_2019"
    p:system="POSTGRESQL"
/>
```
