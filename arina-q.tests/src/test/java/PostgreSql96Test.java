public class PostgreSql96Test extends DbTest
{
	public PostgreSql96Test() throws Exception
	{
		dbaServerPort = 5433;
		dbaDatabaseName = "postgres";
		dbaLogin = "postgres";
		systemName = "POSTGRESQL";
		systemPassword = "PWD";
		systemStorage = "pg_default";

		db = new arina.q.datasource.PostgreSql();
		setUp();
	}
}
