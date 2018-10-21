package com.zkdcloud.proxy.http.util;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

public class ChannelUtil {
    public static ChannelFuture connect(InetSocketAddress dstAddress, final ChannelHandler... channelHandlers){
        Bootstrap bootstrap = new Bootstrap();
        return bootstrap.group(new NioEventLoopGroup(1))
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    protected void initChannel(Channel ch) throws Exception {
                        for (int i = 0; i < channelHandlers.length; i++) {
                            ch.pipeline().addLast(channelHandlers[i]);
                        }
                    }
                }).connect(dstAddress);

    }
}
