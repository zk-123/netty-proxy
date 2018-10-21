package com.zkdcloud.proxy.socks5.handler;

import com.zkdcloud.proxy.socks5.context.ChannelContextConst;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * transfer flow to remote channel
 *
 * @author zk
 * @since 2018/10/9
 */
public class TransferFlowHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private Channel proxyChannel;
    /**
     * static logger
     */
     private static Logger logger = LoggerFactory.getLogger(TransferFlowHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        if(proxyChannel == null){
            proxyChannel = ctx.channel().attr(ChannelContextConst.PROXY_CHANNEL).get();
        }

        if(proxyChannel == null){
            ctx.channel().close();
            logger.error("can't find proxy channel");
            return;
        }
        proxyChannel.writeAndFlush(msg.retain());
    }
}
