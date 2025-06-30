package br.com.whatsut.impl;

import br.com.whatsut.dao.MessageDAO;
import br.com.whatsut.dao.UserDAO;
import br.com.whatsut.model.Message;
import br.com.whatsut.model.User;
import br.com.whatsut.security.SessionManager;
import br.com.whatsut.service.MessageService;

import java.rmi.RemoteException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementação do serviço de mensagens
 */
public class MessageServiceImpl implements MessageService {
    private final MessageDAO messageDAO;
    private final UserDAO userDAO;
    
    public MessageServiceImpl() {
        this.messageDAO = new MessageDAO();
        this.userDAO = new UserDAO();
    }
    
    @Override
    public List<Map<String, Object>> getPrivateConversations(String sessionId) throws RemoteException {
        // Validar sessão
        String userId = SessionManager.getUserIdFromSession(sessionId);
        if (userId == null) {
            throw new RemoteException("Sessão inválida");
        }
        
        // Obter IDs de usuários com quem o usuário tem conversas
        List<String> conversationUserIds = messageDAO.getUserConversations(userId);
        
        // Criar lista de conversas
        List<Map<String, Object>> conversations = new ArrayList<>();
        
        for (String otherUserId : conversationUserIds) {
            User otherUser = userDAO.getUserById(otherUserId);
            if (otherUser == null) continue;
            
            Message lastMessage = messageDAO.getLastPrivateMessage(userId, otherUserId);
            
            Map<String, Object> conversation = new HashMap<>();
            conversation.put("userId", otherUserId);
            conversation.put("username", otherUser.getUsername());
            conversation.put("displayName", otherUser.getDisplayName());
            conversation.put("status", otherUser.getStatus());
            
            if (lastMessage != null) {
                conversation.put("lastMessage", lastMessage.getContent());
                conversation.put("timestamp", lastMessage.getTimestamp());
            } else {
                conversation.put("lastMessage", "");
                conversation.put("timestamp", Instant.now().toEpochMilli());
            }
            
            conversations.add(conversation);
        }
        
        // Ordenar por timestamp da última mensagem (mais recente primeiro)
        conversations.sort((c1, c2) -> {
            Long t1 = (Long) c1.get("timestamp");
            Long t2 = (Long) c2.get("timestamp");
            return t2.compareTo(t1);
        });
        
        return conversations;
    }
    
    @Override
    public List<Map<String, Object>> getPrivateMessages(String sessionId, String otherUserId) throws RemoteException {
        // Validar sessão
        String userId = SessionManager.getUserIdFromSession(sessionId);
        if (userId == null) {
            throw new RemoteException("Sessão inválida");
        }
        
        // Verificar se o outro usuário existe
        User otherUser = userDAO.getUserById(otherUserId);
        if (otherUser == null) {
            throw new RemoteException("Usuário não encontrado");
        }
        
        // Obter mensagens
        List<Message> messages = messageDAO.getPrivateMessages(userId, otherUserId);
        
        // Converter para Map
        return messages.stream().map(this::messageToMap).collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Object> sendPrivateMessage(String sessionId, String receiverId, String content) throws RemoteException {
        // Validar sessão
        String senderId = SessionManager.getUserIdFromSession(sessionId);
        if (senderId == null) {
            throw new RemoteException("Sessão inválida");
        }
        
        // Verificar se o remetente existe
        User sender = userDAO.getUserById(senderId);
        if (sender == null) {
            throw new RemoteException("Remetente não encontrado");
        }
        
        // Verificar se o destinatário existe
        User receiver = userDAO.getUserById(receiverId);
        if (receiver == null) {
            throw new RemoteException("Destinatário não encontrado");
        }
        
        // Criar e salvar a mensagem
        Message message = new Message(
                senderId,
                sender.getDisplayName(),
                receiverId,
                content,
                false
        );
        
        messageDAO.addPrivateMessage(senderId, receiverId, message);
        
        // Retornar a mensagem como Map
        return messageToMap(message);
    }
    
    /**
     * Converte um objeto Message para um Map
     * @param message Mensagem a ser convertida
     * @return Map representando a mensagem
     */
    private Map<String, Object> messageToMap(Message message) {
        Map<String, Object> map = new HashMap<>();
        map.put("messageId", message.getMessageId());
        map.put("senderId", message.getSenderId());
        map.put("senderName", message.getSenderName());
        map.put("receiverId", message.getReceiverId());
        map.put("content", message.getContent());
        map.put("timestamp", message.getTimestamp());
        map.put("isGroupMessage", message.isGroupMessage());
        
        if (message.getFileUrl() != null) {
            map.put("fileUrl", message.getFileUrl());
            map.put("fileType", message.getFileType());
        }
        
        return map;
    }
}
