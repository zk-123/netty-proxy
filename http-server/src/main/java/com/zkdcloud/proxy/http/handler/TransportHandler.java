package com.zkdcloud.proxy.http.handler;

import com.zkdcloud.proxy.http.context.ChannelContext;
import com.zkdcloud.proxy.http.util.ChannelUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class TransportHandler extends ChannelInboundHandlerAdapter {
    /**
     * static logger
     */
    private static Logger logger = LoggerFactory.getLogger(TransportHandler.class);
    private static EventLoopGroup connectExecutors = new NioEventLoopGroup(Math.min(Runtime.getRuntime().availableProcessors() + 1, 32),
            new DefaultThreadFactory("connect-executors"));

    private Channel clientChannel;
    private Channel remoteChannel;
    private List<HttpObject> cumulation;

    private ChannelInboundHandlerAdapter DEFAULT_DATA_CHANGE = new ChannelInboundHandlerAdapter() {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            clientChannel.writeAndFlush(msg);
        }
    };

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.clientChannel = ctx.channel();

        //build connect
        final InetSocketAddress dstAddress = clientChannel.attr(ChannelContext.DST_ADDRESS).get();
        if (dstAddress == null) {
            logger.error("not exist dstAddress.{} will be close", clientChannel.id());
            closeChannel();
        }

        final ChannelContext.PROTOCOL protocol = clientChannel.attr(ChannelContext.TRANSPORT_PROTOCOL).get();
        final ChannelFuture channelFuture;
        switch (protocol) {
            case HTTP:
                channelFuture = ChannelUtil.connect(dstAddress, connectExecutors, new HttpRequestEncoder(), DEFAULT_DATA_CHANGE);
                channelFuture.addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            remoteChannel = channelFuture.channel();
                            if(clientChannel.pipeline().get(HttpResponseEncoder.class) != null){
                                clientChannel.pipeline().remove(HttpResponseEncoder.class);
                            }

                            //send cumulation httpObject
                            consumeAllObject();
                            logger.info("type: http, channel {} is connect success. {}:{}", clientChannel.id(), dstAddress.getHostName(), dstAddress.getPort());
                        } else {
                            logger.error("{} fail connect: {}. {}:{}", clientChannel.id(), future.cause().getMessage(), dstAddress.getHostName(), dstAddress.getPort());
                            closeChannel();
                        }
                    }
                });
                break;
            case TUNNEL:
                channelFuture = ChannelUtil.connect(dstAddress, connectExecutors, DEFAULT_DATA_CHANGE);
                channelFuture.addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            remoteChannel = channelFuture.channel();

                            //response ack
                            clientChannel.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
                                    .addListener(new ChannelFutureListener() {
                                        public void operationComplete(ChannelFuture future) throws Exception {
                                            clientChannel.pipeline().remove(HttpRequestDecoder.class);
                                            clientChannel.pipeline().remove(HttpResponseEncoder.class);
                                        }
                                    });
                            logger.info("type: tunnel, channel {} is connect success. {}:{}", clientChannel.id(), dstAddress.getHostName(), dstAddress.getPort());
                        } else {
                            logger.error("{} fail connect: {}. {}:{}", clientChannel.id(), future.cause().getMessage(), dstAddress.getHostName(), dstAddress.getPort());
                            closeChannel();
                        }
                    }
                });
                break;
            default:
                throw new IllegalArgumentException("protocol type is undefined " + protocol);
        }

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            remoteChannel.writeAndFlush(msg);
        } else if (msg instanceof HttpObject) {
            if (msg instanceof HttpRequest && ((HttpRequest) msg).method() == HttpMethod.CONNECT) {
                return;
            }
            if (remoteChannel == null) {
                cumulate((HttpObject)msg);
            } else if(msg != LastHttpContent.EMPTY_LAST_CONTENT){
                remoteChannel.writeAndFlush(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    public void cumulate(HttpObject httpObject) {
        if (cumulation == null) {
            cumulation = new ArrayList<HttpObject>();
        }
        cumulation.add(httpObject);
    }

    /**
     * close both channel
     */
    private void closeChannel() {
        if (clientChannel != null) {
            clientChannel.close();
        }
        if (remoteChannel != null) {
            remoteChannel.close();
        }
    }

    private void consumeAllObject(){
        try {
            if(remoteChannel == null){
                closeChannel();
                logger.error("remote channel is null when consume httpObject");
            } else {
                for (HttpObject httpObject : cumulation) {
                    remoteChannel.writeAndFlush(httpObject);
                }
            }
        } finally {
            if(cumulation != null){
                cumulation.clear();
                cumulation = null;
            }
        }

    }
}
