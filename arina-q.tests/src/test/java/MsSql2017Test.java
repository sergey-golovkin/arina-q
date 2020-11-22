public class MsSql2017Test extends DbTest
{
	public MsSql2017Test() throws Exception
	{
		dbaServerName = "192.168.1.38";
		dbaServerPort = 1433;
		dbaDatabaseName = "master";
		dbaLogin = "sa";
		dbaPassword = "Manager_2019";
		systemName = "MSSQL";
		systemPassword = "PWD";
		systemStorage = "master";

		db = new arina.q.datasource.MsSql();
		setUp();
	}
}
