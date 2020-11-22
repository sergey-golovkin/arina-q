package arina.q.camel.jmx;

import arina.utils.jmx.JMXAttribute;
import arina.utils.jmx.JMXClass;

import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicLong;

@JMXClass(description="Arina.Q operation")
public class JmxOperation
{
    @JMXAttribute(name = "TotalTime", description = "Total time (msec)")
    public AtomicLong totalTime = new AtomicLong(0);
    @JMXAttribute(name = "TotalCount", description = "Total count")
    public AtomicLong totalCount = new AtomicLong(0);
    @JMXAttribute(name = "AverageProcessingDuration", description = "Average processing duration (msec)")
    public double avgTime = 0;
    @JMXAttribute(name = "LastProcessingDuration", description = "Last processing duration (msec)")
    public AtomicLong lastTime = new AtomicLong(0);
    @JMXAttribute(name = "TotalSuccessCount", description = "Total success count")
    public AtomicLong totalSuccessCount = new AtomicLong(0);
    @JMXAttribute(name = "TotalErrorsCount", description = "Total errors count")
    public AtomicLong totalErrorsCount = new AtomicLong(0);
    @JMXAttribute(name = "LastSuccessTime", description = "Last Success Date/Time")
    public Timestamp lastSuccessTime;
    @JMXAttribute(name = "LastErrorTime", description = "Last Error Date/Time")
    public Timestamp lastErrorTime;

    public void success(Stat stat, long count)
    {
        this.totalCount.addAndGet(count);
        this.totalTime.addAndGet(stat.getDuration());
        this.avgTime = totalTime.doubleValue() / totalCount.doubleValue();
        this.lastTime.set(stat.getDuration());
        this.totalSuccessCount.addAndGet(count);
        this.lastSuccessTime = new Timestamp(stat.getFinish());
    }

    public void success(Stat stat)
    {
        success(stat, 1);
    }

    public void error(Stat stat)
    {
        this.totalCount.incrementAndGet();
        this.totalTime.addAndGet(stat.getDuration());
        this.avgTime = totalTime.doubleValue() / totalCount.doubleValue();
        this.lastTime.set(stat.getDuration());
        this.totalErrorsCount.incrementAndGet();
        this.lastErrorTime = new Timestamp(stat.getFinish());
    }
}
