public class MariaDb55Test extends DbTest
{
	public MariaDb55Test() throws Exception
	{
		dbaServerPort = 3406;
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
