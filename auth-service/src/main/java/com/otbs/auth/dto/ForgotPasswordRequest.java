package com.otbs.auth.dto;

import lombok.Data;
import lombok.Getter;


public record ForgotPasswordRequest(String email) {
}
