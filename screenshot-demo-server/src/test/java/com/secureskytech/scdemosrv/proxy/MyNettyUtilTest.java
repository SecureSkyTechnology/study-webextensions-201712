package com.secureskytech.scdemosrv.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Test;

import com.google.common.base.Strings;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

public class MyNettyUtilTest {

    @Test
    public void testBytesToReq_simpleGET() {
        String hreq = "GET /index.html HTTP/1.1\r\nHost: localhost\r\n\r\n";
        FullHttpRequest fhr = MyNettyUtil.BytesToFullHttpRequest(hreq.getBytes(StandardCharsets.ISO_8859_1));
        assertEquals(HttpMethod.GET, fhr.getMethod());
        assertEquals("/index.html", fhr.getUri());
        assertEquals(HttpVersion.HTTP_1_1, fhr.getProtocolVersion());
        assertEquals("localhost", HttpHeaders.getHost(fhr));
        assertEquals(0, HttpHeaders.getContentLength(fhr));
        assertEquals(2, fhr.headers().entries().size());
        assertEquals(0, fhr.content().readableBytes());
        assertTrue(fhr.release());
    }

    @Test
    public void testBytesToReq_simplePOST() {
        String hreq =
            "POST /index.html HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Content-Length: 6\r\n"
                + "\r\n"
                + "ABCDEF";
        FullHttpRequest fhr = MyNettyUtil.BytesToFullHttpRequest(hreq.getBytes(StandardCharsets.ISO_8859_1));
        assertEquals(HttpMethod.POST, fhr.getMethod());
        assertEquals("/index.html", fhr.getUri());
        assertEquals(HttpVersion.HTTP_1_1, fhr.getProtocolVersion());
        assertEquals("localhost", HttpHeaders.getHost(fhr));
        assertEquals("application/x-www-form-urlencoded", HttpHeaders.getHeader(fhr, "Content-Type"));
        assertEquals(6, HttpHeaders.getContentLength(fhr));
        assertEquals(3, fhr.headers().entries().size());
        assertEquals(6, fhr.content().readableBytes());
        ByteBuf buf = fhr.content();
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        assertEquals("ABCDEF", new String(bytes, StandardCharsets.ISO_8859_1));
        assertTrue(fhr.release());
    }

    @Test
    public void testBytesToReq_largePOST() {
        final int bodylen = 5_000_000;
        final String reqbody = Strings.repeat("1234567890", (bodylen / 10));
        final String longhv = Strings.repeat("1234567890", 10 * 1024);
        String hreq =
            // @formatter:off
            "POST /index.html HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "X-Custom: " + longhv + "\r\n"
                + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "X-Custom: " + longhv + "\r\n"
                + "Content-Length: " + bodylen + "\r\n"
                + "\r\n"
                + reqbody;
            // @formatter:on
        FullHttpRequest fhr = MyNettyUtil.BytesToFullHttpRequest(hreq.getBytes(StandardCharsets.ISO_8859_1));
        assertEquals(HttpMethod.POST, fhr.getMethod());
        assertEquals("/index.html", fhr.getUri());
        assertEquals(HttpVersion.HTTP_1_1, fhr.getProtocolVersion());
        assertEquals("localhost", HttpHeaders.getHost(fhr));
        assertEquals("application/x-www-form-urlencoded", HttpHeaders.getHeader(fhr, "Content-Type"));
        assertEquals(bodylen, HttpHeaders.getContentLength(fhr));
        List<Entry<String, String>> headerEntries = fhr.headers().entries();
        assertEquals(5, headerEntries.size());
        String[] expectedKeys = { "Host", "X-Custom", "Content-Type", "X-Custom", "Content-Length" };
        String[] expectedValues =
            { "localhost", longhv, "application/x-www-form-urlencoded", longhv, Integer.toString(bodylen) };
        for (int i = 0; i < headerEntries.size(); i++) {
            Entry<String, String> e = headerEntries.get(i);
            assertEquals(expectedKeys[i], e.getKey());
            assertEquals(expectedValues[i], e.getValue());
        }
        assertEquals(bodylen, fhr.content().readableBytes());
        ByteBuf buf = fhr.content();
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        assertEquals(reqbody, new String(bytes, StandardCharsets.ISO_8859_1));
        assertTrue(fhr.release());
    }

