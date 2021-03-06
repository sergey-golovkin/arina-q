CREATE SEQUENCE Q_<SYSID>.SQ_STAGES
  START WITH 1
  MAXVALUE 999999999999999999
  MINVALUE 1
  NOCYCLE
  NOCACHE
  NOORDER
~
CREATE TABLE Q_<SYSID>.STAGES_MSG
(
  MSG_ID NUMBER(18) NOT NULL PRIMARY KEY,
  DATA_VALUE CLOB,
  META_INFO CLOB
)
LOB (DATA_VALUE) STORE AS SECUREFILE (DEDUPLICATE COMPRESS HIGH CACHE)
LOB (META_INFO) STORE AS SECUREFILE (DEDUPLICATE COMPRESS HIGH CACHE)
~
CREATE TABLE Q_<SYSID>.STAGES
(
  REQUEST_ID NUMBER(18) NOT NULL PRIMARY KEY,
  REQUEST_DATE TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
  MSG_ID NUMBER(18) NOT NULL,
  EXT_ID VARCHAR2(50 CHAR),
  SYS_ID VARCHAR2(50 CHAR),
  STAGE_DATE TIMESTAMP WITH TIME ZONE NOT NULL,
  STAGE VARCHAR2(50 CHAR) NOT NULL,
  ITERATION NUMBER(18) NOT NULL,
  PROCESS_AFTER TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
  DEP_ID VARCHAR2(100 CHAR),
  PARENT_DEP_ID VARCHAR2(4000 CHAR)
)
~
CREATE INDEX Q_<SYSID>.I_STAGES_1 ON Q_<SYSID>.STAGES (PROCESS_AFTER, STAGE, SYS_ID)
~
CREATE INDEX Q_<SYSID>.I_STAGES_2 ON Q_<SYSID>.STAGES (DEP_ID)
~
CREATE TABLE Q_<SYSID>.STAGES_LOG
(
  REQUEST_ID NUMBER(18) NOT NULL,
  REQUEST_DATE TIMESTAMP WITH TIME ZONE NOT NULL,
  MSG_ID NUMBER(18) NOT NULL,
  EXT_ID VARCHAR2(50 CHAR),
  SYS_ID VARCHAR2(50 CHAR),
  STAGE_DATE TIMESTAMP WITH TIME ZONE NOT NULL,
  STAGE VARCHAR2(50 CHAR) NOT NULL,
  ITERATION NUMBER(18) NOT NULL,
  PROCESS_AFTER TIMESTAMP WITH TIME ZONE NOT NULL,
  DEP_ID VARCHAR2(100 CHAR),
  PARENT_DEP_ID VARCHAR2(4000 CHAR),
  TRANSFER_DATE TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
  FINAL NUMBER(1) DEFAULT 0 NOT NULL CHECK (FINAL IN(1, 0))
)
~
CREATE INDEX Q_<SYSID>.I_STAGES_LOG_1 ON Q_<SYSID>.STAGES_LOG (REQUEST_ID)
~
CREATE TABLE Q_<SYSID>.STAGES_LOG_ARCHIVE
(
  REQUEST_ID NUMBER(18) NOT NULL,
  REQUEST_DATE TIMESTAMP WITH TIME ZONE NOT NULL,
  MSG_ID NUMBER(18) NOT NULL,
  EXT_ID VARCHAR2(50 CHAR),
  SYS_ID VARCHAR2(50 CHAR),
  STAGE_DATE TIMESTAMP WITH TIME ZONE NOT NULL,
  STAGE VARCHAR2(50 CHAR) NOT NULL,
  ITERATION NUMBER(18) NOT NULL,
  PROCESS_AFTER TIMESTAMP WITH TIME ZONE NOT NULL,
  DEP_ID VARCHAR2(100 CHAR),
  PARENT_DEP_ID VARCHAR2(4000 CHAR),
  TRANSFER_DATE TIMESTAMP WITH TIME ZONE NOT NULL,
  FINAL NUMBER(1) NOT NULL
)
~
CREATE INDEX Q_<SYSID>.I_STAGES_LOG_ARCHIVE_1 ON Q_<SYSID>.STAGES_LOG_ARCHIVE (REQUEST_ID, FINAL)
~
CREATE INDEX Q_<SYSID>.I_STAGES_LOG_ARCHIVE_2 ON Q_<SYSID>.STAGES_LOG_ARCHIVE (REQUEST_DATE)
~
CREATE INDEX Q_<SYSID>.I_STAGES_LOG_ARCHIVE_3 ON Q_<SYSID>.STAGES_LOG_ARCHIVE (EXT_ID, SYS_ID)
~
CREATE INDEX Q_<SYSID>.I_STAGES_LOG_ARCHIVE_4 ON Q_<SYSID>.STAGES_LOG_ARCHIVE (STAGE_DATE, REQUEST_ID)
~
CREATE INDEX Q_<SYSID>.I_STAGES_LOG_ARCHIVE_5 ON Q_<SYSID>.STAGES_LOG_ARCHIVE (TRANSFER_DATE)
~
CREATE TABLE Q_<SYSID>.STAGES_ERRORS
(
  REQUEST_ID NUMBER(18) NOT NULL,
  STAGE VARCHAR2(50 CHAR) NOT NULL,
  ITERATION NUMBER(18) NOT NULL,
  ERROR_DATE TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
  ERROR_CODE NUMBER DEFAULT 0 NOT NULL,
  ERROR_TEXT CLOB
)
LOB (ERROR_TEXT) STORE AS SECUREFILE (DEDUPLICATE COMPRESS HIGH CACHE)
~
CREATE INDEX Q_<SYSID>.I_STAGES_ERRORS_1 ON Q_<SYSID>.STAGES_ERRORS (REQUEST_ID, STAGE)
~
CREATE INDEX Q_<SYSID>.I_STAGES_ERRORS_2 ON Q_<SYSID>.STAGES_ERRORS (ERROR_CODE)
~
CREATE INDEX Q_<SYSID>.I_STAGES_ERRORS_3 ON Q_<SYSID>.STAGES_ERRORS (ERROR_DATE)
~
CREATE TABLE Q_<SYSID>.STAGES_DEPS
(
  REQUEST_ID NUMBER(18) NOT NULL,
  PARENT_REQUEST_ID NUMBER(18) NOT NULL
)
~
CREATE INDEX Q_<SYSID>.I_STAGES_DEPS_1 ON Q_<SYSID>.STAGES_DEPS (REQUEST_ID)
~
CREATE INDEX Q_<SYSID>.I_STAGES_DEPS_2 ON Q_<SYSID>.STAGES_DEPS (PARENT_REQUEST_ID)
~
ALTER TABLE Q_<SYSID>.STAGES_DEPS ADD
 FOREIGN KEY (REQUEST_ID)
 REFERENCES Q_<SYSID>.STAGES (REQUEST_ID)
 ON DELETE CASCADE
 DEFERRABLE
 INITIALLY DEFERRED
 ENABLE
 VALIDATE
