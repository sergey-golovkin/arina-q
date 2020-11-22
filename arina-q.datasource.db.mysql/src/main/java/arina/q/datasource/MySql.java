package arina.q.datasource;

import arina.q.camel.QComponent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: GSM
 * Date: 14.05.17
 * Time: 11:44
 * To change this template use File | Settings | File Templates.
 */
public class MySql extends Database
{
	public MySql()
	{
		super();

		this.setDriverClassName("com.mysql.cj.jdbc.Driver"); // 8.xx driver
		this.setValidationQuery("select 1");
        this.port = 3306;
	}

	@Override
	protected void setUrl()
	{
		this.setUrl("jdbc:mysql://" + this.server + ":" + this.port + "/" + this.database + "?noAccessToProcedureBodies=true");
	}

	@Override
	public PreparedStatement get(Connection con, int mode) throws SQLException
	{
		if(mode == QComponent.modeDirect)
			return con.prepareStatement("CALL GET_IN_VALUE_3_11(?, ?, ?)");
		else if(mode == QComponent.modeReverse)
			return con.prepareStatement("CALL GET_OUT_VALUE_2_11(?, ?)");
		else
			throw new SQLException("Invalid mode = [" + mode + "]");
	}

	@Override
	public PreparedStatement getStage(Connection con) throws SQLException
	{
		return con.prepareStatement("CALL GET_STAGE_VALUE_3_10(?, ?, ?)");
	}

    @Override
    public PreparedStatement getFinalStage(Connection con) throws SQLException
    {
        return con.prepareStatement("CALL GET_FINAL_STAGE_VALUE_1_10(?)");
    }

	@Override
	public String getSchema()
	{
		return "";
	}

	@Override
	protected void logMover(String name, Object syncObject)
	{
		log.trace(getSystem() + "." + name + " Log Writer started");
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try
		{
			Locale.setDefault(Locale.US);
			con = getConnection();
			stmt = con.prepareStatement("SELECT " + this.getSchema() + "MOVE_" + name + "_TO_LOG(1000)");

			for(int currentDelay = 0; ! Thread.currentThread().isInterrupted() && currentDelay < logMoverDelays.length; )
			{
				int rows = 0;
				rs = stmt.executeQuery();
				if(rs.next())
				{
					rows = rs.getInt(1);
					con.commit();
				}

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
			try { rs.close(); } catch (Exception ignore) { }
			try { stmt.close(); } catch (Exception ignore) { }
			try { con.close(); } catch (Exception ignore) { }
			log.trace(getSystem() + "." + name + " Log Writer stopped");
		}
	}
}
