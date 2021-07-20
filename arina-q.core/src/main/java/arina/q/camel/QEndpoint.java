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
import org.apache.camel.support.DefaultEndpoint;
import org.apache.commons.lang.StringUtils;

public class QEndpoint extends DefaultEndpoint
{
    private QParameters wsqp;

    QEndpoint(String uri, QComponent component, QParameters p)
    {
        super(uri, component);
        this.wsqp = p;
    }

    public Consumer createConsumer(Processor processor) throws Exception
    {
        this.wsqp.engine = this.getCamelContext().getRegistry().lookupByNameAndType(this.wsqp.dataSource, IQDataSource.class);
        if(this.wsqp.engine == null)
            throw new FailedToCreateConsumerException(this, new IllegalArgumentException("Arina Q DataSource [" + this.wsqp.dataSource + "] invalid. Try to check connection parameters."));

        if(StringUtils.isEmpty(this.wsqp.remoteSystem))
            this.wsqp.remoteSystem = this.wsqp.engine.getSystem();

        QConsumer consumer = new QConsumer(this, processor, this.wsqp);
        configureConsumer(consumer);
        return consumer;
    }

    public Producer createProducer() throws Exception
    {
        return new QProducer(this, this.wsqp);
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }
}
