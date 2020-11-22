# arina-q - Очередь собщений и конвейер (машина состояний) на основе СУБД.

Проект "arina-q"  - это попытка закончить беспорядочные попытки программистов делать интеграции по принципу "просто табличка".
Постоянно с этим сталкиваясь, я решил поделиться тем, что уже более 7 лет успешно используется в продакшене ряда крупных компаний. 

Все что вам нужно - это СУБД и Apache Camel , в рамках которого эти библиотеки работают.

Поддерживаемые СУБД:
 - ORACLE
 - MS SQL
 - POSGRESQL
 - MYSQL
 - MARIABD
 - FIREFIRD
 
Я буду рад, если вам это понравится.

```xml

Термины:
1. DATA_IN - входящая очередь
2. DATA_OUT - исходящая очередь
3. STAGES - конвейер

arina-q - очередь, предназначена для однократной последовательной обработки каждого сообщения в соответсвии с заданными фильтрами, после успешной обработки которого оно изымается из очереди.
      В случае неуспешной обработки очередь будет пытаться раза за разом обработать первое сообщение из очереди в соответствии с указанными фильтрами.

    DATA_SOURCE - имя bean, с описанием Data Source.
    SOURCE_SYSID - идентификатор системы источника сообщения (строка)
    DESTINATION_SYSID - идентификатор системы назначения сообщения (строка)
    MESSAGE_DATA_TYPE - идентификатор типа сообщения (число)
    SUB_Q_NO - номер подочереди, по умолчнию = 0 (число)
    ADDITIONAL_INFO - служебная информация сопровождающая основное сообщение (строка)
    EXPIRE_DATE - время жизни сообщения, (дата-время) в формате yyyy-MM-dd HH:mm:ss.fff
    REPLACE_ID - некий уникальный идентификатор сообщения (строка). Используется для подмены сообщения стоящего во входящей очереди.
    TRANS_ID - идентификатор группы сообщений (строка) - логической транзакции. При указании идентификатора логической транзакции сообщения помещаются в исходящую очередь,
               но реально отправляются из нее только после специального указания об этом, либо удаляются автоматически по истечению времени жизни, если указания не поступило.
    TRANS_SEQ_NO - номер сообщения внутри логической транзакции (число). Позволяет указать порядок следования отсылаемых сообщений и
                   позволяет помещать сообщения в логическую транзакцию в произвольной последовательности,
                   но при получении указания на отсылку логической транзакции, сообщения автоматически сортируются в соответсвии с указанным номером внутри нее.
    TRANS_TTL - время жизни логической транзакции сообщений, (время в секундах), по умолчанию 3600 сек.
                По истечению указанного времени, если не поступила команда на отсылку логической транзакции, сообщения входящие в нее автоматически удаляются.

Чтение из очереди:
<from uri="arina-q:{DATA_SOURCE}?
               [mode={Direct|Reverse}&amp;]
               [systemsFilter={SOURCE_SYSID_01}[,|;]{SOURCE_SYSID_02}&amp;]
               [typesFilter={MESSAGE_DATA_TYPE_01}[,|;]{MESSAGE_DATA_TYPE_02}&amp;]
               [subQ={SUB_Q_NO_00}[,|;]{SUB_Q_NO_01}&amp;]
               [initialDelay=60000&amp;]
               [loopDelays=100[,|;]200[,|;]300[,|;]400[,|;]500[,|;]1000[,|;]...]&amp;]
               [errorDelays=10000[,|;]20000[,|;]30000[,|;]60000[,|;]120000[,|;]...]
"/>

    mode={Direct|Reverse|ChangeSubQ} - режим работы очереди (если не указан, то используется Direct)
        - Direct - читать из DATA_IN
	    - Reverse - читать из DATA_OUT

    systemsFilter - фильтр, список систем источников сообщений, разделенный знаком запятая "," или точкой с запятой ";", которые требуется читать (обрабатывается параллельно).
                    Если не указан, то читаются сообщения от всех систем по мере поступления.
    typesFilter - фильтр, список идентификаторов типов сообщений, разделенный знаком запятая "," или точкой с запятой ";", которые требуется читать (обрабатывается последовательно).
                  Если не указан, то читаются сообщения всех типов по мере поступления.
    subQ - фильтр, список номеров подочередей, разделенный знаком запятая "," или точкой с запятой ";", которые требуется читать (обрабатывается параллельно).
                  Если не указан, то читается из подочереди по умолчанию с номером 0.
    initialDelay - задержка в мсек перед началом чтения сообщений сразу после старта.
                   Если не указан, то используется значние 60000 мсек (1 мин)
    loopDelays -  список задержек в мсек перед началом очередного цикла чтения сообщений в случае пустой очереди, разделенный знаком запятая "," или точкой с запятой ";".
                  В случае если в очереди есть сообщения задержки нет, если не указан, то используется задержка 100 мсек

                  В случае первой цикла происходит остановка обработки сообщений на первое значение мсек указанное в списке.
                  В случае второго цикла происходит остановка обработки сообщений на второе значение мсек указанное в списке, итд

                  Если, например, указано три значения, то на четвертый и более раз происходит остановка обработки сообщений
                  на последнее (третье) значение мсек указанное в списке.

                  Если, последним значением указано троеточие (...) , то на четвертый раз происходит остановка обработки сообщений
                  на первое значение мсек указанное в списке итд циклично.
    errorDelays - список задержек в мсек при возникновении ошибок обработки сообщения, разделенный знаком запятая "," или точкой с запятой ";".
                  Если не указан, то используется значение 60000 мсек (1 мин)

                  В случае первой ошибки происходит остановка обработки сообщений на первое значение мсек указанное в списке.
                  В случае второй ошибки происходит остановка обработки сообщений на второе значение мсек указанное в списке, итд

                  Если, например, указано три значения, то на четвертый и более раз происходит остановка обработки сообщений 
                  на последнее (третье) значение мсек указанное в списке.
                  
                  Если, последним значением указано троеточие (...) , то на четвертый раз происходит остановка обработки сообщений
                  на первое значение мсек указанное в списке итд циклично.
    skipExpired - признак, указывающий пропускать или обрабатывать сообщения с истекшим временем жизни, по умолчанию - true - игнорировать и сразу перекладывать в лог обработанных сообщений.
    expiredWarningText - текст предупреждения, которое записывается в лог обработанных сообщений, в случае игнорирования сообщения с истекшим временем жизни. По умолчанию "Message expired!"

    Чтение происходит в следующие переменные:
        Тело сообщения: ${body}

        Остальные данные:
    	header.ARINA-Q-InId
    	header.ARINA-Q-OutId
    	header.ARINA-Q-FromSystem
    	header.ARINA-Q-ToSystem
    	header.ARINA-Q-DataType
    	header.ARINA-Q-DataDate
        header.ARINA-Q-MetaInfo
        header.ARINA-Q-ExpireDate
        header.ARINA-Q-ReplaceId
        header.ARINA-Q-MsgId

Запись в очередь:    
    <to uri="arina-q:{DATA_SOURCE}?
             [mode={Direct|Reverse|ChangeSubQ}&amp;]
             [messageBody={MESSAGE_BODY}&amp;]
             [fromSystem={SOURCE_SYSID}&amp;]
             [toSystem={DESTINATION_SYSID}&amp;]
             [dataType={MESSAGE_DATA_TYPE}&amp;]
             [subQ={SUB_Q_NO}&amp;]
             [metaInfo={ADDITIONAL_INFO}&amp;]
             [expireDate={EXPIRE_DATE}&amp;]
             [replaceId={REPLACE_ID}&amp;]
             [transId={TRANS_ID}&amp;]
             [transSeqNo={TRANS_SEQ_NO}&amp;]
             [transTTL={TRANS_TTL}&amp;]
             [sendTrans=true|false]
    "/>

    mode={Direct|Reverse|ChangeSubQ} - режим работы очереди (если не указан, то используется Direct)
        - Direct - писать в DATA_OUT
	    - Reverse - писать в DATA_IN
        - ChangeSubQ - переместить сообщенение из текущей подочереди в указанную, не изымая сообщение из очереди.

    messageBody - содержимое сообщения (может быть выражением на любом языке Camel).
                 Если не указано, то используется ${body}
    fromSystem - система источник для отсылаемого сообщения (может быть выражением на любом языке Camel).
                 Если не указан, то используется значение из header.ARINA-Q-FromSystem.
    toSystem - система назначения для отсылаемого сообщения (может быть выражением на любом языке Camel).
                 Если не указан, то используется значение из header.ARINA-Q-ToSystem.
    dataType - идентификатор типа отсылаемого сообщения (может быть выражением на любом языке Camel).
                 Если не указан, то используется значение из header.ARINA-Q-DataType.
    subQ - номеров подочереди, в которую требуется записать отсылаемое сообщение (только для режимлв работы Reverse или ChangeSubQ) (может быть выражением на любом языке Camel).
                 Если не указан, то используется значение из header.ARINA-Q-SubQ, а если нет такого, то равен 0.
    metaInfo - служебная информация сопровождающая основное сообщение (может быть выражением на любом языке Camel).
                 Если не указано, то используется ${header.ARINA-Q-MetaInfo}
    expireDate - время жизни сообщения. По умолчанию, сообщения с истекшим временем жизни сразу переносятся в лог обработанных сообщения, при этом в таблице ошибок остается предупреждение.
                 Формат yyyy-MM-dd HH:mm:ss.fff, например expireDate=date:now:yyyy-MM-dd HH:mm:ss или 2017-06-30 13:43:44.727 или language:groovy:java.time.OffsetDateTime.now().plusDays(1)
                 Если не указано, то используется ${header.ARINA-Q-ExpireDate}
    replaceId - идентификатор сообщения для его подмены во входящей очереди.
                 Использование идентификатора сообщения позволяет реализовать следующие задачи:
                     - замена ошибочного сообщения - система приемник никак не может обработать входящее сообщение, например из за ошибки.
                       Она сообщает системе отправителю об этом и система отправитель посылает скорректированное сообщение с таким же идентификатором сообщения,
                       при попадании во входящую очередь оно сразу же автоматически заменяет собой ошибочное, при этом ошибочное уходит в лог успешнообработанных сообщений DATA_IN_LOG,
                       а так же формируется запись в PROCESS_DATA_IN_LOG с кодом 0 и сообщением 'Message replaced!'
                     - обработка потока телеметрической/статистической информации, когда обработка каждого сообщения не важна, а система приемник не успевает за потоком входящих сообщений.
    transId - идентификатор группы сообщений - логической транзакции.
    transSeqNo - номер сообщения внутри логической транзакции.
    transTTL - время жизни логической транзакции в секундах.
    sendTrans - признак необходимости отсылки сообщений логической транзакции.
                По умолчанию - false, поместить сообщение в логическую транзакцию, но не отсылать.
                true - поместить последнее сообщение в логическую транзакцию и отослать все сообщения вхлдящие в нее.

arina-sq - конвейер (машина состояний), предназначен для многоэтапной параллельной обработки каждого сообщения прежде чем оно будет изъято c конвейера.
           Каждый этап характеризуется своим уникальным строковым идентификатором.

    DATA_SOURCE - имя bean, с описанием Data Source.
    SOURCE_SYSID - идентификатор системы источника сообщения (строка)
    DESTINATION_SYSID - идентификатор системы назначения сообщения (строка)

Чтение с конвейера:
    <from uri="arina-sq:{DATA_SOURCE}?
               stage={STAGE_NAME}&amp;
               [systemsFilter={SOURCE_SYSID}&amp;]
               [delay=0&amp;]
               [autoincrement=true|false&amp;]
               [threads=1&amp;]
               [initialDelay=60000&amp;]
               [loopDelays=100[,|;]200[,|;]300[,|;]400[,|;]500[,|;]1000[,|;]...]&amp;]
               [errorDelays=10000[,|]30000[,|]60000[,|]10000[,|]30000[,|]60000[,|]...]
    "/>

    stage - идентификатор этапа обработки сообщения конвейером (строка, обязательное значение)
    systemsFilter - фильтр, система источник сообщений, которые требуется читать.
                    Если не указан, то читаются сообщения от всех систем по мере поступления.
    delay - задержка в мсек после окончания очередной итерации обработки сообщения текущем этапом конвейера и началом новой итерации.
            Если не указан, то используется значние 0 мсек (без задержки)
    autoincrement - увеличивать ли счетчик итераций обработке на текущем этапе обработки сообщения конвейером.
                    Если не указан, то используется значение (true).
    threads - каждое сообщение на конвейере готово обрабатываеться параллельно. Данный параметр указывает во сколько потоков возможна параллельная обработка сообщений.
                    Если не указан, то используется оден поток.
    initialDelay - задержка в мсек перед началом чтения сообщений сразу после старта.
                   Если не указан, то используется значние 60000 мсек (1 мин)
    loopDelays -  список задержек в мсек перед началом очередного цикла чтения сообщений в случае пустой очереди, разделенный знаком запятая "," или точкой с запятой ";".
                  В случае если в очереди есть сообщения задержки нет, если не указан, то используется задержка 100 мсек

                  В случае первой цикла происходит остановка обработки сообщений на первое значение мсек указанное в списке.
                  В случае второго цикла происходит остановка обработки сообщений на второе значение мсек указанное в списке, итд

                  Если, например, указано три значения, то на четвертый и более раз происходит остановка обработки сообщений
                  на последнее (третье) значение мсек указанное в списке.

                  Если, последним значением указано троеточие (...) , то на четвертый раз происходит остановка обработки сообщений
                  на первое значение мсек указанное в списке итд циклично.
    errorDelays - список задержек в мсек при возникновении ошибок обработки сообщения, разделенный знаком запятая "," или точкой с запятой ";".
                  Если не указан, то используется значение 60000 мсек (1 мин)

                  В случае первой ошибки происходит остановка обработки сообщений на первое значение мсек указанное в списке.
                  В случае второй ошибки происходит остановка обработки сообщений на второе значение мсек указанное в списке, итд

                  Если, например, указано три значения, то на четвертый и более раз происходит остановка обработки сообщений
                  на последнее (третье) значение мсек указанное в списке.

                  Если, последним значением указано троеточие (...) , то на четвертый раз происходит остановка обработки сообщений
                  на первое значение мсек указанное в списке итд циклично.

    Чтение происходит в следующие переменные:
        Тело сообщения: ${body}

        Остальные данные:
            header.ARINA-Q-RequestId
            header.ARINA-Q-RequestDate
            header.ARINA-Q-StageDate
            header.ARINA-Q-Stage
            header.ARINA-Q-Iteration
            header.ARINA-Q-DepId
            header.ARINA-Q-ParentDepId
            header.ARINA-Q-MetaInfo

Запись в конвейер:
	<to uri="arina-sq:{DATA_SOURCE}?
	         stage={STAGE_NAME}&amp;
                 [messageBody={MESSAGE_BODY}&amp;]
                 [delay=0&amp;]
                 [depId={BUSINESS_MESSAGE_ID}&amp;]
                 [parentDepId={PARENT_BUSINESS_MESSAGE_ID}&amp;]
                 [metaInfo={ADDITIONAL_INFO}&amp;]
                 [waitFinal=0&amp;]
                 [loopDelays=100[,|;]200[,|;]300[,|;]400[,|;]500[,|;]1000[,|;]...]&amp;]
                 [final=true|false]
	"/>

    stage - идентификатор этапа обработки сообщения конвейером (строка, обязательное значение) (может быть выражением на любом языке Camel)
    messageBody - содержимое сообщения (может быть выражением на любом языке Camel).
                  Если не указано, то используется ${body}
    delay - задержка в мсек после окончания очередной итерации обработки сообщения текущем этапом конвейера и началом первой итерации обработки новым этапом.
            Если не указан, то используется значние 0 мсек (без задержки)
    depId - собственный бизнес-идентификатор сообщения (может быть выражением на любом языке Camel).
                  Если не указан, то используется значение из header.ARINA-Q-DepId, а если нет такого, то равен null.
    parentDepId - список бизнес-идентификаторов сообщений от которых зависит обработка данного сообщения,
                  разделенный знаком запятая "," или точкой с запятой ";" (может быть выражением на любом языке Camel).
                  Если не указан, то используется значение из header.ARINA-Q-ParentDepId, а если нет такого, то равен null.
    metaInfo - служебная информация сопровождающая основное сообщение (может быть выражением на любом языке Camel).
                 Если не указано, то используется header.ARINA-Q-MetaInfo
    waitFinal - позволяет записать в конвейер сообщение и ждать указанное количество миллисекунд или окончания обработки сообщения на конвейере (что раньше произойдет).
                 При этом, в случае. если произошел таймаут header.ARINA-Q-WaitFinalTimeout будет равен true, а если результат получен, то false.
                 Если header.ARINA-Q-WaitFinalTimeout = false, то ${body} будет содержать обработанное финальным этапом сообщение.
    loopDelays - список задержек в мсек перед началом очередного цикла чтения сообщений в финальном статусе, разделенный знаком запятая "," или точкой с запятой ";".
                 Если не указан, то используется задержка 100 мсек

                 В случае первой цикла происходит остановка обработки сообщений на первое значение мсек указанное в списке.
                 В случае второго цикла происходит остановка обработки сообщений на второе значение мсек указанное в списке, итд

                 Если, например, указано три значения, то на четвертый и более раз происходит остановка обработки сообщений
                 на последнее (третье) значение мсек указанное в списке.

                 Если, последним значением указано троеточие (...) , то на четвертый раз происходит остановка обработки сообщений
                 на первое значение мсек указанное в списке итд циклично.
    final - признак, что сообщение полностью обработано и его надо изъять с конвейера.

    Конвейер подразумевает, что каждое сообщение полностью независимо от остальных и может обрабатываться параллельно наряду с другими сообщениями.
    Но это не всегда так, при помещении сообщения на конвейер можно указать бизнес-идентификатор (строка до 100 символов) сообщения, а так же список
    бизнес-идентификаторов сообщений (строка до 4000 символов) от которых зависит его обработка.
    Таким образом можно добиться отсрочки начала обработки сообщения конвейером до полного окончания обработки всех сообщений от которых зависит его обработка.
    При этом зависимость устанавливается только между сообщениями помещенными на конвейер до вставки текущего сообщения. Вставленные после этого сообщения
    с такими же бизнес-идентификаторами на уже установленную зависимость не влияют.

Примеры:

1. Вставка в DATA_OUT
<route autoStartup="true">
	<from ... />
    <to uri="arina-q:{DESTINATION_DATA_SOURCE}?fromSystem={SOURCE_SYSID}&amp;toSystem={DESTINATION_SYSID}&amp;dataType={MESSAGE_DATA_TYPE}"/>
</route>
или
<route autoStartup="true">
	<from ... />
    <setHeader headerName="ARINA-Q-FromSystem"><constant>{SOURCE_SYSID}</constant></setHeader>
    <setHeader headerName="ARINA-Q-ToSystem"><constant>{DESTINATION_SYSID}</constant></setHeader>
    <setHeader headerName="ARINA-Q-DataType"><constant>{MESSAGE_DATA_TYPE}</constant></setHeader>
    <to uri="arina-q:{DESTINATION_DATA_SOURCE}"/>
</route>
или
<route autoStartup="true">
	<from ... />
    <setHeader headerName="somePropertyFromSystem"><constant>{SOURCE_SYSID}</constant></setHeader>
    <setHeader headerName="somePropertyToSystem"><constant>{DESTINATION_SYSID}</constant></setHeader>
    <setHeader headerName="somePropertyDataType"><constant>{MESSAGE_DATA_TYPE}</constant></setHeader>
    <to uri="arina-q:{DESTINATION_DATA_SOURCE}?fromSystem=${header.somePropertyFromSystem}&amp;toSystem=${header.somePropertyToSystem}&amp;dataType=${header.somePropertyDataType}"/>
</route>


2. Перемещение сообщений из исходящей очереди одной системы во входящую очередь другой системы
<route autoStartup="true">
    <from uri="arina-q:{SOURCE_DATA_SOURCE}?mode=Reverse&amp;systemsFilter={DESTINATION_SYSID}"/>
    <to uri="arina-q:{DESTINATION_DATA_SOURCE}?mode=Reverse"/>
</route>

3. Перемещение сообщений из исходящей очереди одной системы во входящие очереди двух других систем
<route autoStartup="true">
    <from uri="arina-q:{SOURCE_DATA_SOURCE}?mode=Reverse&amp;systemsFilter={DESTINATION_SYSID_01}"/>
    <to uri="arina-q:{DESTINATION_DATA_SOURCE_01}?mode=Reverse"/>
</route>
<route autoStartup="true">
    <from uri="arina-q:{SOURCE_DATA_SOURCE}?mode=Reverse&amp;systemsFilter={DESTINATION_SYSID_02}"/>
    <to uri="arina-q:{DESTINATION_DATA_SOURCE_02}?mode=Reverse"/>
</route>
или если имена data source бинов будут иметь единый принцип наименования, например dsQ.{SYSID}, где SYSID - это строковый идентификатор системы,
то можно заменить два маршрута одним маршрутом использую маску имени data source - "dsQ.${header.ARINA-Q-ToSystem}".
<route autoStartup="true">
    <from uri="arina-q:{SOURCE_DATA_SOURCE}?mode=Reverse&amp;systemsFilter={DESTINATION_SYSID_01},{DESTINATION_SYSID_02}"/>
    <to uri="arina-q:dsQ.${header.ARINA-Q-ToSystem}?mode=Reverse"/>
</route>

3. Перемещение сообщений из исходящей очереди одной системы во входящие очереди двух других систем, причем с размещением их сразу по подочередям {SUB_Q_NO_01} и {SUB_Q_NO_02} по принципу round robin.
<route autoStartup="false">
    <from uri="arina-q:{SOURCE_DATA_SOURCE}?mode=Reverse&amp;systemsFilter={DESTINATION_SYSID_01},{DESTINATION_SYSID_02}"/>
    <loadBalance>
		<roundRobin/>
    	<to uri="arina-q:dsQ.${header.ARINA-Q-ToSystem}?mode=Reverse&amp;subQ={SUB_Q_NO_01}"/>
    	<to uri="arina-q:dsQ.${header.ARINA-Q-ToSystem}?mode=Reverse&amp;subQ={SUB_Q_NO_02}"/>
    </loadBalance>
</route>

5. Обработка сообщений из входящей очереди системы пришедших из систем с иденитфикаторами {SOURCE_SYSID_01},{SOURCE_SYSID_02} и типами сообщений {MESSAGE_DATA_TYPE_01},{MESSAGE_DATA_TYPE_02}
В случае если в процессе обработки сообщения не было сненерировано любое исключение (Exception), то после обработки последней команды маршрута сообщение будет изъято из очереди.
Если же было сгенерировано исключение, то его обработка приостанавливается на указанное в параметре errorDelays мсек, после чего оно будет заново взято для обработки
и так до тех пор пока обработка не завершится успешно.
<route autoStartup="true">
    <from uri="arina-q:{DATA_SOURCE}?systemsFilter={SOURCE_SYSID_01},{SOURCE_SYSID_02}&amp;typesFilter={MESSAGE_DATA_TYPE_01},{MESSAGE_DATA_TYPE_02}"/>
    <to .... />
</route>

6. Так же можно разложить уже полученные сообщения из входящей очереди по подочередям с идентификаторами {SUB_Q_NO_01} и {SUB_Q_NO_01} не изымая их из входящей очереди.
<route autoStartup="false">
    <from uri="arina-q:{DATA_SOURCE}?systemsFilter={SOURCE_SYSID_01},{SOURCE_SYSID_02}&amp;typesFilter={MESSAGE_DATA_TYPE_01},{MESSAGE_DATA_TYPE_02}"/>
    <loadBalance>
    	<roundRobin/>
        <to uri="arina-q:{DATA_SOURCE}?mode=ChangeSubQ&amp;subQ={SUB_Q_NO_01}"/>
        <to uri="arina-q:{DATA_SOURCE}?mode=ChangeSubQ&amp;subQ={SUB_Q_NO_02}"/>
	</loadBalance>
</route>

7. А потом обработать их параллельно
<route autoStartup="false">
    <from uri="arina-q:{SOURCE_SYSID}?subQ={SUB_Q_NO_01},{SUB_Q_NO_02}"/>
    <to .... />
</route>

8. Помещение сообщений типа {MESSAGE_DATA_TYPE} из входящей очереди системы источника на конвейер для этапа обработки с идентификатором {STAGE_NAME_01}
<route autoStartup="true">
    <from uri="arina-q:{SOURCE_SYSID}?typesFilter={MESSAGE_DATA_TYPE}"/>
    <to uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_01}"/>
</route>

9. Обработка конвейером сообщений находящихся на этапе {STAGE_NAME_01} в пять параллельных потоков и перевод сообщений на этап {STAGE_NAME_02} в случае их успешной обработки.
   Если же сообщение было неуспешно обработано, то есть в ходе его обработки было ошибки, то отбработка сообщения откладывается на delay=60000 мсек,
   после чего оно снова будет взято в обработку.

<route autoStartup="true">
    <from uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_01}&amp;delay=60000&amp;threads=5"/>
    ....
    <to uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_02}"/>
</route>

10. Обработка конвейером сообщений находящихся на этапе {STAGE_NAME_02} в пять параллельных потоков и изъятие его с конвейера в случае успешной обработки, final=true.
<route autoStartup="true">
    <from uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_02}&amp;delay=60000&amp;threads=5"/>
    ....
    <to uri="arina-sq:{DATA_SOURCE}?stage={STAGE_NAME_03}&amp;final=true"/>
</route>

11. Пример обработка конвейером сообщений находящихся на этапе {STAGE_NAME_02}, и отсылки его  исходящую очередь системы источника (см. пример 8)
    первоначального сообщения и изъятие его с конвейера в случае успешной обработки.
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

12. Помещение сообщения на конвейер с указанием его бизнес-идентификатора "1".
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

13. Помещение сообщения на конвейер с указанием бизнес-идентификатора сообщения, от которого зависит его обработка.
    При этом обработка этого сообщения начнется после обработки всех сообщений с бизнес-идентификатором "1" находящимся на конвейере на момент помещения указанного сообщения.
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

14. Чтение, обработка и возврат ответа на сообщение
<route autoStartup="true">
    <from uri="arina-q:{DESTINATION_DATA_SOURCE}?systemsFilter={SOURCE_SYSID}"/>
    .... обработка ....
    <to uri="arina-q:{DESTINATION_DATA_SOURCE}?dataType={MESSAGE_DATA_TYPE}&amp;toSystem=${header.ARINA-Q-FromSystem}"/>
</route>

15. Запись в исходящую очередь сообщения с указанием служебной информации и времени жизни сообщения - один месяц от момента попадания в очередь
<route autoStartup="true">
    <from ....>
    <to uri="arina-q:{DESTINATION_DATA_SOURCE}?fromSystem={SOURCE_SYSID}&amp;toSystem={DESTINATION_SYSID}&amp;dataType={MESSAGE_DATA_TYPE}&amp;metaInfo=${id}&amp;expireDate=language:javascript:java.time.OffsetDateTime.now().plusMonths(1)"/>
</route>

16. Чтение сообщений с истекшим временем жизни (для специальной обработки)
<route autoStartup="false">
    <from uri="arina-q:{DESTINATION_DATA_SOURCE}?systemsFilter={SOURCE_SYSID}&amp;skipExpired=false"/>
    <to .... >
</route>

17. Использование ожидания обработки сообщения конвейером
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

18. Использование отсылки группы сообщений с указанием последовательности отсылки сообщений
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
