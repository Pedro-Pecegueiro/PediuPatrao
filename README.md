# PediuPatrão

Sistema web para gerenciamento de uma pizzaria. Permite cadastrar clientes e produtos, registrar e acompanhar pedidos, controlar o acesso dos funcionários e consultar as alterações registradas em auditoria.

## Tecnologias

- Java 21
- Spring Boot
- Spring Security
- Thymeleaf
- MongoDB
- Maven Wrapper

## Requisitos

- JDK 21 instalado;
- MongoDB em execução em `localhost:27017`;
- `mongosh` disponível no terminal;
- porta `8080` livre;
- conexão com a internet na primeira execução do Maven para baixar as dependências.

## Criar o banco de dados

Abra o PowerShell e conecte-se ao MongoDB:

```powershell
mongosh
```

No console do MongoDB, execute:

```javascript
use pediupatrao_pedro
db.createCollection("clientes")
show dbs
exit
```

Confirme que `pediupatrao_pedro` aparece na lista exibida por `show dbs`. Ao iniciar, a aplicação cria as demais coleções necessárias, caso ainda não existam.

## Executar a aplicação

No PowerShell, acesse a pasta raiz do projeto e execute:

```powershell
.\mvnw.cmd spring-boot:run
```

Acesse [http://localhost:8080](http://localhost:8080) no navegador. Para encerrar a aplicação, pressione `Ctrl+C` no terminal em que ela está sendo executada.

Na primeira inicialização, o sistema cria as contas abaixo quando elas ainda não existem no banco. A senha de uma conta já existente não é redefinida.

## Contas locais

| Usuário | Senha inicial | Perfil |
|---|---|---|
| `admin` | `Admin@123` | Administrador (`ADMIN`) |
| `gerente` | `Gerente@123` | Gerente (`GERENTE`) |
| `atendente` | `Atendente@123` | Atendente (`ATENDENTE`) |

## Perfis de acesso

Todos os perfis podem consultar clientes, produtos e pedidos.

O perfil `ADMIN` administra usuários, inclui, altera e exclui produtos, exclui clientes e consulta a auditoria. Não pode incluir ou alterar clientes, registrar pedidos, atualizar status, aplicar descontos ou cancelar pedidos.

O perfil `GERENTE` inclui, altera e exclui produtos e clientes, registra pedidos, atualiza seus status, aplica descontos, cancela pedidos e consulta a auditoria. Não administra usuários.

O perfil `ATENDENTE` inclui e altera clientes, registra pedidos e atualiza seus status. Não administra usuários nem produtos, não exclui clientes, não consulta a auditoria, não aplica descontos e não cancela pedidos.

As permissões são verificadas no servidor. O desconto é limitado a 20% e só pode ser aplicado antes da saída do pedido. O cancelamento também deve ocorrer antes da saída e exige justificativa. Somente o perfil `GERENTE` pode aplicar descontos ou cancelar pedidos.

## Regras dos pedidos

O valor do pedido é calculado no servidor com base nos produtos e preços cadastrados. O fluxo de status é:

```text
RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA → FINALIZADO
                                  └→ RETIRADO ─────────→ FINALIZADO
```

O sistema registra o funcionário responsável e a data e hora de entrada do pedido. Quando o pedido sai para entrega ou é retirado, também registra os dados da saída. O sistema bloqueia transições inválidas e alterações em pedidos finalizados ou cancelados.

## Auditoria e segurança

A auditoria registra o usuário responsável, perfil, data e hora, operação, entidade, identificador e campos alterados. Também registra justificativa quando a operação exige uma. A consulta à auditoria é restrita aos perfis `ADMIN` e `GERENTE`; não há operações para editar ou excluir esses registros.

As senhas são protegidas com BCrypt e não são incluídas nas respostas JSON. A proteção CSRF permanece habilitada. Somente o perfil `ADMIN` pode administrar usuários e atribuir perfis. O sistema não possui cadastro público nem permite que um usuário altere o próprio perfil.

## Testes automatizados

Na raiz do projeto, execute:

```powershell
.\mvnw.cmd test
```

A suíte automatizada verifica o cálculo dos pedidos, o registro de entrada e saída, as transições de status, os descontos e cancelamentos, as permissões dos perfis, a proteção CSRF e os dados de auditoria de pedidos e clientes.
