public class MariaDb102Test extends DbTest
{
	public MariaDb102Test() throws Exception
	{
		dbaServerPort = 3408;
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
