package br.com.whatsut.model;

/**
 * Modelo que representa uma mensagem enviada para um grupo no sistema WhatsUT.
 */
public class GroupMessage extends Message {
    private static final long serialVersionUID = 1L;
    
    private String groupId;
    
    public GroupMessage() {
        super();
    }
    
    public GroupMessage(String messageId, String senderId, String groupId, String content) {
        super(messageId, senderId, content);
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
