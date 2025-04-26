package com.otbs.employee.dto;

import java.util.Base64;

public record ProfilePicture(String type, byte[] picture) {
    public String getBase64Image() {
        return "data:" + type + ";base64," + Base64.getEncoder().encodeToString(picture);
    }
}
