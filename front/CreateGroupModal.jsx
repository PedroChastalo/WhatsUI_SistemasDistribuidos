import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Checkbox } from '@/components/ui/checkbox'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { X, Search, Upload, Users } from 'lucide-react'

export default function CreateGroupModal({ isOpen, onClose, onCreateGroup }) {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    avatar: null,
    selectedUsers: [],
    adminPermissions: 'only-admins',
    editPermissions: 'only-admins',
    privacy: 'public'
  })

  const [searchQuery, setSearchQuery] = useState('')

  // Usuários disponíveis (simulado)
  const availableUsers = [
    { id: 1, name: 'Eleanor Pana', avatar: 'EP', online: true },
    { id: 2, name: 'Darlene Robertson', avatar: 'DR', online: true },
    { id: 3, name: 'Cody Bafter', avatar: 'CB', online: false },
    { id: 4, name: 'Savannah Nguyen', avatar: 'SN', online: true },
    { id: 5, name: 'Kathryn Murphy', avatar: 'KM', online: false },
    { id: 6, name: 'Ralph Edwards', avatar: 'RE', online: true }
  ]

  const filteredUsers = availableUsers.filter(user =>
    user.name.toLowerCase().includes(searchQuery.toLowerCase())
  )

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

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!formData.name.trim()) return

    const newGroup = {
      id: Date.now(),
      name: formData.name,
      description: formData.description,
      participants: formData.selectedUsers.length + 1, // +1 para o criador
      type: 'group',
      unread: 0,
      lastMessage: 'Grupo criado'
    }

    onCreateGroup(newGroup)
    onClose()
    
    // Reset form
    setFormData({
      name: '',
      description: '',
      avatar: null,
      selectedUsers: [],
      adminPermissions: 'only-admins',
      editPermissions: 'only-admins',
      privacy: 'public'
    })
  }

  const handleClose = () => {
    onClose()
    setSearchQuery('')
  }

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="max-w-md max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center justify-between">
            Criar Grupo
            <Button variant="ghost" size="sm" onClick={handleClose}>
              <X size={20} />
            </Button>
          </DialogTitle>
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

          {/* Avatar do grupo */}
          <div className="space-y-2">
            <Label>Avatar do grupo</Label>
            <div className="flex items-center space-x-4">
              <div className="w-16 h-16 border-2 border-dashed border-gray-300 rounded-lg flex items-center justify-center">
                {formData.avatar ? (
                  <img src={formData.avatar} alt="Avatar" className="w-full h-full object-cover rounded-lg" />
                ) : (
                  <Upload size={24} className="text-gray-400" />
                )}
              </div>
              <Button type="button" variant="outline" size="sm">
                Escolher imagem
              </Button>
            </div>
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
              {filteredUsers.map((user) => (
                <div key={user.id} className="flex items-center space-x-3 p-2 hover:bg-gray-50 rounded">
                  <Checkbox
                    checked={formData.selectedUsers.includes(user.id)}
                    onCheckedChange={() => handleUserToggle(user.id)}
                  />
                  <div className="relative">
                    <Avatar className="h-8 w-8">
                      <AvatarFallback className="bg-gray-200 text-sm">
                        {user.avatar}
                      </AvatarFallback>
                    </Avatar>
                    {user.online && (
                      <div className="absolute -bottom-1 -right-1 w-3 h-3 bg-green-500 rounded-full border border-white"></div>
                    )}
                  </div>
                  <div className="flex-1">
                    <p className="text-sm font-medium text-gray-900">{user.name}</p>
                    <p className="text-xs text-gray-500">{user.online ? 'Online' : 'Offline'}</p>
                  </div>
                </div>
              ))}
            </div>
            
            <p className="text-sm text-gray-600">
              {formData.selectedUsers.length} usuário(s) selecionado(s)
            </p>
          </div>

          {/* Permissões de admin */}
          <div className="space-y-3">
            <Label>Quem pode adicionar membros</Label>
            <RadioGroup 
              value={formData.adminPermissions} 
              onValueChange={(value) => handleInputChange('adminPermissions', value)}
            >
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="only-admins" id="admin-only" />
                <Label htmlFor="admin-only" className="text-sm">Apenas administradores</Label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="all-participants" id="admin-all" />
                <Label htmlFor="admin-all" className="text-sm">Todos os participantes</Label>
              </div>
            </RadioGroup>
          </div>

          {/* Permissões de edição */}
          <div className="space-y-3">
            <Label>Quem pode editar informações do grupo</Label>
            <RadioGroup 
              value={formData.editPermissions} 
              onValueChange={(value) => handleInputChange('editPermissions', value)}
            >
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="only-admins" id="edit-only" />
                <Label htmlFor="edit-only" className="text-sm">Apenas administradores</Label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="all-participants" id="edit-all" />
                <Label htmlFor="edit-all" className="text-sm">Todos os participantes</Label>
              </div>
            </RadioGroup>
          </div>

          {/* Privacidade */}
          <div className="space-y-3">
            <Label>Privacidade</Label>
            <RadioGroup 
              value={formData.privacy} 
              onValueChange={(value) => handleInputChange('privacy', value)}
            >
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="public" id="public" />
                <Label htmlFor="public" className="text-sm">Grupo público</Label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="private" id="private" />
                <Label htmlFor="private" className="text-sm">Grupo privado</Label>
              </div>
            </RadioGroup>
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

