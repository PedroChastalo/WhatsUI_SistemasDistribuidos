package br.com.whatsut.impl;

import br.com.whatsut.dao.GroupDAO;
import br.com.whatsut.dao.GroupMemberDAO;
import br.com.whatsut.dao.MessageDAO;
import br.com.whatsut.dao.UserDAO;
import br.com.whatsut.model.Group;
import br.com.whatsut.model.GroupMember;
import br.com.whatsut.model.GroupMessage;
import br.com.whatsut.model.PrivateMessage;
import br.com.whatsut.model.User;
import br.com.whatsut.service.MessageService;
import br.com.whatsut.util.ConfigManager;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementação do serviço de mensagens.
 */
public class MessageServiceImpl extends UnicastRemoteObject implements MessageService {
    private static final Logger logger = Logger.getLogger(MessageServiceImpl.class.getName());
    private final MessageDAO messageDAO;
    private final UserDAO userDAO;
    private final GroupDAO groupDAO;
    private final GroupMemberDAO groupMemberDAO;
    
    public MessageServiceImpl() throws RemoteException {
        super();
        this.messageDAO = new MessageDAO();
        this.userDAO = new UserDAO();
        this.groupDAO = new GroupDAO();
        this.groupMemberDAO = new GroupMemberDAO();
    }
    
