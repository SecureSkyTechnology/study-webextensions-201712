package com.secureskytech.scdemosrv;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.secureskytech.scdemosrv.proxy.LFSMapEntry;
import com.secureskytech.scdemosrv.proxy.LFSMapper.IndexHandlePolicy;

public class GUIConfigTest {

    File tmpf;

    @Before
    public void beforeMethod() throws IOException {
        tmpf = File.createTempFile("GUIConfigTest", ".tmp");
    }

    @After
    public void afterMethod() {
        if (tmpf.exists()) {
            tmpf.delete();
        }
    }

    @Test(expected = FileNotFoundException.class)
    public void testLoadButFileNotFound() throws IOException {
        tmpf.delete();
        GUIConfig.load(tmpf);
    }

    @Test
    public void testNormalOperation() throws IOException {
        GUIConfig gc0 = new GUIConfig();
        gc0.save(tmpf);
        GUIConfig gc1 = GUIConfig.load(tmpf);
        assertEquals(gc0.proxyPort, gc1.proxyPort);
        assertEquals(gc0.webUIPort, gc1.webUIPort);
        assertArrayEquals(gc0.targetHostNames.toArray(new String[] {}), gc1.targetHostNames.toArray(new String[] {}));
        assertArrayEquals(
            gc0.excludeFilenameExtensions.toArray(new String[] {}),
            gc1.excludeFilenameExtensions.toArray(new String[] {}));
        assertEquals(0, gc1.listOfMapEntry4Yaml.size());

        gc1.proxyPort = 10;
        gc1.webUIPort = 20;
        gc1.targetHostNames = Arrays.asList("aaa", "bbb");
        gc1.excludeFilenameExtensions = Arrays.asList(".ccc", ".ddd");
        LFSMapEntry lfsmap0 =
            new LFSMapEntry(
                "localhost",
                "/a",
                "local1",
                IndexHandlePolicy.PreferIndexHtml,
                new LinkedHashSet<>(Arrays.asList(".jpg", ".bmp")),
                StandardCharsets.UTF_8);
        LFSMapEntry lfsmap1 =
            new LFSMapEntry(
                "localhost",
                "/b",
                "local2",
                IndexHandlePolicy.ProxyToServer,
                new LinkedHashSet<>(Arrays.asList(".ico", ".svg")),
                StandardCharsets.ISO_8859_1);
        gc1.updateMapEntries(Arrays.asList(lfsmap0, lfsmap1));
        gc1.save(tmpf);

        GUIConfig gc2 = GUIConfig.load(tmpf);
        assertEquals(gc1.proxyPort, gc2.proxyPort);
        assertEquals(gc1.webUIPort, gc2.webUIPort);
        assertArrayEquals(gc1.targetHostNames.toArray(new String[] {}), gc2.targetHostNames.toArray(new String[] {}));
        assertArrayEquals(
            gc1.excludeFilenameExtensions.toArray(new String[] {}),
            gc2.excludeFilenameExtensions.toArray(new String[] {}));
        List<LFSMapEntry> lfsmapl0 = gc2.convertToLFSMapEntries();
        assertEquals(2, lfsmapl0.size());
        LFSMapEntry lfsmapx = lfsmapl0.get(0);
        assertEquals("localhost", lfsmapx.getHostHeader());
        assertEquals("/a", lfsmapx.getPath());
        assertEquals("local1", lfsmapx.getLocalDir().getName());
        assertEquals(IndexHandlePolicy.PreferIndexHtml, lfsmapx.getIndexHandlePolicy());
        assertEquals(2, lfsmapx.getMappedExtensions().size());
        assertTrue(lfsmapx.getMappedExtensions().contains(".jpg"));
        assertTrue(lfsmapx.getMappedExtensions().contains(".bmp"));
        assertEquals(StandardCharsets.UTF_8, lfsmapx.getTextCharset());

        lfsmapx = lfsmapl0.get(1);
        assertEquals("localhost", lfsmapx.getHostHeader());
        assertEquals("/b", lfsmapx.getPath());
        assertEquals("local2", lfsmapx.getLocalDir().getName());
        assertEquals(IndexHandlePolicy.ProxyToServer, lfsmapx.getIndexHandlePolicy());
        assertEquals(2, lfsmapx.getMappedExtensions().size());
        assertTrue(lfsmapx.getMappedExtensions().contains(".ico"));
        assertTrue(lfsmapx.getMappedExtensions().contains(".svg"));
        assertEquals(StandardCharsets.ISO_8859_1, lfsmapx.getTextCharset());
    }

    @Test
    public void testConvertWildcardToRegexp() {
        assertEquals("^.*$", GUIConfig.convertWildcardToRegexp("*"));
        Pattern pat0 = Pattern.compile(GUIConfig.convertWildcardToRegexp("*"));
        assertTrue(pat0.matcher("a").matches());
        assertEquals("^aaa.*bbb\\.ccc$", GUIConfig.convertWildcardToRegexp("aaa*bbb.ccc"));
        Pattern pat1 = Pattern.compile(GUIConfig.convertWildcardToRegexp("aaa*bbb.ccc"));
        assertTrue(pat1.matcher("aaabbb.ccc").matches());
        assertTrue(pat1.matcher("aaaXbbb.ccc").matches());
        assertTrue(pat1.matcher("aaaXYZbbb.ccc").matches());
    }

    @Test
    public void testIsValidRegexpPattern() {
        assertTrue(GUIConfig.isValidRegexpPattern("~.*$"));
        assertTrue(GUIConfig.isValidRegexpPattern("aaa"));
        assertFalse(GUIConfig.isValidRegexpPattern("[a-0*"));
    }
}
