package arina.q.datasource;

import arina.q.camel.QComponent;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: Golovkin
 * Date: 12.05.17
 * Time: 0:38
 * To change this template use File | Settings | File Templates.
 */
public abstract class Database extends DataSource implements IQDataSource, IQManager, DisposableBean
{
    protected int logMoverDelays[] = new int[] { 15000, 30000, 60000 };
    protected static final Logger log = LoggerFactory.getLogger(Database.class);
    protected String system;
	protected String server;
    protected int port;
	protected String database;
    protected long perfAlertThresholdGet = 0;
    protected long perfAlertThresholdCommit = 0;
    protected long perfAlertThresholdAll = 0;
    protected long perfAlertThresholdProcessMessage = 0;
    protected Thread threadMoveDataInToLog = null;
    protected Thread threadMoveDataOutToLog = null;
    protected Thread threadMoveStageToLog = null;

    protected final static int  Q_ROW_ID = 1;
    protected final static int  Q_DATA_IN_ID = 2;
    protected final static int  Q_DATA_OUT_ID = 3;
    protected final static int  Q_SYS_ID = 4;
    protected final static int  Q_SYS_2_ID = 5;
    protected final static int  Q_DATA_TYPE = 6;
    protected final static int  Q_DATA_VALUE = 7;
    protected final static int  Q_META_INFO = 8;
    protected final static int  Q_RECORD_DATE = 9;
    protected final static int  Q_EXPIRE_DATE = 10;
    protected final static int  Q_REPLACE_ID = 11;

    protected final static int SQ_ROW_ID = 1;
    protected final static int SQ_EXT_ID = 2;
    protected final static int SQ_SYS_ID = 3;
    protected final static int SQ_REQUEST_DATE = 4;
    protected final static int SQ_STAGE_DATE = 5;
    protected final static int SQ_REQUEST_ID = 6;
    protected final static int SQ_DATA_VALUE = 7;
    protected final static int SQ_META_INFO = 8;
    protected final static int SQ_STAGE = 9;
    protected final static int SQ_ITERATION = 10;

	Database()
	{
		super();

		this.setDefaultAutoCommit(false);
		this.setDefaultTransactionIsolation(2);
		this.setInitialSize(1);
		this.setTestWhileIdle(true);
		this.setTestOnBorrow(true);
		this.setValidationInterval(120000);
		this.setRollbackOnReturn(false);
        this.setTimeBetweenEvictionRunsMillis(30000);
	}

    public long getPerfAlertThresholdGet()
    {
        return this.perfAlertThresholdGet;
    }

    public long getPerfAlertThresholdCommit()
    {
        return this.perfAlertThresholdCommit;
    }

    public long getPerfAlertThresholdAll()
    {
        return this.perfAlertThresholdAll;
    }

    public long getPerfAlertThresholdProcessMessage()
    {
        return this.perfAlertThresholdProcessMessage;
    }

    public void setPerfAlertThresholdGet(long value)
    {
        this.perfAlertThresholdGet = value;
    }

    public void setPerfAlertThresholdCommit(long value)
    {
        this.perfAlertThresholdCommit = value;
    }

    public void setPerfAlertThresholdAll(long value)
    {
        this.perfAlertThresholdAll = value;
    }

    public void setPerfAlertThresholdProcessMessage(long value)
    {
        this.perfAlertThresholdProcessMessage = value;
    }

    protected abstract void setUrl();

	public void setServer(String server)
	{
		this.server = server;
		if(StringUtils.isNotEmpty(this.system) && StringUtils.isNotEmpty(this.server) && StringUtils.isNotEmpty(this.database))
			this.setUrl();
	}

	public void setDatabase(String database)
	{
		this.database = database;
		if(StringUtils.isNotEmpty(this.system) && StringUtils.isNotEmpty(this.server) && StringUtils.isNotEmpty(this.database))
			this.setUrl();
	}

    public void setSystem(String system)
    {
        this.system = system;
	    this.setUsername("Q_" + this.system + "_SVC");
	    if(StringUtils.isNotEmpty(this.system) && StringUtils.isNotEmpty(this.server) && StringUtils.isNotEmpty(this.database))
		    this.setUrl();
    }

