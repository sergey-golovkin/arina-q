public class PostgreSql12Test extends DbTest
{
	public PostgreSql12Test() throws Exception
	{
		dbaServerPort = 5436;
		dbaDatabaseName = "postgres";
		dbaLogin = "postgres";
		systemName = "POSTGRESQL";
		systemPassword = "PWD";
		systemStorage = "pg_default";

		db = new arina.q.datasource.PostgreSql();
		setUp();
	}
}
