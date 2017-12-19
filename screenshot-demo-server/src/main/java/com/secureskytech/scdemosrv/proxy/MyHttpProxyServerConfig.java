package com.secureskytech.scdemosrv.proxy;

import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import com.secureskytech.scdemosrv.AppContext;

public class MyHttpProxyServerConfig {

    public final AppContext appContext;
    public final LFSMapper lfsMapper;
    public final List<Pattern> targetHostNameRegexps;
    public final List<String> excludeFilenameExtensions;

    public MyHttpProxyServerConfig(final AppContext appContext, final LFSMapper lfsMapper,
            final List<Pattern> targetHostNameRegexps, final List<String> excludeFilenameExtensions) {
        this.appContext = appContext;
        this.lfsMapper = lfsMapper;
        this.targetHostNameRegexps = targetHostNameRegexps;
        this.excludeFilenameExtensions = excludeFilenameExtensions;
    }

    public boolean isLoggingTarget(String host, URL url) {
        boolean matched = false;
        for (Pattern p : targetHostNameRegexps) {
            if (p.matcher(host).find()) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            return false;
        }
        String path = url.getPath();
        String[] dotseps = path.split("\\.");
        if (dotseps.length < 2) {
            // no filename extension like ".../"
            return true;
        }
        String lastSep = dotseps[dotseps.length - 1];
        for (String ext : excludeFilenameExtensions) {
            if (ext.equals(lastSep)) {
                return false;
            }
        }
        return true;
    }
}
