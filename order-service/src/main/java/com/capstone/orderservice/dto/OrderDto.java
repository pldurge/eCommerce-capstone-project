package com.capstone.orderservice.dto;

import com.capstone.orderservice.model.Order;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
public class OrderDto {

    private UUID id;
    private String orderNumber;
    private String userId;

    private List<OrderItemDto> items;

    private ShippingAddressDto shippingAddress;

    private BigDecimal totalAmount;
    private Order.OrderStatus  status;
    private Order.PaymentMethod paymentMethod;
    private String paymentId;

    private Date createdAt;
    private Date lastUpdatedAt;

    // ─── Nested shipping DTO ──────────────────────────────────────────────────
    @Data
    @Builder
    public static class ShippingAddressDto {
        private String street;
        private String city;
        private String state;
        private String zipCode;
        private String country;
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────
    public static OrderDto from(Order order) {
        ShippingAddressDto addressDto = null;
        if (order.getShippingAddress() != null) {
            Order.ShippingAddress a = order.getShippingAddress();
            addressDto = ShippingAddressDto.builder()
                    .street(a.getStreet())
                    .city(a.getCity())
                    .state(a.getState())
                    .zipCode(a.getZipCode())
                    .country(a.getCountry())
                    .build();
        }

        List<OrderItemDto> itemDtos = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                .map(OrderItemDto::from)
                .collect(Collectors.toList());

        return OrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .items(itemDtos)
                .shippingAddress(addressDto)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentId(order.getPaymentId())
                .createdAt(order.getCreatedAt())
                .lastUpdatedAt(order.getLastUpdatedAt())
                .build();
    }
}
