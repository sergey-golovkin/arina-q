public class PostgreSql10Test extends DbTest
{
	public PostgreSql10Test() throws Exception
	{
		dbaServerPort = 5434;
		dbaDatabaseName = "postgres";
		dbaLogin = "postgres";
		systemName = "POSTGRESQL";
		systemPassword = "PWD";
		systemStorage = "pg_default";

		db = new arina.q.datasource.PostgreSql();
		setUp();
	}
}
