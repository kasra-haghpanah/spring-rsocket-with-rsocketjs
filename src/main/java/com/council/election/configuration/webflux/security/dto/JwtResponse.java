package com.council.election.configuration.webflux.security.dto;

import java.io.Serializable;
import java.util.Map;

public class JwtResponse implements Serializable {
    private String token;
    private String username;
    private int level;
    private String domain;
    private Map<String, Object> metadata;

    public JwtResponse(String token, String username, int level, String domain, Map<String, Object> metadata) {
        this.token = token;
        this.username = username;
        this.level = level;
        this.domain = domain;
        this.metadata = metadata;
    }

    public String getToken() {
        return this.token;
    }

    public JwtResponse setToken(String token) {
        this.token = token;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public JwtResponse setUsername(String username) {
        this.username = username;
        return this;
    }

    public int getLevel() {
        return level;
    }

    public JwtResponse setLevel(int level) {
        this.level = level;
        return this;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "{"
                + "\"token\":\"" + token + "\""
                + ",\"username\":\"" + username + "\""
                + ",\"level\":\"" + level + "\""
                + "}";
    }
}
