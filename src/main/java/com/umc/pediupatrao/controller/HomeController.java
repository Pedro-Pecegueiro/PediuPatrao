package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.dto.UsuarioRequest;
import com.umc.pediupatrao.dto.UsuarioUpdateRequest;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.PerfilUsuario;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import com.umc.pediupatrao.service.UsuarioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    public HomeController(PedidoService pedidoService, ClienteService clienteService,
                          ProdutoService produtoService, UsuarioService usuarioService,
                          AuditoriaService auditoriaService) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.usuarioService = usuarioService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/acesso-negado")
    public String acessoNegado(Model model) {
        model.addAttribute("content", "acesso-negado :: content");
        return "layout";
    }

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        model.addAttribute("totalPedidos", pedidoService.listarPedidos().size());
        model.addAttribute("totalClientes", clienteService.listarClientes().size());
        model.addAttribute("totalProdutos", produtoService.listarProdutos().size());
        model.addAttribute("usuarioAtual", authentication.getName());
        model.addAttribute("content", "home :: content");
        return "layout";
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        model.addAttribute("content", "usuarios/lista :: content");
        return "layout";
    }

    @GetMapping("/usuarios/novo")
    @PreAuthorize("hasRole('ADMIN')")
    public String novoUsuarioForm(Model model) {
        com.umc.pediupatrao.entity.Usuario usuario = new com.umc.pediupatrao.entity.Usuario();
        usuario.setAtivo(true);
        model.addAttribute("usuario", usuario);
        model.addAttribute("perfis", PerfilUsuario.values());
        model.addAttribute("content", "usuarios/form :: content");
        return "layout";
    }

    @GetMapping("/usuarios/editar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String editarUsuarioForm(@PathVariable String id, Model model) {
        model.addAttribute("usuario", usuarioService.buscarPorId(id));
        model.addAttribute("perfis", PerfilUsuario.values());
        model.addAttribute("content", "usuarios/form :: content");
        return "layout";
    }

    @PostMapping("/usuarios/salvar")
    @PreAuthorize("hasRole('ADMIN')")
    public String salvarUsuario(@RequestParam String username, @RequestParam String password,
                                @RequestParam PerfilUsuario role,
                                @RequestParam(defaultValue = "false") boolean ativo,
                                RedirectAttributes redirect) {
        return executar(() -> usuarioService.criar(new UsuarioRequest(username, password, role, ativo)),
                "Usuário criado com sucesso.", "/usuarios", redirect);
    }

    @PostMapping("/usuarios/editar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String atualizarUsuario(@PathVariable String id, @RequestParam String username,
                                   @RequestParam(required = false) String password,
                                   @RequestParam PerfilUsuario role,
                                   @RequestParam(defaultValue = "false") boolean ativo,
                                   RedirectAttributes redirect) {
        return executar(() -> usuarioService.atualizar(id,
                        new UsuarioUpdateRequest(username, password, role, ativo)),
                "Usuário atualizado com sucesso.", "/usuarios", redirect);
    }

    @PostMapping("/usuarios/deletar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deletarUsuario(@PathVariable String id, RedirectAttributes redirect) {
        return executar(() -> { usuarioService.excluir(id); return null; },
                "Usuário removido com sucesso.", "/usuarios", redirect);
    }

    @GetMapping("/clientes")
    public String clientes(Model model) {
        model.addAttribute("clientes", clienteService.listarClientes());
        model.addAttribute("content", "clientes/lista :: content");
        return "layout";
    }

    @GetMapping("/clientes/novo")
    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public String novoClienteForm(Model model) {
        model.addAttribute("cliente", new Cliente());
        model.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    @GetMapping("/clientes/editar/{id}")
    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public String editarClienteForm(@PathVariable String id, Model model) {
        model.addAttribute("cliente", clienteService.buscarPorId(id));
        model.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    @PostMapping({"/clientes/salvar", "/clientes/editar/{id}"})
    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public String salvarCliente(@PathVariable(required = false) String id,
                                @ModelAttribute Cliente cliente, RedirectAttributes redirect) {
        cliente.setId(id);
        return executar(() -> clienteService.salvar(cliente),
                id == null ? "Cliente criado com sucesso." : "Cliente atualizado com sucesso.",
                "/clientes", redirect);
    }

    @PostMapping("/clientes/deletar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public String excluirCliente(@PathVariable String id, RedirectAttributes redirect) {
        return executar(() -> { clienteService.excluir(id); return null; },
                "Cliente excluído e operação auditada.", "/clientes", redirect);
    }

    @GetMapping("/produtos")
    public String produtos(Model model) {
        model.addAttribute("produtos", produtoService.listarProdutos());
        model.addAttribute("content", "produtos/lista :: content");
        return "layout";
    }

    @GetMapping("/produtos/novo")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public String novoProduto(Model model) {
        Produto produto = new Produto();
        produto.setAtivo(true);
        model.addAttribute("produto", produto);
        model.addAttribute("content", "produtos/produto-form :: content");
        return "layout";
    }

    @GetMapping("/produtos/editar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public String editarProduto(@PathVariable String id, Model model) {
        model.addAttribute("produto", produtoService.buscarPorId(id));
        model.addAttribute("content", "produtos/produto-form :: content");
        return "layout";
    }

    @PostMapping("/produtos/salvar")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public String salvarProduto(@ModelAttribute Produto produto, RedirectAttributes redirect) {
        return executar(() -> produtoService.salvar(produto),
                "Produto salvo com sucesso.", "/produtos", redirect);
    }

    @PostMapping("/produtos/deletar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public String excluirProduto(@PathVariable String id, RedirectAttributes redirect) {
        return executar(() -> { produtoService.excluir(id); return null; },
                "Produto removido com sucesso.", "/produtos", redirect);
    }

    @GetMapping("/pedidos")
    public String pedidos(Model model) {
        model.addAttribute("pedidos", pedidoService.listarPedidos());
        model.addAttribute("content", "pedidos :: content");
        return "layout";
    }

    @GetMapping("/pedidos/novo")
    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public String novoPedido(Model model) {
        model.addAttribute("clientes", clienteService.listarClientes());
        model.addAttribute("produtos", produtoService.listarAtivos());
        model.addAttribute("content", "pedido-form :: content");
        return "layout";
    }

    @PostMapping("/pedidos/salvar")
    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public String salvarPedido(@RequestParam String clienteId,
                               @RequestParam("produtoId") List<String> produtoIds,
                               @RequestParam("quantidade") List<String> quantidades,
                               RedirectAttributes redirect) {
        return executar(() -> pedidoService.criarPedido(
                        new PedidoRequest(clienteId, converterItens(produtoIds, quantidades))),
                "Pedido registrado e auditado com sucesso.", "/pedidos", redirect);
    }

    @PostMapping("/pedidos/{id}/status")
    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public String atualizarStatus(@PathVariable String id, @RequestParam StatusPedido status,
                                  RedirectAttributes redirect) {
        return executar(() -> pedidoService.atualizarStatus(id, status),
                "Status atualizado com sucesso.", "/pedidos", redirect);
    }

    @PostMapping("/pedidos/{id}/desconto")
    @PreAuthorize("hasRole('GERENTE')")
    public String aplicarDesconto(@PathVariable String id, @RequestParam BigDecimal percentual,
                                  RedirectAttributes redirect) {
        return executar(() -> pedidoService.aplicarDesconto(id, percentual),
                "Desconto aplicado e auditado.", "/pedidos", redirect);
    }

    @PostMapping("/pedidos/{id}/cancelar")
    @PreAuthorize("hasRole('GERENTE')")
    public String cancelarPedido(@PathVariable String id, @RequestParam String justificativa,
                                 RedirectAttributes redirect) {
        return executar(() -> pedidoService.cancelar(id, justificativa),
                "Pedido cancelado e auditado.", "/pedidos", redirect);
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public String auditoria(Model model) {
        model.addAttribute("registros", auditoriaService.listar());
        model.addAttribute("content", "auditoria :: content");
        return "layout";
    }

    private List<ItemPedidoRequest> converterItens(List<String> produtoIds, List<String> quantidades) {
        List<ItemPedidoRequest> itens = new ArrayList<>();
        int limite = Math.min(produtoIds.size(), quantidades.size());
        for (int i = 0; i < limite; i++) {
            String produtoId = produtoIds.get(i);
            String quantidade = quantidades.get(i);
            if (produtoId != null && !produtoId.isBlank() && quantidade != null && !quantidade.isBlank()) {
                try {
                    itens.add(new ItemPedidoRequest(produtoId, Integer.parseInt(quantidade)));
                } catch (NumberFormatException ex) {
                    throw new RegraNegocioException("Informe quantidades válidas para os itens.");
                }
            }
        }
        if (itens.isEmpty()) {
            throw new RegraNegocioException("Selecione ao menos um produto.");
        }
        return itens;
    }

    private String executar(Operacao operacao, String mensagem, String retorno,
                            RedirectAttributes redirect) {
        try {
            operacao.executar();
            redirect.addFlashAttribute("sucesso", mensagem);
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:" + retorno;
    }

    @FunctionalInterface
    private interface Operacao {
        Object executar();
    }
}
