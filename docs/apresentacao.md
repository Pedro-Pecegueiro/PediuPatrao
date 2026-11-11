# Apresentação do dashboard

## Roteiro de até três minutos

### 0:00–0:25 — Contexto

“O PediuPatrão é um sistema de gerenciamento de pedidos de pizzaria. O dashboard administrativo reúne indicadores de vendas, situação dos pedidos e desempenho dos produtos para apoiar o acompanhamento da operação.”

### 0:25–0:55 — Acesso e filtros

“O dashboard é restrito aos perfis administrador e gerente. Essa autorização é aplicada no servidor, tanto na página quanto na API. A consulta aceita data inicial, data final e status do pedido. O período padrão mostra os últimos 30 dias e a API limita cada consulta a 366 dias.”

### 0:55–1:40 — Demonstração do painel

Abra `/dashboard`. Mostre os indicadores de vendas, pedidos registrados, pedidos com venda e ticket médio. Aplique os filtros para atualizar os resultados. Em seguida, mostre o gráfico de vendas por dia, a distribuição dos pedidos por status e o ranking dos cinco produtos mais vendidos.

“Pedidos cancelados aparecem na contagem e na distribuição por status, mas não entram no faturamento, no ticket médio ou no ranking. O subtotal exibido no ranking corresponde aos itens antes do desconto geral do pedido.”

### 1:40–2:15 — API

Abra `/api/dashboard` na mesma sessão autenticada e mostre a resposta JSON consumida pela interface. A API também possui os endpoints `/api/dashboard/vendas`, `/api/dashboard/pedidos-por-status` e `/api/dashboard/produtos-mais-vendidos`. Datas inválidas, status inválidos e períodos fora do limite retornam HTTP 400.

### 2:15–2:45 — Testes e decisões

“Os testes automatizados verificam os cálculos, os filtros, o tratamento de pedidos cancelados e as permissões por perfil. O servidor calcula os indicadores usando os pedidos salvos no MongoDB; os valores financeiros não são enviados pelo navegador.”

### 2:45–3:00 — Encerramento

“O dashboard complementa o PediuPatrão com uma visão consolidada das vendas e da operação, mantendo os dados vinculados aos pedidos registrados no sistema.”
