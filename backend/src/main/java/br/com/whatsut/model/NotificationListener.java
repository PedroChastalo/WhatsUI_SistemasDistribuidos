package br.com.whatsut.model;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface remota para callback de notificações no sistema WhatsUT.
 */
public interface NotificationListener extends Remote {
    
    /**
     * Notifica sobre uma nova mensagem privada.
     * 
     * @param message A mensagem recebida
     * @throws RemoteException Erro de comunicação RMI
     */
    void onPrivateMessageReceived(PrivateMessage message) throws RemoteException;
    
    /**
     * Notifica sobre uma nova mensagem de grupo.
     * 
     * @param message A mensagem de grupo recebida
     * @throws RemoteException Erro de comunicação RMI
     */
    void onGroupMessageReceived(GroupMessage message) throws RemoteException;
    
    /**
     * Notifica sobre alteração de status de um usuário.
     * 
     * @param userId ID do usuário
     * @param online Status online
     * @throws RemoteException Erro de comunicação RMI
     */
    void onUserStatusChanged(String userId, boolean online) throws RemoteException;
    
    /**
     * Notifica sobre uma solicitação de entrada em grupo.
     * 
     * @param groupId ID do grupo
     * @param userId ID do usuário solicitante
     * @throws RemoteException Erro de comunicação RMI
     */
    void onGroupJoinRequest(String groupId, String userId) throws RemoteException;
    
    /**
     * Notifica sobre o resultado de uma solicitação de entrada em grupo.
     * 
     * @param groupId ID do grupo
     * @param approved true se aprovado, false se rejeitado
     * @throws RemoteException Erro de comunicação RMI
     */
    void onGroupJoinRequestProcessed(String groupId, boolean approved) throws RemoteException;
    
    /**
     * Notifica sobre um ping do servidor para verificar se o cliente está ativo.
     * 
     * @throws RemoteException Erro de comunicação RMI
     */
    void ping() throws RemoteException;
}
