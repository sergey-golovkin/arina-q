package arina.q.camel;

/**
 * Created with IntelliJ IDEA.
 * User: Golovkin
 * Date: 14.08.13
 * Time: 11:20
 * To change this template use File | Settings | File Templates.
 */

import arina.q.camel.jmx.*;
import arina.q.datasource.IQDataSource;
import arina.q.datasource.SQElement;
import arina.utils.jmx.AnnotatedJMXProxy;
import org.apache.camel.*;
import org.apache.camel.support.DefaultConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.management.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class SQConsumer extends DefaultConsumer implements Runnable
{
    private static final String STATUS_INIT = "Initialization in progress";
    private static final String STATUS_WAIT_MSG = "Waiting new message";
    private static final String STATUS_READ_MSG = "Reading new message";
    private static final String STATUS_PROCESSING = "Processing message";
    private static final String STATUS_SLEEP = "Sleeping";
    private static final String STATUS_SHUTDOWN = "Shutdown in progress";

    private static final Logger log = LoggerFactory.getLogger(SQConsumer.class);
    private static final int qSizeMultiplier = 5;
    private Thread[] workers = null;
    private String remoteSystem;
    private String stage;
    private long delay;
    private String filter;
    private Map<Long, Long> used = new ConcurrentHashMap<>();
    private BlockingQueue<SQElement> q;
    private int threads;
    private Integer initialDelay;
    private Integer[] loopDelays;
    private boolean cycleLoopDelays = false;
    private Integer[] errorDelays;
    private boolean cycleErrorDelays = false;
    private IQDataSource engine;

    SQConsumer(SQEndpoint endpoint, Processor processor, SQParameters p) throws Exception
    {
        super(endpoint, processor);

        if(p.stage == null)
            throw new Exception("Stage not specified!");

        this.remoteSystem = p.remoteSystem;
        this.stage = p.stage;
        this.delay = p.delay;
        this.filter = p.systemsFilter;
        this.threads = p.threads;
        this.initialDelay = p.initialDelay;
        this.engine = p.engine;

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

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        this.workers = new Thread[this.threads + 1];
        this.q = new ArrayBlockingQueue<>(this.threads * qSizeMultiplier, true);

        this.workers[0] = this.getEndpoint().getCamelContext().getExecutorServiceManager().newThread(URLDecoder.decode(this.getEndpoint().getEndpointKey(), "utf8") + ",thread=reader", () ->
        {
            int currentErrorDelayIndex = 0;
            int currentLoopDelayIndex = 0;
            JmxConsumer jmxConsumer = new JmxConsumer();
            JmxOperation jmxGet = new JmxOperation();
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            String name = "";

            try
            {

                try
                {
                    name = "arina-sq:type=consumers,name=\"" + URLDecoder.decode(this.getEndpoint().getEndpointKey(), "utf8").replace("?", "\\?") + "\"";
                    server.registerMBean(new AnnotatedJMXProxy(jmxConsumer), new ObjectName(name+ ",thread=\"thread=reader\""));
                    server.registerMBean(new AnnotatedJMXProxy(jmxGet), new ObjectName(name + ",thread=\"thread=reader\",op=get"));
                }
                catch (Exception ignore)
                {
                }

                jmxConsumer.status = STATUS_INIT;
                jmxConsumer.startTime = new Timestamp(System.currentTimeMillis());

                Thread.sleep(initialDelay);

                for( ; ! Thread.currentThread().isInterrupted(); )
                {
                    Stat stat = null;

                    try
                    {
                        if(log.isTraceEnabled())
                            log.trace("stage="  + stage + " " + Thread.currentThread().getName() + " " + System.currentTimeMillis());

                        if( ! q.isEmpty())
                        {
                            synchronized (q)
                            {
                                q.wait();
                            }
                        }

                        stat = Stat.start();
                        long readTimeStamp = System.currentTimeMillis();
                        List<SQElement> rows = getElements(jmxConsumer, jmxGet, readTimeStamp);

                        if(log.isTraceEnabled())
                            this.used.entrySet().forEach(e ->
                                {
                                    if(e.getValue() < readTimeStamp)
                                        log.trace("stage="  + stage + " " + Thread.currentThread().getName() + " remove processed requestId=" + e.getKey() + " => " + e.getValue() + " readTimeStamp=" + readTimeStamp);
                                });

                        this.used.entrySet().removeIf(e -> e.getValue() < readTimeStamp);

                        long count = 0;
                        for(SQElement row : rows)
                        {
                            if(log.isTraceEnabled())
                                log.trace("stage="  + stage + " " + Thread.currentThread().getName() + " read from datasource requestId=" + row.requestId + " stageDate=" + row.stageDate.getTime() + " " + System.currentTimeMillis());

                            if( ! this.used.containsKey(row.requestId))
                            {
                                if(log.isTraceEnabled())
                                    log.trace("stage="  + stage + " " + Thread.currentThread().getName() + " before put to q requestId=" + row.requestId + " stageDate=" + row.stageDate.getTime() + " " + System.currentTimeMillis());

                                this.used.put(row.requestId, Long.MAX_VALUE);
                                q.put(row);
                                count++;

                                if(log.isTraceEnabled())
                                    log.trace("stage="  + stage + " " + Thread.currentThread().getName() + " after put to q requestId=" + row.requestId + " stageDate=" + row.stageDate.getTime() + " " + System.currentTimeMillis());

                                currentLoopDelayIndex = 0;
                            }
                        }

                        currentErrorDelayIndex = 0;
                        jmxConsumer.status = STATUS_PROCESSING;
                        if(count > 0)
                            jmxConsumer.success(stat.finish(), count);

                        if(q.isEmpty())
                            currentLoopDelayIndex = loopSleep(jmxConsumer, currentLoopDelayIndex);
                    }
                    catch (InterruptedException ignore)
                    {
                        return;
                    }
                    catch (Exception ex)
                    {
                        log.error(Thread.currentThread().getName() + " EXCEPTION:", ex);
                        if(stat != null)
                            jmxConsumer.error(stat.finish());

                        currentErrorDelayIndex = errorSleep(jmxConsumer, currentErrorDelayIndex);
                    }
                }
            }
            catch (InterruptedException ignore)
            {
            }
            finally
            {
                try
                {
                    server.unregisterMBean(new ObjectName(name+ ",thread=\"thread=reader\""));
                    server.unregisterMBean(new ObjectName(name + ",thread=\"thread=reader\",op=get"));
                }
                catch (Exception ignore)
                {
                }
            }
        });

        for(int i = 1; i <= this.threads; i++)
            this.workers[i] = this.getEndpoint().getCamelContext().getExecutorServiceManager().newThread(URLDecoder.decode(this.getEndpoint().getEndpointKey(), "utf8") + ",thread=\"thread=" + i + "\"", this);

        for(Thread thread : this.workers)
            thread.start();
    }

    private List<SQElement> getElements(JmxConsumer jmxConsumer, JmxOperation jmxOperation, long readTimeStamp) throws Exception
    {
        Stat stat = Stat.start();

        try
        {
            if (log.isTraceEnabled())
                log.trace("stage=" + stage + " " + Thread.currentThread().getName() + " before read from datasource " + readTimeStamp);

            jmxConsumer.status = STATUS_READ_MSG;

            List<SQElement> rows = engine.getStage(remoteSystem, stage, filter, threads * qSizeMultiplier);

            if (log.isTraceEnabled())
                log.trace("stage=" + stage + " " + Thread.currentThread().getName() + " after read from datasource " + System.currentTimeMillis());

            jmxOperation.success(stat.finish());
            return rows;
        }
        catch (InterruptedException e)
        {
            jmxOperation.success(stat.finish());
            throw e;
        }
        catch (Exception e)
        {
            jmxOperation.error(stat.finish());
            throw e;
        }
    }

    @Override
    protected void doStop() throws Exception
    {
        try
        {
            for(Thread thread : this.workers)
            {
                if( ! thread.isInterrupted() && thread.isAlive() )
                    thread.interrupt();
            }
        }
        finally
        {
            super.doStop();

            for(boolean allStopped = false; ! allStopped; )
            {
                allStopped = true;
                for(Thread thread : this.workers)
                {
                    if(thread.isAlive() )
                    {
                        allStopped = false;
                        break;
                    }
                }
                try { Thread.sleep(100); } catch (InterruptedException ignore){ }
            }

            this.workers = null;
        }
    }

    @Override
    public void run()
    {
        int currentErrorDelayIndex = 0;
        JmxConsumer jmxConsumer = new JmxConsumer();
        JmxOperation jmxProcess = new JmxOperation();
        JmxOperation jmxCommit = new JmxOperation();
        JmxOperation jmxRollback = new JmxOperation();
        JmxSQMessage jmxSQMessage = new JmxSQMessage();

        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        String name = "";

        try
        {
            try
            {
                for (int i = 0; i < this.workers.length; i++)
                    if(this.workers[i] == Thread.currentThread())
                    {
                        name = "arina-sq:type=consumers,name=\"" + URLDecoder.decode(this.getEndpoint().getEndpointKey(), "utf8").replace("?", "\\?") + "\",thread=\"thread=" + i + "\"";
                        break;
                    }
                server.registerMBean(new AnnotatedJMXProxy(jmxConsumer), new ObjectName(name));
                server.registerMBean(new AnnotatedJMXProxy(jmxProcess), new ObjectName(name + ",op=process"));
                server.registerMBean(new AnnotatedJMXProxy(jmxCommit), new ObjectName(name + ",op=commit"));
                server.registerMBean(new AnnotatedJMXProxy(jmxRollback), new ObjectName(name + ",op=rollback"));
                server.registerMBean(new AnnotatedJMXProxy(jmxSQMessage), new ObjectName(name + ",op=message information"));
            }
            catch (Exception ignore)
            {
            }

            jmxConsumer.status = STATUS_INIT;
            jmxConsumer.startTime = new Timestamp(System.currentTimeMillis());

            Thread.sleep(this.initialDelay);

            for( ; ! Thread.currentThread().isInterrupted(); )
            {
	            SQElement element = null;
                Stat stat = null;

                try
                {
                    jmxConsumer.status = STATUS_WAIT_MSG;
                    element = next();
                    if(log.isTraceEnabled())
                        log.trace("stage="  + this.stage + " " + Thread.currentThread().getName() + " read from q requestId=" + element.requestId + " stageDate=" + element.stageDate.getTime() + " " + System.currentTimeMillis());

                    stat = Stat.start();

                    Exchange exchange = processElement(jmxConsumer, jmxProcess, jmxSQMessage, element);

                    if(exchange.isFailed())
                    {
                        if(exchange.getException() instanceof InterruptedException)
                            throw exchange.getException();

                        jmxConsumer.isFailed = true;
                        jmxConsumer.exception = printStackTrace(exchange.getException());
                        rollbackElement(jmxRollback, element, exchange);
                        jmxConsumer.error(stat.finish());
                    }
                    else
                    {
                        commitElement(jmxCommit, element, exchange);
                        currentErrorDelayIndex = 0;
                        jmxConsumer.success(stat.finish());
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
                    jmxConsumer.isFailed = true;
                    jmxConsumer.exception = printStackTrace(ex);
                    if(stat != null)
                        jmxConsumer.error(stat.finish());
                }
	            finally
                {
	                if(element != null)
	                {
		                long processedTimeStamp = System.currentTimeMillis();

		                if(log.isTraceEnabled())
			                log.trace("stage="  + this.stage + " " + Thread.currentThread().getName() + " mark as processed requestId=" + element.requestId + " stageDate=" + element.stageDate.getTime() + " " + processedTimeStamp);

	                    this.used.replace(element.requestId, processedTimeStamp);
	                }
                }

                currentErrorDelayIndex = errorSleep(jmxConsumer, currentErrorDelayIndex);
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
            try
            {
                server.unregisterMBean(new ObjectName(name));
                server.unregisterMBean(new ObjectName(name + ",op=process"));
                server.unregisterMBean(new ObjectName(name + ",op=commit"));
                server.unregisterMBean(new ObjectName(name + ",op=rollback"));
                server.unregisterMBean(new ObjectName(name + ",op=message information"));
            }
            catch (Exception ignore)
            {
            }
        }
    }

    private Exchange processElement(JmxConsumer jmxConsumer, JmxOperation jmxProcess, JmxSQMessage jmxSQMessage, SQElement element) throws InterruptedException
    {
        Stat stat = Stat.start();
        jmxConsumer.status = STATUS_PROCESSING;

        Exchange exchange = getEndpoint().createExchange(ExchangePattern.InOnly);

        jmxConsumer.isFailed = false;
        jmxConsumer.exception = null;
        jmxSQMessage.processingStartAt = new Timestamp(System.currentTimeMillis());
        jmxSQMessage.processingEndAt = null;
        jmxSQMessage.processingDuration = null;

        element.toExchange(exchange);
        element.toJmx(jmxSQMessage);

        try
        {
            if(log.isTraceEnabled())
                log.trace("stage="  + this.stage + " " + Thread.currentThread().getName() + " before process requestId=" + element.requestId + " stageDate=" + element.stageDate.getTime() + " " + System.currentTimeMillis());

            this.getProcessor().process(exchange);

            if(log.isTraceEnabled())
                log.trace("stage="  + this.stage + " " + Thread.currentThread().getName() + " after process requestId=" + element.requestId + " stageDate=" + element.stageDate.getTime() + " " + System.currentTimeMillis());

            jmxProcess.success(stat.finish());
        }
        catch (InterruptedException e)
        {
            jmxProcess.success(stat.finish());
            throw e;
        }
        catch (Exception ex)
        {
            if(log.isTraceEnabled())
                log.trace("stage="  + this.stage + " " + Thread.currentThread().getName() + " exception process requestId=" + element.requestId + " stageDate=" + element.stageDate.getTime() + " " + System.currentTimeMillis());

            exchange.setException(ex);
        }
        finally
        {
            jmxSQMessage.processingEndAt = new Timestamp(System.currentTimeMillis());
            jmxSQMessage.processingDuration = jmxSQMessage.processingEndAt.getTime() - jmxSQMessage.processingStartAt.getTime();
        }
        return exchange;
    }

    private void rollbackElement(JmxOperation jmx, SQElement element, Exchange exchange) throws Exception
    {
        Stat stat = Stat.start();
        try
        {
            log.error(Thread.currentThread().getName() + " EXCEPTION:", exchange.getException());
            rollback(element, exchange);
            jmx.error(stat.finish());
        }
        catch (InterruptedException e)
        {
            jmx.success(stat.finish());
            throw e;
        }
        catch (Exception e)
        {
            jmx.error(stat.finish());
            throw e;
        }
    }

    private void commitElement(JmxOperation jmx, SQElement element, Exchange exchange) throws Exception
    {
        Stat stat = Stat.start();

        try
        {
            commit(element, exchange);
            jmx.success(stat.finish());
        }
        catch (InterruptedException e)
        {
            jmx.success(stat.finish());
            throw e;
        }
        catch (Exception e)
        {
            jmx.error(stat.finish());
            throw e;
        }
    }

    private SQElement next() throws InterruptedException
    {
        if(q.isEmpty())
            synchronized (q)
            {
                q.notify();
            }

            return this.q.take();
        }

    private void rollback(SQElement SQElement, Exchange exchange) throws Exception
    {
        PrintWriter pw = null;

        try
        {
            StringWriter sw = new StringWriter();
            pw = new PrintWriter(sw);
            exchange.getException().printStackTrace(pw);
            String error = sw.toString();

            if(log.isTraceEnabled())
                log.trace("stage="  + this.stage + " " + Thread.currentThread().getName() + " before rollback requestId=" + SQElement.requestId + " stageDate=" + SQElement.stageDate.getTime() + " " + System.currentTimeMillis());

            this.engine.rollbackStage(
                    this.remoteSystem,
                    SQElement.stageDate,
                    SQElement.requestId,
                    -1,
                    error,
                    SQElement.rowId);

            if(log.isTraceEnabled())
                log.trace("stage="  + this.stage + " " + Thread.currentThread().getName() + " after rollback requestId=" + SQElement.requestId + " stageDate=" + SQElement.stageDate.getTime() + " " + System.currentTimeMillis());
        }
        finally
        {
            try { pw.close(); } catch (Exception ignore){ }
        }
    }

    private void commit(SQElement SQElement, Exchange exchange) throws Exception
    {
        if(log.isTraceEnabled())
            log.trace("stage="  + this.stage + " " + Thread.currentThread().getName() + " before commit requestId=" + SQElement.requestId + " stageDate=" + SQElement.stageDate.getTime() + " " + System.currentTimeMillis());

        Integer errorCode = CamelHeaders.getValue(exchange, CamelHeaders.errorCode, Integer.class);
        if(errorCode == null)
            errorCode = 0;

        Long pad = CamelHeaders.getValue(exchange, CamelHeaders.processAfterDelay, Long.class);
        if(pad == null)
            pad = delay;

        this.engine.commitStage(
                this.remoteSystem,
                SQElement.stageDate,
                SQElement.requestId,
                errorCode,
                exchange.getIn().getBody(String.class),
                pad,
                SQElement.rowId);

        if(log.isTraceEnabled())
            log.trace("stage="  + this.stage + " " + Thread.currentThread().getName() + " after commit requestId=" + SQElement.requestId + " stageDate=" + SQElement.stageDate.getTime() + " " + System.currentTimeMillis());
    }

    private int loopSleep(JmxConsumer jmx, int currentDelayIndex) throws InterruptedException
    {
        if( ! Thread.currentThread().isInterrupted())
        {
            jmx.status = STATUS_WAIT_MSG;
            jmx.loopDelay = this.loopDelays[currentDelayIndex];

            try
            {
                Thread.sleep(this.loopDelays[currentDelayIndex]);
            }
            finally
            {
                if (this.cycleLoopDelays)
                    currentDelayIndex = (++currentDelayIndex) % this.loopDelays.length;
                else if (currentDelayIndex < this.loopDelays.length - 1)
                    currentDelayIndex++;
            }

            return currentDelayIndex;
        }
        else
            return 0;
    }

    private int errorSleep(JmxConsumer jmx, int currentDelayIndex) throws InterruptedException
    {
        if( ! Thread.currentThread().isInterrupted())
        {
            jmx.status = STATUS_SLEEP;
            jmx.errorDelay = this.errorDelays[currentDelayIndex];

            try
            {
                Thread.sleep(this.errorDelays[currentDelayIndex]);
            }
            finally
            {
                if(this.cycleErrorDelays)
                    currentDelayIndex = (++currentDelayIndex) % this.errorDelays.length;
                else
                if(currentDelayIndex < this.errorDelays.length - 1)
                    currentDelayIndex++;
            }

            return currentDelayIndex;
        }
        else
            return 0;
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
}
