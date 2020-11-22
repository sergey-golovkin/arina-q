public class Oracle18ACOTest extends DbTest
{
	public Oracle18ACOTest() throws Exception
	{
		dbaServerPort = 1521;
		dbaDatabaseName = "/XEPDB1";
		dbaLogin = "system";
		systemName = "ORACLEACO";
		systemPassword = "PWD";
		systemStorage = "USERS";

		db = new arina.q.datasource.OracleACO();
		setUp();
	}
}
