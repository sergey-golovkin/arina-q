package arina.q.camel;

/**
 * Created with IntelliJ IDEA.
 * User: Golovkin
 * Date: 14.08.13
 * Time: 11:20
 * To change this template use File | Settings | File Templates.
 */

import arina.q.datasource.IQDataSource;
import org.apache.camel.*;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.commons.lang.StringUtils;

public class SQEndpoint extends DefaultEndpoint
{
    private SQParameters wsqsp;

    SQEndpoint(String uri, SQComponent component, SQParameters p)
    {
        super(uri, component);
        this.wsqsp = p;
    }

    public Consumer createConsumer(Processor processor) throws Exception
    {
        this.wsqsp.engine = this.getCamelContext().getRegistry().lookupByNameAndType(this.wsqsp.dataSource, IQDataSource.class);
        if(this.wsqsp.engine == null)
            throw new FailedToCreateConsumerException(this, new IllegalArgumentException("Arina Q DataSource [" + this.wsqsp.dataSource + "] invalid. Try to check connection parameters."));

        if(StringUtils.isEmpty(this.wsqsp.remoteSystem))
            this.wsqsp.remoteSystem = this.wsqsp.engine.getSystem();

        SQConsumer consumer = new SQConsumer(this, processor, this.wsqsp);
        configureConsumer(consumer);
        return consumer;
    }

    public Producer createProducer()
    {
        this.wsqsp.engine = this.getCamelContext().getRegistry().lookupByNameAndType(this.wsqsp.dataSource, IQDataSource.class);
        if(this.wsqsp.engine == null)
            throw new FailedToCreateProducerException(this, new IllegalArgumentException("Arina Q DataSource [" + this.wsqsp.dataSource + "] invalid. Try to check connection parameters."));

        if(StringUtils.isEmpty(this.wsqsp.remoteSystem))
            this.wsqsp.remoteSystem = this.wsqsp.engine.getSystem();

        return new SQProducer(this, this.wsqsp);
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }
}
