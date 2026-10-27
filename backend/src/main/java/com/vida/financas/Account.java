package com.vida.financas;

import com.vida.common.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** Conta corrente, poupança, carteira… O saldo é calculado a partir dos lançamentos. */
@Entity
@Table(name = "accounts")
public class Account extends OwnedEntity {

    public enum Kind { CORRENTE, POUPANCA, CARTEIRA, INVESTIMENTO }

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    @Column(nullable = false)
    private BigDecimal openingBalance;

    @Column(nullable = false)
    private boolean archived;

    protected Account() {
    }

    Account(UUID userId) {
        super(userId);
    }

    void update(String name, Kind kind, BigDecimal openingBalance, boolean archived) {
        this.name = name;
        this.kind = kind;
        this.openingBalance = openingBalance;
        this.archived = archived;
    }

    public String getName() {
        return name;
    }

    public Kind getKind() {
        return kind;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public boolean isArchived() {
        return archived;
    }
}
