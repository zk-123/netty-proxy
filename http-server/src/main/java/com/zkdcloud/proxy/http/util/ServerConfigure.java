package com.zkdcloud.proxy.http.util;

/**
 * server config
 *
 * @author zk
 * @since 2019/1/30
 */
public class ServerConfigure {
    private short port;
    private short numberWorkers;
    private long secondsClientIdle;
    private long secondsRemoteIdle;

    public short getPort() {
        return port;
    }

    public void setPort(short port) {
        this.port = port;
    }

    public short getNumberWorkers() {
        return numberWorkers;
    }

    public void setNumberWorkers(short numberWorkers) {
        this.numberWorkers = numberWorkers;
    }

    public long getSecondsClientIdle() {
        return secondsClientIdle;
    }

    public void setSecondsClientIdle(long secondsClientIdle) {
        this.secondsClientIdle = secondsClientIdle;
    }

    public long getSecondsRemoteIdle() {
        return secondsRemoteIdle;
    }

    public void setSecondsRemoteIdle(long secondsRemoteIdle) {
        this.secondsRemoteIdle = secondsRemoteIdle;
    }
}
