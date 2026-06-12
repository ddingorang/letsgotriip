package com.trip.global.security.oauth2;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class OAuth2Attribute {

    private Map<String, Object> attributes;
    private String attributeKey;
    private String email;
    private String name;
    private String picture;
    private String oauthType;
    private String oauthKey;

    public static OAuth2Attribute of(String provider, String attributeKey, Map<String, Object> attributes) {
        switch (provider) {
            case "google":
                return ofGoogle(attributeKey, attributes);
            default:
                throw new RuntimeException("Unsupported OAuth provider: " + provider);
        }
    }

    private static OAuth2Attribute ofGoogle(String attributeKey, Map<String, Object> attributes) {
        return OAuth2Attribute.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .picture((String) attributes.get("picture"))
                .attributes(attributes)
                .attributeKey(attributeKey)
                .oauthType("GOOGLE")
                .oauthKey((String) attributes.get(attributeKey))
                .build();
    }

    public Map<String, Object> convertToMap() {
        return Map.of(
                "id", attributeKey,
                "key", attributeKey,
                "name", name,
                "email", email,
                "picture", picture,
                "oauthType", oauthType,
                "oauthKey", oauthKey
        );
    }
}
