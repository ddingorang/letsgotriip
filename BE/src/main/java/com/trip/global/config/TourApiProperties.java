// Created: 2026-06-08 15:32:20
package com.trip.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tour.api")
@Getter
@Setter
public class TourApiProperties {
    private String baseUrl;
    private String key;
}
