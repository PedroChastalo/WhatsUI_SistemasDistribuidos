package br.com.whatsut.model;

/**
 * Modelo que representa uma mensagem privada entre dois usuários no sistema WhatsUT.
 */
public class PrivateMessage extends Message {
    private static final long serialVersionUID = 1L;
    
    private String receiverId;
    
    public PrivateMessage() {
        super();
    }
    
    public PrivateMessage(String messageId, String senderId, String receiverId, String content) {
        super(messageId, senderId, content);
        this.receiverId = receiverId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }
}