~
ALTER TABLE Q_<SYSID>.STAGES_DEPS ADD
 FOREIGN KEY (PARENT_REQUEST_ID)
 REFERENCES Q_<SYSID>.STAGES (REQUEST_ID)
 ON DELETE CASCADE
 DEFERRABLE
 INITIALLY DEFERRED
 ENABLE
 VALIDATE
~
CREATE OR REPLACE FUNCTION Q_<SYSID>.PUT_STAGE_VALUE_11_1
(
  P_EXT_ID VARCHAR2,
  P_SYS_ID VARCHAR2,
  P_REQUEST_ID NUMBER,
  P_DATA_VALUE CLOB,
  P_STAGE VARCHAR2,
  P_PROCESS_AFTER_DELAY NUMBER,
  P_FINAL NUMBER,
  P_DEP_ID VARCHAR2,
  P_PARENT_DEP_ID VARCHAR2,
  P_META_INFO CLOB
)
  RETURN NUMBER
AS
    V_MSG_ID NUMBER (18);
    V_TRANSFER_DATE TIMESTAMP WITH TIME ZONE;
BEGIN
    V_MSG_ID := SQ_STAGES.NEXTVAL;

    INSERT INTO STAGES_MSG
    (
      MSG_ID,
      DATA_VALUE,
      META_INFO
    )
    VALUES
    (
      V_MSG_ID,
      P_DATA_VALUE,
      P_META_INFO
    );

    IF P_REQUEST_ID IS NULL
    THEN
        INSERT INTO STAGES
        (
          REQUEST_ID,
          REQUEST_DATE,
          MSG_ID,
          EXT_ID,
          SYS_ID,
          STAGE_DATE,
          STAGE,
          ITERATION,
          PROCESS_AFTER,
          DEP_ID,
          PARENT_DEP_ID
        )
        VALUES
        (
          V_MSG_ID,
          SYSTIMESTAMP,
          V_MSG_ID,
          P_EXT_ID,
          P_SYS_ID,
          SYSTIMESTAMP,
          P_STAGE,
          1,
          SYSTIMESTAMP + NUMTODSINTERVAL(0.001 * P_PROCESS_AFTER_DELAY, 'second'),
          P_DEP_ID,
          P_PARENT_DEP_ID
        );

        IF P_PARENT_DEP_ID IS NOT NULL THEN
            INSERT INTO STAGES_DEPS
            (
                  REQUEST_ID,
                  PARENT_REQUEST_ID
            )
            SELECT V_MSG_ID, REQUEST_ID
            FROM STAGES
            WHERE REQUEST_ID < V_MSG_ID AND
                  DEP_ID IN (SELECT REGEXP_SUBSTR(P_PARENT_DEP_ID, '[^,|;]+', 1, LEVEL) FROM DUAL CONNECT BY REGEXP_SUBSTR(P_PARENT_DEP_ID, '[^,|;]+', 1, LEVEL) IS NOT NULL);
        END IF;
    ELSE
        V_TRANSFER_DATE := SYSTIMESTAMP;

        INSERT INTO STAGES_LOG
        (
          REQUEST_ID,
          REQUEST_DATE,
          MSG_ID,
          EXT_ID,
          SYS_ID,
          STAGE_DATE,
          STAGE,
          ITERATION,
          PROCESS_AFTER,
          TRANSFER_DATE,
          DEP_ID,
          PARENT_DEP_ID
        )
        SELECT
          REQUEST_ID,
          REQUEST_DATE,
          MSG_ID,
          EXT_ID,
          SYS_ID,
          STAGE_DATE,
          STAGE,
          ITERATION,
          PROCESS_AFTER,
          V_TRANSFER_DATE,
          DEP_ID,
          PARENT_DEP_ID
        FROM STAGES
        WHERE REQUEST_ID = P_REQUEST_ID;

        IF P_FINAL != 0
        THEN
            INSERT INTO STAGES_LOG_ARCHIVE
            (
              REQUEST_ID,
              REQUEST_DATE,
              MSG_ID,
              EXT_ID,
              SYS_ID,
              STAGE_DATE,
              STAGE,
              ITERATION,
              PROCESS_AFTER,
              DEP_ID,
              PARENT_DEP_ID,
              TRANSFER_DATE,
              FINAL
            )
            SELECT
              REQUEST_ID,
              REQUEST_DATE,
              V_MSG_ID,
              EXT_ID,
              SYS_ID,
              V_TRANSFER_DATE,
              P_STAGE,
              1,
              V_TRANSFER_DATE,
              DEP_ID,
              PARENT_DEP_ID,
              V_TRANSFER_DATE,
              P_FINAL
            FROM STAGES
            WHERE REQUEST_ID = P_REQUEST_ID;

            DELETE FROM STAGES
            WHERE REQUEST_ID = P_REQUEST_ID;
        ELSE
            UPDATE STAGES
            SET
              STAGE_DATE = V_TRANSFER_DATE,
              MSG_ID = V_MSG_ID,
              STAGE = P_STAGE,
              ITERATION = 1,
              PROCESS_AFTER = V_TRANSFER_DATE + NUMTODSINTERVAL(0.001 * P_PROCESS_AFTER_DELAY, 'second')
            WHERE REQUEST_ID = P_REQUEST_ID;
        END IF;
    END IF;

    RETURN NVL(P_REQUEST_ID, V_MSG_ID);
