package com.duewise.client;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.duewise.common.web.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClientController.class)
class ClientControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ClientService service;

    @Test
    void invalidClientReturnsEveryFieldError() throws Exception {
        mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " ", "email": "not-an-email", "paymentTermsDays": 400,
                                 "address": {"countryCode": "india"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field",
                        hasItems("name", "email", "paymentTermsDays", "address.countryCode")));
    }

    @Test
    void pageSizeAboveLimitIsRejected() throws Exception {
        mvc.perform(get("/api/v1/clients").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    @Test
    void unknownSortFieldIsRejected() throws Exception {
        mvc.perform(get("/api/v1/clients").param("sort", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("sort"));
    }

    @Test
    void listPassesPagingAndSortingToTheService() throws Exception {
        when(service.search(any(), any())).thenReturn(new PageResponse<>(List.of(), 1, 10, 0, 0));

        mvc.perform(get("/api/v1/clients")
                        .param("q", "acme")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "createdAt")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10));

        verify(service).search(eq("acme"), eq(PageRequest.of(1, 10,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")))));
    }
}