    public void setPort(int port)
    {
        this.port = port;
        if(StringUtils.isNotEmpty(this.system) && StringUtils.isNotEmpty(this.server) && StringUtils.isNotEmpty(this.database))
            this.setUrl();
    }

    @Override
    public String getSystem()
    {
        return this.system;
    }

    @Override
    public List<SQElement> getStage(final String sysId, final String stage, final String systemsFilter, final int rows) throws Exception
    {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<SQElement> result = new ArrayList<>(rows);

        try
        {
            Locale.setDefault(Locale.US);
            con = this.getConnection();
            stmt = this.getStage(con);
            stmt.setString(1, stage);
            stmt.setString(2, systemsFilter);
            stmt.setInt(3, rows);

            rs = stmt.executeQuery();

            for( ; ! Thread.currentThread().isInterrupted() && rs.next(); )
                result.add(
                    new SQElement(
                        rs.getString(SQ_ROW_ID),
                        rs.getString(SQ_EXT_ID),
                        rs.getString(SQ_SYS_ID),
                        rs.getTimestamp(SQ_REQUEST_DATE),
                        rs.getTimestamp(SQ_STAGE_DATE),
                        rs.getLong(SQ_REQUEST_ID),
                        this.getClobString(rs, SQ_DATA_VALUE),
                        this.getClobString(rs, SQ_META_INFO),
                        rs.getString(SQ_STAGE),
                        rs.getLong(SQ_ITERATION))
                );

            return result;
        }
        catch (Exception e)
        {
            try { con.rollback(); } catch (Exception ignore) { }
            throw e;
        }
        finally
        {
            try { rs.close();} catch (Exception ignore){ }
            try { stmt.close(); } catch (Exception ignore) { }
            try { con.close(); } catch (Exception ignore) { }
        }
    }


    @Override
    public SQElement getFinalStage(final String sysId, final long requestId) throws Exception
    {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try
        {
            Locale.setDefault(Locale.US);
            con = this.getConnection();
            stmt = this.getFinalStage(con);
            stmt.setLong(1, requestId);

            rs = stmt.executeQuery();

            if(rs.next())
                return new SQElement(
                    rs.getString(SQ_ROW_ID),
                    rs.getString(SQ_EXT_ID),
                    rs.getString(SQ_SYS_ID),
                    rs.getTimestamp(SQ_REQUEST_DATE),
                    rs.getTimestamp(SQ_STAGE_DATE),
                    rs.getLong(SQ_REQUEST_ID),
                    this.getClobString(rs, SQ_DATA_VALUE),
                    this.getClobString(rs, SQ_META_INFO),
                    rs.getString(SQ_STAGE),
                    rs.getLong(SQ_ITERATION));
            else
                return null;
        }
        catch (Exception e)
        {
            try { con.rollback(); } catch (Exception ignore) { }
            throw e;
        }
        finally
        {
            try { rs.close();} catch (Exception ignore){ }
            try { stmt.close(); } catch (Exception ignore) { }
            try { con.close(); } catch (Exception ignore) { }
        }
    }

    @Override
    public void commitStage(final String sysId, final Timestamp stageDate, final long requestId, final int errorCode, final String errorMessage, final long processAfterDelay, final String rowId) throws Exception
    {
        Connection con = null;
        CallableStatement stmt = null;

        try
        {
            Locale.setDefault(Locale.US);
            con = this.getConnection();
            stmt = this.commitStage(con);
            stmt.setTimestamp(1, stageDate);
            stmt.setLong(2, requestId);
            stmt.setInt(3, errorCode);
            stmt.setString(4, errorMessage);
            stmt.setLong(5, processAfterDelay);
            stmt.setString(6, rowId);
            stmt.execute();
            con.commit();
        }
        catch (Exception e)
        {
            try { con.rollback(); } catch (Exception ignore) { }
            throw e;
        }
        finally
        {
            try { stmt.close(); } catch (Exception ignore) { }
            try { con.close(); } catch (Exception ignore) { }
        }

        startStageToLogMover();
    }

