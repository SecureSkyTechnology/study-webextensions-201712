package com.secureskytech.scdemosrv.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.secureskytech.scdemosrv.AppContext;
import com.secureskytech.scdemosrv.model.JdbcConnectionFactory;
import com.secureskytech.scdemosrv.proxy.LFSMapper.IndexHandlePolicy;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class MyHttpFiltersImplTest {

    Path tmpPath;

    @Before
    public void prepareTestDatabase() throws Exception {
        tmpPath = Files.createTempDirectory("MyHttpFiltersImplTest");
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
    public void test() throws IOException {
        String tmpPathDir = tmpPath.toString();
        Files.createDirectory(Paths.get(tmpPathDir, "aaa"));
        Files.createFile(Paths.get(tmpPathDir, "aaa", "index.html"));
        Files.write(Paths.get(tmpPathDir, "aaa", "index.html"), "こんにちは".getBytes(StandardCharsets.UTF_8));

        final AppContext appContext = new AppContext(Clock.systemDefaultZone(), new JdbcConnectionFactory());
        final LFSMapper m0 = new LFSMapper();
        m0.addMap(
            new LFSMapEntry(
                "localhost",
                "/",
                Paths.get(tmpPathDir, "aaa").toString(),
                IndexHandlePolicy.PreferIndexHtml,
                new TreeSet<>(Arrays.asList("html")),
                StandardCharsets.UTF_8));
        List<Pattern> targetHostNameRegexps = Arrays.asList(Pattern.compile("^xxx$"));
        List<String> exlcudeFilenameExtensions = Arrays.asList("js", "css");
        MyHttpProxyServerConfig config =
            new MyHttpProxyServerConfig(appContext, m0, targetHostNameRegexps, exlcudeFilenameExtensions);

        MyHttpFiltersImpl f0 =
            new MyHttpFiltersImpl(null, null, new InetSocketAddress("localhsot", 80), config, 0L, 1L);
        assertNull(f0.proxyToServerRequest(null));

        DefaultFullHttpRequest hreq = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        HttpHeaders.setHost(hreq, "localhost");
        FullHttpResponse hres = (FullHttpResponse) f0.proxyToServerRequest(hreq);
        assertEquals(HttpVersion.HTTP_1_1, hres.getProtocolVersion());
        assertEquals(HttpResponseStatus.OK, hres.getStatus());
        assertEquals("text/html; charset=UTF-8", hres.headers().get(HttpHeaders.Names.CONTENT_TYPE));
        assertEquals(HttpHeaders.Values.CLOSE, hres.headers().get(HttpHeaders.Names.CONNECTION));
        ByteBuf content = hres.content();
        byte[] bytes = new byte[content.readableBytes()];
        content.readBytes(bytes);
        assertEquals("こんにちは", new String(bytes, StandardCharsets.UTF_8));

        hreq = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/");
        HttpHeaders.setHost(hreq, "localhost");
        assertNotNull(f0.proxyToServerRequest(hreq));

        hreq = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, "/");
        HttpHeaders.setHost(hreq, "localhost");
        assertNotNull(f0.proxyToServerRequest(hreq));

        hreq = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PATCH, "/");
        HttpHeaders.setHost(hreq, "localhost");
        assertNotNull(f0.proxyToServerRequest(hreq));

        hreq = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.DELETE, "/");
        HttpHeaders.setHost(hreq, "localhost");
        assertNotNull(f0.proxyToServerRequest(hreq));

        hreq = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.OPTIONS, "/");
        HttpHeaders.setHost(hreq, "localhost");
        assertNull(f0.proxyToServerRequest(hreq));

        hreq = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.HEAD, "/");
        HttpHeaders.setHost(hreq, "localhost");
        assertNull(f0.proxyToServerRequest(hreq));

        hreq = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, "/");
        HttpHeaders.setHost(hreq, "localhost");
        assertNull(f0.proxyToServerRequest(hreq));

        hreq = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.TRACE, "/");
        HttpHeaders.setHost(hreq, "localhost");
        assertNull(f0.proxyToServerRequest(hreq));

        hreq = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        assertNull(f0.proxyToServerRequest(hreq));

        hreq = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "C:\\aaa\\bbb\\ccc.html");
        HttpHeaders.setHost(hreq, "localhost");
        assertNull(f0.proxyToServerRequest(hreq));
    }

}
