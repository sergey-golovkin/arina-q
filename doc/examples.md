###1. Вставка в исходящую очередь
```xml
<route autoStartup="true">
    <from ... />
    <to uri="arina-q:{DESTINATION_DATA_SOURCE}?fromSystem={SOURCE_SYSID}&amp;toSystem={DESTINATION_SYSID}&amp;dataType={MESSAGE_DATA_TYPE}"/>
</route>
```
или
```xml
<route autoStartup="true">
    <from ... />
    <setHeader headerName="ARINA-Q-FromSystem"><constant>{SOURCE_SYSID}</constant></setHeader>
    <setHeader headerName="ARINA-Q-ToSystem"><constant>{DESTINATION_SYSID}</constant></setHeader>
    <setHeader headerName="ARINA-Q-DataType"><constant>{MESSAGE_DATA_TYPE}</constant></setHeader>
    <to uri="arina-q:{DESTINATION_DATA_SOURCE}"/>
</route>
```
или
```xml
<route autoStartup="true">
    <from ... />
    <setHeader headerName="somePropertyFromSystem"><constant>{SOURCE_SYSID}</constant></setHeader>
    <setHeader headerName="somePropertyToSystem"><constant>{DESTINATION_SYSID}</constant></setHeader>
    <setHeader headerName="somePropertyDataType"><constant>{MESSAGE_DATA_TYPE}</constant></setHeader>
    <to uri="arina-q:{DESTINATION_DATA_SOURCE}?fromSystem=${header.somePropertyFromSystem}&amp;toSystem=${header.somePropertyToSystem}&amp;dataType=${header.somePropertyDataType}"/>
</route>
```

###2. Перемещение сообщений из исходящей очереди одной системы во входящую очередь другой системы
```xml
<route autoStartup="true">
    <from uri="arina-q:{SOURCE_DATA_SOURCE}?mode=Reverse&amp;systemsFilter={DESTINATION_SYSID}"/>
    <to uri="arina-q:{DESTINATION_DATA_SOURCE}?mode=Reverse"/>
</route>
```

###3. Перемещение сообщений из исходящей очереди одной системы во входящие очереди двух других систем
```xml
<route autoStartup="true">
    <from uri="arina-q:{SOURCE_DATA_SOURCE}?mode=Reverse&amp;systemsFilter={DESTINATION_SYSID_01}"/>
    <to uri="arina-q:{DESTINATION_DATA_SOURCE_01}?mode=Reverse"/>
</route>

<route autoStartup="true">
    <from uri="arina-q:{SOURCE_DATA_SOURCE}?mode=Reverse&amp;systemsFilter={DESTINATION_SYSID_02}"/>
    <to uri="arina-q:{DESTINATION_DATA_SOURCE_02}?mode=Reverse"/>
</route>
```
или если имена data source бинов будут иметь единый принцип наименования, например dsQ.{SYSID}, где SYSID - это строковый идентификатор системы,
то можно заменить два маршрута одним маршрутом использую маску имени data source - "dsQ.${header.ARINA-Q-ToSystem}".
```xml
<route autoStartup="true">
    <from uri="arina-q:{SOURCE_DATA_SOURCE}?mode=Reverse&amp;systemsFilter={DESTINATION_SYSID_01},{DESTINATION_SYSID_02}"/>
    <to uri="arina-q:dsQ.${header.ARINA-Q-ToSystem}?mode=Reverse"/>
</route>
```

###4. Перемещение сообщений из исходящей очереди одной системы во входящие очереди двух других систем, причем с размещением их сразу по подочередям {SUB_Q_NO_01} и {SUB_Q_NO_02} по принципу round robin.
```xml
<route autoStartup="false">
    <from uri="arina-q:{SOURCE_DATA_SOURCE}?mode=Reverse&amp;systemsFilter={DESTINATION_SYSID_01},{DESTINATION_SYSID_02}"/>
    <loadBalance>
        <roundRobin/>
        <to uri="arina-q:dsQ.${header.ARINA-Q-ToSystem}?mode=Reverse&amp;subQ={SUB_Q_NO_01}"/>
    	<to uri="arina-q:dsQ.${header.ARINA-Q-ToSystem}?mode=Reverse&amp;subQ={SUB_Q_NO_02}"/>
    </loadBalance>
</route>
```

