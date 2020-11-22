package arina.q.camel;

/**
 * Created with IntelliJ IDEA.
 * User: Golovkin
 * Date: 14.08.13
 * Time: 11:20
 * To change this template use File | Settings | File Templates.
 */

import java.util.Map;

import org.apache.camel.*;
import org.apache.camel.impl.DefaultComponent;

public class SQComponent extends DefaultComponent
{
    public SQComponent()
    {
    }

    public SQComponent(CamelContext context)
    {
        super(context);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters)
    {
        SQParameters p = new SQParameters();

        p.dataSource = remaining;
        p.systemsFilter = getAndRemoveParameter(parameters, "systemsFilter", String.class, null);
        p.messageBody = getAndRemoveParameter(parameters, "messageBody", String.class, CamelHeaders.body);
        p.remoteSystem = getAndRemoveParameter(parameters, "remoteSystem", String.class, null);
        p.stage = getAndRemoveParameter(parameters, "stage", String.class, CamelHeaders.simpleName(CamelHeaders.stage));
        p.delay = getAndRemoveParameter(parameters, "delay", Long.class, 0L);
        p.isFinal = getAndRemoveParameter(parameters, "final", String.class, "false");
        p.threads = getAndRemoveParameter(parameters, "threads", Integer.class, 1);
        p.initialDelay = getAndRemoveParameter(parameters, "initialDelay", Integer.class, 60000);
        p.loopDelays = getAndRemoveParameter(parameters, "loopDelays", String.class, "100");
        p.errorDelays = getAndRemoveParameter(parameters, "errorDelays", String.class, "60000");
        p.depId = getAndRemoveParameter(parameters, "depId", String.class, CamelHeaders.simpleName(CamelHeaders.depId));
        p.parentDepId = getAndRemoveParameter(parameters, "parentDepId", String.class, CamelHeaders.simpleName(CamelHeaders.parentDepId));
        p.metaInfo = getAndRemoveParameter(parameters, "metaInfo", String.class, CamelHeaders.simpleName(CamelHeaders.metaInfo));
        p.waitFinal = getAndRemoveParameter(parameters, "waitFinal", String.class, "0");

        return new SQEndpoint(uri, this, p);
    }
}
