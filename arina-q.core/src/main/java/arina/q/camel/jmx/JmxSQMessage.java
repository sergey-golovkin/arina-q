package arina.q.camel.jmx;

import arina.q.camel.CamelHeaders;
import arina.utils.jmx.JMXAttribute;
import arina.utils.jmx.JMXClass;

import java.sql.Timestamp;

@JMXClass(description="Arina.Q queue message")
public class JmxSQMessage
{
    @JMXAttribute(name="ProcessingStartTime", description = "Processing start time")
    public Timestamp processingStartAt;
    @JMXAttribute(name="ProcessingEndTime", description = "Processing end time")
    public Timestamp processingEndAt;
    @JMXAttribute(name="ProcessingDuration", description = "Processing duration (msec)")
    public Long processingDuration;
    @JMXAttribute(name="ExternalId", description = "ext_id")
    public String extId;
    @JMXAttribute(name="FromSystem", description = "sys_id")
    public String sysId;
    @JMXAttribute(name="StageDate", description = "stage_date")
    public Timestamp stageDate;
    @JMXAttribute(name="RequestId", description = "request_id")
    public long requestId;
    @JMXAttribute(name="RequestDate", description = "request_date")
    public Timestamp requestDate;
    @JMXAttribute(name="Stage", description = "stage")
    public String stage;
    @JMXAttribute(name="Body", description = "data_value")
    public String body;
    @JMXAttribute(name="Iteration", description = "iteration")
    public Long iteration;
    @JMXAttribute(name="MetaInformation", description = "meta_info")
    public String metaInfo;
}
