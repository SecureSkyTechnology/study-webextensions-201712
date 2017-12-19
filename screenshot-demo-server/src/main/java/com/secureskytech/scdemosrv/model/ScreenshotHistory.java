package com.secureskytech.scdemosrv.model;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.AbstractListHandler;

import com.google.common.base.Strings;

import lombok.Data;

@Data
public class ScreenshotHistory {

    String logContext;
    long messageRef;
    long id;
    String url;
    String src;
    Timestamp createdAt;
    String comment;

    public static ScreenshotHistory create(ResultSet rs) throws SQLException {
        ScreenshotHistory r = new ScreenshotHistory();
        r.logContext = rs.getString("log_context");
        r.messageRef = rs.getLong("message_ref");
        r.id = rs.getLong("sc_id");
        r.url = rs.getString("screenshot_url");
        r.src = rs.getString("screenshot_src");
        r.createdAt = rs.getTimestamp("created_at");
        r.comment = rs.getString("comment");
        return r;
    }

    public static ResultSetHandler<List<ScreenshotHistory>> createListResultSetHandler() {
        return new AbstractListHandler<ScreenshotHistory>() {
            @Override
            protected ScreenshotHistory handleRow(ResultSet rs) throws SQLException {
                return ScreenshotHistory.create(rs);
            }
        };
    }

    public static ResultSetHandler<ScreenshotHistory> createDetailResultSetHandler() {
        return new ResultSetHandler<ScreenshotHistory>() {
            @Override
            public ScreenshotHistory handle(ResultSet rs) throws SQLException {
                if (!rs.next()) {
                    return null;
                }
                return ScreenshotHistory.create(rs);
            }
        };
    }

    public static List<ScreenshotHistory> getList(Connection dbconn, final String logContext, final long messageRef)
            throws SQLException {
        String sql =
            "select * from screenshot_history where log_context = ? and message_ref = ? order by created_at asc";
        QueryRunner runner = new QueryRunner();
        return runner.query(dbconn, sql, createListResultSetHandler(), logContext, messageRef);
    }

    public static ScreenshotHistory getDetail(Connection dbconn, final String logContext, final long messageRef,
            final long id) throws SQLException {
        String sql = "select * from screenshot_history where log_context = ? and message_ref = ? and sc_id = ?";
        QueryRunner runner = new QueryRunner();
        return runner.query(dbconn, sql, createDetailResultSetHandler(), logContext, messageRef, id);
    }

    public static int register(
            // @formatter:off
            Connection dbconn,
            final String logContext,
            final long messageRef,
            final long id,
            final String url,
            final String src,
            final Timestamp createdAt
            // @formatter:on
    ) throws SQLException {
        QueryRunner runner = new QueryRunner();
        List<String> columns =
            Arrays.asList("log_context", "message_ref", "sc_id", "screenshot_url", "screenshot_src", "created_at");
        String sql =
            "insert into screenshot_history("
                + String.join(",", columns)
                + ") values ("
                + Strings.repeat("?,", columns.size() - 1)
                + "?)";
        return runner.update(dbconn, sql, logContext, messageRef, id, url, src, createdAt);
    }
}
