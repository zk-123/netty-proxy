package com.zkdcloud.proxy.socks5;

import com.zkdcloud.proxy.socks5.handler.Socks5ServerDoorHandler;
import com.zkdcloud.proxy.socks5.handler.Socks5ServerEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * socks5 start
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
    private static NioEventLoopGroup workGroup = new NioEventLoopGroup(Math.min(Runtime.getRuntime().availableProcessors() + 1, 32),
            new DefaultThreadFactory("proxy-workers"));

    public static void main(String[] args) throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        serverBootstrap.group(bossGroup,workGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addFirst("idle", new IdleStateHandler(0, 0, 7, TimeUnit.MINUTES){
                                    private Logger logger = LoggerFactory.getLogger("client idle logger");
                                    @Override
                                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                        if (evt instanceof IdleStateEvent) {
                                            ctx.channel().close();
                                            this.logger.warn("{} idle timeout, will be close", ctx.channel().id());
                                        }
                                    }
                                })
                                .addLast("socks5-init", new Socks5InitialRequestDecoder())
                                .addLast("socks5-door", new Socks5ServerDoorHandler())
                                .addLast(new Socks5ServerEncoder());
                    }
                });
        int port = args != null && args.length >= 1 ? Integer.parseInt(args[0]) : 1081;
        logger.info("socks5 server start at : : {}", port);

        serverBootstrap.bind(port).sync().channel().closeFuture().sync();
    }
}
