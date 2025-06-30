// WebSocket Client para WhatsUT
// Este cliente gerencia a conexão WebSocket e fornece métodos para enviar e receber mensagens

import { v4 as uuidv4 } from 'uuid';

class WebSocketClient {
  constructor() {
    this.socket = null;
    this.isConnected = false;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectTimeout = null;
    this.reconnectTimer = null;
    this.heartbeatInterval = null;
    this.heartbeatTimeout = null;
    this.pendingRequests = new Map();
    this.eventListeners = new Map();
    this.mockMode = false; // Desativando o modo mock para usar o backend real
    this.url = 'ws://localhost:8080/whatsut';
    this.globalEventHandler = null;
  }

  // Conectar ao servidor WebSocket
  connect(url, sessionId) {
    // Atualizar URL se fornecida
    if (url) {
      this.url = url;
    }
    
    // Limpar qualquer temporizador de reconexão existente
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    
    // Limpar heartbeat se existir
    this.stopHeartbeat();
    
    if (this.mockMode) {
      console.log('[WebSocket] Modo mock ativado. Simulando conexão.');
      this.simulateConnection();
      return;
    }

    // Verificar se já existe uma conexão ativa
    if (this.socket) {
      const currentState = this.socket.readyState;
      
      // Se já estiver conectado ou conectando, não fazer nada
      if (currentState === WebSocket.OPEN) {
        // Removido log de conexão ativa
        return;
      }
      
      if (currentState === WebSocket.CONNECTING) {
        // Removido log de conexão em andamento
        return;
      }
      
      // Se estiver fechando, aguardar antes de tentar novamente
      if (currentState === WebSocket.CLOSING) {
        console.log('[WebSocket] Conexão está fechando, aguardando...');
        this.reconnectTimer = setTimeout(() => this.connect(), 500);
        return;
      }
    }

    try {
      // Construir a URL com o token de sessão se disponível
      let connectionUrl = this.url;
      if (sessionId) {
        // Adicionar o token como parâmetro de query
        const separator = connectionUrl.includes('?') ? '&' : '?';
        connectionUrl = `${connectionUrl}${separator}sessionId=${sessionId}`;
        console.log(`[WebSocket] Conectando com sessionId`);
      } else {
        console.log('[WebSocket] Conectando sem sessionId');
      }
      
      // Conexão simplificada, sem mostrar a URL completa
      this.socket = new WebSocket(connectionUrl);

      this.socket.onopen = this.onOpen.bind(this);
      this.socket.onclose = this.onClose.bind(this);
      this.socket.onmessage = this.onMessage.bind(this);
      this.socket.onerror = this.onError.bind(this);
    } catch (error) {
      console.error('[WebSocket] Erro ao criar conexão:', error);
      this.attemptReconnect();
    }
  }

  // Manipuladores de eventos WebSocket
  onOpen() {
    console.log('[WebSocket] Conexão estabelecida');
    this.isConnected = true;
    this.reconnectAttempts = 0;
    
    // Notificar sobre mudança de status de conexão
    this.dispatchEvent('connectionChange', true);
    
    // Iniciar heartbeat para manter a conexão ativa
    this.startHeartbeat();
    
    // Enviar um ping para verificar se a conexão está realmente funcional
    try {
      this.socket.send(JSON.stringify({ type: 'ping' }));
    } catch (error) {
      console.warn('[WebSocket] Erro ao enviar ping inicial');
    }
  }
  
  // Iniciar heartbeat para manter a conexão ativa
  startHeartbeat() {
    this.stopHeartbeat(); // Limpar qualquer heartbeat existente
    
    // Enviar ping a cada 30 segundos
    this.heartbeatInterval = setInterval(() => {
      if (this.isConnected && this.socket && this.socket.readyState === WebSocket.OPEN) {
        try {
          this.socket.send(JSON.stringify({ type: 'ping' }));
          
          // Definir um timeout para verificar se recebemos resposta
          this.heartbeatTimeout = setTimeout(() => {
            console.warn('[WebSocket] Timeout de ping, reconectando...');
            this.reconnect();
          }, 5000); // Esperar 5 segundos pela resposta
        } catch (error) {
          this.reconnect();
        }
      } else {
        this.reconnect();
      }
    }, 30000); // 30 segundos
  }
  
  // Parar heartbeat
  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
    
