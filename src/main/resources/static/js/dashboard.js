(() => {
    const form = document.getElementById('dashboard-filtros');
    if (!form) return;

    const erro = document.getElementById('dashboard-erro');
    const carregando = document.getElementById('dashboard-carregando');
    const ranking = document.getElementById('ranking-produtos');
    const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
    const coresStatus = ['#b42332', '#f97316', '#f59e0b', '#2563eb', '#7c3aed', '#16a34a', '#64748b'];
    let graficoVendas;
    let graficoStatus;

    const hoje = new Date();
    const inicio = new Date(hoje);
    inicio.setDate(inicio.getDate() - 29);
    form.elements.dataInicio.value = dataLocal(inicio);
    form.elements.dataFim.value = dataLocal(hoje);

    form.addEventListener('submit', evento => {
        evento.preventDefault();
        carregarDashboard();
    });

    carregarDashboard();

    async function carregarDashboard() {
        erro.classList.add('d-none');
        carregando.classList.remove('d-none');

        const parametros = new URLSearchParams(new FormData(form));
        for (const [chave, valor] of parametros.entries()) {
            if (!valor) parametros.delete(chave);
        }

        try {
            const resposta = await fetch(`/api/dashboard?${parametros.toString()}`, {
                headers: { Accept: 'application/json' },
                credentials: 'same-origin'
            });
            const dados = await resposta.json().catch(() => ({}));
            if (!resposta.ok) {
                throw new Error(dados.erro || `Não foi possível consultar os dados (HTTP ${resposta.status}).`);
            }
            atualizarPainel(dados);
            document.getElementById('dashboard-atualizado').textContent =
                `Atualizado às ${new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' }).format(new Date())}`;
        } catch (falha) {
            erro.textContent = falha.message || 'Falha ao carregar o dashboard. Verifique a conexão e tente novamente.';
            erro.classList.remove('d-none');
        } finally {
            carregando.classList.add('d-none');
        }
    }

    function atualizarPainel(dados) {
        document.getElementById('kpi-vendas').textContent = moeda.format(dados.totalVendas || 0);
        document.getElementById('kpi-pedidos').textContent = numero(dados.totalPedidos);
        document.getElementById('kpi-pedidos-venda').textContent = numero(dados.pedidosComVenda);
        document.getElementById('kpi-ticket').textContent = moeda.format(dados.ticketMedio || 0);
        desenharGraficoVendas(dados.vendasPorDia || []);
        desenharGraficoStatus(dados.pedidosPorStatus || []);
        desenharRanking(dados.produtosMaisVendidos || []);
    }

    function desenharGraficoVendas(vendas) {
        if (graficoVendas) graficoVendas.destroy();
        graficoVendas = new Chart(document.getElementById('grafico-vendas'), {
            type: 'line',
            data: {
                labels: vendas.map(item => new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' }).format(new Date(`${item.data}T12:00:00`))),
                datasets: [{
                    label: 'Vendas',
                    data: vendas.map(item => Number(item.vendas || 0)),
                    borderColor: '#b42332',
                    backgroundColor: 'rgba(180, 35, 50, .12)',
                    borderWidth: 2,
                    pointRadius: 2,
                    fill: true,
                    tension: .3
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, ticks: { callback: valor => moeda.format(valor) } },
                    x: { ticks: { maxTicksLimit: 12 } }
                }
            }
        });
    }

    function desenharGraficoStatus(statuses) {
        if (graficoStatus) graficoStatus.destroy();
        graficoStatus = new Chart(document.getElementById('grafico-status'), {
            type: 'doughnut',
            data: {
                labels: statuses.map(item => item.descricao),
                datasets: [{
                    data: statuses.map(item => Number(item.quantidade)),
                    backgroundColor: coresStatus,
                    borderWidth: 2,
                    borderColor: '#fff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom' } },
                cutout: '62%'
            }
        });
    }

    function desenharRanking(produtos) {
        ranking.replaceChildren();
        if (produtos.length === 0) {
            const linha = document.createElement('tr');
            const celula = document.createElement('td');
            celula.colSpan = 4;
            celula.className = 'text-secondary py-4';
            celula.textContent = 'Nenhum produto vendido no período selecionado.';
            linha.append(celula);
            ranking.append(linha);
            return;
        }

        produtos.forEach((produto, indice) => {
            const linha = document.createElement('tr');
            [indice + 1, produto.nome, numero(produto.quantidade), moeda.format(produto.vendas || 0)]
                .forEach(valor => {
                    const celula = document.createElement('td');
                    celula.textContent = valor;
                    linha.append(celula);
                });
            ranking.append(linha);
        });
    }

    function numero(valor) {
        return new Intl.NumberFormat('pt-BR').format(valor || 0);
    }

    function dataLocal(data) {
        const ano = data.getFullYear();
        const mes = String(data.getMonth() + 1).padStart(2, '0');
        const dia = String(data.getDate()).padStart(2, '0');
        return `${ano}-${mes}-${dia}`;
    }
})();
