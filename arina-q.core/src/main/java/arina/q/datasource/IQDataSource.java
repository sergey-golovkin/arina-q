package arina.q.datasource;

import java.sql.Timestamp;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Golovkin
 * Date: 11.05.17
 * Time: 19:25
 * To change this template use File | Settings | File Templates.
 */
public interface IQDataSource
{
    String getSystem();

    QElement getIn(final String sysId, final String fromSystem, final String typesFilter, final Long subQ) throws Exception;
    QElement getOut(final String sysId, final String toSystem, final String typesFilter) throws Exception;
    void commitIn(final String sysId, final long inId, final Timestamp recordDate, final String errorMessage, final String rowId) throws Exception;
    void commitOut(final String sysId, final long outId, final String errorMessage, final String rowId) throws Exception;
    void rollbackIn(final String sysId, final long inId, final int errorCode, final String errorMessage, final String rowId) throws Exception;
    void rollbackOut(final String sysId, final long outId, final int errorCode, final String errorMessage, final String rowId) throws Exception;
    long putIn(final String sysId, final String fromSystem, final long outId, final int dataType, final String body, final Timestamp recordDate, final Long subQ, final String metaInfo, final Timestamp expireDate, final String replaceId) throws Exception;
    long putOut(final String sysId, final String toSystem, final int dataType, final String body, final String metaInfo, final Timestamp expireDate, final String replaceId, final String transId, final Long transSeqNo, final Long transTTLSeconds) throws Exception;
    void sendOutTrans(final String transId) throws Exception;
    void changeSubQ(final String sysId, final Long subQ, final long inId, final String rowId) throws Exception;

    List<SQElement> getStage(final String sysId, final String stage, final String systemsFilter, final int rows) throws Exception;
    SQElement getFinalStage(final String sysId, final long requestId) throws Exception;
    void commitStage(final String sysId, final Timestamp stageDate, final long requestId, final int errorCode, final String errorMessage, final long processAfterDelay, final String rowId) throws Exception;
    void rollbackStage(final String sysId, final Timestamp stageDate, final long requestId, final int errorCode, final String errorMessage, final String rowId) throws Exception;
    long putStage(final String sysId, final String extId, final String fromSystem, final Long requestId, final String body, final String stage, final long processAfterDelay, final boolean isFinal, final String depId, final String parentDepId, final String metaInfo) throws Exception;

    long getPerfAlertThresholdGet();
    long getPerfAlertThresholdCommit();
    long getPerfAlertThresholdAll();
    long getPerfAlertThresholdProcessMessage();
}