    @Test
    public void testBytesToReq_oversizedPOST() {
        String hreq =
            "POST /index.html HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Content-Length: 6\r\n"
                + "\r\n"
                + "ABCDEF\r\nGHIJKLMN";
        FullHttpRequest fhr = MyNettyUtil.BytesToFullHttpRequest(hreq.getBytes(StandardCharsets.ISO_8859_1));
        assertEquals(HttpMethod.POST, fhr.getMethod());
        assertEquals("/index.html", fhr.getUri());
        assertEquals(HttpVersion.HTTP_1_1, fhr.getProtocolVersion());
        assertEquals("localhost", HttpHeaders.getHost(fhr));
        assertEquals("application/x-www-form-urlencoded", HttpHeaders.getHeader(fhr, "Content-Type"));
        assertEquals(6, HttpHeaders.getContentLength(fhr));
        assertEquals(3, fhr.headers().entries().size());
        assertEquals(6, fhr.content().readableBytes());
        ByteBuf buf = fhr.content();
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        assertEquals("ABCDEF", new String(bytes, StandardCharsets.ISO_8859_1));
        assertTrue(fhr.release());
    }

    @Test
    public void testBytesToReq_complexPOST() {
        String hreq =
            "POST https://user:password@www.example.com:8443 HTTP/1.0\r\n"
                + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Content-Length: 6\r\n"
                + "Host: localhost\r\n"
                + "Cookie: abc=def\r\n"
                + "Cookie: AAA=BBB\r\n"
                + "X-Custom1: abcdefg\r\n"
                + "           123456789\r\n"
                + "X-Custom2: ABCDEFG    \r\n"
                + " 987654321    \r\n"
                + "\r\n"
                + "ABCDEF";
        FullHttpRequest fhr = MyNettyUtil.BytesToFullHttpRequest(hreq.getBytes(StandardCharsets.ISO_8859_1));
        assertEquals(HttpMethod.POST, fhr.getMethod());
        assertEquals("https://user:password@www.example.com:8443", fhr.getUri());
        assertEquals(HttpVersion.HTTP_1_0, fhr.getProtocolVersion());
        assertEquals("localhost", HttpHeaders.getHost(fhr));
        assertEquals("application/x-www-form-urlencoded", HttpHeaders.getHeader(fhr, "Content-Type"));
        assertEquals(6, HttpHeaders.getContentLength(fhr));
        List<Entry<String, String>> headerEntries = fhr.headers().entries();
        assertEquals(7, headerEntries.size());
        String[] expectedKeys =
            { "Content-Type", "Content-Length", "Host", "Cookie", "Cookie", "X-Custom1", "X-Custom2" };
        String[] expectedValues = {
            "application/x-www-form-urlencoded",
            "6",
            "localhost",
            "abc=def",
            "AAA=BBB",
            "abcdefg 123456789",
            "ABCDEFG 987654321" };
        for (int i = 0; i < headerEntries.size(); i++) {
            Entry<String, String> e = headerEntries.get(i);
            assertEquals(expectedKeys[i], e.getKey());
            assertEquals(expectedValues[i], e.getValue());
        }
        assertEquals(6, fhr.content().readableBytes());
        ByteBuf buf = fhr.content();
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        assertEquals("ABCDEF", new String(bytes, StandardCharsets.ISO_8859_1));
        assertTrue(fhr.release());
    }

    @Test
    public void testReqToBytes_simpleGET() {
        String hreq = "GET /index.html HTTP/1.1\r\nHost: localhost\r\n\r\n";
        FullHttpRequest fhr = MyNettyUtil.BytesToFullHttpRequest(hreq.getBytes(StandardCharsets.ISO_8859_1));
        byte[] r = MyNettyUtil.FullHttpRequestToBytes(fhr);
        String expected = "GET /index.html HTTP/1.1\r\nHost: localhost\r\nContent-Length: 0\r\n\r\n";
        assertEquals(expected, new String(r, StandardCharsets.ISO_8859_1));
        assertEquals(0, fhr.refCnt());
    }