    @Override
    public void rollbackStage(final String sysId, final Timestamp stageDate, final long requestId, final int errorCode, final String errorMessage, final String rowId) throws Exception
    {
        Connection con = null;
        CallableStatement stmt = null;

        try
        {
            Locale.setDefault(Locale.US);
            con = this.getConnection();
            stmt = this.rollbackStage(con);
            stmt.setTimestamp(1, stageDate);
            stmt.setLong(2, requestId);
            stmt.setInt(3, errorCode);
            stmt.setString(4, errorMessage);
            stmt.setString(5, rowId);
            stmt.execute();
            con.commit();
        }
        catch (Exception e)
        {
            try { con.rollback(); } catch (Exception ignore) { }
            throw e;
        }
        finally
        {
            try { stmt.close(); } catch (Exception ignore) { }
            try { con.close(); } catch (Exception ignore) { }
        }
    }

    @Override
    public long putStage(final String sysId, final String extId, final String fromSystem, final Long requestId, final String body, final String stage, final long processAfterDelay, final boolean isFinal, final String depId, final String parentDepId, final String metaInfo) throws Exception
    {
        Connection con = null;
        CallableStatement stmt = null;

        try
        {
            Locale.setDefault(Locale.US);
            con = this.getConnection();
            stmt = this.putStage(con);
            stmt.registerOutParameter(1, Types.BIGINT);
            stmt.setString(2, extId);
            stmt.setString(3, fromSystem);

            if(requestId != null)
                stmt.setLong(4, requestId);
            else
                stmt.setNull(4, Types.BIGINT);

            stmt.setString(5, body);
            stmt.setString(6, stage);
            stmt.setLong(7, processAfterDelay);
            stmt.setInt(8, isFinal ? 1 : 0);
            stmt.setString(9, depId);
            stmt.setString(10, parentDepId);
            stmt.setString(11, metaInfo);
            stmt.execute();
            Long result = stmt.getLong(1);
            con.commit();
            return result;
        }
        catch (Exception e)
        {
            try { con.rollback(); } catch (Exception ignore) { }
            throw e;
        }
        finally
        {
            try { stmt.close(); } catch (Exception ignore) { }
            try { con.close(); } catch (Exception ignore) { }
        }
    }

    @Override
    public long putIn(final String sysId, final String fromSystem, final long outId, final int dataType, final String body, final Timestamp recordDate, final Long subQ, final String metaInfo, final Timestamp expireDate, final String replaceId) throws Exception
    {
        Connection con = null;
        CallableStatement stmt = null;

        try
        {
            Locale.setDefault(Locale.US);
            con = this.getConnection();
            stmt = this.put(con, QComponent.modeReverse);
            stmt.registerOutParameter(1, Types.BIGINT);
            stmt.setString(2, fromSystem);
            stmt.setLong(3, outId);
            stmt.setInt(4, dataType);
            stmt.setString(5, body);
            stmt.setTimestamp(6, recordDate);
            stmt.setLong(7, subQ);
            stmt.setString(8, metaInfo);
            stmt.setTimestamp(9, expireDate);
            stmt.setString(10, replaceId);
            stmt.execute();
            Long result = stmt.getLong(1);
            con.commit();
            return result;
        }
        catch (Exception e)
        {
            try { con.rollback(); } catch (Exception ignore) { }
            throw e;
        }
        finally
        {
            try { stmt.close(); } catch (Exception ignore) { }
            try { con.close(); } catch (Exception ignore) { }
        }
    }

