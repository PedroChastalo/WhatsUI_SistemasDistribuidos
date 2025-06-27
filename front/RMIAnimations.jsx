import { useState, useEffect } from 'react'

// Hook para simular callbacks RMI com animações
export const useRMICallbacks = () => {
  const [newMessage, setNewMessage] = useState(null)
  const [userStatusChange, setUserStatusChange] = useState(null)
  const [typingUsers, setTypingUsers] = useState([])

  // Simular callback de nova mensagem
  const simulateNewMessage = (message) => {
    setNewMessage(message)
    // Limpar após animação
    setTimeout(() => setNewMessage(null), 1000)
  }

  // Simular callback de mudança de status
  const simulateStatusChange = (userId, status) => {
    setUserStatusChange({ userId, status })
    // Limpar após animação
    setTimeout(() => setUserStatusChange(null), 2000)
  }

  // Simular callback de digitação
  const simulateTyping = (userId, isTyping) => {
    if (isTyping) {
      setTypingUsers(prev => [...prev.filter(id => id !== userId), userId])
    } else {
      setTypingUsers(prev => prev.filter(id => id !== userId))
    }
  }

  return {
    newMessage,
    userStatusChange,
    typingUsers,
    simulateNewMessage,
    simulateStatusChange,
    simulateTyping
  }
}

// Componente de notificação animada
export const AnimatedNotification = ({ message, type = 'info', onClose }) => {
  useEffect(() => {
    const timer = setTimeout(onClose, 3000)
    return () => clearTimeout(timer)
  }, [onClose])

  const bgColor = {
    success: 'bg-green-500',
    error: 'bg-red-500',
    info: 'bg-blue-500',
    warning: 'bg-orange-500'
  }[type] || 'bg-blue-500'

  return (
    <div className={`fixed top-4 right-4 ${bgColor} text-white px-4 py-2 rounded-lg shadow-lg z-50 animate-slide-in-right`}>
      <div className="flex items-center space-x-2">
        <span>{message}</span>
        <button onClick={onClose} className="ml-2 text-white hover:text-gray-200">
          ×
        </button>
      </div>
    </div>
  )
}

// Componente de indicador de status online animado
export const OnlineStatusIndicator = ({ isOnline, animate = false }) => {
  return (
    <div className="relative">
      <div 
        className={`w-3 h-3 rounded-full ${
          isOnline ? 'bg-green-500' : 'bg-gray-400'
        } ${animate ? 'animate-pulse' : ''}`}
      />
      {animate && isOnline && (
        <div className="absolute inset-0 w-3 h-3 bg-green-500 rounded-full animate-ping opacity-75" />
      )}
    </div>
  )
}

// Componente de mensagem com animação de entrada
export const AnimatedMessage = ({ message, isOwn, children }) => {
  return (
    <div 
      className={`animate-fade-in-up ${isOwn ? 'justify-end' : 'justify-start'} flex`}
      style={{ animationDelay: '0.1s' }}
    >
      {children}
    </div>
  )
}

// Componente de indicador de digitação animado
export const TypingIndicator = ({ users = [] }) => {
  if (users.length === 0) return null

  const displayText = users.length === 1 
    ? `${users[0]} está digitando...`
    : `${users.length} pessoas estão digitando...`

  return (
    <div className="flex items-center space-x-2 text-gray-500 text-sm animate-fade-in">
      <div className="flex space-x-1">
        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" />
        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0.1s' }} />
        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }} />
      </div>
      <span>{displayText}</span>
    </div>
  )
}

