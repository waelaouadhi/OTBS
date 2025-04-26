package com.otbs.auth.model;

import lombok.Data;
import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import javax.naming.Name;

@Entry(objectClasses = {"top", "person", "organizationalPerson", "user"}, base = "dc=otbs,dc=local")
@Data
public class LdapUser {
    @Id
    private Name dn;

    @Attribute(name = "cn")
    private String fullName;

    @Attribute(name = "sAMAccountName")
    private String username;

    @Attribute(name = "mail")
    private String email;

    @Attribute(name = "distinguishedName")
    private String distinguishedName;

    @Attribute(name = "ou")
    private String organizationalUnit;

    @Attribute(name = "userPassword")
    private byte[] password;
}