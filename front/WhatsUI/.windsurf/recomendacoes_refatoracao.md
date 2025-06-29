# Recomendações de Refatoração para o WhatsUT

Este documento apresenta recomendações para refatorar o código do WhatsUT seguindo a estrutura de projeto sugerida no arquivo `estrutura.md`. A refatoração pode ser implementada gradualmente sem quebrar a funcionalidade existente.

## Estrutura Atual vs. Estrutura Proposta

### Estrutura Atual
```
/src
  /assets
  /components
  /contexts
  /lib
  /stores
  App.jsx
  main.jsx
  ...
```

### Estrutura Proposta
```
/src
  /assets
  /data
    /contexts
    /hooks
    /services
    /states
  /modules
    /auth
      /components
      /data
        /hooks
        /services
    /chat
      /components
      /data
        /hooks
        /services
    /groups
      /components
      /data
        /hooks
        /services
  /shared
    /components
  App.jsx
  main.jsx
  ...
```

## Plano de Refatoração

### Fase 1: Reorganização dos Arquivos Globais

1. **Criar a estrutura de diretórios**:
   ```
   mkdir -p src/data/contexts src/data/hooks src/data/services src/data/states src/shared/components
   ```

2. **Mover os contextos globais**:
   - Mover `/src/contexts/WebSocketContext.jsx` para `/src/data/contexts/WebSocketContext.jsx`
   - Atualizar importações em todos os arquivos que usam o contexto

3. **Mover os stores globais**:
   - Mover `/src/stores/websocketStore.js` para `/src/data/states/websocketStore.js`
   - Atualizar importações em todos os arquivos que usam o store

4. **Mover os serviços globais**:
   - Mover `/src/lib/websocket.js` para `/src/data/services/websocket.js`
   - Mover `/src/lib/mockResponses.js` para `/src/data/services/mockResponses.js`
   - Atualizar importações em todos os arquivos que usam esses serviços

### Fase 2: Criação dos Módulos

1. **Módulo de Autenticação**:
   ```
   mkdir -p src/modules/auth/components src/modules/auth/data/services src/modules/auth/data/hooks
   ```
   - Mover `/src/components/Login.jsx` para `/src/modules/auth/components/Login.jsx`
   - Criar serviços específicos de autenticação em `/src/modules/auth/data/services/authService.js`

2. **Módulo de Chat**:
   ```
   mkdir -p src/modules/chat/components src/modules/chat/data/services src/modules/chat/data/hooks
   ```
   - Mover `/src/components/Chat.jsx` para `/src/modules/chat/components/Chat.jsx`
   - Criar serviços específicos de chat em `/src/modules/chat/data/services/chatService.js`

3. **Módulo de Grupos**:
   ```
   mkdir -p src/modules/groups/components src/modules/groups/data/services src/modules/groups/data/hooks
   ```
   - Mover `/src/components/CreateGroupModal.jsx` para `/src/modules/groups/components/CreateGroupModal.jsx`
   - Criar serviços específicos de grupos em `/src/modules/groups/data/services/groupService.js`

4. **Componentes Compartilhados**:
   - Mover componentes UI reutilizáveis para `/src/shared/components/`

### Fase 3: Refatoração dos Serviços

1. **Criar um adaptador HTTP base**:
   - Implementar `/src/data/services/httpAdapter.js` que servirá como base para todos os serviços

2. **Refatorar serviços para usar o adaptador HTTP**:
   - Refatorar o serviço WebSocket para estender o adaptador HTTP
   - Implementar serviços específicos para cada módulo que estendam o adaptador HTTP

### Fase 4: Implementação de Hooks Específicos

1. **Criar hooks para cada módulo**:
   - Implementar hooks específicos para autenticação, chat e grupos
   - Mover a lógica dos componentes para esses hooks

## Melhorias de Código Identificadas

Durante a análise do código, identificamos várias oportunidades de melhoria:

1. **Separação de Responsabilidades**:
   - O WebSocketContext está fazendo muitas coisas diferentes
   - Separar em contextos menores e mais específicos

2. **Tratamento de Erros**:
   - Implementar um sistema de tratamento de erros mais robusto
   - Centralizar os erros em um serviço de logging

3. **Tipagem**:
   - Adicionar TypeScript para melhorar a segurança do tipo
   - Definir interfaces para todos os objetos de dados

4. **Testes**:
   - Implementar testes unitários para serviços e hooks
   - Implementar testes de integração para componentes

5. **Documentação**:
   - Adicionar JSDoc para documentar funções e componentes
   - Criar uma documentação mais abrangente para o projeto

## Conclusão

A refatoração proposta melhorará significativamente a organização, manutenibilidade e escalabilidade do código. Recomendamos implementar essas mudanças gradualmente, começando pela reorganização dos arquivos globais e, em seguida, criando os módulos específicos.

Cada fase deve ser testada completamente antes de prosseguir para a próxima, garantindo que a funcionalidade existente não seja quebrada durante o processo de refatoração.