###5. Обработка сообщений из входящей очереди системы пришедших из систем с иденитфикаторами {SOURCE_SYSID_01},{SOURCE_SYSID_02} и типами сообщений {MESSAGE_DATA_TYPE_01},{MESSAGE_DATA_TYPE_02}
В случае если в процессе обработки сообщения не было сненерировано любое исключение (Exception), то после обработки последней команды маршрута сообщение будет изъято из очереди.
Если же было сгенерировано исключение, то его обработка приостанавливается на указанное в параметре errorDelays мсек, после чего оно будет заново взято для обработки
и так до тех пор пока обработка не завершится успешно.
```xml
<route autoStartup="true">
    <from uri="arina-q:{DATA_SOURCE}?systemsFilter={SOURCE_SYSID_01},{SOURCE_SYSID_02}&amp;typesFilter={MESSAGE_DATA_TYPE_01},{MESSAGE_DATA_TYPE_02}"/>
    <to .... />
</route>
```

###6. Так же можно разложить уже полученные сообщения из входящей очереди по подочередям с идентификаторами {SUB_Q_NO_01} и {SUB_Q_NO_01} не изымая их из входящей очереди.
```xml
<route autoStartup="false">
    <from uri="arina-q:{DATA_SOURCE}?systemsFilter={SOURCE_SYSID_01},{SOURCE_SYSID_02}&amp;typesFilter={MESSAGE_DATA_TYPE_01},{MESSAGE_DATA_TYPE_02}"/>
    <loadBalance>
    	<roundRobin/>
        <to uri="arina-q:{DATA_SOURCE}?mode=ChangeSubQ&amp;subQ={SUB_Q_NO_01}"/>
        <to uri="arina-q:{DATA_SOURCE}?mode=ChangeSubQ&amp;subQ={SUB_Q_NO_02}"/>
	</loadBalance>
</route>
```

###7. А потом обработать их параллельно
```xml
<route autoStartup="false">
    <from uri="arina-q:{SOURCE_SYSID}?subQ={SUB_Q_NO_01},{SUB_Q_NO_02}"/>
    <to .... />
</route>
```

###8. Помещение сообщений типа {MESSAGE_DATA_TYPE} из входящей очереди системы источника на конвейер для этапа обработки с идентификатором {STAGE_NAME_01}
```xml
<route autoStartup="true">
    <from uri="arina-q:{SOURCE_SYSID}?typesFilter={MESSAGE_DATA_TYPE}"/>
    <to uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_01}"/>
</route>
```

###9. Обработка конвейером сообщений находящихся на этапе {STAGE_NAME_01} в пять параллельных потоков и перевод сообщений на этап {STAGE_NAME_02} в случае их успешной обработки.
   Если же сообщение было неуспешно обработано, то есть в ходе его обработки было ошибки, то отбработка сообщения откладывается на delay=60000 мсек,
   после чего оно снова будет взято в обработку.

```xml
<route autoStartup="true">
    <from uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_01}&amp;delay=60000&amp;threads=5"/>
    ....
    <to uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_02}"/>
</route>
```

###10. Обработка конвейером сообщений находящихся на этапе {STAGE_NAME_02} в пять параллельных потоков и изъятие его с конвейера в случае успешной обработки, final=true.
```xml
<route autoStartup="true">
    <from uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_02}&amp;delay=60000&amp;threads=5"/>
    ....
    <to uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_03}&amp;final=true"/>
</route>
```

###11. Пример обработка конвейером сообщений находящихся на этапе {STAGE_NAME_02}, и отсылки его  исходящую очередь системы источника (см. пример 8)
    первоначального сообщения и изъятие его с конвейера в случае успешной обработки.
```xml
<route autoStartup="true">
    <from uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_02}&amp;delay=60000"/>
    ....
    <setHeader headerName="ARINA-Q-ToSystem"><simple>${header.ARINA-Q-FromSystem}</simple></setHeader>
    <to uri="arina-q:{DATA_SOURCE}?dataType={MESSAGE_DATA_TYPE}"/>
    <to uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_03}&amp;final=true"/>
</route>

или

<route autoStartup="true">
    <from uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_02}&amp;delay=60000"/>
    ....
    <to uri="arina-q:{DATA_SOURCE}?dataType={MESSAGE_DATA_TYPE}&amp;toSystem=${header.ARINA-Q-FromSystem}"/>
    <to uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_03}&amp;final=true"/>
</route>
```

###12. Помещение сообщения на конвейер с указанием его бизнес-идентификатора "1".
```xml
<route autoStartup="true">
    <from ...."/>
    <setHeader headerName="ARINA-Q-DepId"><constant>1</constant></setHeader>
    <to uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_01}"/>
</route>

или

<route autoStartup="true">
    <from ...."/>
    <to uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_01}&amp;depId=1"/>
</route>

или

<route autoStartup="true">
    <from ...."/>
    <setHeader headerName="someProperty"><constant>1</constant></setHeader>
    <to uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_01}&amp;depId=${header.someProperty}"/>
</route>
```

