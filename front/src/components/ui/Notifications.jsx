// Notifications.jsx
// Componente para exibir notificações do sistema

import React from 'react';
import { useWebSocketStore } from '@/stores/websocketStore';
import { AnimatePresence, motion } from 'framer-motion';
import { X, Check, AlertCircle, Bell, Info } from 'lucide-react';
import { Button } from '@/components/ui/button';

const Notifications = () => {
  const { notifications, clearNotification, respondToGroupRequest } = useWebSocketStore();

  // Depurar notificações
  console.log('[Notifications] Renderizando notificações:', notifications);

  if (notifications.length === 0) return null;
  
  // Função para responder a uma solicitação de grupo
  const handleRespondToRequest = async (notification, accept) => {
    try {
      if (notification.data && notification.data.userId && notification.data.groupId) {
        await respondToGroupRequest(notification.data.userId, notification.data.groupId, accept);
      }
      clearNotification(notification.id);
    } catch (error) {
      console.error('Erro ao responder à solicitação:', error);
    }
  };
  
  // Função para renderizar o ícone da notificação com base no tipo
  const renderIcon = (type) => {
    switch (type) {
      case 'error':
        return <AlertCircle size={18} className="text-red-500 mr-2 flex-shrink-0" />;
      case 'groupRequest':
        return <Bell size={18} className="text-blue-500 mr-2 flex-shrink-0" />;
      case 'success':
        return <Check size={18} className="text-green-500 mr-2 flex-shrink-0" />;
      case 'message':
        return <Info size={18} className="text-blue-500 mr-2 flex-shrink-0" />;
      default:
        return <Info size={18} className="text-gray-500 mr-2 flex-shrink-0" />;
    }
  };
  
  // Função para renderizar a borda da notificação com base no tipo
  const getBorderColor = (type) => {
    switch (type) {
      case 'error': return 'border-red-500';
      case 'groupRequest': return 'border-blue-500';
      case 'success': return 'border-green-500';
      case 'message': return 'border-blue-500';
      default: return 'border-gray-300';
    }
  };

  return (
    <div className="fixed top-4 right-4 z-50 flex flex-col gap-2 max-w-sm">
      <AnimatePresence>
        {notifications.map((notification) => (
          <motion.div
            key={notification.id}
            initial={{ opacity: 0, y: -20, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -20, scale: 0.95 }}
            className={`bg-white dark:bg-gray-800 rounded-lg shadow-lg p-4 border-l-4 ${getBorderColor(notification.type)}`}
          >
            <div className="flex items-start">
              {renderIcon(notification.type)}
              <div className="flex-1">
                <h4 className="font-semibold text-sm">{notification.title}</h4>
                <p className="text-sm text-gray-600 dark:text-gray-300 mt-1">
                  {notification.message}
                </p>
                <p className="text-xs text-gray-500 mt-1">
                  {new Date(notification.timestamp).toLocaleTimeString()}
                </p>
                
                {/* Botões de ação para solicitações de grupo */}
                {notification.type === 'groupRequest' && notification.data && (
                  <div className="flex space-x-2 mt-2">
                    <Button 
                      size="sm" 
                      variant="outline" 
                      className="bg-green-50 text-green-600 border-green-200 hover:bg-green-100 h-8"
                      onClick={() => handleRespondToRequest(notification, true)}
                    >
                      <Check size={14} className="mr-1" />
                      Aceitar
                    </Button>
                    <Button 
                      size="sm" 
                      variant="outline" 
                      className="bg-red-50 text-red-600 border-red-200 hover:bg-red-100 h-8"
                      onClick={() => handleRespondToRequest(notification, false)}
                    >
                      <X size={14} className="mr-1" />
                      Rejeitar
                    </Button>
                  </div>
                )}
              </div>
              <button
                onClick={() => clearNotification(notification.id)}
                className="text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 ml-2"
              >
                <X size={16} />
              </button>
            </div>
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  );
};

export default Notifications;
