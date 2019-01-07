package com.zkdcloud.proxy.http.handler;

import com.zkdcloud.proxy.http.context.ChannelContext;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * judge http type and finish handshake
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
                clientChannel.attr(ChannelContext.TRANSPORT_PROTOCOL).setIfAbsent(protocol);

                //set dstAddress
                InetSocketAddress dstAddress = getDstAddress(httpMsg);
                clientChannel.attr(ChannelContext.DST_ADDRESS).setIfAbsent(dstAddress);

                clientChannel.pipeline().addAfter(ctx.name(), "data-transport", new TransportHandler());
                ctx.fireChannelRead(msg);
            } else {
                ctx.fireChannelRead(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private InetSocketAddress getDstAddress(HttpRequest httpMsg) {
        InetSocketAddress result = null;

        String uri = httpMsg.uri();
        if(uri.startsWith("http://") || uri.startsWith("https://")){
            try {
                URL url = new URL(uri);
                result = new InetSocketAddress(url.getHost(),url.getPort() == -1 ? 80 : url.getPort());
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(httpMsg.uri() + " is getDstAddress fail");
            }
        } else {
            String host = uri.contains(":") ? uri.substring(0, uri.lastIndexOf(":")) : uri;
            int port = uri.contains(":") ? Integer.valueOf(uri.substring(uri.lastIndexOf(":") + 1)) : 80;
            return new InetSocketAddress(host, port);
        }

        return result;
    }

    private void closeChannel() {
        clientChannel.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();
        if (logger.isDebugEnabled()) {
            logger.debug("{} will be close : {}", ctx.channel().id(), cause.getMessage());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.channel().close();

            if (logger.isDebugEnabled()) {
                logger.debug("{} will be close", ctx.channel().id());
            }
        }
    }
}
