package arina.q.datasource;

/**
 * Created with IntelliJ IDEA.
 * User: GSM
 * Date: 14.05.17
 * Time: 11:33
 * To change this template use File | Settings | File Templates.
 */
public class MsSql extends Database
{
	public MsSql()
	{
		super();

		this.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		this.setValidationQuery("select 1");
        this.port = 1433;
	}

	@Override
	protected void setUrl()
	{
		this.setUrl("jdbc:sqlserver://" + this.server + ":" + this.port + ";databaseName=" + this.database + ";selectMethod=cursor");
	}
}
