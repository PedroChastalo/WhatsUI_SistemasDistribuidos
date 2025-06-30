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
    private final UserDAO userDAO;
    
    public UserServiceImpl() {
        this.userDAO = new UserDAO();
    }
    
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
    
    @Override
    public User getUserById(String sessionId, String targetUserId) throws RemoteException {
        // Validar sessão
        String userId = SessionManager.getUserIdFromSession(sessionId);
        if (userId == null) {
            throw new RemoteException("Sessão inválida");
        }
        
        return userDAO.getUserById(targetUserId);
    }
    
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
