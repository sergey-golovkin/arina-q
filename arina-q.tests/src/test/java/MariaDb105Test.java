public class MariaDb105Test extends DbTest
{
	public MariaDb105Test() throws Exception
	{
		dbaServerName = "192.168.1.38";
		dbaServerPort = 3411;
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
