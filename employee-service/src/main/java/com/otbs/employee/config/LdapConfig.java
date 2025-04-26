package com.otbs.employee.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

@Configuration
public class LdapConfig {

    @Value("${spring.ldap.urls}")
    private String ldapUrl;

    @Value("${spring.ldap.username}")
    private String managerDn;

    @Value("${spring.ldap.password}")
    private String managerPassword;

    @Value("${spring.ldap.base}")
    private String baseDn;

    @Bean
    public LdapContextSource ldapContextSource() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapUrl);
        contextSource.setBase(baseDn);
        contextSource.setUserDn(managerDn);
        contextSource.setPassword(managerPassword);
        return contextSource;
    }

    @Bean
    @Primary
    public LdapTemplate ldapTemplate(LdapContextSource ldapContextSource) {
        LdapTemplate template = new LdapTemplate(ldapContextSource);
        template.setIgnorePartialResultException(true);
        return template;
    }
}
