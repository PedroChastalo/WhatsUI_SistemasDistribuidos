package br.com.whatsut.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Modelo para representar uma mensagem
 */
public class Message implements Serializable {
    private String messageId;
    private String senderId;
    private String senderName;
    private String receiverId; // Pode ser userId ou groupId
    private String content;
    private long timestamp;
    // Indica se é mensagem de grupo
    private boolean isGroupMessage;
    // URL do arquivo (se houver)
    private String fileUrl;
    // Tipo do arquivo (se houver)
    private String fileType;
    
    /**
     * Construtor padrão
     */
    public Message() {
        this.messageId = UUID.randomUUID().toString();
        this.timestamp = Instant.now().toEpochMilli();
    }
    
    /**
     * Construtor com parâmetros principais
     * @param senderId ID do remetente
     * @param senderName Nome do remetente
     * @param receiverId ID do destinatário
     * @param content Conteúdo
     * @param isGroupMessage Se é mensagem de grupo
     */
    public Message(String senderId, String senderName, String receiverId, String content, boolean isGroupMessage) {
        this();
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.content = content;
        this.isGroupMessage = isGroupMessage;
    }

    /**
     * Obtém o ID da mensagem
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Define o ID da mensagem
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * Obtém o ID do remetente
     */
    public String getSenderId() {
        return senderId;
    }

    /**
     * Define o ID do remetente
     */
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    /**
     * Obtém o nome do remetente
     */
    public String getSenderName() {
        return senderName;
    }

    /**
     * Define o nome do remetente
     */
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    /**
     * Obtém o ID do destinatário
     */
    public String getReceiverId() {
        return receiverId;
    }

    /**
     * Define o ID do destinatário
     */
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    /**
     * Obtém o conteúdo da mensagem
     */
    public String getContent() {
        return content;
    }

    /**
     * Define o conteúdo da mensagem
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Obtém o timestamp da mensagem
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Define o timestamp da mensagem
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Verifica se é mensagem de grupo
     */
    public boolean isGroupMessage() {
        return isGroupMessage;
    }

    /**
     * Define se é mensagem de grupo
     */
    public void setGroupMessage(boolean groupMessage) {
        isGroupMessage = groupMessage;
    }

    /**
     * Obtém a URL do arquivo
     */
    public String getFileUrl() {
        return fileUrl;
    }

    /**
     * Define a URL do arquivo
     */
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    /**
     * Obtém o tipo do arquivo
     */
    public String getFileType() {
        return fileType;
    }

    /**
     * Define o tipo do arquivo
     */
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    /**
     * Retorna uma representação em string da mensagem
     */
    @Override
    public String toString() {
        return "Message{" +
                "messageId='" + messageId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", senderName='" + senderName + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                ", isGroupMessage=" + isGroupMessage +
                ", fileUrl='" + fileUrl + '\'' +
                ", fileType='" + fileType + '\'' +
                '}';
    }
}
