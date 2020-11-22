package arina.q.camel.jmx;

import arina.utils.jmx.JMXAttribute;
import arina.utils.jmx.JMXClass;

import java.sql.Timestamp;

@JMXClass(description="Arina.Q consumer operation")
public class JmxConsumer extends JmxOperation
{
    @JMXAttribute(name = "Status", description = "Status")
    public String status;
    @JMXAttribute(name = "LoopDelay", description = "Current idle loop delay time (msec)")
    public Integer loopDelay;
    @JMXAttribute(name = "ErrorDelay", description = "Current error delay time (msec)")
    public Integer errorDelay;
    @JMXAttribute(name = "IsFailed", description = "Is failed")
    public Boolean isFailed;
    @JMXAttribute(name = "Exception", description = "Exception")
    public String exception;
    @JMXAttribute(name = "StartTime", description = "Start Date/Time")
    public Timestamp startTime;
}
