package com.example.test.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegistrationParams {
    private String username;
    private String password;
}
