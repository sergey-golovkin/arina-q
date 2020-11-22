package arina.q.datasource;

import arina.q.camel.CamelHeaders;
import arina.q.camel.jmx.JmxQMessage;
import org.apache.camel.Exchange;

import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: Golovkin
 * Date: 11.05.17
 * Time: 19:32
 * To change this template use File | Settings | File Templates.
 */
public class QElement
{
    public String rowId;
    public long inId;
    public long outId;
    public String fromSystem;
    public String toSystem;
    public int dataType;
    public Timestamp dataDate;
    public String dataValue;
    public String metaInfo;
    public Timestamp expireDate;
    public String replaceId;

    QElement(final String rowId, final long inId, final long outId, final String fromSystem, final String toSystem, final int dataType, final String dataValue, final String metaInfo, final Timestamp dataDate, final Timestamp expireDate, final String replaceId)
    {
        this.rowId = rowId;
        this.inId = inId;
        this.outId = outId;
        this.fromSystem = fromSystem;
        this.toSystem = toSystem;
        this.dataType = dataType;
        this.dataDate = dataDate;
        this.dataValue = dataValue;
        this.metaInfo = metaInfo;
        this.expireDate = expireDate;
        this.replaceId = replaceId;
    }

    public void toExchange(Exchange exchange)
    {
        CamelHeaders.setValue(exchange, CamelHeaders.inId, this.inId);
        CamelHeaders.setValue(exchange, CamelHeaders.outId, this.outId);
        CamelHeaders.setValue(exchange, CamelHeaders.fromSystem, this.fromSystem);
        CamelHeaders.setValue(exchange, CamelHeaders.toSystem, this.toSystem);
        CamelHeaders.setValue(exchange, CamelHeaders.dataType, this.dataType);
        CamelHeaders.setValue(exchange, CamelHeaders.dataDate, this.dataDate);
        CamelHeaders.setValue(exchange, CamelHeaders.metaInfo, this.metaInfo);
        CamelHeaders.setValue(exchange, CamelHeaders.expireDate, this.expireDate);
        CamelHeaders.setValue(exchange, CamelHeaders.rowId, this.rowId);
        CamelHeaders.setValue(exchange, CamelHeaders.replaceId, this.replaceId);
        exchange.getIn().setBody(this.dataValue);
    }

    public void toJmx(JmxQMessage jmx)
    {
        jmx.inId = this.inId;
        jmx.outId = this.outId;
        jmx.fromSystem = this.fromSystem;
        jmx.toSystem = this.toSystem;
        jmx.dataType = this.dataType;
        jmx.dataDate = this.dataDate;
        jmx.dataValue = this.dataValue;
        jmx.metaInfo = this.metaInfo;
        jmx.expireDate = this.expireDate;
    }
}