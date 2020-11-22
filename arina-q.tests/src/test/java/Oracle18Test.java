public class Oracle18Test extends DbTest
{
	public Oracle18Test() throws Exception
	{
		dbaServerPort = 1521;
		dbaDatabaseName = "/XEPDB1";
		dbaLogin = "system";
		systemName = "ORACLE";
		systemPassword = "PWD";
		systemStorage = "USERS";

		db = new arina.q.datasource.Oracle();
		setUp();
	}
}
