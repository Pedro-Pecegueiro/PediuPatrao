# API do dashboard

## Acesso e formato

Os endpoints são `GET`, retornam JSON e exigem sessão autenticada com perfil `ADMIN` ou `GERENTE`. A interface `/dashboard` consome `/api/dashboard` na mesma origem.

Os parâmetros opcionais são compartilhados por todos os endpoints:

| Parâmetro | Tipo | Regra |
|---|---|---|
| `dataInicio` | `AAAA-MM-DD` | Primeiro dia incluído. Sem valor, considera hoje menos 29 dias. |
| `dataFim` | `AAAA-MM-DD` | Último dia incluído. Sem valor, considera hoje. |
| `status` | Enum `StatusPedido` | Um de `RECEBIDO`, `EM_PREPARACAO`, `PRONTO`, `SAIU_PARA_ENTREGA`, `RETIRADO`, `FINALIZADO` ou `CANCELADO`. Sem valor, considera todos. |

O período máximo é de 366 dias. Os valores usam datas no fuso local configurado no servidor.

## Endpoints

### `GET /api/dashboard`

Retorna indicadores, série diária, distribuição por status e ranking de produtos em uma resposta.

Exemplo:

```http
GET /api/dashboard?dataInicio=2026-11-01&dataFim=2026-11-10&status=FINALIZADO
Accept: application/json
```

Exemplo de resposta `200 OK`:

```json
{
  "dataInicio": "2026-11-01",
  "dataFim": "2026-11-10",
  "statusFiltrado": "FINALIZADO",
  "totalPedidos": 12,
  "pedidosComVenda": 12,
  "totalVendas": 734.80,
  "ticketMedio": 61.23,
  "vendasPorDia": [
    { "data": "2026-11-01", "pedidos": 2, "vendas": 104.80 },
    { "data": "2026-11-02", "pedidos": 0, "vendas": 0.00 }
  ],
  "pedidosPorStatus": [
    { "status": "RECEBIDO", "descricao": "Recebido", "quantidade": 0 },
    { "status": "FINALIZADO", "descricao": "Finalizado", "quantidade": 12 },
    { "status": "CANCELADO", "descricao": "Cancelado", "quantidade": 0 }
  ],
  "produtosMaisVendidos": [
    { "produtoId": "produto-1", "nome": "Pizza Margherita", "quantidade": 8, "vendas": 399.20 }
  ]
}
```

### `GET /api/dashboard/vendas`

Retorna somente a série temporal usada no gráfico de vendas.

Exemplo: `GET /api/dashboard/vendas?dataInicio=2026-11-01&dataFim=2026-11-10`

```json
[
  { "data": "2026-11-01", "pedidos": 2, "vendas": 104.80 },
  { "data": "2026-11-02", "pedidos": 0, "vendas": 0.00 }
]
```

### `GET /api/dashboard/pedidos-por-status`

Retorna a quantidade de pedidos por status. Inclui status com quantidade zero.

Exemplo: `GET /api/dashboard/pedidos-por-status?dataInicio=2026-11-01&dataFim=2026-11-10`

```json
[
  { "status": "RECEBIDO", "descricao": "Recebido", "quantidade": 3 },
  { "status": "CANCELADO", "descricao": "Cancelado", "quantidade": 1 }
]
```

### `GET /api/dashboard/produtos-mais-vendidos`

Retorna até cinco produtos, ordenados por unidades vendidas em pedidos não cancelados.

Exemplo: `GET /api/dashboard/produtos-mais-vendidos?dataInicio=2026-11-01&dataFim=2026-11-10`. O campo `vendas` representa a soma dos subtotais dos itens antes do desconto geral aplicado ao pedido.

```json
[
  { "produtoId": "produto-1", "nome": "Pizza Margherita", "quantidade": 8, "vendas": 399.20 }
]
```

## Erros

| HTTP | Situação | Exemplo de corpo |
|---|---|---|
| `400 Bad Request` | Data fora do formato ISO, status inválido, data inicial posterior à final ou período acima de 366 dias. | `{"timestamp":"2026-11-10T12:00:00Z","status":400,"erro":"A data inicial deve ser anterior ou igual à data final."}` |
| `302 Found` | Usuário sem sessão é redirecionado para `/login` pelo login de formulário da aplicação. | Redirecionamento para a tela de login. |
| `403 Forbidden` | Usuário autenticado sem perfil `ADMIN` ou `GERENTE`. | Resposta de acesso negado. |
| `500 Internal Server Error` | Falha inesperada na aplicação ou indisponibilidade do MongoDB. | Resposta de erro padrão do servidor. |

Exemplo de período inválido: `GET /api/dashboard?dataInicio=2026-11-10&dataFim=2026-11-01` retorna `400 Bad Request`.
