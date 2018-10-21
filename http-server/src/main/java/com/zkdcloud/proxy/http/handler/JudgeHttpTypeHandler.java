package com.zkdcloud.proxy.http.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;

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

    private PROTOCOL protocol;
    private boolean isFirst = true;
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof HttpObject){
            if(msg instanceof HttpRequest && isFirst){
                HttpRequest httpMsg = (HttpRequest) msg;
                protocol = httpMsg.method() == HttpMethod.CONNECT ? PROTOCOL.HTTPS : PROTOCOL.HTTP;
                isFirst = false;
            }
        } else {
            super.channelRead(ctx,msg);
        }
    }

    private void closeChannel(){

    }
}
