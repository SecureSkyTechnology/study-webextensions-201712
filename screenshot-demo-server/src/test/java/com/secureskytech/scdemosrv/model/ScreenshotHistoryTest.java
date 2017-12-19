package com.secureskytech.scdemosrv.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.dbutils.DbUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.secureskytech.scdemosrv.proxy.MyNettyUtil;

import io.netty.handler.codec.http.FullHttpRequest;

public class ScreenshotHistoryTest {

    Connection conn;
    String logContext;

    @Before
    public void beforeMethod() throws SQLException {
        LocalDateTime ldt0 = LocalDateTime.now(Clock.systemDefaultZone());
        DateTimeFormatter dtf0 = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        logContext = ldt0.format(dtf0) + UUID.randomUUID().toString();

        JdbcConnectionFactory cf = new JdbcConnectionFactory("db_" + logContext);
        JdbcConnectionFactory.migrate(cf);
        conn = cf.newConnection();
    }

    @After
    public void afterMethod() {
        DbUtils.closeQuietly(conn);
    }

    public static Calendar createCalendar(int year, int month, int date, int hour, int minute, int second, int mills) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, date, hour, minute, second);
        c.set(Calendar.MILLISECOND, mills);
        return c;
    }

    public static Timestamp createTimestamp(int year, int month, int date, int hour, int minute, int second,
            int mills) {
        return new Timestamp(createCalendar(year, month, date, hour, minute, second, mills).getTimeInMillis());
    }

    @Test
    public void testEmptyDbOperation() throws SQLException {
        String hreq =
            "POST /index.html HTTP/1.1\r\n"
                + "Host: localhost.localdomain\r\n"
                + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Content-Length: 6\r\n"
                + "\r\n"
                + "ABCDEF";
        FullHttpRequest fhr = MyNettyUtil.BytesToFullHttpRequest(hreq.getBytes(StandardCharsets.ISO_8859_1));
        Timestamp atClientToProxy = createTimestamp(2010, 1, 2, 3, 4, 5, 100);
        ProxyHistory.insert(
            conn,
            logContext,
            1L,
            new InetSocketAddress("192.168.1.1", 10080),
            new InetSocketAddress("localhost", 80),
            "http",
            fhr,
            atClientToProxy);
        List<ScreenshotHistory> l0 = ScreenshotHistory.getList(conn, logContext, 1L);
        assertEquals(0, l0.size());
        ScreenshotHistory sch0 = ScreenshotHistory.getDetail(conn, logContext, 1L, 1);
        assertTrue(Objects.isNull(sch0));
    }

    @Test
    public void testTypicalOperation() throws SQLException {
        String get1 = "GET /index1.html HTTP/1.1\r\nHost: localhost\r\n\r\n";
        ProxyHistory.insert(
            conn,
            logContext,
            1L,
            new InetSocketAddress("192.168.1.1", 10080),
            new InetSocketAddress("localhost", 80),
            "http",
            MyNettyUtil.BytesToFullHttpRequest(get1.getBytes(StandardCharsets.UTF_8)),
            createTimestamp(2010, 1, 2, 3, 4, 5, 100));
        String get2 = "GET /index2.html HTTP/1.1\r\nHost: localhost\r\n\r\n";
        ProxyHistory.insert(
            conn,
            logContext,
            2L,
            new InetSocketAddress("192.168.1.1", 10080),
            new InetSocketAddress("localhost", 80),
            "http",
            MyNettyUtil.BytesToFullHttpRequest(get2.getBytes(StandardCharsets.UTF_8)),
            createTimestamp(2010, 1, 2, 3, 14, 15, 200));
        Timestamp ts1 = createTimestamp(2010, 1, 2, 3, 10, 20, 100);
        ScreenshotHistory
            .register(conn, logContext, 1L, 1L, "http://localhost/index1.html", "data:image/png:base64,xxx1", ts1);
        Timestamp ts2 = createTimestamp(2010, 1, 2, 3, 10, 25, 200);
        ScreenshotHistory
            .register(conn, logContext, 1L, 2L, "http://localhost/index1.html", "data:image/png:base64,xxx2", ts2);
        Timestamp ts3 = createTimestamp(2010, 1, 2, 3, 24, 35, 300);
        ScreenshotHistory
            .register(conn, logContext, 2L, 3L, "http://localhost/index2.html", "data:image/png:base64,xxx3", ts3);

        List<ScreenshotHistory> l0 = ScreenshotHistory.getList(conn, logContext, 1L);
        assertEquals(2, l0.size());
        ScreenshotHistory schx = l0.get(0);
        assertEquals(logContext, schx.logContext);
        assertEquals(1L, schx.messageRef);
        assertEquals(1L, schx.id);
        assertEquals("http://localhost/index1.html", schx.url);
        assertEquals("data:image/png:base64,xxx1", schx.src);
        assertEquals(ts1, schx.createdAt);
        assertNull(schx.comment);

        schx = ScreenshotHistory.getDetail(conn, logContext, 1L, 1L);
        assertEquals(logContext, schx.logContext);
        assertEquals(1L, schx.messageRef);
        assertEquals(1L, schx.id);
        assertEquals("http://localhost/index1.html", schx.url);
        assertEquals("data:image/png:base64,xxx1", schx.src);
        assertEquals(ts1, schx.createdAt);
        assertNull(schx.comment);

        schx = l0.get(1);
        assertEquals(logContext, schx.logContext);
        assertEquals(1L, schx.messageRef);
        assertEquals(2L, schx.id);
        assertEquals("http://localhost/index1.html", schx.url);
        assertEquals("data:image/png:base64,xxx2", schx.src);
        assertEquals(ts2, schx.createdAt);
        assertNull(schx.comment);
        schx = ScreenshotHistory.getDetail(conn, logContext, 1L, 2L);
        assertEquals(logContext, schx.logContext);
        assertEquals(1L, schx.messageRef);
        assertEquals(2L, schx.id);
        assertEquals("http://localhost/index1.html", schx.url);
        assertEquals("data:image/png:base64,xxx2", schx.src);
        assertEquals(ts2, schx.createdAt);
        assertNull(schx.comment);

        l0 = ScreenshotHistory.getList(conn, logContext, 2L);
        assertEquals(1, l0.size());
        schx = l0.get(0);
        assertEquals(logContext, schx.logContext);
        assertEquals(2L, schx.messageRef);
        assertEquals(3L, schx.id);
        assertEquals("http://localhost/index2.html", schx.url);
        assertEquals("data:image/png:base64,xxx3", schx.src);
        assertEquals(ts3, schx.createdAt);
        assertNull(schx.comment);
        schx = ScreenshotHistory.getDetail(conn, logContext, 2L, 3L);
        assertEquals(logContext, schx.logContext);
        assertEquals(2L, schx.messageRef);
        assertEquals(3L, schx.id);
        assertEquals("http://localhost/index2.html", schx.url);
        assertEquals("data:image/png:base64,xxx3", schx.src);
        assertEquals(ts3, schx.createdAt);
        assertNull(schx.comment);
    }
}
