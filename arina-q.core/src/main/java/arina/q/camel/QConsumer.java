package arina.q.camel;

/**
 * Created with IntelliJ IDEA.
 * User: Golovkin
 * Date: 14.08.13
 * Time: 11:20
 * To change this template use File | Settings | File Templates.
 */

import arina.q.camel.jmx.JmxConsumer;
import arina.q.camel.jmx.JmxOperation;
import arina.q.camel.jmx.JmxQMessage;
import arina.q.camel.jmx.Stat;
import arina.q.datasource.IQDataSource;
import arina.q.datasource.QElement;
import arina.utils.LanguageUtils;
import org.apache.camel.*;
import org.apache.camel.support.DefaultConsumer;
import org.apache.commons.lang.StringUtils;
import arina.utils.jmx.AnnotatedJMXProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.management.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.URLDecoder;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QConsumer extends DefaultConsumer implements Runnable
{
    private static final String STATUS_INIT = "Initialization in progress";
    private static final String STATUS_WAIT_MSG = "Waiting new message";
    private static final String STATUS_READ_MSG = "Reading new message";
    private static final String STATUS_PROCESSING = "Processing message";
    private static final String STATUS_SLEEP = "Sleeping";
    private static final String STATUS_SHUTDOWN = "Shutdown in progress";

    private static final Logger log = LoggerFactory.getLogger(QConsumer.class);
    private static ArrayList<QConsumer> wsqConsumers = new ArrayList<>();
    private int mode;
    private String remoteSystem;
    private String systemsFilter;
    private String typesFilter;
    private String processedSubQ;
    private final Map<Thread, ThreadContext> tcMap = new ConcurrentHashMap<>();
    private Integer initialDelay;
    private Integer[] loopDelays;
    private boolean cycleLoopDelays = false;
    private Integer[] errorDelays;
    private boolean cycleErrorDelays = false;
    private IQDataSource engine;
    private boolean skipExpired;
    private String expiredWarningText;

    public class ThreadContext
    {
        public String name;
        public String system;
        public Long subQ;
        public int currentErrorDelayIndex = 0;
        public int currentLoopDelayIndex = 0;
        public JmxConsumer jmxConsumer = new JmxConsumer();
        public JmxOperation jmxGet = new JmxOperation();
        public JmxOperation jmxProcess = new JmxOperation();
        public JmxOperation jmxCommit = new JmxOperation();
        public JmxOperation jmxRollback = new JmxOperation();
        public JmxQMessage  jmxQMessage = new JmxQMessage();


        ThreadContext(String name, String system, Long subQ) throws MalformedObjectNameException,InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException
        {
            this.name = name;
            this.system = system;
            this.subQ = subQ;

            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            server.registerMBean(new AnnotatedJMXProxy(jmxConsumer), new ObjectName(name));
            server.registerMBean(new AnnotatedJMXProxy(jmxGet), new ObjectName(name + ",op=get"));
            server.registerMBean(new AnnotatedJMXProxy(jmxProcess), new ObjectName(name + ",op=process"));
            server.registerMBean(new AnnotatedJMXProxy(jmxCommit), new ObjectName(name + ",op=commit"));
            server.registerMBean(new AnnotatedJMXProxy(jmxRollback), new ObjectName(name + ",op=rollback"));
            server.registerMBean(new AnnotatedJMXProxy(jmxQMessage), new ObjectName(name + ",op=message information"));
        }

        public void destroy() throws MalformedObjectNameException, InstanceNotFoundException, MBeanRegistrationException
        {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            server.unregisterMBean(new ObjectName(name));
            server.unregisterMBean(new ObjectName(name + ",op=get"));
            server.unregisterMBean(new ObjectName(name + ",op=process"));
            server.unregisterMBean(new ObjectName(name + ",op=commit"));
            server.unregisterMBean(new ObjectName(name + ",op=rollback"));
            server.unregisterMBean(new ObjectName(name + ",op=message information"));
        }
    }

    QConsumer(QEndpoint endpoint, Processor processor, QParameters p) throws FailedToCreateConsumerException
    {
        super(endpoint, processor);

        this.engine = p.engine;
        this.systemsFilter = p.systemsFilter;
        this.typesFilter = p.typesFilter;
        this.mode = p.mode;
        this.processedSubQ = CamelHeaders.simpleName(CamelHeaders.subQ).equals(p.processedSubQ) ? "0" : p.processedSubQ;
        this.initialDelay = p.initialDelay;
	    this.remoteSystem = p.remoteSystem;
        this.skipExpired = p.skipExpired;
        this.expiredWarningText = p.expiredWarningText;

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
        {
            ArrayList<Integer> tmp = new ArrayList<>();
            for (String ed : p.errorDelays.split("[,|;]"))
            {
                if ("...".equals(ed.trim()))
                {
                    cycleErrorDelays = true;
                    break;
                } else
                    tmp.add(Integer.parseInt(ed.trim()));
            }
            this.errorDelays = tmp.toArray(new Integer[0]);
        }

    }

    private String[] getSystemsFilter()
    {
        if(StringUtils.isEmpty(systemsFilter))
            return new String[] { "*" };
        else
            return systemsFilter.split("[,|;]");
    }

    private String[] getSubQ()
    {
        if(StringUtils.isEmpty(processedSubQ))
            return new String[] { "0" };
        else
            return processedSubQ.split("[,|;]");
    }

    private String[] getTypesFilter()
    {
        if(StringUtils.isEmpty(typesFilter))
            return new String[] { "*" };
        else
            return typesFilter.split("[,|;]");
    }

    private void checkIntersection() throws FailedToStartRouteException
    {
        for(QConsumer currentConsumer : wsqConsumers)
        {
            if(currentConsumer != this && currentConsumer.engine.equals(this.engine))
            {
                for(String currentSystem : currentConsumer.getSystemsFilter())
                {
                    for(String currentType : currentConsumer.getTypesFilter())
                    {
                        currentType = currentType.trim();
                        for(String currentSubQ : currentConsumer.getSubQ())
                        {
                            currentSubQ = currentSubQ.trim();
                            for(String thisSystem : this.getSystemsFilter())
                            {
                                for(String thisType : this.getTypesFilter())
                                {
                                    thisType = thisType.trim();
                                    for(String thisSubQ : this.getSubQ())
                                    {
                                        thisSubQ = thisSubQ.trim();

                                        if((thisSystem.equals(currentSystem) || thisSystem.equals("*") || currentSystem.equals("*")) &&
                                                (thisType.equals(currentType) || thisType.equals("*") || currentType.equals("*"))
                                                && currentConsumer.mode == this.mode && currentSubQ.equals(thisSubQ))
                                            throw new FailedToStartRouteException(this.getRoute().getId(), "SYSTEM [" + currentSystem + "] and TYPE [" + currentType + "] and SUBQ [" + currentSubQ + "] used in [" + this.getEndpoint().getEndpointUri() + "] and [" + currentConsumer.getEndpoint().getEndpointUri() + "] endpoints.");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void doStart() throws Exception
    {
        checkIntersection();

        wsqConsumers.add(this);

        super.doStart();

        tcMap.clear();

        for(String system : (StringUtils.isEmpty(systemsFilter) ? new String[] { null } : systemsFilter.split("[,|;]")))
        {
            if( ! StringUtils.isEmpty(system))
                system = system.trim();

            for(String subQ : getSubQ())
            {
                Thread thread = this.getEndpoint().getCamelContext().getExecutorServiceManager().newThread(URLDecoder.decode(this.getEndpoint().getEndpointKey(), "utf8") + (StringUtils.isEmpty(system) ? "" : ",system=" + system) + ",subQ=" + subQ, this);
                this.tcMap.put(thread, new ThreadContext("arina-q:type=consumers,name=\"" + URLDecoder.decode(this.getEndpoint().getEndpointKey(), "utf8").replace("?", "\\?") + "\"" + (StringUtils.isEmpty(system) ? "" : ",system=\"system=" + system + "\"") + ",subQ=\"subQ=" + subQ + "\"", system, Long.parseLong(subQ.trim())));
                thread.start();
            }
        }
    }

    @Override
    protected void doStop() throws Exception
    {
        try
        {
            Locale.setDefault(Locale.US);

            wsqConsumers.remove(this);

            for(Thread thread : tcMap.keySet())
            {
                if( ! thread.isInterrupted() && thread.isAlive() )
                {
                    this.tcMap.get(thread).jmxConsumer.status = STATUS_SHUTDOWN;
                    thread.interrupt();
                    try { Thread.sleep(100); } catch (InterruptedException ignore){ }
                }
            }
        }
        finally
        {
            super.doStop();

            for(boolean allStopped = false; ! allStopped; )
            {
                allStopped = true;
                for(Thread thread : tcMap.keySet())
                {
                    if(thread.isAlive() )
                    {
                        allStopped = false;
                        break;
                    }
                }
                try { Thread.sleep(100); } catch (InterruptedException ignore){ }
            }

            tcMap.clear();
        }
    }

	@Override
    public void run()
    {
        ThreadContext threadContext = null;
        log.info(Thread.currentThread().getName() + " started.");

        try
        {
            threadContext = this.tcMap.get(Thread.currentThread());
            threadContext.jmxConsumer.status = STATUS_INIT;
            threadContext.jmxConsumer.startTime = new Timestamp(System.currentTimeMillis());

            Thread.sleep(initialDelay);

            for( ; ! Thread.currentThread().isInterrupted(); )
            {
                Stat stat = Stat.start();

                try
                {
                    QElement qElement = getElement(threadContext);

                    if(qElement != null)
                    {
                        Exchange exchange = processElement(threadContext, qElement);

                        if(exchange.isFailed())
                        {
                            if(exchange.getException() instanceof InterruptedException)
                                throw exchange.getException();

                            rollbackElement(threadContext, qElement, exchange);
                            threadContext.jmxConsumer.error(stat.finish());
                        }
                        else
                        {
                            commitElement(threadContext, qElement, exchange);
                            threadContext.currentErrorDelayIndex = 0;
                            threadContext.jmxConsumer.success(stat.finish());
                            continue;
                        }
//                            perfAlert(qElement, "GET", this.engine.getPerfAlertThresholdGet(), executionTimestampGet[START], executionTimestampGet[STOP]);
//                            perfAlert(qElement, "COMMIT", this.engine.getPerfAlertThresholdCommit(), executionTimestampCommit[START], executionTimestampCommit[STOP]);
//                            perfAlert(qElement, "PROCESS", this.engine.getPerfAlertThresholdProcessMessage(), executionTimestampProcess[START], executionTimestampProcess[STOP]);
//                            perfAlert(qElement, "ALL", this.engine.getPerfAlertThresholdAll(), executionTimestampAll[START], executionTimestampAll[STOP]);
                    }
                    else
                    {
                        loopSleep(threadContext);
                        continue;
                    }
                }
                catch (InterruptedException ignore)
                {
                    return;
                }
                catch (Exception ex)
                {
                    log.error(Thread.currentThread().getName() + " EXCEPTION:", ex);
                    threadContext.jmxConsumer.isFailed = true;
                    threadContext.jmxConsumer.exception = printStackTrace(ex);
                    threadContext.jmxConsumer.error(stat.finish());
                }

                errorSleep(threadContext);
            }
        }
        catch (InterruptedException ignore)
        {
        }
        catch (Exception ex)
        {
            log.error(Thread.currentThread().getName() + " EXCEPTION:", ex);
        }
        finally
        {
            log.info(Thread.currentThread().getName() + " stopped");
            try
            {
                if(threadContext != null)
                    threadContext.destroy();
            }
            catch (Exception ignore)
            {
            }
        }
    }

    private String printStackTrace(Exception ex)
    {
        PrintWriter pw = null;
        try
        {
            StringWriter sw = new StringWriter();
            pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            return sw.toString();
        }
        catch (Exception ignore){ return "oops!"; }
        finally
        {
            try { pw.close(); } catch (Exception ignore){ }
        }
    }

    private QElement getElement(ThreadContext threadContext) throws Exception
    {
        Stat stat = Stat.start();
        threadContext.jmxConsumer.status = STATUS_READ_MSG;

        try
        {
            QElement e;
            if (mode == QComponent.modeDirect)
                e = this.engine.getIn(this.remoteSystem, threadContext.system, this.typesFilter, threadContext.subQ);
            else
                e = this.engine.getOut(this.remoteSystem, threadContext.system, this.typesFilter);

            threadContext.jmxGet.success(stat.finish());
            return e;
        }
        catch (InterruptedException e)
        {
            threadContext.jmxGet.success(stat.finish());
            throw e;
        }
        catch (Exception e)
        {
            threadContext.jmxGet.error(stat.finish());
            throw e;
        }
    }

    private Exchange processElement(ThreadContext threadContext, QElement qElement) throws InterruptedException
    {
        Stat stat = Stat.start();
        threadContext.jmxConsumer.status = STATUS_PROCESSING;

        Exchange exchange = this.getEndpoint().createExchange(ExchangePattern.InOnly);

        try
        {
            threadContext.currentLoopDelayIndex = 0;
            threadContext.jmxConsumer.loopDelay = this.loopDelays[threadContext.currentLoopDelayIndex];
            threadContext.jmxConsumer.errorDelay = this.errorDelays[threadContext.currentErrorDelayIndex];
            threadContext.jmxConsumer.isFailed = false;
            threadContext.jmxConsumer.exception = null;
            threadContext.jmxQMessage.processingStartAt = new Timestamp(System.currentTimeMillis());
            threadContext.jmxQMessage.processingEndAt = null;
            threadContext.jmxQMessage.processingDuration = null;

            qElement.toExchange(exchange);
            qElement.toJmx(threadContext.jmxQMessage);

            if(qElement.expireDate != null && this.skipExpired && System.currentTimeMillis() > qElement.expireDate.getTime())
                CamelHeaders.setValue(exchange, CamelHeaders.errorText, LanguageUtils.evaluate(exchange, this.expiredWarningText, String.class));
            else
                this.getProcessor().process(exchange);

            if(exchange.isFailed())
                threadContext.jmxProcess.error(stat.finish());
            else
                threadContext.jmxProcess.success(stat.finish());
        }
        catch (InterruptedException e)
        {
            threadContext.jmxProcess.success(stat.finish());
            throw e;
        }
        catch (Exception e)
        {
            exchange.setException(e);
            threadContext.jmxProcess.error(stat.finish());
        }
        finally
        {
            threadContext.jmxQMessage.processingEndAt = new Timestamp(System.currentTimeMillis());
            threadContext.jmxQMessage.processingDuration = threadContext.jmxQMessage.processingEndAt.getTime() - threadContext.jmxQMessage.processingStartAt.getTime();
        }
        return exchange;
    }

    private void rollbackElement(ThreadContext threadContext, QElement qElement, Exchange exchange) throws Exception
    {
        Stat stat = Stat.start();

        log.error(Thread.currentThread().getName() + " EXCEPTION:", exchange.getException());

        try
        {
            String error = printStackTrace(exchange.getException());
            threadContext.jmxConsumer.isFailed = true;
            threadContext.jmxConsumer.exception = error;

            if(mode == QComponent.modeDirect)
                this.engine.rollbackIn(this.remoteSystem, qElement.inId, -1, error, qElement.rowId);
            else
                this.engine.rollbackOut(this.remoteSystem, qElement.outId, -1, error, qElement.rowId);

            threadContext.jmxRollback.success(stat.finish());
        }
        catch (InterruptedException e)
        {
            threadContext.jmxRollback.success(stat.finish());
            throw e;
        }
        catch (Exception e)
        {
            threadContext.jmxRollback.error(stat.finish());
            throw e;
        }
    }

    private void commitElement(ThreadContext threadContext, QElement qElement, Exchange exchange) throws Exception
    {
        Boolean skip = CamelHeaders.getValue(exchange, CamelHeaders.skipMessage, Boolean.class);
        if(skip == null || !skip)
        {
            Stat stat = Stat.start();
            try
            {
                if (mode == QComponent.modeDirect)
                    this.engine.commitIn(this.remoteSystem, qElement.inId, qElement.dataDate, CamelHeaders.getValue(exchange, CamelHeaders.errorText, String.class), qElement.rowId);
                else
                    this.engine.commitOut(this.remoteSystem, qElement.outId, CamelHeaders.getValue(exchange, CamelHeaders.errorText, String.class), qElement.rowId);

                threadContext.jmxCommit.success(stat.finish());
            }
            catch (InterruptedException e)
            {
                threadContext.jmxCommit.success(stat.finish());
                throw e;
            }
            catch (Exception e)
            {
                threadContext.jmxCommit.error(stat.finish());
                throw e;
            }
        }
    }

    private void loopSleep(ThreadContext tc) throws InterruptedException
    {
        if( ! Thread.currentThread().isInterrupted())
        {
            tc.jmxConsumer.status = STATUS_WAIT_MSG;
            tc.jmxConsumer.loopDelay = this.loopDelays[tc.currentLoopDelayIndex];

            try
            {
                Thread.sleep(this.loopDelays[tc.currentLoopDelayIndex]);
            }
            finally
            {
                if (this.cycleLoopDelays)
                    tc.currentLoopDelayIndex = (++tc.currentLoopDelayIndex) % this.loopDelays.length;
                else if (tc.currentLoopDelayIndex < this.loopDelays.length - 1)
                    tc.currentLoopDelayIndex++;
            }
        }
    }

    private void errorSleep(ThreadContext tc) throws InterruptedException
    {
        if( ! Thread.currentThread().isInterrupted())
        {
            tc.jmxConsumer.status = STATUS_SLEEP;
            tc.jmxConsumer.errorDelay = this.errorDelays[tc.currentErrorDelayIndex];

            try
            {
                Thread.sleep(this.errorDelays[tc.currentErrorDelayIndex]);
            }
            finally
            {
                if (this.cycleErrorDelays)
                    tc.currentErrorDelayIndex = (++tc.currentErrorDelayIndex) % this.errorDelays.length;
                else if (tc.currentErrorDelayIndex < this.errorDelays.length - 1)
                    tc.currentErrorDelayIndex++;
            }
        }
    }

    private void perfAlert(QElement qElement, String text, long threshold, long start, long stop)
    {
        if(log.isInfoEnabled() && qElement != null && threshold != 0 && (stop - start) > threshold)
            log.info("PERF ALERT: \t" + text + "\t" + (this.mode == QComponent.modeDirect ? "IN" : "OUT") + "\t" + (stop - start) + "\t" + (this.mode == QComponent.modeDirect ? qElement.inId : qElement.outId));
    }
}

