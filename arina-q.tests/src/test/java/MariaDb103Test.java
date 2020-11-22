public class MariaDb103Test extends DbTest
{
	public MariaDb103Test() throws Exception
	{
		dbaServerPort = 3409;
		dbaDatabaseName = "test";
		dbaLogin = "root";
		dbaPassword = "Manager_2019";
		systemName = "MARIADB";
		systemPassword = "PWD";
		systemStorage = "";

		db = new arina.q.datasource.MariaDb();
		setUp();
	}
}
