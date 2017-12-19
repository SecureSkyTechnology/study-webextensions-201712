package com.secureskytech.scdemosrv.proxy;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.regex.Pattern;

import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import com.secureskytech.scdemosrv.AppContext;

import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;

public class MyHttpProxyServer {

    private final MyHttpProxyServerConfig config;
    private final InetSocketAddress listeningAddress;
    private final ImpersonatingMitmManager mitmManager =
        ImpersonatingMitmManager.builder().trustAllServers(true).build();

    private HttpProxyServer server;

    public MyHttpProxyServer(final AppContext appContext, final InetSocketAddress listeningAddress,
            final LFSMapper lfsMapper, final List<Pattern> targetHostNameRegexps,
            final List<String> excludeFilenameExtensions) {
        config = new MyHttpProxyServerConfig(appContext, lfsMapper, targetHostNameRegexps, excludeFilenameExtensions);
        this.listeningAddress = listeningAddress;
    }

    public void start() {
        server =
            DefaultHttpProxyServer
                .bootstrap()
                .withAddress(listeningAddress)
                .withManInTheMiddle(mitmManager)
                .withFiltersSource(new MyHttpFiltersSourceImpl(config))
                .start();
    }

    public void stop() {
        server.stop();
    }
}
