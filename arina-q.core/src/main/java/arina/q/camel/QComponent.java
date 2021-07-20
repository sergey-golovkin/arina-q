package arina.q.camel;

/**
 * Created with IntelliJ IDEA.
 * User: Golovkin
 * Date: 14.08.13
 * Time: 11:20
 * To change this template use File | Settings | File Templates.
 */

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;

public class QComponent extends DefaultComponent
{
    public static final int modeDirect = 0;
    public static final int modeReverse = 1;
    public static final int modeChangeSubQ = 2;

    public QComponent()
    {
    }

    public QComponent(CamelContext context)
    {
        super(context);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception
    {
        String m = getAndRemoveParameter(parameters, "mode", String.class, "Direct");
        if( ! m.equalsIgnoreCase("Direct") && ! m.equalsIgnoreCase("Reverse") && ! m.equalsIgnoreCase("ChangeSubQ"))
            throw new Exception("Invalid mode: (" + m + "). Must be mode=Direct or mode=Reverse or mode=ChangeSubQ");

        QParameters p = new QParameters();

        p.dataSource = remaining;
        p.systemsFilter = getAndRemoveParameter(parameters, "systemsFilter", String.class, null);
        p.messageBody = getAndRemoveParameter(parameters, "messageBody", String.class, CamelHeaders.body);
        p.typesFilter = getAndRemoveParameter(parameters, "typesFilter", String.class, null);
        p.mode = m.equalsIgnoreCase("Direct") ? modeDirect : m.equalsIgnoreCase("Reverse") ? modeReverse : m.equalsIgnoreCase("ChangeSubQ") ? modeChangeSubQ : -1;
	    p.remoteSystem = getAndRemoveParameter(parameters, "remoteSystem", String.class, null);
	    p.toSystem = getAndRemoveParameter(parameters, "toSystem", String.class, CamelHeaders.simpleName(CamelHeaders.toSystem));
        p.fromSystem = getAndRemoveParameter(parameters, "fromSystem", String.class, CamelHeaders.simpleName(CamelHeaders.fromSystem));
        p.dataType = getAndRemoveParameter(parameters, "dataType", String.class, CamelHeaders.simpleName(CamelHeaders.dataType));
        p.processedSubQ = getAndRemoveParameter(parameters, "subQ", String.class, CamelHeaders.simpleName(CamelHeaders.subQ));
        p.initialDelay = getAndRemoveParameter(parameters, "initialDelay", Integer.class, 60000);
        p.loopDelays = getAndRemoveParameter(parameters, "loopDelays", String.class, "100");
        p.errorDelays = getAndRemoveParameter(parameters, "errorDelays", String.class, "60000");
        p.metaInfo = getAndRemoveParameter(parameters, "metaInfo", String.class, CamelHeaders.simpleName(CamelHeaders.metaInfo));
        p.expireDate = getAndRemoveParameter(parameters, "expireDate", String.class, CamelHeaders.simpleName(CamelHeaders.expireDate));
        p.skipExpired = getAndRemoveParameter(parameters, "skipExpired", Boolean.class, true);
        p.expiredWarningText = getAndRemoveParameter(parameters, "expiredWarningText", String.class, "Message expired!");
        p.replaceId = getAndRemoveParameter(parameters, "replaceId", String.class, CamelHeaders.simpleName(CamelHeaders.replaceId));
        p.transId = getAndRemoveParameter(parameters, "transId", String.class, null);
        p.transSeqNo= getAndRemoveParameter(parameters, "transSeqNo", String.class, null);
        p.transTTL = getAndRemoveParameter(parameters, "transTTL", String.class, null);
        p.sendTrans = getAndRemoveParameter(parameters, "sendTrans", String.class, "false");

        return new QEndpoint(uri, this, p);
    }
}
