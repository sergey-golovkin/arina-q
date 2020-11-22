public class MySql56Test extends DbTest
{
	public MySql56Test() throws Exception
	{
		dbaServerName = "192.168.1.38";
		dbaServerPort = 3306;
		dbaDatabaseName = "test";
		dbaLogin = "root";
		dbaPassword = "Manager_2019";
		systemName = "MYSQL";
		systemPassword = "PWD";
		systemStorage = "";

		db = new arina.q.datasource.MySql();
		setUp();
	}
}
