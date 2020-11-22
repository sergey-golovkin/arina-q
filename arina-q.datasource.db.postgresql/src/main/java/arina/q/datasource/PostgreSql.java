package arina.q.datasource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: GSM
 * Date: 14.05.17
 * Time: 11:42
 * To change this template use File | Settings | File Templates.
 */
public class PostgreSql extends Database
{
	public PostgreSql() throws ClassNotFoundException
	{
		super();

		this.setDriverClassName("org.postgresql.Driver");
		this.setValidationQuery("select 1");
        this.port = 5432;

		Locale.setDefault(Locale.US);
		Class.forName(this.getDriverClassName());
	}

	@Override
	protected void setUrl()
	{
		this.setUrl("jdbc:postgresql://" + this.server + ":" + this.port + "/" + this.database + "?currentSchema=Q_" + this.system);
	}

	@Override
	public String getClobString(ResultSet rs, int index) throws SQLException
	{
		return rs.getString(index);
	}

	@Override
	public void setUsername(String username)
	{
		super.setUsername(username.toLowerCase());
	}
}
