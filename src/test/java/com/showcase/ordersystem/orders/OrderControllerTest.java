package com.showcase.ordersystem.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showcase.ordersystem.infrastructure.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void shouldCreateOrderWhenRequestIsValid() throws Exception {
        OrderService.CreateOrderRequest request = new OrderService.CreateOrderRequest(
                "CUST-1", "customer@test.com",
                List.of(new OrderService.CreateOrderRequest.OrderItemRequest(
                        "P1", "Product 1", 2, new BigDecimal("50.00")))
        );

        when(orderService.createOrder(any())).thenReturn("order-123");

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-123"));
    }

    @Test
    void shouldReturnBadRequestWhenEmailIsInvalid() throws Exception {
        OrderService.CreateOrderRequest request = new OrderService.CreateOrderRequest(
                "CUST-1", "invalid-email",
                List.of(new OrderService.CreateOrderRequest.OrderItemRequest(
                        "P1", "Product 1", 2, new BigDecimal("50.00")))
        );

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void shouldReturnPaginatedOrders() throws Exception {
        OrderService.OrderInfo info = new OrderService.OrderInfo("order-1", "CUST-1", new BigDecimal("100.00"), "COMPLETED", Instant.now());
        org.springframework.data.domain.Page<OrderService.OrderInfo> page = new PageImpl<>(List.of(info));

        when(orderService.getOrdersByCustomer(eq("CUST-1"), any())).thenReturn(page);

        mockMvc.perform(get("/api/orders/customer/CUST-1")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value("order-1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        when(orderService.getOrderById("none")).thenThrow(new IllegalArgumentException("Order not found: none"));

        mockMvc.perform(get("/api/orders/none"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.detail").value("Order not found: none"));
    }
}
