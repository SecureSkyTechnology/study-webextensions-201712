package com.secureskytech.scdemosrv.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import io.netty.handler.codec.http.FullHttpResponse;

public class ProxyHistoryTest {

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
        List<ProxyHistory> l0 = ProxyHistory.getList(conn);
        assertEquals(0, l0.size());
        ProxyHistory ph0 = ProxyHistory.getOne(conn, logContext, 1L);
        assertTrue(Objects.isNull(ph0));
        assertEquals(0, ProxyHistory.updateCharset(conn, logContext, 1L, "UTF-8", "UTF-8"));
    }

    @Test
    public void testInsert() throws Exception {
        String hreq =
            "POST /index.html HTTP/1.1\r\n"
                + "Host: localhost.localdomain\r\n"
                + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Content-Length: 6\r\n"
                + "\r\n"
                + "ABCDEF";
        FullHttpRequest fhr = MyNettyUtil.BytesToFullHttpRequest(hreq.getBytes(StandardCharsets.ISO_8859_1));
        Timestamp atClientToProxy = createTimestamp(2010, 1, 2, 3, 4, 5, 678);
        int affectedRows =
            ProxyHistory.insert(
                conn,
                logContext,
                1L,
                new InetSocketAddress("192.168.1.1", 10080),
                new InetSocketAddress("localhost", 80),
                "http",
                fhr,
                atClientToProxy);
        assertEquals(1, affectedRows);
        List<ProxyHistory> l0 = ProxyHistory.getList(conn);
        assertEquals(1, l0.size());
        ProxyHistory ph0 = l0.get(0);

        assertEquals(logContext, ph0.logContext);
        assertEquals(1L, ph0.messageRef);
        assertEquals("192.168.1.1", ph0.clientIp);
        assertEquals(10080, ph0.clientPort);
        assertEquals("localhost", ph0.remoteHostname);
        assertEquals("127.0.0.1", ph0.remoteIp);
        assertEquals(80, ph0.remotePort);

        assertEquals("http://localhost/index.html", ph0.fullUrl);
        assertEquals("http", ph0.requestScheme);
        assertEquals("POST", ph0.requestMethod);
        assertEquals("/index.html", ph0.requestUrl);
        assertEquals("HTTP/1.1", ph0.requestProtocolVersion);
        assertEquals("localhost.localdomain", ph0.requestHostHeader);
        assertNull(ph0.requestBytes);
        assertEquals("", ph0.requestString);
        assertEquals("UTF-8", ph0.requestCharset);

        assertEquals(0, ph0.responseStatusCode);
        assertNull(ph0.responseBytes);
        assertEquals("", ph0.responseString);
        assertEquals("UTF-8", ph0.responseCharset);

        assertEquals(atClientToProxy, ph0.atClientToProxy);
        assertNull(ph0.atProxyToServerConnectStart);
        assertNull(ph0.atProxyToServerConnected);
        assertNull(ph0.atProxyToServerRequestSent);
        assertNull(ph0.atServerToProxyResponseReceived);
        assertEquals(0, ph0.elapsedTimeConnect);
        assertEquals(0, ph0.elapsedTimeRequestSent);
        assertEquals(0, ph0.elapsedTimeResponseReceived);
        assertEquals(0, ph0.elapsedTimeFull);

        assertNull(ph0.comment);
        assertEquals(0, ph0.countOfAttachedScreenshot);

        ProxyHistory ph1 = ProxyHistory.getOne(conn, logContext, 1L);

        assertEquals(ph0.logContext, ph1.logContext);
        assertEquals(ph0.messageRef, ph1.messageRef);
        assertEquals(ph0.clientIp, ph1.clientIp);
        assertEquals(ph0.clientPort, ph1.clientPort);
        assertEquals(ph0.remoteHostname, ph1.remoteHostname);
        assertEquals(ph0.remoteIp, ph1.remoteIp);
        assertEquals(ph0.remotePort, ph1.remotePort);

        assertEquals(ph0.fullUrl, ph1.fullUrl);
        assertEquals(ph0.requestScheme, ph1.requestScheme);
        assertEquals(ph0.requestMethod, ph1.requestMethod);
        assertEquals(ph0.requestUrl, ph1.requestUrl);
        assertEquals(ph0.requestProtocolVersion, ph1.requestProtocolVersion);
        assertEquals(ph0.requestHostHeader, ph1.requestHostHeader);
        assertArrayEquals(hreq.getBytes(StandardCharsets.ISO_8859_1), ph1.requestBytes);
        assertEquals(hreq, ph1.requestString);
        assertEquals(ph0.requestCharset, ph1.requestCharset);

        assertEquals(ph0.responseStatusCode, ph1.responseStatusCode);
        assertNull(ph1.responseBytes);
        assertEquals(ph0.responseString, ph1.responseString);
        assertEquals(ph0.responseCharset, ph1.responseCharset);

        assertEquals(ph0.atClientToProxy, ph1.atClientToProxy);
        assertNull(ph1.atProxyToServerConnectStart);
        assertNull(ph1.atProxyToServerConnected);
        assertNull(ph1.atProxyToServerRequestSent);
        assertNull(ph1.atServerToProxyResponseReceived);
        assertEquals(0, ph1.elapsedTimeConnect);
        assertEquals(0, ph1.elapsedTimeRequestSent);
        assertEquals(0, ph1.elapsedTimeResponseReceived);
        assertEquals(0, ph1.elapsedTimeFull);

        assertNull(ph1.comment);
        assertEquals(0, ph1.countOfAttachedScreenshot);
    }

    @Test
    public void testFullUrlColumn() throws Exception {
        String hreq = "GET /index.html HTTP/1.1\r\nHost: localhost.localdomain\r\n\r\n";
        int affectedRows =
            ProxyHistory.insert(
                conn,
                logContext,
                1L,
                new InetSocketAddress("192.168.1.1", 10080),
                new InetSocketAddress("localhost", 80),
                "http",
                MyNettyUtil.BytesToFullHttpRequest(hreq.getBytes(StandardCharsets.ISO_8859_1)),
                createTimestamp(2010, 1, 2, 3, 4, 5, 100));
        assertEquals(1, affectedRows);
        List<ProxyHistory> l0 = ProxyHistory.getList(conn);
        assertEquals(1, l0.size());
        ProxyHistory ph0 = l0.get(0);
        assertEquals("http://localhost/index.html", ph0.fullUrl);
        ProxyHistory ph1 = ProxyHistory.getOne(conn, logContext, 1L);
        assertEquals(ph0.fullUrl, ph1.fullUrl);

        affectedRows =
            ProxyHistory.insert(
                conn,
                logContext,
                2L,
                new InetSocketAddress("192.168.1.1", 10080),
                new InetSocketAddress("localhost", 443),
                "https",
                MyNettyUtil.BytesToFullHttpRequest(hreq.getBytes(StandardCharsets.ISO_8859_1)),
                createTimestamp(2010, 1, 2, 3, 4, 5, 200));
        assertEquals(1, affectedRows);
        l0 = ProxyHistory.getList(conn);
        assertEquals(2, l0.size());
        ph0 = l0.get(0);
        assertEquals("https://localhost/index.html", ph0.fullUrl);
        ph1 = ProxyHistory.getOne(conn, logContext, 2L);
        assertEquals(ph0.fullUrl, ph1.fullUrl);

        affectedRows =
            ProxyHistory.insert(
                conn,
                logContext,
                3L,
                new InetSocketAddress("192.168.1.1", 10080),
                new InetSocketAddress("localhost", 443),
                "http",
                MyNettyUtil.BytesToFullHttpRequest(hreq.getBytes(StandardCharsets.ISO_8859_1)),
                createTimestamp(2010, 1, 2, 3, 4, 5, 300));
        assertEquals(1, affectedRows);
        l0 = ProxyHistory.getList(conn);
        assertEquals(3, l0.size());
        ph0 = l0.get(0);
        assertEquals("http://localhost:443/index.html", ph0.fullUrl);
        ph1 = ProxyHistory.getOne(conn, logContext, 3L);
        assertEquals(ph0.fullUrl, ph1.fullUrl);

        affectedRows =
            ProxyHistory.insert(
                conn,
                logContext,
                4L,
                new InetSocketAddress("192.168.1.1", 10080),
                new InetSocketAddress("localhost", 80),
                "https",
                MyNettyUtil.BytesToFullHttpRequest(hreq.getBytes(StandardCharsets.ISO_8859_1)),
                createTimestamp(2010, 1, 2, 3, 4, 5, 400));
        assertEquals(1, affectedRows);
        l0 = ProxyHistory.getList(conn);
        assertEquals(4, l0.size());
        ph0 = l0.get(0);
        assertEquals("https://localhost:80/index.html", ph0.fullUrl);
        ph1 = ProxyHistory.getOne(conn, logContext, 4L);
        assertEquals(ph0.fullUrl, ph1.fullUrl);
    }

    @Test
    public void testInsertThenUpdate() throws Exception {
        String hreq =
            "POST /index.html HTTP/1.1\r\n"
                + "Host: localhost.localdomain\r\n"
                + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Content-Length: 6\r\n"
                + "\r\n"
                + "ABCDEF";
        FullHttpRequest fhreq = MyNettyUtil.BytesToFullHttpRequest(hreq.getBytes(StandardCharsets.ISO_8859_1));
        Timestamp atClientToProxy = createTimestamp(2010, 1, 2, 3, 4, 5, 100);
        int affectedRows =
            ProxyHistory.insert(
                conn,
                logContext,
                1L,
                new InetSocketAddress("192.168.1.1", 10080),
                new InetSocketAddress("localhost", 80),
                "http",
                fhreq,
                atClientToProxy);
        assertEquals(1, affectedRows);
        // @formatter:off
        String hres = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/plain\r\n"
                + "Content-Length: 15\r\n"
                + "\r\n"
                + "こんにちは";
        // @formatter:on
        FullHttpResponse fhres = MyNettyUtil.BytesToFullHttpResponse(hres.getBytes(StandardCharsets.UTF_8));
        Timestamp atProxyToServerConnectStart = createTimestamp(2010, 1, 2, 3, 4, 5, 200);
        Timestamp atProxyToServerConnected = createTimestamp(2010, 1, 2, 3, 4, 5, 310);
        Timestamp atProxyToServerRequestSent = createTimestamp(2010, 1, 2, 3, 4, 5, 430);
        Timestamp atServerToProxyResponseReceived = createTimestamp(2010, 1, 2, 3, 4, 5, 560);
        affectedRows =
            ProxyHistory.update(
                conn,
                logContext,
                999999L,
                fhres,
                atClientToProxy,
                atProxyToServerConnectStart,
                atProxyToServerConnected,
                atProxyToServerRequestSent,
                atServerToProxyResponseReceived);
        assertEquals(0, affectedRows);
        // avoid refcount decrement error (refCnt = 0)
        fhres = MyNettyUtil.BytesToFullHttpResponse(hres.getBytes(StandardCharsets.UTF_8));
        affectedRows =
            ProxyHistory.update(
                conn,
                logContext,
                1L,
                fhres,
                atClientToProxy,
                atProxyToServerConnectStart,
                atProxyToServerConnected,
                atProxyToServerRequestSent,
                atServerToProxyResponseReceived);
        assertEquals(1, affectedRows);

        List<ProxyHistory> l0 = ProxyHistory.getList(conn);
        assertEquals(1, l0.size());
        ProxyHistory ph0 = l0.get(0);

        assertEquals(logContext, ph0.logContext);
        assertEquals(1L, ph0.messageRef);
        assertEquals("192.168.1.1", ph0.clientIp);
        assertEquals(10080, ph0.clientPort);
        assertEquals("localhost", ph0.remoteHostname);
        assertEquals("127.0.0.1", ph0.remoteIp);
        assertEquals(80, ph0.remotePort);

        assertEquals("http://localhost/index.html", ph0.fullUrl);
        assertEquals("http", ph0.requestScheme);
        assertEquals("POST", ph0.requestMethod);
        assertEquals("/index.html", ph0.requestUrl);
        assertEquals("HTTP/1.1", ph0.requestProtocolVersion);
        assertEquals("localhost.localdomain", ph0.requestHostHeader);
        assertNull(ph0.requestBytes);
        assertEquals("", ph0.requestString);
        assertEquals("UTF-8", ph0.requestCharset);

        assertEquals(200, ph0.responseStatusCode);
        assertNull(ph0.responseBytes);
        assertEquals("", ph0.responseString);
        assertEquals("UTF-8", ph0.responseCharset);

        assertEquals(atClientToProxy, ph0.atClientToProxy);
        assertEquals(atProxyToServerConnectStart, ph0.atProxyToServerConnectStart);
        assertEquals(atProxyToServerConnected, ph0.atProxyToServerConnected);
        assertEquals(atProxyToServerRequestSent, ph0.atProxyToServerRequestSent);
        assertEquals(atServerToProxyResponseReceived, ph0.atServerToProxyResponseReceived);
        assertEquals(110, ph0.elapsedTimeConnect);
        assertEquals(120, ph0.elapsedTimeRequestSent);
        assertEquals(130, ph0.elapsedTimeResponseReceived);
        assertEquals(460, ph0.elapsedTimeFull);

        assertNull(ph0.comment);
        assertEquals(0, ph0.countOfAttachedScreenshot);

        ProxyHistory ph1 = ProxyHistory.getOne(conn, logContext, 1L);

        assertEquals(ph0.logContext, ph1.logContext);
        assertEquals(ph0.messageRef, ph1.messageRef);
        assertEquals(ph0.clientIp, ph1.clientIp);
        assertEquals(ph0.clientPort, ph1.clientPort);
        assertEquals(ph0.remoteHostname, ph1.remoteHostname);
        assertEquals(ph0.remoteIp, ph1.remoteIp);
        assertEquals(ph0.remotePort, ph1.remotePort);

        assertEquals(ph0.fullUrl, ph1.fullUrl);
        assertEquals(ph0.requestScheme, ph1.requestScheme);
        assertEquals(ph0.requestMethod, ph1.requestMethod);
        assertEquals(ph0.requestUrl, ph1.requestUrl);
        assertEquals(ph0.requestProtocolVersion, ph1.requestProtocolVersion);
        assertEquals(ph0.requestHostHeader, ph1.requestHostHeader);
        assertArrayEquals(hreq.getBytes(StandardCharsets.ISO_8859_1), ph1.requestBytes);
        assertEquals(hreq, ph1.requestString);
        assertEquals(ph0.requestCharset, ph1.requestCharset);

        assertEquals(ph0.responseStatusCode, ph1.responseStatusCode);
        assertArrayEquals(hres.getBytes(StandardCharsets.UTF_8), ph1.responseBytes);
        assertEquals(hres, ph1.responseString);
        assertEquals(ph0.responseCharset, ph1.responseCharset);

        assertEquals(atClientToProxy, ph0.atClientToProxy);
        assertEquals(atProxyToServerConnectStart, ph0.atProxyToServerConnectStart);
        assertEquals(atProxyToServerConnected, ph0.atProxyToServerConnected);
        assertEquals(atProxyToServerRequestSent, ph0.atProxyToServerRequestSent);
        assertEquals(atServerToProxyResponseReceived, ph0.atServerToProxyResponseReceived);
        assertEquals(110, ph0.elapsedTimeConnect);
        assertEquals(120, ph0.elapsedTimeRequestSent);
        assertEquals(130, ph0.elapsedTimeResponseReceived);
        assertEquals(460, ph0.elapsedTimeFull);

        assertNull(ph1.comment);
        assertEquals(0, ph1.countOfAttachedScreenshot);

        affectedRows = ProxyHistory.updateCharset(conn, logContext, 0L, "Shift_JIS", "EUC-JP");
        assertEquals(0, affectedRows);
        affectedRows = ProxyHistory.updateCharset(conn, logContext, 1L, "Shift_JIS", "EUC-JP");
        assertEquals(1, affectedRows);

        l0 = ProxyHistory.getList(conn);
        assertEquals(1, l0.size());
        ph0 = l0.get(0);
        assertEquals("Shift_JIS", ph0.requestCharset);
        assertEquals("EUC-JP", ph0.responseCharset);

        ph1 = ProxyHistory.getOne(conn, logContext, 1L);
        assertEquals(ph0.requestCharset, ph1.requestCharset);
        assertEquals(ph0.responseCharset, ph1.responseCharset);
    }

    @Test
    public void testScreenshotAttachTypicalOperation() throws SQLException {
        String get1old = "GET /index1.html HTTP/1.1\r\nHost: localhost\r\n\r\n";
        ProxyHistory.insert(
            conn,
            logContext,
            1L,
            new InetSocketAddress("192.168.1.1", 10080),
            new InetSocketAddress("localhost", 80),
            "http",
            MyNettyUtil.BytesToFullHttpRequest(get1old.getBytes(StandardCharsets.UTF_8)),
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
        String get1new = "GET /index1.html HTTP/1.1\r\nHost: localhost\r\n\r\n";
        ProxyHistory.insert(
            conn,
            logContext,
            3L,
            new InetSocketAddress("192.168.1.1", 10080),
            new InetSocketAddress("localhost", 80),
            "http",
            MyNettyUtil.BytesToFullHttpRequest(get1new.getBytes(StandardCharsets.UTF_8)),
            createTimestamp(2010, 1, 2, 3, 24, 25, 300));

        Timestamp ts1 = createTimestamp(2010, 1, 2, 3, 25, 26, 100);
        long foundmref =
            ProxyHistory.attachScreenshot(
                conn,
                logContext,
                1L,
                "http://localhost/index1.html",
                "data:image/png:base64,xxx1",
                ts1);
        assertEquals(3L, foundmref);
        Timestamp ts2 = createTimestamp(2010, 1, 2, 3, 25, 27, 200);
        foundmref =
            ProxyHistory.attachScreenshot(
                conn,
                logContext,
                2L,
                "http://localhost/index1.html",
                "data:image/png:base64,xxx2",
                ts2);
        assertEquals(3L, foundmref);
        Timestamp ts3 = createTimestamp(2010, 1, 2, 3, 25, 28, 300);
        foundmref =
            ProxyHistory.attachScreenshot(
                conn,
                logContext,
                3L,
                "http://localhost/index2.html",
                "data:image/png:base64,xxx3",
                ts3);
        assertEquals(2L, foundmref);

        List<ProxyHistory> phl0 = ProxyHistory.getList(conn);
        assertEquals(3, phl0.size());
        assertEquals(3L, phl0.get(0).messageRef);
        assertEquals(2, phl0.get(0).countOfAttachedScreenshot);
        assertEquals(2L, phl0.get(1).messageRef);
        assertEquals(1, phl0.get(1).countOfAttachedScreenshot);
        assertEquals(1L, phl0.get(2).messageRef);
        assertEquals(0, phl0.get(2).countOfAttachedScreenshot);

        ProxyHistory ph0 = ProxyHistory.getOne(conn, logContext, 1L);
        assertEquals(0, ph0.countOfAttachedScreenshot);
        ph0 = ProxyHistory.getOne(conn, logContext, 2L);
        assertEquals(1, ph0.countOfAttachedScreenshot);
        ph0 = ProxyHistory.getOne(conn, logContext, 3L);
        assertEquals(2, ph0.countOfAttachedScreenshot);

        List<ScreenshotHistory> l0 = ScreenshotHistory.getList(conn, logContext, 1L);
        assertEquals(0, l0.size());

        l0 = ScreenshotHistory.getList(conn, logContext, 3L);
        assertEquals(2, l0.size());
        ScreenshotHistory schx = l0.get(0);
        assertEquals(logContext, schx.logContext);
        assertEquals(3L, schx.messageRef);
        assertEquals(1L, schx.id);
        assertEquals("http://localhost/index1.html", schx.url);
        assertEquals("data:image/png:base64,xxx1", schx.src);
        assertEquals(ts1, schx.createdAt);
        assertNull(schx.comment);

        schx = ScreenshotHistory.getDetail(conn, logContext, 3L, 1L);
        assertEquals(logContext, schx.logContext);
        assertEquals(3L, schx.messageRef);
        assertEquals(1L, schx.id);
        assertEquals("http://localhost/index1.html", schx.url);
        assertEquals("data:image/png:base64,xxx1", schx.src);
        assertEquals(ts1, schx.createdAt);
        assertNull(schx.comment);

        schx = l0.get(1);
        assertEquals(logContext, schx.logContext);
        assertEquals(3L, schx.messageRef);
        assertEquals(2L, schx.id);
        assertEquals("http://localhost/index1.html", schx.url);
        assertEquals("data:image/png:base64,xxx2", schx.src);
        assertEquals(ts2, schx.createdAt);
        assertNull(schx.comment);
        schx = ScreenshotHistory.getDetail(conn, logContext, 3L, 2L);
        assertEquals(logContext, schx.logContext);
        assertEquals(3L, schx.messageRef);
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

    @Test
    public void testScreenshotAttachMatchingUrlNotFound() throws SQLException {
        try {
            ProxyHistory.attachScreenshot(
                conn,
                logContext,
                1L,
                "http://localhost/index1.html",
                "data:image/png:base64,xxx1",
                createTimestamp(2010, 1, 2, 3, 10, 20, 100));
            fail("must not reach here.");
        } catch (IllegalArgumentException e) {
            assertEquals("no proxy history item matches to url[http://localhost/index1.html]", e.getMessage());
        }
    }
}
