package com.zkdcloud.proxy.http.handler;

import com.zkdcloud.proxy.http.context.ChannelContext;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class TransportHandler extends ChannelInboundHandlerAdapter {
    /**
     * static logger
     */
     private static Logger logger = LoggerFactory.getLogger(TransportHandler.class);

    private Channel clientChannel;
    private Channel remoteChannel;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.clientChannel = ctx.channel();

        //build connect
        InetSocketAddress dstAddress = clientChannel.attr(ChannelContext.DST_ADDRESS).get();
        if(dstAddress == null){
            closeChannel();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

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
}
