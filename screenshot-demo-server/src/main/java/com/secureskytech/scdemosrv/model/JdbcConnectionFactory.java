package com.secureskytech.scdemosrv.model;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbutils.DbUtils;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcConnectionFactory {
    private static final Logger LOG = LoggerFactory.getLogger(JdbcConnectionFactory.class);

    private final String jdbcurl;

    public JdbcConnectionFactory() {
        this("demodb");
    }

    public JdbcConnectionFactory(String dbname) {
        jdbcurl = "jdbc:h2:mem:" + dbname + ";DB_CLOSE_DELAY=-1";
    }

    public static void migrate(final JdbcConnectionFactory cf) {
        Connection conn = null;
        try {
            conn = cf.newConnection();
            DataSource ds = new CustomFlywayDataSource(new PrintWriter(System.out), conn);
            Flyway flyway = new Flyway();
            flyway.setDataSource(ds);
            flyway.migrate();
            LOG.info("db migration success");
        } catch (SQLException e) {
            LOG.error("db migration error", e);
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    public Connection newConnection() throws SQLException {
        return DriverManager.getConnection(jdbcurl, "sa", "");
    }

}
