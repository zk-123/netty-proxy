package com.zkdcloud.proxy.socks5.context;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * contextConst
 *
 * @author zk
 * @since 2018/10/9
 */
public class ChannelContextConst {
    /**
     * serverConfig
     */
    public static AttributeKey<Channel> REMOTE_CHANNEL = AttributeKey.valueOf("remoteChannel");
    /**
     * clientConfig
     */
    public static AttributeKey<Channel> CLIENT_CHANNEL = AttributeKey.valueOf("clientChannel");
}
