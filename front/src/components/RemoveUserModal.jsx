import React, { useState, useEffect } from 'react';
import { X, Search, UserMinus, Crown } from 'lucide-react';
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

const RemoveUserModal = ({ isOpen, onClose, groupId }) => {
  // Flag se usuário atual é admin
  const [currentIsAdmin, setCurrentIsAdmin] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [participants, setParticipants] = useState([]);
  const [filteredParticipants, setFilteredParticipants] = useState([]);
  const [isRemoving, setIsRemoving] = useState(false);
  const [feedback, setFeedback] = useState({ type: '', message: '' });
  const { getGroupMembers, removeUserFromGroup, currentUser } = useWebSocket();

  // Carregar participantes do grupo
  useEffect(() => {
    if (isOpen && groupId) {
      // Limpar feedback ao abrir o modal
      setFeedback({ type: '', message: '' });
      
      const loadParticipants = async () => {
        try {
          const members = await getGroupMembers(groupId);
          if (members) {
                        setParticipants(members);
                        setFilteredParticipants(members);
            // Atualizar flag de admin atual
            const isAdminFlag = members.some(m => m.userId === currentUser?.userId && m.isAdmin);
            setCurrentIsAdmin(isAdminFlag);
          }
        } catch (error) {
          console.error("Erro ao carregar participantes:", error);
          setFeedback({ type: 'error', message: 'Erro ao carregar participantes' });
        }
      };
      
      loadParticipants();
    }
  }, [isOpen, groupId, getGroupMembers, currentUser?.userId]);

  // Filtrar participantes com base no termo de pesquisa
  useEffect(() => {
    if (participants.length > 0) {
      const filtered = participants.filter(user => 
        user.displayName?.toLowerCase().includes(searchTerm.toLowerCase()) || 
        user.username?.toLowerCase().includes(searchTerm.toLowerCase())
      );
      setFilteredParticipants(filtered);
    }
  }, [searchTerm, participants]);

  // Remover usuário do grupo
  const handleRemoveUser = async (userId) => {
    setIsRemoving(true);
    setFeedback({ type: '', message: '' });
    
    try {
      await removeUserFromGroup(groupId, userId);
      
      // Atualizar a lista de participantes
      setParticipants(prev => prev.filter(p => p.userId !== userId));
      setFilteredParticipants(prev => prev.filter(p => p.userId !== userId));
      
      // Mostrar feedback de sucesso
      setFeedback({ 
        type: 'success', 
        message: 'Participante removido com sucesso' 
      });
      
      // Limpar o feedback após 3 segundos
      setTimeout(() => {
        setFeedback({ type: '', message: '' });
      }, 3000);
      
    } catch (error) {
      console.error("Erro ao remover usuário:", error);
      setFeedback({ 
        type: 'error', 
        message: `Erro ao remover participante: ${error.message || 'Desconhecido'}` 
      });
    } finally {
      setIsRemoving(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md">
        <div className="flex items-center justify-between p-4 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900">Remover Participantes</h3>
          <button 
            onClick={onClose}
            className="text-gray-400 hover:text-gray-500 focus:outline-none"
          >
            <X size={20} />
          </button>
        </div>
        
        <div className="p-4">
          <div className="relative mb-4">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Search size={18} className="text-gray-400" />
            </div>
            <input
              type="text"
              className="pl-10 pr-4 py-2 border border-gray-300 rounded-md w-full focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="Buscar participantes..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          
          {/* Feedback de sucesso ou erro */}
          {feedback.message && (
            <div className={`mb-4 p-3 rounded-md ${feedback.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
              {feedback.message}
            </div>
          )}
          
          <div className="max-h-60 overflow-y-auto">
            {filteredParticipants.length > 0 ? (
              filteredParticipants.map(user => (
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
                      {user.isAdmin && (
                        <Crown size={16} className="text-yellow-500 ml-1" title="Admin" />
                      )}
                    </div>
                  </div>
                  {currentIsAdmin && user.userId !== currentUser?.userId && (
                    <Button 
                      variant="ghost"
                      className="p-2 text-red-500 hover:text-red-700"
                      onClick={() => handleRemoveUser(user.userId)}
                      disabled={isRemoving}
                    >
                    {isRemoving ? (
                      <div className="animate-spin h-4 w-4 border-2 border-red-500 rounded-full border-t-transparent"></div>
                    ) : (
                      <UserMinus size={18} />
                    )}
                    </Button>
                  )}
                </div>
              ))
            ) : (
              <p className="text-center text-gray-500 py-4">
                {searchTerm ? "Nenhum participante encontrado" : "Carregando participantes..."}
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

export default RemoveUserModal;
