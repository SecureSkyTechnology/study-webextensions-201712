package com.secureskytech.scdemosrv.proxy;

import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class MyNettyUtil {
    /** 
     * See Effective-Java 2nd, Item 4 / Checkstyle:HideUtilityClassConstructor
     */
    private MyNettyUtil() {
        throw new AssertionError("Java Rationale: Utility class prohibits Instantiation.");
    }

    private final static int MAX_INITIAL_LINE_LENGTH = 10 * 1024 * 1024;
    private final static int MAX_HEADER_SIZE = 10 * 1024 * 1024;
    private final static int MAX_CHUNK_SIZE = 10 * 1024 * 1024;
    private final static int MAX_CONTENT_LENGTH = 10 * 1024 * 1024;

    /* FullHttpRequest/Responseとbyte[]を相互変換するユーティリティメソッド。
     * 
     * どうもNetty同梱のhttp codecだと、HTTPのJava表現(FullHttpRequest, FullHttpResponse)で以下のような自動調整が入る。
     * - GETでContent-Length: 0がJava表現になると自動でくっつく。
     * - 複数行ヘッダがJava表現になると自動で連結さえる。
     * - ヘッダー末尾の空白がJava表現になるとtrimされる。
     * 
     * HTTPリクエスト/レスポンスの生データに触る仕組みはLittleProxy自体には仕込まれていない。
     * そのため、EmbeddecChannelを使って手動で FullHttpRequest/Response を byte[] に変換しようとすると
     * 上記の調整が入ったデータとなり、本当に送られたリクエスト/レスポンスとは異なったものになってしまう。
     * 
     * →これの解決ヒントとして、以下のサンプルコードでchannelのpipelineに実際の生byte[]を記録するサンプルが仕込まれている。
     * https://github.com/anjackson/warc-proxy
     * 将来的にはこのような技法を組み合わせる必要が出てくるかもしれない。
     */

    public static FullHttpRequest BytesToFullHttpRequest(byte[] src) {
        HttpRequestDecoder dec =
            new HttpRequestDecoder(MAX_INITIAL_LINE_LENGTH, MAX_HEADER_SIZE, MAX_CHUNK_SIZE, false);
        HttpObjectAggregator aggr = new HttpObjectAggregator(MAX_CONTENT_LENGTH);
        EmbeddedChannel ch = new EmbeddedChannel(dec, aggr);
        ch.writeInbound(Unpooled.copiedBuffer(src));
        ch.finish();
        FullHttpRequest fhr = (FullHttpRequest) ch.readInbound();
        ch.close();
        return fhr;
    }

    /**
     * FullHttpRequestをbyte[]に変換する。
     * 2017-12-18時点では「これぞ正解」という解法が見つからなかった。
     * (depreFullHttpRequestToBytes() 参照)
     * そのため、やむなく、FullHttpRequestから取り出せるパースされた内容を
     * 手動で組み立てなおしている。
     * よって、元のHTTPレスポンスとは形が変わっている可能性がある点に注意。
     * ヘッダーの順番は維持されているはずだが、複数行ヘッダーは元より、
     * ヘッダー中にmultibyteを使用している場合に文字化けしてしまう可能性がある。
     * 
     * @param fhr
     * @return
     */
    public static byte[] FullHttpRequestToBytes(FullHttpRequest fhr) {
        StringBuilder headersb = new StringBuilder(8 * 1024);
        headersb.append(fhr.getMethod().name());
        headersb.append(" ");
        headersb.append(fhr.getUri());
        headersb.append(" ");
        headersb.append(fhr.getProtocolVersion().text());
        headersb.append("\r\n");
        HttpHeaders headers = fhr.headers();
        for (Entry<String, String> header : headers) {
            headersb.append(header.getKey());
            headersb.append(": ");
            headersb.append(header.getValue());
            headersb.append("\r\n");
        }
        headersb.append("\r\n");
        ByteBuf bb = Unpooled.buffer();
        bb.writeBytes(headersb.toString().getBytes(StandardCharsets.UTF_8));

        ByteBuf body = fhr.content();
        byte[] bodyBytes = new byte[body.readableBytes()];
        body.readBytes(bodyBytes);
        body.release();

        bb.writeBytes(bodyBytes);
        byte[] finalBytes = new byte[bb.readableBytes()];
        bb.readBytes(finalBytes);
        return finalBytes;
    }

    /**
     * FullHttpRequestをbyte[]に変換するメソッドの非推奨版。
     * FullHttpRequestのcontentがCompositeByteBufで2つ以上のコンポーネントで構成されていた場合
     * (= HttpObjectAggregatorが生成するデフォルトのFullHttpRequest実装クラス)、
     * ch.writeOutbound()しただけでは2個目以降のcomponentが中でwriteされないらしく、
     * 例えばレスポンスボディが欠落してしまう。
     * 
     * 2017-12-18時点で相当暗中模索で解決策が見えない状況であるため、本アプローチは一旦放棄する。
     * 
     * @deprecated
     * @param fhr
     * @return
     */
    @Deprecated
    public static byte[] depreFullHttpRequestToBytes(FullHttpRequest fhr) {
        HttpRequestEncoder enc = new HttpRequestEncoder();
        HttpObjectAggregator aggr = new HttpObjectAggregator(MAX_CONTENT_LENGTH);
        EmbeddedChannel ch = new EmbeddedChannel(enc, aggr);
        ch.writeOutbound(fhr);
        ch.flush();
        ByteBuf encoded = (ByteBuf) ch.readOutbound();
        byte[] bytes = new byte[encoded.readableBytes()];
        encoded.readBytes(bytes);
        encoded.release();
        ch.close();
        return bytes;
    }

    public static FullHttpResponse BytesToFullHttpResponse(byte[] src) {
        HttpResponseDecoder dec =
            new HttpResponseDecoder(MAX_INITIAL_LINE_LENGTH, MAX_HEADER_SIZE, MAX_CHUNK_SIZE, false);
        HttpObjectAggregator aggr = new HttpObjectAggregator(MAX_CONTENT_LENGTH);
        EmbeddedChannel ch = new EmbeddedChannel(dec, aggr);
        ch.writeInbound(Unpooled.copiedBuffer(src));
        ch.finish();
        FullHttpResponse fhr = (FullHttpResponse) ch.readInbound();
        ch.close();
        return fhr;
    }

    /**
     * FullHttpResponseをbyte[]に変換する。
     * 2017-12-18時点では「これぞ正解」という解法が見つからなかった。
     * (depreFullHttpResponseToBytes() 参照)
     * そのため、やむなく、FullHttpResponseから取り出せるパースされた内容を
     * 手動で組み立てなおしている。
     * よって、元のHTTPレスポンスとは形が変わっている可能性がある点に注意。
     * ヘッダーの順番は維持されているはずだが、複数行ヘッダーは元より、
     * ヘッダー中にmultibyteを使用している場合に文字化けしてしまう可能性がある。
     * 
     * @param fhr
     * @return
     */
    public static byte[] FullHttpResponseToBytes(FullHttpResponse fhr) {
        StringBuilder headersb = new StringBuilder(8 * 1024);
        headersb.append(fhr.getProtocolVersion().text());
        headersb.append(" ");
        headersb.append(fhr.getStatus().code());
        headersb.append(" ");
        headersb.append(fhr.getStatus().reasonPhrase());
        headersb.append("\r\n");
        HttpHeaders headers = fhr.headers();
        for (Entry<String, String> header : headers) {
            headersb.append(header.getKey());
            headersb.append(": ");
            headersb.append(header.getValue());
            headersb.append("\r\n");
        }
        headersb.append("\r\n");
        ByteBuf bb = Unpooled.buffer();
        bb.writeBytes(headersb.toString().getBytes(StandardCharsets.UTF_8));

        ByteBuf body = fhr.content();
        byte[] bodyBytes = new byte[body.readableBytes()];
        body.readBytes(bodyBytes);
        body.release();

        bb.writeBytes(bodyBytes);
        byte[] finalBytes = new byte[bb.readableBytes()];
        bb.readBytes(finalBytes);
        return finalBytes;
    }

    /**
     * FullHttpResponseをbyte[]に変換するメソッドの非推奨版。
     * FullHttpResponseのcontentがCompositeByteBufで2つ以上のコンポーネントで構成されていた場合
     * (= HttpObjectAggregatorが生成するデフォルトのFullHttpResponse実装クラス)、
     * ch.writeOutbound()しただけでは2個目以降のcomponentが中でwriteされないらしく、
     * 例えばレスポンスボディが欠落してしまう。
     * 
     * 2017-12-18時点で相当暗中模索で解決策が見えない状況であるため、本アプローチは一旦放棄する。
     * 
     * @deprecated
     * @param fhr
     * @return
     */
    @Deprecated
    public static byte[] depreFullHttpResponseToBytes(FullHttpResponse fhr) {
        HttpResponseEncoder enc = new HttpResponseEncoder();
        HttpObjectAggregator aggr = new HttpObjectAggregator(MAX_CONTENT_LENGTH);
        EmbeddedChannel ch = new EmbeddedChannel(enc, aggr);
        ch.writeOutbound(fhr);
        ch.flush();
        ByteBuf encoded = (ByteBuf) ch.readOutbound();
        byte[] bytes = new byte[encoded.readableBytes()];
        encoded.readBytes(bytes);
        encoded.release();
        ch.close();
        return bytes;
    }
}
