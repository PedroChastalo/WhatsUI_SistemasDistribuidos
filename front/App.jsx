import { useState } from 'react'
import Login from './components/Login'
import Dashboard from './components/Dashboard'
import Chat from './components/Chat'
import './App.css'

function App() {
  const [currentView, setCurrentView] = useState('login')
  const [currentUser, setCurrentUser] = useState(null)
  const [selectedChat, setSelectedChat] = useState(null)

  const handleLogin = (user) => {
    setCurrentUser(user)
    setCurrentView('dashboard')
  }

  const handleLogout = () => {
    setCurrentUser(null)
    setSelectedChat(null)
    setCurrentView('login')
  }

  const handleSelectChat = (chat) => {
    setSelectedChat(chat)
    setCurrentView('chat')
  }

  const handleBackToDashboard = () => {
    setSelectedChat(null)
    setCurrentView('dashboard')
  }

  return (
    <div className="App">
      {currentView === 'login' && (
        <Login onLogin={handleLogin} />
      )}
      
      {currentView === 'dashboard' && (
        <Dashboard 
          user={currentUser}
          onSelectChat={handleSelectChat}
          onLogout={handleLogout}
        />
      )}
      
      {currentView === 'chat' && selectedChat && (
        <Chat 
          chat={selectedChat}
          currentUser={currentUser}
          onBack={handleBackToDashboard}
        />
      )}
    </div>
  )
}

export default App

