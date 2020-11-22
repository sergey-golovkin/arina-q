public class MariaDb101Test extends DbTest
{
	public MariaDb101Test() throws Exception
	{
		dbaServerPort = 3407;
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
