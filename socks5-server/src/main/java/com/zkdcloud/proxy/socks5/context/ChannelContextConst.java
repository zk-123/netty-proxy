package com.zkdcloud.proxy.socks5.context;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;

/**
 * contextConst
 *
 * @author zk
 * @since 2018/10/9
 */
public class ChannelContextConst {
    /**
     * dst address
     */
    public static AttributeKey<InetSocketAddress> DST_ADDRESS = AttributeKey.valueOf("dstAddress");
    /**
     * proxy channel
     */
    public static AttributeKey<Channel> PROXY_CHANNEL = AttributeKey.valueOf("proxyChannel");
}
