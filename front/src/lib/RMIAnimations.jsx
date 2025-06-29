import { useEffect, useRef } from 'react';

// Hook personalizado para lidar com callbacks do RMI
export function useRMICallbacks() {
  const callbacks = useRef({});

  // Registra um callback para um evento específico
  const registerCallback = (eventName, callback) => {
    callbacks.current[eventName] = callback;
  };

  // Remove um callback registrado
  const unregisterCallback = (eventName) => {
    delete callbacks.current[eventName];
  };

  // Executa um callback para um evento específico
  const executeCallback = (eventName, data) => {
    const callback = callbacks.current[eventName];
    if (callback && typeof callback === 'function') {
      return callback(data);
    }
    return null;
  };

  // Limpa todos os callbacks quando o componente é desmontado
  useEffect(() => {
    return () => {
      callbacks.current = {};
    };
  }, []);

  return {
    registerCallback,
    unregisterCallback,
    executeCallback
  };
}

// Funções de animação para mensagens
export const messageAnimations = {
  fadeIn: (element) => {
    if (!element) return;
    element.style.opacity = '0';
    element.style.transform = 'translateY(10px)';
    
    setTimeout(() => {
      element.style.transition = 'opacity 0.3s ease-out, transform 0.3s ease-out';
      element.style.opacity = '1';
      element.style.transform = 'translateY(0)';
    }, 10);
  },
  
  slideIn: (element) => {
    if (!element) return;
    element.style.opacity = '0';
    element.style.transform = 'translateX(20px)';
    
    setTimeout(() => {
      element.style.transition = 'opacity 0.3s ease-out, transform 0.3s ease-out';
      element.style.opacity = '1';
      element.style.transform = 'translateX(0)';
    }, 10);
  }
};

// Funções de utilidade para WebSocket
export const websocketUtils = {
  formatMessage: (message) => {
    return {
      ...message,
      timestamp: message.timestamp || new Date().toISOString(),
      formatted: true
    };
  },
  
  parseMessage: (rawMessage) => {
    try {
      if (typeof rawMessage === 'string') {
        return JSON.parse(rawMessage);
      }
      return rawMessage;
    } catch (error) {
      console.error('Failed to parse message:', error);
      return null;
    }
  }
};

export default {
  useRMICallbacks,
  messageAnimations,
  websocketUtils
};