    @Override
    public long putOut(final String sysId, final String toSystem, final int dataType, final String body, final String metaInfo, final Timestamp expireDate, final String replaceId, final String transId, final Long transSeqNo, final Long transTTLSeconds) throws Exception
    {
        Connection con = null;
        CallableStatement stmt = null;

        try
        {
            Locale.setDefault(Locale.US);
            con = this.getConnection();
            stmt = this.put(con, QComponent.modeDirect);
            stmt.registerOutParameter(1, Types.BIGINT);
            stmt.setString(2, toSystem);
            stmt.setInt(3, dataType);
            stmt.setString(4, body);
            stmt.setString(5, metaInfo);
            stmt.setTimestamp(6, expireDate);
            stmt.setString(7, replaceId);
            stmt.setString(8, transId);

            if(transSeqNo != null)
                stmt.setLong(9, transSeqNo);
            else
                stmt.setNull(9, Types.BIGINT);

            if(transTTLSeconds != null)
                stmt.setLong(10, transTTLSeconds);
            else
                stmt.setNull(10, Types.BIGINT);

            stmt.execute();
            Long result = stmt.getLong(1);
            con.commit();
            return result;
        }
        catch (Exception e)
        {
            try { con.rollback(); } catch (Exception ignore) { }
            throw e;
        }
        finally
        {
            try { stmt.close(); } catch (Exception ignore) { }
            try { con.close(); } catch (Exception ignore) { }
        }
    }

    @Override
    public void sendOutTrans(final String transId) throws Exception
    {
        Connection con = null;
        CallableStatement stmt = null;

        try
        {
            Locale.setDefault(Locale.US);
            con = this.getConnection();
            stmt = this.sendOutTrans(con);
            stmt.setString(1, transId);
            stmt.execute();
            con.commit();
        }
        catch (Exception e)
        {
            try { con.rollback(); } catch (Exception ignore) { }
            throw e;
        }
        finally
        {
            try { stmt.close(); } catch (Exception ignore) { }
            try { con.close(); } catch (Exception ignore) { }
        }
    }

    @Override
    public void changeSubQ(final String sysId, final Long subQ, final long inId, final String rowId) throws Exception
    {
        Connection con = null;
        CallableStatement stmt = null;

        try
        {
            Locale.setDefault(Locale.US);
            con = this.getConnection();
            stmt = this.changeSubQ(con, QComponent.modeChangeSubQ);
            stmt.setLong(1, subQ);
            stmt.setLong(2, inId);
            stmt.setString(3, rowId);
            stmt.execute();
            con.commit();
        }
        catch (Exception e)
        {
            try { con.rollback(); } catch (Exception ignore) { }
            throw e;
        }
        finally
        {
            try { stmt.close(); } catch (Exception ignore) { }
            try { con.close(); } catch (Exception ignore) { }
        }
    }

    @Override
    public QElement getIn(final String sysId, final String fromSystem, final String typesFilter, final Long subQ) throws Exception
    {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try
        {
            Locale.setDefault(Locale.US);
            con = this.getConnection();
            stmt = this.get(con, QComponent.modeDirect);
            stmt.setString(1, fromSystem);
            stmt.setString(2, typesFilter);
            if(subQ != null)
                stmt.setLong(3, subQ);
            else
                stmt.setNull(3, Types.BIGINT);

            rs = stmt.executeQuery();

            if(rs.next())
                return new QElement(
                    rs.getString(Q_ROW_ID),
                    rs.getLong(Q_DATA_IN_ID),
                    rs.getLong(Q_DATA_OUT_ID),
                    rs.getString(Q_SYS_ID),
                    rs.getString(Q_SYS_2_ID),
                    rs.getInt(Q_DATA_TYPE),
                    this.getClobString(rs, Q_DATA_VALUE),
                    this.getClobString(rs, Q_META_INFO),
                    rs.getTimestamp(Q_RECORD_DATE),
                    rs.getTimestamp(Q_EXPIRE_DATE),
                    rs.getString(Q_REPLACE_ID)
                );
            else
                return null;
        }
        catch (Exception e)
        {
            try { con.rollback(); } catch (Exception ignore) { }
            throw e;
        }
        finally
        {
            try { rs.close(); } catch (Exception ignore) { }
            try { stmt.close(); } catch (Exception ignore) { }
            try { con.close(); } catch (Exception ignore) { }
        }
    }

