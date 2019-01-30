package com.zkdcloud.proxy.http;

import com.zkdcloud.proxy.http.handler.JudgeHttpTypeHandler;
import com.zkdcloud.proxy.http.config.ServerConfigure;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
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
    private static NioEventLoopGroup bossGroup;
    private static NioEventLoopGroup workGroup;

    public static void main(String[] args) throws InterruptedException {
        // init args
        initCliArgs(args);
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        bossGroup = new NioEventLoopGroup(1);
        workGroup = new NioEventLoopGroup(serverConfigure.getNumberWorkers(),
                new DefaultThreadFactory("proxy-workers"));


        serverBootstrap.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addFirst("idle", new IdleStateHandler(0, 0, serverConfigure.getSecondsClientIdle(), TimeUnit.SECONDS) {
                                    private Logger logger = LoggerFactory.getLogger("client idle logger");

                                    @Override
                                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                        if (evt instanceof IdleStateEvent) {
                                            ctx.channel().close();
                                            this.logger.warn("{} idle timeout, will be close", ctx.channel().id());
                                        }
                                    }
                                })
                                .addFirst("http-decoder", new HttpRequestDecoder())
                                .addLast("http-encoder", new HttpResponseEncoder())
                                .addLast("http-init", new JudgeHttpTypeHandler());
                    }
                });
        short port = serverConfigure.getPort();
        logger.info("http server start at : : {}", port);

        serverBootstrap.bind(port).sync().channel().closeFuture().sync();
    }

    public static ServerConfigure serverConfigure = new ServerConfigure();
    private static Options OPTIONS = new Options();
    private static CommandLine commandLine;
    private static String HELP_STRING = null;

    /**
     * init args
     *
     * @param args args
     */
    private static void initCliArgs(String[] args) {
        // validate args
        {
            CommandLineParser commandLineParser = new DefaultParser();
            // help
            OPTIONS.addOption("help","usage help");
            // port
            OPTIONS.addOption(Option.builder("p").hasArg(true).longOpt("port").type(Short.TYPE).desc("the port of server startup").build());
            // number fo workers thread
            OPTIONS.addOption(Option.builder("n").hasArg(true).longOpt("number_workers").type(Short.TYPE).desc("the number of workers thread").build());
            // client idle seconds
            OPTIONS.addOption(Option.builder("c").hasArg(true).longOpt("seconds_client_idle").type(Long.TYPE).desc("the seconds of client idle").build());
            // remote idle seconds
            OPTIONS.addOption(Option.builder("r").hasArg(true).longOpt("seconds_remote_idle").type(Long.TYPE).desc("the seconds of remote idle").build());
            try {
                commandLine = commandLineParser.parse(OPTIONS, args);
            } catch (ParseException e) {
                logger.error("{}\n{}", e.getMessage(), getHelpString());
                System.exit(0);
            }
        }

        // init serverConfigure
        {
            if(commandLine.hasOption("help")){
                logger.info("\n" + getHelpString());
                System.exit(1);
            }
            // server port
            String portOptionValue = commandLine.getOptionValue("p");
            short port = portOptionValue == null || "".equals(portOptionValue) ? 1081 : Short.parseShort(portOptionValue);
            serverConfigure.setPort(port);

            // netty workers thread number(client)
            String numberWorksOptionValue = commandLine.getOptionValue("n");
            short numberWorks = numberWorksOptionValue == null || "".equals(numberWorksOptionValue) ?
                    (short) Math.min(Runtime.getRuntime().availableProcessors() + 1, 32) : Short.parseShort(numberWorksOptionValue);
            serverConfigure.setNumberWorkers(numberWorks);

            // client idle seconds, default is 1 min
            String scOptionValue = commandLine.getOptionValue("c");
            short secondsClientIdle = scOptionValue == null || "".equals(scOptionValue) ? 60 : Short.parseShort(scOptionValue);
            serverConfigure.setSecondsClientIdle(secondsClientIdle);

            // remote idle seconds, default is 1min
            String srOptionValue = commandLine.getOptionValue("r");
            short secondsRemoteIdle = srOptionValue == null || "".equals(srOptionValue) ? 60 : Short.parseShort(srOptionValue);
            serverConfigure.setSecondsRemoteIdle(secondsRemoteIdle);
        }

    }

    /**
     * get string of help usage
     *
     * @return help string
     */
    private static String getHelpString() {
        if (HELP_STRING == null) {
            HelpFormatter helpFormatter = new HelpFormatter();

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PrintWriter printWriter = new PrintWriter(byteArrayOutputStream);
            helpFormatter.printHelp(printWriter, HelpFormatter.DEFAULT_WIDTH, "http server proxy", null,
                    OPTIONS, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, null);
            printWriter.flush();
            HELP_STRING = new String(byteArrayOutputStream.toByteArray());
            printWriter.close();
        }
        return HELP_STRING;
    }
}
