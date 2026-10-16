package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PedidoController.class)
@Import(SecurityConfig.class)
class AutorizacaoApiTest {

    @Autowired MockMvc mockMvc;
    @MockBean PedidoService pedidoService;
    @MockBean UsuarioDetailsService usuarioDetailsService;

    @Test
    void exigeAutenticacaoParaConsultarPedidos() throws Exception {
        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void adminNaoPodeAplicarDesconto() throws Exception {
        mockMvc.perform(post("/api/pedidos/1/desconto")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"percentual\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void gerentePodeAplicarDesconto() throws Exception {
        Pedido pedido = new Pedido();
        pedido.setId("1");
        pedido.setStatus(StatusPedido.RECEBIDO);
        when(pedidoService.aplicarDesconto("1", new BigDecimal("10"))).thenReturn(pedido);

        mockMvc.perform(post("/api/pedidos/1/desconto")
                        .with(user("gerente").roles("GERENTE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"percentual\":10}"))
                .andExpect(status().isOk());
    }

    @Test
    void csrfProtegeCriacaoDePedido() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .with(user("atendente").roles("ATENDENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":\"c1\",\"itens\":[{\"produtoId\":\"p1\",\"quantidade\":1}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminSoConsultaPedidos() throws Exception {
        when(pedidoService.listarPedidos()).thenReturn(List.of());
        mockMvc.perform(get("/api/pedidos").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/pedidos")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":\"c1\",\"itens\":[{\"produtoId\":\"p1\",\"quantidade\":1}]}"))
                .andExpect(status().isForbidden());
    }
}
