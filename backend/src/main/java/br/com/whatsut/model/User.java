package br.com.whatsut.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Modelo para representar um usuário
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String userId;
    private String username;
    private String email;
    private String displayName;
    private String passwordHash;
    private String status;
    private Date lastSeen;
    private boolean isActive;
    
    /**
     * Construtor padrão
     */
    public User() {
        this.lastSeen = new Date();
        this.status = "online";
        this.isActive = true;
    }
    
    /**
     * Obtém o ID do usuário
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * Define o ID do usuário
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    /**
     * Obtém o nome de usuário
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Define o nome de usuário
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * Obtém o email do usuário
     */
    public String getEmail() {
        return email;
    }
    
    /**
     * Define o email do usuário
     */
    public void setEmail(String email) {
        this.email = email;
    }
    
    /**
     * Obtém o nome de exibição
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Define o nome de exibição
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Obtém o hash da senha
     */
    public String getPasswordHash() {
        return passwordHash;
    }
    
    /**
     * Define o hash da senha
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    /**
     * Obtém o status do usuário
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * Define o status do usuário
     */
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Obtém a data da última vez que o usuário foi visto
     */
    public Date getLastSeen() {
        return lastSeen;
    }
    
    /**
     * Define a data da última vez que o usuário foi visto
     */
    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    /**
     * Verifica se o usuário está ativo
     */
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * Define se o usuário está ativo
     */
    public void setActive(boolean active) {
        isActive = active;
    }
    
    /**
     * Retorna uma representação em string do usuário
     */
    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", status='" + status + '\'' +
                ", lastSeen=" + lastSeen +
                ", isActive=" + isActive +
                '}';
    }
}
