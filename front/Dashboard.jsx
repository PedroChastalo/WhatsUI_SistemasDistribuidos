import { useState, useEffect } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Search, Plus, MessageCircle, Users, Settings, LogOut } from 'lucide-react'
import CreateGroupModal from './CreateGroupModal'

export default function Dashboard({ user, onSelectChat, onLogout }) {
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedTab, setSelectedTab] = useState('recent')
  const [showCreateGroupModal, setShowCreateGroupModal] = useState(false)

  // Dados simulados
  const [onlineUsers] = useState([
    { id: 1, name: 'Eleanor Pana', status: 'online', avatar: 'EP' },
    { id: 2, name: 'Darlene Robertson', status: 'online', avatar: 'DR' },
    { id: 3, name: 'Cody Bafter', status: 'online', avatar: 'CB' },
    { id: 4, name: 'Savannah Nguyen', status: 'online', avatar: 'SN' }
  ])

  const [groups, setGroups] = useState([
    { id: 1, name: 'Equipe Dev', unread: 3, lastMessage: 'Nova feature pronta!' },
    { id: 2, name: 'Projeto Alpha', unread: 1, lastMessage: 'Reunião amanhã às 14h' },
    { id: 3, name: 'Design Team', unread: 0, lastMessage: 'Wireframes aprovados' }
  ])

  const handleCreateGroup = (newGroup) => {
    setGroups(prev => [...prev, newGroup])
    setSelectedTab('groups')
  }

  const [recentChats] = useState([
    { id: 1, name: 'Eleanor Pana', lastMessage: 'Oi, como vai?', time: '11:37', unread: 2, type: 'user' },
    { id: 2, name: 'Equipe Dev', lastMessage: 'Nova feature pronta!', time: '10:10', unread: 3, type: 'group' },
    { id: 3, name: 'Cody Bafter', lastMessage: 'Vamos almoçar?', time: '15:20', unread: 0, type: 'user' },
    { id: 4, name: 'Design Team', lastMessage: 'Wireframes aprovados', time: '09:15', unread: 0, type: 'group' }
  ])

  const filteredChats = recentChats.filter(chat =>
    chat.name.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const filteredUsers = onlineUsers.filter(user =>
    user.name.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const filteredGroups = groups.filter(group =>
    group.name.toLowerCase().includes(searchQuery.toLowerCase())
  )

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
                  {user?.charAt(0).toUpperCase()}
                </AvatarFallback>
              </Avatar>
              <div>
                <h3 className="font-medium text-gray-900">{user}</h3>
                <p className="text-sm text-green-600">Online</p>
              </div>
            </div>
            <div className="flex items-center space-x-2">
              <Button variant="ghost" size="sm" onClick={onLogout}>
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
              onClick={() => setSelectedTab('users')}
              className={`flex-1 py-2 px-3 text-sm font-medium rounded-md transition-colors ${
                selectedTab === 'users'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Online
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

        {/* Lista de conteúdo */}
        <div className="flex-1 overflow-y-auto p-4">
          {selectedTab === 'recent' && (
            <div className="space-y-2">
              {filteredChats.map((chat) => (
                <div
                  key={chat.id}
                  onClick={() => onSelectChat(chat)}
                  className="flex items-center space-x-3 p-3 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors"
                >
                  <Avatar className="h-12 w-12">
                    <AvatarFallback className="bg-gray-200">
                      {chat.name.split(' ').map(n => n[0]).join('').toUpperCase()}
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                      <h4 className="font-medium text-gray-900 truncate">{chat.name}</h4>
                      <span className="text-xs text-gray-500">{chat.time}</span>
                    </div>
                    <p className="text-sm text-gray-600 truncate">{chat.lastMessage}</p>
                  </div>
                  {chat.unread > 0 && (
                    <Badge className="bg-blue-600 text-white min-w-[20px] h-5 text-xs">
                      {chat.unread}
                    </Badge>
                  )}
                </div>
              ))}
            </div>
          )}

          {selectedTab === 'users' && (
            <div className="space-y-2">
              <h4 className="text-sm font-medium text-gray-700 mb-3">Usuários Online ({filteredUsers.length})</h4>
              {filteredUsers.map((user) => (
                <div
                  key={user.id}
                  onClick={() => onSelectChat({ ...user, type: 'user' })}
                  className="flex items-center space-x-3 p-3 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors"
                >
                  <div className="relative">
                    <Avatar className="h-10 w-10">
                      <AvatarFallback className="bg-gray-200">
                        {user.avatar}
                      </AvatarFallback>
                    </Avatar>
                    <div className="absolute -bottom-1 -right-1 w-4 h-4 bg-green-500 rounded-full border-2 border-white"></div>
                  </div>
                  <div className="flex-1">
                    <h4 className="font-medium text-gray-900">{user.name}</h4>
                    <p className="text-sm text-green-600">Online</p>
                  </div>
                </div>
              ))}
            </div>
          )}

          {selectedTab === 'groups' && (
            <div className="space-y-2">
              <div className="flex items-center justify-between mb-3">
                <h4 className="text-sm font-medium text-gray-700">Grupos ({filteredGroups.length})</h4>
                <Button size="sm" className="bg-blue-600 hover:bg-blue-700" onClick={() => setShowCreateGroupModal(true)}>
                  <Plus size={16} className="mr-1" />
                  Novo
                </Button>
              </div>
              {filteredGroups.map((group) => (
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
                      {group.unread > 0 && (
                        <Badge className="bg-blue-600 text-white min-w-[20px] h-5 text-xs">
                          {group.unread}
                        </Badge>
                      )}
                    </div>
                    <p className="text-sm text-gray-600 truncate">{group.lastMessage}</p>
                  </div>
                </div>
              ))}
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

