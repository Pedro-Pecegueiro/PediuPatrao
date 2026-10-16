PediuPatrão
Sistema web para gerenciamento de uma pizzaria. Permite cadastrar clientes e produtos, registrar e acompanhar pedidos, controlar o acesso dos funcionários e consultar as alterações registradas na auditoria.
Tecnologias
- Java 21
- Spring Boot
- Spring Security
- Thymeleaf
- MongoDB
- Maven Wrapper
Requisitos
- JDK 21 instalado;
- MongoDB instalado e em execução;
- porta 8080 disponível.
Criar o banco de dados
Abra o PowerShell e conecte-se ao MongoDB:
mongosh
No console do MongoDB, execute:
use pediupatrao_pedro
db.createCollection("clientes")
show dbs
exit
O comando show dbs deve exibir pediupatrao_pedro na lista.
Executar a aplicação
No PowerShell, acesse a pasta raiz do projeto e inicie a aplicação:
.\mvnw.cmd spring-boot:run
Com a aplicação em execução, acesse http://localhost:8080. Para encerrá-la, pressione Ctrl+C no terminal.
Na primeira inicialização, as contas abaixo são criadas quando ainda não existem no banco. A senha de uma conta existente não é redefinida.
Contas locais
Usuário	Senha inicial	Perfil
admin	Admin@123	Administrador (ADMIN)
gerente	Gerente@123	Gerente (GERENTE)
atendente	Atendente@123	Atendente (ATENDENTE)


As senhas iniciais podem ser definidas pelas variáveis de ambiente ADMIN_PASSWORD, GERENTE_PASSWORD e ATENDENTE_PASSWORD.
Perfis de acesso
Todos os perfis podem consultar clientes, produtos e pedidos.
O perfil ADMIN administra usuários, inclui, altera e exclui produtos, exclui clientes e consulta a auditoria. Não registra pedidos, não altera dados de clientes e não aplica descontos nem cancela pedidos.
O perfil GERENTE inclui, altera e exclui produtos e clientes, registra pedidos, atualiza seus status, aplica descontos, cancela pedidos e consulta a auditoria. Não administra usuários.
O perfil ATENDENTE inclui e altera clientes, registra pedidos e atualiza seus status. Não administra usuários, produtos ou auditoria, não exclui clientes e não aplica descontos nem cancela pedidos.
As permissões são verificadas no servidor. O desconto é limitado a 20% e só pode ser aplicado antes da saída do pedido. O cancelamento também deve ocorrer antes da saída e exige justificativa. Essas operações são permitidas somente ao perfil GERENTE.
Regras dos pedidos
O valor do pedido é calculado no servidor com base nos produtos e preços cadastrados. O fluxo dos pedidos é:
RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA → FINALIZADO
                                  └→ RETIRADO ─────────→ FINALIZADO
O sistema registra o funcionário e a data e hora de entrada do pedido. Quando há saída, registra também os dados correspondentes. Transições inválidas e alterações em pedidos finalizados ou cancelados são bloqueadas.
Auditoria e segurança
Os registros de auditoria podem incluir usuário responsável, perfil, data e hora, operação, entidade, identificador, campos alterados e justificativa, quando aplicável. A consulta é restrita aos perfis ADMIN e GERENTE. Não há operações para editar ou excluir registros de auditoria.
As senhas são protegidas com BCrypt e não são incluídas nas respostas JSON. A proteção CSRF permanece habilitada nos formulários e nas operações sensíveis. A atribuição de perfis é restrita à administração de usuários; não há cadastro público nem autoelevação de perfil.
Testes automatizados
Na raiz do projeto, execute:
.\mvnw.cmd test
A suíte automatizada cobre cálculo de pedidos, registro de entrada e saída, transições de status, descontos, cancelamentos, permissões dos perfis, proteção CSRF e dados de auditoria de pedidos e clientes.