    if (this.heartbeatTimeout) {
      clearTimeout(this.heartbeatTimeout);
      this.heartbeatTimeout = null;
    }
  }
  
  onClose(event) {
    console.log(`[WebSocket] Conexão fechada: ${event.code} ${event.reason || 'Sem motivo'}`);
    this.isConnected = false;
    this.stopHeartbeat();
    
    // Notificar sobre mudança de status de conexão
    this.dispatchEvent('connectionChange', false);
    
    if (event && event.code !== 1000) {
      console.warn(`[WebSocket] Conexão fechada com código: ${event.code}, razão: ${event.reason || 'Desconhecida'}`);
      this.attemptReconnect();
    } else {
      console.log('[WebSocket] Conexão fechada normalmente');
    }
  }
  
  onMessage(event) {
    try {
      this.handleMessage(event.data);
    } catch (error) {
      console.error('[WebSocket] Erro ao processar mensagem recebida:', error);
    }
  }
  
  onError(error) {
    console.error('[WebSocket] Erro na conexão:', error);
    this.dispatchEvent('connectionChange', false);
    // Não precisamos chamar attemptReconnect() aqui porque onClose será chamado automaticamente após um erro
  }
  
  // Simular conexão para modo mock
  simulateConnection() {
    setTimeout(() => {
      this.isConnected = true;
      this.dispatchEvent('connectionChange', true);
      console.log('[WebSocket Mock] Conexão simulada estabelecida.');
    }, 500);
  }

  // Reconectar imediatamente
  reconnect(sessionId) {
    console.log('[WebSocket] Reconectando imediatamente...');
    
    // Limpar qualquer temporizador de reconexão existente
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
    
    // Fechar a conexão existente se estiver aberta
    if (this.socket) {
      try {
        this.socket.close();
      } catch (error) {
        console.warn('[WebSocket] Erro ao fechar conexão existente:', error);
      }
    }
    
    // Pequeno delay para garantir que a conexão anterior foi fechada
    setTimeout(() => {
      this.connect(null, sessionId);
    }, 100);
    
    // Retornar uma promise que resolve quando a conexão for estabelecida
    return this.waitForConnection();
  }
  
  // Tentar reconectar automaticamente com backoff exponencial
  attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log('[WebSocket] Número máximo de tentativas de reconexão atingido.');
      return;
    }

    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
    }

    this.reconnectAttempts++;
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
    
    console.log(`[WebSocket] Tentando reconectar em ${delay/1000} segundos...`);
    
    this.reconnectTimeout = setTimeout(() => {
      console.log(`[WebSocket] Tentativa de reconexão ${this.reconnectAttempts}...`);
      this.connect();
    }, delay);
  }

  // Desconectar do servidor
  disconnect() {
    if (this.mockMode) {
      console.log('[WebSocket Mock] Desconectando...');
      this.isConnected = false;
      useWebSocketStore.getState().setConnectionStatus(false);
      return;
    }

    if (this.socket && this.isConnected) {
      this.socket.close();
    }
  }

  // Enviar requisição para o servidor
  async sendRequest(type, data = {}) {
    // Se estiver em modo mock, usar respostas simuladas
    if (this.mockMode) {
      return this.getMockResponse(type, data);
    }
    
    // Aguardar conexão antes de enviar
    try {
      console.log(`[WebSocket] Aguardando conexão para enviar requisição: ${type}`);
      await this.waitForConnection();
    } catch (error) {
      console.error(`[WebSocket] Falha ao estabelecer conexão para requisição ${type}:`, error);
      throw new Error(`Não foi possível estabelecer conexão: ${error.message}`);
    }
    
    // Gerar ID único para a requisição
    const requestId = uuidv4();
    
    // Criar objeto de requisição
    const request = {
      requestId,
      type,
      data
    };

    return new Promise((resolve, reject) => {
      // Armazenar callbacks para resolver/rejeitar a Promise quando a resposta chegar
      this.pendingRequests.set(requestId, { resolve, reject });
      
      try {
        // Verificar novamente se a conexão está aberta antes de enviar
        if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
          throw new Error('WebSocket não está conectado');
        }
        
        console.log(`[WebSocket] Enviando requisição: ${type}`, data);
        // Enviar requisição serializada
        this.socket.send(JSON.stringify(request));
      } catch (error) {
        // Remover da lista de pendentes e rejeitar a Promise
        this.pendingRequests.delete(requestId);
        console.error(`[WebSocket] Erro ao enviar requisição ${type}:`, error);
        reject(new Error(`Erro ao enviar requisição: ${error.message}`));
      }
      
      // Definir timeout para a requisição
      setTimeout(() => {
        if (this.pendingRequests.has(requestId)) {
          this.pendingRequests.delete(requestId);
          console.warn(`[WebSocket] Timeout da requisição ${type} (ID: ${requestId})`);
          reject(new Error(`Tempo limite da requisição ${type} excedido`));
        }
      }, 10000); // 10 segundos de timeout
    });
  }

  // Lidar com mensagens recebidas do servidor
  handleMessage(data) {
    if (!data) {
      console.warn('[WebSocket] Mensagem vazia recebida');
      return;
    }
    
    try {
      // Verificar se é uma string JSON válida
      if (typeof data !== 'string') {
        console.warn('[WebSocket] Mensagem recebida não é uma string:', typeof data);
        return;
      }
      
      const message = JSON.parse(data);
      
      // Lidar com mensagens de ping/pong para manter a conexão ativa
      if (message.type === 'ping') {
        // Removido log de ping
        try {
          this.socket.send(JSON.stringify({ type: 'pong' }));
        } catch (error) {
          // Removido log de erro pong
        }
        return;
      }
      
      if (message.type === 'pong') {
        // Removido log de pong
        // Limpar o timeout do heartbeat pois recebemos resposta
        if (this.heartbeatTimeout) {
          clearTimeout(this.heartbeatTimeout);
          this.heartbeatTimeout = null;
        }
        return;
      }
      
      // Se é uma resposta a uma requisição
      if (message.requestId && this.pendingRequests.has(message.requestId)) {
        const { resolve, reject } = this.pendingRequests.get(message.requestId);
        this.pendingRequests.delete(message.requestId);
        
        if (message.success) {
          resolve(message.data);
        } else {
          reject(new Error(message.error || 'Erro desconhecido'));
        }
        return;
      }
      
      // Se é um evento do servidor
      if (message.type) {
        this.dispatchEvent(message.type, message.data);
      } else {
        console.warn('[WebSocket] Mensagem sem tipo recebida:', message);
      }
    } catch (error) {
      console.error('[WebSocket] Erro ao processar mensagem:', error, 'Dados recebidos:', data);
    }
  }

  // Registrar um ouvinte para eventos
  addEventListener(eventType, callback) {
    if (!this.eventListeners.has(eventType)) {
      this.eventListeners.set(eventType, new Set());
    }
    
    this.eventListeners.get(eventType).add(callback);
    return () => this.removeEventListener(eventType, callback);
  }

  // Remover um ouvinte de eventos
  removeEventListener(eventType, callback) {
    if (this.eventListeners.has(eventType)) {
      this.eventListeners.get(eventType).delete(callback);
    }
  }

  // Registrar um handler global para todos os eventos
  setGlobalEventHandler(handler) {
    this.globalEventHandler = handler;
  }

  // Disparar um evento para todos os ouvintes registrados
  dispatchEvent(eventType, data) {
    // Notificar listeners específicos para este tipo de evento
    if (this.eventListeners.has(eventType)) {
      this.eventListeners.get(eventType).forEach(callback => {
        try {
          callback(data);
        } catch (error) {
          console.error(`[WebSocket] Erro ao executar callback para evento ${eventType}:`, error);
        }
      });
    }
    
    // Notificar o handler global se existir
    if (this.globalEventHandler) {
      try {
        this.globalEventHandler(eventType, data);
      } catch (error) {
        console.error(`[WebSocket] Erro ao executar handler global para evento ${eventType}:`, error);
      }
    }
  }

  // Método vazio para manter compatibilidade com código existente
  handleMockRequest(request, resolve, reject) {
    reject(new Error('Modo mock desativado. Usando conexão real com o backend.'));
  }
  
  // Aguardar conexão antes de enviar requisição
  async waitForConnection(maxAttempts = 10, timeout = 500) {
    return new Promise((resolve, reject) => {
      let attempts = 0;
      
      const checkConnection = () => {
        attempts++;
        
        if (this.isConnected && this.socket && this.socket.readyState === WebSocket.OPEN) {
          console.log(`[WebSocket] Conexão estabelecida após ${attempts} tentativas`);
          resolve();
          return;
        }
        
        // Se atingiu o número máximo de tentativas, rejeitar a Promise
        if (attempts >= maxAttempts) {
          console.error(`[WebSocket] Não foi possível conectar após ${maxAttempts} tentativas`);
          reject(new Error(`Tempo limite de conexão excedido após ${maxAttempts} tentativas`));
          return;
        }
        
        // Tentar novamente após o timeout (log apenas na primeira tentativa)
        if (attempts === 1) {
          console.log(`[WebSocket] Aguardando conexão...`);
        }
        setTimeout(checkConnection, timeout);
      };
      
      // Iniciar verificação
      checkConnection();
    });
  }
}

// Singleton
const websocketClient = new WebSocketClient();
export default websocketClient;