    @Override
    public QElement getOut(final String sysId, final String toSystem, final String typesFilter) throws Exception
    {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try
        {
            Locale.setDefault(Locale.US);
            con = this.getConnection();
            stmt = this.get(con, QComponent.modeReverse);
            stmt.setString(1, toSystem);
            stmt.setString(2, typesFilter);

            rs = stmt.executeQuery();

            if(rs.next())
                return new QElement(
                        rs.getString(Q_ROW_ID),
                        rs.getLong(Q_DATA_IN_ID),
                        rs.getLong(Q_DATA_OUT_ID),
                        rs.getString(Q_SYS_ID),
                        rs.getString(Q_SYS_2_ID),
                        rs.getInt(Q_DATA_TYPE),
                        this.getClobString(rs, Q_DATA_VALUE),
                        this.getClobString(rs, Q_META_INFO),
                        rs.getTimestamp(Q_RECORD_DATE),
                        rs.getTimestamp(Q_EXPIRE_DATE),
                        rs.getString(Q_REPLACE_ID)
                );
            else
                return null;
        }
        catch (Exception e)
        {
            try { con.rollback(); } catch (Exception ignore) { }
            throw e;
        }
        finally
        {
            try { rs.close(); } catch (Exception ignore) { }
            try { stmt.close(); } catch (Exception ignore) { }
            try { con.close(); } catch (Exception ignore) { }
        }
    }

    @Override
    public void commitIn(final String sysId, final long inId, final Timestamp recordDate, final String errorMessage, final String rowId) throws Exception
    {
        Connection con = null;
        CallableStatement stmt = null;

        try
        {
            Locale.setDefault(Locale.US);
            con = this.getConnection();
            stmt = this.commit(con, QComponent.modeDirect);
            stmt.setLong(1, inId);
            stmt.setTimestamp(2, recordDate);
            stmt.setString(3, errorMessage);
            stmt.setString(4, rowId);
            stmt.execute();
            con.commit();
        }
        catch (Exception e)
        {
            try { con.rollback(); } catch (Exception ignore) { }
            throw e;
        }
        finally
        {
            try { stmt.close(); } catch (Exception ignore) { }
            try { con.close(); } catch (Exception ignore) { }
        }

        startDataInToLogMover();
    }

    @Override
    public void commitOut(final String sysId, final long outId, final String errorMessage, final String rowId) throws Exception
    {
        Connection con = null;
        CallableStatement stmt = null;

        try
        {
            Locale.setDefault(Locale.US);
            con = this.getConnection();
            stmt = this.commit(con, QComponent.modeReverse);
            stmt.setLong(1, outId);
            stmt.setString(2, errorMessage);
            stmt.setString(3, rowId);
            stmt.execute();
            con.commit();
        }
        catch (Exception e)
        {
            try { con.rollback(); } catch (Exception ignore) { }
            throw e;
        }
        finally
        {
            try { stmt.close(); } catch (Exception ignore) { }
            try { con.close(); } catch (Exception ignore) { }
        }

        startDataOutToLogMover();
    }

    @Override
    public void rollbackIn(final String sysId, final long inId, final int errorCode, final String errorMessage, final String rowId) throws Exception
    {
        Connection con = null;
        CallableStatement stmt = null;

        try
        {
            Locale.setDefault(Locale.US);
            con = this.getConnection();
            stmt = this.rollback(con, QComponent.modeDirect);
            stmt.setLong(1, inId);
            stmt.setInt(2, errorCode);
            stmt.setString(3, errorMessage);
            stmt.setString(4, rowId);
            stmt.execute();
            con.commit();
        }
        catch (Exception e)
        {
            try { con.rollback(); } catch (Exception ignore) { }
            throw e;
        }
        finally
        {
            try { stmt.close(); } catch (Exception ignore) { }
            try { con.close(); } catch (Exception ignore) { }
        }
    }

    @Override
    public void rollbackOut(final String sysId, final long outId, final int errorCode, final String errorMessage, final String rowId) throws Exception
    {
        Connection con = null;
        CallableStatement stmt = null;

        try
        {
            Locale.setDefault(Locale.US);
            con = this.getConnection();
            stmt = this.rollback(con, QComponent.modeReverse);
            stmt.setLong(1, outId);
            stmt.setInt(2, errorCode);
            stmt.setString(3, errorMessage);
            stmt.setString(4, rowId);
            stmt.execute();
            con.commit();
        }
        catch (Exception e)
        {
            try { con.rollback(); } catch (Exception ignore) { }
            throw e;
        }
        finally
        {
            try { stmt.close(); } catch (Exception ignore) { }
            try { con.close(); } catch (Exception ignore) { }
        }
    }