END;
~
CREATE OR REPLACE TYPE Q_<SYSID>.STQ_ROW_10 AS OBJECT
(
  ROW_ID VARCHAR2(128),
  EXT_ID VARCHAR2(50 CHAR),
  SYS_ID VARCHAR2(50 CHAR),
  REQUEST_DATE TIMESTAMP WITH TIME ZONE,
  STAGE_DATE TIMESTAMP WITH TIME ZONE,
  REQUEST_ID NUMBER(18),
  DATA_VALUE CLOB,
  META_INFO CLOB,
  STAGE VARCHAR2(50 CHAR),
  ITERATION NUMBER(18)
)
~
CREATE OR REPLACE TYPE Q_<SYSID>.STQ_TABLE_10 IS TABLE OF Q_<SYSID>.STQ_ROW_10
~
CREATE OR REPLACE FUNCTION Q_<SYSID>.GET_STAGE_VALUE_3_10(P_STAGE VARCHAR2, P_SYS_ID VARCHAR2 DEFAULT NULL, P_RECORDS NUMBER DEFAULT 1) RETURN STQ_TABLE_10
AS
  V_TABLE STQ_TABLE_10 := STQ_TABLE_10();
BEGIN
    IF P_SYS_ID IS NULL THEN
        SELECT CAST(MULTISET(SELECT /*+INDEX_ASC(STAGES, I_STAGES_1)*/ ROWIDTOCHAR(STAGES.ROWID) ROW_ID, EXT_ID, SYS_ID, REQUEST_DATE, STAGE_DATE, REQUEST_ID, DATA_VALUE, META_INFO, STAGE, ITERATION FROM STAGES, STAGES_MSG WHERE PROCESS_AFTER <= SYSTIMESTAMP AND STAGE LIKE P_STAGE AND ROWNUM <= P_RECORDS AND STAGES.MSG_ID = STAGES_MSG.MSG_ID AND NOT EXISTS (SELECT 1 FROM STAGES_DEPS WHERE STAGES_DEPS.REQUEST_ID = STAGES.REQUEST_ID)) AS STQ_TABLE_10) INTO V_TABLE FROM DUAL;
    ELSE
        SELECT CAST(MULTISET(SELECT /*+INDEX_ASC(STAGES, I_STAGES_1)*/ ROWIDTOCHAR(STAGES.ROWID) ROW_ID, EXT_ID, SYS_ID, REQUEST_DATE, STAGE_DATE, REQUEST_ID, DATA_VALUE, META_INFO, STAGE, ITERATION FROM STAGES, STAGES_MSG WHERE PROCESS_AFTER <= SYSTIMESTAMP AND STAGE LIKE P_STAGE AND ROWNUM <= P_RECORDS AND STAGES.MSG_ID = STAGES_MSG.MSG_ID AND NOT EXISTS (SELECT 1 FROM STAGES_DEPS WHERE STAGES_DEPS.REQUEST_ID = STAGES.REQUEST_ID) AND SYS_ID = P_SYS_ID) AS STQ_TABLE_10) INTO V_TABLE FROM DUAL;
    END IF;

    RETURN V_TABLE;
    EXCEPTION
    WHEN OTHERS
    THEN
    RETURN V_TABLE;
