import { useState, useEffect } from 'react'
import Login from './components/Login'
import Dashboard from './components/Dashboard'
import Chat from './components/Chat'
import { useWebSocket, WebSocketProvider } from './contexts/WebSocketContext'
import Notifications from './components/ui/Notifications'
import './App.css'

function AppContent() {
  const [currentView, setCurrentView] = useState('login')
  const [selectedChat, setSelectedChat] = useState(null)
  
  // Usar o contexto WebSocket
  const { 
    isAuthenticated, 
    currentUser, 
    login,
    logout,
    notifications,
    removeNotification,
    getMessages,
    fetchPrivateMessages,
    fetchGroupMessages
  } = useWebSocket()
  
  // Efeito para mudar a view com base na autenticação
  useEffect(() => {
    if (isAuthenticated && currentUser) {
      setCurrentView('dashboard')
    } else {
      setCurrentView('login')
    }
  }, [isAuthenticated, currentUser])

  const handleLogin = async (credentials) => {
    try {
      await login(credentials.email, credentials.password)
      // A mudança de view é feita pelo efeito acima
    } catch (error) {
      console.error('Erro ao fazer login:', error)
      // Aqui poderia mostrar uma mensagem de erro
    }
  }

  const handleLogout = async () => {
    try {
      await logout()
      setSelectedChat(null)
      // A mudança de view é feita pelo efeito acima
    } catch (error) {
      console.error('Erro ao fazer logout:', error)
    }
  }

  const handleSelectChat = (chat) => {
    setSelectedChat(chat)
    setCurrentView('chat')
    
    // Carregar mensagens do chat selecionado
    if (chat.type === 'private') {
      fetchPrivateMessages(chat.userId)
    } else if (chat.type === 'group') {
      fetchGroupMessages(chat.groupId)
    }
  }

  const handleBackToDashboard = () => {
    setSelectedChat(null)
    setCurrentView('dashboard')
  }

  return (
    <div className="App">
      <Notifications />
      
      {currentView === 'login' && (
        <Login onLogin={handleLogin} />
      )}
      
      {currentView === 'dashboard' && currentUser && (
        <Dashboard 
          user={currentUser}
          onSelectChat={handleSelectChat}
          onLogout={handleLogout}
        />
      )}
      
      {currentView === 'chat' && selectedChat && currentUser && (
        <Chat 
          chat={selectedChat}
          onBack={handleBackToDashboard}
        />
      )}
    </div>
  )
}

function App() {
  return (
    <WebSocketProvider>
      <AppContent />
    </WebSocketProvider>
  )
}

export default App
