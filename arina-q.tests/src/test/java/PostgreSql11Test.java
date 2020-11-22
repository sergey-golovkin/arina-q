public class PostgreSql11Test extends DbTest
{
	public PostgreSql11Test() throws Exception
	{
		dbaServerPort = 5435;
		dbaDatabaseName = "postgres";
		dbaLogin = "postgres";
		systemName = "POSTGRESQL";
		systemPassword = "PWD";
		systemStorage = "pg_default";

		db = new arina.q.datasource.PostgreSql();
		setUp();
	}
}
