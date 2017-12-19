package com.secureskytech.scdemosrv.proxy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.secureskytech.scdemosrv.ApacheHttpdMimeTypes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class LFSMapper {
    private static Logger LOG = LoggerFactory.getLogger(LFSMapper.class);

    public static enum IndexHandlePolicy {
        ProxyToServer("Proxy to server-side \"/\""),
        PreferIndexHtml("Return index.html if exists.");
        final String desc;

        IndexHandlePolicy(String description) {
            this.desc = description;
        }

        @Override
        public String toString() {
            return desc;
        }
    }

    private List<LFSMapEntry> maps = new ArrayList<>();

    public void addMap(LFSMapEntry e) {
        this.maps.add(e);
    }

    public List<LFSMapEntry> getMaps() {
        return maps;
    }

    public void clearMap() {
        this.maps.clear();
    }

    /**
     * @param testHostHeader Host header value in HTTP request
     * @param testPath path component in HTTP request line ({@link java.net.URL#getPath()} or {@link java.net.URI#getPath()})
     * @return mapped entry or null (if not found any matching entry)
     */
    public LFSMapEntry getMap(String testHostHeader, String testPath) {
        LOG.trace("getMap({}, {})", testHostHeader, testPath);
        if (StringUtils.isEmpty(testHostHeader) || StringUtils.isEmpty(testPath)) {
            return null;
        }
        for (LFSMapEntry e : maps) {
            if (e.matches(testHostHeader, testPath)) {
                return e;
            }
        }
        return null;
    }

    /**
     * @param map mapped entry
     * @param srcPath path component in HTTP request line ({@link java.net.URL#getPath()} or {@link java.net.URI#getPath()})
     * @return {@link io.netty.handler.codec.http.HttpResponse} or null (if pass through)
     */
    public static HttpResponse createMappedResponse(LFSMapEntry map, String srcPath) {
        final String remains = srcPath.substring(map.getPath().length());
        boolean addIndexHtml = false;
        if (StringUtils.isBlank(remains) || (remains.lastIndexOf("/") == (remains.length() - 1))) {
            if (map.getIndexHandlePolicy() == IndexHandlePolicy.ProxyToServer) {
                LOG.debug("pass through \"/\" to backend server (source path = {})", srcPath);
                return null;
            }
            if (map.getIndexHandlePolicy() == IndexHandlePolicy.PreferIndexHtml) {
                addIndexHtml = true;
            }
        }
        final File mapf =
            new File(map.getLocalDir().getAbsolutePath() + "/" + remains + (addIndexHtml ? "index.html" : ""));
        final String mapfname = mapf.getAbsolutePath();
        LOG.debug("map : path=[{}] to local=[{}]", srcPath, mapfname);
        if (map.getIndexHandlePolicy() == IndexHandlePolicy.PreferIndexHtml && !mapf.exists()) {
            LOG.debug(
                "map : suspected index.html not found in local, pass throug \"/\" to backend server (path=[{}] to local=[{}]",
                srcPath,
                mapfname);
            return null;
        }
        try {
            final String pathext = ApacheHttpdMimeTypes.defaultMimeTypes.getExtension(mapfname);
            String mimetype = ApacheHttpdMimeTypes.defaultMimeTypes.getMimeType(pathext);
            if (mimetype.startsWith("text/") || "application/javascript".equals(mimetype)) {
                mimetype = mimetype + "; charset=" + map.getTextCharset().name();
            }
            LOG.debug("generated content-type: [{}] for local=[{}]", mimetype, mapfname);

            final byte[] mapfdata = Files.toByteArray(mapf);
            final ByteBuf buffer = Unpooled.wrappedBuffer(mapfdata);
            final int cl = buffer.readableBytes();
            LOG.debug("{} bytes read from local=[{}]", cl, mapfname);

            final HttpResponse response =
                new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
            HttpHeaders.setContentLength(response, cl);
            HttpHeaders.setHeader(response, HttpHeaders.Names.CONTENT_TYPE, mimetype);
            HttpHeaders.setHeader(response, HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);

            return response;
        } catch (Exception e) {
            LOG.error("error, mapping canceled, pass through to backend server.", e);
            return null;
        }
    }
}
