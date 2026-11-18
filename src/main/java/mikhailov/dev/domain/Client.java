package mikhailov.dev.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "clients")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone", nullable = false, length = 15)
    private String phone;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "client", fetch = FetchType.LAZY)
    private List<Account> accounts = new ArrayList<>();

    public Client (String fullName, String phone) {
        this.fullName = fullName;
        this.phone = phone;
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public static Client restore(Long id, String fullName,
                                 String phone, Instant createdAt) {
        Client client = new Client();

        client.id = id;
        client.fullName = fullName;
        client.phone = phone;
        client.createdAt = createdAt;

        return client;
    }
}