// WebSocket Store usando Zustand
// Gerencia o estado global relacionado à conexão WebSocket e eventos

import { create } from 'zustand';
import websocketClient from '@/lib/websocket';

export const useWebSocketStore = create((set, get) => ({
  // Estado de conexão
  isConnected: false,
  connectionError: null,
  
  // Estado de autenticação
  isAuthenticated: false,
  currentUser: null,
  sessionId: null,
  
  // Dados do usuário
  users: [],
  userStatus: {},
  
  // Conversas e mensagens
  privateConversations: [],
  privateMessages: {},
  groups: [],
  groupMessages: {},
  cachedGroupMembers: {},
  
  // Estado da UI
  isLoading: false,
  notifications: [],
  
  // Funções de gerenciamento de cache
  clearGroupCache: (groupId) => {
    if (!groupId) return;
    
    set(state => ({
      cachedGroupMembers: {
        ...state.cachedGroupMembers,
        [groupId]: undefined
      },
      groupMessages: {
        ...state.groupMessages,
        [groupId]: undefined
      }
    }));
  },
  
  clearPrivateMessageCache: (userId) => {
    if (!userId) return;
    
    set(state => ({
      privateMessages: {
        ...state.privateMessages,
        [userId]: undefined
      }
    }));
  },
  
  // Ações de conexão
  setConnectionStatus: (isConnected) => set({ isConnected }),
  
  // Inicializar conexão WebSocket
  initializeWebSocket: () => {
    websocketClient.connect();
  },
  
  // Autenticação
  login: async (username, password) => {
    set({ isLoading: true });
    
    try {
      const response = await websocketClient.sendRequest('login', { email: username, password });
      
      set({
        isAuthenticated: true,
        currentUser: response.user,
        sessionId: response.sessionId,
        isLoading: false
      });
      
      // Carregar dados iniciais após login
      get().loadInitialData();
      
      return response;
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },
  
  register: async (userData) => {
    set({ isLoading: true });
    
    try {
      const response = await websocketClient.sendRequest('register', userData);
      set({ isLoading: false });
      return response;
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },
  
  logout: async () => {
    set({ isLoading: true });
    
    try {
      if (get().sessionId) {
        await websocketClient.sendRequest('logout', { sessionId: get().sessionId });
      }
      
      // Limpar estado
      set({
        isAuthenticated: false,
        currentUser: null,
        sessionId: null,
        users: [],
        privateConversations: [],
        privateMessages: {},
        groups: [],
        groupMessages: {},
        isLoading: false
      });
      
      return true;
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },
  
  // Carregar dados iniciais após login
  loadInitialData: async () => {
    const { sessionId } = get();
    if (!sessionId) return;
    
    // Carregar usuários
    get().fetchUsers();
    
    // Carregar conversas privadas
    get().fetchPrivateConversations();
    
    // Carregar grupos
    get().fetchGroups();
  },
  
  // Ações de usuários
  fetchUsers: async () => {
    const { sessionId } = get();
    if (!sessionId) return;
    
    try {
      const response = await websocketClient.sendRequest('getUsers', { sessionId });
      
      // Mapear status de usuários para acesso rápido
      const userStatus = {};
      response.users.forEach(user => {
        userStatus[user.userId] = user.status;
      });
      
      set({
        users: response.users,
        userStatus
      });
      
      return response.users;
    } catch (error) {
      console.error('Erro ao buscar usuários:', error);
      return [];
    }
  },
  
  updateStatus: async (status) => {
    const { sessionId, currentUser } = get();
    if (!sessionId || !currentUser) return;
    
    try {
      await websocketClient.sendRequest('updateStatus', { sessionId, status });
      
      // Atualizar status local
      set(state => ({
        userStatus: {
          ...state.userStatus,
          [currentUser.userId]: status
        }
      }));
      
      return true;
    } catch (error) {
      console.error('Erro ao atualizar status:', error);
      return false;
    }
  },
  
  // Ações de mensagens privadas
  fetchPrivateConversations: async () => {
    const { sessionId } = get();
    if (!sessionId) return;
    
    try {
      const response = await websocketClient.sendRequest('getPrivateConversations', { sessionId });
      set({ privateConversations: response.conversations });
      return response.conversations;
    } catch (error) {
      console.error('Erro ao buscar conversas privadas:', error);
      return [];
    }
  },
  
  fetchPrivateMessages: async (otherUserId) => {
    const { sessionId, privateMessages } = get();
    if (!sessionId) return [];
    
    try {
      // Verificar se já temos as mensagens em cache
      if (privateMessages[otherUserId] && privateMessages[otherUserId].length > 0) {
        // Se já tivermos mensagens em cache, retornar diretamente
        return privateMessages[otherUserId];
      }
      
      const response = await websocketClient.sendRequest('getPrivateMessages', { 
        sessionId, 
        otherUserId,
        limit: 50
      });
      
      // Verificar se a resposta é válida
      if (!response || !response.messages) {
        return [];
      }
      
      // Atualizar mensagens no estado
      set({
        privateMessages: {
          ...privateMessages,
          [otherUserId]: response.messages
        }
      });
      
      return response.messages;
    } catch (error) {
      console.error(`Erro ao buscar mensagens com usuário ${otherUserId}:`, error);
      return [];
    }
  },
  
  sendPrivateMessage: async (receiverId, content) => {
    const { sessionId, currentUser } = get();
    if (!sessionId || !currentUser || !receiverId || !content) {
      throw new Error('Parâmetros inválidos');
    }
    
    try {
      const response = await websocketClient.sendRequest('sendPrivateMessage', {
        sessionId,
        receiverId,
        content
      });
      
      // Adicionar mensagem ao histórico local
      set(state => {
        const currentMessages = state.privateMessages[receiverId] || [];
        
        return {
          privateMessages: {
            ...state.privateMessages,
            [receiverId]: [...currentMessages, {
              messageId: response.messageId,
              senderId: currentUser.userId,
              senderName: currentUser.displayName,
              content,
              timestamp: response.timestamp,
              status: 'sent',
              isOwn: true,
              isFile: false
            }]
          }
        };
      });
      
      // Atualizar última mensagem na conversa
      get().updateConversationLastMessage(receiverId, content, response.timestamp);
      
      return response;
    } catch (error) {
      console.error('Erro ao enviar mensagem privada:', error);
      throw error;
    }
  },
  
  sendPrivateFile: async (receiverId, file, fileType = null) => {
    const { sessionId, currentUser } = get();
    if (!sessionId || !currentUser || !receiverId || !file) {
      throw new Error('Parâmetros inválidos');
    }
    
    try {
      // Criar FormData para envio do arquivo
      const formData = new FormData();
      formData.append('file', file);
      formData.append('sessionId', sessionId);
      formData.append('receiverId', receiverId);
      if (fileType) {
        formData.append('fileType', fileType);
      }
      
      // Em modo mock, simular envio
      if (websocketClient.mockMode) {
        const mockResponse = {
          messageId: 'file_' + Date.now(),
          timestamp: Date.now(),
          fileName: file.name,
          fileUrl: URL.createObjectURL(file),
          fileType: fileType || file.type
        };
        
        // Adicionar mensagem ao histórico local
        set(state => {
          const currentMessages = state.privateMessages[receiverId] || [];
          
          return {
            privateMessages: {
              ...state.privateMessages,
              [receiverId]: [...currentMessages, {
                messageId: mockResponse.messageId,
                senderId: currentUser.userId,
                senderName: currentUser.displayName,
                content: `Arquivo: ${file.name}`,
                fileName: file.name,
                fileUrl: mockResponse.fileUrl,
                fileType: mockResponse.fileType,
                timestamp: mockResponse.timestamp,
                status: 'sent',
                isOwn: true,
                isFile: true
              }]
            }
          };
        });
        
        // Atualizar última mensagem na conversa
        get().updateConversationLastMessage(receiverId, `Arquivo: ${file.name}`, mockResponse.timestamp);
        
        return mockResponse;
      }
      
      // Enviar para o servidor via API REST (não WebSocket)
      const response = await fetch('/api/messages/private/file', {
        method: 'POST',
        body: formData
      });
      
      if (!response.ok) {
        throw new Error(`Erro ao enviar arquivo: ${response.statusText}`);
      }
      
      const result = await response.json();
      
      // Adicionar mensagem ao histórico local
      set(state => {
        const currentMessages = state.privateMessages[receiverId] || [];
        
        return {
          privateMessages: {
            ...state.privateMessages,
            [receiverId]: [...currentMessages, {
              messageId: result.messageId,
              senderId: currentUser.userId,
              senderName: currentUser.displayName,
              content: `Arquivo: ${file.name}`,
              fileName: file.name,
              fileUrl: result.fileUrl,
              fileType: result.fileType,
              timestamp: result.timestamp,
              status: 'sent',
              isOwn: true,
              isFile: true
            }]
          }
        };
      });
      
      // Atualizar última mensagem na conversa
      get().updateConversationLastMessage(receiverId, `Arquivo: ${file.name}`, result.timestamp);
      
      return result;
    } catch (error) {
      console.error('Erro ao enviar arquivo em mensagem privada:', error);
      throw error;
    }
  },
  
  updateConversationLastMessage: (userId, content, timestamp) => {
    set(state => {
      const conversations = [...state.privateConversations];
      const existingIndex = conversations.findIndex(c => c.userId === userId);
      
      if (existingIndex >= 0) {
        // Atualizar conversa existente
        conversations[existingIndex] = {
          ...conversations[existingIndex],
          lastMessage: content,
          timestamp,
          unreadCount: 0 // Zerar contagem não lida para mensagens enviadas
        };
      } else {
        // Adicionar nova conversa
        const user = state.users.find(u => u.userId === userId);
        if (user) {
          conversations.push({
            userId,
            username: user.username,
            displayName: user.displayName,
            lastMessage: content,
            timestamp,
            unreadCount: 0
          });
        }
      }
      
      // Ordenar por timestamp mais recente
      conversations.sort((a, b) => b.timestamp - a.timestamp);
      
      return { privateConversations: conversations };
    });
  },
  
  // Ações de grupos
  fetchGroups: async () => {
    const { sessionId } = get();
    if (!sessionId) return;
    
    try {
      const response = await websocketClient.sendRequest('getGroups', { sessionId });
      set({ groups: response.groups });
      return response.groups;
    } catch (error) {
      console.error('Erro ao buscar grupos:', error);
      return [];
    }
  },
  
  getGroupMembers: async (groupId) => {
    const { sessionId } = get();
    if (!sessionId) return [];
    
    try {
      // Verificar se já temos os membros em cache para evitar requisições repetidas
      const cachedMembers = get().cachedGroupMembers?.[groupId];
      if (cachedMembers) {
        return cachedMembers;
      }
      
      const response = await websocketClient.sendRequest('getGroupMembers', { 
        sessionId,
        groupId 
      });
      
      if (!response || !response.members) {
        return [];
      }
      
      // Mapear os membros com informações adicionais dos usuários
      const users = get().users;
      const members = response.members.map(member => {
        const userInfo = users.find(u => u.userId === member.userId) || {};
        return {
          ...member,
          ...userInfo,
          name: userInfo.displayName || userInfo.username || member.name || 'Usuário',
          isAdmin: member.role === 'admin'
        };
      });
      
      // Armazenar em cache para evitar requisições repetidas
      set(state => ({
        cachedGroupMembers: {
          ...state.cachedGroupMembers,
          [groupId]: members
        }
      }));
      
      return members;
    } catch (error) {
      console.error('Erro ao buscar membros do grupo:', error);
      return [];
    }
  },
  
  createGroup: async (groupData) => {
    const { sessionId } = get();
    if (!sessionId) return;
    
    try {
      const response = await websocketClient.sendRequest('createGroup', {
        sessionId,
        ...groupData
      });
      
      // Atualizar lista de grupos
      await get().fetchGroups();
      
      return response;
    } catch (error) {
      console.error('Erro ao criar grupo:', error);
      throw error;
    }
  },
  
  fetchGroupMessages: async (groupId) => {
    const { sessionId, groupMessages } = get();
    if (!sessionId) return [];
    
    try {
      // Verificar se já temos as mensagens em cache
      if (groupMessages[groupId] && groupMessages[groupId].length > 0) {
        // Se já tivermos mensagens em cache, retornar diretamente
        return groupMessages[groupId];
      }
      
      const response = await websocketClient.sendRequest('getGroupMessages', { 
        sessionId, 
        groupId,
        limit: 50
      });
      
      // Verificar se a resposta é válida
      if (!response || !response.messages) {
        return [];
      }
      
      // Atualizar mensagens no estado
      set({
        groupMessages: {
          ...groupMessages,
          [groupId]: response.messages
        }
      });
      
      return response.messages;
    } catch (error) {
      console.error(`Erro ao buscar mensagens do grupo ${groupId}:`, error);
      return [];
    }
  },
  
  sendGroupMessage: async (groupId, content) => {
    const { sessionId, groupMessages, currentUser } = get();
    if (!sessionId || !currentUser) return;
    
    try {
      const response = await websocketClient.sendRequest('sendGroupMessage', {
        sessionId,
        groupId,
        content
      });
      
      // Adicionar mensagem localmente para feedback imediato
      const newMessage = {
        messageId: response.messageId,
        senderId: currentUser.userId,
        senderName: currentUser.displayName || currentUser.username,
        content,
        timestamp: response.timestamp,
        isFile: false,
        isOwn: true // Marcar como mensagem própria para exibição correta
      };
      
      set(state => {
        const currentMessages = state.groupMessages[groupId] || [];
        
        return {
          groupMessages: {
            ...state.groupMessages,
            [groupId]: [...currentMessages, newMessage]
          }
        };
      });
      
      // Atualizar também a lista de grupos
      get().updateGroupLastMessage(groupId, content, response.timestamp);
      
      return response;
    } catch (error) {
      console.error('Erro ao enviar mensagem para grupo:', error);
      throw error;
    }
  },
  
  updateGroupLastMessage: (groupId, content, timestamp) => {
    set(state => {
      const groups = state.groups.map(group => {
        if (group.groupId === groupId) {
          return {
            ...group,
            lastMessage: content,
            timestamp
          };
        }
        return group;
      });
      
      return { groups };
    });
  },
  
  // Enviar arquivo em mensagem de grupo
  sendGroupFile: async (groupId, file, fileType = null) => {
    const { sessionId, currentUser } = get();
    if (!sessionId || !currentUser || !groupId || !file) {
      throw new Error('Parâmetros inválidos');
    }
    
    try {
      // Criar FormData para envio do arquivo
      const formData = new FormData();
      formData.append('file', file);
      formData.append('sessionId', sessionId);
      formData.append('groupId', groupId);
      if (fileType) {
        formData.append('fileType', fileType);
      }
      
      // Em modo mock, simular envio
      if (websocketClient.mockMode) {
        const mockResponse = {
          messageId: 'file_group_' + Date.now(),
          timestamp: Date.now(),
          fileName: file.name,
          fileUrl: URL.createObjectURL(file),
          fileType: fileType || file.type
        };
        
        // Adicionar mensagem ao histórico local
        set(state => {
          const currentMessages = state.groupMessages[groupId] || [];
          
          return {
            groupMessages: {
              ...state.groupMessages,
              [groupId]: [...currentMessages, {
                messageId: mockResponse.messageId,
                senderId: currentUser.userId,
                senderName: currentUser.displayName,
                content: `Arquivo: ${file.name}`,
                fileName: file.name,
                fileUrl: mockResponse.fileUrl,
                fileType: mockResponse.fileType,
                timestamp: mockResponse.timestamp,
                status: 'sent',
                isOwn: true,
                isFile: true
              }]
            }
          };
        });
        
        // Atualizar última mensagem do grupo
        get().updateGroupLastMessage(groupId, `Arquivo: ${file.name}`, mockResponse.timestamp);
        
        return mockResponse;
      }
      
      // Enviar para o servidor via API REST (não WebSocket)
      const response = await fetch('/api/messages/group/file', {
        method: 'POST',
        body: formData
      });
      
      if (!response.ok) {
        throw new Error(`Erro ao enviar arquivo: ${response.statusText}`);
      }
      
      const result = await response.json();
      
      // Adicionar mensagem ao histórico local
      set(state => {
        const currentMessages = state.groupMessages[groupId] || [];
        
        return {
          groupMessages: {
            ...state.groupMessages,
            [groupId]: [...currentMessages, {
              messageId: result.messageId,
              senderId: currentUser.userId,
              senderName: currentUser.displayName,
              content: `Arquivo: ${file.name}`,
              fileName: file.name,
              fileUrl: result.fileUrl,
              fileType: result.fileType,
              timestamp: result.timestamp,
              status: 'sent',
              isOwn: true,
              isFile: true
            }]
          }
        };
      });
      
      // Atualizar última mensagem do grupo
      get().updateGroupLastMessage(groupId, `Arquivo: ${file.name}`, result.timestamp);
      
      return result;
    } catch (error) {
      console.error('Erro ao enviar arquivo em mensagem de grupo:', error);
      throw error;
    }
  },
  
  // Enviar arquivo em mensagem de grupo
  sendGroupFile: async (groupId, file, fileType = null) => {
    const { sessionId, currentUser } = get();
    if (!sessionId || !currentUser || !groupId || !file) {
      throw new Error('Parâmetros inválidos');
    }
    
    try {
      // Criar FormData para envio do arquivo
      const formData = new FormData();
      formData.append('file', file);
      formData.append('sessionId', sessionId);
      formData.append('groupId', groupId);
      if (fileType) {
        formData.append('fileType', fileType);
      }
      
      // Em modo mock, simular envio
      if (websocketClient.mockMode) {
        const mockResponse = {
          messageId: 'file_group_' + Date.now(),
          timestamp: Date.now(),
          fileName: file.name,
          fileUrl: URL.createObjectURL(file),
          fileType: fileType || file.type
        };
        
        // Adicionar mensagem ao histórico local
        set(state => {
          const currentMessages = state.groupMessages[groupId] || [];
          
          return {
            groupMessages: {
              ...state.groupMessages,
              [groupId]: [...currentMessages, {
                messageId: mockResponse.messageId,
                senderId: currentUser.userId,
                senderName: currentUser.displayName,
                content: `Arquivo: ${file.name}`,
                fileName: file.name,
                fileUrl: mockResponse.fileUrl,
                fileType: mockResponse.fileType,
                timestamp: mockResponse.timestamp,
                status: 'sent',
                isOwn: true,
                isFile: true
              }]
            }
          };
        });
        
        // Atualizar última mensagem do grupo
        get().updateGroupLastMessage(groupId, `Arquivo: ${file.name}`, mockResponse.timestamp);
        
        return mockResponse;
      }
      
      // Enviar para o servidor via API REST (não WebSocket)
      const response = await fetch('/api/messages/group/file', {
        method: 'POST',
        body: formData
      });
      
      if (!response.ok) {
        throw new Error(`Erro ao enviar arquivo: ${response.statusText}`);
      }
      
      const result = await response.json();
      
      // Adicionar mensagem ao histórico local
      set(state => {
        const currentMessages = state.groupMessages[groupId] || [];
        
        return {
          groupMessages: {
            ...state.groupMessages,
            [groupId]: [...currentMessages, {
              messageId: result.messageId,
              senderId: currentUser.userId,
              senderName: currentUser.displayName,
              content: `Arquivo: ${file.name}`,
              fileName: file.name,
              fileUrl: result.fileUrl,
              fileType: result.fileType,
              timestamp: result.timestamp,
              status: 'sent',
              isOwn: true,
              isFile: true
            }]
          }
        };
      });
      
      // Atualizar última mensagem do grupo
      get().updateGroupLastMessage(groupId, `Arquivo: ${file.name}`, result.timestamp);
      
      return result;
    } catch (error) {
      console.error('Erro ao enviar arquivo em mensagem de grupo:', error);
      throw error;
    }
  },
  
  // Funções de gerenciamento de grupo
  addUserToGroup: async (groupId, userId) => {
    const { sessionId } = get();
    if (!sessionId) return;
    
    try {
      const response = await websocketClient.sendRequest('addUserToGroup', {
        sessionId,
        groupId,
        userId
      });
      
      // Limpar o cache de membros do grupo para forçar uma nova busca
      get().clearGroupCache(groupId);
      
      return response;
    } catch (error) {
      console.error('Erro ao adicionar usuário ao grupo:', error);
      throw error;
    }
  },
  
  removeUserFromGroup: async (groupId, userId) => {
    const { sessionId } = get();
    if (!sessionId) return;
    
    try {
      const response = await websocketClient.sendRequest('removeUserFromGroup', {
        sessionId,
        groupId,
        userId
      });
      
      // Limpar o cache de membros do grupo para forçar uma nova busca
      get().clearGroupCache(groupId);
      
      return response;
    } catch (error) {
      console.error('Erro ao remover usuário do grupo:', error);
      throw error;
    }
  },
  
  setGroupAdmin: async (groupId, userId) => {
    const { sessionId } = get();
    if (!sessionId) return;
    
    try {
      const response = await websocketClient.sendRequest('setGroupAdmin', {
        sessionId,
        groupId,
        userId
      });
      
      // Limpar o cache de membros do grupo para forçar uma nova busca
      get().clearGroupCache(groupId);
      
      return response;
    } catch (error) {
      console.error('Erro ao definir administrador do grupo:', error);
      throw error;
    }
  },
  
  leaveGroup: async (groupId, deleteIfAdmin = false) => {
    const { sessionId, currentUser } = get();
    if (!sessionId || !currentUser) return;
    
    try {
      const response = await websocketClient.sendRequest('leaveGroup', {
        sessionId,
        groupId,
        deleteIfAdmin
      });
      
      // Limpar o cache do grupo
      get().clearGroupCache(groupId);
      
      // Atualizar a lista de grupos
      await get().fetchGroups();
      
      return response;
    } catch (error) {
      console.error('Erro ao sair do grupo:', error);
      throw error;
    }
  },
  
  deleteGroup: async (groupId) => {
    const { sessionId } = get();
    if (!sessionId) return;
    
    try {
      const response = await websocketClient.sendRequest('deleteGroup', {
        sessionId,
        groupId
      });
      
      // Limpar o cache do grupo
      get().clearGroupCache(groupId);
      
      // Atualizar a lista de grupos
      await get().fetchGroups();
      
      return response;
    } catch (error) {
      console.error('Erro ao excluir grupo:', error);
      throw error;
    }
  },
  
  // Manipulador de eventos WebSocket
  handleEvent: (eventType, data) => {
    switch (eventType) {
      case 'userStatusChanged':
        get().handleUserStatusChanged(data);
        break;
      case 'newPrivateMessage':
        get().handleNewPrivateMessage(data);
        break;
      case 'newGroupMessage':
        get().handleNewGroupMessage(data);
        break;
      default:
        console.log(`Evento não tratado: ${eventType}`, data);
    }
  },
  
  // Manipuladores de eventos específicos
  handleUserStatusChanged: (data) => {
    const { userId, status } = data;
    
    set(state => ({
      userStatus: {
        ...state.userStatus,
        [userId]: status
      }
    }));
    
    // Atualizar também na lista de usuários
    set(state => ({
      users: state.users.map(user => 
        user.userId === userId ? { ...user, status } : user
      )
    }));
  },
  
  handleNewPrivateMessage: (data) => {
    const { senderId, content, timestamp } = data;
    const { currentUser } = get();
    
    // Ignorar mensagens enviadas pelo próprio usuário (já tratadas pelo sendPrivateMessage)
    if (currentUser && senderId === currentUser.userId) return;
    
    // Adicionar mensagem à conversa
    set(state => {
      const currentMessages = state.privateMessages[senderId] || [];
      
      return {
        privateMessages: {
          ...state.privateMessages,
          [senderId]: [...currentMessages, data]
        }
      };
    });
    
    // Atualizar lista de conversas
    set(state => {
      const conversations = [...state.privateConversations];
      const existingIndex = conversations.findIndex(c => c.userId === senderId);
      
      if (existingIndex >= 0) {
        // Atualizar conversa existente
        conversations[existingIndex] = {
          ...conversations[existingIndex],
          lastMessage: content,
          timestamp,
          unreadCount: (conversations[existingIndex].unreadCount || 0) + 1
        };
      } else {
        // Adicionar nova conversa
        const sender = state.users.find(u => u.userId === senderId);
        if (sender) {
          conversations.push({
            userId: senderId,
            username: sender.username,
            displayName: sender.displayName,
            lastMessage: content,
            timestamp,
            unreadCount: 1
          });
        }
      }
      
      // Ordenar por timestamp mais recente
      conversations.sort((a, b) => b.timestamp - a.timestamp);
      
      return { privateConversations: conversations };
    });
    
    // Adicionar notificação
    get().addNotification({
      type: 'message',
      title: data.senderName,
      message: content,
      timestamp
    });
  },
  
  handleNewGroupMessage: (data) => {
    const { groupId, senderId, content, timestamp } = data;
    const { currentUser } = get();
    
    // Ignorar mensagens enviadas pelo próprio usuário (já tratadas pelo sendGroupMessage)
    if (currentUser && senderId === currentUser.userId) return;
    
    // Adicionar mensagem ao grupo
    set(state => {
      const currentMessages = state.groupMessages[groupId] || [];
      
      return {
        groupMessages: {
          ...state.groupMessages,
          [groupId]: [...currentMessages, data]
        }
      };
    });
    
    // Atualizar lista de grupos
    set(state => ({
      groups: state.groups.map(group => {
        if (group.groupId === groupId) {
          return {
            ...group,
            lastMessage: content,
            timestamp,
            unreadCount: (group.unreadCount || 0) + 1
          };
        }
        return group;
      })
    }));
    
    // Adicionar notificação
    get().addNotification({
      type: 'groupMessage',
      title: `${data.senderName} em ${state.groups.find(g => g.groupId === groupId)?.name || 'Grupo'}`,
      message: content,
      timestamp
    });
  },
  
  // Gerenciamento de notificações
  addNotification: (notification) => {
    set(state => ({
      notifications: [...state.notifications, { id: Date.now(), ...notification }]
    }));
    
    // Remover notificação após 5 segundos
    setTimeout(() => {
      get().removeNotification(notification.id);
    }, 5000);
  },
  
  removeNotification: (id) => {
    set(state => ({
      notifications: state.notifications.filter(n => n.id !== id)
    }));
  },
  
  clearNotifications: () => {
    set({ notifications: [] });
  }
}));

// Inicializar WebSocket quando o módulo for carregado
setTimeout(() => {
  useWebSocketStore.getState().initializeWebSocket();
}, 1000);

export default useWebSocketStore;
