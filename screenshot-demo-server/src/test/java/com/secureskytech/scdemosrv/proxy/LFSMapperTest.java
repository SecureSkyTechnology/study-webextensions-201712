package com.secureskytech.scdemosrv.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.secureskytech.scdemosrv.proxy.LFSMapper.IndexHandlePolicy;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class LFSMapperTest {

    Path tmpPath;

    @Before
    public void prepareTestDatabase() throws Exception {
        tmpPath = Files.createTempDirectory("test");
    }

    @After
    public void cleanUp() throws Exception {
        Files.walkFileTree(tmpPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Test
    public void testAddAndClearMap() {
        LFSMapper m0 = new LFSMapper();
        m0.addMap(
            new LFSMapEntry(
                "localhost1",
                "/",
                "/",
                IndexHandlePolicy.ProxyToServer,
                new TreeSet<>(Arrays.asList("txt")),
                StandardCharsets.UTF_8));
        m0.addMap(
            new LFSMapEntry(
                "localhost2",
                "/",
                "/",
                IndexHandlePolicy.ProxyToServer,
                new TreeSet<>(Arrays.asList("txt")),
                StandardCharsets.UTF_8));

        List<LFSMapEntry> el0 = m0.getMaps();
        assertEquals(2, el0.size());
        assertEquals("localhost1", el0.get(0).getHostHeader());
        assertEquals("localhost2", el0.get(1).getHostHeader());

        m0.clearMap();
        List<LFSMapEntry> el2 = m0.getMaps();
        assertEquals(0, el2.size());

        assertNull(m0.getMap("localhost999", "/"));
    }

    @Test
    public void testGetMap() {
        LFSMapper m0 = new LFSMapper();
        m0.addMap(
            new LFSMapEntry(
                "localhost1",
                "/",
                "/",
                IndexHandlePolicy.PreferIndexHtml,
                new TreeSet<>(Arrays.asList("txt")),
                StandardCharsets.UTF_8));
        m0.addMap(
            new LFSMapEntry(
                "localhost2",
                "/",
                "/",
                IndexHandlePolicy.ProxyToServer,
                new TreeSet<>(Arrays.asList("txt")),
                StandardCharsets.UTF_8));

        assertNull(m0.getMap(null, null));
        assertNull(m0.getMap("", null));
        assertNull(m0.getMap(null, ""));
        assertNull(m0.getMap("localhost", "/"));
        assertEquals("localhost1", m0.getMap("localhost1", "/").getHostHeader());
        assertNull(m0.getMap("localhost2", "/"));
        assertEquals("localhost2", m0.getMap("localhost2", "/aaa.txt").getHostHeader());
        assertNull(m0.getMap("localhost999", "/"));
    }

    @Test
    public void testCreateMappedResponseWithPreferIndexHtml() throws IOException {
        String tmpPathDir = tmpPath.toString();
        Files.createDirectory(Paths.get(tmpPathDir, "aaa"));
        Files.createFile(Paths.get(tmpPathDir, "aaa", "index.html"));
        Files.write(Paths.get(tmpPathDir, "aaa", "index.html"), "こんにちは".getBytes(StandardCharsets.UTF_8));

        LFSMapEntry e0 =
            new LFSMapEntry(
                "localhost",
                "/",
                Paths.get(tmpPathDir, "aaa").toString(),
                IndexHandlePolicy.PreferIndexHtml,
                // LFSMapper.createMappedResponse() does NOT check file extensions.
                new TreeSet<>(Arrays.asList("dummy")),
                StandardCharsets.UTF_8);

        FullHttpResponse hr = (FullHttpResponse) LFSMapper.createMappedResponse(e0, "/test.txt");
        assertNull(hr);

        hr = (FullHttpResponse) LFSMapper.createMappedResponse(e0, "/aaa/bbb/");
        assertNull(hr);

        hr = (FullHttpResponse) LFSMapper.createMappedResponse(e0, "/");
        assertEquals(HttpVersion.HTTP_1_1, hr.getProtocolVersion());
        assertEquals(HttpResponseStatus.OK, hr.getStatus());
        assertEquals("text/html; charset=UTF-8", hr.headers().get(HttpHeaders.Names.CONTENT_TYPE));
        assertEquals(HttpHeaders.Values.CLOSE, hr.headers().get(HttpHeaders.Names.CONNECTION));
        ByteBuf content = hr.content();
        byte[] bytes = new byte[content.readableBytes()];
        content.readBytes(bytes);
        assertEquals("こんにちは", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testCreateMappedResponseWithProxyToServer() throws IOException {
        String tmpPathDir = tmpPath.toString();
        Files.createDirectory(Paths.get(tmpPathDir, "bbb"));
        Files.createFile(Paths.get(tmpPathDir, "bbb", "test.html"));
        Files.write(Paths.get(tmpPathDir, "bbb", "test.html"), "こんにちは".getBytes(StandardCharsets.UTF_8));

        LFSMapEntry e0 =
            new LFSMapEntry(
                "localhost",
                "/bbb/",
                Paths.get(tmpPathDir, "bbb").toString(),
                IndexHandlePolicy.ProxyToServer,
                // LFSMapper.createMappedResponse() does NOT check file extensions.
                new TreeSet<>(Arrays.asList("dummy")),
                StandardCharsets.UTF_8);

        FullHttpResponse hr = (FullHttpResponse) LFSMapper.createMappedResponse(e0, "/bbb/");
        assertNull(hr);

        hr = (FullHttpResponse) LFSMapper.createMappedResponse(e0, "/bbb/ccc/ddd/");
        assertNull(hr);

        hr = (FullHttpResponse) LFSMapper.createMappedResponse(e0, "/bbb/test.css");
        assertNull(hr);

        hr = (FullHttpResponse) LFSMapper.createMappedResponse(e0, "/bbb/test.html");
        assertEquals(HttpVersion.HTTP_1_1, hr.getProtocolVersion());
        assertEquals(HttpResponseStatus.OK, hr.getStatus());
        assertEquals("text/html; charset=UTF-8", hr.headers().get(HttpHeaders.Names.CONTENT_TYPE));
        assertEquals(HttpHeaders.Values.CLOSE, hr.headers().get(HttpHeaders.Names.CONNECTION));
        ByteBuf content = hr.content();
        byte[] bytes = new byte[content.readableBytes()];
        content.readBytes(bytes);
        assertEquals("こんにちは", new String(bytes, StandardCharsets.UTF_8));
    }
}