    @Test
    public void testReqToBytes_simplePOST() {
        String hreq =
            "POST /index.html HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Content-Length: 6\r\n"
                + "\r\n"
                + "ABCDEF";
        FullHttpRequest fhr = MyNettyUtil.BytesToFullHttpRequest(hreq.getBytes(StandardCharsets.ISO_8859_1));
        byte[] r = MyNettyUtil.FullHttpRequestToBytes(fhr);
        assertEquals(hreq, new String(r, StandardCharsets.ISO_8859_1));
        assertEquals(0, fhr.refCnt());
    }

    @Test
    public void testReqToBytes_largePOST() {
        final int bodylen = 5_000_000;
        final String reqbody = Strings.repeat("1234567890", (bodylen / 10));
        final String longhv = Strings.repeat("1234567890", 10 * 1024);
        String hreq =
            // @formatter:off
            "POST /index.html HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "X-Custom: " + longhv + "\r\n"
                + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "X-Custom: " + longhv + "\r\n"
                + "Content-Length: " + bodylen + "\r\n"
                + "\r\n"
                + reqbody;
            // @formatter:on
        FullHttpRequest fhr = MyNettyUtil.BytesToFullHttpRequest(hreq.getBytes(StandardCharsets.ISO_8859_1));
        byte[] r = MyNettyUtil.FullHttpRequestToBytes(fhr);
        assertEquals(hreq, new String(r, StandardCharsets.ISO_8859_1));
        assertEquals(0, fhr.refCnt());
    }

    @Test
    public void testReqToBytes_complexPOST() {
        String hreq =
            "POST https://user:password@www.example.com:8443 HTTP/1.0\r\n"
                + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Content-Length: 6\r\n"
                + "Host: localhost\r\n"
                + "Cookie: abc=def\r\n"
                + "Cookie: AAA=BBB\r\n"
                + "X-Custom1: abcdefg\r\n"
                + "           123456789\r\n"
                + "X-Custom2: ABCDEFG    \r\n"
                + " 987654321    \r\n"
                + "\r\n"
                + "ABCDEF";
        FullHttpRequest fhr = MyNettyUtil.BytesToFullHttpRequest(hreq.getBytes(StandardCharsets.ISO_8859_1));
        byte[] r = MyNettyUtil.FullHttpRequestToBytes(fhr);
        String expected =
            "POST https://user:password@www.example.com:8443 HTTP/1.0\r\n"
                + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Content-Length: 6\r\n"
                + "Host: localhost\r\n"
                + "Cookie: abc=def\r\n"
                + "Cookie: AAA=BBB\r\n"
                + "X-Custom1: abcdefg 123456789\r\n"
                + "X-Custom2: ABCDEFG 987654321\r\n"
                + "\r\n"
                + "ABCDEF";
        assertEquals(expected, new String(r, StandardCharsets.ISO_8859_1));
        assertEquals(0, fhr.refCnt());
    }

    @Test
    public void testBytesToRes_nobody() {
        // @formatter:off
        String hres = "HTTP/1.1 302 Found\r\n"
                + "Location: /index.html\r\n"
                + "Content-Length: 0\r\n"
                + "\r\n";
        // @formatter:on
        FullHttpResponse fhr = MyNettyUtil.BytesToFullHttpResponse(hres.getBytes(StandardCharsets.ISO_8859_1));
        assertEquals(HttpVersion.HTTP_1_1, fhr.getProtocolVersion());
        assertEquals(302, fhr.getStatus().code());
        assertEquals("Found", fhr.getStatus().reasonPhrase());
        assertEquals("/index.html", HttpHeaders.getHeader(fhr, "Location"));
        assertEquals(0, HttpHeaders.getContentLength(fhr));
        assertEquals(2, fhr.headers().entries().size());
        assertEquals(0, fhr.content().readableBytes());
        assertTrue(fhr.release());
    }

