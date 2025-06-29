// Mock responses para simular o backend
// Este arquivo contém respostas simuladas para as requisições WebSocket

// Dados mockados
const mockUsers = [
  { userId: 'u1', username: 'joao', displayName: 'João Silva', status: 'online', isAdmin: true },
  { userId: 'u2', username: 'maria', displayName: 'Maria Souza', status: 'busy', isAdmin: false },
  { userId: 'u3', username: 'carlos', displayName: 'Carlos Pereira', status: 'offline', isAdmin: false },
  { userId: 'u4', username: 'ana', displayName: 'Ana Oliveira', status: 'online', isAdmin: false },
  { userId: 'u5', username: 'pedro', displayName: 'Pedro', status: 'online', isAdmin: false, email: 'Teste@email.com' }
];

const mockGroups = [
  { 
    groupId: 'g1', 
    name: 'Equipe Dev', 
    description: 'Grupo da equipe de desenvolvimento',
    adminId: 'u1',
    adminName: 'João Silva',
    memberCount: 3,
    lastMessage: 'Nova feature pronta!',
    timestamp: Date.now() - 3600000,
    members: ['u1', 'u2', 'u4']
  },
  { 
    groupId: 'g2', 
    name: 'Projeto Alpha', 
    description: 'Discussões sobre o Projeto Alpha',
    adminId: 'u2',
    adminName: 'Maria Souza',
    memberCount: 4,
    lastMessage: 'Reunião amanhã às 14h',
    timestamp: Date.now() - 7200000,
    members: ['u1', 'u2', 'u3', 'u4']
  }
];

const mockPrivateMessages = {
  'u1_u2': [
    { messageId: 'pm1', senderId: 'u1', content: 'Olá Maria, tudo bem?', timestamp: Date.now() - 86400000, isFile: false },
    { messageId: 'pm2', senderId: 'u2', content: 'Oi João! Tudo ótimo e você?', timestamp: Date.now() - 86300000, isFile: false },
    { messageId: 'pm3', senderId: 'u1', content: 'Estou bem! Precisamos conversar sobre o projeto.', timestamp: Date.now() - 86200000, isFile: false }
  ],
  'u1_u3': [
    { messageId: 'pm4', senderId: 'u3', content: 'João, você viu o email que enviei?', timestamp: Date.now() - 172800000, isFile: false },
    { messageId: 'pm5', senderId: 'u1', content: 'Vi sim, vou responder hoje!', timestamp: Date.now() - 172700000, isFile: false }
  ]
};

const mockGroupMessages = {
  'g1': [
    { messageId: 'gm1', senderId: 'u2', senderName: 'Maria Souza', content: 'Pessoal, terminei a implementação', timestamp: Date.now() - 7200000, isFile: false },
    { messageId: 'gm2', senderId: 'u4', senderName: 'Ana Oliveira', content: 'Ótimo! Vou revisar', timestamp: Date.now() - 7100000, isFile: false },
    { messageId: 'gm3', senderId: 'u1', senderName: 'João Silva', content: 'Nova feature pronta!', timestamp: Date.now() - 3600000, isFile: false }
  ],
  'g2': [
    { messageId: 'gm4', senderId: 'u2', senderName: 'Maria Souza', content: 'Reunião amanhã às 14h', timestamp: Date.now() - 7200000, isFile: false },
    { messageId: 'gm5', senderId: 'u3', senderName: 'Carlos Pereira', content: 'Estarei presente', timestamp: Date.now() - 7000000, isFile: false }
  ]
};

// Funções auxiliares
const getConversationKey = (user1Id, user2Id) => {
  return [user1Id, user2Id].sort().join('_');
};

const generateSessionId = () => {
  return 'session_' + Math.random().toString(36).substring(2, 15);
};

