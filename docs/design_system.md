# Design System - WhatsUT

## Paleta de Cores

### Cores Primárias
- **Primary Blue**: #2563EB (Azul principal para botões e elementos de destaque)
- **Primary Blue Hover**: #1D4ED8 (Estado hover do azul principal)
- **Primary Blue Light**: #DBEAFE (Fundo sutil para elementos destacados)

### Cores Secundárias
- **Success Green**: #10B981 (Indicadores online, mensagens enviadas)
- **Warning Orange**: #F59E0B (Alertas e notificações)
- **Error Red**: #EF4444 (Erros e ações destrutivas)

### Cores Neutras
- **Gray 900**: #111827 (Texto principal)
- **Gray 700**: #374151 (Texto secundário)
- **Gray 500**: #6B7280 (Texto terciário, placeholders)
- **Gray 300**: #D1D5DB (Bordas, divisores)
- **Gray 100**: #F3F4F6 (Fundos sutis)
- **Gray 50**: #F9FAFB (Fundo principal)
- **White**: #FFFFFF (Fundo de cards, modais)

## Tipografia

### Fonte Principal
**Inter** - Fonte moderna e legível, otimizada para interfaces digitais

### Hierarquia Tipográfica
- **H1**: 2.25rem (36px) - Títulos principais
- **H2**: 1.875rem (30px) - Títulos de seção
- **H3**: 1.5rem (24px) - Subtítulos
- **Body Large**: 1.125rem (18px) - Texto importante
- **Body**: 1rem (16px) - Texto padrão
- **Body Small**: 0.875rem (14px) - Texto secundário
- **Caption**: 0.75rem (12px) - Legendas, timestamps

## Componentes e Padrões UX

### 1. Botões
#### Botão Primário
- Fundo: Primary Blue (#2563EB)
- Texto: Branco
- Hover: Primary Blue Hover (#1D4ED8)
- Padding: 12px 24px
- Border-radius: 8px
- Transição suave (200ms)

#### Botão Secundário
- Fundo: Transparente
- Borda: 1px solid Gray 300
- Texto: Gray 700
- Hover: Gray 100 background

#### Botão de Ação Destrutiva
- Fundo: Error Red (#EF4444)
- Texto: Branco
- Hover: Mais escuro

### 2. Campos de Input
- Borda: 1px solid Gray 300
- Focus: Borda Primary Blue + shadow azul sutil
- Padding: 12px 16px
- Border-radius: 8px
- Placeholder: Gray 500
- Error state: Borda vermelha + texto de erro

### 3. Cards de Conversa
- Fundo: White
- Sombra sutil: 0 1px 3px rgba(0,0,0,0.1)
- Border-radius: 12px
- Padding: 16px
- Hover: Sombra mais intensa

### 4. Bolhas de Mensagem
#### Mensagens Enviadas
- Fundo: Primary Blue (#2563EB)
- Texto: Branco
- Alinhamento: Direita
- Border-radius: 18px 18px 4px 18px

#### Mensagens Recebidas
- Fundo: Gray 100 (#F3F4F6)
- Texto: Gray 900
- Alinhamento: Esquerda
- Border-radius: 18px 18px 18px 4px

### 5. Indicadores de Status
#### Online
- Círculo verde (Success Green)
- Tamanho: 8px
- Posição: Canto inferior direito do avatar

#### Digitando
- Três pontos animados
- Cor: Gray 500
- Animação pulsante

#### Status de Mensagem
- Enviado: ✓ (Gray 500)
- Entregue: ✓✓ (Gray 500)
- Lido: ✓✓ (Primary Blue)

### 6. Modais
- Overlay: rgba(0,0,0,0.5)
- Fundo: White
- Border-radius: 16px
- Sombra: 0 20px 25px rgba(0,0,0,0.1)
- Animação de entrada: Scale + fade

### 7. Notificações
#### Toast de Sucesso
- Fundo: Success Green
- Texto: Branco
- Ícone: Check
- Posição: Top-right

#### Toast de Erro
- Fundo: Error Red
- Texto: Branco
- Ícone: X
- Posição: Top-right

### 8. Listagens
#### Lista de Usuários Online
- Item height: 56px
- Avatar: 40px diameter
- Status indicator: 8px
- Hover: Gray 50 background

#### Lista de Grupos
- Badge de mensagens não lidas: Primary Blue background
- Texto do badge: Branco
- Border-radius: 12px

## Animações e Transições

### Micro-interações
- **Hover states**: 200ms ease-out
- **Button press**: Scale 0.98 + 100ms
- **Modal open**: Scale 0.95 → 1.0 + opacity 0 → 1 (300ms)
- **Toast notifications**: Slide in from right (250ms)

### Animações RMI
- **Nova mensagem**: Slide up + fade in (300ms)
- **Usuário online**: Pulse do indicador verde (500ms)
- **Digitando**: Dots pulsing animation (1s loop)

## Responsividade

### Breakpoints
- **Mobile**: < 768px
- **Tablet**: 768px - 1024px
- **Desktop**: > 1024px

### Layout Adaptativo
- **Mobile**: Stack vertical, sidebar como drawer
- **Tablet**: Sidebar colapsável
- **Desktop**: Sidebar fixa, layout de 3 colunas

## Acessibilidade

### Contraste
- Todos os textos atendem WCAG AA (4.5:1)
- Elementos interativos têm contraste adequado

### Navegação
- Suporte completo a navegação por teclado
- Focus indicators visíveis
- Skip links para navegação rápida

### Screen Readers
- Labels apropriados em todos os elementos
- Estados dinâmicos anunciados
- Estrutura semântica correta

