package arina.q.datasource;

import arina.q.camel.QComponent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: GSM
 * Date: 14.05.17
 * Time: 11:44
 * To change this template use File | Settings | File Templates.
 */
public class MariaDb extends Database
{
	public MariaDb()
	{
		super();

		this.setDriverClassName("org.mariadb.jdbc.Driver");
		this.setValidationQuery("select 1");
        this.port = 3306;
	}

	@Override
	protected void setUrl()
	{
		this.setUrl("jdbc:mariadb://" + this.server + ":" + this.port + "/" + this.database);
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
}
