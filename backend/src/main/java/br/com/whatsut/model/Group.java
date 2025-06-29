package br.com.whatsut.model;

import java.io.Serializable;

/**
 * Modelo que representa um grupo de chat no sistema WhatsUT.
 */
public class Group implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String groupId;
    private String name;
    private String description;
    private String adminId;
    private String creatorId;
    private long createdAt;
    private boolean deleteOnAdminExit;
    
    public Group() {
    }
    
    public Group(String groupId, String name, String description, String adminId) {
        this.groupId = groupId;
        this.name = name;
        this.description = description;
        this.adminId = adminId;
        this.creatorId = adminId; // Por padrão, o criador é o administrador
        this.createdAt = System.currentTimeMillis();
        this.deleteOnAdminExit = false;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }
    
    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isDeleteOnAdminExit() {
        return deleteOnAdminExit;
    }

    public void setDeleteOnAdminExit(boolean deleteOnAdminExit) {
        this.deleteOnAdminExit = deleteOnAdminExit;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return groupId.equals(group.groupId);
    }

    @Override
    public int hashCode() {
        return groupId.hashCode();
    }
}
