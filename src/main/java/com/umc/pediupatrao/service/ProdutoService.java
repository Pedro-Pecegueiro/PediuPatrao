package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @PreAuthorize("isAuthenticated()")
    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    @PreAuthorize("isAuthenticated()")
    public List<Produto> listarAtivos() {
        return produtoRepository.findByAtivoTrueOrderByNomeAsc();
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public Produto salvar(Produto recebido) {
        validar(recebido);
        Produto destino = recebido.getId() == null || recebido.getId().isBlank()
                ? new Produto()
                : buscarPorId(recebido.getId());
        destino.setNome(recebido.getNome().trim());
        destino.setTipo(recebido.getTipo().trim());
        destino.setPreco(recebido.getPreco().setScale(2, RoundingMode.HALF_UP));
        destino.setAtivo(recebido.getAtivo() == null || recebido.getAtivo());
        return produtoRepository.save(destino);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public void excluir(String id) {
        produtoRepository.delete(buscarPorId(id));
    }

    public Produto buscarPorId(String id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));
    }

    private void validar(Produto produto) {
        if (produto.getNome() == null || produto.getNome().isBlank()
                || produto.getTipo() == null || produto.getTipo().isBlank()) {
            throw new RegraNegocioException("Nome e tipo do produto são obrigatórios.");
        }
        if (produto.getPreco() == null || produto.getPreco().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("O preço do produto deve ser maior que zero.");
        }
    }
}
