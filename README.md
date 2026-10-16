# PediuPatrão

Sistema web para apoiar a operação de uma pizzaria. O projeto permite administrar clientes e produtos, registrar e acompanhar pedidos e controlar o acesso dos funcionários conforme suas responsabilidades. As alterações relevantes também são registradas para consulta em auditoria.

## Tecnologias

- Java 21
- Spring Boot
- Spring Security
- Thymeleaf
- MongoDB
- Maven Wrapper

## Requisitos

Antes de iniciar a aplicação, verifique se o ambiente possui:

- JDK 21 instalado;
- MongoDB em execução e acessível em `localhost:27017`;
- porta `8080` disponível.

## Execução

Abra um terminal na pasta raiz do projeto e execute:

```powershell
.\mvnw.cmd spring-boot:run
```

Com a aplicação iniciada, acesse [http://localhost:8080](http://localhost:8080) pelo navegador. Para encerrar, pressione `Ctrl+C` no terminal em que o servidor está sendo executado.

Por padrão, a aplicação conecta ao banco `pediupatrao_pedro`, no MongoDB local. O endereço de conexão pode ser configurado pela variável de ambiente `MONGODB_URI`. Exemplo para selecionar outro banco local:

```powershell
$env:MONGODB_URI = "mongodb://localhost:27017/outro_banco"
.\mvnw.cmd spring-boot:run
```

Na inicialização, as três contas locais são criadas quando ainda não existem no banco selecionado. A inicialização não redefine a senha de uma conta já existente.

## Contas locais

| Usuário | Senha inicial | Perfil |
| --- | --- | --- |
| `admin` | `Admin@123` | Administrador (`ADMIN`) |
| `gerente` | `Gerente@123` | Gerente (`GERENTE`) |
| `atendente` | `Atendente@123` | Atendente (`ATENDENTE`) |

As senhas iniciais podem ser configuradas pelas variáveis `ADMIN_PASSWORD`, `GERENTE_PASSWORD` e `ATENDENTE_PASSWORD`.

## Perfis e permissões

| Ação | ADMIN | GERENTE | ATENDENTE |
| --- | :---: | :---: | :---: |
| Consultar clientes, produtos e pedidos | Sim | Sim | Sim |
| Administrar usuários | Sim | Não | Não |
| Incluir, alterar e excluir produtos | Sim | Sim | Não |
| Incluir e alterar clientes | Não | Sim | Sim |
| Excluir clientes | Sim | Sim | Não |
| Registrar pedidos e atualizar status | Não | Sim | Sim |
| Aplicar desconto e cancelar pedido | Não | Sim | Não |
| Consultar auditoria | Sim | Sim | Não |

As permissões são aplicadas no servidor, além do controle de opções exibidas na interface. O desconto é limitado a 20% e só pode ser aplicado antes da saída do pedido. O cancelamento também deve ocorrer antes da saída e exige justificativa. Essas duas operações são exclusivas do perfil `GERENTE`.

## Regras dos pedidos

O valor do pedido é calculado no servidor com base nos produtos e preços cadastrados. A sequência normal de andamento é:


RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA → FINALIZADO
                              └────→ RETIRADO ─────────→ FINALIZADO


O sistema registra o funcionário e a data e hora de entrada do pedido e, quando aplicável, os dados de sua saída. Transições inválidas e alterações em pedidos finalizados ou cancelados são bloqueadas.

## Auditoria e segurança

Os registros de auditoria podem conter o usuário responsável, perfil, data e hora, operação, entidade, identificador, campos alterados e justificativa, quando aplicável. A consulta é restrita aos perfis `ADMIN` e `GERENTE`. Não há operações comuns para editar ou excluir registros de auditoria.

As senhas são protegidas com BCrypt e não são devolvidas nas respostas JSON. A proteção CSRF permanece habilitada nos formulários e nas operações sensíveis. A atribuição de perfis é restrita à administração de usuários; não há cadastro público nem mecanismo de autoelevação.

## Testes automatizados

Na raiz do projeto, execute:

```powershell
.\mvnw.cmd test
```

A suíte automatizada cobre o cálculo dos pedidos, o registro de entrada e saída, as transições de status, os descontos e cancelamentos, as permissões dos perfis, a proteção CSRF e os dados enviados para auditoria de pedidos e clientes.

