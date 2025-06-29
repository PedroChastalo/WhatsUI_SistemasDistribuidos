package br.com.whatsut.service;

import br.com.whatsut.model.GroupMessage;
import br.com.whatsut.model.PrivateMessage;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface remota para serviços relacionados a mensagens no sistema WhatsUT.
 */
public interface MessageService extends Remote {
    
    /**
     * Envia uma mensagem privada para outro usuário.
     * 
     * @param senderId ID do remetente
     * @param receiverId ID do destinatário
     * @param content Conteúdo da mensagem
     * @return A mensagem criada ou null em caso de erro
     * @throws RemoteException Erro de comunicação RMI
     */
    PrivateMessage sendPrivateMessage(String senderId, String receiverId, String content) throws RemoteException;
    
    /**
     * Envia uma mensagem para um grupo.
     * 
     * @param senderId ID do remetente
     * @param groupId ID do grupo
     * @param content Conteúdo da mensagem
     * @return A mensagem criada ou null em caso de erro
     * @throws RemoteException Erro de comunicação RMI
     */
    GroupMessage sendGroupMessage(String senderId, String groupId, String content) throws RemoteException;
    
    /**
     * Envia um arquivo para outro usuário.
     * 
     * @param senderId ID do remetente
     * @param receiverId ID do destinatário
     * @param file Arquivo a ser enviado
     * @param fileType Tipo do arquivo
     * @return A mensagem criada ou null em caso de erro
     * @throws RemoteException Erro de comunicação RMI
     */
    PrivateMessage sendPrivateFile(String senderId, String receiverId, File file, String fileType) throws RemoteException;
    
    /**
     * Envia um arquivo para um grupo.
     * 
     * @param senderId ID do remetente
     * @param groupId ID do grupo
     * @param file Arquivo a ser enviado
     * @param fileType Tipo do arquivo
     * @return A mensagem criada ou null em caso de erro
     * @throws RemoteException Erro de comunicação RMI
     */
    GroupMessage sendGroupFile(String senderId, String groupId, File file, String fileType) throws RemoteException;
    
    /**
     * Busca mensagens privadas entre dois usuários.
     * 
     * @param userId1 ID do primeiro usuário
     * @param userId2 ID do segundo usuário
     * @param limit Número máximo de mensagens
     * @param offset Deslocamento para paginação
     * @return Lista de mensagens
     * @throws RemoteException Erro de comunicação RMI
     */
    List<PrivateMessage> getPrivateMessages(String userId1, String userId2, int limit, int offset) throws RemoteException;
    
    /**
     * Busca mensagens de um grupo.
     * 
     * @param groupId ID do grupo
     * @param limit Número máximo de mensagens
     * @param offset Deslocamento para paginação
     * @return Lista de mensagens
     * @throws RemoteException Erro de comunicação RMI
     */
    List<GroupMessage> getGroupMessages(String groupId, int limit, int offset) throws RemoteException;
    
    /**
     * Marca uma mensagem como lida.
     * 
     * @param messageId ID da mensagem
     * @param userId ID do usuário que leu a mensagem
     * @return true se marcada com sucesso, false caso contrário
     * @throws RemoteException Erro de comunicação RMI
     */
    boolean markMessageAsRead(String messageId, String userId) throws RemoteException;
    
    /**
     * Exclui uma mensagem.
     * 
     * @param messageId ID da mensagem
     * @param userId ID do usuário que está excluindo (deve ser o remetente)
     * @return true se excluída com sucesso, false caso contrário
     * @throws RemoteException Erro de comunicação RMI
     */
    boolean deleteMessage(String messageId, String userId) throws RemoteException;
}
