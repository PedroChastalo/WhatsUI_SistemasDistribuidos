// WebSocket Client para WhatsUT
// Este cliente gerencia a conexão WebSocket e fornece métodos para enviar e receber mensagens

import { v4 as uuidv4 } from 'uuid';
import { useWebSocketStore } from '@/stores/websocketStore';
import mockResponses from './mockResponses.js';

class WebSocketClient {
  constructor() {
    this.socket = null;
    this.isConnected = false;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectTimeout = null;
    this.pendingRequests = new Map();
    this.eventListeners = new Map();
    this.mockMode = true; // Modo mock ativado por padrão até termos o backend
  }

  // Conectar ao servidor WebSocket
  connect(url = 'ws://localhost:8080/whatsut') {
    if (this.mockMode) {
      console.log('[WebSocket] Modo mock ativado. Simulando conexão.');
      this.simulateConnection();
      return;
    }

    if (this.socket && this.isConnected) {
      console.log('[WebSocket] Já conectado.');
      return;
    }

    try {
      this.socket = new WebSocket(url);
      
      this.socket.onopen = () => {
        console.log('[WebSocket] Conexão estabelecida.');
        this.isConnected = true;
        this.reconnectAttempts = 0;
        useWebSocketStore.getState().setConnectionStatus(true);
      };

      this.socket.onmessage = (event) => {
        this.handleMessage(event.data);
      };

      this.socket.onclose = () => {
        console.log('[WebSocket] Conexão fechada.');
        this.isConnected = false;
        useWebSocketStore.getState().setConnectionStatus(false);
        this.attemptReconnect();
      };

      this.socket.onerror = (error) => {
        console.error('[WebSocket] Erro:', error);
        useWebSocketStore.getState().setConnectionStatus(false);
      };
    } catch (error) {
      console.error('[WebSocket] Erro ao conectar:', error);
      this.attemptReconnect();
    }
  }

  // Simular conexão para modo mock
  simulateConnection() {
    setTimeout(() => {
      this.isConnected = true;
      useWebSocketStore.getState().setConnectionStatus(true);
      console.log('[WebSocket Mock] Conexão simulada estabelecida.');
    }, 500);
  }

  // Tentar reconectar automaticamente
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

  // Enviar uma requisição para o servidor
  sendRequest(action, data = {}) {
    const requestId = uuidv4();
    const request = {
      requestId,
      action,
      data
    };

    return new Promise((resolve, reject) => {
      if (this.mockMode) {
        // Em modo mock, simulamos respostas do servidor
        this.handleMockRequest(request, resolve, reject);
        return;
      }

      if (!this.isConnected) {
        reject(new Error('WebSocket não está conectado'));
        return;
      }

      // Armazenar a promessa para resolver quando a resposta chegar
      this.pendingRequests.set(requestId, { resolve, reject, timestamp: Date.now() });
      
      try {
        this.socket.send(JSON.stringify(request));
      } catch (error) {
        this.pendingRequests.delete(requestId);
        reject(error);
      }

      // Timeout para a requisição
      setTimeout(() => {
        if (this.pendingRequests.has(requestId)) {
          this.pendingRequests.delete(requestId);
          reject(new Error('Timeout da requisição'));
        }
      }, 10000); // 10 segundos de timeout
    });
  }

  // Lidar com mensagens recebidas do servidor
  handleMessage(data) {
    try {
      const message = JSON.parse(data);
      
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
      }
    } catch (error) {
      console.error('[WebSocket] Erro ao processar mensagem:', error);
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

  // Disparar um evento para todos os ouvintes registrados
  dispatchEvent(eventType, data) {
    if (this.eventListeners.has(eventType)) {
      this.eventListeners.get(eventType).forEach(callback => {
        try {
          callback(data);
        } catch (error) {
          console.error(`[WebSocket] Erro ao executar callback para evento ${eventType}:`, error);
        }
      });
    }

    // Também notificar a store global
    useWebSocketStore.getState().handleEvent(eventType, data);
  }

  // Simular respostas do servidor em modo mock
  handleMockRequest(request, resolve, reject) {
    const { action, data } = request;
    
    console.log(`[WebSocket Mock] Requisição recebida: ${action}`, data);
    
    // Simular latência de rede
    setTimeout(() => {
      try {
        const mockResponse = this.getMockResponse(action, data);
        if (mockResponse.success) {
          resolve(mockResponse.data);
          
          // Se houver eventos associados a esta ação, dispará-los após um pequeno delay
          if (mockResponse.events) {
            mockResponse.events.forEach(event => {
              setTimeout(() => {
                this.dispatchEvent(event.type, event.data);
              }, event.delay || 500);
            });
          }
        } else {
          reject(new Error(mockResponse.error));
        }
      } catch (error) {
        reject(error);
      }
    }, Math.random() * 300 + 100); // Latência entre 100ms e 400ms
  }

  // Obter resposta mockada com base na ação
  getMockResponse(action, data) {
    // Usar as respostas mockadas importadas no topo do arquivo
    if (mockResponses[action]) {
      return mockResponses[action](data);
    }
    
    return {
      success: false,
      error: `Ação não implementada no modo mock: ${action}`
    };
  }
}

// Singleton
const websocketClient = new WebSocketClient();
export default websocketClient;
