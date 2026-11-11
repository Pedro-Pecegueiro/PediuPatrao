# Evidências de testes

## Execução automatizada

Comando executado na raiz do projeto em 10/11/2026:

```powershell
.\mvnw.cmd test
```

Resultado Maven: `BUILD SUCCESS` — 19 testes executados, 0 falhas, 0 erros e 0 ignorados.

## Casos cobertos

- `DashboardServiceTest`: cálculo dos indicadores, série com dias sem pedidos, distribuição por status, ranking, exclusão de cancelamentos das vendas, aplicação do filtro de status e validação do período.
- `DashboardApiControllerTest`: consulta autenticada por gerente, bloqueio de acesso para atendente e rejeição de data em formato inválido.
- `AutorizacaoApiTest`: autorização de pedidos e proteção CSRF.
- `PedidoServiceTest`: regras de criação, fluxo, descontos, cancelamentos e dados de auditoria.
- `ClienteServiceTest` e `AuditoriaServiceTest`: alterações cadastrais e persistência dos eventos de auditoria.

Os relatórios detalhados do Maven são gerados localmente em `target/surefire-reports/` a cada execução. Os testes de serviço usam repositórios simulados e os testes de API usam `MockMvc`; esta suíte não substitui um teste de integração manual com MongoDB em execução.

## Verificação local com MongoDB

Em 10/11/2026, a aplicação foi iniciada com `spring-boot:run` e conectou ao MongoDB local em `localhost:27017`. Após autenticação com perfil `ADMIN`, `/dashboard` respondeu HTTP 200 e `/api/dashboard?dataInicio=2026-11-01&dataFim=2026-11-10` retornou JSON válido, com os sete status. Não havia pedidos nesse intervalo; os indicadores e o ranking vieram vazios ou zerados, conforme as regras de cálculo.

A suíte automatizada complementa essa verificação: os testes de serviço usam `MongoTemplate` simulado e os testes dos endpoints usam `MockMvc`. Os relatórios detalhados são gerados em `target/surefire-reports/` após cada execução.
