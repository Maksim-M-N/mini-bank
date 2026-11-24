package mikhailov.dev.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mikhailov.dev.domain.enums.AccountStatus;
import mikhailov.dev.domain.enums.CurrencyCode;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false, length = 34)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private CurrencyCode currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "fromAccount")
    private List<Transfer> transfersFrom = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY,mappedBy = "toAccount")
    private List<Transfer> transfersTo = new ArrayList<>();

    public Account(String accountNumber, CurrencyCode currencyCode, Client client) {
        this.accountNumber = accountNumber;
        this.currencyCode = currencyCode;
        this.client = client;
    }

    public List<Transfer> getTransfersFrom() {
        return Collections.unmodifiableList(transfersFrom);
    }

    public List<Transfer> getTransfersTo() {
        return Collections.unmodifiableList(transfersTo);
    }

    public void closeAccount() {
        if (status != AccountStatus.CLOSED) {
            if (balance.signum() != 0) {
                throw new IllegalStateException("Баланс должен быть равен 0");
            }
            this.status = AccountStatus.CLOSED;
            this.closedAt = Instant.now();
        }
    }

    public void changeBalance(BigDecimal newBalance) {
        if (newBalance == null) {
            throw new IllegalArgumentException("Новый баланс счёта не может быть null.");
        }

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Новый баланс счёта не может быть отрицательным.");
        }

        this.balance = newBalance;
    }

    public static Account restore(Long id, String accountNumber,
                                  CurrencyCode currencyCode, AccountStatus status,
                                  BigDecimal balance, Instant openedAt,
                                  Instant closedAt, Client client) {
        Account account = new Account();

        account.id = id;
        account.accountNumber = accountNumber;
        account.currencyCode = currencyCode;
        account.status = status;
        account.balance = balance;
        account.openedAt = openedAt;
        account.closedAt = closedAt;
        account.client = client;
        return account;
    }
}