    @Test
    public void testBytesToRes_withbody() {
        // @formatter:off
        String hres = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/plain\r\n"
                + "Content-Length: 15\r\n"
                + "\r\n"
                + "こんにちは";
        // @formatter:on
        FullHttpResponse fhr = MyNettyUtil.BytesToFullHttpResponse(hres.getBytes(StandardCharsets.UTF_8));
        assertEquals(HttpVersion.HTTP_1_1, fhr.getProtocolVersion());
        assertEquals(200, fhr.getStatus().code());
        assertEquals("OK", fhr.getStatus().reasonPhrase());
        assertEquals("text/plain", HttpHeaders.getHeader(fhr, "Content-Type"));
        assertEquals(15, HttpHeaders.getContentLength(fhr));
        assertEquals(2, fhr.headers().entries().size());
        assertEquals(15, fhr.content().readableBytes());
        ByteBuf buf = fhr.content();
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        assertEquals("こんにちは", new String(bytes, StandardCharsets.UTF_8));
        assertTrue(fhr.release());
    }

    @Test
    public void testBytesToRes_withlongbody() {
        final int bodylen = 5_000_000;
        final String reqbody = Strings.repeat("1234567890", (bodylen / 10));
        final String longsc = Strings.repeat("1234567890", 10 * 1024);
        // @formatter:off
        String hres = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/plain\r\n"
                + "Set-Cookie: abc=" + longsc + "\r\n"
                + "Content-Length: " + bodylen + "\r\n"
                + "Set-Cookie: abc=" + longsc + "\r\n"
                + "\r\n"
                + reqbody;
        // @formatter:on
        FullHttpResponse fhr = MyNettyUtil.BytesToFullHttpResponse(hres.getBytes(StandardCharsets.UTF_8));
        assertEquals(HttpVersion.HTTP_1_1, fhr.getProtocolVersion());
        assertEquals(200, fhr.getStatus().code());
        assertEquals("OK", fhr.getStatus().reasonPhrase());
        assertEquals("text/plain", HttpHeaders.getHeader(fhr, "Content-Type"));
        assertEquals(bodylen, HttpHeaders.getContentLength(fhr));
        List<Entry<String, String>> headerEntries = fhr.headers().entries();
        assertEquals(4, headerEntries.size());
        String[] expectedKeys = { "Content-Type", "Set-Cookie", "Content-Length", "Set-Cookie" };
        String[] expectedValues = { "text/plain", "abc=" + longsc, Integer.toString(bodylen), "abc=" + longsc };
        for (int i = 0; i < headerEntries.size(); i++) {
            Entry<String, String> e = headerEntries.get(i);
            assertEquals(expectedKeys[i], e.getKey());
            assertEquals(expectedValues[i], e.getValue());
        }
        assertEquals(bodylen, fhr.content().readableBytes());
        ByteBuf buf = fhr.content();
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        assertEquals(reqbody, new String(bytes, StandardCharsets.UTF_8));
        assertTrue(fhr.release());
    }

    @Test
    public void testBytesToRes_complex_oversizedbody() {
        // @formatter:off
        String hres = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/plain\r\n"
                + "Set-Cookie: abc=123\r\n"
                + "Set-Cookie: abc=456\r\n"
                + "X-Custom1: abcdefg\r\n"
                + "           123456789\r\n"
                + "X-Custom2: ABCDEFG    \r\n"
                + " 987654321    \r\n"
                + "Content-Length: 15\r\n"
                + "\r\n"
                + "こんにちは\r\nABCDEFG";
        // @formatter:on
        FullHttpResponse fhr = MyNettyUtil.BytesToFullHttpResponse(hres.getBytes(StandardCharsets.UTF_8));
        assertEquals(HttpVersion.HTTP_1_1, fhr.getProtocolVersion());
        assertEquals(200, fhr.getStatus().code());
        assertEquals("OK", fhr.getStatus().reasonPhrase());
        assertEquals("text/plain", HttpHeaders.getHeader(fhr, "Content-Type"));
        assertEquals(15, HttpHeaders.getContentLength(fhr));
        List<Entry<String, String>> headerEntries = fhr.headers().entries();
        assertEquals(6, headerEntries.size());
        String[] expectedKeys =
            { "Content-Type", "Set-Cookie", "Set-Cookie", "X-Custom1", "X-Custom2", "Content-Length" };
        String[] expectedValues =
            { "text/plain", "abc=123", "abc=456", "abcdefg 123456789", "ABCDEFG 987654321", "15", };
        for (int i = 0; i < headerEntries.size(); i++) {
            Entry<String, String> e = headerEntries.get(i);
            assertEquals(expectedKeys[i], e.getKey());
            assertEquals(expectedValues[i], e.getValue());
        }
        assertEquals(15, fhr.content().readableBytes());
        ByteBuf buf = fhr.content();
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        assertEquals("こんにちは", new String(bytes, StandardCharsets.UTF_8));
        assertTrue(fhr.release());
    }

