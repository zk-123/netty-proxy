package com.zkdcloud.proxy.socks5.config;

/**
 * server config
 *
 * @author zk
 * @since 2019/1/30
 */
public class ServerConfigure {
    /**
     * expose port
     */
    private short port;
    /**
     * boss thread numbers
     */
    private short numberBoss;
    /**
     * worker thread numbers
     */
    private short numberWorkers;
    /**
     * client all(r/w) idle
     */
    private long secondsClientIdle;
    /**
     * remote all(r/w) idle
     */
    private long secondsRemoteIdle;
    /**
     * socks5 auth
     */
    private String authUsername;
    private String authPassword;

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

    public short getNumberBoss() {
        return numberBoss;
    }

    public void setNumberBoss(short numberBoss) {
        this.numberBoss = numberBoss;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public void setAuthUsername(String authUsername) {
        this.authUsername = authUsername;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }
}
