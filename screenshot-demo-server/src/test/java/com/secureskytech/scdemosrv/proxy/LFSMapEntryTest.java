package com.secureskytech.scdemosrv.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.TreeSet;

import org.junit.Test;

import com.secureskytech.scdemosrv.proxy.LFSMapper.IndexHandlePolicy;

public class LFSMapEntryTest {

    @Test
    public void test() {
        LFSMapEntry e0 =
            new LFSMapEntry(
                "localhost",
                "/aaa/bbb/",
                "/local/fs/",
                IndexHandlePolicy.ProxyToServer,
                new TreeSet<>(Arrays.asList("txt", "css")),
                StandardCharsets.UTF_8);
        assertFalse(e0.matches(null, null));
        assertFalse(e0.matches("", null));
        assertFalse(e0.matches(null, ""));
        assertFalse(e0.matches("localhos", "/"));
        assertFalse(e0.matches("localhost", "/"));
        assertFalse(e0.matches("localhost", "/aaa/"));
        assertFalse(e0.matches("localhost", "/aaa/bbb"));
        assertFalse(e0.matches("localhost", "/aaa/bbb/"));
        assertFalse(e0.matches("localhost", "/aaa/bbb/ccc/ddd/"));
        assertFalse(e0.matches("localhost", "/aaa/bbb/ccc/ddd.html"));
        assertTrue(e0.matches("localhost", "/aaa/bbb/test.txt"));
        assertTrue(e0.matches("localhost", "/aaa/bbb/ccc/ddd/test.css"));

        String[] rows = (String[]) e0.toJTableRow();
        assertEquals(6, rows.length);
        assertEquals("localhost", rows[0]);
        assertEquals("/aaa/bbb/", rows[1]);
        if (File.separatorChar == '\\') {
            assertEquals("C:\\local\\fs", rows[2]);
        } else {
            assertEquals("", rows[2]);
        }
        assertEquals(e0.getIndexHandlePolicy().toString(), rows[3]);
        assertEquals("css txt", rows[4]);
        assertEquals("UTF-8", rows[5]);

        LFSMapEntry e1 = (LFSMapEntry) e0.clone();
        e1.setHostHeader("localhostX");
        e1.setIndexHandlePolicy(IndexHandlePolicy.PreferIndexHtml);
        assertNotEquals(e0.getHostHeader(), e1.getHostHeader());
        assertNotEquals(e0.getIndexHandlePolicy(), e1.getIndexHandlePolicy());
        assertTrue(e1.matches("localhostX", "/aaa/bbb/"));
    }

}