END;
~
CREATE OR REPLACE FUNCTION Q_<SYSID>.GET_FINAL_STAGE_VALUE_1_10(P_REQUEST_ID NUMBER DEFAULT 1) RETURN STQ_TABLE_10
AS
  V_TABLE STQ_TABLE_10 := STQ_TABLE_10();
BEGIN
    SELECT CAST(MULTISET(SELECT ROWIDTOCHAR(STAGES_LOG_ARCHIVE.ROWID) ROW_ID, EXT_ID, SYS_ID, REQUEST_DATE, STAGE_DATE, REQUEST_ID, DATA_VALUE, META_INFO, STAGE, ITERATION FROM STAGES_LOG_ARCHIVE, STAGES_MSG WHERE STAGES_LOG_ARCHIVE.MSG_ID = STAGES_MSG.MSG_ID AND REQUEST_ID = P_REQUEST_ID AND FINAL = 1) AS STQ_TABLE_10) INTO V_TABLE FROM DUAL;
    RETURN V_TABLE;
    EXCEPTION
    WHEN OTHERS
    THEN
    RETURN V_TABLE;
END;
~
CREATE OR REPLACE PROCEDURE Q_<SYSID>.COMMIT_STAGE_PROCESSING_8(P_STAGE_DATE TIMESTAMP WITH TIME ZONE, P_REQUEST_ID NUMBER, P_ERROR_CODE NUMBER DEFAULT 0, P_ERROR_TEXT CLOB DEFAULT NULL, P_PROCESS_AFTER_DELAY NUMBER DEFAULT 0, P_ROW_ID VARCHAR2 DEFAULT NULL)
AS
BEGIN
    IF P_ROW_ID IS NOT NULL THEN
        IF P_ERROR_TEXT IS NOT NULL
        THEN
            INSERT INTO STAGES_ERRORS (REQUEST_ID, STAGE, ITERATION, ERROR_DATE, ERROR_CODE, ERROR_TEXT)
            SELECT REQUEST_ID, STAGE, ITERATION, SYSTIMESTAMP, P_ERROR_CODE, P_ERROR_TEXT FROM STAGES WHERE ROWID = CHARTOROWID(P_ROW_ID) AND STAGE_DATE = P_STAGE_DATE;

            IF SQL%ROWCOUNT = 0 THEN
                INSERT INTO STAGES_ERRORS (REQUEST_ID, STAGE, ITERATION, ERROR_DATE, ERROR_CODE, ERROR_TEXT)
                SELECT REQUEST_ID, STAGE, ITERATION, SYSTIMESTAMP, P_ERROR_CODE, P_ERROR_TEXT FROM STAGES_LOG WHERE STAGE_DATE = P_STAGE_DATE AND REQUEST_ID = P_REQUEST_ID
                UNION ALL
                SELECT REQUEST_ID, STAGE, ITERATION, SYSTIMESTAMP, P_ERROR_CODE, P_ERROR_TEXT FROM STAGES_LOG_ARCHIVE WHERE STAGE_DATE = P_STAGE_DATE AND REQUEST_ID = P_REQUEST_ID;
            END IF;
        END IF;

        UPDATE STAGES
        SET
            ITERATION = ITERATION + 1,
            PROCESS_AFTER = SYSTIMESTAMP + NUMTODSINTERVAL(0.001 * P_PROCESS_AFTER_DELAY, 'second')
        WHERE ROWID = CHARTOROWID(P_ROW_ID) AND STAGE_DATE = P_STAGE_DATE;
    ELSE
        IF P_ERROR_TEXT IS NOT NULL
        THEN
            INSERT INTO STAGES_ERRORS (REQUEST_ID, STAGE, ITERATION, ERROR_DATE, ERROR_CODE, ERROR_TEXT)
            SELECT REQUEST_ID, STAGE, ITERATION, SYSTIMESTAMP, P_ERROR_CODE, P_ERROR_TEXT FROM STAGES WHERE REQUEST_ID = P_REQUEST_ID AND STAGE_DATE = P_STAGE_DATE;

            IF SQL%ROWCOUNT = 0 THEN
                INSERT INTO STAGES_ERRORS (REQUEST_ID, STAGE, ITERATION, ERROR_DATE, ERROR_CODE, ERROR_TEXT)
                SELECT REQUEST_ID, STAGE, ITERATION, SYSTIMESTAMP, P_ERROR_CODE, P_ERROR_TEXT FROM STAGES_LOG WHERE STAGE_DATE = P_STAGE_DATE AND REQUEST_ID = P_REQUEST_ID
                UNION ALL
                SELECT REQUEST_ID, STAGE, ITERATION, SYSTIMESTAMP, P_ERROR_CODE, P_ERROR_TEXT FROM STAGES_LOG_ARCHIVE WHERE STAGE_DATE = P_STAGE_DATE AND REQUEST_ID = P_REQUEST_ID;
            END IF;
        END IF;

        UPDATE STAGES
        SET
            ITERATION = ITERATION + 1,
            PROCESS_AFTER = SYSTIMESTAMP + NUMTODSINTERVAL(0.001 * P_PROCESS_AFTER_DELAY, 'second')
        WHERE REQUEST_ID = P_REQUEST_ID AND STAGE_DATE = P_STAGE_DATE;
    END IF;
  END;
