package com.zkdcloud.proxy.socks5.handler;

import com.zkdcloud.proxy.socks5.util.SocksServerUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.*;

import static com.zkdcloud.proxy.socks5.ServerStart.serverConfigure;

/**
 * add all kinds of channel
 *
 * @author zk
 * @since 2019/11/29
 */
public class Socks5ServerHandler extends SimpleChannelInboundHandler<SocksMessage> {

    @Override
    public void channelRead0(ChannelHandlerContext ctx, SocksMessage socksRequest) throws Exception {
        switch (socksRequest.version()) {
            case SOCKS5:
                if (socksRequest instanceof Socks5InitialRequest) {
                    // auth
                    if (enableAuth()) {
                        ctx.pipeline().addAfter("client-idle", "client-auth-decoder", new Socks5PasswordAuthRequestDecoder());
                        ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.PASSWORD));
                    } else {
                        ctx.pipeline().addAfter("client-idle", "client-socks5-command-decoder", new Socks5CommandRequestDecoder());
                        ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
                    }
                } else if (socksRequest instanceof Socks5PasswordAuthRequest) {
                    Socks5PasswordAuthRequest passwordAuthRequest = (Socks5PasswordAuthRequest) socksRequest;
                    if (passwordAuthRequest.username().equals(serverConfigure.getAuthUsername()) &&
                            passwordAuthRequest.password().equals(serverConfigure.getAuthPassword())) {
                        ctx.pipeline().remove(Socks5PasswordAuthRequestDecoder.class);
                        ctx.pipeline().addAfter("client-idle", "client-socks5-command-decoder", new Socks5CommandRequestDecoder());
                        ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
                    } else {
                        ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE));
                        SocksServerUtils.closeOnFlush(ctx.channel());
                    }
                } else if (socksRequest instanceof Socks5CommandRequest) {
                    Socks5CommandRequest socks5CmdRequest = (Socks5CommandRequest) socksRequest;
                    if (socks5CmdRequest.type() == Socks5CommandType.CONNECT) {
                        ctx.pipeline().addLast(new SocksServerConnectHandler());
                        ctx.pipeline().remove(this);
                        ctx.fireChannelRead(socksRequest);
                    } else {
                        ctx.close();
                    }
                } else {
                    ctx.close();
                }
                break;
            case SOCKS4a:
            case UNKNOWN:
                ctx.close();
                break;
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
        SocksServerUtils.closeOnFlush(ctx.channel());
    }

    private boolean enableAuth() {
        return serverConfigure.getAuthUsername() != null && !"".equals(serverConfigure.getAuthUsername()) &&
                serverConfigure.getAuthPassword() != null && !"".equals(serverConfigure.getAuthPassword());
    }
}
