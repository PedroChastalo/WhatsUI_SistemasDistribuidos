package br.com.whatsut.service;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * Interface para o serviço RMI de mensagens
 */
public interface MessageService extends Remote {
    
    /**
     * Obtém as conversas privadas do usuário
     * @param sessionId ID da sessão do usuário autenticado
     * @return Lista de conversas privadas
     * @throws RemoteException Erro RMI
     */
    List<Map<String, Object>> getPrivateConversations(String sessionId) throws RemoteException;
    
    /**
     * Obtém mensagens de uma conversa privada
     * @param sessionId ID da sessão do usuário autenticado
     * @param otherUserId ID do outro usuário na conversa
     * @return Lista de mensagens
     * @throws RemoteException Erro RMI
     */
    List<Map<String, Object>> getPrivateMessages(String sessionId, String otherUserId) throws RemoteException;
    
    /**
     * Envia uma mensagem privada
     * @param sessionId ID da sessão do usuário autenticado
     * @param receiverId ID do usuário destinatário
     * @param content Conteúdo da mensagem
     * @return Dados da mensagem enviada
     * @throws RemoteException Erro RMI
     */
    Map<String, Object> sendPrivateMessage(String sessionId, String receiverId, String content) throws RemoteException;
    
    /**
     * Envia um ARQUIVO em mensagem privada (conteúdo em base64).
     * @param sessionId sessão válida
     * @param receiverId destinatário
     * @param fileName nome original do arquivo
     * @param fileType content-type MIME
     * @param fileData bytes do arquivo
     * @return dados da mensagem enviada
     * @throws RemoteException Erro RMI
     */
    Map<String, Object> sendPrivateFile(String sessionId, String receiverId, String fileName, String fileType, byte[] fileData) throws RemoteException;
}