    public String getSchema()
    {
        return "Q_" + this.system + ".";
    }

	public CallableStatement put(Connection con, int mode) throws SQLException
	{
		if(mode == QComponent.modeDirect)
			return con.prepareCall("{ ? = call " + this.getSchema() + "PUT_OUT_VALUE_9_1(?, ?, ?, ?, ?, ?, ?, ?, ?) }");
		else if(mode == QComponent.modeReverse)
			return con.prepareCall("{ ? = call " + this.getSchema() + "PUT_IN_VALUE_9_1(?, ?, ?, ?, ?, ?, ?, ?, ?) }");
		else
			throw new SQLException("Invalid mode = [" + mode + "]");
	}

    public CallableStatement sendOutTrans(Connection con) throws SQLException
    {
        return con.prepareCall("{ call " + this.getSchema() + "SEND_OUT_TRANS_1(?) }");
    }

	public PreparedStatement get(Connection con, int mode) throws SQLException
	{
		if(mode == QComponent.modeDirect)
			return con.prepareStatement("SELECT * FROM " + this.getSchema() + "GET_IN_VALUE_3_11(?, ?, ?)");
		else if(mode == QComponent.modeReverse)
			return con.prepareStatement("SELECT * FROM " + this.getSchema() + "GET_OUT_VALUE_2_11(?, ?)");
		else
			throw new SQLException("Invalid mode = [" + mode + "]");
	}

	public CallableStatement commit(Connection con, int mode) throws SQLException
	{
		if(mode == QComponent.modeDirect)
			return con.prepareCall("{ call " + this.getSchema() + "COMMIT_IN_PROCESSING_4(?, ?, ?, ?) }");
		else if(mode == QComponent.modeReverse)
			return con.prepareCall("{ call " + this.getSchema() + "COMMIT_OUT_PROCESSING_3(?, ?, ?) }");
		else
			throw new SQLException("Invalid mode = [" + mode + "]");
	}

	public CallableStatement rollback(Connection con, int mode) throws SQLException
	{
		if(mode == QComponent.modeDirect)
			return con.prepareCall("{ call " + this.getSchema() + "ROLLBACK_IN_PROCESSING_4(?, ?, ?, ?) }");
		else if(mode == QComponent.modeReverse)
			return con.prepareCall("{ call " + this.getSchema() + "ROLLBACK_OUT_PROCESSING_4(?, ?, ?, ?) }");
		else
			throw new SQLException("Invalid mode = [" + mode + "]");
	}

	public CallableStatement changeSubQ(Connection con, int mode) throws SQLException
	{
		if(mode == QComponent.modeChangeSubQ)
			return con.prepareCall("{ call " + this.getSchema() + "CHANGE_IN_SUBQ_3(?, ?, ?) }");
		else
			throw new SQLException("Invalid mode = [" + mode + "]");
	}

	public CallableStatement putStage(Connection con) throws SQLException
	{
		return con.prepareCall("{ ? = call " + this.getSchema() + "PUT_STAGE_VALUE_11_1(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");
	}

    public PreparedStatement getStage(Connection con) throws SQLException
    {
        return con.prepareStatement("SELECT * FROM " + this.getSchema() + "GET_STAGE_VALUE_3_10(?, ?, ?)");
    }

    public PreparedStatement getFinalStage(Connection con) throws SQLException
    {
        return con.prepareStatement("SELECT * FROM " + this.getSchema() + "GET_FINAL_STAGE_VALUE_1_10(?)");
    }

    public CallableStatement commitStage(Connection con) throws SQLException
	{
		return con.prepareCall("{ call " + this.getSchema() + "COMMIT_STAGE_PROCESSING_8(?, ?, ?, ?, ?, ?) }");
	}

