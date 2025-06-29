package br.com.whatsut.model;

import java.io.Serializable;

/**
 * Modelo que representa uma mensagem no sistema WhatsUT.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String messageId;
    private String senderId;
    private String receiverId;   // ID do destinatário (para mensagens privadas)
    private String groupId;      // ID do grupo (para mensagens de grupo)
    private String content;
    private long timestamp;
    private String fileUrl;
    private String fileType;
    private boolean read;
    private String attachmentUrl; // URL para anexos
    
    public Message() {
    }
    
    public Message(String messageId, String senderId, String content) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.read = false;
    }
    
    public Message(String messageId, String senderId, String receiverId, String groupId, String content) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.groupId = groupId;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.read = false;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }
    
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
    
    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return messageId.equals(message.messageId);
    }

    @Override
    public int hashCode() {
        return messageId.hashCode();
    }
}
