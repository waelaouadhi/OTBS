package com.otbs.employee.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.otbs.employee.util.LdapNameSerializer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import javax.naming.Name;
import java.util.Set;

@Entry(objectClasses = {"top", "person", "organizationalPerson", "user"}, base = "dc=otbs,dc=local")
@Getter
@Setter
@ToString
public class LdapUser {
    @Id
    @JsonSerialize(using = LdapNameSerializer.class)
    private String dn;

    @Attribute(name = "cn")
    private String commonName;

    @Attribute(name = "sAMAccountName")
    private String username;

    @Attribute(name = "mail")
    private String email;

    @Attribute(name = "telephoneNumber")
    private String phoneNumber;

    @Attribute(name = "memberOf")
    private Set<String> groups;

    @Attribute(name = "userAccountControl")
    private Integer accountStatus;

    @Attribute(name = "givenName")
    private String firstName;

    @Attribute(name = "sn")
    private String lastName;
}