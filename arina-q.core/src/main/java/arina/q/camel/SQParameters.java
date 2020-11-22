package arina.q.camel;

import arina.q.datasource.IQDataSource;

/**
 * Created with IntelliJ IDEA.
 * User: Golovkin
 * Date: 13.04.17
 * Time: 15:44
 * To change this template use File | Settings | File Templates.
 */
class SQParameters
{
    IQDataSource engine;
    String messageBody;
    String remoteSystem;
    String dataSource;
    String stage;
    String systemsFilter;
    long delay;
    String isFinal;
    int threads;
    Integer initialDelay;
    String loopDelays;
    String errorDelays;
    String depId;
    String parentDepId;
    String metaInfo;
    String waitFinal;
}
