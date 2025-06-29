package br.com.whatsut.model;

import java.io.Serializable;

/**
 * Modelo que representa uma sessão de usuário no sistema WhatsUT.
 */
public class Session implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String sessionId;
    private String userId;
    private long createdAt;
    private long expiresAt;
    private String token;
    private String clientAddress;
    
    public Session() {
    }
    
    public Session(String sessionId, String userId, String token, String clientAddress, long expirationTime) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.token = token;
        this.clientAddress = clientAddress;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = this.createdAt + expirationTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }
    
    public boolean isValid() {
        return System.currentTimeMillis() < expiresAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return sessionId.equals(session.sessionId);
    }

    @Override
    public int hashCode() {
        return sessionId.hashCode();
    }
}
