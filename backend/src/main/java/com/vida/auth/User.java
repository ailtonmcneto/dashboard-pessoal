package com.vida.auth;

import com.vida.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String timezone;

    protected User() {
    }

    public User(String email, String name, String passwordHash, String timezone) {
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
        this.timezone = timezone;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getTimezone() {
        return timezone;
    }
}