~
CREATE OR REPLACE PROCEDURE Q_<SYSID>.ROLLBACK_STAGE_PROCESSING_6(P_STAGE_DATE TIMESTAMP WITH TIME ZONE, P_REQUEST_ID NUMBER, P_ERROR_CODE NUMBER, P_ERROR_TEXT CLOB DEFAULT NULL, P_ROW_ID VARCHAR2 DEFAULT NULL)
AS
BEGIN
    IF P_ROW_ID IS NOT NULL THEN
        INSERT INTO STAGES_ERRORS (REQUEST_ID, STAGE, ITERATION, ERROR_DATE, ERROR_CODE, ERROR_TEXT)
        SELECT REQUEST_ID, STAGE, ITERATION, SYSTIMESTAMP, P_ERROR_CODE, P_ERROR_TEXT FROM STAGES WHERE ROWID = CHARTOROWID(P_ROW_ID) AND STAGE_DATE = P_STAGE_DATE;
    ELSE
        INSERT INTO STAGES_ERRORS (REQUEST_ID, STAGE, ITERATION, ERROR_DATE, ERROR_CODE, ERROR_TEXT)
        SELECT REQUEST_ID, STAGE, ITERATION, SYSTIMESTAMP, P_ERROR_CODE, P_ERROR_TEXT FROM STAGES WHERE REQUEST_ID = P_REQUEST_ID AND STAGE_DATE = P_STAGE_DATE;
    END IF;
