package com.secureskytech.scdemosrv.model;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.AbstractListHandler;

import com.google.common.base.Strings;
import com.google.common.io.Resources;
import com.secureskytech.scdemosrv.proxy.MyNettyUtil;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.Data;

@Data
public class ProxyHistory {
    String logContext;
    long messageRef;
    String clientIp;
    int clientPort;
    String remoteHostname;
    String remoteIp;
    int remotePort;

    String fullUrl;

    String requestScheme;
    String requestMethod;
    String requestUrl;
    String requestProtocolVersion;
    String requestHostHeader;
    byte[] requestBytes;
    String requestString = "";
    String requestCharset;
    Charset requestCharsetInstance;

    short responseStatusCode;
    byte[] responseBytes;
    String responseString = "";;
    String responseCharset;
    Charset responseCharsetInstance;

    Timestamp atClientToProxy;
    Timestamp atProxyToServerConnectStart;
    Timestamp atProxyToServerConnected;
    Timestamp atProxyToServerRequestSent;
    Timestamp atServerToProxyResponseReceived;
    int elapsedTimeConnect;
    int elapsedTimeRequestSent;
    int elapsedTimeResponseReceived;
    int elapsedTimeFull;

    String comment;
    int countOfAttachedScreenshot = 0;

    public static Charset getCharset(String charsetName) {
        if (Strings.isNullOrEmpty(charsetName)) {
            return StandardCharsets.ISO_8859_1;
        }
        try {
            Charset charset = Charset.forName(charsetName);
            return charset;
        } catch (Exception ignore) {
            return StandardCharsets.ISO_8859_1;
        }
    }

    public static ProxyHistory create(ResultSet rs, final boolean needsHttpBytes, final boolean screenshotAware)
            throws SQLException {
        ProxyHistory r = new ProxyHistory();
        r.logContext = rs.getString("log_context");
        r.messageRef = rs.getLong("message_ref");
        r.clientIp = rs.getString("client_ip");
        r.clientPort = rs.getInt("client_port");
        r.remoteHostname = rs.getString("remote_hostname");
        r.remoteIp = rs.getString("remote_ip");
        r.remotePort = rs.getInt("remote_port");
        r.fullUrl = rs.getString("full_url");
        r.requestScheme = rs.getString("request_scheme");
        r.requestMethod = rs.getString("request_method");
        r.requestUrl = rs.getString("request_url");
        r.requestProtocolVersion = rs.getString("request_protocol_version");
        r.requestHostHeader = rs.getString("request_host_header");
        r.requestCharset = rs.getString("request_charset");
        r.requestCharsetInstance = getCharset(r.requestCharset);
        r.responseStatusCode = rs.getShort("response_status_code");
        r.responseCharset = rs.getString("response_charset");
        r.responseCharsetInstance = getCharset(r.responseCharset);
        if (needsHttpBytes) {
            r.requestBytes = rs.getBytes("request_bytes");
            r.requestString = new String(r.requestBytes, r.requestCharsetInstance);
            r.responseBytes = rs.getBytes("response_bytes");
            if (Objects.isNull(r.responseBytes)) {
                r.responseString = "";
            } else {
                r.responseString = new String(r.responseBytes, r.responseCharsetInstance);
            }
        }
        r.atClientToProxy = rs.getTimestamp("at_client_to_proxy");
        r.atProxyToServerConnectStart = rs.getTimestamp("at_proxy_to_server_connect_start");
        r.atProxyToServerConnected = rs.getTimestamp("at_proxy_to_server_connected");
        r.atProxyToServerRequestSent = rs.getTimestamp("at_proxy_to_server_request_sent");
        r.atServerToProxyResponseReceived = rs.getTimestamp("at_server_to_proxy_response_recv");
        r.elapsedTimeConnect = rs.getInt("elapsed_time_connect");
        r.elapsedTimeRequestSent = rs.getInt("elapsed_time_req_sent");
        r.elapsedTimeResponseReceived = rs.getInt("elapsed_time_res_recv");
        r.elapsedTimeFull = rs.getInt("elapsed_time_full");
        r.comment = rs.getString("comment");
        if (screenshotAware) {
            r.countOfAttachedScreenshot = rs.getInt("sch_count");
        }
        return r;
    }

    public static ResultSetHandler<List<ProxyHistory>> createListResultSetHandler() {
        return new AbstractListHandler<ProxyHistory>() {
            @Override
            protected ProxyHistory handleRow(ResultSet rs) throws SQLException {
                return ProxyHistory.create(rs, false, true);
            }
        };
    }

    public static ResultSetHandler<ProxyHistory> createGetOneResultSetHandler(final boolean screenshotAware) {
        return new ResultSetHandler<ProxyHistory>() {
            @Override
            public ProxyHistory handle(ResultSet rs) throws SQLException {
                if (!rs.next()) {
                    return null;
                }
                return ProxyHistory.create(rs, true, screenshotAware);
            }
        };
    }

    public static List<ProxyHistory> getList(Connection dbconn) throws SQLException {
        try {
            final String sql =
                Resources.toString(Resources.getResource("db/proxy_history_list_join_sc.sql"), StandardCharsets.UTF_8);
            QueryRunner runner = new QueryRunner();
            return runner.query(dbconn, sql, createListResultSetHandler());
        } catch (IOException e) {
            throw new SQLException("sql resource file load error", e);
        }
    }

    public static ProxyHistory getOne(Connection dbconn, final String logContext, final long messageRef)
            throws SQLException {
        try {
            final String sql =
                Resources
                    .toString(Resources.getResource("db/proxy_history_getone_join_sc.sql"), StandardCharsets.UTF_8);
            QueryRunner runner = new QueryRunner();
            return runner
                .query(dbconn, sql, createGetOneResultSetHandler(true), logContext, messageRef, logContext, messageRef);
        } catch (IOException e) {
            throw new SQLException("sql resource file load error", e);
        }
    }

