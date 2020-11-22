public class FireBird30Test extends DbTest
{
	public FireBird30Test() throws Exception
	{
		dbaServerPort = 3050;
		dbaDatabaseName = "test";
		dbaLogin = "sysdba";
		systemName = "FIREBIRD";
		systemPassword = "PWD";
		systemStorage = "";

		db = new arina.q.datasource.FireBird();
		setUp();
	}
}
