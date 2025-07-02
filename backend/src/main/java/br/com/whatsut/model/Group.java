package br.com.whatsut.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Modelo para representar um grupo de chat
 */
public class Group implements Serializable {
    private String groupId;
    private String name;
    private String adminId;
    private List<String> members;
    private long createdAt;
    private String lastMessage;
    private long lastMessageTime;
    
    /**
     * Construtor padrão
     */
    public Group() {
        this.groupId = UUID.randomUUID().toString();
        this.createdAt = Instant.now().toEpochMilli();
        this.members = new ArrayList<>();
    }
    
    /**
     * Construtor com nome e admin
     * @param name Nome do grupo
     * @param adminId ID do admin
     */
    public Group(String name, String adminId) {
        this();
        this.name = name;
        this.adminId = adminId;
        this.members.add(adminId); // O admin é automaticamente um membro
    }

    /**
     * Obtém o ID do grupo
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Define o ID do grupo
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * Obtém o nome do grupo
     */
    public String getName() {
        return name;
    }

    /**
     * Define o nome do grupo
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Obtém o ID do admin
     */
    public String getAdminId() {
        return adminId;
    }

    /**
     * Define o ID do admin
     */
    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    /**
     * Obtém a lista de membros
     */
    public List<String> getMembers() {
        return members;
    }

    /**
     * Define a lista de membros
     */
    public void setMembers(List<String> members) {
        this.members = members;
    }
    
    /**
     * Adiciona um membro ao grupo
     * @param userId ID do usuário a ser adicionado
     */
    public void addMember(String userId) {
        if (!this.members.contains(userId)) {
            this.members.add(userId);
        }
    }
    
    /**
     * Remove um membro do grupo
     * @param userId ID do usuário a ser removido
     */
    public void removeMember(String userId) {
        this.members.remove(userId);
    }

    /**
     * Obtém o timestamp de criação
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Define o timestamp de criação
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Obtém o conteúdo da última mensagem
     */
    public String getLastMessage() {
        return lastMessage;
    }

    /**
     * Define o conteúdo da última mensagem e atualiza o timestamp
     */
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
        this.lastMessageTime = Instant.now().toEpochMilli();
    }

    /**
     * Obtém o timestamp da última mensagem
     */
    public long getLastMessageTime() {
        return lastMessageTime;
    }

    /**
     * Define o timestamp da última mensagem
     */
    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
    
    /**
     * Retorna uma representação em string do grupo
     */
    @Override
    public String toString() {
        return "Group{" +
                "groupId='" + groupId + '\'' +
                ", name='" + name + '\'' +
                ", adminId='" + adminId + '\'' +
                ", members=" + members +
                ", createdAt=" + createdAt +
                ", lastMessage='" + lastMessage + '\'' +
                ", lastMessageTime=" + lastMessageTime +
                '}';
    }
}
