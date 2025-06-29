package br.com.whatsut.server;

import br.com.whatsut.impl.AuthServiceImpl;
import br.com.whatsut.impl.GroupServiceImpl;
import br.com.whatsut.impl.MessageServiceImpl;
import br.com.whatsut.impl.UserServiceImpl;
import br.com.whatsut.service.AuthService;
import br.com.whatsut.service.GroupService;
import br.com.whatsut.service.MessageService;
import br.com.whatsut.service.UserService;
import br.com.whatsut.util.ConfigManager;
import br.com.whatsut.websocket.WhatsUTWebSocketServer;

import java.io.File;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe principal do servidor WhatsUT.
 * Responsável por iniciar o servidor RMI e o servidor WebSocket.
 */
public class WhatsUTServer {
    private static final Logger logger = Logger.getLogger(WhatsUTServer.class.getName());
    private Registry registry;
    private WhatsUTWebSocketServer webSocketServer;
    
    /**
     * Construtor do servidor WhatsUT.
     */
    public WhatsUTServer() {
        // Inicializar diretórios necessários
        initializeDirectories();
    }
    
    /**
     * Inicializa os diretórios necessários para o servidor.
     */
    private void initializeDirectories() {
        try {
            // Obter caminhos de armazenamento
            String userStoragePath = ConfigManager.getProperty("storage.users.path", "data/users");
            String groupStoragePath = ConfigManager.getProperty("storage.groups.path", "data/groups");
            String messageStoragePath = ConfigManager.getProperty("storage.messages.path", "data/messages");
            String sessionStoragePath = ConfigManager.getProperty("storage.sessions.path", "data/sessions");
            
            // Criar diretórios se não existirem
            createDirectoryIfNotExists(userStoragePath);
            createDirectoryIfNotExists(groupStoragePath);
            createDirectoryIfNotExists(messageStoragePath);
            createDirectoryIfNotExists(sessionStoragePath);
            
            logger.info("Diretórios de armazenamento inicializados com sucesso");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao inicializar diretórios de armazenamento", e);
        }
    }
    
    /**
     * Cria um diretório se não existir.
     *
     * @param path Caminho do diretório
     */
    private void createDirectoryIfNotExists(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                logger.info("Diretório criado: " + path);
            } else {
                logger.warning("Falha ao criar diretório: " + path);
            }
        }
    }
    
    /**
     * Inicia o servidor RMI.
     */
    public void startRmiServer() {
        try {
            // Nota: O SecurityManager foi removido pois está obsoleto desde Java 17
            // A configuração de segurança agora deve ser feita através de outros mecanismos
            // como permissões de arquivo do sistema operacional e configurações de rede
            
            // Obter configurações do servidor RMI
            String host = ConfigManager.getProperty("server.host", "localhost");
            int port = ConfigManager.getIntProperty("server.rmi.port", 1099);
            
            // Criar ou obter o registro RMI
            try {
                registry = LocateRegistry.createRegistry(port);
                logger.info("Registro RMI criado na porta " + port);
            } catch (Exception e) {
                logger.info("Registro RMI já existe, obtendo referência");
                registry = LocateRegistry.getRegistry(host, port);
            }
            
            // Criar e registrar os serviços RMI
            AuthService authService = new AuthServiceImpl();
            UserService userService = new UserServiceImpl();
            GroupService groupService = new GroupServiceImpl();
            MessageService messageService = new MessageServiceImpl();
            
            registry.rebind("AuthService", authService);
            registry.rebind("UserService", userService);
            registry.rebind("GroupService", groupService);
            registry.rebind("MessageService", messageService);
            
            logger.info("Serviços RMI registrados com sucesso");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao iniciar servidor RMI", e);
        }
    }
    
    /**
     * Inicia o servidor WebSocket.
     */
    public void startWebSocketServer() {
        try {
            int port = ConfigManager.getIntProperty("server.websocket.port", 8080);
            webSocketServer = new WhatsUTWebSocketServer(port);
            webSocketServer.start();
            logger.info("Servidor WebSocket iniciado na porta " + port);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao iniciar servidor WebSocket", e);
        }
    }
    
    /**
     * Para o servidor.
     */
    public void stop() {
        try {
            if (webSocketServer != null) {
                webSocketServer.stop();
                logger.info("Servidor WebSocket parado");
            }
            
            // Não é possível parar o registro RMI diretamente
            // Em uma implementação real, seria necessário desregistrar os serviços
            
            logger.info("Servidor WhatsUT parado");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao parar servidor", e);
        }
    }
    
    /**
     * Método principal para iniciar o servidor.
     *
     * @param args Argumentos da linha de comando
     */
    public static void main(String[] args) {
        try {
            // Inicializar o gerenciador de configurações
            ConfigManager.init();
            
            // Criar e iniciar o servidor
            final WhatsUTServer server = new WhatsUTServer();
            server.startRmiServer();
            server.startWebSocketServer();
            
            logger.info("Servidor WhatsUT iniciado com sucesso");
            
            // Adicionar hook de desligamento
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Desligando servidor WhatsUT...");
                server.stop();
            }));
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao iniciar servidor WhatsUT", e);
        }
    }
}
