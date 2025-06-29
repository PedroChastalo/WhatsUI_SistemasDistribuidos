A ideia por trás da arquitetura do projeto atual é permitir que cada módulo e suas funcionalidades contextuais sejam isolados do restante da aplicação, garantindo maior organização dos arquivos e pastas existentes no projeto, a partir da separação por responsabilidade de cada item presente, contextual ou compartilhadamente.

## Estrutura do projeto

### /assets

Nesta página serão armazenados estilos, fontes, imagens, lotties e outros assets presentes na aplicação.

### /data

Todos os arquivos e pastas presentes nesta pasta gerenciará algum tipo de informação GLOBALMENTE pela aplicação. Sejam serviços, contextos, hooks ou states.

/data/contexts

Todos os contextos de utilização global devem estar presentes nesta pasta.

/data/hooks

Todos os hooks que manipulem algum tipo de informação devem estar nesta página, seja ele um hook wrapper do React Query ou qualquer outro.

/data/services

Todos os services (conectores de API) que serão utilizados de forma global devem estar armazenados nesta página, inclusive o adaptador HTTP principal da aplicação.

/data/states

Todos os states de utilização global, sejam eles do Redux, zustand ou Recoil, devem estar presentes nesta página. Cada um separado em sua página.

### /modules

Todos os módulos presentes na aplicação devem estar presentes nesta pasta. Lembrando que módulos são subdivisões da aplicação, como autenticação, por exemplo. Um módulo pode conter submódulos, que deverão ser armazeandos dentro da pasta submodules, respeitando a estrutura da pasta modules.

/modules/[modulo]/components

Todos os componentes ESPECÍFICOS do módulo X devem estar presentes nesta pasta. Imagine que, no módulo de autenticação exista um componente que cuide da autenticação via OAuth2 ou TOTP. Este componente com certeza não será reutilizado em outras áreas da aplicação, desta forma, podemos assumí-lo como um componente contextual de autenticação, isto é, só será utilizado dentro do contexto do módulo de autenticação.

/modules/[modulo]/data

Todos os itens que gerenciarem informações de forma contextual, isto é, informações que somente serão utilizadas no contexto do módulo X, estarão presentes nesta pasta.

Geralmente existirão services para cada módulo, como por exemplo, service de autenticação para o módulo de autenticação. Este service criará uma conexão externa com um servidor REST API, por exemplo. Pode existir a possibilidade de um módulo conter mais de um service, como por exemplo um módulo de produto conter o service de Product, ProductCharacteristics, ProductFees, ProductVariants. A ideia é que cada service seja responsável apenas pela sua função. Os services precisam ser classes que extendam o adaptador HTTP previamente definido em `/data/services`.

Também estarão presentes nesta pasta as mutations, query, suspenseQueries, contextos e states contextuais ao módulo.

/modules/[modulo]/data/hooks

Nesta pasta ficarão os hooks que trafeguem informações, sejam query, mutations ou suspenseQueries.

/modules/[modulo]/data/services

Nesta pasta ficarão os services que extenderão os adaptadores HTTP presentes em `/data/services`. Cada service contará também com seus `schemas`, `types` e `utils`, caso necessário.

/modules/[modulo]/data/contexts

Nesta pasta ficarão todos os contextos do módulo.

/modules/[modulo]/screens

Todas as telas que serão retornadas pelo módulo na pasta `/app` precisarão estar nesta página. Deve seguir a seguinte estrutura:

Página

`/[nome da tela, ex: login]/page.tsx`

Skeleton (caso a tela necessite)

`/[nome da tela, ex: product/skeleton.tsx`

/modules/[modulo]/types

Todas as interfaces ou types pertinentes ao módulo deverão estar nesta pasta.

### /shared

Nesta página ficarão armazenados arquivos, componentes e utilitários que são amplamente utilizados em N contextos da aplicação, portanto, necessita muita atenção para alterar ou remover itens desta página, pois poderão causar efeitos em N áreas da aplicação.

/shared/components

Todos os componentes de utilização global da aplicação devem estar aqui, com exceção dos componentes provenientes de bibliotecas, como inputs ou buttons, se este for o caso. Devem ser organizados em pastas, contendo seu nome, exemplo:

`/shared/components/[nome do componente/index.tsx`

Componente: Avatar

`/shared/components/Avatar/index.tsx`

/shared/interfaces (ou types)

Todas as interfaces ou types de utilização global ou off-context devem estar presentes nesta pasta.

/shared/schemas

Qualquer schema de utilização global ou em server actions deve estar presente nesta pasta.

/shared/utils

Qualquer função, constantes ou manipulador de informação de utilização global ou off-context deve estar aqui.