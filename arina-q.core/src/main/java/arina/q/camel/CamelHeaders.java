package arina.q.camel;

/**
 * Created with IntelliJ IDEA.
 * User: GSM
 * Date: 30.04.17
 * Time: 23:52
 * To change this template use File | Settings | File Templates.
 */

public class CamelHeaders extends arina.utils.CamelHeaders
{
    public final static String body               = "${body}";

    public final static String metaInfo           = "ARINA-Q-MetaInfo";
    public final static String rowId              = "ARINA-Q-RowId";

	public final static String inId               = "ARINA-Q-InId";
	public final static String outId              = "ARINA-Q-OutId";
	public final static String fromSystem         = "ARINA-Q-FromSystem";
	public final static String toSystem           = "ARINA-Q-ToSystem";
	public final static String dataType           = "ARINA-Q-DataType";
	public final static String dataDate           = "ARINA-Q-DataDate";
	public final static String errorText          = "ARINA-Q-ErrorText";
    public final static String subQ               = "ARINA-Q-SubQ";
    public final static String skipMessage        = "ARINA-Q-SkipMessage";
    public final static String expireDate         = "ARINA-Q-ExpireDate";
    public final static String replaceId          = "ARINA-Q-ReplaceId";
    public final static String msgId              = "ARINA-Q-MsgId";

    public final static String stageDate          = "ARINA-Q-StageDate";
    public final static String requestId          = "ARINA-Q-RequestId";
    public final static String requestDate        = "ARINA-Q-RequestDate";
    public final static String stage              = "ARINA-Q-Stage";
    public final static String iteration          = "ARINA-Q-Iteration";
    public final static String errorCode          = "ARINA-Q-ErrorCode";
    public final static String processAfterDelay  = "ARINA-Q-ProcessAfterDelay";
    public final static String depId              = "ARINA-Q-DepId";
    public final static String parentDepId        = "ARINA-Q-ParentDepId";
    public final static String waitFinalTimeout   = "ARINA-Q-WaitFinalTimeout";
}