	public CallableStatement rollbackStage(Connection con) throws SQLException
	{
		return con.prepareCall("{ call " + this.getSchema() + "ROLLBACK_STAGE_PROCESSING_6(?, ?, ?, ?, ?) }");
	}

	public String getClobString(ResultSet rs, int index) throws SQLException
	{
		Clob clob = null;

		try
		{
			clob = rs.getClob(index);
            if(clob != null)
			    return clob.getSubString(1, (int) clob.length());
            else
                return null;
		}
		finally
		{
			try { clob.free(); } catch (Exception ignore){ }
		}
	}

	public void install(String dbaServerName, String dbaServerPort, String dbaDatabaseName, String dbaLogin, String dbaPassword, String systemName, String systemPassword, String systemStorage) throws Exception
	{
		Connection con = null;
		InputStream is = null;

		Locale.setDefault(Locale.US);
		Class.forName(this.getDriverClassName());

		try
		{
            this.setSystem(systemName);

			this.setServer(dbaServerName);
            if(StringUtils.isNotEmpty(dbaServerPort))
                this.setPort(Integer.parseInt(dbaServerPort));
            this.setDatabase(dbaDatabaseName);

            con = DriverManager.getConnection(this.getUrl(), dbaLogin, dbaPassword);
			con.setAutoCommit(false);

			is = this.getClass().getResourceAsStream("/" + this.getClass().getName() + ".InstallQ.sql");
			String sql = IOUtils.toString(is, "utf8");
			try { is.close(); } catch (Exception ignore) { }

			sql = sql.replace("<SYSID>", systemName);
			sql = sql.replace("<PASSWORD>", systemPassword);
			sql = sql.replace("<STORAGE>", systemStorage);

			processScript(con, sql, false);

			is = this.getClass().getResourceAsStream("/" + this.getClass().getName() + ".InstallStageQ.sql");
			sql = IOUtils.toString(is, "utf8");
			try { is.close(); } catch (Exception ignore) { }

            sql = sql.replace("<SYSID>", systemName);
            sql = sql.replace("<PASSWORD>", systemPassword);
            sql = sql.replace("<STORAGE>", systemStorage);

			processScript(con, sql, false);

			con.commit();
		}
        catch (Exception e)
        {
            try { con.rollback(); } catch (Exception ignore) { }
            throw e;
        }
		finally
		{
			try { con.close(); } catch (Exception ignore) { }
		}
	}

	public void processScript(Connection con, String sql, boolean ingnoreFaults) throws SQLException
	{
		Statement stmt = null;
		try
		{
			stmt = con.createStatement();
			for(String cmd : sql.split("~"))
			{
				if(cmd.trim().length() > 0)
				{
					log.trace(cmd);
					try
                    {
                        stmt.execute(cmd);
                    }
					catch (SQLException ex)
                    {
                        log.error(cmd, ex);
                        if( ! ingnoreFaults)
                            throw ex;
                    }
				}
			}
		}
		finally
		{
			try { stmt.close(); } catch (Exception ignore) { }
		}
	}

	public void drop(String dbaServerName, String dbaServerPort, String dbaDatabaseName, String dbaLogin, String dbaPassword, String systemName, String systemStorage) throws Exception
	{
        Connection con = null;
        InputStream is = null;

        Locale.setDefault(Locale.US);
        Class.forName(this.getDriverClassName());

		try
		{
            this.setSystem(systemName);

            this.setServer(dbaServerName);
            if(StringUtils.isNotEmpty(dbaServerPort))
                this.setPort(Integer.parseInt(dbaServerPort));
            this.setDatabase(dbaDatabaseName);

            con = DriverManager.getConnection(this.getUrl(), dbaLogin, dbaPassword);
            con.setAutoCommit(false);

			is = this.getClass().getResourceAsStream("/" + this.getClass().getName() + ".DropQ.sql");
			String sql = IOUtils.toString(is, "utf8");
			try { is.close(); } catch (Exception ignore) { }

            sql = sql.replace("<SYSID>", systemName);
            sql = sql.replace("<STORAGE>", systemStorage);

			processScript(con, sql, true);

			con.commit();
		}
        catch (Exception e)
        {
            try { con.rollback(); } catch (Exception ignore) { }
            throw e;
        }
		finally
		{
			try { con.close(); } catch (Exception ignore) { }
		}
	}

