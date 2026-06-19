package com.trip.group.dto;

public record GroupDiscountResponse(
        String title,
        String description,
        String partner,
        int discountRate,
        boolean demo
) {}
