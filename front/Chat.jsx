import { useState, useEffect, useRef } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { 
  ArrowLeft, 
  Phone, 
  Video, 
  MoreVertical, 
  Paperclip, 
  Smile, 
  Send,
  Check,
  CheckCheck,
  Users,
  UserPlus,
  UserMinus,
  Crown
} from 'lucide-react'
import { useRMICallbacks, AnimatedNotification, OnlineStatusIndicator, AnimatedMessage, TypingIndicator } from './RMIAnimations'

export default function Chat({ chat, onBack, currentUser }) {
  const [message, setMessage] = useState('')
  const [messages, setMessages] = useState([])
  const [isTyping, setIsTyping] = useState(false)
  const [showParticipants, setShowParticipants] = useState(false)
  const [notification, setNotification] = useState(null)
  const messagesEndRef = useRef(null)
  
  // Hook para callbacks RMI
  const { 
    newMessage, 
    userStatusChange, 
    typingUsers, 
    simulateNewMessage, 
    simulateStatusChange, 
    simulateTyping 
  } = useRMICallbacks()

  // Dados simulados de mensagens
  useEffect(() => {
    const simulatedMessages = [
      {
        id: 1,
        sender: chat.type === 'group' ? 'Eleanor Pana' : chat.name,
        content: 'Oi! Como você está?',
        timestamp: '10:30',
        status: 'read',
        isOwn: false
      },
      {
        id: 2,
        sender: currentUser,
        content: 'Oi! Estou bem, obrigado. E você?',
        timestamp: '10:32',
        status: 'read',
        isOwn: true
      },
      {
        id: 3,
        sender: chat.type === 'group' ? 'Cody Bafter' : chat.name,
        content: 'Tudo ótimo por aqui! Você viu o novo projeto?',
        timestamp: '10:35',
        status: 'delivered',
        isOwn: false
      }
    ]
    setMessages(simulatedMessages)
  }, [chat, currentUser])

  // Participantes do grupo (se for grupo)
  const participants = chat.type === 'group' ? [
    { id: 1, name: 'Você', isAdmin: true, isOnline: true },
    { id: 2, name: 'Eleanor Pana', isAdmin: false, isOnline: true },
    { id: 3, name: 'Cody Bafter', isAdmin: false, isOnline: true },
    { id: 4, name: 'Darlene Robertson', isAdmin: false, isOnline: false }
  ] : []

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const handleSendMessage = () => {
    if (!message.trim()) return

    const newMessage = {
      id: messages.length + 1,
      sender: currentUser,
      content: message,
      timestamp: new Date().toLocaleTimeString('pt-BR', { 
        hour: '2-digit', 
        minute: '2-digit' 
      }),
      status: 'sent',
      isOwn: true
    }

    setMessages(prev => [...prev, newMessage])
    setMessage('')
    
    // Simular callback RMI para nova mensagem
    simulateNewMessage(newMessage)
    setNotification({ message: 'Mensagem enviada!', type: 'success' })

    // Simular resposta automática com animações RMI
    setTimeout(() => {
      simulateTyping(chat.name, true)
      setIsTyping(true)
      
      setTimeout(() => {
        simulateTyping(chat.name, false)
        setIsTyping(false)
        
        const response = {
          id: messages.length + 2,
          sender: chat.type === 'group' ? 'Eleanor Pana' : chat.name,
          content: 'Obrigado pela mensagem! 😊',
          timestamp: new Date().toLocaleTimeString('pt-BR', { 
            hour: '2-digit', 
            minute: '2-digit' 
          }),
          status: 'delivered',
          isOwn: false
        }
        
        setMessages(prev => [...prev, response])
        simulateNewMessage(response)
        setNotification({ message: 'Nova mensagem recebida!', type: 'info' })
      }, 2000)
    }, 1000)
  }

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSendMessage()
    }
  }

  const getStatusIcon = (status) => {
    switch (status) {
      case 'sent':
        return <Check size={16} className="text-gray-400" />
      case 'delivered':
        return <CheckCheck size={16} className="text-gray-400" />
      case 'read':
        return <CheckCheck size={16} className="text-blue-600" />
      default:
        return null
    }
  }

  return (
    <div className="h-screen bg-white flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-gray-200 bg-white">
        <div className="flex items-center space-x-3">
          <Button variant="ghost" size="sm" onClick={onBack}>
            <ArrowLeft size={20} />
          </Button>
          <Avatar className="h-10 w-10">
            <AvatarFallback className={chat.type === 'group' ? 'bg-blue-100 text-blue-600' : 'bg-gray-200'}>
              {chat.type === 'group' ? <Users size={20} /> : chat.name?.charAt(0).toUpperCase()}
            </AvatarFallback>
          </Avatar>
          <div>
            <h3 className="font-medium text-gray-900">{chat.name}</h3>
            <p className="text-sm text-gray-600">
              {chat.type === 'group' ? `${participants.length} participantes` : 'Online'}
            </p>
          </div>
        </div>
        <div className="flex items-center space-x-2">
          {chat.type === 'user' && (
            <>
              <Button variant="ghost" size="sm">
                <Phone size={20} />
              </Button>
              <Button variant="ghost" size="sm">
                <Video size={20} />
              </Button>
            </>
          )}
          {chat.type === 'group' && (
            <Button 
              variant="ghost" 
              size="sm"
              onClick={() => setShowParticipants(!showParticipants)}
            >
              <Users size={20} />
            </Button>
          )}
          <Button variant="ghost" size="sm">
            <MoreVertical size={20} />
          </Button>
        </div>
      </div>

      <div className="flex flex-1 overflow-hidden">
        {/* Área de mensagens */}
        <div className="flex-1 flex flex-col">
          {/* Mensagens */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4">
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`flex ${msg.isOwn ? 'justify-end' : 'justify-start'}`}
              >
                <div className={`max-w-xs lg:max-w-md ${msg.isOwn ? 'order-2' : 'order-1'}`}>
                  {chat.type === 'group' && !msg.isOwn && (
                    <p className="text-xs text-gray-600 mb-1 ml-3">{msg.sender}</p>
                  )}
                  <div
                    className={`px-4 py-2 rounded-2xl ${
                      msg.isOwn
                        ? 'bg-blue-600 text-white rounded-br-sm'
                        : 'bg-gray-100 text-gray-900 rounded-bl-sm'
                    }`}
                  >
                    <p>{msg.content}</p>
                    <div className={`flex items-center justify-end space-x-1 mt-1 ${
                      msg.isOwn ? 'text-blue-100' : 'text-gray-500'
                    }`}>
                      <span className="text-xs">{msg.timestamp}</span>
                      {msg.isOwn && getStatusIcon(msg.status)}
                    </div>
                  </div>
                </div>
              </div>
            ))}
            
            {isTyping && (
              <div className="flex justify-start">
                <div className="bg-gray-100 rounded-2xl rounded-bl-sm px-4 py-2">
                  <div className="flex space-x-1">
                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></div>
                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0.1s' }}></div>
                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
                  </div>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Input de mensagem */}
          <div className="p-4 border-t border-gray-200 bg-white">
            <div className="flex items-end space-x-2">
              <Button variant="ghost" size="sm">
                <Paperclip size={20} />
              </Button>
              <div className="flex-1 relative">
                <Input
                  placeholder="Digite uma mensagem..."
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  onKeyPress={handleKeyPress}
                  className="pr-10 resize-none"
                />
                <Button variant="ghost" size="sm" className="absolute right-2 top-1/2 -translate-y-1/2">
                  <Smile size={20} />
                </Button>
              </div>
              <Button 
                onClick={handleSendMessage}
                disabled={!message.trim()}
                className="bg-blue-600 hover:bg-blue-700"
              >
                <Send size={20} />
              </Button>
            </div>
          </div>
        </div>

        {/* Sidebar de participantes (apenas para grupos) */}
        {chat.type === 'group' && showParticipants && (
          <div className="w-80 border-l border-gray-200 bg-gray-50 flex flex-col">
            <div className="p-4 border-b border-gray-200 bg-white">
              <h3 className="font-medium text-gray-900">Participantes</h3>
              <p className="text-sm text-gray-600">{participants.length} membros</p>
            </div>
            
            <div className="flex-1 overflow-y-auto p-4">
              <div className="space-y-2">
                {participants.map((participant) => (
                  <div key={participant.id} className="flex items-center justify-between p-3 bg-white rounded-lg">
                    <div className="flex items-center space-x-3">
                      <div className="relative">
                        <Avatar className="h-10 w-10">
                          <AvatarFallback className="bg-gray-200">
                            {participant.name.charAt(0).toUpperCase()}
                          </AvatarFallback>
                        </Avatar>
                        {participant.isOnline && (
                          <div className="absolute -bottom-1 -right-1 w-4 h-4 bg-green-500 rounded-full border-2 border-white"></div>
                        )}
                      </div>
                      <div>
                        <h4 className="font-medium text-gray-900">{participant.name}</h4>
                        <p className="text-sm text-gray-600">
                          {participant.isOnline ? 'Online' : 'Offline'}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center space-x-2">
                      {participant.isAdmin && (
                        <Crown size={16} className="text-yellow-500" />
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Controles de admin */}
            <div className="p-4 border-t border-gray-200 bg-white space-y-2">
              <Button variant="outline" className="w-full justify-start">
                <UserPlus size={16} className="mr-2" />
                Adicionar participante
              </Button>
              <Button variant="outline" className="w-full justify-start text-red-600 hover:text-red-700">
                <UserMinus size={16} className="mr-2" />
                Remover participante
              </Button>
              <Button variant="outline" className="w-full justify-start">
                <Crown size={16} className="mr-2" />
                Transferir liderança
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

