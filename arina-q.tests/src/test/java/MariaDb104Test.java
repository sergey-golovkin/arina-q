public class MariaDb104Test extends DbTest
{
	public MariaDb104Test() throws Exception
	{
		dbaServerPort = 3410;
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
