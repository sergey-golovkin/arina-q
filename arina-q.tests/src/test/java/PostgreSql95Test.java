public class PostgreSql95Test extends DbTest
{
	public PostgreSql95Test() throws Exception
	{
		dbaServerPort = 5432;
		dbaDatabaseName = "postgres";
		dbaLogin = "postgres";
		systemName = "POSTGRESQL";
		systemPassword = "PWD";
		systemStorage = "pg_default";

		db = new arina.q.datasource.PostgreSql();
		setUp();
	}
}
