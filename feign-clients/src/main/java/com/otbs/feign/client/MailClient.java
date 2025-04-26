package com.otbs.feign.client;

import com.otbs.feign.dto.MailRequest;
import com.otbs.feign.dto.MailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "mail-service", url = "http://localhost:8084")
public interface MailClient {

    @PostMapping("api/v1/mail/send")
    MailResponse sendMail(@RequestBody MailRequest mailRequest);
}