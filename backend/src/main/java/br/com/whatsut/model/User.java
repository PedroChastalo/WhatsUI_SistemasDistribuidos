package br.com.whatsut.model;

import java.io.Serializable;

/**
 * Modelo que representa um usuário no sistema WhatsUT.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String userId;
    private String username;
    private String displayName;
    private String passwordHash;
    private String email;
    private boolean online;
    private long lastSeen;
    private boolean banned;
    
    public User() {
    }
    
    public User(String userId, String username, String displayName, String email) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.online = false;
        this.lastSeen = System.currentTimeMillis();
        this.banned = false;
    }
    
    /**
     * Construtor de cópia para criar um novo objeto User baseado em um existente.
     * 
     * @param user Objeto User a ser copiado
     */
    public User(User user) {
        if (user != null) {
            this.userId = user.userId;
            this.username = user.username;
            this.displayName = user.displayName;
            this.passwordHash = user.passwordHash;
            this.email = user.email;
            this.online = user.online;
            this.lastSeen = user.lastSeen;
            this.banned = user.banned;
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public boolean isBanned() {
        return banned;
    }
    
    public void setBanned(boolean banned) {
        this.banned = banned;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return userId.equals(user.userId);
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }
}
