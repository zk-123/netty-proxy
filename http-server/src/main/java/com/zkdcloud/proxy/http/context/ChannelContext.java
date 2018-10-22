package com.zkdcloud.proxy.http.context;

import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;

public class ChannelContext {

    public enum PROTOCOL {
        HTTP,
        TUNNEL
    }
    public static AttributeKey<PROTOCOL> TRANSPORT_PROTOCOL = AttributeKey.valueOf("TRANSPORT_PROTOCOL");
    public static AttributeKey<InetSocketAddress> DST_ADDRESS = AttributeKey.valueOf("DST_ADDRESS");
}
