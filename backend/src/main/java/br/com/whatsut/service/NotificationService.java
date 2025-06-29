package br.com.whatsut.service;

import br.com.whatsut.model.NotificationListener;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface remota para serviços de notificação no sistema WhatsUT.
 */
public interface NotificationService extends Remote {
    
    /**
     * Registra um cliente para receber notificações.
     * 
     * @param sessionId ID da sessão do cliente
     * @param listener Objeto de callback para receber notificações
     * @throws RemoteException Erro de comunicação RMI
     */
    void registerClient(String sessionId, NotificationListener listener) throws RemoteException;
    
    /**
     * Remove o registro de um cliente para notificações.
     * 
     * @param sessionId ID da sessão do cliente
     * @throws RemoteException Erro de comunicação RMI
     */
    void unregisterClient(String sessionId) throws RemoteException;
}
