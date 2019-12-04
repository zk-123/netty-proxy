package com.zkdcloud.proxy.socks5.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.zkdcloud.proxy.socks5.context.ChannelContextConst.CLIENT_CHANNEL;
import static com.zkdcloud.proxy.socks5.context.ChannelContextConst.REMOTE_CHANNEL;


/**
 * channel of tail
 *
 * @author zk
 * @since 2019/7/9
 */
public class ExceptionDuplexHandler extends ChannelDuplexHandler {
    /**
     * static logger
     */
    private static Logger logger = LoggerFactory.getLogger(ExceptionDuplexHandler.class);

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().attr(REMOTE_CHANNEL).get() != null && ctx.channel().attr(REMOTE_CHANNEL).get().isActive()) {
            ctx.channel().attr(REMOTE_CHANNEL).get().close();
        } else if (ctx.channel().attr(CLIENT_CHANNEL).get() != null && ctx.channel().attr(CLIENT_CHANNEL).get().isActive()) {
            ctx.channel().attr(CLIENT_CHANNEL).get().close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.channel().close();
        if (ctx.channel().attr(REMOTE_CHANNEL).get() != null) {
            logger.error(String.format("client happen error, will be close : %s", cause.getMessage()), cause);
            ctx.channel().attr(REMOTE_CHANNEL).get().close();
        } else if (ctx.channel().attr(CLIENT_CHANNEL).get() != null) {
            logger.error(String.format("remote happen error, will be close : %s", cause.getMessage()), cause);
            ctx.channel().attr(CLIENT_CHANNEL).get().close();
        } else {
            logger.error(String.format("unknown happen error, will be close : %s", cause.getMessage()), cause);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            ctx.channel().close();
            if (ctx.channel().attr(REMOTE_CHANNEL).get() != null) {
                logger.warn("client idle timeout, will be close");
                ctx.channel().attr(REMOTE_CHANNEL).get().close();
            } else if (ctx.channel().attr(CLIENT_CHANNEL).get() != null) {
                logger.error("remote idle timeout, will be close");
                ctx.channel().attr(CLIENT_CHANNEL).get().close();
            } else {
                logger.error("unknown idle timeout, will be close");
            }
        }
    }
}
