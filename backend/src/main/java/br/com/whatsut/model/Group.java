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
    
    public Group() {
        this.groupId = UUID.randomUUID().toString();
        this.createdAt = Instant.now().toEpochMilli();
        this.members = new ArrayList<>();
    }
    
    public Group(String name, String adminId) {
        this();
        this.name = name;
        this.adminId = adminId;
        this.members.add(adminId); // O admin é automaticamente um membro
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }
    
    public void addMember(String userId) {
        if (!this.members.contains(userId)) {
            this.members.add(userId);
        }
    }
    
    public void removeMember(String userId) {
        this.members.remove(userId);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
        this.lastMessageTime = Instant.now().toEpochMilli();
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
    
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
