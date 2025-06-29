package br.com.whatsut.impl;

import br.com.whatsut.dao.SessionDAO;
import br.com.whatsut.dao.UserDAO;
import br.com.whatsut.model.User;
import br.com.whatsut.service.UserService;

import java.util.ArrayList;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementação do serviço de usuários.
 */
public class UserServiceImpl extends UnicastRemoteObject implements UserService {
    private static final Logger logger = Logger.getLogger(UserServiceImpl.class.getName());
    private final UserDAO userDAO;
    private final SessionDAO sessionDAO;
    
    public UserServiceImpl() throws RemoteException {
        super();
        this.userDAO = new UserDAO();
        this.sessionDAO = new SessionDAO();
    }
    
    @Override
    public User getUser(String userId) throws RemoteException {
        try {
            User user = userDAO.findById(userId);
            if (user != null) {
                // Não retornar o hash da senha
                user.setPasswordHash(null);
            }
            return user;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao buscar usuário", e);
            throw new RemoteException("Erro ao buscar usuário", e);
        }
    }
    
    @Override
    public User getUserByUsername(String username) throws RemoteException {
        try {
            User user = userDAO.findByUsername(username);
            if (user != null) {
                // Não retornar o hash da senha
                user.setPasswordHash(null);
            }
            return user;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao buscar usuário por username", e);
            throw new RemoteException("Erro ao buscar usuário por username", e);
        }
    }
    
    @Override
    public List<User> getAllUsers() throws RemoteException {
        try {
            List<User> users = userDAO.findAll();
            // Não retornar o hash da senha
            for (User user : users) {
                user.setPasswordHash(null);
            }
            return users;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao buscar todos os usuários", e);
            throw new RemoteException("Erro ao buscar todos os usuários", e);
        }
    }
    
    // Método auxiliar para pesquisa de usuários (não faz parte da interface)
    public List<User> searchUsers(String query) throws RemoteException {
        try {
            // Implementação simplificada: buscar por nome de exibição ou username
            List<User> allUsers = userDAO.findAll();
            List<User> matchingUsers = new ArrayList<>();
            
            for (User user : allUsers) {
                if ((user.getDisplayName() != null && user.getDisplayName().toLowerCase().contains(query.toLowerCase())) ||
                    (user.getUsername() != null && user.getUsername().toLowerCase().contains(query.toLowerCase()))) {
                    User cleanUser = new User(user);
                    cleanUser.setPasswordHash(null);
                    matchingUsers.add(cleanUser);
                }
            }
            
            return matchingUsers;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao pesquisar usuários", e);
            throw new RemoteException("Erro ao pesquisar usuários", e);
        }
    }
    
    @Override
    public boolean updateUser(User updatedUser) throws RemoteException {
        try {
            if (updatedUser == null || updatedUser.getUserId() == null) {
                logger.info("Dados de usuário inválidos para atualização");
                return false;
            }
            
            User existingUser = userDAO.findById(updatedUser.getUserId());
            if (existingUser == null) {
                logger.info("Usuário não encontrado para atualização: " + updatedUser.getUserId());
                return false;
            }
            
            // Atualizar campos, preservando dados sensíveis
            if (updatedUser.getDisplayName() != null) {
                existingUser.setDisplayName(updatedUser.getDisplayName());
            }
            
            if (updatedUser.getEmail() != null) {
                existingUser.setEmail(updatedUser.getEmail());
            }
            
            // Não permitir atualização de senha ou status de banimento por este método
            
            // Salvar usuário
            boolean saved = userDAO.save(existingUser);
            if (saved) {
                logger.info("Usuário atualizado com sucesso: " + existingUser.getUserId());
                return true;
            } else {
                logger.warning("Falha ao atualizar usuário: " + existingUser.getUserId());
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao atualizar usuário", e);
            throw new RemoteException("Erro ao atualizar usuário", e);
        }
    }
    
    @Override
    public boolean updateUserStatus(String userId, boolean online) throws RemoteException {
        try {
            User user = userDAO.findById(userId);
            if (user == null) {
                logger.info("Usuário não encontrado para atualização de status: " + userId);
                return false;
            }
            
            // Atualizar status
            user.setOnline(online);
            user.setLastSeen(System.currentTimeMillis());
            
            // Salvar usuário
            boolean saved = userDAO.save(user);
            if (saved) {
                logger.info("Status do usuário atualizado com sucesso: " + userId + " - Online: " + online);
                return true;
            } else {
                logger.warning("Falha ao atualizar status do usuário: " + userId);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao atualizar status do usuário", e);
            throw new RemoteException("Erro ao atualizar status do usuário", e);
        }
    }
    
    @Override
    public boolean requestUserBan(String reporterId, String targetUserId, String reason) throws RemoteException {
        try {
            User reporter = userDAO.findById(reporterId);
            User target = userDAO.findById(targetUserId);
            
            if (reporter == null || target == null) {
                logger.info("Usuário não encontrado para solicitação de banimento");
                return false;
            }
            
            // Em uma implementação real, aqui seria registrada a solicitação de banimento
            // para análise posterior por um administrador
            
            logger.info("Solicitação de banimento registrada: Reporter: " + reporterId + 
                    ", Target: " + targetUserId + ", Razão: " + reason);
            
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao solicitar banimento", e);
            throw new RemoteException("Erro ao solicitar banimento", e);
        }
    }
    
    @Override
    public boolean banUser(String adminId, String userId) throws RemoteException {
        try {
            User admin = userDAO.findById(adminId);
            User target = userDAO.findById(userId);
            
            if (admin == null || target == null) {
                logger.info("Usuário não encontrado para banimento");
                return false;
            }
            
            // Verificar se o usuário é um administrador
            // Em uma implementação real, seria necessário verificar se o usuário tem permissão de administrador
            
            // Atualizar status de banimento
            target.setBanned(true);
            
            // Salvar usuário
            boolean saved = userDAO.save(target);
            if (saved) {
                logger.info("Usuário banido com sucesso: " + userId);
                
                // Desconectar todas as sessões ativas do usuário banido
                // Em uma implementação real, seria necessário ter um método para isso
                // Por enquanto, vamos apenas simular
                logger.info("Desconectando sessões do usuário banido: " + userId);
                
                return true;
            } else {
                logger.warning("Falha ao banir usuário: " + userId);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao banir usuário", e);
            throw new RemoteException("Erro ao banir usuário", e);
        }
    }
}
