package com.secureskytech.scdemosrv.proxy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;

import com.secureskytech.scdemosrv.AppContext;
import com.secureskytech.scdemosrv.model.JdbcConnectionFactory;

public class MyHttpProxyServerConfigTest {
    @Test
    public void testBytesToReq_simpleGET() throws IOException {
        AppContext appContext = new AppContext(Clock.systemDefaultZone(), new JdbcConnectionFactory());
        LFSMapper lfsMapper = new LFSMapper();
        List<Pattern> targetHostNameRegexps =
            Arrays.asList(Pattern.compile("^localhost$"), Pattern.compile(".*\\.test\\.localhost"));
        List<String> exlcudeFilenameExtensions = Arrays.asList("js", "css");
        MyHttpProxyServerConfig config =
            new MyHttpProxyServerConfig(appContext, lfsMapper, targetHostNameRegexps, exlcudeFilenameExtensions);
        assertTrue(config.isLoggingTarget("localhost", new URL("http://localhost/")));
        assertTrue(config.isLoggingTarget("localhost", new URL("http://localhost/aaa/")));
        assertTrue(config.isLoggingTarget("localhost", new URL("http://localhost/aaa/index.html")));
        assertTrue(config.isLoggingTarget("aaa.test.localhost", new URL("http://localhost/css/")));
        assertFalse(config.isLoggingTarget("xxx.localhost", new URL("http://localhost/")));
        assertFalse(config.isLoggingTarget("Xlocalhost", new URL("http://localhost/")));
        assertFalse(config.isLoggingTarget("localhostY", new URL("http://localhost/")));
        assertFalse(config.isLoggingTarget("localhost", new URL("http://localhost/test.js?aaa=bbb")));
        assertFalse(config.isLoggingTarget("localhost", new URL("http://localhost/test.test.css?aaa=bbb")));
    }
}
