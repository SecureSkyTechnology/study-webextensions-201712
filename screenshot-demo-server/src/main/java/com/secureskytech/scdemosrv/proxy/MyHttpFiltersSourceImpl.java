package com.secureskytech.scdemosrv.proxy;

import java.net.InetSocketAddress;
import java.net.URI;

import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;

import com.secureskytech.scdemosrv.ApacheHttpdMimeTypes;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpRequest;

public class MyHttpFiltersSourceImpl extends HttpFiltersSourceAdapter {

    private final MyHttpProxyServerConfig config;
    private long httpCount = 0L;

    public MyHttpFiltersSourceImpl(MyHttpProxyServerConfig config) {
        this.config = config;
    }

    @Override
    public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
        try {
            final URI uri = new URI(originalRequest.getUri());
            final String ext = ApacheHttpdMimeTypes.defaultMimeTypes.getExtension(uri.getPath());
            if ("iso".equals(ext) || "dmg".equals(ext) || "exe".equals(ext)) {
                return new HttpFiltersAdapter(originalRequest, ctx) {
                    @Override
                    public void proxyToServerConnectionSucceeded(ChannelHandlerContext serverCtx) {
                        ChannelPipeline pipeline = serverCtx.pipeline();
                        if (pipeline.get("inflater") != null) {
                            pipeline.remove("inflater");
                        }
                        if (pipeline.get("aggregator") != null) {
                            pipeline.remove("aggregator");
                        }
                        super.proxyToServerConnectionSucceeded(serverCtx);
                    }
                };
            }
        } catch (Exception ignore) {
        }
        httpCount++;
        final long at0 = config.appContext.now();
        return new MyHttpFiltersImpl(
            originalRequest,
            ctx,
            (InetSocketAddress) ctx.channel().remoteAddress(),
            config,
            at0,
            httpCount);
    }

    @Override
    public int getMaximumRequestBufferSizeInBytes() {
        return 10 * 1024 * 1024; // aggregate chunks and decompress until 10MB request.
    }

    @Override
    public int getMaximumResponseBufferSizeInBytes() {
        return 10 * 1024 * 1024; // aggregate chunks and decompress until 10MB response.
    }

}
