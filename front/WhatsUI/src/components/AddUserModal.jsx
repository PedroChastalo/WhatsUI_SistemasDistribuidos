import React, { useState, useEffect } from 'react';
import { X, Search, UserPlus, Loader2 } from 'lucide-react';
import { useWebSocket } from '../contexts/WebSocketContext';

// Componentes UI
const Button = ({ children, variant = 'default', className = '', ...props }) => {
  const baseClasses = 'px-4 py-2 rounded-md font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2';
  const variantClasses = {
    default: 'bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500',
    outline: 'border border-gray-300 text-gray-700 hover:bg-gray-50 focus:ring-blue-500',
    ghost: 'text-gray-700 hover:bg-gray-100 focus:ring-blue-500',
    danger: 'bg-red-600 text-white hover:bg-red-700 focus:ring-red-500',
  };
  
  return (
    <button 
      className={`${baseClasses} ${variantClasses[variant]} ${className}`} 
      {...props}
    >
      {children}
    </button>
  );
};

const AddUserModal = ({ isOpen, onClose, groupId }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [users, setUsers] = useState([]);
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [participants, setParticipants] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isAdding, setIsAdding] = useState(false);
  const [feedback, setFeedback] = useState({ type: '', message: '' });
  const { getUsers, getGroupMembers, addUserToGroup, currentUser } = useWebSocket();

  // Carregar usuários e participantes do grupo
  useEffect(() => {
    if (isOpen) {
      // Limpar feedback ao abrir o modal
      setFeedback({ type: '', message: '' });
      setIsLoading(true);
      
      const loadData = async () => {
        try {
          // Carregar todos os usuários
          const allUsers = await getUsers();
          if (allUsers) {
            setUsers(allUsers);
            setFilteredUsers(allUsers);
          }
          
          // Carregar participantes do grupo
          if (groupId) {
            const groupMembers = await getGroupMembers(groupId);
            if (groupMembers) {
              setParticipants(groupMembers);
            }
          }
        } catch (error) {
          console.error("Erro ao carregar dados:", error);
          setFeedback({
            type: 'error',
            message: `Erro ao carregar dados: ${error.message || 'Desconhecido'}`
          });
        } finally {
          setIsLoading(false);
        }
      };
      
      loadData();
    }
  }, [isOpen, groupId, getUsers, getGroupMembers]);

  // Filtrar usuários com base no termo de pesquisa
  useEffect(() => {
    if (users.length > 0) { 
      const filtered = users.filter(user => 
        (user.displayName?.toLowerCase().includes(searchTerm.toLowerCase()) || 
         user.username?.toLowerCase().includes(searchTerm.toLowerCase())) &&
        // Excluir usuários que já são participantes
        !participants.some(p => p.userId === user.userId)
      );
      setFilteredUsers(filtered);
    }
  }, [searchTerm, users, participants]);

  // Adicionar usuário ao grupo
  const handleAddUser = async (userId) => {
    setIsAdding(true);
    setFeedback({ type: '', message: '' });
    
    try {
      await addUserToGroup(groupId, userId);
      
      // Atualizar a lista de participantes
      const updatedMembers = await getGroupMembers(groupId);
      if (updatedMembers) {
        setParticipants(updatedMembers);
      }
      
      // Atualizar a lista de usuários filtrados
      setFilteredUsers(prev => prev.filter(user => user.userId !== userId));
      
      // Mostrar feedback de sucesso
      setFeedback({
        type: 'success',
        message: 'Participante adicionado com sucesso'
      });
      
      // Limpar o feedback após 3 segundos
      setTimeout(() => {
        setFeedback({ type: '', message: '' });
      }, 3000);
      
    } catch (error) {
      console.error("Erro ao adicionar usuário:", error);
      setFeedback({
        type: 'error',
        message: `Erro ao adicionar participante: ${error.message || 'Desconhecido'}`
      });
    } finally {
      setIsAdding(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md">
        <div className="flex items-center justify-between p-4 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900">Adicionar Participantes</h3>
          <button 
            onClick={onClose}
            className="text-gray-400 hover:text-gray-500 focus:outline-none"
          >
            <X size={20} />
          </button>
        </div>
        
        <div className="p-4">
          {/* Feedback de sucesso ou erro */}
          {feedback.message && (
            <div className={`mb-4 p-3 rounded-md ${feedback.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
              {feedback.message}
            </div>
          )}
          
          <div className="relative mb-4">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Search size={18} className="text-gray-400" />
            </div>
            <input
              type="text"
              className="pl-10 pr-4 py-2 border border-gray-300 rounded-md w-full focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="Buscar usuários..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              disabled={isLoading}
            />
          </div>
          
          <div className="max-h-60 overflow-y-auto">
            {isLoading ? (
              <div className="flex justify-center items-center py-8">
                <Loader2 size={24} className="animate-spin text-blue-600" />
                <span className="ml-2 text-gray-600">Carregando...</span>
              </div>
            ) : filteredUsers.length > 0 ? (
              filteredUsers.map(user => (
                <div 
                  key={user.userId}
                  className="flex items-center justify-between p-2 hover:bg-gray-50 rounded-md"
                >
                  <div className="flex items-center">
                    <div className="w-8 h-8 bg-gray-200 rounded-full flex items-center justify-center mr-3">
                      {(user.displayName || user.username || '?').charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">{user.displayName || user.username}</p>
                      <p className="text-sm text-gray-500">@{user.username}</p>
                    </div>
                  </div>
                  <Button 
                    variant="ghost"
                    className="p-2"
                    onClick={() => handleAddUser(user.userId)}
                    disabled={isAdding}
                  >
                    {isAdding ? (
                      <div className="animate-spin h-4 w-4 border-2 border-blue-500 rounded-full border-t-transparent"></div>
                    ) : (
                      <UserPlus size={18} className="text-blue-600" />
                    )}
                  </Button>
                </div>
              ))
            ) : (
              <p className="text-center text-gray-500 py-4">
                {searchTerm ? "Nenhum usuário encontrado" : "Nenhum usuário disponível"}
              </p>
            )}
          </div>
        </div>
        
        <div className="p-4 border-t border-gray-200 flex justify-end">
          <Button variant="outline" onClick={onClose}>
            Fechar
          </Button>
        </div>
      </div>
    </div>
  );
};

export default AddUserModal;
