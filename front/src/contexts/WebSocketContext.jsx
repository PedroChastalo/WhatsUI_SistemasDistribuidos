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
    store.initializeWebSocket();
    
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
    allGroups: store.allGroups,
    availableGroups: store.availableGroups,
    groupMessages: store.groupMessages,
    isLoading: store.isLoading,
    notifications: store.notifications,
    conversations: store.privateConversations,
    cachedGroupMembers: store.cachedGroupMembers,
    pendingGroupRequests: store.pendingGroupRequests,
    
    // Estado de erros
    loginError: store.loginError,
    fetchError: store.fetchError,
    connectionError: store.connectionError,
    
    // Funções de gerenciamento de cache
    clearGroupMessageCache: store.clearGroupMessageCache,
    clearPrivateMessageCache: store.clearPrivateMessageCache,
    
    // Funções para gerenciamento de notificações
    addNotification: store.addNotification,
    clearNotification: store.clearNotification,
    clearAllNotifications: store.clearAllNotifications,
    
    // Ações de autenticação
    login: store.login,
    register: store.register,
    logout: store.logout,
    
    // Ações de usuários
    fetchUsers: store.fetchUsers,
    updateStatus: store.updateStatus,
    getUsers: store.fetchUsers,
    
    // Ações de mensagens privadas
    fetchPrivateConversations: store.fetchPrivateConversations,
    fetchPrivateMessages: store.fetchPrivateMessages,
    sendPrivateMessage: store.sendPrivateMessage,
    sendPrivateFile: store.sendPrivateFile,
    
    // Ações de grupos
    fetchGroups: store.fetchGroups,
    fetchAllGroups: store.fetchAllGroups,
    fetchAllAvailableGroups: store.fetchAllAvailableGroups,
    createGroup: store.createGroup,
    fetchGroupMessages: store.fetchGroupMessages,
    sendGroupMessage: store.sendGroupMessage,
    sendGroupFile: store.sendGroupFile,
    getGroups: store.fetchGroups,
    getAllGroups: store.fetchAllGroups,
    getConversations: store.fetchPrivateConversations,
    clearGroupMembersCache: store.clearGroupMembersCache,
    
    // Ações de solicitações de grupo
    requestJoinGroup: store.requestJoinGroup,
    respondToGroupRequest: store.respondToGroupRequest,
    fetchPendingGroupRequests: store.fetchPendingGroupRequests,
    getPendingGroupRequests: store.fetchPendingGroupRequests,
    

    
    // Funções para o componente Chat
    getMessages: async (chatId, chatType) => {
      if (!chatId) {
        console.error('getMessages chamado sem chatId');
        return [];
      }
      
      try {
        // Se chatType não for fornecido, assumir que é um chat privado (user)
        const normalizedType = chatType || 'user';
        
        if (normalizedType === 'user' || normalizedType === 'private') {
          console.log(`Buscando mensagens privadas para usuário ${chatId}`);
          return await store.fetchPrivateMessages(chatId);
        } else if (normalizedType === 'group') {
          console.log(`Buscando mensagens de grupo para ${chatId}`);
          return await store.fetchGroupMessages(chatId);
        } else {
          console.warn(`Tipo de chat desconhecido: ${normalizedType}, assumindo chat privado`);
          return await store.fetchPrivateMessages(chatId);
        }
      } catch (error) {
        console.error(`Erro ao buscar mensagens para ${chatType || 'user'} ${chatId}:`, error);
        return [];
      }
    },
    sendMessage: async (message) => {
      if (!message || !message.recipientId || !message.content) {
        console.error('sendMessage chamado com parâmetros inválidos:', message);
        throw new Error('Parâmetros inválidos para enviar mensagem');
      }
      
      try {
        // Usar o recipientId como userId para mensagens privadas
        // ou como groupId para mensagens de grupo
        if (message.type === 'user') {
          // Passar o recipientId como userId para o método sendPrivateMessage
          return await store.sendPrivateMessage(message.recipientId, message.content);
        } else if (message.type === 'group') {
          return await store.sendGroupMessage(message.recipientId, message.content);
        } else {
          console.error(`Tipo de destinatário inválido: ${message.type}`);
          throw new Error(`Tipo de destinatário inválido: ${message.type}`);
        }
      } catch (error) {
        console.error(`Erro ao enviar mensagem para ${message.type} ${message.recipientId}:`, error);
        throw error; // Propagar o erro para que o componente possa mostrar feedback ao usuário
      }
    },
    sendFile: async (file, recipientId, type, fileType) => {
      if (!file || !recipientId || !type) {
        console.error('sendFile chamado com parâmetros inválidos:', { file, recipientId, type });
        throw new Error('Parâmetros inválidos para enviar arquivo');
      }
      
      try {
        if (type === 'user') {
          return await store.sendPrivateFile(recipientId, file, fileType);
        } else if (type === 'group') {
          return await store.sendGroupFile(recipientId, file, fileType);
        } else {
          console.error(`Tipo de destinatário inválido: ${type}`);
          throw new Error(`Tipo de destinatário inválido: ${type}`);
        }
      } catch (error) {
        console.error(`Erro ao enviar arquivo para ${type} ${recipientId}:`, error);
        throw error; // Propagar o erro para que o componente possa mostrar feedback ao usuário
      }
    },
    
    // A função sendTypingStatus foi removida
    
    getGroupMembers: async (groupId) => {
      if (!groupId) {
        console.error('getGroupMembers chamado sem groupId');
        return [];
      }
      
      try {
        return await store.getGroupMembers(groupId);
      } catch (error) {
        console.error(`Erro ao buscar membros do grupo ${groupId}:`, error);
        return [];
      }
    },
    
    addUserToGroup: async (groupId, userId) => {
      if (!groupId || !userId) {
        console.error('addUserToGroup chamado com parâmetros inválidos:', { groupId, userId });
        throw new Error('Parâmetros inválidos para adicionar usuário ao grupo');
      }
      
      try {
        return await store.addUserToGroup(groupId, userId);
      } catch (error) {
        console.error(`Erro ao adicionar usuário ${userId} ao grupo ${groupId}:`, error);
        throw error;
      }
    },
    
    removeUserFromGroup: async (groupId, userId) => {
      if (!groupId || !userId) {
        console.error('removeUserFromGroup chamado com parâmetros inválidos:', { groupId, userId });
        throw new Error('Parâmetros inválidos para remover usuário do grupo');
      }
      
      try {
        return await store.removeUserFromGroup(groupId, userId);
      } catch (error) {
        console.error(`Erro ao remover usuário ${userId} do grupo ${groupId}:`, error);
        throw error;
      }
    },
    
    setGroupAdmin: async (groupId, userId) => {
      if (!groupId || !userId) {
        console.error('setGroupAdmin chamado com parâmetros inválidos:', { groupId, userId });
        throw new Error('Parâmetros inválidos para definir administrador do grupo');
      }
      
      try {
        return await store.setGroupAdmin(groupId, userId);
      } catch (error) {
        console.error(`Erro ao definir usuário ${userId} como admin do grupo ${groupId}:`, error);
        throw error;
      }
    },
    leaveGroup: async (groupId, deleteIfAdmin = false) => {
      if (!groupId) {
        console.error('leaveGroup chamado sem groupId');
        throw new Error('Parâmetros inválidos para sair do grupo');
      }
      
      try {
        // Se deleteIfAdmin for true, chamar deleteGroup em vez de leaveGroup
        if (deleteIfAdmin) {
          return await store.deleteGroup(groupId);
        } else {
          return await store.leaveGroup(groupId);
        }
      } catch (error) {
        console.error(`Erro ao sair do grupo ${groupId}:`, error);
        throw error;
      }
    },
    
    deleteGroup: async (groupId) => {
      if (!groupId) {
        console.error('deleteGroup chamado sem groupId');
        throw new Error('Parâmetros inválidos para deletar grupo');
      }
      
      try {
        return await store.deleteGroup(groupId);
      } catch (error) {
        console.error(`Erro ao deletar grupo ${groupId}:`, error);
        throw error;
      }
    },
    
    getUser: async (userId) => {
      if (!userId) {
        console.error('getUser chamado sem userId');
        throw new Error('ID de usuário não fornecido');
      }
      
      try {
        return await store.getUser(userId);
      } catch (error) {
        console.error(`Erro ao buscar usuário ${userId}:`, error);
        throw error;
      }
    },
    
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
