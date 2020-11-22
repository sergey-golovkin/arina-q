package arina.q.datasource;

import arina.q.camel.CamelHeaders;
import arina.q.camel.jmx.JmxQMessage;
import arina.q.camel.jmx.JmxSQMessage;
import org.apache.camel.Exchange;

import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: Golovkin
 * Date: 11.05.17
 * Time: 19:32
 * To change this template use File | Settings | File Templates.
 */
public class SQElement
{
    public String rowId;
    public String inId;
    public String fromSystem;
    public Timestamp stageDate;
    public long requestId;
    public Timestamp requestDate;
    public String stage;
    public String body;
    public long iteration;
    public String metaInfo;

    SQElement(final String rowId, final String inId, final String fromSystem, final Timestamp requestDate, final Timestamp stageDate, final long requestId, final String body, final String metaInfo, final String stage, final Long iteration)
    {
        this.rowId = rowId;
        this.inId = inId;
        this.fromSystem = fromSystem;
        this.stageDate = stageDate;
        this.requestId = requestId;
        this.requestDate = requestDate;
        this.stage = stage;
        this.body = body;
        this.iteration = iteration;
        this.metaInfo = metaInfo;
    }

    public void toExchange(Exchange exchange)
    {
        CamelHeaders.setValue(exchange, CamelHeaders.rowId, this.rowId);
        CamelHeaders.setValue(exchange, CamelHeaders.inId, this.inId);
        CamelHeaders.setValue(exchange, CamelHeaders.fromSystem, this.fromSystem);
        CamelHeaders.setValue(exchange, CamelHeaders.stageDate, this.stageDate); // new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(this.stageDate)
        CamelHeaders.setValue(exchange, CamelHeaders.requestId, this.requestId);
        CamelHeaders.setValue(exchange, CamelHeaders.requestDate, this.requestDate); // new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(this.requestDate)
        CamelHeaders.setValue(exchange, CamelHeaders.stage, this.stage);
        CamelHeaders.setValue(exchange, CamelHeaders.iteration, this.iteration);
        CamelHeaders.setValue(exchange, CamelHeaders.metaInfo, this.metaInfo);
        exchange.getIn().setBody(this.body);

    }

    public void toJmx(JmxSQMessage jmx)
    {
        jmx.body = this.body;
        jmx.sysId = this.fromSystem;
        jmx.extId = this.inId;
        jmx.iteration = this.iteration;
        jmx.metaInfo = this.metaInfo;
        jmx.requestId = this.requestId;
        jmx.requestDate = this.requestDate;
        jmx.stage = this.stage;
        jmx.stageDate = this.stageDate;
    }
}