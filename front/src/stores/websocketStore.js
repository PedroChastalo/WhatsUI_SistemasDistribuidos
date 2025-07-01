// websocketStore.js
// Store Zustand para gerenciar estado do WebSocket, autenticação, dados do usuário, mensagens e grupos
// Implementa tratamento robusto de erros e validação de respostas do backend

import websocketClient from "@/lib/websocket";
import { create } from "zustand";

const useWebSocketStore = create((set, get) => ({
  // Estado da conexão WebSocket
  isConnected: false,
  connectionError: null,

  // Estado de autenticação
  isAuthenticated: false,
  currentUser: null,
  sessionId: null,
  isLoading: false,
  loginError: null,

  // Estado de dados
  users: [],
  userStatus: {},
  privateConversations: [],
  privateMessages: {},
  groups: [],
  allGroups: [], // Todos os grupos no sistema
  availableGroups: [], // Grupos que o usuário não é membro
  groupMessages: {},
  notifications: [],
  fetchError: null,
  pendingGroupRequests: [], // Solicitações pendentes de entrada em grupos

  // Cache para evitar requisições repetidas
  cachedGroupMembers: {},

  // Flag para evitar inicialização duplicada
  isInitialized: false,

  // Inicializar o WebSocket e configurar eventos
  initializeWebSocket: () => {
    // Verificar se já está inicializado para evitar duplicação
    if (get().isInitialized) return;

    // Registrar o handler global para todos os eventos do WebSocket
    websocketClient.setGlobalEventHandler((eventType, data) => {
      if (eventType === "connectionChange") {
        set({
          isConnected: data,
          connectionError: data ? null : "Conexão com o servidor perdida",
        });
      } else {
        // Processar outros eventos recebidos do servidor
        get().handleEvent(eventType, data);
      }
    });

    // Inicializar o websocketClient se ainda não estiver conectado
    if (!websocketClient.socket) {
      websocketClient.connect();
    }

    set({ isInitialized: true });
  },

  // Atualizar status de conexão
  setConnectionStatus: (isConnected) => {
    set({ isConnected });
  },

  // Ações de autenticação
  login: async (email, password) => {
    try {
      set({ isLoading: true, loginError: null, fetchError: null });

      // Garantir que o WebSocket esteja inicializado
      get().initializeWebSocket();

      // Aguardar a conexão estar pronta
      await websocketClient.waitForConnection();

      const response = await websocketClient.sendRequest("login", {
        email,
        password,
      });

      // Validar formato da resposta
      if (!response || !response.userId || !response.sessionId) {
        set({
          isLoading: false,
          loginError: "Formato de resposta inválido do servidor",
        });
        throw new Error("Formato de resposta inválido do servidor");
      }

      set({
        isAuthenticated: true,
        currentUser: response,
        sessionId: response.sessionId,
        isLoading: false,
        loginError: null,
      });

      // Reconectar com o sessionId para autenticar a conexão WebSocket
      await websocketClient.reconnect(response.sessionId);

      // Carregar dados iniciais após login bem-sucedido
      try {
        await get().loadInitialData();
      } catch (dataError) {
        set({ fetchError: `Erro ao carregar dados: ${dataError.message}` });
      }

      return response;
    } catch (error) {
      set({
        isAuthenticated: false,
        currentUser: null,
        sessionId: null,
        isLoading: false,
        loginError: error.message || "Erro ao fazer login. Tente novamente.",
      });
      throw error;
    }
  },

  register: async (userData) => {
    set({ isLoading: true, fetchError: null, loginError: null });

    try {
      if (!userData || !userData.email || !userData.password || !userData.username) {
        throw new Error("Dados de registro incompletos");
      }

      const response = await websocketClient.sendRequest("register", userData);

      // O backend pode retornar diferentes formatos. Se houver a chave "error", tratar como falha.
      if (response && response.error) {
        throw new Error(response.error);
      }
      // Caso a chave "success" exista e seja falsa, também é considerado erro.
      if (typeof response?.success !== "undefined" && response.success === false) {
        throw new Error(response?.message || "Erro ao registrar usuário");
      }

      // Sucesso: limpar estados de erro
      set({ isLoading: false, fetchError: null });

      return response;
    } catch (error) {
      set({ isLoading: false, fetchError: error.message }); // <-- manter o erro aqui
      throw error;
    }
  },


  logout: async () => {
    set({ isLoading: true });

    try {
      const { sessionId } = get();
      if (sessionId) {
        await websocketClient.sendRequest("logout", { sessionId });
      }

      // Limpar estado
      set({
        isAuthenticated: false,
        currentUser: null,
        sessionId: null,
        users: [],
        userStatus: {},
        privateConversations: [],
        privateMessages: {},
        groups: [],
        groupMessages: {},
        cachedGroupMembers: {},
        notifications: [],
        isLoading: false,
        loginError: null,
        fetchError: null,
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
    await get().fetchUsers();

    // Carregar conversas privadas
    await get().fetchPrivateConversations();

    // Carregar grupos
    await get().fetchGroups();
  },

  // Ações de usuários
  fetchUsers: async () => {
    const { sessionId } = get();
    if (!sessionId) {
      set({ fetchError: "Sessão não encontrada" });
      return [];
    }

    try {
      const response = await websocketClient.sendRequest("getUsers", {
        sessionId,
      });

      // Verificar se a resposta é um array (formato direto) ou um objeto com propriedade users
      let users = [];

      if (Array.isArray(response)) {
        // Resposta direta como array
        users = response;
      } else if (response && response.users && Array.isArray(response.users)) {
        // Resposta no formato esperado {users: [...]}
        users = response.users;
      } else {
        console.warn(
          "Resposta inesperada do servidor para getUsers:",
          response
        );
        return [];
      }

      // Mapear status de usuários para acesso rápido
      const userStatus = {};
      users.forEach((user) => {
        if (user && user.userId) {
          userStatus[user.userId] = user.status || "offline";
        }
      });

      set({
        users,
        userStatus,
        fetchError: null,
      });

      return users;
    } catch (error) {
      console.error("Erro ao buscar usuários:", error);
      set({ fetchError: `Erro ao buscar usuários: ${error.message}` });
      return [];
    }
  },

  updateStatus: async (status) => {
    const { sessionId, currentUser } = get();
    if (!sessionId || !currentUser) return;

    try {
      await websocketClient.sendRequest("updateStatus", { sessionId, status });

      // Atualizar status local
      set((state) => ({
        userStatus: {
          ...state.userStatus,
          [currentUser.userId]: status,
        },
      }));

      return true;
    } catch (error) {
      console.error("Erro ao atualizar status:", error);
      set({ fetchError: `Erro ao atualizar status: ${error.message}` });
      return false;
    }
  },

  // Ações de mensagens privadas
  fetchPrivateConversations: async () => {
    const { sessionId } = get();
    if (!sessionId) {
      set({ fetchError: "Sessão não encontrada" });
      return [];
    }

    try {
      const response = await websocketClient.sendRequest(
        "getPrivateConversations",
        { sessionId }
      );

      // Verificar se a resposta é um array (formato direto) ou um objeto com propriedade conversations
      let conversations = [];

      if (Array.isArray(response)) {
        // Resposta direta como array
        conversations = response;
      } else if (
        response &&
        response.conversations &&
        Array.isArray(response.conversations)
      ) {
        // Resposta no formato esperado {conversations: [...]}
        conversations = response.conversations;
      } else {
        console.warn(
          "Resposta inesperada do servidor para getPrivateConversations:",
          response
        );
        return [];
      }

      set({
        privateConversations: conversations,
        fetchError: null,
      });

      return conversations;
    } catch (error) {
      console.error("Erro ao buscar conversas privadas:", error);
      set({ fetchError: `Erro ao buscar conversas: ${error.message}` });
      return [];
    }
  },

  fetchPrivateMessages: async (userId) => {
    const { sessionId, privateMessages } = get();
    if (!sessionId) {
      set({ fetchError: "Sessão não encontrada" });
      return [];
    }

    if (!userId) {
      console.error("fetchPrivateMessages chamado sem userId");
      set({ fetchError: "ID de usuário não fornecido" });
      return [];
    }

    try {
      // Verificar se já temos as mensagens em cache
      if (privateMessages[userId] && privateMessages[userId].length > 0) {
        // Se já tivermos mensagens em cache, retornar diretamente
        return privateMessages[userId];
      }

      const response = await websocketClient.sendRequest("getPrivateMessages", {
        sessionId,
        userId,
        limit: 50,
      });

      // Verificar se a resposta é um array (formato direto) ou um objeto com propriedade messages
      let messages = [];

      if (Array.isArray(response)) {
        // Resposta direta como array
        messages = response;
      } else if (
        response &&
        response.messages &&
        Array.isArray(response.messages)
      ) {
        // Resposta no formato esperado {messages: [...]}
        messages = response.messages;
      } else {
        console.warn(
          "Resposta inesperada do servidor para getPrivateMessages:",
          response
        );
        return [];
      }

      // Atualizar mensagens no estado
      set({
        privateMessages: {
          ...privateMessages,
          [userId]: messages,
        },
        fetchError: null,
      });

      return messages;
    } catch (error) {
      console.error(`Erro ao buscar mensagens privadas com ${userId}:`, error);
      set({ fetchError: `Erro ao buscar mensagens: ${error.message}` });
      return [];
    }
  },

  sendPrivateMessage: async (receiverId, content) => {
    const { sessionId, currentUser, privateMessages } = get();

    // Validação de parâmetros
    if (!sessionId || !currentUser) {
      const errorMsg = "Sessão não encontrada";
      console.error("sendPrivateMessage:", errorMsg);
      set({ fetchError: errorMsg });
      throw new Error(errorMsg);
    }

    if (!receiverId || !content) {
      const errorMsg = "Parâmetros inválidos";
      console.error("sendPrivateMessage:", errorMsg, { receiverId, content });
      set({ fetchError: errorMsg });
      throw new Error(errorMsg);
    }

    try {
      const response = await websocketClient.sendRequest("sendPrivateMessage", {
        sessionId,
        userId: receiverId, // Usar userId em vez de receiverId para compatibilidade com o backend
        content,
      });

      // Validar resposta
      if (!response || !response.messageId) {
        const errorMsg = "Formato de resposta inválido do servidor";
        console.error("sendPrivateMessage:", errorMsg, response);
        set({ fetchError: errorMsg });
        throw new Error(errorMsg);
      }

      // Adicionar mensagem ao histórico local
      set((state) => {
        const currentMessages = state.privateMessages[receiverId] || [];

        return {
          privateMessages: {
            ...state.privateMessages,
            [receiverId]: [
              ...currentMessages,
              {
                messageId: response.messageId,
                senderId: currentUser.userId,
                senderName: currentUser.displayName,
                content,
                timestamp: response.timestamp,
                status: "sent",
                isOwn: true,
                isFile: false,
              },
            ],
          },
        };
      });

      // Atualizar última mensagem na conversa
      get().updateConversationLastMessage(
        receiverId,
        content,
        response.timestamp
      );

      return response;
    } catch (error) {
      console.error("Erro ao enviar mensagem privada:", error);
      set({ fetchError: `Erro ao enviar mensagem: ${error.message}` });
      throw error;
    }
  },

  sendPrivateFile: async (receiverId, file, fileType = null) => {
    const { currentUser } = get();
    if (!currentUser || !receiverId || !file) {
      const msg = "Parâmetros inválidos";
      set({ fetchError: msg });
      throw new Error(msg);
    }
    try {
      const base64Data = await new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => {
          const res = reader.result;
          resolve(typeof res === "string" ? res.split(",")[1] : "");
        };
        reader.onerror = reject;
        reader.readAsDataURL(file);
      });

      const wsRes = await websocketClient.sendRequest("sendPrivateFile", {
        receiverId,
        fileName: file.name,
        fileType: fileType || file.type,
        fileData: base64Data,
      });
      if (!wsRes || !wsRes.messageId) throw new Error("Resposta inválida");

      // Adicionar mensagem ao histórico local
      set((state) => {
        const currentMessages = state.privateMessages[receiverId] || [];
        return {
          privateMessages: {
            ...state.privateMessages,
            [receiverId]: [
              ...currentMessages,
              {
                messageId: wsRes.messageId,
                senderId: currentUser.userId,
                senderName: currentUser.displayName,
                content: `Arquivo: ${file.name}`,
                fileName: file.name,
                fileUrl: wsRes.fileUrl || "",
                fileType: wsRes.fileType || fileType || file.type,
                timestamp: wsRes.timestamp,
                status: "sent",
                isOwn: true,
                isFile: true,
              },
            ],
          },
        };
      });

      // Atualizar última mensagem na conversa
      get().updateConversationLastMessage(
        receiverId,
        `Arquivo: ${file.name}`,
        wsRes.timestamp
      );

      return wsRes;
    } catch (error) {
      console.error("Erro ao enviar arquivo em mensagem privada:", error);
      set({ fetchError: `Erro ao enviar arquivo: ${error.message}` });
      throw error;
    }
  },

  // Envio de arquivo para grupo
  sendGroupFile: async (groupId, file, fileType = null) => {
    const { currentUser } = get();
    if (!currentUser || !groupId || !file) {
      const msg = "Parâmetros inválidos";
      set({ fetchError: msg });
      throw new Error(msg);
    }
    try {
      const base64Data = await new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => {
          const res = reader.result;
          resolve(typeof res === "string" ? res.split(",")[1] : "");
        };
        reader.onerror = reject;
        reader.readAsDataURL(file);
      });

      const wsRes = await websocketClient.sendRequest("sendGroupFile", {
        groupId,
        fileName: file.name,
        fileType: fileType || file.type,
        fileData: base64Data,
      });
      if (!wsRes || !wsRes.messageId) throw new Error("Resposta inválida");

      set((state) => {
        const msgs = state.groupMessages[groupId] || [];
        return {
          groupMessages: {
            ...state.groupMessages,
            [groupId]: [
              ...msgs,
              {
                messageId: wsRes.messageId,
                senderId: currentUser.userId,
                senderName: currentUser.displayName,
                content: `Arquivo: ${file.name}`,
                fileName: file.name,
                fileUrl: wsRes.fileUrl,
                fileType: wsRes.fileType,
                timestamp: wsRes.timestamp,
                status: "sent",
                isOwn: true,
                isFile: true,
              },
            ],
          },
        };
      });
      return wsRes;
    } catch (err) {
      console.error("Erro ao enviar arquivo para grupo", err);
      set({ fetchError: err.message });
      throw err;
    }
  },

  updateConversationLastMessage: (userId, content, timestamp) => {
    set((state) => {
      const conversations = [...state.privateConversations];
      const existingIndex = conversations.findIndex((c) => c.userId === userId);

      if (existingIndex >= 0) {
        // Atualizar conversa existente
        conversations[existingIndex] = {
          ...conversations[existingIndex],
          lastMessage: content,
          timestamp,
          unreadCount: 0, // Zerar contagem não lida para mensagens enviadas
        };
      } else {
        // Adicionar nova conversa
        const user = state.users.find((u) => u.userId === userId);
        if (user) {
          conversations.push({
            userId,
            username: user.username,
            displayName: user.displayName,
            lastMessage: content,
            timestamp,
            unreadCount: 0,
          });
        }
      }

      // Ordenar por timestamp mais recente
      conversations.sort((a, b) => b.timestamp - a.timestamp);

      return { privateConversations: conversations };
    });
  },

  // Limpar cache de mensagens privadas
  clearPrivateMessageCache: (userId = null) => {
    if (userId) {
      // Limpar cache para um usuário específico
      set((state) => ({
        privateMessages: {
          ...state.privateMessages,
          [userId]: [],
        },
      }));
    } else {
      // Limpar todo o cache
      set({ privateMessages: {} });
    }
  },

  // Ações de grupos
  fetchAllGroups: async () => {
    try {
      set({ isLoading: true, fetchError: null });
      
      // Verificar se o usuário está autenticado
      const sessionId = get().sessionId;
      if (!sessionId) {
        throw new Error("Usuário não autenticado");
      }
      
      // Enviar requisição para obter todos os grupos disponíveis
      const response = await websocketClient.sendRequest("getGroups", { sessionId });
      
      // Validar resposta
      if (!response || !Array.isArray(response)) {
        throw new Error("Formato de resposta inválido ao buscar todos os grupos");
      }
      
      // Atualizar estado
      set({ allGroups: response, isLoading: false });
      return response;
    } catch (error) {
      console.error("[WebSocketStore] Erro ao buscar todos os grupos:", error);
      set({
        isLoading: false,
        fetchError: `Erro ao buscar todos os grupos: ${error.message || "Desconhecido"}`
      });
      throw error;
    }
  },
  
  // Buscar todos os grupos disponíveis (que o usuário não é membro)
  fetchAllAvailableGroups: async () => {
    try {
      set({ isLoading: true, fetchError: null });
      
      // Verificar se o usuário está autenticado
      const sessionId = get().sessionId;
      if (!sessionId) {
        throw new Error("Usuário não autenticado");
      }
      
      // Enviar requisição para obter todos os grupos disponíveis
      const response = await websocketClient.sendRequest("getAllAvailableGroups", { sessionId });
      
      // Validar resposta
      if (!response || !Array.isArray(response)) {
        throw new Error("Formato de resposta inválido ao buscar grupos disponíveis");
      }
      
      // Atualizar estado
      set({ availableGroups: response, isLoading: false });
      return response;
    } catch (error) {
      console.error("[WebSocketStore] Erro ao buscar grupos disponíveis:", error);
      set({
        isLoading: false,
        fetchError: `Erro ao buscar grupos disponíveis: ${error.message || "Desconhecido"}`
      });
      throw error;
    }
  },
  
  fetchGroups: async () => {
    const { sessionId } = get();
    if (!sessionId) {
      set({ fetchError: "Sessão não encontrada" });
      return [];
    }

    try {
      const response = await websocketClient.sendRequest("getGroups", {
        sessionId,
      });

      // Verificar se a resposta é um array (formato direto) ou um objeto com propriedade groups
      let groups = [];

      if (Array.isArray(response)) {
        // Resposta direta como array
        groups = response;
      } else if (
        response &&
        response.groups &&
        Array.isArray(response.groups)
      ) {
        // Resposta no formato esperado {groups: [...]}
        groups = response.groups;
      } else {
        console.warn(
          "Resposta inesperada do servidor para getGroups:",
          response
        );
        return [];
      }

      set({
        groups,
        fetchError: null,
      });

      return groups;
    } catch (error) {
      console.error("Erro ao buscar grupos:", error);
      set({ fetchError: `Erro ao buscar grupos: ${error.message}` });
      return [];
    }
  },

  fetchGroupMessages: async (groupId) => {
    const { sessionId, groupMessages } = get();
    if (!sessionId) {
      set({ fetchError: "Sessão não encontrada" });
      return [];
    }

    if (!groupId) {
      console.error("fetchGroupMessages chamado sem groupId");
      set({ fetchError: "ID de grupo não fornecido" });
      return [];
    }

    try {
      // Verificar se já temos as mensagens em cache
      if (groupMessages[groupId] && groupMessages[groupId].length > 0) {
        // Se já tivermos mensagens em cache, retornar diretamente
        return groupMessages[groupId];
      }

      const response = await websocketClient.sendRequest("getGroupMessages", {
        sessionId,
        groupId,
        limit: 50,
      });

      // Verificar se a resposta é um array (formato direto) ou um objeto com propriedade messages
      let messages = [];

      if (Array.isArray(response)) {
        // Resposta direta como array
        messages = response;
      } else if (
        response &&
        response.messages &&
        Array.isArray(response.messages)
      ) {
        // Resposta no formato esperado {messages: [...]}
        messages = response.messages;
      } else {
        console.warn(
          "Resposta inesperada do servidor para getGroupMessages:",
          response
        );
        return [];
      }

      // Atualizar mensagens no estado
      set({
        groupMessages: {
          ...groupMessages,
          [groupId]: messages,
        },
        fetchError: null,
      });

      return messages;
    } catch (error) {
      console.error(`Erro ao buscar mensagens do grupo ${groupId}:`, error);
      set({ fetchError: `Erro ao buscar mensagens: ${error.message}` });
      return [];
    }
  },

  sendGroupMessage: async (groupId, content) => {
    const { sessionId, currentUser, groupMessages } = get();

    // Validação de parâmetros
    if (!sessionId || !currentUser) {
      const errorMsg = "Sessão não encontrada";
      console.error("sendGroupMessage:", errorMsg);
      set({ fetchError: errorMsg });
      throw new Error(errorMsg);
    }

    if (!groupId || !content) {
      const errorMsg = "Parâmetros inválidos";
      console.error("sendGroupMessage:", errorMsg, { groupId, content });
      set({ fetchError: errorMsg });
      throw new Error(errorMsg);
    }

    try {
      const response = await websocketClient.sendRequest("sendGroupMessage", {
        sessionId,
        groupId,
        content,
      });

      // Validar resposta
      if (!response || !response.messageId) {
        const errorMsg = "Formato de resposta inválido do servidor";
        console.error("sendGroupMessage:", errorMsg, response);
        set({ fetchError: errorMsg });
        throw new Error(errorMsg);
      }

      // Adicionar mensagem ao histórico local
      set((state) => {
        const currentMessages = state.groupMessages[groupId] || [];

        return {
          groupMessages: {
            ...state.groupMessages,
            [groupId]: [
              ...currentMessages,
              {
                messageId: response.messageId,
                senderId: currentUser.userId,
                senderName: currentUser.displayName || currentUser.username,
                content,
                timestamp: response.timestamp,
                status: "sent",
                isOwn: true,
                isFile: false,
              },
            ],
          },
        };
      });

      // Atualizar última mensagem no grupo
      get().updateGroupLastMessage(groupId, content, response.timestamp);

      return response;
    } catch (error) {
      console.error("Erro ao enviar mensagem de grupo:", error);
      set({ fetchError: `Erro ao enviar mensagem: ${error.message}` });
      throw error;
    }
  },

  sendGroupFile: async (groupId, file, fileType = null) => {
    const { sessionId, currentUser } = get();

    // Validação de parâmetros
    if (!sessionId || !currentUser || !groupId || !file) {
      const errorMsg = "Parâmetros inválidos";
      console.error("sendGroupFile:", errorMsg, { groupId, file });
      set({ fetchError: errorMsg });
      throw new Error(errorMsg);
    }

    try {
      // Converter arquivo para base64 para envio via WebSocket
      const base64Data = await new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => {
          const res = reader.result;
          resolve(typeof res === "string" ? res.split(",")[1] : "");
        };
        reader.onerror = reject;
        reader.readAsDataURL(file);
      });

      // Em modo mock, simular envio
      if (websocketClient.mockMode) {
        const mockResponse = {
          messageId: "file_" + Date.now(),
          timestamp: Date.now(),
          fileName: file.name,
          fileUrl: URL.createObjectURL(file),
          fileType: fileType || file.type,
        };

        // Adicionar mensagem ao histórico local
        set((state) => {
          const currentMessages = state.groupMessages[groupId] || [];

          return {
            groupMessages: {
              ...state.groupMessages,
              [groupId]: [
                ...currentMessages,
                {
                  messageId: mockResponse.messageId,
                  senderId: currentUser.userId,
                  senderName: currentUser.displayName || currentUser.username,
                  content: `Arquivo: ${file.name}`,
                  fileName: file.name,
                  fileUrl: mockResponse.fileUrl,
                  fileType: mockResponse.fileType,
                  timestamp: mockResponse.timestamp,
                  status: "sent",
                  isOwn: true,
                  isFile: true,
                },
              ],
            },
          };
        });

        // Atualizar última mensagem no grupo
        get().updateGroupLastMessage(
          groupId,
          `Arquivo: ${file.name}`,
          mockResponse.timestamp
        );

        return mockResponse;
      }

      // Enviar via WebSocket RPC
      const wsRes = await websocketClient.sendRequest("sendGroupFile", {
        groupId,
        fileName: file.name,
        fileType: fileType || file.type,
        fileData: base64Data,
      });
      if (!wsRes || !wsRes.messageId) throw new Error("Resposta inválida");

      const result = {
        ...wsRes,
        fileUrl: wsRes.fileUrl || `data:${fileType || file.type};base64,${base64Data}`,
      };

      // Adicionar mensagem ao histórico local
      set((state) => {
        const currentMessages = state.groupMessages[groupId] || [];

        return {
          groupMessages: {
            ...state.groupMessages,
            [groupId]: [
              ...currentMessages,
              {
                messageId: result.messageId,
                senderId: currentUser.userId,
                senderName: currentUser.displayName || currentUser.username,
                content: `Arquivo: ${file.name}`,
                fileName: file.name,
                fileUrl: result.fileUrl,
                fileType: result.fileType,
                timestamp: result.timestamp,
                status: "sent",
                isOwn: true,
                isFile: true,
              },
            ],
          },
        };
      });

      // Atualizar última mensagem no grupo
      get().updateGroupLastMessage(
        groupId,
        `Arquivo: ${file.name}`,
        result.timestamp
      );

      return result;
    } catch (error) {
      console.error("Erro ao enviar arquivo em mensagem de grupo:", error);
      set({ fetchError: `Erro ao enviar arquivo: ${error.message}` });
      throw error;
    }
  },

  updateGroupLastMessage: (groupId, content, timestamp) => {
    set((state) => {
      const groups = [...state.groups];
      const existingIndex = groups.findIndex((g) => g.groupId === groupId);

      if (existingIndex >= 0) {
        // Atualizar grupo existente
        groups[existingIndex] = {
          ...groups[existingIndex],
          lastMessage: content,
          timestamp,
          unreadCount: 0, // Zerar contagem não lida para mensagens enviadas
        };
      }

      // Ordenar por timestamp mais recente
      groups.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));

      return { groups };
    });
  },

  // Limpar cache de mensagens de grupo
  clearGroupMessageCache: (groupId = null) => {
    if (groupId) {
      // Limpar cache para um grupo específico
      set((state) => ({
        groupMessages: {
          ...state.groupMessages,
          [groupId]: [],
        },
      }));
    } else {
      // Limpar todo o cache
      set({ groupMessages: {} });
    }
  },

  // Limpar cache de membros de grupo
  clearGroupMembersCache: (groupId = null) => {
    if (groupId) {
      // Limpar cache para um grupo específico
      set((state) => ({
        cachedGroupMembers: {
          ...state.cachedGroupMembers,
          [groupId]: [],
        },
      }));
    } else {
      // Limpar todo o cache
      set({ cachedGroupMembers: {} });
    }
  },

  // Gerenciamento de grupos
  createGroup: async (groupData) => {
    const { sessionId, currentUser } = get();

    // Validação de parâmetros
    if (!sessionId || !currentUser) {
      const errorMsg = "Sessão não encontrada";
      console.error("createGroup:", errorMsg);
      set({ fetchError: errorMsg });
      throw new Error(errorMsg);
    }
    
    // Suporte para formato antigo (string) e novo formato (objeto)
    let groupName, members = [], description = "", deleteIfAdminLeaves = false;
    
    if (typeof groupData === 'string') {
      // Formato antigo: apenas o nome do grupo
      groupName = groupData;
    } else if (typeof groupData === 'object') {
      // Novo formato: objeto com propriedades
      groupName = groupData.name;
      members = groupData.members || [];
      description = groupData.description || "";
      deleteIfAdminLeaves = !!groupData.deleteIfAdminLeaves;
    } else {
      const errorMsg = "Formato de dados do grupo inválido";
      console.error("createGroup:", errorMsg);
      set({ fetchError: errorMsg });
      throw new Error(errorMsg);
    }

    if (!groupName || groupName.trim() === "") {
      const errorMsg = "Nome do grupo não pode ser vazio";
      console.error("createGroup:", errorMsg);
      set({ fetchError: errorMsg });
      throw new Error(errorMsg);
    }

    try {
      const response = await websocketClient.sendRequest("createGroup", {
        sessionId,
        name: groupName.trim(),
        description: description,
        members: Array.isArray(members) ? members : [],
        deleteIfAdminLeaves: deleteIfAdminLeaves
      });

      // Validar resposta
      if (!response || !response.groupId) {
        const errorMsg = "Formato de resposta inválido do servidor";
        console.error("createGroup:", errorMsg, response);
        set({ fetchError: errorMsg });
        throw new Error(errorMsg);
      }

      // Adicionar grupo à lista local
      set((state) => ({
        groups: [
          ...state.groups,
          {
            groupId: response.groupId,
            name: groupName.trim(),
            description: description,
            createdBy: currentUser.userId,
            timestamp: response.timestamp || new Date().toISOString(),
            members: [currentUser.userId, ...members],
            admins: [currentUser.userId],
            deleteIfAdminLeaves: deleteIfAdminLeaves,
          },
        ],
      }));

      return response;
    } catch (error) {
      console.error("Erro ao criar grupo:", error);
      set({ fetchError: `Erro ao criar grupo: ${error.message}` });
      throw error;
    }
  },

  // Função para sair de um grupo
  leaveGroup: async (groupId) => {
    const { sessionId, currentUser, cachedGroupMembers } = get();

    // Validação de parâmetros
    if (!sessionId || !currentUser) {
      const errorMsg = "Sessão não encontrada";
      console.error("leaveGroup:", errorMsg);
      set({ fetchError: errorMsg });
      throw new Error(errorMsg);
    }

    if (!groupId) {
      const errorMsg = "ID do grupo não fornecido";
      console.error("leaveGroup:", errorMsg);
      set({ fetchError: errorMsg });
      throw new Error(errorMsg);
    }

    try {
      const response = await websocketClient.sendRequest("leaveGroup", {
        sessionId,
        groupId,
      });

      // Validar resposta
      if (!response || response.success !== true) {
        const errorMsg = response?.error || "Erro ao sair do grupo";
        console.error("leaveGroup:", errorMsg, response);
        set({ fetchError: errorMsg });
        throw new Error(errorMsg);
      }

      // Remover grupo da lista local
      set((state) => ({
        groups: state.groups.filter((g) => g.groupId !== groupId),
        cachedGroupMembers: {
          ...state.cachedGroupMembers,
          [groupId]: [],
        },
        groupMessages: {
          ...state.groupMessages,
          [groupId]: [],
        },
      }));

      return response;
    } catch (error) {
      console.error(`Erro ao sair do grupo ${groupId}:`, error);
      set({ fetchError: `Erro ao sair do grupo: ${error.message}` });
      throw error;
    }
  },

  // Solicitar entrada em um grupo
  requestJoinGroup: async (groupId) => {
    try {
      set({ isLoading: true, fetchError: null });
      
      // Verificar se o usuário está autenticado
      const sessionId = get().sessionId;
      if (!sessionId) {
        throw new Error("Usuário não autenticado");
      }
      
      // Verificar se o grupo existe
      if (!groupId) {
        throw new Error("ID do grupo não fornecido");
      }
      
      console.log("[WebSocket] Enviando requisição: requestJoinGroup", { groupId });
      
      // Enviar requisição para solicitar entrada no grupo
      const response = await websocketClient.sendRequest("requestJoinGroup", {
        groupId
      });
      
      console.log("[WebSocket] Resposta recebida para requestJoinGroup:", response);
      
      set({ isLoading: false });
      
      // Adicionar notificação de sucesso
      get().addNotification({
        id: Date.now(),
        title: "Solicitação enviada",
        message: response?.message || "Sua solicitação para entrar no grupo foi enviada ao administrador.",
        type: "info",
        timestamp: Date.now()
      });
      
      return response;
    } catch (error) {
      console.error("[WebSocketStore] Erro ao solicitar entrada no grupo:", error);
      set({
        isLoading: false,
        fetchError: `Erro ao solicitar entrada no grupo: ${error.message || "Desconhecido"}`
      });
      throw error;
    }
  },
  
  // Responder a uma solicitação de entrada em grupo (aprovar/rejeitar)
  respondToGroupRequest: async (userId, groupId, accept) => {
    try {
      set({ isLoading: true, fetchError: null });
      
      // Verificar se o usuário está autenticado
      const sessionId = get().sessionId;
      if (!sessionId) {
        throw new Error("Usuário não autenticado");
      }
      
      // Verificar parâmetros
      if (!userId || !groupId) {
        throw new Error("Parâmetros incompletos");
      }
      
      // Enviar requisição para responder à solicitação
      const response = await websocketClient.sendRequest("respondJoinGroup", {
        sessionId,
        userId,
        groupId,
        accept
      });
      
      // Validar resposta
      if (!response || !response.success) {
        throw new Error(response?.error || "Erro ao responder à solicitação");
      }
      
      // Remover a solicitação da lista de pendentes
      set(state => ({
        pendingGroupRequests: state.pendingGroupRequests.filter(
          req => !(req.userId === userId && req.groupId === groupId)
        ),
        isLoading: false
      }));
      
      // Se aceitou, atualizar a lista de grupos
      if (accept) {
        await get().fetchGroups();
      }
      
      return response;
    } catch (error) {
      console.error("[WebSocketStore] Erro ao responder à solicitação de grupo:", error);
      set({
        isLoading: false,
        fetchError: `Erro ao responder à solicitação: ${error.message || "Desconhecido"}`
      });
      throw error;
    }
  },
  
  // Obter solicitações pendentes de entrada em grupo
  // Nota: O backend não tem um endpoint específico para isso.
  // As solicitações pendentes são enviadas automaticamente quando o usuário faz login.
  fetchPendingGroupRequests: async () => {
    try {
      // Apenas retorna as solicitações pendentes que já estão no estado
      const pendingRequests = get().pendingGroupRequests || [];
      return pendingRequests;
    } catch (error) {
      console.error("[WebSocketStore] Erro ao acessar solicitações pendentes:", error);
      return [];
    }
  },
  
  // Obter membros de um grupo
  getGroupMembers: async (groupId) => {
    try {
      set({ isLoading: true, fetchError: null });
      
      // Verificar se o usuário está autenticado
      const sessionId = get().sessionId;
      if (!sessionId) {
        throw new Error("Usuário não autenticado");
      }
      
      if (!groupId) {
        throw new Error("ID do grupo não fornecido");
      }
      
      // Enviar requisição para obter membros do grupo
      const response = await websocketClient.sendRequest("getGroupMembers", {
        sessionId,
        groupId
      });
      
      // Validar resposta
      if (!response || !Array.isArray(response)) {
        throw new Error("Formato de resposta inválido ao buscar membros do grupo");
      }
      
      set({ isLoading: false });
      return response;
    } catch (error) {
      console.error(`[WebSocketStore] Erro ao buscar membros do grupo ${groupId}:`, error);
      set({
        isLoading: false,
        fetchError: `Erro ao buscar membros do grupo: ${error.message || "Desconhecido"}`
      });
      return [];
    }
  },
  
  leaveGroup: async (groupId) => {
    const { sessionId } = get();

    // Validações básicas
    if (!sessionId) {
      const errorMsg = "Sessão não encontrada";
      console.error("leaveGroup:", errorMsg);
      set({ fetchError: errorMsg });
      throw new Error(errorMsg);
    }
    if (!groupId) {
      const errorMsg = "ID do grupo não fornecido";
      console.error("leaveGroup:", errorMsg);
      set({ fetchError: errorMsg });
      throw new Error(errorMsg);
    }

    try {
      const response = await websocketClient.sendRequest("leaveGroup", {
        sessionId,
        groupId,
      });

      // Caso o backend retorne erro explícito
      if (response && response.error) {
        const errorMsg = response.error;
        console.error("leaveGroup:", errorMsg);
        set({ fetchError: errorMsg });
        throw new Error(errorMsg);
      }
      // Caso exista success === false
      if (typeof response?.success !== "undefined" && response.success === false) {
        const errorMsg = response?.message || "Erro ao sair do grupo";
        console.error("leaveGroup:", errorMsg);
        set({ fetchError: errorMsg });
        throw new Error(errorMsg);
      }

      // Remover grupo localmente
      set((state) => ({
        groups: state.groups.filter((g) => g.groupId !== groupId),
        cachedGroupMembers: {
          ...state.cachedGroupMembers,
          [groupId]: [],
        },
        groupMessages: {
          ...state.groupMessages,
          [groupId]: [],
        },
      }));

      return response;
    } catch (error) {
      console.error(`Erro ao sair do grupo ${groupId}:`, error);
      set({ fetchError: `Erro ao sair do grupo: ${error.message}` });
      throw error;
    }
  },

  deleteGroup: async (groupId) => {
    const { sessionId, cachedGroupMembers } = get();

    // Validação de parâmetros
    if (!sessionId) {
      const errorMsg = "Sessão não encontrada";
      console.error("deleteGroup:", errorMsg);
      set({ fetchError: errorMsg });
      throw new Error(errorMsg);
    }

    if (!groupId) {
      const errorMsg = "ID do grupo não fornecido";
      console.error("deleteGroup:", errorMsg);
      set({ fetchError: errorMsg });
      throw new Error(errorMsg);
    }

    try {
      const response = await websocketClient.sendRequest("deleteGroup", {
        sessionId,
        groupId,
      });

      // Validar resposta
      if (!response || response.success !== true) {
        const errorMsg = response?.error || "Erro ao excluir grupo";
        console.error("deleteGroup:", errorMsg, response);
        set({ fetchError: errorMsg });
        throw new Error(errorMsg);
      }

      // Remover grupo da lista local
      set((state) => ({
        groups: state.groups.filter((g) => g.groupId !== groupId),
        cachedGroupMembers: {
          ...state.cachedGroupMembers,
          [groupId]: [],
        },
        groupMessages: {
          ...state.groupMessages,
          [groupId]: [],
        },
      }));

      return response;
    } catch (error) {
      console.error(`Erro ao excluir grupo ${groupId}:`, error);
      set({ fetchError: `Erro ao excluir grupo: ${error.message}` });
      throw error;
    }
  },

  // Notificações
  addNotification: (notification) => {
    if (!notification || !notification.message) return;

    set((state) => ({
      notifications: [
        {
          id: Date.now(),
          timestamp: new Date().toISOString(),
          ...notification,
        },
        ...state.notifications,
      ],
    }));
  },

  clearNotification: (notificationId) => {
    set((state) => ({
      notifications: state.notifications.filter((n) => n.id !== notificationId),
    }));
  },

  clearAllNotifications: () => {
    set({ notifications: [] });
  },

  // Utilitários
  getUser: async (userId) => {
    const { users, sessionId } = get();

    // Tentar encontrar no cache primeiro
    const cachedUser = users.find((u) => u.userId === userId);
    if (cachedUser) return cachedUser;

    // Se não encontrado, buscar do servidor
    if (!sessionId) {
      throw new Error("Sessão não encontrada");
    }

    try {
      const response = await websocketClient.sendRequest("getUser", {
        sessionId,
        userId,
      });

      // Verificar se a resposta é um objeto válido com userId
      let user;

      if (response && typeof response === "object") {
        if (response.userId) {
          // Resposta já é um objeto de usuário válido
          user = response;
        } else if (response.user && response.user.userId) {
          // Resposta é um objeto com propriedade user
          user = response.user;
        } else {
          console.warn("getUser: Resposta inesperada do servidor:", response);
          throw new Error("Usuário não encontrado");
        }
      } else {
        console.warn("getUser: Resposta inválida do servidor:", response);
        throw new Error("Usuário não encontrado");
      }

      // Adicionar ao cache
      set((state) => ({
        users: [...state.users.filter((u) => u.userId !== userId), user],
      }));

      return user;
    } catch (error) {
      console.error(`Erro ao buscar usuário ${userId}:`, error);
      throw error;
    }
  },

  // Atualizar mensagem privada recebida
  updatePrivateMessage: (message) => {
    if (!message || !message.messageId) {
      console.warn(
        "[WebSocketStore] Tentativa de atualizar mensagem privada inválida:",
        message
      );
      return;
    }

    const userId =
      message.senderId === get().currentUser?.userId
        ? message.recipientId
        : message.senderId;

    if (!userId) {
      console.warn(
        "[WebSocketStore] Não foi possível determinar o userId para a mensagem:",
        message
      );
      return;
    }

    // Adicionar mensagem ao cache de mensagens privadas
    set((state) => {
      const currentMessages = state.privateMessages[userId] || [];

      // Verificar se a mensagem já existe para evitar duplicação
      if (currentMessages.some((m) => m.messageId === message.messageId)) {
        return state;
      }

      return {
        privateMessages: {
          ...state.privateMessages,
          [userId]: [...currentMessages, message],
        },
      };
    });

    // Atualizar última mensagem do usuário
    set((state) => ({
      users: state.users.map((u) =>
        u.userId === userId
          ? {
              ...u,
              lastMessage: message.content,
              lastMessageTime: message.timestamp,
            }
          : u
      ),
    }));
  },

  // Atualizar mensagem de grupo recebida
  updateGroupMessage: (message) => {
    if (!message || !message.messageId || !message.groupId) {
      console.warn(
        "[WebSocketStore] Tentativa de atualizar mensagem de grupo inválida:",
        message
      );
      return;
    }

    const { groupId } = message;

    // Adicionar mensagem ao cache de mensagens do grupo
    set((state) => {
      const currentMessages = state.groupMessages[groupId] || [];

      // Verificar se a mensagem já existe para evitar duplicação
      if (currentMessages.some((m) => m.messageId === message.messageId)) {
        return state;
      }

      return {
        groupMessages: {
          ...state.groupMessages,
          [groupId]: [...currentMessages, message],
        },
      };
    });

    // Atualizar última mensagem do grupo
    set((state) => ({
      groups: state.groups.map((g) =>
        g.groupId === groupId
          ? {
              ...g,
              lastMessage: message.content,
              lastMessageTime: message.timestamp,
            }
          : g
      ),
    }));
  },

  // Processar eventos recebidos do WebSocket
  handleEvent: (eventType, data) => {
    console.log(`[WebSocketStore] Evento recebido: ${eventType}`, data);

    switch (eventType) {
      case "newMessage":
        // Processar nova mensagem recebida
        if (data && (data.userId || data.groupId)) {
          if (data.userId) {
            // Mensagem privada
            get().updatePrivateMessage(data);
          } else if (data.groupId) {
            // Mensagem de grupo
            get().updateGroupMessage(data);
          }

          // Adicionar notificação
          get().addNotification({
            type: "message",
            message: `Nova mensagem de ${data.senderName || "Alguém"}`,
            variant: "default",
            timestamp: Date.now()
          });
        }
        break;

      case "userStatusChange":
        // Atualizar status de um usuário
        if (data && data.userId) {
          set((state) => ({
            users: state.users.map((u) =>
              u.userId === data.userId
                ? { ...u, status: data.status, lastSeen: data.lastSeen }
                : u
            ),
          }));
        }
        break;

      case "groupUpdate":
        // Atualizar informações de um grupo
        if (data && data.groupId) {
          set((state) => ({
            groups: state.groups.map((g) =>
              g.groupId === data.groupId ? { ...g, ...data } : g
            ),
          }));

          // Limpar cache de membros do grupo
          get().clearGroupMembersCache(data.groupId);
        }
        break;
      
      case "joinGroupRequest":
        // Processar solicitação de entrada em grupo
        console.log("[WebSocketStore] Recebida solicitação de entrada em grupo:", data);
        console.log("[WebSocketStore] Estado atual - grupos:", get().groups);
        console.log("[WebSocketStore] Estado atual - usuário:", get().currentUser);
        
        if (data && data.groupId && data.userId) {
          const currentUser = get().currentUser;
          const groups = get().groups;

          // Tenta identificar o grupo, mas não bloqueia o fluxo caso ainda não tenha sido carregado
          const targetGroup = groups.find((g) => g.groupId === data.groupId);

          // O backend só envia este evento ao administrador do grupo, então se o usuário estiver autenticado podemos assumir que é o admin.
          if (currentUser) {
            console.log("[WebSocketStore] Processando joinGroupRequest para admin", {
              currentUserId: currentUser.userId,
              targetGroupId: data.groupId,
              targetGroup,
            });

            // Adiciona (ou substitui) a solicitação pendente
            set((state) => ({
              pendingGroupRequests: [
                ...state.pendingGroupRequests.filter(
                  (req) => !(req.userId === data.userId && req.groupId === data.groupId)
                ),
                data,
              ],
            }));

            // Mensagem amigável usando o nome do grupo, se disponível; caso contrário usa o ID
            const groupName = targetGroup?.name || data.groupName || data.groupId;

            get().addNotification({
              type: "groupRequest",
              title: "Solicitação de entrada em grupo",
              message: `${data.userName || data.username || "Alguém"} solicitou entrada no grupo ${groupName}`,
              variant: "info",
              timestamp: Date.now(),
              data: {
                userId: data.userId,
                groupId: data.groupId,
                userName: data.userName || data.username,
                groupName,
              },
            });
          } else {
            console.warn("[WebSocketStore] joinGroupRequest recebido sem usuário autenticado", data);
          }
        } else {
          console.warn("[WebSocketStore] Dados de joinGroupRequest incompletos:", data);
        }
        break;

      case "joinGroupAccepted":
        // Processar aceitação de solicitação de entrada em grupo
        console.log("[WebSocketStore] Solicitação de entrada em grupo aceita:", data);
        console.log("[WebSocketStore] Estado atual - grupos antes da atualização:", get().groups);
        console.log("[WebSocketStore] Estado atual - usuário:", get().currentUser);
        
        if (data && data.groupId && data.groupName) {
          // Atualizar lista de grupos
          get().fetchGroups();
          
          // Adicionar notificação
          get().addNotification({
            type: "success",
            title: "Solicitação aceita",
            message: `Sua solicitação para entrar no grupo ${data.groupName} foi aceita!`,
            variant: "success",
            timestamp: Date.now()
          });
        }
        break;
        
      case "joinGroupRejected":
        // Processar rejeição de solicitação de entrada em grupo
        console.log("[WebSocketStore] Solicitação de entrada em grupo rejeitada:", data);
        console.log("[WebSocketStore] Estado atual - usuário:", get().currentUser);
        
        if (data && data.groupId && data.groupName) {
          // Adicionar notificação
          get().addNotification({
            type: "error",
            title: "Solicitação rejeitada",
            message: `Sua solicitação para entrar no grupo ${data.groupName} foi rejeitada.`,
            variant: "error",
            timestamp: Date.now()
          });
        }
        break;
        
      default:
        // Outros tipos de eventos não processados especificamente
        console.log(
          `[WebSocketStore] Evento não processado: ${eventType}`,
          data
        );
    }
  },
}));

export { useWebSocketStore };
