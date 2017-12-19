package com.secureskytech.scdemosrv;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.google.common.io.Resources;

public class VersionInfo {
    /** 
     * See Effective-Java 2nd, Item 4 / Checkstyle:HideUtilityClassConstructor
     */
    private VersionInfo() {
        throw new AssertionError("Java Rationale: Utility class prohibits Instantiation.");
    }

    public static String getManifestInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Version Info (from META-INF/MANIFEST.MF)\n");
        try {
            InputStream is = Resources.getResource("META-INF/MANIFEST.MF").openStream();
            if (null != is) {
                Manifest manifest = new Manifest(is);
                Attributes attrs = manifest.getMainAttributes();
                final String[] keys =
                    new String[] {
                        "Specification-Title",
                        "Specification-Version",
                        "X-Compile-Source-JDK",
                        "X-Compile-Target-JDK",
                        "Jenkins-Build-Tag",
                        "Jenkins-Build-Id",
                        "Git-Commit",
                        "Git-Branch" };
                for (String k : keys) {
                    String v = attrs.getValue(k);
                    sb.append(k + "=" + v + "\n");
                }
            } else {
                sb.append("no MANIFEST.MF (local debug environment ??)");
            }
        } catch (Exception e) {
            sb.append("MANIFEST.MF open,read error : " + e.getMessage());
        }
        sb.append("\n");
        return sb.toString();
    }

    public static String getSystemProperties() {
        StringBuilder sb = new StringBuilder();
        sb.append("System Information\n");
        Properties props = System.getProperties();
        TreeSet<Object> keys = new TreeSet<>(props.keySet());
        for (Object ko : keys) {
            String k = ko.toString();
            sb.append(k + "=" + props.getProperty(k) + "\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    public static String getThirdPartyLicenses() {
        StringBuilder sb = new StringBuilder();
        sb.append("Third-Party Licenses\n");
        try {
            sb.append(Resources.toString(Resources.getResource("THIRD-PARTY.txt"), StandardCharsets.UTF_8));
        } catch (Exception e) {
            sb.append("THIRD-PARTY.txt open,read error : " + e.getMessage());
        }
        sb.append("\n");
        return sb.toString();
    }
}
