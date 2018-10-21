package com.zkdcloud.proxy.http.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpObject;

/**
 * description
 *
 * @author zk
 * @since 2018/10/21
 */
public class JudgeHttpTypeHandler  extends ChannelInboundHandlerAdapter {
    enum PROTOCOL {
        HTTP,
        HTTPS
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof HttpObject){
            if()
        } else {
            super.channelRead(ctx,msg);
        }
    }

    private void closeChannel(){

    }
}
