import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  ArrowLeft,
  Check,
  CheckCheck,
  Crown,
  MoreVertical,
  Paperclip,
  Phone,
  Send,
  Smile,
  UserMinus,
  UserPlus,
  Users,
  Video,
  AlertCircle,
  Loader2,
  LogOut,
  Trash
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useWebSocket } from "@/contexts/WebSocketContext";
import AddUserModal from "./AddUserModal";
import RemoveUserModal from "./RemoveUserModal";

export default function Chat({ chat, onBack }) {
  const [message, setMessage] = useState("");
  const [showParticipants, setShowParticipants] = useState(false);
  const [showAddUserModal, setShowAddUserModal] = useState(false);
  const [showRemoveUserModal, setShowRemoveUserModal] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const messagesEndRef = useRef(null);

  const {
    currentUser,
    users,
    getMessages,
    sendMessage,
    getGroupMembers,
    addUserToGroup,
    removeUserFromGroup,
    setGroupAdmin,
    leaveGroup,
    deleteGroup
  } = useWebSocket();

  // Estado para mensagens e participantes
  const [messages, setMessages] = useState([]);
  const [participants, setParticipants] = useState([]);
  
  // Carregar mensagens e participantes
  useEffect(() => {
    const loadData = async () => {
      setIsLoading(true);
      setError("");
      try {
        // Identificar o ID correto com base no tipo de chat
        const chatId = chat.type === 'user' ? chat.userId : chat.groupId;
        
        // Carregar mensagens
        const chatMessages = await getMessages(chatId, chat.type);
        setMessages(chatMessages || []);
        
        // Carregar participantes se for um grupo
        if (chat.type === "group") {
          const members = await getGroupMembers(chatId);
          setParticipants(members || []);
        }
      } catch (err) {
        setError("Erro ao carregar dados: " + (err.message || "Desconhecido"));
      } finally {
        setIsLoading(false);
      }
    };
    
    loadData();
    // Removidas as dependências que causavam o loop infinito
    // Apenas recarregar quando o chat mudar
  }, [chat.userId, chat.groupId, chat.type]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // Função para enviar mensagem
  const handleSendMessage = async () => {
    if (!message.trim()) return;

    try {
      // Identificar o ID correto com base no tipo de chat
      const chatId = chat.type === 'user' ? chat.userId : chat.groupId;
      
      // Criar objeto de mensagem
      const newMessage = {
        content: message.trim(),
        recipientId: chatId,
        type: chat.type
      };
      
      // Adicionar temporariamente à UI para feedback imediato
      const tempId = `temp-${Date.now()}`;
      const tempMessage = {
        id: tempId,
        sender: currentUser?.displayName || 'Você',
        senderId: currentUser?.userId,
        content: message.trim(),
        timestamp: new Date().toISOString(),
        status: "sending",
        isOwn: true,
      };
      
      // Adicionar mensagem temporária ao estado
      setMessages(prev => [...prev, tempMessage]);
      
      // Limpar campo de mensagem
      setMessage("");
      
      // Enviar mensagem para o servidor
      const response = await sendMessage(newMessage);
      
      // Atualizar status da mensagem temporária
      setMessages(prev => prev.map(msg => 
        msg.id === tempId 
          ? { ...msg, id: response?.id || tempId, status: "sent" } 
          : msg
      ));
      
    } catch (err) {
      // Marcar mensagem como falha
      setMessages(prev => prev.map(msg => 
        msg.status === "sending" 
          ? { ...msg, status: "failed" } 
          : msg
      ));
      
      setError("Erro ao enviar mensagem: " + (err.message || "Desconhecido"));
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  // Funções para gerenciar participantes do grupo
  const handleAddParticipant = async (userId) => {
    try {
      await addUserToGroup(chat.groupId, userId);
      // Recarregar participantes
      const members = await getGroupMembers(chat.groupId);
      setParticipants(members || []);
    } catch (err) {
      setError("Erro ao adicionar participante: " + (err.message || "Desconhecido"));
    }
  };

  const handleRemoveParticipant = async (userId) => {
    try {
      await removeUserFromGroup(chat.groupId, userId);
      // Recarregar participantes
      const members = await getGroupMembers(chat.groupId);
      setParticipants(members || []);
    } catch (err) {
      setError("Erro ao remover participante: " + (err.message || "Desconhecido"));
    }
  };

  const handleSetAdmin = async (userId) => {
    try {
      await setGroupAdmin(chat.groupId, userId);
      // Recarregar participantes
      const members = await getGroupMembers(chat.groupId);
      setParticipants(members || []);
    } catch (err) {
      setError("Erro ao definir administrador: " + (err.message || "Desconhecido"));
    }
  };

  // Verificar se o usuário atual é administrador do grupo
  const isCurrentUserAdmin = () => {
    return participants.some(p => 
      p.userId === currentUser?.userId && p.isAdmin
    );
  };

  const handleLeaveGroup = async (shouldDeleteGroup = false) => {
    try {
      // Se o usuário é admin e escolheu excluir o grupo
      if (shouldDeleteGroup && isCurrentUserAdmin()) {
        await deleteGroup(chat.groupId);
      } else {
        // Caso contrário, apenas sair do grupo
        await leaveGroup(chat.groupId, false);
      }
      onBack(); // Voltar para a lista de chats
    } catch (err) {
      setError("Erro ao sair do grupo: " + (err.message || "Desconhecido"));
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case "sending":
        return <Loader2 size={16} className="text-gray-400 animate-spin" />;
      case "sent":
        return <Check size={16} className="text-gray-400" />;
      case "delivered":
        return <CheckCheck size={16} className="text-gray-400" />;
      case "read":
        return <CheckCheck size={16} className="text-blue-600" />;
      case "failed":
        return <AlertCircle size={16} className="text-red-500" />;
      default:
        return null;
    }
  };

  return (
    <div className="h-screen bg-white flex flex-col">
      {/* Cabeçalho */}
      <div className="p-4 border-b border-gray-200 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <Button variant="ghost" size="sm" onClick={onBack}>
            <ArrowLeft size={20} />
          </Button>
          <div className="flex items-center space-x-3">
            <Avatar className="h-10 w-10">
              <AvatarFallback
                className={
                  chat.type === "group"
                    ? "bg-blue-100 text-blue-600"
                    : "bg-gray-200"
                }
              >
                {chat.type === "group" ? (
                  <Users size={20} />
                ) : (
                  (chat.displayName || chat.username || "?").charAt(0).toUpperCase()
                )}
              </AvatarFallback>
            </Avatar>
            <div>
              <h3 className="font-medium text-gray-900">{chat.type === "group" ? chat.name : (chat.displayName || chat.username || "Usuário")}</h3>
              {chat.type === "group" ? (
                <p className="text-sm text-gray-600">
                  {participants.length} participantes
                </p>
              ) : (
                <p className="text-sm text-green-600">
                  {chat.status === "online" ? "Online" : "Offline"}
                </p>
              )}
            </div>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          {/* Manter apenas o botão de participantes para grupos */}
          {chat.type === "group" && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setShowParticipants(!showParticipants)}
            >
              <Users size={20} />
            </Button>
          )}
          {/* Removidos os ícones de ligar, vídeo e menu conforme solicitado */}
        </div>
      </div>

      <div className="flex-1 flex overflow-hidden">
        <div className="flex-1 flex flex-col">
          {/* Área de mensagens */}
          <div className="flex-1 overflow-y-auto p-4">
            {isLoading ? (
              <div className="flex items-center justify-center h-full">
                <div className="text-center">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
                  <p className="text-gray-600">Carregando mensagens...</p>
                </div>
              </div>
            ) : error ? (
              <div className="flex items-center justify-center h-full">
                <div className="text-center">
                  <AlertCircle className="mx-auto text-red-500 mb-2" size={32} />
                  <p className="text-red-500">{error}</p>
                </div>
              </div>
            ) : messages.length === 0 ? (
              <div className="flex justify-center items-center h-full">
                <p className="text-gray-500">Nenhuma mensagem ainda. Diga olá!</p>
              </div>
            ) : (
              <div className="space-y-4">
                {messages.map((msg, index) => {
                  // Determinar se a mensagem é do usuário atual
                  // Garantir que a propriedade isOwn seja respeitada e não mude após a renderização inicial
                  const isOwn = msg.isOwn === true || msg.senderId === currentUser?.userId;
                  
                  return (
                    <div
                      key={msg.id || index}
                      className={`flex ${isOwn ? "justify-end" : "justify-start"}`}
                    >
                      {!isOwn && chat.type === 'group' && (
                        <div className="flex flex-col items-start mr-2">
                          <Avatar className="h-8 w-8">
                            <AvatarFallback className="bg-gray-200 text-xs">
                              {(msg.senderName || msg.sender || '?').charAt(0).toUpperCase()}
                            </AvatarFallback>
                          </Avatar>
                        </div>
                      )}
                      <div
                        className={`max-w-[80%] rounded-lg p-3 ${isOwn
                          ? "bg-blue-600 text-white"
                          : "bg-gray-100 text-gray-900"
                        }`}
                      >
                        {!isOwn && chat.type === 'group' && (
                          <p className="text-xs font-medium mb-1 text-gray-600">{msg.senderName || msg.sender || 'Usuário'}</p>
                        )}
                        <p>{msg.content}</p>
                        <div
                          className={`text-xs mt-1 flex items-center ${isOwn ? "text-blue-100" : "text-gray-500"}`}
                        >
                          <span>{new Date(msg.timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
                          {isOwn && (
                            <span className="ml-1">
                              {msg.status === "sending" && <Loader2 size={12} className="animate-spin" />}
                              {msg.status === "sent" && <Check size={12} />}
                              {msg.status === "delivered" && <Check size={12} />}
                              {msg.status === "read" && <CheckCheck size={12} />}
                              {msg.status === "failed" && <AlertCircle size={12} className="text-red-500" />}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
                <div ref={messagesEndRef} />
              </div>
            )}
          </div>

          {/* Input de mensagem */}
          <div className="p-4 border-t border-gray-200">
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
                <Button
                  variant="ghost"
                  size="sm"
                  className="absolute right-2 top-1/2 -translate-y-1/2"
                >
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
        {chat.type === "group" && showParticipants && (
          <div className="w-80 border-l border-gray-200 bg-gray-50 flex flex-col">
            <div className="p-4 border-b border-gray-200 bg-white">
              <h3 className="font-medium text-gray-900">Participantes</h3>
              <p className="text-sm text-gray-600">
                {participants.length} membros
              </p>
            </div>

            <div className="flex-1 overflow-y-auto p-4">
              <div className="space-y-2">
                {participants.map((participant) => (
                  <div
                    key={participant.id}
                    className="flex items-center justify-between p-3 bg-white rounded-lg"
                  >
                    <div className="flex items-center space-x-3">
                      <div className="relative">
                        <Avatar className="h-10 w-10">
                          <AvatarFallback className="bg-gray-200">
                            {(participant.displayName || participant.username || '?').charAt(0).toUpperCase()}
                          </AvatarFallback>
                        </Avatar>
                        {participant.status === 'online' && (
                          <div className="absolute -bottom-1 -right-1 w-4 h-4 bg-green-500 rounded-full border-2 border-white"></div>
                        )}
                      </div>
                      <div>
                        <h4 className="font-medium text-gray-900">
                          {participant.displayName || participant.username || 'Usuário'}
                        </h4>
                        <p className="text-sm text-gray-600">
                          {participant.status === 'online' ? "Online" : "Offline"}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center space-x-2">
                      {participant.role === 'admin' && (
                        <Crown size={16} className="text-yellow-500" />
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Controles de admin */}
            <div className="p-4 border-t border-gray-200 bg-white space-y-2">
              {isCurrentUserAdmin() && (
                <>
                  <Button 
                    variant="outline" 
                    className="w-full justify-start"
                    onClick={() => setShowAddUserModal(true)}
                  >
                    <UserPlus size={16} className="mr-2" />
                    Adicionar participante
                  </Button>
                  <Button
                    variant="outline"
                    className="w-full justify-start text-red-600 hover:text-red-700"
                    onClick={() => setShowRemoveUserModal(true)}
                  >
                    <UserMinus size={16} className="mr-2" />
                    Remover participante
                  </Button>
                  <Button 
                    variant="outline" 
                    className="w-full justify-start"
                    onClick={() => {
                      // Mostrar lista de participantes e permitir selecionar um para tornar admin
                      const participantsList = participants.map(p => `${p.displayName || p.username} (${p.userId})`);
                      const message = `Selecione um participante para tornar admin:\n${participantsList.join('\n')}\n\nDigite o ID do usuário (entre parênteses):`;                      
                      const userId = prompt(message);
                      if (userId) handleSetAdmin(userId);
                    }}
                  >
                    <Crown size={16} className="mr-2" />
                    Definir como admin
                  </Button>
                </>
              )}
              
              {/* Botões para todos os participantes */}
              <Button 
                variant="outline" 
                className="w-full justify-start"
                onClick={() => handleLeaveGroup(false)}
              >
                <ArrowLeft size={16} className="mr-2" />
                Sair do grupo
              </Button>
              
              {/* Botão de excluir grupo (apenas para admin) */}
              {isCurrentUserAdmin() && (
                <Button 
                  variant="outline" 
                  className="w-full justify-start text-red-600 hover:text-red-700"
                  onClick={() => {
                    if (confirm("Tem certeza que deseja excluir este grupo? Esta ação não pode ser desfeita.")) {
                      handleLeaveGroup(true);
                    }
                  }}
                >
                  <AlertCircle size={16} className="mr-2" />
                  Excluir grupo
                </Button>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Modais para gerenciamento de grupo */}
      {chat.type === "group" && (
        <>
          <AddUserModal 
            isOpen={showAddUserModal} 
            onClose={() => setShowAddUserModal(false)} 
            groupId={chat.groupId} 
          />
          <RemoveUserModal 
            isOpen={showRemoveUserModal} 
            onClose={() => setShowRemoveUserModal(false)} 
            groupId={chat.groupId} 
          />
        </>
      )}
    </div>
  );
}