    @Override
    public PrivateMessage sendPrivateMessage(String senderId, String receiverId, String content) throws RemoteException {
        try {
            User sender = userDAO.findById(senderId);
            User receiver = userDAO.findById(receiverId);
            
            if (sender == null || receiver == null) {
                logger.info("Remetente ou destinatário não encontrado para envio de mensagem privada");
                return null;
            }
            
            // Verificar se o destinatário está banido
            if (receiver.isBanned()) {
                logger.info("Destinatário banido, não é possível enviar mensagem: " + receiverId);
                return null;
            }
            
            // Criar nova mensagem privada
            PrivateMessage message = new PrivateMessage();
            message.setMessageId(UUID.randomUUID().toString());
            message.setSenderId(senderId);
            message.setReceiverId(receiverId);
            message.setContent(content);
            message.setTimestamp(System.currentTimeMillis());
            message.setRead(false);
            
            // Salvar mensagem
            boolean saved = messageDAO.savePrivateMessage(message);
            if (saved) {
                logger.info("Mensagem privada enviada com sucesso: " + senderId + " para " + receiverId);
                return message;
            } else {
                logger.warning("Falha ao salvar mensagem privada: " + senderId + " para " + receiverId);
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao enviar mensagem privada", e);
            throw new RemoteException("Erro ao enviar mensagem privada", e);
        }
    }
    
    @Override
    public GroupMessage sendGroupMessage(String senderId, String groupId, String content) throws RemoteException {
        try {
            User sender = userDAO.findById(senderId);
            Group group = groupDAO.findById(groupId);
            
            if (sender == null || group == null) {
                logger.info("Remetente ou grupo não encontrado para envio de mensagem de grupo");
                return null;
            }
            
            // Verificar se o remetente é membro do grupo
            GroupMember member = groupMemberDAO.findByGroupAndUser(groupId, senderId);
            if (member == null) {
                logger.info("Remetente não é membro do grupo: " + senderId);
                return null;
            }
            
            // Criar nova mensagem de grupo
            GroupMessage message = new GroupMessage();
            message.setMessageId(UUID.randomUUID().toString());
            message.setSenderId(senderId);
            message.setGroupId(groupId);
            message.setContent(content);
            message.setTimestamp(System.currentTimeMillis());
            message.setRead(false);
            
            // Salvar mensagem
            boolean saved = messageDAO.saveGroupMessage(message);
            if (saved) {
                logger.info("Mensagem de grupo enviada com sucesso: " + senderId + " para grupo " + groupId);
                return message;
            } else {
                logger.warning("Falha ao salvar mensagem de grupo: " + senderId + " para grupo " + groupId);
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao enviar mensagem de grupo", e);
            throw new RemoteException("Erro ao enviar mensagem de grupo", e);
        }
    }
    
    @Override
    public PrivateMessage sendPrivateFile(String senderId, String receiverId, File file, String fileType) throws RemoteException {
        try {
            User sender = userDAO.findById(senderId);
            User receiver = userDAO.findById(receiverId);
            
            if (sender == null || receiver == null) {
                logger.info("Remetente ou destinatário não encontrado para envio de arquivo");
                return null;
            }
            
            // Verificar se o destinatário está banido
            if (receiver.isBanned()) {
                logger.info("Destinatário banido, não é possível enviar arquivo: " + receiverId);
                return null;
            }
            
            // Verificar se o arquivo existe
            if (file == null || !file.exists()) {
                logger.info("Arquivo não existe ou é nulo");
                return null;
            }
            
            // Se o tipo de arquivo não foi especificado, determinar com base na extensão
            if (fileType == null || fileType.isEmpty()) {
                fileType = getFileType(file.getName());
            }
            
            // Gerar um nome único para o arquivo
            String fileName = UUID.randomUUID().toString() + "_" + file.getName();
            
            // Definir o diretório de armazenamento de arquivos
            String storageDir = ConfigManager.getProperty("storage.files.dir", "files");
            String basePath = ConfigManager.getProperty("storage.base.path", "data");
            String filePath = basePath + File.separator + storageDir + File.separator + fileName;
            
            // Criar diretório se não existir
            File directory = new File(basePath + File.separator + storageDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // Copiar o arquivo para o diretório de armazenamento
            File destFile = new File(filePath);
            java.nio.file.Files.copy(file.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            // Criar nova mensagem privada com arquivo
            PrivateMessage message = new PrivateMessage();
            message.setMessageId(UUID.randomUUID().toString());
            message.setSenderId(senderId);
            message.setReceiverId(receiverId);
            message.setContent("Arquivo: " + file.getName());
            message.setTimestamp(System.currentTimeMillis());
            message.setRead(false);
            message.setFileUrl(fileName);
            message.setFileType(fileType);
            
            // Salvar mensagem
            boolean saved = messageDAO.savePrivateMessage(message);
            if (saved) {
                logger.info("Arquivo enviado com sucesso: " + senderId + " para " + receiverId + ", arquivo: " + fileName);
                return message;
            } else {
                // Se falhou ao salvar a mensagem, excluir o arquivo
                destFile.delete();
                logger.warning("Falha ao salvar mensagem com arquivo: " + senderId + " para " + receiverId);
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao enviar arquivo", e);
            throw new RemoteException("Erro ao enviar arquivo", e);
        }
    }
    
    @Override
    public List<PrivateMessage> getPrivateMessages(String userId1, String userId2, int limit, int offset) throws RemoteException {
        try {
            // Buscar mensagens usando o método getPrivateMessages do MessageDAO
            List<PrivateMessage> messages = messageDAO.getPrivateMessages(userId1, userId2, limit, offset);
            if (messages != null) {
                logger.info("Mensagens privadas recuperadas com sucesso: " + userId1 + " e " + userId2);
                return messages;
            } else {
                logger.warning("Falha ao recuperar mensagens privadas: " + userId1 + " e " + userId2);
                return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao buscar mensagens privadas", e);
            throw new RemoteException("Erro ao buscar mensagens privadas", e);
        }
    }
    
    @Override
    public List<GroupMessage> getGroupMessages(String groupId, int limit, int offset) throws RemoteException {
        try {
            // Buscar mensagens usando o método getGroupMessages do MessageDAO
            List<GroupMessage> messages = messageDAO.getGroupMessages(groupId, limit, offset);
            if (messages != null) {
                logger.info("Mensagens de grupo recuperadas com sucesso: " + groupId);
                return messages;
            } else {
                logger.warning("Falha ao recuperar mensagens de grupo: " + groupId);
                return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao buscar mensagens de grupo", e);
            throw new RemoteException("Erro ao buscar mensagens de grupo", e);
        }
    }
    
    // Método auxiliar não definido na interface, mas usado internamente
    public List<PrivateMessage> getUnreadMessages(String userId) throws RemoteException {
        try {
            // Implementação temporária: buscar todas as mensagens privadas e filtrar as não lidas
            List<PrivateMessage> allMessages = new ArrayList<>();
            List<String> conversations = getConversations(userId);
            
            for (String otherUserId : conversations) {
                List<PrivateMessage> messages = getPrivateMessages(userId, otherUserId, 100, 0);
                for (PrivateMessage message : messages) {
                    if (message.getReceiverId().equals(userId) && !message.isRead()) {
                        allMessages.add(message);
                    }
                }
            }
            
            return allMessages;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao buscar mensagens não lidas", e);
            throw new RemoteException("Erro ao buscar mensagens não lidas", e);
        }
    }
    
    @Override
    public boolean markMessageAsRead(String messageId, String userId) throws RemoteException {
        try {
            // Usar o método específico do MessageDAO para marcar como lida
            boolean marked = messageDAO.markMessageAsRead(messageId, userId);
            if (marked) {
                logger.info("Mensagem marcada como lida com sucesso: " + messageId);
                return true;
            } else {
                logger.warning("Falha ao marcar mensagem como lida: " + messageId);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao marcar mensagem como lida", e);
            throw new RemoteException("Erro ao marcar mensagem como lida", e);
        }
    }
    
    @Override
    public boolean deleteMessage(String messageId, String userId) throws RemoteException {
        try {
            // Usar o método específico do MessageDAO para excluir mensagem
            boolean deleted = messageDAO.deleteMessage(messageId, userId);
            if (deleted) {
                logger.info("Mensagem excluída com sucesso: " + messageId);
                return true;
            } else {
                logger.warning("Falha ao excluir mensagem: " + messageId);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao excluir mensagem", e);
            throw new RemoteException("Erro ao excluir mensagem", e);
        }
    }
    
    // Método auxiliar não definido na interface, mas usado internamente
    public List<String> getConversations(String userId) throws RemoteException {
        try {
            // Implementação alternativa: buscar todas as conversas privadas do usuário
            // Primeiro, obtemos todos os usuários
            List<User> allUsers = userDAO.findAll();
            List<String> conversationIds = new ArrayList<>();
            
            // Para cada usuário, verificamos se há mensagens trocadas com o usuário atual
            for (User user : allUsers) {
                if (!user.getUserId().equals(userId)) {
                    List<PrivateMessage> messages = messageDAO.getPrivateMessages(userId, user.getUserId(), 1, 0);
                    if (messages != null && !messages.isEmpty()) {
                        conversationIds.add(user.getUserId());
                    }
                }
            }
            
            return conversationIds;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao buscar conversas", e);
            throw new RemoteException("Erro ao buscar conversas", e);
        }
    }
    
    /**
     * Método auxiliar para determinar o tipo de arquivo com base na extensão.
     * 
     * @param fileName Nome do arquivo
     * @return Tipo do arquivo (mime type)
     */
    private String getFileType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "application/octet-stream";
        }
        
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1).toLowerCase();
        }
        
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "pdf":
                return "application/pdf";
            case "doc":
            case "docx":
                return "application/msword";
            case "xls":
            case "xlsx":
                return "application/vnd.ms-excel";
            case "ppt":
            case "pptx":
                return "application/vnd.ms-powerpoint";
            case "txt":
                return "text/plain";
            case "html":
            case "htm":
                return "text/html";
            case "mp3":
                return "audio/mpeg";
            case "mp4":
                return "video/mp4";
            case "wav":
                return "audio/wav";
            case "zip":
                return "application/zip";
            case "rar":
                return "application/x-rar-compressed";
            default:
                return "application/octet-stream";
        }
    }
    
    @Override
    public GroupMessage sendGroupFile(String senderId, String groupId, File file, String fileType) throws RemoteException {
        try {
            // Validar remetente
            User sender = userDAO.findById(senderId);
            if (sender == null) {
                logger.warning("Remetente não encontrado: " + senderId);
                return null;
            }
            
            // Validar grupo
            Group group = groupDAO.findById(groupId);
            if (group == null) {
                logger.warning("Grupo não encontrado: " + groupId);
                return null;
            }
            
            // Verificar se o usuário é membro do grupo
            GroupMember member = groupMemberDAO.findByGroupAndUser(groupId, senderId);
            if (member == null) {
                logger.warning("Usuário não é membro do grupo: " + senderId + " - " + groupId);
                return null;
            }
            
            // Validar arquivo
            if (file == null || !file.exists() || file.length() == 0) {
                logger.warning("Arquivo inválido ou vazio");
                return null;
            }
            
            // Se o tipo de arquivo não foi especificado, determinar com base na extensão
            if (fileType == null || fileType.isEmpty()) {
                fileType = getFileType(file.getName());
            }
            
            // Gerar ID único para a mensagem
            String messageId = UUID.randomUUID().toString();
            
            // Gerar um nome único para o arquivo
            String fileName = UUID.randomUUID().toString() + "_" + file.getName();
            
            // Definir o diretório de armazenamento de arquivos
            String storageDir = ConfigManager.getProperty("storage.files.dir", "files");
            String basePath = ConfigManager.getProperty("storage.base.path", "data");
            String filePath = basePath + File.separator + storageDir + File.separator + fileName;
            
            // Criar diretório se não existir
            File directory = new File(basePath + File.separator + storageDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // Copiar o arquivo para o diretório de armazenamento
            File destFile = new File(filePath);
            java.nio.file.Files.copy(file.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            // Criar mensagem com referência ao arquivo
            GroupMessage message = new GroupMessage();
            message.setMessageId(messageId);
            message.setSenderId(senderId);
            message.setGroupId(groupId);
            message.setContent("Arquivo: " + file.getName());
            message.setTimestamp(System.currentTimeMillis());
            message.setRead(false);
            message.setFileUrl(fileName);
            message.setFileType(fileType);
            
            // Salvar mensagem
            boolean saved = messageDAO.saveGroupMessage(message);
            if (saved) {
                logger.info("Arquivo enviado com sucesso para o grupo: " + groupId + ", arquivo: " + fileName);
                return message;
            } else {
                // Se falhou ao salvar a mensagem, excluir o arquivo
                destFile.delete();
                logger.warning("Falha ao salvar mensagem com arquivo para o grupo: " + groupId);
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao enviar arquivo para grupo", e);
            throw new RemoteException("Erro ao enviar arquivo para grupo", e);
        }
    }
}
