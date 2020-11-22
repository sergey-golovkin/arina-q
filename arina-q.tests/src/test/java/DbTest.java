import arina.q.datasource.Database;
import arina.q.datasource.QElement;
import arina.q.datasource.SQElement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentest4j.AssertionFailedError;

import java.sql.*;
import java.util.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class DbTest
{
	protected String  dbaServerName = "192.168.1.38";
	protected Integer dbaServerPort;
	protected String  dbaDatabaseName;
	protected String  dbaLogin;
	protected String  dbaPassword = "Manager_2019";
	protected String  systemName;
	protected String  systemPassword;
	protected String  systemStorage;
	protected Database db;

	protected String insertTypesScript = "";
	protected String insertExTypesScript = "";

	protected List<String> extSystems = Arrays.asList("EXT_SYS_1", "EXT_SYS_2", "EXT_SYS_3");
	protected int typesCount;
	protected int extTypesCount;
	protected Integer dataType = Integer.MAX_VALUE;
	protected Long outId;
	protected String fromSystem;
	protected String dataValue;
	protected Timestamp dataDate;
	protected Long subQ;
	protected String metaInfo;
	protected Timestamp expireDate;
	protected String replaceId;
	protected Connection con;

	public DbTest()
	{
	}

	void tryAssertEquals(long expected, String tableName, String filter) throws InterruptedException
	{
		for(int retries = 601 /*MAGIC VALUE :-) value depends from "logMoverDelays" array*/; retries > 0; retries--)
		{
			try
			{
				assertEquals(expected, getCount(tableName, filter));
				return;
			}
			catch (AssertionFailedError e)
			{
				if(retries == 1)
					throw e;
				else
					Thread.sleep(100); // delay until log mover runs
			}
		}
	}

	protected void setUp() throws Exception
	{
		db.setServer(dbaServerName);
		db.setPort(dbaServerPort);
		db.setDatabase(dbaDatabaseName);
		db.setPassword(systemPassword);
		db.setSystem(systemName);

		insertTypesScript = "INSERT INTO " + db.getSchema() + "TYPES VALUES(" + dataType + ", 'description')";
		typesCount = 1;

		extSystems.forEach(x -> insertExTypesScript += "INSERT INTO " + db.getSchema() + "EXT_TYPES VALUES('" + x + "'," + dataType + ", 'description')~");
		extTypesCount = extSystems.size();

		Locale.setDefault(Locale.US);
		Class.forName(db.getDriverClassName());
		con = DriverManager.getConnection(db.getUrl(), dbaLogin, dbaPassword);
		con.setAutoCommit(false);
	}

	protected void fillTestValues()
	{
		outId = genLong();
		fromSystem = genString();
		dataValue = "body is " + genString();
		dataDate = new Random().nextBoolean() ? genTimestamp(0) : null;
		subQ = genLong();
		metaInfo = new Random().nextBoolean() ? "metaInfo is " + genString() : null;
		expireDate = new Random().nextBoolean() ? genTimestamp(60000) : null;
		replaceId = new Random().nextBoolean() ? "replaceId is " + genString() : null;
	}

	protected static Timestamp genTimestamp(long delta)
	{
		return new Timestamp(System.currentTimeMillis() + delta);
	}

	protected static String genString()
	{
		return UUID.randomUUID().toString().replace("-", "");
	}

	protected static long genLong()
	{
		return (new Random().nextLong() / 10); // division by ten because the field length is 18 digits.
	}

	@AfterEach
	public void afterEach() throws Exception
	{
		clean("DATA_IN_MSG", null);
		clean("DATA_IN", null);
		clean("DATA_IN_LOG", null);
		clean("DATA_IN_LOG_ARCHIVE", null);
		clean("DATA_IN_ERRORS", null);
		clean("DATA_OUT_MSG", null);
		clean("DATA_OUT", null);
		clean("DATA_OUT_LOG", null);
		clean("DATA_OUT_LOG_ARCHIVE", null);
		clean("DATA_OUT_ERRORS", null);
		clean("STAGES_MSG", null);
		clean("STAGES", null);
		clean("STAGES_LOG", null);
		clean("STAGES_LOG_ARCHIVE", null);
		clean("STAGES_ERRORS", null);
		clean("STAGES_DEPS", null);

		try { con.close(); } catch (Exception ignore) { }
		db.destroy();
		db.close(true);
	}

	public void processScript(String sql) throws Exception
	{
		try
		{
			con.rollback();
			db.processScript(con, sql, false);
			con.commit();
		}
		catch (Exception e)
		{
			try { con.rollback(); } catch (Exception ignore) { }
			throw e;
		}
	}

	public long getCount(String tableName, String filter)
	{
		Statement stmt = null;
		ResultSet rs = null;

		try
		{
			con.rollback();
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT COUNT(*) FROM " + db.getSchema() + tableName + (filter == null || filter.length() == 0 ? "" : " WHERE " + filter));
			rs.next();
			return rs.getLong(1);
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			return -1;
		}
		finally
		{
			try { rs.close(); } catch (Exception ignore) { }
			try { stmt.close(); } catch (Exception ignore) { }
		}
	}

	public void clean(String tableName, String filter)
	{
		Statement stmt = null;

		try
		{
			con.rollback();
			stmt = con.createStatement();
			stmt.executeUpdate("DELETE FROM " + db.getSchema() + tableName + (filter == null || filter.length() == 0 ? "" : " WHERE " + filter));
			con.commit();
		}
		catch (SQLException ex)
		{
			try { con.rollback(); } catch (Exception ignore) { }
		}
		finally
		{
			try { stmt.close(); } catch (Exception ignore) { }
		}
	}

	public void checkOutRecord(QElement el)
	{
		assertNotNull(el);
		assertEquals(systemName, el.fromSystem);
		assertEquals(dataType, el.dataType);
		assertNotNull(el.dataDate);
		assertEquals(dataValue, el.dataValue);
		assertEquals(metaInfo, el.metaInfo);
		assertEquals(expireDate, el.expireDate);
		assertNull(el.replaceId);
	}

	public void checkInRecord(QElement el)
	{
		assertNotNull(el);
		assertEquals(outId, el.outId);
		assertEquals(systemName, el.toSystem);
		assertEquals(dataType, el.dataType);
		if(dataDate != null)
			assertEquals(dataDate, el.dataDate);
		else
			assertNotNull(el.dataDate);
		assertEquals(dataValue, el.dataValue);
		assertEquals(metaInfo, el.metaInfo);
		assertEquals(expireDate, el.expireDate);
		assertEquals(replaceId, el.replaceId);
	}

	public void checkStageRecord(SQElement el)
	{
		assertNotNull(el);
		assertEquals(fromSystem, el.fromSystem);
		assertTrue(el.body.startsWith(dataValue));
		assertEquals(metaInfo, el.metaInfo);
	}

	@Order(0)
	@ParameterizedTest
	@ValueSource(booleans = {true})
	public void install(boolean installRequired) throws Exception
	{
		if(installRequired)
		{
			db.install(dbaServerName, dbaServerPort.toString(), dbaDatabaseName, dbaLogin, dbaPassword, systemName, systemPassword, systemStorage);

			// insert predefined test values
			processScript(insertTypesScript);
			processScript(insertExTypesScript);

			assertEquals(extTypesCount, getCount("EXT_TYPES", null));
			assertEquals(typesCount, getCount("TYPES", null));
		}
	}

	@Order(9999)
	@ParameterizedTest
	@ValueSource(booleans = {true})
	public void drop(boolean dropRequired) throws Exception
	{
		if(dropRequired)
		{
			db.drop(dbaServerName, dbaServerPort.toString(), dbaDatabaseName, dbaLogin, dbaPassword, systemName, systemStorage);
		}
	}

	@Order(1000)
	@ParameterizedTest
	@ValueSource(ints = {1})
	public void testIn1(int iterationCount) throws Exception
	{
		for(int i = 1; i <= iterationCount; i++)
		{
			fillTestValues();

			long id = db.putIn(systemName, fromSystem, outId, dataType, dataValue, dataDate, subQ, metaInfo, expireDate, replaceId);
			assertTrue(id > 0);
			assertEquals(1, getCount("DATA_IN", "MSG_ID = " + id));
			assertEquals(1, getCount("DATA_IN_MSG", "MSG_ID = " + id));

			QElement el = db.getIn(systemName, fromSystem, dataType.toString(), subQ);
			checkInRecord(el);
			assertEquals(id, el.inId);
			assertEquals(fromSystem, el.fromSystem);
			db.commitIn(systemName, el.inId, el.dataDate, null, el.rowId);
			assertEquals(0, getCount("DATA_IN", null));
			assertEquals(0, getCount("DATA_IN_ERRORS", null));
		}

		tryAssertEquals(0, "DATA_IN_LOG", null);
		tryAssertEquals(iterationCount, "DATA_IN_LOG_ARCHIVE", null);
	}

	@Order(1001)
	@ParameterizedTest
	@ValueSource(ints = {1})
	public void testIn2(int iterationCount) throws Exception
	{
		int errorsCount = 0;
		for(int i = 1; i <= iterationCount; i++)
		{
			fillTestValues();

			int count = 0;
			for(String extSystem : extSystems)
			{
				long id = db.putIn(systemName, extSystem, outId, dataType, dataValue, dataDate, subQ, metaInfo, expireDate, replaceId);
				assertTrue(id > 0);
				assertEquals(1, getCount("DATA_IN", "MSG_ID = " + id));
				assertEquals(1, getCount("DATA_IN_MSG", "MSG_ID = " + id));
				count++;
			}

			assertEquals(count * i, getCount("DATA_IN_MSG", null));
			if(replaceId == null)
			{
				assertEquals(count, getCount("DATA_IN", null));
			}
			else
			{
				errorsCount += 2;
				assertEquals(1, getCount("DATA_IN", null));
			}
			assertEquals(errorsCount, getCount("DATA_IN_ERRORS", null));

			// read from any system and wrong data type
			QElement el = db.getIn(systemName, null, Integer.toString(Integer.MIN_VALUE), subQ);
			assertNull(el);

			// read from wrong system and any data type
			el = db.getIn(systemName, genString(), null, subQ);
			assertNull(el);

			// read from any system and any data type and wrong sub-queue
			el = db.getIn(systemName, null, null, genLong());
			assertNull(el);

			for(String extSystem : extSystems)
			{
				el = db.getIn(systemName, extSystem, dataType.toString(), subQ);
				if(replaceId == null || count == 1)
				{
					checkInRecord(el);
					count--;
					assertEquals(extSystem, el.fromSystem);
					db.commitIn(systemName, el.inId, el.dataDate, null, el.rowId);
					assertEquals(count, getCount("DATA_IN", null));
				}
				else
				{
					assertNull(el);
					count--;
				}
			}
			assertEquals(errorsCount, getCount("DATA_IN_ERRORS", null));
		}

		assertEquals(0, getCount("DATA_IN", null));
		tryAssertEquals(0, "DATA_IN_LOG", null);
		tryAssertEquals(iterationCount * extSystems.size(),"DATA_IN_LOG_ARCHIVE", null);
	}

	@Order(1002)
	@ParameterizedTest
	@ValueSource(ints = {1})
	public void testIn3(int iterationCount) throws Exception
	{
		int errorsCount = 0;
		for(int i = 1; i <= iterationCount; i++)
		{
			fillTestValues();

			long id = db.putIn(systemName, fromSystem, outId, dataType, dataValue, dataDate, subQ, metaInfo, expireDate, replaceId);
			assertTrue(id > 0);
			assertEquals(1, getCount("DATA_IN", "MSG_ID = " + id));
			assertEquals(1, getCount("DATA_IN_MSG", "MSG_ID = " + id));

			QElement el = db.getIn(systemName, fromSystem, dataType.toString(), subQ);
			checkInRecord(el);
			assertEquals(id, el.inId);
			assertEquals(fromSystem, el.fromSystem);
			db.rollbackIn(systemName, el.inId, Integer.MIN_VALUE, "error message (rollback) is: " + genString(), el.rowId);
			assertEquals(1, getCount("DATA_IN", null));
			assertEquals(++errorsCount, getCount("DATA_IN_ERRORS", null));
			Long newSubQ = genLong();
			assertEquals(1, getCount("DATA_IN", "SUBQ_ID = " + subQ));
			assertEquals(0, getCount("DATA_IN", "SUBQ_ID = " + newSubQ));
			db.changeSubQ(systemName, newSubQ, el.inId, el.rowId);
			assertEquals(0, getCount("DATA_IN", "SUBQ_ID = " + subQ));
			assertEquals(1, getCount("DATA_IN", "SUBQ_ID = " + newSubQ));
			el = db.getIn(systemName, null, null, subQ);
			assertNull(el);
			el = db.getIn(systemName, null, null, newSubQ);
			assertNotNull(el);
			db.commitIn(systemName, el.inId, el.dataDate, "error message (commit with warning) is: " + genString(), el.rowId);
			assertEquals(0, getCount("DATA_IN", null));
			assertEquals(++errorsCount, getCount("DATA_IN_ERRORS", null));
			// check linkage of a log and an error by data_in_id and transfer_date
			assertEquals(1, getCount("DATA_IN_ERRORS", "DATA_IN_ID = " + el.inId + " AND ERROR_DATE IN (SELECT TRANSFER_DATE FROM " + db.getSchema() + "DATA_IN_LOG WHERE DATA_IN_ID = " + el.inId + " UNION ALL SELECT TRANSFER_DATE FROM " + db.getSchema() + "DATA_IN_LOG_ARCHIVE WHERE DATA_IN_ID = " + el.inId + ")"));
		}

		tryAssertEquals(0,"DATA_IN_LOG", null);
		tryAssertEquals(iterationCount,"DATA_IN_LOG_ARCHIVE", null);
	}

	@Order(2000)
	@ParameterizedTest
	@ValueSource(ints = {1})
	public void testOutExtSystem(int iterationCount) throws Exception
	{
		for(int i = 0; i < iterationCount; i++)
		{
			fillTestValues();

			for(String extSystem : extSystems)
			{
				long id = db.putOut(systemName, extSystem, dataType, dataValue, metaInfo, expireDate, null, null, null, null);
				assertTrue(id > 0);
				assertEquals(1, getCount("DATA_OUT", "MSG_ID = " + id));
				assertEquals(1, getCount("DATA_OUT_MSG", "MSG_ID = " + id));

				QElement el = db.getOut(systemName, extSystem, dataType.toString());
				checkOutRecord(el);
				assertEquals(extSystem, el.toSystem);
				db.commitOut(systemName, el.outId, null, el.rowId);
				assertEquals(0, getCount("DATA_OUT", null));
				assertEquals(0, getCount("DATA_OUT_ERRORS", null));
			}
		}

		tryAssertEquals(0,"DATA_OUT_LOG", null);
		tryAssertEquals(iterationCount * extSystems.size(),"DATA_OUT_LOG_ARCHIVE", null);
	}

	@Order(2001)
	@ParameterizedTest
	@ValueSource(ints = {1})
	public void testOutAnySystem1(int iterationCount) throws Exception
	{
		for(int i = 0; i < iterationCount; i++)
		{
			fillTestValues();

			long id = db.putOut(systemName, null, dataType, dataValue, metaInfo, expireDate, null, null, null, null);
			assertTrue(id > 0);
			assertEquals(extSystems.size(), getCount("DATA_OUT", "MSG_ID = " + id));
			assertEquals(1, getCount("DATA_OUT_MSG", "MSG_ID = " + id));

			int count = extSystems.size();
			for(String extSystem : extSystems)
			{
				QElement el = db.getOut(systemName, extSystem, dataType.toString());
				count--;
				checkOutRecord(el);
				assertEquals(extSystem, el.toSystem);
				db.commitOut(systemName, el.outId, null, el.rowId);
				assertEquals(count, getCount("DATA_OUT", null));
				assertEquals(0, getCount("DATA_OUT_ERRORS", null));
				el = db.getOut(systemName, extSystem, null);
				assertNull(el);
			}
		}

		tryAssertEquals(0,"DATA_OUT_LOG", null);
		tryAssertEquals(iterationCount * extSystems.size(),"DATA_OUT_LOG_ARCHIVE", null);
	}

	@Order(2002)
	@ParameterizedTest
	@ValueSource(ints = {1})
	public void testOutAnySystem2(int iterationCount) throws Exception
	{
		for(int i = 0; i < iterationCount; i++)
		{
			fillTestValues();

			long id = db.putOut(systemName, null, dataType, dataValue, metaInfo, expireDate, null, null, null, null);
			assertTrue(id > 0);
			assertEquals(extSystems.size(), getCount("DATA_OUT", "MSG_ID = " + id));
			assertEquals(1, getCount("DATA_OUT_MSG", "MSG_ID = " + id));

			// read any system and wrong data type
			QElement el = db.getOut(systemName, null, Integer.toString(Integer.MIN_VALUE));
			assertNull(el);

			// read wrong system and any data type
			el = db.getOut(systemName, genString(), null);
			assertNull(el);

			// read all systems and all data types, one by one
			for(int count = extSystems.size(); count > 0; )
			{
				count--;
				el = db.getOut(systemName, null, null);
				checkOutRecord(el);
				assertTrue(extSystems.contains(el.toSystem));
				db.commitOut(systemName, el.outId, null, el.rowId);
				assertEquals(count, getCount("DATA_OUT", null));
				assertEquals(0, getCount("DATA_OUT_ERRORS", null));
			}
		}

		tryAssertEquals(0,"DATA_OUT_LOG", null);
		tryAssertEquals(iterationCount * extSystems.size(),"DATA_OUT_LOG_ARCHIVE", null);
	}

	@Order(2003)
	@ParameterizedTest
	@ValueSource(ints = {1})
	public void testOutAnySystem3(int iterationCount) throws Exception
	{
		int errorsCount = 0;

		for(int i = 0; i < iterationCount; i++)
		{
			fillTestValues();
			String transId = "trans id is: " + genString();

			for(int j = 0; j <= iterationCount; j++)
			{
				// trans_seq_no = new Long(iterationCount - j) is inserting in descending order
				long id = db.putOut(systemName, null, dataType, dataValue, metaInfo, expireDate, null, transId, (long) (iterationCount - j), null);
				assertTrue(id > 0);
				assertEquals(extSystems.size(), getCount("DATA_OUT", "MSG_ID = " + id));
				assertEquals(1, getCount("DATA_OUT_MSG", "MSG_ID = " + id));
			}

			QElement el = db.getOut(systemName, null, null);
			assertNull(el);

			db.sendOutTrans(transId);

			for(int j = 0; j < (iterationCount + 1) * extSystems.size(); j++)
			{
				el = db.getOut(systemName, null, null);
				assertNotNull(el);
				checkOutRecord(el);
				// check correct order by trans_seq_no
				assertEquals(1, getCount("DATA_OUT", "DATA_OUT_ID = " + el.outId + " AND TRANS_SEQ_NO = " + (j / 3)));
				db.commitOut(systemName, el.outId, "error message (commit with warning) is: " + genString(), el.rowId);
				assertEquals(++errorsCount, getCount("DATA_OUT_ERRORS", null));
				// check linkage of a log and an error by data_out_id and transfer_date
				assertEquals(1, getCount("DATA_OUT_ERRORS", "DATA_OUT_ID = " + el.outId + " AND ERROR_DATE IN (SELECT TRANSFER_DATE FROM " + db.getSchema() + "DATA_OUT_LOG WHERE DATA_OUT_ID = " + el.outId + " UNION ALL SELECT TRANSFER_DATE FROM " + db.getSchema() + "DATA_OUT_LOG_ARCHIVE WHERE DATA_OUT_ID = " + el.outId + ")"));
			}
		}

		assertEquals(0, getCount("DATA_OUT", null));
		tryAssertEquals(0,"DATA_OUT_LOG", null);
		tryAssertEquals(iterationCount * (iterationCount + 1) * extSystems.size(),"DATA_OUT_LOG_ARCHIVE", null);
		assertEquals(errorsCount, getCount("DATA_OUT_ERRORS", null));
	}

	@Order(3000)
	@ParameterizedTest
	@ValueSource(ints = {1})
	public void testStage1(int iterationCount) throws Exception
	{
		for(int i = 1; i <= iterationCount; i++)
		{
			fillTestValues();
			long processAfterDelay = 2000;

			long id = db.putStage(systemName, outId.toString(), fromSystem, null, dataValue + " stage1", "stage1", processAfterDelay, false, null, null, metaInfo);
			assertTrue(id > 0);
			assertEquals(1, getCount("STAGES", "REQUEST_ID = " + id));
			assertEquals(1, getCount("STAGES_MSG", "MSG_ID IN (SELECT MSG_ID FROM " + db.getSchema() + "STAGES WHERE REQUEST_ID = " + id + ")"));

			List<SQElement> els = db.getStage(systemName, "stage1", fromSystem, 1);
			assertNotNull(els);
			assertEquals(0, els.size());

			Thread.sleep(processAfterDelay);
			els = db.getStage(systemName, "stage1", fromSystem, 1);
			assertNotNull(els);
			assertTrue(els.size() > 0);
			assertEquals(1L, els.get(0).iteration);
			checkStageRecord(els.get(0));
			db.commitStage(systemName, els.get(0).stageDate, els.get(0).requestId, 0, null, processAfterDelay, els.get(0).rowId);
			assertEquals(0, getCount("STAGES_ERRORS", "REQUEST_ID = " + id + " AND STAGE = '" + els.get(0).stage + "' AND ITERATION = " + els.get(0).iteration));

			Thread.sleep(processAfterDelay);
			els = db.getStage(systemName, "stage1", fromSystem, 1);
			assertNotNull(els);
			assertTrue(els.size() > 0);
			assertEquals(2L, els.get(0).iteration);
			checkStageRecord(els.get(0));
			db.commitStage(systemName, els.get(0).stageDate, els.get(0).requestId, 0, "error message (commit with warning) is: " + genString(), processAfterDelay, els.get(0).rowId);
			assertEquals(1, getCount("STAGES_ERRORS", "REQUEST_ID = " + id + " AND STAGE = '" + els.get(0).stage + "' AND ITERATION = " + els.get(0).iteration));

			Thread.sleep(processAfterDelay);
			els = db.getStage(systemName, "stage1", fromSystem, 1);
			assertNotNull(els);
			assertTrue(els.size() > 0);
			assertEquals(3L, els.get(0).iteration);
			checkStageRecord(els.get(0));

			id = db.putStage(systemName, outId.toString(), fromSystem, els.get(0).requestId, dataValue + " stage2", "stage2", processAfterDelay, false, null, null, metaInfo);
			assertTrue(id > 0);
			assertEquals(1, getCount("STAGES", "REQUEST_ID = " + id));
			assertEquals(1, getCount("STAGES_MSG", "MSG_ID IN (SELECT MSG_ID FROM " + db.getSchema() + "STAGES WHERE REQUEST_ID = " + id + ")"));
			assertEquals(1, getCount("STAGES_LOG", "STAGE = 'stage1' AND REQUEST_ID = " + id) + getCount("STAGES_LOG_ARCHIVE", "STAGE = 'stage1' AND REQUEST_ID = " + id));
			db.commitStage(systemName, els.get(0).stageDate, els.get(0).requestId, 0, "warning after changing stage", processAfterDelay, els.get(0).rowId);
			assertEquals(1, getCount("STAGES_ERRORS", "REQUEST_ID = " + id + " AND STAGE = '" + els.get(0).stage + "' AND ITERATION = " + els.get(0).iteration));

			Thread.sleep(processAfterDelay);
			els = db.getStage(systemName, "stage2", fromSystem, 1);
			assertNotNull(els);
			assertTrue(els.size() > 0);
			assertEquals(1L, els.get(0).iteration);
			checkStageRecord(els.get(0));
			id = db.putStage(systemName, outId.toString(), fromSystem, els.get(0).requestId, dataValue + " stage3", "stage3", processAfterDelay, true, null, null, metaInfo);
			assertTrue(id > 0);
			assertEquals(0, getCount("STAGES", "REQUEST_ID = " + id));
			assertEquals(1, getCount("STAGES_MSG", "MSG_ID IN (SELECT MSG_ID FROM " + db.getSchema() + "STAGES_LOG_ARCHIVE WHERE STAGE = 'stage3' AND REQUEST_ID = " + id + ")"));
			db.commitStage(systemName, els.get(0).stageDate, els.get(0).requestId, 0, "warning after final", processAfterDelay, els.get(0).rowId);
			assertEquals(1, getCount("STAGES_ERRORS", "REQUEST_ID = " + id + " AND STAGE = '" + els.get(0).stage + "' AND ITERATION = " + els.get(0).iteration));
		}

		tryAssertEquals(0,"STAGES_LOG", null);
		tryAssertEquals(3,"STAGES_MSG", "MSG_ID IN (SELECT MSG_ID FROM " + db.getSchema() + "STAGES_LOG_ARCHIVE)");
	}

	@Order(3001)
	@ParameterizedTest
	@ValueSource(ints = {1})
	public void testStage2(int iterationCount) throws Exception
	{
		for(int i = 1; i <= iterationCount; i++)
		{
			fillTestValues();
			String depId = genString();
			long processAfterDelay = 3000;

			long id1 = db.putStage(systemName, outId.toString(), fromSystem, null, dataValue + " stage1", "stage1", processAfterDelay, false, depId, null, metaInfo);
			assertTrue(id1 > 0);

			List<SQElement> els = db.getStage(systemName, "stage1", fromSystem, 2);
			assertNotNull(els);
			assertEquals(0, els.size());

			long id2 = db.putStage(systemName, outId.toString(), fromSystem, null, dataValue + " stage1", "stage1", 0, false, null, depId, metaInfo);
			assertTrue(id2 > 0);

			els = db.getStage(systemName, "stage1", fromSystem, 2);
			assertNotNull(els);
			assertEquals(0, els.size());

			long id3 = db.putStage(systemName, outId.toString(), fromSystem, null, dataValue + " stage1", "stage1", 0, false, null, null, metaInfo);
			assertTrue(id2 > 0);

			SQElement el = db.getFinalStage(systemName, id3);
			assertNull(el);

			els = db.getStage(systemName, "stage1", fromSystem, 2);
			assertNotNull(els);
			assertEquals(1, els.size());
			assertEquals(id3, els.get(0).requestId);
			checkStageRecord(els.get(0));

			int rollbackCode = -1;
			assertEquals(0, getCount("STAGES_ERRORS", "REQUEST_ID = " + id3 + " AND STAGE = '" + els.get(0).stage + "' AND ITERATION = " + els.get(0).iteration));
			db.rollbackStage(systemName, els.get(0).stageDate, els.get(0).requestId, rollbackCode, "just test rollback", els.get(0).rowId);
			assertEquals(1, getCount("STAGES_ERRORS", "REQUEST_ID = " + id3 + " AND STAGE = '" + els.get(0).stage + "' AND ITERATION = " + els.get(0).iteration +
					" AND ERROR_CODE = " + rollbackCode));

			id3 = db.putStage(systemName, outId.toString(), fromSystem, els.get(0).requestId, dataValue + " stage2", "stage2", 0, true, null, null, metaInfo);
			assertTrue(id3 > 0);
			db.commitStage(systemName, els.get(0).stageDate, els.get(0).requestId, 0, null, 0, els.get(0).rowId);

			el = db.getFinalStage(systemName, id3);
			assertNotNull(el);
			assertEquals(id3, el.requestId);
			checkStageRecord(el);

			els = db.getStage(systemName, "stage1", fromSystem, 2);
			assertNotNull(els);
			assertEquals(0, els.size());

			Thread.sleep(processAfterDelay);

			els = db.getStage(systemName, "stage1", fromSystem, 2);
			assertNotNull(els);
			assertTrue(els.size() > 0);
			assertEquals(id1, els.get(0).requestId);
			checkStageRecord(els.get(0));
			id1 = db.putStage(systemName, outId.toString(), fromSystem, els.get(0).requestId, dataValue + " stage2", "stage2", 0, true, null, null, metaInfo);
			assertTrue(id1 > 0);
			db.commitStage(systemName, els.get(0).stageDate, els.get(0).requestId, 0, null, 0, els.get(0).rowId);

			els = db.getStage(systemName, "stage1", fromSystem, 2);
			assertNotNull(els);
			assertTrue(els.size() > 0);
			assertEquals(id2, els.get(0).requestId);
			checkStageRecord(els.get(0));

			id2 = db.putStage(systemName, outId.toString(), fromSystem, els.get(0).requestId, dataValue + " stage2", "stage2", 0, true, null, null, metaInfo);
			assertTrue(id2 > 0);
			db.commitStage(systemName, els.get(0).stageDate, els.get(0).requestId, 0, null, 0, els.get(0).rowId);
		}
		Thread.sleep(500); // delay required for log mover
	}
}