###13. Помещение сообщения на конвейер с указанием бизнес-идентификатора сообщения, от которого зависит его обработка.
При этом обработка этого сообщения начнется после обработки всех сообщений с бизнес-идентификатором "1" находящимся на конвейере на момент помещения указанного сообщения.
```xml
<route autoStartup="true">
    <from ...."/>
    <setHeader headerName="ARINA-Q-ParentDepId"><constant>1</constant></setHeader>
    <to uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_01}"/>
</route>

или

<route autoStartup="true">
    <from ...."/>
    <to uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_01}&amp;parentDepId=1"/>
</route>

или

<route autoStartup="true">
    <from ...."/>
    <setHeader headerName="someProperty"><constant>1</constant></setHeader>
    <to uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_01}&amp;parentDepId=${header.someProperty}"/>
</route>
```

###14. Чтение, обработка и возврат ответа на сообщение
```xml
<route autoStartup="true">
    <from uri="arina-q:{DESTINATION_DATA_SOURCE}?systemsFilter={SOURCE_SYSID}"/>
    .... обработка ....
    <to uri="arina-q:{DESTINATION_DATA_SOURCE}?dataType={MESSAGE_DATA_TYPE}&amp;toSystem=${header.ARINA-Q-FromSystem}"/>
</route>
```

###15. Запись в исходящую очередь сообщения с указанием служебной информации и времени жизни сообщения - один месяц от момента попадания в очередь
```xml
<route autoStartup="true">
    <from ....>
    <to uri="arina-q:{DESTINATION_DATA_SOURCE}?fromSystem={SOURCE_SYSID}&amp;toSystem={DESTINATION_SYSID}&amp;dataType={MESSAGE_DATA_TYPE}&amp;metaInfo=${id}&amp;expireDate=language:javascript:java.time.OffsetDateTime.now().plusMonths(1)"/>
</route>
```

###16. Чтение сообщений с истекшим временем жизни (для специальной обработки)
```xml
<route autoStartup="false">
    <from uri="arina-q:{DESTINATION_DATA_SOURCE}?systemsFilter={SOURCE_SYSID}&amp;skipExpired=false"/>
    <to .... >
</route>
```

###17. Использование ожидания обработки сообщения конвейером
```xml
<route autoStartup="true">
    <from uri="arina-q:dsQ.{SYS_ID}"/>
    <log message="\n\nBEFORE STAGES\n\n${header.ARINA-Q-MetaInfo}\n\n${body}"/>
    <to uri="arina-sq:dsQ.{SYS_ID}?stage=STEP1&amp;metaInfo=${header.ARINA-Q-MetaInfo}\n${id}&amp;messageBody=${body}\nTEST CASE STEP 2&amp;waitFinal=60000"/>
    <log message="\n\nAFTER STAGES\n\n${header.ARINA-Q-WaitFinalTimeout}\n${header.ARINA-Q-MetaInfo}\n${body}"/>
</route>

<route autoStartup="true">
    <from uri="arina-sq:dsQ.{SYS_ID}?stage=STEP1&amp;delay=60000"/>
    <delay>
        <constant>5000</constant>
    </delay>
    <to uri="arina-sq:dsQ.{SYS_ID}?stage=STEP2&amp;metaInfo=${header.ARINA-Q-MetaInfo}\n${id}&amp;messageBody=${body}\nTEST CASE STEP 3"/>
</route>

<route autoStartup="true">
    <from uri="arina-sq:dsQ.{SYS_ID}?stage=STEP2&amp;delay=60000"/>
    <setBody><simple>${body}\nTEST CASE STEP 4</simple></setBody>
    <setHeader headerName="ARINA-Q-MetaInfo"><simple>${header.ARINA-Q-MetaInfo}\n${id}</simple></setHeader>
    <to uri="arina-sq:dsQ.{SYS_ID}?stage=STEP3&amp;final=true"/>
</route>
```

###18. Использование отсылки группы сообщений с указанием последовательности отсылки сообщений
```xml
    ...
    <setBody><constant>1</constant></setBody>
    <loop doWhile="true">
        <simple>${body} &lt;= 10</simple>
        <to uri="arina-q:dsQ.{SYS_ID}?messageBody=${body}&amp;toSystem=FILE&amp;dataType=0&amp;metaInfo=${body}&amp;transId=1&amp;transSeqNo=${body}"/>
        <setBody><simple>${body}++</simple></setBody>
    </loop>
    <to uri="arina-q:dsQ.{SYS_ID}?messageBody=START&amp;toSystem=FILE&amp;dataType=0&amp;metaInfo=START&amp;transId=1&amp;transSeqNo=0"/>
    <to uri="arina-q:dsQ.{SYS_ID}?messageBody=FINISH&amp;toSystem=FILE&amp;dataType=0&amp;metaInfo=FINISH&amp;transId=1&amp;transSeqNo=${body}&amp;sendTrans=true"/>
    ...
```
