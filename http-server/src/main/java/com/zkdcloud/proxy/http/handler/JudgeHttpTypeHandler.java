package com.zkdcloud.proxy.http.handler;

import com.zkdcloud.proxy.http.context.ChannelContext;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * description
 *
 * @author zk
 * @since 2018/10/21
 */
public class JudgeHttpTypeHandler extends ChannelInboundHandlerAdapter {

    /**
     * static logger
     */
    private static Logger logger = LoggerFactory.getLogger(JudgeHttpTypeHandler.class);
    private Channel clientChannel;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        clientChannel = ctx.channel();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpObject) {
            if (msg instanceof HttpRequest) {
                HttpRequest httpMsg = (HttpRequest) msg;

                //set http proxy request type
                ChannelContext.PROTOCOL protocol = httpMsg.method() == HttpMethod.CONNECT ? ChannelContext.PROTOCOL.TUNNEL : ChannelContext.PROTOCOL.HTTP;
                clientChannel.attr(ChannelContext.HTTP_PROTOCOL).setIfAbsent(protocol);

                //set dstAddress
                InetSocketAddress dstAddress = getDstAddress(httpMsg);
                clientChannel.attr(ChannelContext.DST_ADDRESS).setIfAbsent(dstAddress);

                if(protocol == ChannelContext.PROTOCOL.TUNNEL){

                } else {

                }
            } else {
                logger.error("unsupport not httpRequest begin");
                closeChannel();
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }

    private InetSocketAddress getDstAddress(HttpRequest httpMsg) {
        String uri = httpMsg.uri();
        String host = uri.contains(":") ? uri.substring(0, uri.lastIndexOf(":")) : uri;
        int port = uri.contains(":") ? Integer.valueOf(uri.substring(uri.lastIndexOf(":"))) : 80;
        return new InetSocketAddress(host, port);
    }

    private void closeChannel() {
        clientChannel.close();
    }
}