END;
~
CREATE OR REPLACE FUNCTION Q_<SYSID>.MOVE_STAGE_TO_LOG (P_ROWS NUMBER DEFAULT 1) RETURN NUMBER
AS
   V_LOCKHANDLE VARCHAR2 (200);
   V_RESULT     NUMBER;
   V_COUNT      NUMBER;
   V_ROWID      ROWID;
BEGIN
   V_COUNT := 0;
   DBMS_LOCK.ALLOCATE_UNIQUE ('Q_<SYSID>_MOVE_STAGE_TO_LOG', V_LOCKHANDLE);

   IF DBMS_LOCK.REQUEST (V_LOCKHANDLE, DBMS_LOCK.X_MODE, 0, FALSE) = 0
   THEN
      BEGIN
         BEGIN
            FOR I IN 1 .. P_ROWS
            LOOP
               SELECT ROWID INTO V_ROWID FROM STAGES_LOG WHERE ROWNUM < 2;

               INSERT INTO STAGES_LOG_ARCHIVE
               (
                      REQUEST_ID,
                      REQUEST_DATE,
                      MSG_ID,
                      EXT_ID,
                      SYS_ID,
                      STAGE_DATE,
                      STAGE,
                      ITERATION,
                      PROCESS_AFTER,
                      DEP_ID,
                      PARENT_DEP_ID,
                      TRANSFER_DATE,
                      FINAL
               )
               SELECT REQUEST_ID,
                      REQUEST_DATE,
                      MSG_ID,
                      EXT_ID,
                      SYS_ID,
                      STAGE_DATE,
                      STAGE,
                      ITERATION,
                      PROCESS_AFTER,
                      DEP_ID,
                      PARENT_DEP_ID,
                      TRANSFER_DATE,
                      FINAL
               FROM STAGES_LOG
               WHERE ROWID = V_ROWID;

               DELETE FROM STAGES_LOG WHERE ROWID = V_ROWID;

               V_COUNT := V_COUNT + 1;
            END LOOP;
         EXCEPTION
            WHEN OTHERS
            THEN
               NULL;
         END;

         V_RESULT := DBMS_LOCK.RELEASE (V_LOCKHANDLE);
      END;
   END IF;
   RETURN V_COUNT;
END;
~
GRANT EXECUTE ON Q_<SYSID>.PUT_STAGE_VALUE_11_1 TO Q_<SYSID>_SVC_ROLE
~
GRANT EXECUTE ON Q_<SYSID>.GET_STAGE_VALUE_3_10 TO Q_<SYSID>_SVC_ROLE
~
GRANT EXECUTE ON Q_<SYSID>.GET_FINAL_STAGE_VALUE_1_10 TO Q_<SYSID>_SVC_ROLE
~
GRANT EXECUTE ON Q_<SYSID>.COMMIT_STAGE_PROCESSING_8 TO Q_<SYSID>_SVC_ROLE
~
GRANT EXECUTE ON Q_<SYSID>.ROLLBACK_STAGE_PROCESSING_6 TO Q_<SYSID>_SVC_ROLE
~
GRANT EXECUTE ON Q_<SYSID>.MOVE_STAGE_TO_LOG TO Q_<SYSID>_SVC_ROLE
~