    public static int updateCharset(Connection dbconn, final String logContext, final long messageRef,
            final String requestCharset, final String responseCharset) throws SQLException {
        String sql =
            "update proxy_history set request_charset = ?, response_charset = ? where log_context = ? and message_ref = ?";
        QueryRunner runner = new QueryRunner();
        return runner.update(
            dbconn,
            sql,
            getCharset(requestCharset).name(),
            getCharset(responseCharset).name(),
            logContext,
            messageRef);
    }

    public static int insert(
            // @formatter:off
            Connection dbconn,
            final String logContext,
            final long messageRef,
            final InetSocketAddress clientAddress,
            final InetSocketAddress remoteAddress,
            final String scheme,
            final FullHttpRequest httpRequest,
            final Timestamp atClientToProxy
            // @formatter:on
    ) throws SQLException {
        QueryRunner runner = new QueryRunner();
        List<String> columns =
            Arrays.asList(
                "log_context",
                "message_ref",
                "client_ip",
                "client_port",
                "remote_hostname",
                "remote_ip",
                "remote_port",
                "full_url",
                "request_scheme",
                "request_method",
                "request_url",
                "request_protocol_version",
                "request_host_header",
                "request_bytes",
                "at_client_to_proxy");
        String sql =
            "insert into proxy_history("
                + String.join(",", columns)
                + ") values ("
                + Strings.repeat("?,", columns.size() - 1)
                + "?)";
        String fullUrl = scheme + "://" + remoteAddress.getHostString();
        if (("http".equals(scheme) && 80 == remoteAddress.getPort())
            || ("https".equals(scheme) && 443 == remoteAddress.getPort())) {
            fullUrl = fullUrl + httpRequest.getUri();
        } else {
            fullUrl = fullUrl + ":" + remoteAddress.getPort() + httpRequest.getUri();
        }
        return runner.update(
            dbconn,
            sql,
            logContext,
            messageRef,
            clientAddress.getAddress().getHostAddress(),
            clientAddress.getPort(),
            remoteAddress.getHostString(),
            remoteAddress.getAddress().getHostAddress(),
            remoteAddress.getPort(),
            fullUrl,
            scheme,
            httpRequest.getMethod().toString(),
            httpRequest.getUri(),
            httpRequest.getProtocolVersion().text(),
            HttpHeaders.getHost(httpRequest, ""),
            MyNettyUtil.FullHttpRequestToBytes(httpRequest),
            atClientToProxy);
    }

    public static int update(
            // @formatter:off
            Connection dbconn,
            final String logContext,
            final long messageRef,
            final FullHttpResponse httpResponse,
            final Timestamp atClientToProxy,
            final Timestamp atProxyToServerConnectStart,
            final Timestamp atProxyToServerConnected,
            final Timestamp atProxyToServerRequestSent,
            final Timestamp atServerToProxyResponseReceived
            // @formatter:on
    ) throws SQLException {
        QueryRunner runner = new QueryRunner();
        String sql =
            "update proxy_history set "
                + "response_status_code = ?"
                + ", response_bytes = ?"
                + ", at_proxy_to_server_connect_start = ?"
                + ", at_proxy_to_server_connected = ?"
                + ", at_proxy_to_server_request_sent = ?"
                + ", at_server_to_proxy_response_recv = ?"
                + ", elapsed_time_connect = ?"
                + ", elapsed_time_req_sent = ?"
                + ", elapsed_time_res_recv = ?"
                + ", elapsed_time_full = ?"
                + " where log_context = ? and message_ref = ?";
        long et_connect = atProxyToServerConnected.getTime() - atProxyToServerConnectStart.getTime();
        long et_req_sent = atProxyToServerRequestSent.getTime() - atProxyToServerConnected.getTime();
        long et_res_recv = atServerToProxyResponseReceived.getTime() - atProxyToServerRequestSent.getTime();
        long et_full = atServerToProxyResponseReceived.getTime() - atClientToProxy.getTime();
        return runner.update(
            dbconn,
            sql,
            // @formatter:off
            httpResponse.getStatus().code(),
            MyNettyUtil.FullHttpResponseToBytes(httpResponse),
            atProxyToServerConnectStart,
            atProxyToServerConnected,
            atProxyToServerRequestSent,
            atServerToProxyResponseReceived,
            et_connect,
            et_req_sent,
            et_res_recv,
            et_full,
            logContext,
            messageRef
            // @formatter:on
        );
    }

    public static long attachScreenshot(
            // @formatter:off
            Connection dbconn,
            final String logContext,
            final long screenshotId,
            final String screenshotUrl,
            final String screenshotSrc,
            final Timestamp createdAt
            // @formatter:on
    ) throws SQLException {
        String sql =
            "select * from proxy_history where log_context = ? and full_url = ? order by at_client_to_proxy desc limit 1";
        QueryRunner runner = new QueryRunner();
        ProxyHistory ph0 = runner.query(dbconn, sql, createGetOneResultSetHandler(false), logContext, screenshotUrl);
        if (Objects.isNull(ph0)) {
            throw new IllegalArgumentException("no proxy history item matches to url[" + screenshotUrl + "]");
        }
        final int affectedRow =
            ScreenshotHistory
                .register(dbconn, logContext, ph0.messageRef, screenshotId, screenshotUrl, screenshotSrc, createdAt);
        if (affectedRow > 0) {
            return ph0.messageRef;
        }
        return 0L;
    }
}
