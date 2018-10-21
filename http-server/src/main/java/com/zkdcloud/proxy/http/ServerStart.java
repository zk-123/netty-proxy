package com.zkdcloud.proxy.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * http start
 *
 * @author zk
 * @since 2018/10/9
 */
public class ServerStart {
    /**
     * static logger
     */
    private static Logger logger = LoggerFactory.getLogger(ServerStart.class);
    private static NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private static NioEventLoopGroup workGroup = new NioEventLoopGroup(2);

    public static void main(String[] args) throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        serverBootstrap.group(bossGroup,workGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast("idle", new IdleStateHandler(0, 0, 7, TimeUnit.SECONDS) {
                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                        ctx.channel().close();
                                        if(logger.isDebugEnabled()){
                                            logger.debug("{} will be close : {}", ctx.channel().id(),cause.getMessage());
                                        }
                                    }

                                    @Override
                                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                        if(evt instanceof IdleStateEvent){
                                            ctx.channel().close();

                                            if(logger.isDebugEnabled()){
                                                logger.debug("{} will be close", ctx.channel().id());
                                            }
                                        }
                                    }
                                }).addFirst("http-decoder", new HttpRequestDecoder());
                    }
                });
        int port = args != null && args.length >= 1 ? Integer.parseInt(args[0]) : 1081;
        logger.info("socks5 server start at : : {}", port);

        serverBootstrap.bind(port).sync().channel().closeFuture().sync();
    }
}
