package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.dto.DashboardResponse;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.DashboardService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardApiController.class)
@Import(SecurityConfig.class)
class DashboardApiControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean DashboardService dashboardService;
    @MockBean UsuarioDetailsService usuarioDetailsService;

    @Test
    void gerenteConsultaDashboardComFiltros() throws Exception {
        LocalDate inicio = LocalDate.parse("2026-11-01");
        LocalDate fim = LocalDate.parse("2026-11-10");
        when(dashboardService.consultar(inicio, fim, StatusPedido.FINALIZADO))
                .thenReturn(new DashboardResponse(inicio, fim, StatusPedido.FINALIZADO,
                        2, 2, new BigDecimal("125.00"), new BigDecimal("62.50"),
                        List.of(), List.of(), List.of()));

        mockMvc.perform(get("/api/dashboard")
                        .param("dataInicio", "2026-11-01")
                        .param("dataFim", "2026-11-10")
                        .param("status", "FINALIZADO")
                        .with(user("gerente").roles("GERENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(2))
                .andExpect(jsonPath("$.totalVendas").value(125.00));

        verify(dashboardService).consultar(inicio, fim, StatusPedido.FINALIZADO);
    }

    @Test
    void atendenteNaoPodeConsultarDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(user("atendente").roles("ATENDENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejeitaFormatoDeDataInvalido() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .param("dataInicio", "ontem")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Parâmetro inválido: dataInicio."));
    }
}