    @Test
    public void testResToBytes_nobody() {
        // @formatter:off
        String hres = "HTTP/1.1 302 Found\r\n"
                + "Location: /index.html\r\n"
                + "Content-Length: 0\r\n"
                + "\r\n";
        // @formatter:on
        FullHttpResponse fhr = MyNettyUtil.BytesToFullHttpResponse(hres.getBytes(StandardCharsets.ISO_8859_1));
        byte[] r = MyNettyUtil.FullHttpResponseToBytes(fhr);
        String expected = hres;
        assertEquals(expected, new String(r, StandardCharsets.ISO_8859_1));
        assertEquals(0, fhr.refCnt());
    }

    @Test
    public void testResToBytes_withbody() {
        // @formatter:off
        String hres = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/plain\r\n"
                + "Content-Length: 15\r\n"
                + "\r\n"
                + "こんにちは";
        // @formatter:on
        FullHttpResponse fhr = MyNettyUtil.BytesToFullHttpResponse(hres.getBytes(StandardCharsets.UTF_8));
        byte[] r = MyNettyUtil.FullHttpResponseToBytes(fhr);
        String expected = hres;
        assertEquals(expected, new String(r, StandardCharsets.UTF_8));
        assertEquals(0, fhr.refCnt());
    }

    @Test
    public void testResToBytes_withlongbody() {
        final int bodylen = 5_000_000;
        final String reqbody = Strings.repeat("1234567890", (bodylen / 10));
        // @formatter:off
        String hres = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/plain\r\n"
                + "Content-Length: " + bodylen + "\r\n"
                + "\r\n"
                + reqbody;
        // @formatter:on
        FullHttpResponse fhr = MyNettyUtil.BytesToFullHttpResponse(hres.getBytes(StandardCharsets.UTF_8));
        byte[] r = MyNettyUtil.FullHttpResponseToBytes(fhr);
        String expected = hres;
        assertEquals(expected, new String(r, StandardCharsets.UTF_8));
        assertEquals(0, fhr.refCnt());
    }

    @Test
    public void testResToBytes_complex_oversizedbody() {
        // @formatter:off
        String hres = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/plain\r\n"
                + "Set-Cookie: abc=123\r\n"
                + "Set-Cookie: abc=456\r\n"
                + "X-Custom1: abcdefg\r\n"
                + "           123456789\r\n"
                + "X-Custom2: ABCDEFG    \r\n"
                + " 987654321    \r\n"
                + "Content-Length: 15\r\n"
                + "\r\n"
                + "こんにちは\r\nABCDEFG";
        // @formatter:on
        FullHttpResponse fhr = MyNettyUtil.BytesToFullHttpResponse(hres.getBytes(StandardCharsets.UTF_8));
        byte[] r = MyNettyUtil.FullHttpResponseToBytes(fhr);
        // @formatter:off
        String expected = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/plain\r\n"
                + "Set-Cookie: abc=123\r\n"
                + "Set-Cookie: abc=456\r\n"
                + "X-Custom1: abcdefg 123456789\r\n"
                + "X-Custom2: ABCDEFG 987654321\r\n"
                + "Content-Length: 15\r\n"
                + "\r\n"
                + "こんにちは";
        // @formatter:on
        assertEquals(expected, new String(r, StandardCharsets.UTF_8));
        assertEquals(0, fhr.refCnt());
    }
}
