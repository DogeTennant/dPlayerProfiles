package com.dogetennant.dplayerprofiles.database;

import javax.sql.DataSource;

/**
 * The contract against the MySQL dialect, run on H2 in MySQL mode. That checks the SQL the
 * plugin sends ({@code ON DUPLICATE KEY UPDATE}, {@code INSERT IGNORE}, {@code SHOW TABLES}),
 * not MySQL itself; runs without Docker. {@link RealMySqlManagerTest} runs it on MySQL.
 */
class MySQLManagerTest extends DatabaseManagerContractTest {

    /** One database per test, shared by every table set the test opens. */
    private DataSource h2;

    @Override
    protected DatabaseManager connect(String prefix) throws Exception {
        if (h2 == null) {
            h2 = TestDatabases.h2();
        }
        return TestDatabases.h2Connect(h2, prefix);
    }

    /** H2 cannot parse them; RealMySqlManagerTest covers them. */
    @Override
    protected boolean runsMergeStatements() {
        return false;
    }
}
