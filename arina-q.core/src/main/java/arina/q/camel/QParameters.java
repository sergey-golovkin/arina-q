package arina.q.camel;

import arina.q.datasource.IQDataSource;

/**
 * Created with IntelliJ IDEA.
 * User: Golovkin
 * Date: 13.04.17
 * Time: 15:38
 * To change this template use File | Settings | File Templates.
 */
class QParameters
{
    IQDataSource engine;
    String dataSource;
    String messageBody;
    String systemsFilter;
    String typesFilter;
    int mode;
	String remoteSystem;
	String toSystem;
    String fromSystem;
    String dataType;
    String processedSubQ;
    String loopDelays;
    int initialDelay;
    String errorDelays;
    String metaInfo;
    String expireDate;
    Boolean skipExpired;
    String expiredWarningText;
    String replaceId;
    String transId;
    String transSeqNo;
    String transTTL;
    String sendTrans;
}
