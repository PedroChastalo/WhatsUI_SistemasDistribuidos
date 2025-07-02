package br.com.whatsut.server;

import br.com.whatsut.impl.AuthenticationServiceImpl;
import br.com.whatsut.impl.GroupServiceImpl;
import br.com.whatsut.impl.MessageServiceImpl;
import br.com.whatsut.impl.UserServiceImpl;
import br.com.whatsut.service.AuthenticationService;
import br.com.whatsut.service.GroupService;
import br.com.whatsut.service.MessageService;
import br.com.whatsut.service.UserService;
import br.com.whatsut.websocket.WhatsUTWebSocketServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Classe principal para inicialização do servidor WhatsUT.
 * Responsável por registrar os serviços RMI e iniciar o servidor WebSocket.
 */
public class WhatsUTServer {
    // Instância do servidor WebSocket
    private static WhatsUTWebSocketServer webSocketServer;
    // Registro RMI
    private static Registry registry;
    
    /**
     * Ponto de entrada da aplicação do servidor.
     * Inicializa e registra os serviços RMI e inicia o WebSocket.
     */
    public static void main(String[] args) {
        try {
            System.out.println("Iniciando servidor WhatsUT...");
            
            // Criar instâncias dos serviços
            AuthenticationServiceImpl authService = new AuthenticationServiceImpl();
            UserServiceImpl userService = new UserServiceImpl();
            MessageServiceImpl messageService = new MessageServiceImpl();
            GroupServiceImpl groupService = new GroupServiceImpl();
            
            // Exportar objetos remotos
            AuthenticationService authStub = (AuthenticationService) UnicastRemoteObject.exportObject(authService, 0);
            UserService userStub = (UserService) UnicastRemoteObject.exportObject(userService, 0);
            MessageService messageStub = (MessageService) UnicastRemoteObject.exportObject(messageService, 0);
            GroupService groupStub = (GroupService) UnicastRemoteObject.exportObject(groupService, 0);
            
            // Criar e obter o registro
            registry = LocateRegistry.createRegistry(1099);
            
            // Registrar os serviços no registro
            registry.rebind("AuthenticationService", authStub);
            System.out.println("Serviço RMI de autenticação registrado com sucesso!");
            
            registry.rebind("UserService", userStub);
            System.out.println("Serviço RMI de usuários registrado com sucesso!");
            
            registry.rebind("MessageService", messageStub);
            System.out.println("Serviço RMI de mensagens registrado com sucesso!");
            
            registry.rebind("GroupService", groupStub);
            System.out.println("Serviço RMI de grupos registrado com sucesso!");
            
            // Aguardar um momento para garantir que os serviços estejam disponíveis
            Thread.sleep(1000);
            
            // Iniciar o servidor WebSocket
            webSocketServer = new WhatsUTWebSocketServer(8080);
            webSocketServer.start();
            
            System.out.println("Servidor WebSocket iniciado na porta 8080");
            System.out.println("Servidor WhatsUT está pronto para receber conexões!");
            
            // Adicionar shutdown hook para fechar corretamente o servidor quando for encerrado
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Encerrando servidor WhatsUT...");
                try {
                    if (webSocketServer != null) {
                        webSocketServer.stop(1000);
                        System.out.println("Servidor WebSocket encerrado.");
                    }
                    
                    if (registry != null) {
                        UnicastRemoteObject.unexportObject(registry, true);
                        System.out.println("Registro RMI encerrado.");
                    }
                    
                    System.out.println("Servidor WhatsUT encerrado com sucesso!");
                } catch (Exception e) {
                    System.err.println("Erro ao encerrar o servidor: " + e.getMessage());
                }
            }));
            
        } catch (Exception e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
