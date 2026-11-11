# Requisitos do dashboard administrativo

## Objetivo

Permitir que a administração acompanhe vendas e pedidos a partir dos dados persistidos pelo PediuPatrão. A interface consulta endpoints da API do próprio sistema; não utiliza dados fixos de demonstração.

## Requisitos funcionais

- **RF01 — Indicadores gerais de vendas:** apresentar valor de vendas no período, quantidade total de pedidos, quantidade de pedidos que geraram venda e ticket médio. Pedidos cancelados entram na contagem total, mas não entram no valor de vendas, na quantidade de vendas nem no ticket médio.
- **RF02 — Análise de vendas:** exibir um gráfico de linha com o valor vendido por dia e um gráfico de rosca com a quantidade de pedidos por status. Dias sem venda aparecem com valor zero.
- **RF03 — Situação dos pedidos:** apresentar a quantidade de pedidos em cada status do sistema, incluindo status sem ocorrências no período.
- **RF04 — Produtos mais vendidos:** exibir até cinco produtos ordenados pela quantidade de unidades em pedidos não cancelados. Para cada produto, apresentar as unidades e o subtotal bruto dos itens, antes do desconto geral do pedido.
- **RF05 — Filtros de consulta:** permitir a seleção de data inicial, data final e status do pedido. O período deve estar em ordem cronológica e não pode ultrapassar 366 dias. Sem parâmetros, a consulta considera os últimos 30 dias, incluindo hoje.
- **RF06 — Integração com a API:** carregar os dados do painel pelo endpoint `GET /api/dashboard`, usando a sessão autenticada. A API também disponibiliza endpoints individuais para vendas por dia, pedidos por status e produtos mais vendidos.
- **RF07 — Atualização e tratamento de dados:** buscar os dados ao abrir o painel e ao aplicar filtros, indicar o horário da última atualização, exibir mensagem para falhas HTTP ou de rede e apresentar estado vazio quando não houver produtos vendidos.

## Requisitos não funcionais

- **RNF01 — Controle de acesso:** somente usuários autenticados com perfil `ADMIN` ou `GERENTE` podem abrir o painel e consultar seus endpoints. O controle é aplicado no servidor.
- **RNF02 — Integridade dos dados:** indicadores e agrupamentos são calculados a partir de pedidos e itens persistidos no MongoDB; pedidos cancelados são excluídos dos cálculos de venda.
- **RNF03 — Validação:** datas inválidas, status desconhecidos, período invertido ou superior a 366 dias são rejeitados com HTTP 400.
- **RNF04 — Compatibilidade:** interface web responsiva e API JSON executada na mesma origem da aplicação.
- **RNF05 — Segurança da sessão:** o dashboard não expõe dados sem autenticação e não aceita parâmetros de valores financeiros enviados pelo navegador para calcular indicadores.

## Regras de cálculo

- O período inclui o dia inicial e o dia final, conforme o fuso horário local do servidor.
- `totalPedidos` conta todos os pedidos com data de entrada no período, após eventual filtro por status.
- `pedidosComVenda` conta pedidos do período cujo status não seja `CANCELADO`.
- `totalVendas` é a soma de `valorTotal` dos pedidos que geram venda.
- `ticketMedio` é `totalVendas / pedidosComVenda`, arredondado para duas casas decimais; quando não há pedidos com venda, o resultado é `0,00`.
- `vendasPorDia` soma valores e quantidades de pedidos não cancelados em cada dia.
- `pedidosPorStatus` conta os pedidos filtrados por data/status e retorna todos os status, inclusive com quantidade zero.
- `produtosMaisVendidos` soma unidades e subtotais brutos dos itens de pedidos não cancelados e limita a resposta aos cinco primeiros. O subtotal dos itens não rateia o desconto aplicado ao pedido; o indicador `totalVendas` já considera o valor total líquido gravado no pedido.
- Quando informado, o filtro `status` restringe todos os indicadores e agrupamentos ao status solicitado. Para o filtro `CANCELADO`, as vendas e o ranking resultam em zero ou lista vazia.
