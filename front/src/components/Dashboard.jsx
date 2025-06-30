import { useState, useEffect } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Search, Plus, MessageCircle, Users, Settings, LogOut, AlertCircle } from 'lucide-react'
import CreateGroupModal from './CreateGroupModal'
import { useWebSocket } from '@/contexts/WebSocketContext'

export default function Dashboard({ onSelectChat, onLogout }) {
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedTab, setSelectedTab] = useState('recent') // Apenas 'recent' e 'groups' agora
  const [showCreateGroupModal, setShowCreateGroupModal] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  
  // Usar o contexto WebSocket
  const { 
    currentUser, 
    users, 
    groups, 
    conversations, 
    getUsers, 
    getGroups, 
    getConversations,
    createGroup,
    logout
  } = useWebSocket()
  
  // Carregar dados ao montar o componente
  useEffect(() => {
    const loadData = async () => {
      setIsLoading(true)
      setError('')
      try {
        await Promise.all([
          getUsers(),
          getGroups(),
          getConversations()
        ])
      } catch (err) {
        setError('Erro ao carregar dados: ' + (err.message || 'Desconhecido'))
      } finally {
        setIsLoading(false)
      }
    }
    
    loadData()
  }, [])
  
  // Função para criar um novo grupo
  const handleCreateGroup = async (newGroup) => {
    try {
      await createGroup(newGroup)
      setSelectedTab('groups')
      setShowCreateGroupModal(false)
    } catch (err) {
      setError('Erro ao criar grupo: ' + (err.message || 'Desconhecido'))
    }
  }
  
  // Função para fazer logout
  const handleLogout = async () => {
    try {
      await logout()
      onLogout()
    } catch (err) {
      setError('Erro ao fazer logout: ' + (err.message || 'Desconhecido'))
    }
  }

  // Combinar usuários e conversas recentes
  // Primeiro, vamos criar um mapa de conversas existentes por userId
  const conversationMap = {};
  (conversations || []).forEach(chat => {
    if (chat.userId) {
      conversationMap[chat.userId] = chat;
    }
  });
  
  // Agora, vamos criar uma lista combinada de todos os usuários
  // Se um usuário já tem uma conversa, usamos os dados da conversa
  // Caso contrário, criamos uma entrada para o usuário
  const allUsers = users.map(user => {
    // Não mostrar o usuário atual na lista
    if (user.userId === currentUser?.userId) return null;
    
    // Se já existe uma conversa com este usuário, use os dados da conversa
    if (conversationMap[user.userId]) {
      return conversationMap[user.userId];
    }
    
    // Caso contrário, crie uma entrada para o usuário
    return {
      ...user,
      type: 'private',
      lastMessage: 'Iniciar conversa',
      timestamp: null
    };
  }).filter(Boolean); // Remove entradas nulas (usuário atual)
  
  // Filtrar e ordenar a lista combinada
  const filteredChats = allUsers
    .filter(chat => {
      const searchName = chat?.displayName || chat?.username || '';
      return searchName.toLowerCase().includes(searchQuery.toLowerCase());
    })
    .sort((a, b) => {
      // Primeiro ordenar por timestamp (conversas com mensagens primeiro)
      if (a.timestamp && !b.timestamp) return -1;
      if (!a.timestamp && b.timestamp) return 1;
      if (a.timestamp && b.timestamp) {
        return new Date(b.timestamp) - new Date(a.timestamp);
      }
      // Se ambos não têm timestamp, ordenar por nome
      return (a.displayName || a.username || '').localeCompare(b.displayName || b.username || '');
    });

  // Filtrar grupos
  const filteredGroups = groups
    .filter(group => group.name.toLowerCase().includes(searchQuery.toLowerCase()))

  return (
    <div className="h-screen bg-gray-50 flex">
      {/* Sidebar */}
      <div className="w-80 bg-white border-r border-gray-200 flex flex-col">
        {/* Header do usuário */}
        <div className="p-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <Avatar className="h-10 w-10">
                <AvatarFallback className="bg-blue-600 text-white">
                  {currentUser?.displayName?.charAt(0).toUpperCase() || '?'}
                </AvatarFallback>
              </Avatar>
              <div>
                <h3 className="font-medium text-gray-900">{currentUser?.displayName || 'Usuário'}</h3>
                <p className="text-sm text-green-600">Online</p>
              </div>
            </div>
            <div className="flex items-center space-x-2">
              <Button variant="ghost" size="sm" onClick={handleLogout}>
                <LogOut size={16} />
              </Button>
            </div>
          </div>
        </div>

        {/* Busca */}
        <div className="p-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
            <Input
              placeholder="Buscar usuários e grupos"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
          </div>
        </div>

        {/* Tabs */}
        <div className="px-4">
          <div className="flex space-x-1 bg-gray-100 p-1 rounded-lg">
            <button
              onClick={() => setSelectedTab('recent')}
              className={`flex-1 py-2 px-3 text-sm font-medium rounded-md transition-colors ${
                selectedTab === 'recent'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Recentes
            </button>
            <button
              onClick={() => setSelectedTab('groups')}
              className={`flex-1 py-2 px-3 text-sm font-medium rounded-md transition-colors ${
                selectedTab === 'groups'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Grupos
            </button>
          </div>
        </div>

        {/* Área de conteúdo */}
        <div className="flex-1 overflow-y-auto p-4">
          {/* Exibir mensagem de carregamento */}
          {isLoading && (
            <div className="flex items-center justify-center h-full">
              <div className="text-center">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
                <p className="text-gray-600">Carregando dados...</p>
              </div>
            </div>
          )}
          
          {/* Exibir mensagem de erro */}
          {error && (
            <div className="bg-red-50 p-4 rounded-lg mb-4 flex items-center gap-2">
              <AlertCircle size={20} className="text-red-600" />
              <p className="text-red-600">{error}</p>
            </div>
          )}
          
          {/* Conversas recentes */}
          {!isLoading && selectedTab === 'recent' && (
            <div className="space-y-2">
              <h4 className="text-sm font-medium text-gray-700 mb-3">Usuários ({filteredChats.length})</h4>
              {filteredChats.length === 0 ? (
                <p className="text-gray-500 text-center py-4">Nenhuma conversa recente</p>
              ) : (
                filteredChats.map((chat) => (
                  <div
                    key={chat.id}
                    onClick={() => onSelectChat(chat)}
                    className="flex items-center space-x-3 p-3 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors"
                  >
                    <Avatar className="h-10 w-10">
                      <AvatarFallback className={chat.type === 'group' ? 'bg-blue-100 text-blue-600' : 'bg-gray-200'}>
                        {chat.type === 'group' ? <Users size={20} /> : (chat.displayName || chat.username || '?').charAt(0)}
                      </AvatarFallback>
                    </Avatar>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between">
                        <h4 className="font-medium text-gray-900 truncate">{chat.displayName || chat.username || 'Usuário'}</h4>
                        <span className="text-xs text-gray-500">
                          {chat.timestamp ? new Date(chat.timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) : ''}
                        </span>
                      </div>
                      <p className="text-sm text-gray-600 truncate">{chat.lastMessage}</p>
                    </div>
                    {/* Removido o badge de mensagens não lidas conforme solicitado */}
                  </div>
                ))
              )}
            </div>
          )}

          {!isLoading && selectedTab === 'groups' && (
            <div className="space-y-2">
              <div className="flex items-center justify-between mb-3">
                <h4 className="text-sm font-medium text-gray-700">Grupos ({filteredGroups.length})</h4>
                <Button size="sm" className="bg-blue-600 hover:bg-blue-700" onClick={() => setShowCreateGroupModal(true)}>
                  <Plus size={16} className="mr-1" />
                  Novo
                </Button>
              </div>
              {filteredGroups.length === 0 ? (
                <p className="text-gray-500 text-center py-4">Nenhum grupo encontrado</p>
              ) : (
                filteredGroups.map((group) => (
                  <div
                    key={group.id}
                    onClick={() => onSelectChat({ ...group, type: 'group' })}
                    className="flex items-center space-x-3 p-3 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors"
                  >
                    <Avatar className="h-10 w-10">
                      <AvatarFallback className="bg-blue-100 text-blue-600">
                        <Users size={20} />
                      </AvatarFallback>
                    </Avatar>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between">
                        <h4 className="font-medium text-gray-900 truncate">{group.name}</h4>
                        {group.unreadCount > 0 && (
                          <Badge className="bg-blue-600 text-white min-w-[20px] h-5 text-xs">
                            {group.unreadCount}
                          </Badge>
                        )}
                      </div>
                      <p className="text-sm text-gray-600 truncate">{group.lastMessage || 'Sem mensagens'}</p>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}
        </div>
      </div>

      {/* Área principal */}
      <div className="flex-1 flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <MessageCircle size={64} className="mx-auto text-gray-400 mb-4" />
          <h3 className="text-xl font-medium text-gray-900 mb-2">Bem-vindo ao WhatsUT</h3>
          <p className="text-gray-600">Selecione uma conversa para começar a conversar</p>
          {currentUser && (
            <div className="mt-4 text-sm text-gray-500">
              Conectado como <span className="font-medium">{currentUser.displayName}</span>
            </div>
          )}
        </div>
      </div>

      <CreateGroupModal 
        isOpen={showCreateGroupModal}
        onClose={() => setShowCreateGroupModal(false)}
        onCreateGroup={handleCreateGroup}
      />
    </div>
  )
}

