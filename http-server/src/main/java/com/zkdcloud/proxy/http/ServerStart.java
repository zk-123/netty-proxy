package com.zkdcloud.proxy.http;

import com.zkdcloud.proxy.http.handler.JudgeHttpTypeHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
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
    private static NioEventLoopGroup bossGroup = new NioEventLoopGroup();
    private static NioEventLoopGroup workGroup = new NioEventLoopGroup(Math.min(Runtime.getRuntime().availableProcessors() + 1, 32));

    public static void main(String[] args) throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        serverBootstrap.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast("idle", new IdleStateHandler(0, 0, 7, TimeUnit.HOURS))
                                .addFirst("http-decoder", new HttpRequestDecoder())
                                .addLast("http-encoder", new HttpResponseEncoder())
                                .addLast("http-init", new JudgeHttpTypeHandler());
                    }
                });
        int port = args != null && args.length >= 1 ? Integer.parseInt(args[0]) : 1081;
        logger.info("socks5 server start at : : {}", port);

        serverBootstrap.bind(port).sync().channel().closeFuture().sync();
    }
}
