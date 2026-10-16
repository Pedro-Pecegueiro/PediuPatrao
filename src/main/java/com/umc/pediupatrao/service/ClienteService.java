package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;

    public ClienteService(ClienteRepository clienteRepository, AuditoriaService auditoriaService) {
        this.clienteRepository = clienteRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public Cliente buscarPorId(String id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
    }

    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Cliente salvar(Cliente recebido) {
        validar(recebido);
        if (recebido.getId() == null || recebido.getId().isBlank()) {
            Cliente salvo = clienteRepository.save(recebido);
            auditoriaService.registrar("INCLUSAO", "CLIENTE", salvo.getId(),
                    snapshot(null, salvo), null);
            return salvo;
        }

        Cliente existente = buscarPorId(recebido.getId());
        Map<String, AlteracaoCampo> mudancas = snapshot(existente, recebido);
        copiarDados(recebido, existente);
        Cliente salvo = clienteRepository.save(existente);
        if (!mudancas.isEmpty()) {
            auditoriaService.registrar("ALTERACAO", "CLIENTE", salvo.getId(), mudancas, null);
        }
        return salvo;
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public void excluir(String id) {
        Cliente existente = buscarPorId(id);
        clienteRepository.delete(existente);
        auditoriaService.registrar("EXCLUSAO", "CLIENTE", id, snapshot(existente, null), null);
    }

    private void validar(Cliente cliente) {
        if (cliente.getNome() == null || cliente.getNome().isBlank()) {
            throw new RegraNegocioException("O nome do cliente é obrigatório.");
        }
        if (cliente.getTelefone() == null || cliente.getTelefone().isBlank()) {
            throw new RegraNegocioException("O telefone do cliente é obrigatório.");
        }
    }

    private void copiarDados(Cliente origem, Cliente destino) {
        destino.setNome(origem.getNome());
        destino.setTelefone(origem.getTelefone());
        destino.setCep(origem.getCep());
        destino.setLogradouro(origem.getLogradouro());
        destino.setNumero(origem.getNumero());
        destino.setComplemento(origem.getComplemento());
        destino.setBairro(origem.getBairro());
        destino.setCidade(origem.getCidade());
        destino.setEstado(origem.getEstado());
        destino.setEndereco(origem.getEndereco());
    }

    private Map<String, AlteracaoCampo> snapshot(Cliente anterior, Cliente novo) {
        Map<String, AlteracaoCampo> mudancas = new LinkedHashMap<>();
        adicionar(mudancas, "nome", valor(anterior, Cliente::getNome), valor(novo, Cliente::getNome));
        adicionar(mudancas, "telefone", valor(anterior, Cliente::getTelefone), valor(novo, Cliente::getTelefone));
        adicionar(mudancas, "cep", valor(anterior, Cliente::getCep), valor(novo, Cliente::getCep));
        adicionar(mudancas, "logradouro", valor(anterior, Cliente::getLogradouro), valor(novo, Cliente::getLogradouro));
        adicionar(mudancas, "numero", valor(anterior, Cliente::getNumero), valor(novo, Cliente::getNumero));
        adicionar(mudancas, "complemento", valor(anterior, Cliente::getComplemento), valor(novo, Cliente::getComplemento));
        adicionar(mudancas, "bairro", valor(anterior, Cliente::getBairro), valor(novo, Cliente::getBairro));
        adicionar(mudancas, "cidade", valor(anterior, Cliente::getCidade), valor(novo, Cliente::getCidade));
        adicionar(mudancas, "estado", valor(anterior, Cliente::getEstado), valor(novo, Cliente::getEstado));
        adicionar(mudancas, "endereco", valor(anterior, Cliente::getEndereco), valor(novo, Cliente::getEndereco));
        return mudancas;
    }

    private String valor(Cliente cliente, java.util.function.Function<Cliente, String> getter) {
        return cliente == null ? null : getter.apply(cliente);
    }

    private void adicionar(Map<String, AlteracaoCampo> mudancas, String campo, Object anterior, Object novo) {
        if (!Objects.equals(anterior, novo)) {
            mudancas.put(campo, new AlteracaoCampo(anterior, novo));
        }
    }
}
