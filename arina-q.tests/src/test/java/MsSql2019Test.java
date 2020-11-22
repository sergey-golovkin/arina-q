public class MsSql2019Test extends DbTest
{
	public MsSql2019Test() throws Exception
	{
		dbaServerName = "192.168.1.38";
		dbaServerPort = 1434;
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
