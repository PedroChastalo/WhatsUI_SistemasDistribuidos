import { useState, useEffect } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Checkbox } from '@/components/ui/checkbox'
import { Switch } from '@/components/ui/switch'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { X, Search, Upload, Users, User } from 'lucide-react'
import { useWebSocket } from '@/contexts/WebSocketContext'

export default function CreateGroupModal({ isOpen, onClose, onCreateGroup }) {
  const { users, userStatus, currentUser, getUsers } = useWebSocket()
  
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    selectedUsers: [],
    deleteIfAdminLeaves: false
  })

  const [searchQuery, setSearchQuery] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  
  // Carregar usuários ao abrir o modal
  useEffect(() => {
    if (isOpen) {
      const loadUsers = async () => {
        setIsLoading(true)
        try {
          await getUsers()
        } catch (error) {
          console.error('Erro ao carregar usuários:', error)
        } finally {
          setIsLoading(false)
        }
      }
      
      loadUsers()
    }
  }, [isOpen, getUsers])
  
  // Filtrar usuários disponíveis (excluindo o usuário atual)
  const availableUsers = users.filter(user => 
    user.userId !== currentUser?.userId
  )
  
  // Aplicar filtro de busca
  const filteredUsers = availableUsers.filter(user => {
    const displayName = user.displayName || user.username || ''
    return displayName.toLowerCase().includes(searchQuery.toLowerCase())
  })

  const handleInputChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }))
  }

  const handleUserToggle = (userId) => {
    setFormData(prev => ({
      ...prev,
      selectedUsers: prev.selectedUsers.includes(userId)
        ? prev.selectedUsers.filter(id => id !== userId)
        : [...prev.selectedUsers, userId]
    }))
  }
  
  const handleDeleteIfAdminLeavesChange = (checked) => {
    setFormData(prev => ({
      ...prev,
      deleteIfAdminLeaves: checked
    }))
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!formData.name.trim()) return

    const newGroup = {
      name: formData.name,
      description: formData.description,
      members: formData.selectedUsers,
      deleteIfAdminLeaves: formData.deleteIfAdminLeaves
    }

    onCreateGroup(newGroup)
    onClose()
    
    // Reset form
    setFormData({
      name: '',
      description: '',
      selectedUsers: [],
      deleteIfAdminLeaves: false
    })
  }

  const handleClose = () => {
    onClose()
    setSearchQuery('')
    setFormData({
      name: '',
      description: '',
      selectedUsers: [],
      deleteIfAdminLeaves: false
    })
  }

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="max-w-md max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Criar Grupo</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Nome do grupo */}
          <div className="space-y-2">
            <Label htmlFor="groupName">Nome do grupo *</Label>
            <Input
              id="groupName"
              placeholder="Digite o nome do grupo"
              value={formData.name}
              onChange={(e) => handleInputChange('name', e.target.value)}
              required
            />
          </div>

          {/* Descrição */}
          <div className="space-y-2">
            <Label htmlFor="groupDescription">Descrição</Label>
            <Textarea
              id="groupDescription"
              placeholder="Descrição do grupo (opcional)"
              value={formData.description}
              onChange={(e) => handleInputChange('description', e.target.value)}
              rows={3}
            />
          </div>

          {/* Seleção de participantes */}
          <div className="space-y-3">
            <Label>Participantes</Label>
            
            {/* Busca de usuários */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
              <Input
                placeholder="Buscar usuários"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10"
              />
            </div>

            {/* Lista de usuários */}
            <div className="max-h-40 overflow-y-auto space-y-2 border rounded-lg p-2">
              {isLoading ? (
                <div className="flex justify-center items-center p-4">
                  <p className="text-sm text-gray-500">Carregando usuários...</p>
                </div>
              ) : filteredUsers.length === 0 ? (
                <div className="flex justify-center items-center p-4">
                  <p className="text-sm text-gray-500">Nenhum usuário encontrado</p>
                </div>
              ) : (
                filteredUsers.map((user) => (
                  <div key={user.userId} className="flex items-center space-x-3 p-2 hover:bg-gray-50 rounded">
                    <Checkbox
                      checked={formData.selectedUsers.includes(user.userId)}
                      onCheckedChange={() => handleUserToggle(user.userId)}
                    />
                    <div className="relative">
                      <Avatar className="h-8 w-8">
                        <AvatarFallback className="bg-gray-200 text-sm">
                          {(user.displayName || user.username || '?').charAt(0)}
                        </AvatarFallback>
                      </Avatar>
                      {userStatus[user.userId] === 'online' && (
                        <div className="absolute -bottom-1 -right-1 w-3 h-3 bg-green-500 rounded-full border border-white"></div>
                      )}
                    </div>
                    <div className="flex-1">
                      <p className="text-sm font-medium text-gray-900">{user.displayName || user.username}</p>
                      <p className="text-xs text-gray-500">
                        {userStatus[user.userId] === 'online' ? 'Online' : 'Offline'}
                      </p>
                    </div>
                  </div>
                ))
              )}
            </div>
            
            <p className="text-sm text-gray-600">
              {formData.selectedUsers.length} usuário(s) selecionado(s)
            </p>
          </div>

          {/* Opção de exclusão do grupo se o admin sair */}
          <div className="flex items-center space-x-2">
            <Switch 
              id="delete-if-admin-leaves" 
              checked={formData.deleteIfAdminLeaves}
              onCheckedChange={handleDeleteIfAdminLeavesChange}
            />
            <Label htmlFor="delete-if-admin-leaves" className="text-sm">
              Excluir grupo se o administrador sair
            </Label>
          </div>
          
          {/* Botões */}
          <div className="flex space-x-3 pt-4">
            <Button type="button" variant="outline" onClick={handleClose} className="flex-1">
              Cancelar
            </Button>
            <Button 
              type="submit" 
              className="flex-1 bg-blue-600 hover:bg-blue-700"
              disabled={!formData.name.trim()}
            >
              Criar Grupo
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}

