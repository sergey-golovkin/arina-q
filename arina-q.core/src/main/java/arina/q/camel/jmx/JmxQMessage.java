package arina.q.camel.jmx;

import arina.utils.jmx.JMXAttribute;
import arina.utils.jmx.JMXClass;

import java.sql.Timestamp;

@JMXClass(description="Arina.Q queue message")
public class JmxQMessage
{
    @JMXAttribute(name="ProcessingStartTime", description = "Processing start time")
    public Timestamp processingStartAt;
    @JMXAttribute(name="ProcessingEndTime", description = "Processing end time")
    public Timestamp processingEndAt;
    @JMXAttribute(name="ProcessingDuration", description = "Processing duration (msec)")
    public Long processingDuration;
    @JMXAttribute(name="DataInId", description = "data_in_id")
    public Long inId;
    @JMXAttribute(name="DataOutId", description = "data_out_id")
    public Long outId;
    @JMXAttribute(name="FromSystem", description = "from_system")
    public String fromSystem;
    @JMXAttribute(name="ToSystem", description = "to_system")
    public String toSystem;
    @JMXAttribute(name="DataType", description = "data_type")
    public Integer dataType;
    @JMXAttribute(name="RecordDate", description = "record_date")
    public Timestamp dataDate;
    @JMXAttribute(name="Body", description = "data_value")
    public String dataValue;
    @JMXAttribute(name="SubQ", description = "Sub Q")
    public Long subQ;
    @JMXAttribute(name="MetaInformation", description = "meta_info")
    public String metaInfo;
    @JMXAttribute(name="ExpiredTime", description = "Message expire time")
    public Timestamp expireDate;
}
