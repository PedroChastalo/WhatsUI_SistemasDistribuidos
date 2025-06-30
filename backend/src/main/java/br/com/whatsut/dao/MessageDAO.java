package br.com.whatsut.dao;

import br.com.whatsut.model.Message;
import br.com.whatsut.util.DataPersistenceUtil;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * DAO para gerenciar mensagens com persistência em JSON
 */
public class MessageDAO {
    private static final String PRIVATE_MESSAGES_FILE = "private_messages";
    private static final String GROUP_MESSAGES_FILE = "group_messages";
    
    // Armazenar mensagens privadas: chave é userId_otherUserId
    private static Map<String, List<Message>> privateMessages;
    
    // Armazenar mensagens de grupo: chave é groupId
    private static Map<String, List<Message>> groupMessages;
    
    public MessageDAO() {
        loadData();
    }
    
    /**
     * Carrega os dados dos arquivos JSON
     */
    private void loadData() {
        // Carregar mensagens privadas
        TypeReference<Map<String, List<Message>>> privateTypeRef = new TypeReference<Map<String, List<Message>>>() {};
        privateMessages = DataPersistenceUtil.loadData(PRIVATE_MESSAGES_FILE, privateTypeRef, new ConcurrentHashMap<>());
        
        // Carregar mensagens de grupo
        TypeReference<Map<String, List<Message>>> groupTypeRef = new TypeReference<Map<String, List<Message>>>() {};
        groupMessages = DataPersistenceUtil.loadData(GROUP_MESSAGES_FILE, groupTypeRef, new ConcurrentHashMap<>());
        
        System.out.println("Dados de mensagens carregados: " + 
                          privateMessages.size() + " conversas privadas, " + 
                          groupMessages.size() + " conversas de grupo");
    }
    
    /**
     * Salva os dados em arquivos JSON
     */
    private void saveData() {
        DataPersistenceUtil.saveData(PRIVATE_MESSAGES_FILE, privateMessages);
        DataPersistenceUtil.saveData(GROUP_MESSAGES_FILE, groupMessages);
    }
    
    /**
     * Adiciona uma mensagem privada
     * @param senderId ID do remetente
     * @param receiverId ID do destinatário
     * @param message Mensagem a ser adicionada
     * @return A mensagem adicionada
     */
    public Message addPrivateMessage(String senderId, String receiverId, Message message) {
        // Armazenar a mensagem em ambas as direções para que ambos os usuários possam vê-la
        String key1 = getPrivateKey(senderId, receiverId);
        String key2 = getPrivateKey(receiverId, senderId);
        
        privateMessages.computeIfAbsent(key1, k -> new ArrayList<>()).add(message);
        privateMessages.computeIfAbsent(key2, k -> new ArrayList<>()).add(message);
        
        // Persistir dados
        saveData();
        
        return message;
    }
    
    /**
     * Adiciona uma mensagem de grupo
     * @param groupId ID do grupo
     * @param message Mensagem a ser adicionada
     * @return A mensagem adicionada
     */
    public Message addGroupMessage(String groupId, Message message) {
        groupMessages.computeIfAbsent(groupId, k -> new ArrayList<>()).add(message);
        
        // Persistir dados
        saveData();
        
        return message;
    }
    
    /**
     * Obtém mensagens privadas entre dois usuários
     * @param userId ID do usuário atual
     * @param otherUserId ID do outro usuário
     * @return Lista de mensagens
     */
    public List<Message> getPrivateMessages(String userId, String otherUserId) {
        String key = getPrivateKey(userId, otherUserId);
        return privateMessages.getOrDefault(key, Collections.emptyList());
    }
    
    /**
     * Obtém mensagens de um grupo
     * @param groupId ID do grupo
     * @return Lista de mensagens
     */
    public List<Message> getGroupMessages(String groupId) {
        return groupMessages.getOrDefault(groupId, Collections.emptyList());
    }
    
    /**
     * Obtém todas as conversas privadas de um usuário
     * @param userId ID do usuário
     * @return Lista de IDs de usuários com quem o usuário tem conversas
     */
    public List<String> getUserConversations(String userId) {
        return privateMessages.keySet().stream()
                .filter(key -> key.startsWith(userId + "_"))
                .map(key -> key.substring(key.indexOf("_") + 1))
                .collect(Collectors.toList());
    }
    
    /**
     * Obtém a última mensagem de uma conversa privada
     * @param userId ID do usuário atual
     * @param otherUserId ID do outro usuário
     * @return A última mensagem ou null se não houver mensagens
     */
    public Message getLastPrivateMessage(String userId, String otherUserId) {
        List<Message> messages = getPrivateMessages(userId, otherUserId);
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }
    
    /**
     * Obtém a última mensagem de um grupo
     * @param groupId ID do grupo
     * @return A última mensagem ou null se não houver mensagens
     */
    public Message getLastGroupMessage(String groupId) {
        List<Message> messages = getGroupMessages(groupId);
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }
    
    /**
     * Cria uma chave para armazenar mensagens privadas
     * @param userId1 ID do primeiro usuário
     * @param userId2 ID do segundo usuário
     * @return Chave no formato userId1_userId2
     */
    private String getPrivateKey(String userId1, String userId2) {
        return userId1 + "_" + userId2;
    }
}
