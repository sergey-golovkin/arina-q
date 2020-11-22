package arina.q.datasource;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: GSM
 * Date: 14.05.17
 * Time: 11:45
 * To change this template use File | Settings | File Templates.
 */
public class FireBird extends Database
{
	public FireBird()
	{
		super();

		this.setDriverClassName("org.firebirdsql.jdbc.FBDriver");
		this.setValidationQuery("select 1 from RDB$DATABASE");
        this.port = 3050;
	}

	@Override
	protected void setUrl()
	{
		this.setUrl("jdbc:firebirdsql:" + this.server + "/" + this.port + ":" + this.database + "?charSet=UTF-8&roleName=Q_" + this.system + "_SVC_ROLE");
	}

	@Override
	public String getClobString(ResultSet rs, int index) throws SQLException
	{
//        Clob clob = null;
//
//        try
//        {
//            clob = rs.getClob(index);
//            return clob.getSubString(1, 0x7FFFFFFF);
//        }
//        finally
//        {
//            try { clob.free(); } catch (Exception ignore){ ; }
//        }
		return rs.getString(index);
	}

	@Override
	public String getSchema()
	{
		return "";
	}
}
