package br.com.whatsut.impl;

import br.com.whatsut.dao.SessionDAO;
import br.com.whatsut.dao.UserDAO;
import br.com.whatsut.model.Session;
import br.com.whatsut.model.User;
import br.com.whatsut.service.AuthService;
import br.com.whatsut.util.ConfigManager;
import br.com.whatsut.util.SecurityUtils;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementação do serviço de autenticação.
 */
public class AuthServiceImpl extends UnicastRemoteObject implements AuthService {
    private static final Logger logger = Logger.getLogger(AuthServiceImpl.class.getName());
    private final UserDAO userDAO;
    private final SessionDAO sessionDAO;
    private final long sessionExpirationTime;
    
    public AuthServiceImpl() throws RemoteException {
        super();
        this.userDAO = new UserDAO();
        this.sessionDAO = new SessionDAO();
        this.sessionExpirationTime = ConfigManager.getLongProperty("security.token.expiration", 3600000);
    }
    
    @Override
    public User register(String username, String displayName, String password, String email) throws RemoteException {
        try {
            // Verificar se o usuário já existe
            User existingUser = userDAO.findByUsername(username);
            if (existingUser != null) {
                logger.info("Tentativa de registro com username já existente: " + username);
                return null;
            }
            
            // Criar novo usuário
            User newUser = new User();
            newUser.setUserId(UUID.randomUUID().toString());
            newUser.setUsername(username);
            newUser.setDisplayName(displayName);
            newUser.setEmail(email);
            
            // Hash da senha
            String passwordHash = SecurityUtils.hashPassword(password);
            newUser.setPasswordHash(passwordHash);
            
            // Salvar usuário
            boolean saved = userDAO.save(newUser);
            if (saved) {
                logger.info("Novo usuário registrado: " + username);
                return newUser;
            } else {
                logger.warning("Falha ao salvar novo usuário: " + username);
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao registrar usuário", e);
            throw new RemoteException("Erro ao registrar usuário", e);
        }
    }
    
    @Override
    public Session login(String username, String password, String clientAddress) throws RemoteException {
        try {
            // Buscar usuário pelo username
            User user = userDAO.findByUsername(username);
            if (user == null) {
                logger.info("Tentativa de login com username inexistente: " + username);
                return null;
            }
            
            // Verificar senha
            if (!SecurityUtils.checkPassword(password, user.getPasswordHash())) {
                logger.info("Senha incorreta para usuário: " + username);
                return null;
            }
            
            // Criar nova sessão
            String sessionId = UUID.randomUUID().toString();
            String token = SecurityUtils.generateToken();
            
            Session session = new Session(sessionId, user.getUserId(), token, clientAddress, sessionExpirationTime);
            
            // Salvar sessão
            boolean saved = sessionDAO.save(session);
            if (saved) {
                // Atualizar status do usuário para online
                user.setOnline(true);
                user.setLastSeen(System.currentTimeMillis());
                userDAO.save(user);
                
                logger.info("Login bem-sucedido para usuário: " + username);
                return session;
            } else {
                logger.warning("Falha ao salvar sessão para usuário: " + username);
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao realizar login", e);
            throw new RemoteException("Erro ao realizar login", e);
        }
    }
    
    @Override
    public boolean validateToken(String sessionId, String token) throws RemoteException {
        try {
            Session session = sessionDAO.findById(sessionId);
            if (session == null) {
                logger.info("Sessão não encontrada: " + sessionId);
                return false;
            }
            
            if (!session.isValid()) {
                logger.info("Sessão expirada: " + sessionId);
                sessionDAO.delete(sessionId);
                return false;
            }
            
            if (!session.getToken().equals(token)) {
                logger.warning("Token inválido para sessão: " + sessionId);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao validar token", e);
            throw new RemoteException("Erro ao validar token", e);
        }
    }
    
    @Override
    public boolean logout(String sessionId) throws RemoteException {
        try {
            Session session = sessionDAO.findById(sessionId);
            if (session == null) {
                logger.info("Tentativa de logout com sessão inexistente: " + sessionId);
                return false;
            }
            
            // Atualizar status do usuário para offline
            User user = userDAO.findById(session.getUserId());
            if (user != null) {
                user.setOnline(false);
                user.setLastSeen(System.currentTimeMillis());
                userDAO.save(user);
            }
            
            // Remover sessão
            boolean removed = sessionDAO.delete(sessionId);
            if (removed) {
                logger.info("Logout bem-sucedido para sessão: " + sessionId);
                return true;
            } else {
                logger.warning("Falha ao remover sessão: " + sessionId);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao realizar logout", e);
            throw new RemoteException("Erro ao realizar logout", e);
        }
    }
    
    @Override
    public boolean changePassword(String userId, String currentPassword, String newPassword) throws RemoteException {
        try {
            User user = userDAO.findById(userId);
            if (user == null) {
                logger.info("Usuário não encontrado para alteração de senha: " + userId);
                return false;
            }
            
            // Verificar senha atual
            if (!SecurityUtils.checkPassword(currentPassword, user.getPasswordHash())) {
                logger.info("Senha atual incorreta para usuário: " + userId);
                return false;
            }
            
            // Gerar novo hash de senha
            String newPasswordHash = SecurityUtils.hashPassword(newPassword);
            user.setPasswordHash(newPasswordHash);
            
            // Salvar usuário
            boolean saved = userDAO.save(user);
            if (saved) {
                logger.info("Senha alterada com sucesso para usuário: " + userId);
                return true;
            } else {
                logger.warning("Falha ao salvar nova senha para usuário: " + userId);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao alterar senha", e);
            throw new RemoteException("Erro ao alterar senha", e);
        }
    }
}
