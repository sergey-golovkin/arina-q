package arina.q.camel;

/**
 * Created with IntelliJ IDEA.
 * User: Golovkin
 * Date: 14.08.13
 * Time: 11:20
 * To change this template use File | Settings | File Templates.
 */

import arina.q.camel.jmx.Stat;
import arina.q.camel.jmx.JmxOperation;
import arina.q.datasource.IQDataSource;
import arina.utils.LanguageUtils;
import arina.utils.jmx.AnnotatedJMXProxy;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.apache.commons.lang.StringUtils;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.net.URLDecoder;
import java.sql.Timestamp;

public class QProducer extends DefaultProducer
{
    private String messageBody;
    private int mode;
    private String remoteSystem;
    private String toSystem;
    private String fromSystem;
    private String dataType;
    private String subQ;
    private String dataSource;
    private String metaInfo;
    private String expireDate;
    private String replaceId;
    private String transId;
    private String transSeqNo;
    private String transTTL;
    private String sendTrans;
    public JmxOperation jmxOperation = new JmxOperation();
    private ObjectName objectName;

    QProducer(QEndpoint endpoint, QParameters p)
    {
        super(endpoint);

        this.dataSource = p.dataSource;
        this.mode = p.mode;
        this.messageBody = p.messageBody;
	    this.remoteSystem = p.remoteSystem;
        this.toSystem = p.toSystem;
	    this.fromSystem = p.fromSystem;
        this.dataType = p.dataType;
        this.subQ = p.processedSubQ;
        this.metaInfo = p.metaInfo;
        this.expireDate = p.expireDate;
        this.replaceId = p.replaceId;
        this.transId = p.transId;
        this.transSeqNo = p.transSeqNo;
        this.transTTL = p.transTTL;
        this.sendTrans = p.sendTrans;
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try
        {
            objectName = new ObjectName("arina-q:type=processors,name=\"" + URLDecoder.decode(this.getEndpoint().getEndpointKey(), "utf8").replace("?", "\\?") + "\"");
            server.registerMBean(new AnnotatedJMXProxy(jmxOperation), objectName);
        }
        catch (InstanceAlreadyExistsException e)
        {
            objectName = new ObjectName("arina-q:type=processors,name=\"" + URLDecoder.decode(this.getEndpoint().getEndpointKey(), "utf8").replace("?", "\\?") + "\",threadId=\"threadId=" + Thread.currentThread().getId() + "\"");
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

        try
        {
            String rSystem = this.remoteSystem;
            String toS = LanguageUtils.evaluate(exchange, this.toSystem, String.class);
            String fromS = LanguageUtils.evaluate(exchange, this.fromSystem, String.class);
            String dsName = LanguageUtils.evaluate(exchange, this.dataSource, String.class);
            IQDataSource engine = this.getEndpoint().getCamelContext().getRegistry().lookupByNameAndType(dsName, IQDataSource.class);
            if (engine == null)
                throw new IllegalArgumentException("Arina Q DataSource [" + dsName + "] invalid. Try to check connection parameters.");

            if (StringUtils.isEmpty(rSystem))
                rSystem = engine.getSystem();

            Long sq = LanguageUtils.evaluate(exchange, this.subQ, Long.class);
            if (sq == null)
                sq = 0L;

            if (this.mode == QComponent.modeDirect)
            {
                Long msgId = engine.putOut(
                        StringUtils.isNotEmpty(rSystem) ? rSystem : fromS,
                        toS,
                        LanguageUtils.evaluate(exchange, this.dataType, Integer.class),
                        LanguageUtils.evaluate(exchange, this.messageBody, String.class),
                        LanguageUtils.evaluate(exchange, this.metaInfo, String.class),
                        LanguageUtils.evaluate(exchange, this.expireDate, Timestamp.class),
                        LanguageUtils.evaluate(exchange, this.replaceId, String.class),
                        LanguageUtils.evaluate(exchange, this.transId, String.class),
                        LanguageUtils.evaluate(exchange, this.transSeqNo, Long.class),
                        LanguageUtils.evaluate(exchange, this.transTTL, Long.class)
                );

                CamelHeaders.setValue(exchange, CamelHeaders.msgId, msgId);

                if (LanguageUtils.evaluate(exchange, this.sendTrans, Boolean.class))
                    engine.sendOutTrans(LanguageUtils.evaluate(exchange, this.transId, String.class));
            } else if (this.mode == QComponent.modeReverse)
            {
                Long outId = CamelHeaders.getValue(exchange, CamelHeaders.outId, Long.class);

                Long msgId = engine.putIn(
                        StringUtils.isNotEmpty(rSystem) ? rSystem : toS,
                        fromS,
                        outId != null ? outId : 0L,
                        LanguageUtils.evaluate(exchange, this.dataType, Integer.class),
                        LanguageUtils.evaluate(exchange, this.messageBody, String.class),
                        null,
                        sq,
                        LanguageUtils.evaluate(exchange, this.metaInfo, String.class),
                        LanguageUtils.evaluate(exchange, this.expireDate, Timestamp.class),
                        LanguageUtils.evaluate(exchange, this.replaceId, String.class)
                );

                CamelHeaders.setValue(exchange, CamelHeaders.msgId, msgId);
            } else if (this.mode == QComponent.modeChangeSubQ)
            {
                engine.changeSubQ(
                        StringUtils.isNotEmpty(rSystem) ? rSystem : toS,
                        sq,
                        CamelHeaders.getValue(exchange, CamelHeaders.inId, Long.class),
                        CamelHeaders.getValue(exchange, CamelHeaders.rowId, String.class)
                );
                CamelHeaders.setValue(exchange, CamelHeaders.skipMessage, true);
            }
            jmxOperation.success(stat.finish());
        }
        catch (Exception e)
        {
            jmxOperation.error(stat.finish());
            throw e;
        }
    }
}
