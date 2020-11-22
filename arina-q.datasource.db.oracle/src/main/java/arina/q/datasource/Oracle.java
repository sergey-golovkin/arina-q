package arina.q.datasource;

import arina.q.camel.QComponent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: GSM
 * Date: 14.05.17
 * Time: 11:24
 * To change this template use File | Settings | File Templates.
 */
public class Oracle extends Database
{
	public Oracle() throws ClassNotFoundException
	{
		super();

		this.setDriverClassName("oracle.jdbc.OracleDriver");
		this.setValidationQuery("select 1 from dual");
        this.port = 1521;

		Locale.setDefault(Locale.US);
		Class.forName(this.getDriverClassName());
	}

	@Override
	protected void setUrl()
	{
		this.setUrl("jdbc:oracle:thin:@" + this.server + ":" + this.port + (this.database.startsWith(":") || this.database.startsWith("/") ? this.database : ":" + this.database));
	}

	@Override
	public PreparedStatement get(Connection con, int mode) throws SQLException
	{
		if(mode == QComponent.modeDirect)
			return con.prepareStatement("SELECT * FROM TABLE(" + this.getSchema() + "GET_IN_VALUE_3_11(?, ?, ?))");
		else if(mode == QComponent.modeReverse)
			return con.prepareStatement("SELECT * FROM TABLE(" + this.getSchema() + "GET_OUT_VALUE_2_11(?, ?))");
		else
			throw new SQLException("Invalid mode = [" + mode + "]");
	}

	@Override
	public PreparedStatement getStage(Connection con) throws SQLException
	{
		return con.prepareStatement("SELECT * FROM TABLE(" + this.getSchema() + "GET_STAGE_VALUE_3_10(?, ?, ?))");
	}

    @Override
    public PreparedStatement getFinalStage(Connection con) throws SQLException
    {
        return con.prepareStatement("SELECT * FROM TABLE(" + this.getSchema() + "GET_FINAL_STAGE_VALUE_1_10(?))");
    }
}
