package br.com.whatsut.model;

import java.io.Serializable;

/**
 * Modelo que representa um membro de um grupo no sistema WhatsUT.
 */
public class GroupMember implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String memberId;
    private String groupId;
    private String userId;
    private long joinedAt;
    private boolean isAdmin;
    
    public GroupMember() {
    }
    
    public GroupMember(String memberId, String groupId, String userId, boolean isAdmin) {
        this.memberId = memberId;
        this.groupId = groupId;
        this.userId = userId;
        this.joinedAt = System.currentTimeMillis();
        this.isAdmin = isAdmin;
    }
    
    public GroupMember(String groupId, String userId, boolean isAdmin) {
        this.groupId = groupId;
        this.userId = userId;
        this.joinedAt = System.currentTimeMillis();
        this.isAdmin = isAdmin;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }
    
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupMember that = (GroupMember) o;
        return groupId.equals(that.groupId) && userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        int result = groupId.hashCode();
        result = 31 * result + userId.hashCode();
        return result;
    }
}
