// WebSocketContext.jsx
// Fornece acesso ao WebSocket e estado global em toda a aplicação

import React, { createContext, useContext, useEffect } from 'react';
import { useWebSocketStore } from '@/stores/websocketStore';
import websocketClient from '@/lib/websocket';

// Criar contexto
const WebSocketContext = createContext(null);

// Hook personalizado para usar o contexto
export const useWebSocket = () => {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocket deve ser usado dentro de um WebSocketProvider');
  }
  return context;
};

// Provedor do contexto
export const WebSocketProvider = ({ children }) => {
  // Obter estado e ações do store Zustand
  const store = useWebSocketStore();
  
  // Inicializar WebSocket na montagem do componente
  useEffect(() => {
    // Iniciar conexão WebSocket
    websocketClient.connect();
    
    // Limpar na desmontagem
    return () => {
      websocketClient.disconnect();
    };
  }, []);
  
  // Valor do contexto
  const contextValue = {
    // Estado
    isConnected: store.isConnected,
    isAuthenticated: store.isAuthenticated,
    currentUser: store.currentUser,
    users: store.users,
    userStatus: store.userStatus,
    privateConversations: store.privateConversations,
    privateMessages: store.privateMessages,
    groups: store.groups,
    groupMessages: store.groupMessages,
    isLoading: store.isLoading,
    notifications: store.notifications,
    conversations: store.privateConversations,
    
    // Ações de autenticação
    login: store.login,
    register: store.register,
    logout: store.logout,
    
    // Ações de usuários
    fetchUsers: store.fetchUsers,
    updateStatus: store.updateStatus,
    getUser: store.getUser || ((userId) => store.users.find(u => u.userId === userId)),
    getUsers: store.fetchUsers,
    
    // Ações de mensagens privadas
    fetchPrivateConversations: store.fetchPrivateConversations,
    fetchPrivateMessages: store.fetchPrivateMessages,
    sendPrivateMessage: store.sendPrivateMessage,
    sendPrivateFile: store.sendPrivateFile,
    
    // Ações de grupos
    fetchGroups: store.fetchGroups,
    createGroup: store.createGroup,
    fetchGroupMessages: store.fetchGroupMessages,
    sendGroupMessage: store.sendGroupMessage,
    sendGroupFile: store.sendGroupFile,
    getGroups: store.fetchGroups,
    getConversations: store.fetchPrivateConversations,
    
    // Funções para o componente Chat
    getMessages: (chatId, chatType) => {
      if (chatType === 'user') {
        return store.fetchPrivateMessages(chatId);
      } else if (chatType === 'group') {
        return store.fetchGroupMessages(chatId);
      }
      return [];
    },
    sendMessage: (message) => {
      if (message.type === 'user') {
        return store.sendPrivateMessage(message.recipientId, message.content);
      } else if (message.type === 'group') {
        return store.sendGroupMessage(message.recipientId, message.content);
      }
      return null;
    },
    sendFile: (file, recipientId, type, fileType) => {
      if (type === 'user') {
        return store.sendPrivateFile(recipientId, file, fileType);
      } else if (type === 'group') {
        return store.sendGroupFile(recipientId, file, fileType);
      }
      return Promise.reject(new Error('Tipo de chat inválido'));
    },
    sendTypingStatus: () => {}, // Implementação futura
    getGroupMembers: (groupId) => {
      return store.getGroupMembers(groupId);
    },
    addUserToGroup: (groupId, userId) => {
      return store.addUserToGroup ? store.addUserToGroup(groupId, userId) : Promise.resolve({ success: true });
    },
    removeUserFromGroup: (groupId, userId) => {
      return store.removeUserFromGroup ? store.removeUserFromGroup(groupId, userId) : Promise.resolve({ success: true });
    },
    setGroupAdmin: (groupId, userId) => {
      return store.setGroupAdmin ? store.setGroupAdmin(groupId, userId) : Promise.resolve({ success: true });
    },
    leaveGroup: (groupId, deleteIfAdmin = false) => {
      return store.leaveGroup ? store.leaveGroup(groupId, deleteIfAdmin) : Promise.resolve({ success: true });
    },
    deleteGroup: (groupId) => {
      return store.deleteGroup ? store.deleteGroup(groupId) : Promise.resolve({ success: true });
    },
    
    // Notificações
    removeNotification: store.removeNotification,
    clearNotifications: store.clearNotifications,
    
    // Acesso direto ao cliente WebSocket para casos especiais
    websocketClient
  };
  
  return (
    <WebSocketContext.Provider value={contextValue}>
      {children}
    </WebSocketContext.Provider>
  );
};

export default WebSocketProvider;
