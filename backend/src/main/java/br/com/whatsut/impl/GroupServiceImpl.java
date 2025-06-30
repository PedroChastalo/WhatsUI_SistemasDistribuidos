package br.com.whatsut.impl;

import br.com.whatsut.dao.GroupDAO;
import br.com.whatsut.dao.MessageDAO;
import br.com.whatsut.dao.UserDAO;
import br.com.whatsut.model.Group;
import br.com.whatsut.model.Message;
import br.com.whatsut.model.User;
import br.com.whatsut.security.SessionManager;
import br.com.whatsut.service.GroupService;

import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementação do serviço de grupos
 */
public class GroupServiceImpl implements GroupService {
    private final GroupDAO groupDAO;
    private final UserDAO userDAO;
    private final MessageDAO messageDAO;
    
    public GroupServiceImpl() {
        this.groupDAO = new GroupDAO();
        this.userDAO = new UserDAO();
        this.messageDAO = new MessageDAO();
    }
    
    @Override
    public List<Map<String, Object>> getGroups(String sessionId) throws RemoteException {
        // Validar sessão
        String userId = SessionManager.getUserIdFromSession(sessionId);
        if (userId == null) {
            throw new RemoteException("Sessão inválida");
        }
        
        // Obter grupos do usuário
        List<Group> userGroups = groupDAO.getUserGroups(userId);
        
        // Converter para Map
        return userGroups.stream().map(this::groupToMap).collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Object> createGroup(String sessionId, String name, List<String> members) throws RemoteException {
        // Validar sessão
        String userId = SessionManager.getUserIdFromSession(sessionId);
        if (userId == null) {
            throw new RemoteException("Sessão inválida");
        }
        
        // Verificar se o usuário existe
        User user = userDAO.getUserById(userId);
        if (user == null) {
            throw new RemoteException("Usuário não encontrado");
        }
        
        // Validar membros
        List<String> validMembers = new ArrayList<>();
        validMembers.add(userId); // Adicionar o criador
        
        if (members != null) {
            for (String memberId : members) {
                if (userDAO.getUserById(memberId) != null && !memberId.equals(userId)) {
                    validMembers.add(memberId);
                }
            }
        }
        
        // Criar o grupo
        Group group = groupDAO.createGroup(name, userId, validMembers);
        
        // Criar mensagem de sistema
        Message systemMessage = new Message(
                "system",
                "Sistema",
                group.getGroupId(),
                user.getDisplayName() + " criou o grupo",
                true
        );
        
        messageDAO.addGroupMessage(group.getGroupId(), systemMessage);
        groupDAO.updateLastMessage(group.getGroupId(), systemMessage.getContent());
        
        // Retornar o grupo como Map
        return groupToMap(group);
    }
    
    @Override
    public List<Map<String, Object>> getGroupMessages(String sessionId, String groupId) throws RemoteException {
        // Validar sessão
        String userId = SessionManager.getUserIdFromSession(sessionId);
        if (userId == null) {
            throw new RemoteException("Sessão inválida");
        }
        
        // Verificar se o grupo existe
        Group group = groupDAO.getGroupById(groupId);
        if (group == null) {
            throw new RemoteException("Grupo não encontrado");
        }
        
        // Verificar se o usuário é membro do grupo
        if (!group.getMembers().contains(userId)) {
            throw new RemoteException("Usuário não é membro do grupo");
        }
        
        // Obter mensagens
        List<Message> messages = messageDAO.getGroupMessages(groupId);
        
        // Converter para Map
        return messages.stream().map(this::messageToMap).collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Object> sendGroupMessage(String sessionId, String groupId, String content) throws RemoteException {
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
        
        // Verificar se o grupo existe
        Group group = groupDAO.getGroupById(groupId);
        if (group == null) {
            throw new RemoteException("Grupo não encontrado");
        }
        
        // Verificar se o usuário é membro do grupo
        if (!group.getMembers().contains(senderId)) {
            throw new RemoteException("Usuário não é membro do grupo");
        }
        
        // Criar e salvar a mensagem
        Message message = new Message(
                senderId,
                sender.getDisplayName(),
                groupId,
                content,
                true
        );
        
        messageDAO.addGroupMessage(groupId, message);
        groupDAO.updateLastMessage(groupId, message.getContent());
        
        // Retornar a mensagem como Map
        return messageToMap(message);
    }
    
    @Override
    public List<Map<String, Object>> getGroupMembers(String sessionId, String groupId) throws RemoteException {
        // Validar sessão
        String userId = SessionManager.getUserIdFromSession(sessionId);
        if (userId == null) {
            throw new RemoteException("Sessão inválida");
        }
        
        // Verificar se o grupo existe
        Group group = groupDAO.getGroupById(groupId);
        if (group == null) {
            throw new RemoteException("Grupo não encontrado");
        }
        
        // Verificar se o usuário é membro do grupo
        if (!group.getMembers().contains(userId)) {
            throw new RemoteException("Usuário não é membro do grupo");
        }
        
        // Obter membros
        List<Map<String, Object>> members = new ArrayList<>();
        
        for (String memberId : group.getMembers()) {
            User member = userDAO.getUserById(memberId);
            if (member != null) {
                Map<String, Object> memberMap = new HashMap<>();
                memberMap.put("userId", member.getUserId());
                memberMap.put("username", member.getUsername());
                memberMap.put("displayName", member.getDisplayName());
                memberMap.put("status", member.getStatus());
                memberMap.put("isAdmin", memberId.equals(group.getAdminId()));
                
                members.add(memberMap);
            }
        }
        
        return members;
    }
    
    /**
     * Converte um objeto Group para um Map
     * @param group Grupo a ser convertido
     * @return Map representando o grupo
     */
    private Map<String, Object> groupToMap(Group group) {
        Map<String, Object> map = new HashMap<>();
        map.put("groupId", group.getGroupId());
        map.put("name", group.getName());
        map.put("adminId", group.getAdminId());
        map.put("members", group.getMembers());
        map.put("createdAt", group.getCreatedAt());
        map.put("lastMessage", group.getLastMessage());
        map.put("lastMessageTime", group.getLastMessageTime());
        return map;
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
