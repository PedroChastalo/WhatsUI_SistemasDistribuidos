package br.com.whatsut.impl;

import br.com.whatsut.dao.UserDAO;
import br.com.whatsut.model.User;
import br.com.whatsut.security.SessionManager;
import br.com.whatsut.service.UserService;

import java.rmi.RemoteException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementação do serviço de usuários
 */
public class UserServiceImpl implements UserService {
    // DAO para acesso aos dados de usuários
    private final UserDAO userDAO;
    
    public UserServiceImpl() {
        this.userDAO = new UserDAO();
    }
    
    /**
     * Obtém todos os usuários, exceto o próprio usuário da sessão
     * @param sessionId ID da sessão
     * @return Lista de usuários
     */
    @Override
    public List<User> getAllUsers(String sessionId) throws RemoteException {
        // Validar sessão
        String userId = SessionManager.getUserIdFromSession(sessionId);
        if (userId == null) {
            throw new RemoteException("Sessão inválida");
        }
        
        // Obter todos os usuários, exceto o próprio usuário
        return userDAO.getAllUsers().stream()
                .filter(user -> !user.getUserId().equals(userId))
                .collect(Collectors.toList());
    }
    
    /**
     * Obtém um usuário pelo ID
     * @param sessionId ID da sessão
     * @param targetUserId ID do usuário alvo
     * @return Usuário encontrado
     */
    @Override
    public User getUserById(String sessionId, String targetUserId) throws RemoteException {
        // Validar sessão
        String userId = SessionManager.getUserIdFromSession(sessionId);
        if (userId == null) {
            throw new RemoteException("Sessão inválida");
        }
        
        return userDAO.getUserById(targetUserId);
    }
    
    /**
     * Atualiza o status do usuário logado
     * @param sessionId ID da sessão
     * @param status Novo status
     * @return true se atualizado com sucesso
     */
    @Override
    public boolean updateStatus(String sessionId, String status) throws RemoteException {
        // Validar sessão
        String userId = SessionManager.getUserIdFromSession(sessionId);
        if (userId == null) {
            throw new RemoteException("Sessão inválida");
        }
        
        User user = userDAO.getUserById(userId);
        if (user == null) {
            throw new RemoteException("Usuário não encontrado");
        }
        
        return userDAO.updateUserStatus(userId, status);
    }
}
