package br.com.whatsut.dao;

import br.com.whatsut.model.GroupMessage;
import br.com.whatsut.model.Message;
import br.com.whatsut.model.PrivateMessage;
import br.com.whatsut.util.ConfigManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO para operações de persistência de mensagens.
 * Armazena mensagens em arquivos separados por conversa.
 */
public class MessageDAO {
    private static final Logger logger = Logger.getLogger(MessageDAO.class.getName());
    private final ObjectMapper objectMapper;
    private final String basePath;
    private final String messagesDir;
    
    // Cache para melhorar performance e evitar loops infinitos de requisições
    private final Map<String, List<Message>> messageCache;
    private final int maxCacheSize;
    
    public MessageDAO() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        this.basePath = ConfigManager.getProperty("storage.base.path", "data");
        this.messagesDir = ConfigManager.getProperty("storage.messages.dir", "messages");
        this.maxCacheSize = ConfigManager.getIntProperty("cache.messages.max.size", 1000);
        this.messageCache = new ConcurrentHashMap<>();
        
        // Garantir que os diretórios existam
        createDirectories();
        
        logger.info("MessageDAO inicializado");
    }
    
    /**
     * Cria os diretórios necessários para armazenamento de mensagens.
     */
    private void createDirectories() {
        Path directory = Paths.get(basePath, messagesDir);
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao criar diretório para mensagens", e);
        }
    }
    
    /**
     * Obtém o caminho do arquivo para uma conversa privada.
     *
     * @param userId1 ID do primeiro usuário
     * @param userId2 ID do segundo usuário
     * @return Caminho do arquivo
     */
    private String getPrivateConversationFilePath(String userId1, String userId2) {
        // Ordenar IDs para garantir consistência
        String[] ids = {userId1, userId2};
        Arrays.sort(ids);
        
        return basePath + File.separator + messagesDir + File.separator + "private_" + ids[0] + "_" + ids[1] + ".json";
    }
    
    /**
     * Obtém o caminho do arquivo para mensagens de um grupo.
     *
     * @param groupId ID do grupo
     * @return Caminho do arquivo
     */
    private String getGroupConversationFilePath(String groupId) {
        return basePath + File.separator + messagesDir + File.separator + "group_" + groupId + ".json";
    }
    
    /**
     * Obtém a chave de cache para uma conversa privada.
     *
     * @param userId1 ID do primeiro usuário
     * @param userId2 ID do segundo usuário
     * @return Chave de cache
     */
    private String getPrivateConversationCacheKey(String userId1, String userId2) {
        String[] ids = {userId1, userId2};
        Arrays.sort(ids);
        return "private_" + ids[0] + "_" + ids[1];
    }
    
    /**
     * Obtém a chave de cache para mensagens de um grupo.
     *
     * @param groupId ID do grupo
     * @return Chave de cache
     */
    private String getGroupConversationCacheKey(String groupId) {
        return "group_" + groupId;
    }
    
    /**
     * Carrega mensagens de um arquivo.
     *
     * @param filePath Caminho do arquivo
     * @return Lista de mensagens
     */
    private List<Message> loadMessages(String filePath) {
        File file = new File(filePath);
        
        if (!file.exists()) {
            try {
                file.createNewFile();
                objectMapper.writeValue(file, new ArrayList<>());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Erro ao criar arquivo de mensagens", e);
                return new ArrayList<>();
            }
        }
        
        try {
            if (file.length() == 0) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(file, new TypeReference<List<Message>>() {});
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao carregar mensagens do arquivo: " + filePath, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Salva mensagens em um arquivo.
     *
     * @param filePath Caminho do arquivo
     * @param messages Lista de mensagens
     * @return true se salvo com sucesso, false caso contrário
     */
    private boolean saveMessages(String filePath, List<Message> messages) {
        try {
            File file = new File(filePath);
            objectMapper.writeValue(file, messages);
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao salvar mensagens no arquivo: " + filePath, e);
            return false;
        }
    }
    
    /**
     * Salva uma mensagem privada.
     *
     * @param message Mensagem privada
     * @return true se salvo com sucesso, false caso contrário
     */
    public boolean savePrivateMessage(PrivateMessage message) {
        String senderId = message.getSenderId();
        String receiverId = message.getReceiverId();
        String filePath = getPrivateConversationFilePath(senderId, receiverId);
        String cacheKey = getPrivateConversationCacheKey(senderId, receiverId);
        
        // Carregar mensagens existentes
        List<Message> messages;
        synchronized (this) {
            messages = messageCache.get(cacheKey);
            if (messages == null) {
                messages = loadMessages(filePath);
            }
            
            // Adicionar nova mensagem
            messages.add(message);
            
            // Atualizar cache
            messageCache.put(cacheKey, messages);
            
            // Salvar no arquivo
            return saveMessages(filePath, messages);
        }
    }
    
    /**
     * Salva uma mensagem de grupo.
     *
     * @param message Mensagem de grupo
     * @return true se salvo com sucesso, false caso contrário
     */
    public boolean saveGroupMessage(GroupMessage message) {
        String groupId = message.getGroupId();
        String filePath = getGroupConversationFilePath(groupId);
        String cacheKey = getGroupConversationCacheKey(groupId);
        
        // Carregar mensagens existentes
        List<Message> messages;
        synchronized (this) {
            messages = messageCache.get(cacheKey);
            if (messages == null) {
                messages = loadMessages(filePath);
            }
            
            // Adicionar nova mensagem
            messages.add(message);
            
            // Atualizar cache
            messageCache.put(cacheKey, messages);
            
            // Salvar no arquivo
            return saveMessages(filePath, messages);
        }
    }
    
    /**
     * Busca mensagens privadas entre dois usuários.
     *
     * @param userId1 ID do primeiro usuário
     * @param userId2 ID do segundo usuário
     * @param limit Número máximo de mensagens
     * @param offset Deslocamento para paginação
     * @return Lista de mensagens privadas
     */
    public List<PrivateMessage> getPrivateMessages(String userId1, String userId2, int limit, int offset) {
        String filePath = getPrivateConversationFilePath(userId1, userId2);
        String cacheKey = getPrivateConversationCacheKey(userId1, userId2);
        
        // Carregar mensagens
        List<Message> messages;
        synchronized (this) {
            messages = messageCache.get(cacheKey);
            if (messages == null) {
                messages = loadMessages(filePath);
                
                // Atualizar cache se não exceder o limite
                if (messageCache.size() < maxCacheSize) {
                    messageCache.put(cacheKey, messages);
                }
            }
        }
        
        // Filtrar e converter para PrivateMessage
        List<PrivateMessage> privateMessages = new ArrayList<>();
        for (Message message : messages) {
            if (message instanceof PrivateMessage) {
                privateMessages.add((PrivateMessage) message);
            }
        }
        
        // Ordenar por timestamp (mais recentes primeiro)
        privateMessages.sort(Comparator.comparing(Message::getTimestamp).reversed());
        
        // Aplicar paginação
        int end = Math.min(offset + limit, privateMessages.size());
        if (offset < privateMessages.size()) {
            return privateMessages.subList(offset, end);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Busca mensagens de um grupo.
     *
     * @param groupId ID do grupo
     * @param limit Número máximo de mensagens
     * @param offset Deslocamento para paginação
     * @return Lista de mensagens de grupo
     */
    public List<GroupMessage> getGroupMessages(String groupId, int limit, int offset) {
        String filePath = getGroupConversationFilePath(groupId);
        String cacheKey = getGroupConversationCacheKey(groupId);
        
        // Carregar mensagens
        List<Message> messages;
        synchronized (this) {
            messages = messageCache.get(cacheKey);
            if (messages == null) {
                messages = loadMessages(filePath);
                
                // Atualizar cache se não exceder o limite
                if (messageCache.size() < maxCacheSize) {
                    messageCache.put(cacheKey, messages);
                }
            }
        }
        
        // Filtrar e converter para GroupMessage
        List<GroupMessage> groupMessages = new ArrayList<>();
        for (Message message : messages) {
            if (message instanceof GroupMessage) {
                groupMessages.add((GroupMessage) message);
            }
        }
        
        // Ordenar por timestamp (mais recentes primeiro)
        groupMessages.sort(Comparator.comparing(Message::getTimestamp).reversed());
        
        // Aplicar paginação
        int end = Math.min(offset + limit, groupMessages.size());
        if (offset < groupMessages.size()) {
            return groupMessages.subList(offset, end);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Busca uma mensagem pelo ID.
     *
     * @param messageId ID da mensagem
     * @return A mensagem encontrada ou null se não existir
     */
    public Message findById(String messageId) {
        // Buscar em todas as conversas em cache
        for (List<Message> messages : messageCache.values()) {
            for (Message message : messages) {
                if (message.getMessageId().equals(messageId)) {
                    return message;
                }
            }
        }
        
        // Se não encontrado no cache, buscar em todos os arquivos
        // (implementação simplificada, em produção seria necessário um índice)
        File messagesDirectory = new File(basePath + File.separator + messagesDir);
        File[] files = messagesDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        
        if (files != null) {
            for (File file : files) {
                List<Message> messages = loadMessages(file.getAbsolutePath());
                for (Message message : messages) {
                    if (message.getMessageId().equals(messageId)) {
                        return message;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Marca uma mensagem como lida.
     *
     * @param messageId ID da mensagem
     * @param userId ID do usuário que leu a mensagem
     * @return true se marcada com sucesso, false caso contrário
     */
    public boolean markMessageAsRead(String messageId, String userId) {
        Message message = findById(messageId);
        if (message == null) {
            return false;
        }
        
        // Verificar se é o destinatário
        boolean isRecipient = false;
        String filePath = null;
        String cacheKey = null;
        
        if (message instanceof PrivateMessage) {
            PrivateMessage privateMessage = (PrivateMessage) message;
            if (privateMessage.getReceiverId().equals(userId)) {
                isRecipient = true;
                filePath = getPrivateConversationFilePath(privateMessage.getSenderId(), privateMessage.getReceiverId());
                cacheKey = getPrivateConversationCacheKey(privateMessage.getSenderId(), privateMessage.getReceiverId());
            }
        } else if (message instanceof GroupMessage) {
            GroupMessage groupMessage = (GroupMessage) message;
            if (!groupMessage.getSenderId().equals(userId)) {
                isRecipient = true;
                filePath = getGroupConversationFilePath(groupMessage.getGroupId());
                cacheKey = getGroupConversationCacheKey(groupMessage.getGroupId());
            }
        }
        
        if (!isRecipient || filePath == null) {
            return false;
        }
        
        // Atualizar mensagem
        message.setRead(true);
        
        // Atualizar no cache e no arquivo
        synchronized (this) {
            List<Message> messages = messageCache.get(cacheKey);
            if (messages != null) {
                for (int i = 0; i < messages.size(); i++) {
                    if (messages.get(i).getMessageId().equals(messageId)) {
                        messages.set(i, message);
                        break;
                    }
                }
            } else {
                messages = loadMessages(filePath);
                for (int i = 0; i < messages.size(); i++) {
                    if (messages.get(i).getMessageId().equals(messageId)) {
                        messages.set(i, message);
                        break;
                    }
                }
                
                // Atualizar cache se não exceder o limite
                if (messageCache.size() < maxCacheSize) {
                    messageCache.put(cacheKey, messages);
                }
            }
            
            return saveMessages(filePath, messages);
        }
    }
    
    /**
     * Exclui uma mensagem.
     *
     * @param messageId ID da mensagem
     * @param userId ID do usuário que está excluindo (deve ser o remetente)
     * @return true se excluída com sucesso, false caso contrário
     */
    public boolean deleteMessage(String messageId, String userId) {
        Message message = findById(messageId);
        if (message == null) {
            return false;
        }
        
        // Verificar se é o remetente
        if (!message.getSenderId().equals(userId)) {
            return false;
        }
        
        String filePath = null;
        String cacheKey = null;
        
        if (message instanceof PrivateMessage) {
            PrivateMessage privateMessage = (PrivateMessage) message;
            filePath = getPrivateConversationFilePath(privateMessage.getSenderId(), privateMessage.getReceiverId());
            cacheKey = getPrivateConversationCacheKey(privateMessage.getSenderId(), privateMessage.getReceiverId());
        } else if (message instanceof GroupMessage) {
            GroupMessage groupMessage = (GroupMessage) message;
            filePath = getGroupConversationFilePath(groupMessage.getGroupId());
            cacheKey = getGroupConversationCacheKey(groupMessage.getGroupId());
        }
        
        if (filePath == null) {
            return false;
        }
        
        // Remover do cache e do arquivo
        synchronized (this) {
            List<Message> messages = messageCache.get(cacheKey);
            if (messages != null) {
                messages.removeIf(m -> m.getMessageId().equals(messageId));
            } else {
                messages = loadMessages(filePath);
                messages.removeIf(m -> m.getMessageId().equals(messageId));
                
                // Atualizar cache se não exceder o limite
                if (messageCache.size() < maxCacheSize) {
                    messageCache.put(cacheKey, messages);
                }
            }
            
            return saveMessages(filePath, messages);
        }
    }
    
    /**
     * Limpa o cache de mensagens.
     */
    public void clearCache() {
        messageCache.clear();
        logger.info("Cache de mensagens limpo");
    }
    
    /**
     * Limpa o cache de uma conversa privada específica.
     *
     * @param userId1 ID do primeiro usuário
     * @param userId2 ID do segundo usuário
     */
    public void clearPrivateConversationCache(String userId1, String userId2) {
        String cacheKey = getPrivateConversationCacheKey(userId1, userId2);
        messageCache.remove(cacheKey);
        logger.info("Cache da conversa privada entre " + userId1 + " e " + userId2 + " limpo");
    }
    
    /**
     * Limpa o cache de mensagens de um grupo específico.
     *
     * @param groupId ID do grupo
     */
    public void clearGroupConversationCache(String groupId) {
        String cacheKey = getGroupConversationCacheKey(groupId);
        messageCache.remove(cacheKey);
        logger.info("Cache de mensagens do grupo " + groupId + " limpo");
    }
}