    protected void startDataInToLogMover()
    {
        synchronized(this)
        {
            if (threadMoveDataInToLog == null)
            {
                threadMoveDataInToLog = new Thread(() -> {
                    try
                    {
                        logMover("DATA_IN", threadMoveDataInToLog);
                    }
                    finally
                    {
                        synchronized (this)
                        {
                            threadMoveDataInToLog = null;
                        }
                    }
                });
                threadMoveDataInToLog.start();
            }
            else
            {
                synchronized (threadMoveDataInToLog)
                {
                    threadMoveDataInToLog.notify();
                }
            }
        }
    }

    protected void startDataOutToLogMover()
    {
        synchronized(this)
        {
            if (threadMoveDataOutToLog == null)
            {
                threadMoveDataOutToLog = new Thread(() -> {
                    try
                    {
                        logMover("DATA_OUT", threadMoveDataOutToLog);
                    }
                    finally
                    {
                        synchronized (this)
                        {
                            threadMoveDataOutToLog = null;
                        }
                    }
                });
                threadMoveDataOutToLog.start();
            }
            else
            {
                synchronized (threadMoveDataOutToLog)
                {
                    threadMoveDataOutToLog.notify();
                }
            }
        }
    }

    protected void startStageToLogMover()
    {
        synchronized(this)
        {
            if (threadMoveStageToLog == null)
            {
                threadMoveStageToLog = new Thread(() -> {
                    try
                    {
                        logMover("STAGE", threadMoveStageToLog);
                    }
                    finally
                    {
                        synchronized (this)
                        {
                            threadMoveStageToLog = null;
                        }
                    }
                });
                threadMoveStageToLog.start();
            }
            else
            {
                synchronized (threadMoveStageToLog)
                {
                    threadMoveStageToLog.notify();
                }
            }
        }
    }

    protected void logMover(String name, Object syncObject)
    {
        log.trace(getSystem() + "." + name + " Log Writer started");
        Connection con = null;
        CallableStatement stmt = null;

        try
        {
            Locale.setDefault(Locale.US);
            con = getConnection();
            stmt = con.prepareCall("{ ? = call " + this.getSchema() + "MOVE_" + name + "_TO_LOG(1000) }");
            stmt.registerOutParameter(1, Types.INTEGER);

            for(int currentDelay = 0; ! Thread.currentThread().isInterrupted() && currentDelay < logMoverDelays.length; )
            {
                stmt.execute();
                int rows = stmt.getInt(1);
                con.commit();

                if(rows == 0)
                {
                    synchronized (syncObject)
                    {
                        syncObject.wait(logMoverDelays[currentDelay++]);
                    }
                }
                else
                    currentDelay = 0;
            }
        }
        catch (InterruptedException ignore)
        {
        }
        catch (Exception ex)
        {
            log.error("EXCEPTION: ", ex);
            try { con.rollback(); } catch (Exception ignore) { }
        }
        finally
        {
            try { stmt.close(); } catch (Exception ignore) { }
            try { con.close(); } catch (Exception ignore) { }
            log.trace(getSystem() + "." + name + " Log Writer stopped");
        }
    }

    @Override
    public void destroy() throws java.lang.Exception
    {
        synchronized (this)
        {
            if(this.threadMoveDataInToLog != null)
            {
                this.threadMoveDataInToLog.interrupt();
                try { Thread.sleep(100); } catch (InterruptedException ignore){ }
                this.threadMoveDataInToLog = null;
            }

            if(this.threadMoveDataOutToLog != null)
            {
                this.threadMoveDataOutToLog.interrupt();
                try { Thread.sleep(100); } catch (InterruptedException ignore){ }
                this.threadMoveDataOutToLog = null;
            }

            if(this.threadMoveStageToLog!= null)
            {
                this.threadMoveStageToLog.interrupt();
                try { Thread.sleep(100); } catch (InterruptedException ignore){ }
                this.threadMoveStageToLog = null;
            }
        }
    }
}
