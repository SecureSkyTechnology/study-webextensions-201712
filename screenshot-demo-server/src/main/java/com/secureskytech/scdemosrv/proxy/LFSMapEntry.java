package com.secureskytech.scdemosrv.proxy;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.secureskytech.scdemosrv.ApacheHttpdMimeTypes;
import com.secureskytech.scdemosrv.proxy.LFSMapper.IndexHandlePolicy;

import lombok.Data;

@Data
public class LFSMapEntry implements Cloneable {

    private String hostHeader;
    private String path;
    private File localDir;
    private IndexHandlePolicy indexHandlePolicy;
    private Set<String> mappedExtensions;
    private Charset textCharset;

    public LFSMapEntry(String hostHeader, String path, String localDirStr, IndexHandlePolicy policy,
            Set<String> mappedExtensions, Charset textCharset) {
        this.hostHeader = hostHeader;
        this.path = path;
        this.localDir = new File(localDirStr);
        this.indexHandlePolicy = policy;
        this.mappedExtensions = new TreeSet<>(mappedExtensions);
        this.textCharset = textCharset;
    }

    /**
     * @param testHostHeader Host header value in HTTP request
     * @param testPath path component in HTTP request line ({@link java.net.URL#getPath()} or {@link java.net.URI#getPath()})
     * @return
     */
    public boolean matches(String testHostHeader, String testPath) {
        if (StringUtils.isEmpty(testHostHeader) || StringUtils.isEmpty(testPath)) {
            return false;
        }
        if (!testHostHeader.equals(hostHeader)) {
            return false;
        }
        if (!testPath.startsWith(path)) {
            return false;
        }
        if (testPath.lastIndexOf("/") == (testPath.length() - 1)) {
            if (indexHandlePolicy == IndexHandlePolicy.ProxyToServer) {
                return false;
            }
            if (indexHandlePolicy == IndexHandlePolicy.PreferIndexHtml) {
                return true;
            }
        }
        final String pathext = ApacheHttpdMimeTypes.defaultMimeTypes.getExtension(testPath);
        if (mappedExtensions.contains(pathext)) {
            return true;
        }
        return false;
    }

    public Object[] toJTableRow() {
        String[] r = new String[6];
        r[0] = this.hostHeader;
        r[1] = this.path;
        r[2] = this.localDir.getAbsolutePath();
        r[3] = this.indexHandlePolicy.toString();
        r[4] = String.join(" ", this.mappedExtensions);
        r[5] = this.textCharset.name();
        return r;
    }

    @Override
    protected Object clone() {
        LFSMapEntry n =
            new LFSMapEntry(
                hostHeader,
                path,
                localDir.getAbsolutePath(),
                indexHandlePolicy,
                mappedExtensions,
                textCharset);
        return n;
    }
}
