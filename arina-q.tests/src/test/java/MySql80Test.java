public class MySql80Test extends DbTest
{
	public MySql80Test() throws Exception
	{
		dbaServerPort = 3308;
		dbaDatabaseName = "test";
		dbaLogin = "root";
		systemName = "MYSQL";
		systemPassword = "PWD";
		systemStorage = "";

		db = new arina.q.datasource.MySql();
		setUp();
	}
}
