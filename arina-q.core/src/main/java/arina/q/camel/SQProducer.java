package arina.q.camel;

/**
 * Created with IntelliJ IDEA.
 * User: Golovkin
 * Date: 14.08.13
 * Time: 11:20
 * To change this template use File | Settings | File Templates.
 */

import arina.q.camel.jmx.JmxOperation;
import arina.q.camel.jmx.Stat;
import arina.q.datasource.IQDataSource;
import arina.q.datasource.SQElement;
import arina.utils.LanguageUtils;
import arina.utils.jmx.AnnotatedJMXProxy;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.net.URLDecoder;
import java.util.ArrayList;

public class SQProducer extends DefaultProducer
{
    private static final Logger log = LoggerFactory.getLogger(SQProducer.class);
    private IQDataSource engine;
    private String messageBody;
    private String remoteSystem;
    private String stage;
    private long delay;
    private Integer[] loopDelays;
    private boolean cycleLoopDelays = false;
    private String isFinal;
    private String depId;
    private String parentDepId;
    private String metaInfo;
    private String waitFinal;
    public JmxOperation jmxOperation = new JmxOperation();
    private ObjectName objectName;

    SQProducer(SQEndpoint endpoint, SQParameters p)
    {
        super(endpoint);

        this.messageBody = p.messageBody;
        this.remoteSystem = p.remoteSystem;
        this.stage = p.stage;
        this.delay = p.delay;
        this.isFinal = p.isFinal;
        this.engine = p.engine;
        this.depId = p.depId;
        this.parentDepId = p.parentDepId;
        this.metaInfo = p.metaInfo;
        this.waitFinal = p.waitFinal;

        {
            ArrayList<Integer> tmp = new ArrayList<>();
            for (String ed : p.loopDelays.split("[,|;]"))
            {
                if ("...".equals(ed.trim()))
                {
                    cycleLoopDelays = true;
                    break;
                } else
                    tmp.add(Integer.parseInt(ed.trim()));
            }
            this.loopDelays = tmp.toArray(new Integer[0]);
        }
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try
        {
            objectName = new ObjectName("arina-sq :type=processors,name=\"" + URLDecoder.decode(this.getEndpoint().getEndpointKey(), "utf8").replace("?", "\\?") + "\"");
            server.registerMBean(new AnnotatedJMXProxy(jmxOperation), objectName);
        }
        catch (InstanceAlreadyExistsException e)
        {
            objectName = new ObjectName("arina-sq:type=processors,name=\"" + URLDecoder.decode(this.getEndpoint().getEndpointKey(), "utf8").replace("?", "\\?") + "\",threadId=\"threadId=" + Thread.currentThread().getId() + "\"");
            server.registerMBean(new AnnotatedJMXProxy(jmxOperation), objectName);
        }
    }

    @Override
    protected void doStop() throws Exception
    {
        try
        {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            server.unregisterMBean(objectName);
        }
        catch (Exception ignore)
        {
        }

        super.doStop();
    }

    public void process(final Exchange exchange) throws Exception
    {
        Stat stat = Stat.start();
        int currentLoopDelayIndex = 0;
        String stg = LanguageUtils.evaluate(exchange, this.stage, String.class);

        if(StringUtils.isEmpty(stg))
            throw new Exception("Stage not specified!");

        String fromSystem = CamelHeaders.getValue(exchange, CamelHeaders.fromSystem, String.class);
        Long requestId = CamelHeaders.getValue(exchange, CamelHeaders.requestId, Long.class);
        Long pad = CamelHeaders.getValue(exchange, CamelHeaders.processAfterDelay, Long.class);
        if(pad == null)
            pad = delay;

        try
        {
            if(log.isTraceEnabled())
            {
                if(requestId != null)
                    log.trace("stage="  + stg + " " + Thread.currentThread().getName() + " SQProducer.process.start " + requestId + " " + System.currentTimeMillis());
                else
                    log.trace("stage="  + stg + " " + Thread.currentThread().getName() + " SQProducer.process.start NULL " + System.currentTimeMillis());
            }

            requestId = engine.putStage(
                this.remoteSystem,
                CamelHeaders.getValue(exchange, CamelHeaders.inId, String.class),
                fromSystem,
                requestId,
                LanguageUtils.evaluate(exchange, this.messageBody, String.class),
                stg,
                pad,
                LanguageUtils.evaluate(exchange, this.isFinal, Boolean.class),
                LanguageUtils.evaluate(exchange, this.depId, String.class),
                LanguageUtils.evaluate(exchange, this.parentDepId, String.class),
                LanguageUtils.evaluate(exchange, this.metaInfo, String.class)
            );

            CamelHeaders.setValue(exchange, CamelHeaders.requestId, requestId);

            long waitTime = LanguageUtils.evaluate(exchange, this.waitFinal, Long.class);
            for(long startTs = System.currentTimeMillis() ; ! Thread.currentThread().isInterrupted() && System.currentTimeMillis() - startTs < waitTime; )
            {
                SQElement element = engine.getFinalStage(this.remoteSystem, requestId);
                if(element != null)
                {
                    element.toExchange(exchange);
                    CamelHeaders.setValue(exchange, CamelHeaders.waitFinalTimeout, false);
                    return;
                }
                else
                {
                    try
                    {
                        Thread.sleep(this.loopDelays[currentLoopDelayIndex]);
                    }
                    finally
                    {
                        if(this.cycleLoopDelays)
                            currentLoopDelayIndex = (++currentLoopDelayIndex) % this.loopDelays.length;
                        else
                        if(currentLoopDelayIndex< this.loopDelays.length - 1)
                            currentLoopDelayIndex++;
                    }
                }
            }
            CamelHeaders.setValue(exchange, CamelHeaders.waitFinalTimeout, true);
            jmxOperation.success(stat.finish());
        }
        catch (Exception e)
        {
            jmxOperation.error(stat.finish());
            throw e;
        }
        finally
        {
            if(log.isTraceEnabled())
            {
                if(requestId != null)
                    log.trace("stage="  + stg + " " + Thread.currentThread().getName() + " SQProducer.process.end " + requestId + " " + System.currentTimeMillis());
                else
                    log.trace("stage="  + stg + " " + Thread.currentThread().getName() + " SQProducer.process.end NULL " + System.currentTimeMillis());
            }
        }
    }
}
