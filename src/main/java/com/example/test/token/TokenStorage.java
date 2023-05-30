package com.example.test.token;

import lombok.Data;

@Data
public class TokenStorage {
    private static final TokenStorage instance = new TokenStorage();

    public static TokenStorage getInstance() {
        return instance;
    }

    private TokenStorage() {

    }

    private String token;
}
