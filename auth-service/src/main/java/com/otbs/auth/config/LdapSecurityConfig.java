package com.otbs.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;

import java.util.Collections;

@Configuration
public class LdapSecurityConfig {

    @Value("${spring.ldap.urls}")
    private String ldapUrl;

    @Bean
    public AuthenticationManager authenticationManager() {
        ActiveDirectoryLdapAuthenticationProvider adProvider =
                new ActiveDirectoryLdapAuthenticationProvider("otbs.local", ldapUrl);
        adProvider.setConvertSubErrorCodesToExceptions(true);
        adProvider.setUseAuthenticationRequestCredentials(true);
        return new ProviderManager(Collections.singletonList(adProvider));
    }
}