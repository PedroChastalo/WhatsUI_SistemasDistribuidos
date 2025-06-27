# Fluxos de Usuário - WhatsUT

## 1. Fluxo de Login
**Início:** Usuário acessa a aplicação
1. Usuário visualiza tela de login
2. Usuário insere username/email e senha
3. Sistema valida credenciais
4. **Se válido:** Redireciona para dashboard
5. **Se inválido:** Exibe mensagem de erro com feedback visual
6. **Opção:** "Esqueceu a senha?" → Fluxo de recuperação
7. **Opção:** "Cadastrar-se" → Fluxo de registro

## 2. Fluxo do Dashboard
**Início:** Usuário logado acessa dashboard
1. Sistema carrega lista de usuários online
2. Sistema carrega lista de grupos do usuário
3. Sistema carrega conversas recentes
4. Usuário pode:
   - Pesquisar usuários/grupos
   - Clicar em conversa existente → Vai para chat
   - Clicar no botão "+" → Opções de novo chat/grupo
   - Ver status online dos contatos

## 3. Fluxo de Chat Privado
**Início:** Usuário seleciona contato para conversar
1. Sistema carrega histórico de mensagens
2. Sistema estabelece conexão RMI para tempo real
3. Usuário pode:
   - Enviar mensagens de texto
   - Enviar arquivos (drag & drop)
   - Ver status de entrega/leitura
   - Ver indicador de digitação do contato
4. **Callbacks RMI:** Novas mensagens aparecem com animação sutil
5. **Feedback visual:** Mensagens enviadas/entregues/lidas

## 4. Fluxo de Chat em Grupo
**Início:** Usuário seleciona grupo para conversar
1. Sistema carrega histórico de mensagens do grupo
2. Sistema carrega lista de participantes
3. Sistema estabelece conexão RMI para tempo real
4. Usuário pode:
   - Enviar mensagens (com nome do remetente)
   - Ver lista de participantes online
   - **Se admin:** Acessar controles administrativos
5. **Controles de Admin:**
   - Convidar novos usuários
   - Remover participantes
   - Transferir liderança
   - Editar informações do grupo

## 5. Fluxo de Criação de Grupo
**Início:** Usuário clica em "Criar Grupo" no dashboard
1. Modal de criação abre
2. Usuário preenche:
   - Nome do grupo
   - Descrição (opcional)
   - Avatar do grupo (opcional)
3. Usuário seleciona participantes:
   - Campo de busca para encontrar usuários
   - Lista com checkboxes para seleção
4. Usuário configura permissões:
   - Quem pode adicionar membros
   - Quem pode editar informações do grupo
   - Privacidade (público/privado)
5. Usuário clica "Criar"
6. Sistema cria grupo e redireciona para o chat do grupo

## 6. Fluxo de Notificações RMI
**Callbacks em tempo real:**
1. **Nova mensagem recebida:**
   - Animação sutil na conversa
   - Badge de mensagem não lida
   - Som de notificação (opcional)
2. **Usuário online/offline:**
   - Atualização do status em tempo real
   - Indicador visual verde/cinza
3. **Digitação em andamento:**
   - Indicador "Usuário está digitando..."
   - Animação de pontos pulsantes
4. **Entrega/leitura de mensagem:**
   - Ícones de status (enviado ✓, entregue ✓✓, lido ✓✓ azul)

## Considerações de UX
- **Feedback imediato:** Todas as ações têm resposta visual instantânea
- **Estados de loading:** Spinners ou skeletons durante carregamento
- **Tratamento de erros:** Mensagens claras e ações sugeridas
- **Responsividade:** Interface adapta para mobile e desktop
- **Acessibilidade:** Suporte a leitores de tela e navegação por teclado

