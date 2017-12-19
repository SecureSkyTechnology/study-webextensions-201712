package com.secureskytech.scdemosrv.proxy;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.secureskytech.scdemosrv.model.ProxyHistory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public class MyHttpFiltersImpl extends HttpFiltersAdapter {
    private static Logger LOG = LoggerFactory.getLogger(MyHttpFiltersImpl.class);
    private final Marker m;
    private final MyHttpProxyServerConfig config;
    private final long httpCount;
    private final InetSocketAddress clientAddress;
    private boolean isHttps = false;
    private String scheme;
    private FullHttpRequest requestSent;
    private FullHttpResponse responseReceived;
    private InetSocketAddress connectedRemoteAddress;
    private final long at0;
    private long atProxyToServerRequestSent;
    private long atServerToProxyResponseReceived;

    /* HTTPSのMITMを通すときにインスタンスが新しく作成されるために引き継げないフィールドがある。
     * 以下は、それら引き継げないフィールドを channel の attribute に設定するkey。
     * 
     * 例: resolvedRemoteAddress ->
     * HTTPSでMITMを通った時は、resolvedされるのは最初のCONNECT時のインスタンスで、
     * MITMの中を通すときのインスタンスは別となり、resolveされたInetSocketAddressが引き継がれない。
     * また、MITMの中を通すときのインスタンス中から ctx.channel().remoteAddress() を呼んでもそれは
     * MITM用のサーバとなってしまうため、本当の接続先情報とはならない。
     * このため、proxyToServerResolutionSucceeded() の時点で InetSocketAddress を channel のattribute
     * にset()し、これを使うことでhttp/https共用、かつ、外部からインジェクション可能な状態にして
     * テストコードを書ける状態にしておく。
     * 
     * テストコード作成を考慮するとchannelのmockが非常に手間取ることがわかったため、可能な限り
     * 外部からインジェクション可能な設計にしている。
     * (= adapterなどの補助クラスが見当たらない)
     */
    private final AttributeKey<Boolean> isHttpsAttrKey = AttributeKey.valueOf("isHttps");
    private final AttributeKey<InetSocketAddress> resolvedRemoteAddressKey =
        AttributeKey.valueOf("resolvedRemoteAddress");
    private final AttributeKey<Long> atProxyToServerConnectionStartedKey =
        AttributeKey.valueOf("atProxyToServerConnectionStarted");
    private final AttributeKey<Long> atProxyToServerConnectionSucceededKey =
        AttributeKey.valueOf("atProxyToServerConnectionSucceeded");

    public MyHttpFiltersImpl(HttpRequest originalRequest, ChannelHandlerContext ctx,
            final InetSocketAddress clientAddress, final MyHttpProxyServerConfig config, final long at0,
            final long httpCount) {
        super(originalRequest, ctx);
        this.config = config;
        this.clientAddress = clientAddress;
        this.httpCount = httpCount;
        this.at0 = at0;
        this.m = MarkerFactory.getMarker("http#" + httpCount);
        LOG.debug(m, "client IP : {}", clientAddress.toString());
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        if (httpObject instanceof HttpRequest) {
            HttpRequest hr = (HttpRequest) httpObject;
            Attribute<Boolean> isHttpsAttr = this.ctx.attr(this.isHttpsAttrKey);
            Boolean isHttpsVal = isHttpsAttr.get();
            if (Objects.isNull(isHttpsVal)) {
                isHttpsVal = false;
            }
            if (hr.getMethod().equals(HttpMethod.CONNECT)) {
                isHttpsVal = true;
            }
            this.isHttps = isHttpsVal.booleanValue();
            isHttpsAttr.set(isHttpsVal);
            LOG.debug(
                m,
                "http request : https? : {}, {} {} {}",
                this.isHttps,
                hr.getMethod(),
                hr.getUri(),
                hr.getProtocolVersion());
        }
        return null;
    }

    @Override
    public void proxyToServerResolutionSucceeded(String serverHostAndPort, InetSocketAddress resolvedRemoteAddress) {
        LOG.debug(m, "proxyToServerResolutionSucceeded({}, {}", serverHostAndPort, resolvedRemoteAddress);
        Attribute<InetSocketAddress> resolvedRemoteAddressAttr = this.ctx.attr(this.resolvedRemoteAddressKey);
        resolvedRemoteAddressAttr.set(resolvedRemoteAddress);
    }

    @Override
    public HttpResponse proxyToServerRequest(HttpObject httpObject) {
        LOG.trace(m, "proxyToServerRequest()");
        if (!(httpObject instanceof FullHttpRequest)) {
            return null;
        }
        FullHttpRequest fhr = (FullHttpRequest) httpObject;
        final HttpMethod method = fhr.getMethod();
        boolean isLFSMappableMethod =
            HttpMethod.GET.equals(method)
                || HttpMethod.POST.equals(method)
                || HttpMethod.PUT.equals(method)
                || HttpMethod.DELETE.equals(method)
                || HttpMethod.PATCH.equals(method);
        URI uri = null;
        try {
            // schema + host is dummy.
            uri = new URI(fhr.getUri());
        } catch (Exception e) {
            LOG.trace(m, "proxyToServerRequest() : newURI(fhr.getUri()) error : {}", e.getMessage());
            isLFSMappableMethod = false;
        }
        final String hostHeader = HttpHeaders.getHost(fhr, "");
        if (!StringUtils.isEmpty(hostHeader) && isLFSMappableMethod && Objects.nonNull(uri)) {
            LOG.info(m, "proxyToServerRequest() : proxy -> server, Host={}, {}, {}", hostHeader, method, fhr.getUri());
            final String path = uri.getPath();
            LOG.trace(m, "proxyToServerRequest() : matcher path = {}", path);
            LFSMapEntry map = config.lfsMapper.getMap(hostHeader, path);
            if (Objects.nonNull(map)) {
                LOG.info(
                    m,
                    "proxyToServerRequest() : proxy -> server, map path={} to {}",
                    path,
                    map.getLocalDir().getAbsolutePath());
                return LFSMapper.createMappedResponse(map, path);
            }
        }
        if (Objects.isNull(this.ctx)) {
            // for unit test (TODO : この辺の上手なmock技法が無いか調査)
            return null;
        }
        Attribute<InetSocketAddress> resolvedRemoteAddressAttr = this.ctx.attr(this.resolvedRemoteAddressKey);
        InetSocketAddress currentResolved = resolvedRemoteAddressAttr.get();
        final StringBuilder sburl = new StringBuilder();
        this.scheme = (this.isHttps) ? "https" : "http";
        sburl.append(this.scheme);
        sburl.append("://");
        String destHost = currentResolved.getHostString();
        sburl.append(destHost);
        int destPort = currentResolved.getPort();
        if (((!this.isHttps) && 80 == destPort) || (this.isHttps && 443 == destPort)) {
            sburl.append(fhr.getUri());
        } else {
            sburl.append(":");
            sburl.append(destPort);
            sburl.append(fhr.getUri());
        }
        final String fullUrl = sburl.toString();
        try {
            if (!this.config.isLoggingTarget(hostHeader, new URL(fullUrl))) {
                LOG.trace(m, "proxyToServerRequest() : logging SKIPPED for no matched url : {}", fullUrl);
                return null;
            }
        } catch (MalformedURLException e) {
            LOG.warn(m, "proxyToServerRequest() URL build error", e);
            return null;
        }
        LOG.trace(m, "proxyToServerRequest() : log TARGETTED request, matched url : {}", fullUrl);
        this.requestSent = fhr.copy();
        try (Connection conn = this.config.appContext.createDbConnection()) {
            ProxyHistory.insert(
                conn,
                this.config.appContext.getLogContext(),
                httpCount,
                this.clientAddress,
                currentResolved,
                this.scheme,
                this.requestSent,
                new Timestamp(this.at0));
        } catch (SQLException e) {
            LOG.error(m, "ProxyHistory.insert() db error", e);
        }
        return null;
    }

    @Override
    public void proxyToServerConnectionStarted() {
        LOG.trace(m, "proxyToServerConnectionStarted()");
        Attribute<Long> atProxyToServerConnectionStartedAttr = this.ctx.attr(this.atProxyToServerConnectionStartedKey);
        atProxyToServerConnectionStartedAttr.set(config.appContext.now());
    }

    @Override
    public void proxyToServerConnectionSucceeded(ChannelHandlerContext serverCtx) {
        this.connectedRemoteAddress = (InetSocketAddress) serverCtx.channel().remoteAddress();
        LOG.debug(m, "proxyToServerConnectionSucceeded() to {}", this.connectedRemoteAddress);
        Attribute<Long> atProxyToServerConnectionSucceededAttr =
            this.ctx.attr(this.atProxyToServerConnectionSucceededKey);
        atProxyToServerConnectionSucceededAttr.set(config.appContext.now());
    }

    @Override
    public void proxyToServerRequestSent() {
        LOG.trace(m, "proxyToServerRequestSent()");
        this.atProxyToServerRequestSent = config.appContext.now();
    }

    @Override
    public HttpObject serverToProxyResponse(HttpObject httpObject) {
        LOG.trace(m, "serverToProxyResponse()");
        if (!(httpObject instanceof FullHttpResponse)) {
            return httpObject;
        }
        FullHttpResponse fhr = (FullHttpResponse) httpObject;
        final HttpResponseStatus status = fhr.getStatus();
        final HttpVersion version = fhr.getProtocolVersion();
        final long contentLength = HttpHeaders.getContentLength(fhr, 0L);
        LOG.debug(
            m,
            "serverToProxyResponse(): {} {} {}, length={}",
            version.text(),
            status.code(),
            status.reasonPhrase(),
            contentLength);
        this.responseReceived = fhr.copy();
        return httpObject;
    }

    @Override
    public HttpObject proxyToClientResponse(HttpObject httpObject) {
        LOG.trace(m, "proxyToClientResponse()");
        if (httpObject instanceof FullHttpResponse) {
            FullHttpResponse fhr = (FullHttpResponse) httpObject;
            final HttpResponseStatus status = fhr.getStatus();
            final HttpVersion version = fhr.getProtocolVersion();
            final long contentLength = HttpHeaders.getContentLength(fhr, 0L);
            LOG.debug(
                m,
                "proxyToClientResponse(): {} {} {}, length={}",
                version.text(),
                status.code(),
                status.reasonPhrase(),
                contentLength);
        }
        return httpObject;
    }

    @Override
    public void serverToProxyResponseReceived() {
        LOG.trace(m, "serverToProxyResponseReceived()");
        this.atServerToProxyResponseReceived = config.appContext.now();
        if (Objects.isNull(this.requestSent) || Objects.isNull(this.responseReceived)) {
            LOG.trace(m, "serverToProxyResponseReceived() : logging SKIPPED cause neither request nor response saved.");
            return;
        }
        Attribute<Long> atProxyToServerConnectionStartedAttr = this.ctx.attr(this.atProxyToServerConnectionStartedKey);
        final long atProxyToServerConnectionStarted = atProxyToServerConnectionStartedAttr.get();
        Attribute<Long> atProxyToServerConnectionSucceededAttr =
            this.ctx.attr(this.atProxyToServerConnectionSucceededKey);
        final long atProxyToServerConnectionSucceeded = atProxyToServerConnectionSucceededAttr.get();
        LOG.trace(m, "serverToProxyResponseReceived() : log TARGETTED response.");
        try (Connection conn = this.config.appContext.createDbConnection()) {
            ProxyHistory.update(
                conn,
                this.config.appContext.getLogContext(),
                httpCount,
                this.responseReceived,
                new Timestamp(this.at0),
                new Timestamp(atProxyToServerConnectionStarted),
                new Timestamp(atProxyToServerConnectionSucceeded),
                new Timestamp(this.atProxyToServerRequestSent),
                new Timestamp(this.atServerToProxyResponseReceived));
        } catch (SQLException e) {
            LOG.error(m, "ProxyHistory.update() db error", e);
        }
    }
}
