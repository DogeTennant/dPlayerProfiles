package com.dogetennant.dplayerprofiles.database;

import com.dogetennant.dplayerprofiles.config.MainConfig;
import com.dogetennant.dplayerprofiles.util.LogUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Opens the real SQLiteManager on a temporary folder and the MySQLManager on H2 in MySQL mode. */
final class TestDatabases {

    private TestDatabases() {
    }

    /** dPlayerProfiles logs through a static logger that the plugin normally sets up. */
    static synchronized void initLogging() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("dPlayerProfiles-test"));
        LogUtil.init(plugin);
    }

    static SQLiteManager sqlite(Path dataFolder, String prefix) throws SQLException {
        initLogging();
        SQLiteManager manager = new SQLiteManager(dataFolder.toFile(), prefix);
        manager.initialize();
        return manager;
    }

    /** A fresh in-memory H2 database in MySQL mode (one per call). */
    static DataSource h2() {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:dpp" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        return indexNamesPerDatabase(h2);
    }

    /** A MySQLManager on the given H2 database, with its tables created. */
    static MySQLManager h2MySql(DataSource h2, String prefix) throws SQLException {
        MySQLManager manager = h2Connect(h2, prefix);
        manager.createTables();
        return manager;
    }

    /** A MySQLManager on the given H2 database, tables not created yet. */
    static MySQLManager h2Connect(DataSource h2, String prefix) {
        initLogging();
        HikariConfig hikari = new HikariConfig();
        hikari.setDataSource(h2);
        hikari.setMaximumPoolSize(2);
        MainConfig config = new MainConfig();
        config.tablePrefix = prefix;
        MySQLManager manager = new MySQLManager(config);
        // connect() would open a jdbc:mysql: URL; the pool is set directly instead
        manager.dataSource = new HikariDataSource(hikari);
        return manager;
    }

    private static final Pattern INDEX = Pattern.compile("\\b(UNIQUE KEY|INDEX|KEY) (\\w+) \\(");
    private static final AtomicInteger INDEX_NAMES = new AtomicInteger();

    /**
     * MySQL scopes index names to their table, H2 to the whole database, so two table sets
     * (two prefixes) declaring {@code INDEX idx_username} fail on H2 only. This renames such
     * indexes in DDL on the way to H2; all other SQL passes unchanged.
     */
    private static DataSource indexNamesPerDatabase(JdbcDataSource h2) {
        ClassLoader loader = TestDatabases.class.getClassLoader();
        return (DataSource) Proxy.newProxyInstance(loader, new Class<?>[] {DataSource.class}, (dsProxy, dsMethod, dsArgs) -> {
            Object result = invoke(h2, dsMethod, dsArgs);
            if (!(result instanceof Connection con)) return result;
            return Proxy.newProxyInstance(loader, new Class<?>[] {Connection.class}, (conProxy, conMethod, conArgs) -> {
                Object made = invoke(con, conMethod, conArgs);
                if (!(made instanceof Statement stmt) || made instanceof PreparedStatement) return made;
                return Proxy.newProxyInstance(loader, new Class<?>[] {Statement.class}, (stProxy, stMethod, stArgs) -> {
                    if (stArgs != null && stArgs.length > 0 && stArgs[0] instanceof String sql && sql.contains("CREATE TABLE")) {
                        stArgs[0] = INDEX.matcher(sql).replaceAll(m ->
                                m.group(1) + " " + m.group(2) + "_" + INDEX_NAMES.incrementAndGet() + " (");
                    }
                    return invoke(stmt, stMethod, stArgs);
                });
            });
        });
    }

    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