// Respostas mockadas para cada tipo de ação
const mockResponses = {
  // Autenticação
  login: (data) => {
    const { email, password } = data;
    
    // Validação simples
    if (!email || !password) {
      return {
        success: false,
        error: 'Email e senha são obrigatórios'
      };
    }
    
    // Encontrar usuário pelo email (simulando)
    const user = mockUsers.find(u => u.username === email || email.includes(u.username) || (u.email && u.email === email));
    
    if (!user) {
      return {
        success: false,
        error: 'Usuário não encontrado'
      };
    }
    
    // Simular verificação de senha (em produção, usaria bcrypt)
    // Aceitar qualquer senha para facilitar o teste
    if (password === '') {
      return {
        success: false,
        error: 'Senha não pode ser vazia'
      };
    }
    
    const sessionId = generateSessionId();
    
    return {
      success: true,
      data: {
        sessionId,
        user: {
          userId: user.userId,
          username: user.username,
          displayName: user.displayName,
          isAdmin: user.isAdmin
        }
      },
      events: [
        {
          type: 'userStatusChanged',
          data: {
            userId: user.userId,
            username: user.username,
            status: 'online'
          },
          delay: 500
        }
      ]
    };
  },
  
  register: (data) => {
    const { username, email, password, displayName } = data;
    
    // Validação simples
    if (!username || !email || !password || !displayName) {
      return {
        success: false,
        error: 'Todos os campos são obrigatórios'
      };
    }
    
    // Verificar se usuário já existe
    if (mockUsers.some(u => u.username === username || u.username === email)) {
      return {
        success: false,
        error: 'Usuário já existe'
      };
    }
    
    const newUserId = 'u' + (mockUsers.length + 1);
    
    // Em um cenário real, armazenaria no banco de dados
    mockUsers.push({
      userId: newUserId,
      username,
      displayName,
      status: 'online',
      isAdmin: false
    });
    
    return {
      success: true,
      data: {
        userId: newUserId,
        username,
        displayName
      }
    };
  },
  
  logout: (data) => {
    const { sessionId } = data;
    
    if (!sessionId) {
      return {
        success: false,
        error: 'Sessão inválida'
      };
    }
    
    // Simular logout (em produção, invalidaria a sessão no banco)
    return {
      success: true,
      data: {},
      events: [
        {
          type: 'userStatusChanged',
          data: {
            userId: 'u1', // Assumindo que é o usuário atual
            username: 'joao',
            status: 'offline'
          },
          delay: 500
        }
      ]
    };
  },
  
  // Usuários
  getUsers: (data) => {
    const { sessionId } = data;
    
    if (!sessionId) {
      return {
        success: false,
        error: 'Sessão inválida'
      };
    }
    
    return {
      success: true,
      data: {
        users: mockUsers.map(user => ({
          userId: user.userId,
          username: user.username,
          displayName: user.displayName,
          status: user.status,
          isAdmin: user.isAdmin
        }))
      }
    };
  },
  
  updateStatus: (data) => {
    const { sessionId, status } = data;
    
    if (!sessionId || !status) {
      return {
        success: false,
        error: 'Parâmetros inválidos'
      };
    }
    
    if (!['online', 'busy', 'offline'].includes(status)) {
      return {
        success: false,
        error: 'Status inválido'
      };
    }
    
    // Simular atualização de status
    return {
      success: true,
      data: {},
      events: [
        {
          type: 'userStatusChanged',
          data: {
            userId: 'u1', // Assumindo que é o usuário atual
            username: 'joao',
            status
          },
          delay: 200
        }
      ]
    };
  },
  
  // Mensagens Privadas
  getPrivateConversations: (data) => {
    const { sessionId } = data;
    
    if (!sessionId) {
      return {
        success: false,
        error: 'Sessão inválida'
      };
    }
    
    // Simular lista de conversas
    const conversations = [];
    
    // Para cada usuário, verificar se há mensagens
    mockUsers.forEach(user => {
      if (user.userId === 'u1') return; // Pular o próprio usuário
      
      const conversationKey = getConversationKey('u1', user.userId);
      const messages = mockPrivateMessages[conversationKey] || [];
      
      if (messages.length > 0) {
        const lastMessage = messages[messages.length - 1];
        conversations.push({
          userId: user.userId,
          username: user.username,
          displayName: user.displayName,
          lastMessage: lastMessage.content,
          timestamp: lastMessage.timestamp,
          unreadCount: Math.floor(Math.random() * 3) // Simulando contagem não lida
        });
      }
    });
    
    return {
      success: true,
      data: {
        conversations
      }
    };
  },
  
  getPrivateMessages: (data) => {
    const { sessionId, otherUserId, limit = 50, before } = data;
    
    if (!sessionId || !otherUserId) {
      return {
        success: false,
        error: 'Parâmetros inválidos'
      };
    }
    
    const conversationKey = getConversationKey('u1', otherUserId);
    let messages = mockPrivateMessages[conversationKey] || [];
    
    // Filtrar por timestamp se 'before' for fornecido
    if (before) {
      messages = messages.filter(msg => msg.timestamp < before);
    }
    
    // Limitar número de mensagens
    messages = messages.slice(-limit);
    
    // Adicionar nomes dos remetentes
    messages = messages.map(msg => {
      const sender = mockUsers.find(u => u.userId === msg.senderId);
      return {
        ...msg,
        senderName: sender ? sender.displayName : 'Usuário Desconhecido'
      };
    });
    
    return {
      success: true,
      data: {
        messages
      }
    };
  },
  
  sendPrivateMessage: (data) => {
    const { sessionId, receiverId, content } = data;
    
    if (!sessionId || !receiverId || !content) {
      return {
        success: false,
        error: 'Parâmetros inválidos'
      };
    }
    
    const messageId = 'pm' + Date.now();
    const timestamp = Date.now();
    
    // Adicionar mensagem ao histórico (em produção, seria no banco)
    const conversationKey = getConversationKey('u1', receiverId);
    if (!mockPrivateMessages[conversationKey]) {
      mockPrivateMessages[conversationKey] = [];
    }
    
    mockPrivateMessages[conversationKey].push({
      messageId,
      senderId: 'u1',
      content,
      timestamp,
      isFile: false
    });
    
    return {
      success: true,
      data: {
        messageId,
        timestamp
      },
      events: [
        {
          type: 'newPrivateMessage',
          data: {
            messageId,
            senderId: 'u1',
            senderName: 'João Silva',
            receiverId,
            content,
            timestamp,
            isFile: false
          },
          delay: 300
        }
      ]
    };
  },
  
  // Grupos
  getGroups: (data) => {
    const { sessionId } = data;
    
    if (!sessionId) {
      return {
        success: false,
        error: 'Sessão inválida'
      };
    }
    
    // Mapear grupos com informação de associação
    const groups = mockGroups.map(group => ({
      groupId: group.groupId,
      name: group.name,
      description: group.description,
      adminId: group.adminId,
      adminName: group.adminName,
      memberCount: group.memberCount,
      lastMessage: group.lastMessage,
      timestamp: group.timestamp,
      isMember: group.members.includes('u1')
    }));
    
    return {
      success: true,
      data: {
        groups
      }
    };
  },
  
  createGroup: (data) => {
    const { sessionId, name, description, deleteOnAdminExit, initialMembers } = data;
    
    if (!sessionId || !name) {
      return {
        success: false,
        error: 'Parâmetros inválidos'
      };
    }
    
    const groupId = 'g' + (mockGroups.length + 1);
    
    // Criar novo grupo
    const newGroup = {
      groupId,
      name,
      description: description || '',
      adminId: 'u1',
      adminName: 'João Silva',
      memberCount: (initialMembers?.length || 0) + 1,
      lastMessage: null,
      timestamp: Date.now(),
      members: ['u1', ...(initialMembers || [])]
    };
    
    mockGroups.push(newGroup);
    
    return {
      success: true,
      data: {
        groupId,
        name
      },
      events: initialMembers?.map(userId => ({
        type: 'groupInvitation',
        data: {
          groupId,
          groupName: name,
          adminId: 'u1',
          adminName: 'João Silva'
        },
        delay: 500
      })) || []
    };
  },
  
  getGroupMessages: (data) => {
    const { sessionId, groupId, limit = 50, before } = data;
    
    if (!sessionId || !groupId) {
      return {
        success: false,
        error: 'Parâmetros inválidos'
      };
    }
    
    let messages = mockGroupMessages[groupId] || [];
    
    // Filtrar por timestamp se 'before' for fornecido
    if (before) {
      messages = messages.filter(msg => msg.timestamp < before);
    }
    
    // Limitar número de mensagens
    messages = messages.slice(-limit);
    
    return {
      success: true,
      data: {
        messages
      }
    };
  },
  
  getGroupMembers: (data) => {
    const { sessionId, groupId } = data;
    
    if (!sessionId || !groupId) {
      return {
        success: false,
        error: 'Parâmetros inválidos'
      };
    }
    
    // Encontrar o grupo
    const group = mockGroups.find(g => g.groupId === groupId);
    
    if (!group) {
      return {
        success: false,
        error: 'Grupo não encontrado'
      };
    }
    
    // Mapear IDs de membros para objetos de usuário
    const members = group.members.map(userId => {
      const user = mockUsers.find(u => u.userId === userId);
      return {
        userId: user.userId,
        username: user.username,
        displayName: user.displayName,
        status: user.status,
        role: user.userId === group.adminId ? 'admin' : 'member'
      };
    });
    
    return {
      success: true,
      data: {
        members
      }
    };
  },
  
  sendGroupMessage: (data) => {
    const { sessionId, groupId, content } = data;
    
    if (!sessionId || !groupId || !content) {
      return {
        success: false,
        error: 'Parâmetros inválidos'
      };
    }
    
    const messageId = 'gm' + Date.now();
    const timestamp = Date.now();
    
    // Adicionar mensagem ao histórico do grupo
    if (!mockGroupMessages[groupId]) {
      mockGroupMessages[groupId] = [];
    }
    
    mockGroupMessages[groupId].push({
      messageId,
      senderId: 'u1',
      senderName: 'João Silva',
      content,
      timestamp,
      isFile: false
    });
    
    // Atualizar última mensagem do grupo
    const group = mockGroups.find(g => g.groupId === groupId);
    if (group) {
      group.lastMessage = content;
      group.timestamp = timestamp;
    }
    
    return {
      success: true,
      data: {
        messageId,
        timestamp
      },
      events: [
        {
          type: 'newGroupMessage',
          data: {
            messageId,
            groupId,
            senderId: 'u1',
            senderName: 'João Silva',
            content,
            timestamp,
            isFile: false
          },
          delay: 300
        }
      ]
    };
  },
  
  getGroupMembers: (data) => {
    const { sessionId, groupId } = data;
    
    if (!sessionId || !groupId) {
      return {
        success: false,
        error: 'Parâmetros inválidos'
      };
    }
    
    const group = mockGroups.find(g => g.groupId === groupId);
    
    if (!group) {
      return {
        success: false,
        error: 'Grupo não encontrado'
      };
    }
    
    // Mapear membros com detalhes
    const members = group.members.map(userId => {
      const user = mockUsers.find(u => u.userId === userId);
      return {
        userId: user.userId,
        username: user.username,
        displayName: user.displayName,
        isAdmin: userId === group.adminId,
        status: user.status
      };
    });
    
    return {
      success: true,
      data: {
        members
      }
    };
  }
};

export default mockResponses;
