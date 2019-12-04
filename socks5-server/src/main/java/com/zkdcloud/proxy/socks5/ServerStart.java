package com.zkdcloud.proxy.socks5;

import com.zkdcloud.proxy.socks5.config.ServerConfigure;
import com.zkdcloud.proxy.socks5.handler.ExceptionDuplexHandler;
import com.zkdcloud.proxy.socks5.handler.Socks5ServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * socks5 start
 *
 * @author zk
 * @since 2018/10/9
 */
public class ServerStart {
    public static NioEventLoopGroup bossGroup;
    public static NioEventLoopGroup workGroup;
    public static ServerConfigure serverConfigure = new ServerConfigure();
    /**
     * static logger
     */
    private static Logger logger = LoggerFactory.getLogger(ServerStart.class);
    private static Options OPTIONS = new Options();
    private static CommandLine commandLine;
    private static String HELP_STRING = null;

    public static void main(String[] args) throws Exception {
        // init args
        initCliArgs(args);
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        bossGroup = new NioEventLoopGroup(serverConfigure.getNumberBoss());
        workGroup = new NioEventLoopGroup(serverConfigure.getNumberWorkers(),
                new DefaultThreadFactory("proxy-workers"));

        serverBootstrap.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addFirst("client-idle", new IdleStateHandler(0, 0, serverConfigure.getSecondsClientIdle(), TimeUnit.SECONDS))
                                .addLast("client-socks5-handshake", new SocksPortUnificationServerHandler())
                                .addLast("client-socks5-server", new Socks5ServerHandler())
                                .addLast("client-exception-handler", new ExceptionDuplexHandler());
                    }
                });
        int port = serverConfigure.getPort();
        logger.info("socks5 server start at : : {}", port);

        serverBootstrap.bind(port).sync().channel().closeFuture().sync();
    }

    /**
     * init args
     *
     * @param args args
     */
    private static void initCliArgs(String[] args) throws IOException {
        // validate args
        {
            CommandLineParser commandLineParser = new DefaultParser();
            // help
            OPTIONS.addOption("h", "usage help");
            // port
            OPTIONS.addOption(Option.builder("p").hasArg(true).longOpt("port").type(Short.TYPE).desc("the port of server startup").build());
            // auth
            OPTIONS.addOption(Option.builder("a").hasArg(true).longOpt("auth").type(String.class).desc("auth like 'username:password' or 's5://base64(username:password)'").build());
            // client idle seconds
            OPTIONS.addOption(Option.builder("c").hasArg(true).longOpt("seconds_client_idle").type(Long.TYPE).desc("the seconds of client idle").build());
            // remote idle seconds
            OPTIONS.addOption(Option.builder("r").hasArg(true).longOpt("seconds_remote_idle").type(Long.TYPE).desc("the seconds of remote idle").build());
            // number fo workers thread
            OPTIONS.addOption(Option.builder("bn").hasArg(true).longOpt("number_boss").type(Short.TYPE).desc("the number of boss thread").build());
            // number fo workers thread
            OPTIONS.addOption(Option.builder("wn").hasArg(true).longOpt("number_workers").type(Short.TYPE).desc("the number of workers thread").build());
            try {
                commandLine = commandLineParser.parse(OPTIONS, args);
            } catch (ParseException e) {
                logger.error("{}\n{}", e.getMessage(), getHelpString());
                System.exit(0);
            }
        }

        // init serverConfigure
        {
            if (commandLine.hasOption("h")) {
                logger.info("\n" + getHelpString());
                System.exit(1);
            }
            // server port
            String portOptionValue = commandLine.getOptionValue("p");
            short port = portOptionValue == null || "".equals(portOptionValue) ? 1081 : Short.parseShort(portOptionValue);
            serverConfigure.setPort(port);

            // client boss thread number
            String numberBossOptionValue = commandLine.getOptionValue("bn");
            short numberBoss = numberBossOptionValue == null || "".equals(numberBossOptionValue) ? 1 : Short.parseShort(numberBossOptionValue);
            serverConfigure.setNumberBoss(numberBoss);

            // client workers thread number
            String numberWorksOptionValue = commandLine.getOptionValue("wn");
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

            //auth
            String authString = commandLine.getOptionValue("a");
            if (authString != null && !"".equals(authString)) {
                if (authString.startsWith("s5://")) {
                    authString = new String(new BASE64Decoder().decodeBuffer(authString.substring(5)), StandardCharsets.UTF_8);
                }
                if (authString.indexOf(':') != -1) {
                    serverConfigure.setAuthUsername(authString.substring(0, authString.indexOf(':')));
                    serverConfigure.setAuthPassword(authString.substring(authString.indexOf(':') + 1));
                }
            }
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
            helpFormatter.setOptionComparator(null);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PrintWriter printWriter = new PrintWriter(byteArrayOutputStream);
            helpFormatter.printHelp(printWriter, HelpFormatter.DEFAULT_WIDTH * 2, "./socks5-server.jar -h", null,
                    OPTIONS, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, null);
            printWriter.flush();
            HELP_STRING = new String(byteArrayOutputStream.toByteArray());
            printWriter.close();
        }
        return HELP_STRING;
    }
}
