package kz.spt.lib.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users", schema = "crm")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    @Email(message = "Please provide a valid e-mail")
    @NotEmpty(message = "Please provide an e-mail")
    private String email;

    private String firstName;

    private String lastName;

    private String patronymic;

    private String password;

    private int enabled;

    @Column(name = "main_server_port")
    private String mainServerPort;

    @Column(name = "online_server_port")
    private String onlineServerIpPort;

    @Column(name = "parking_server_port")
    private String parkingServerIpPort;

    @Column(name = "telegram_channel")
    private String telegramChannel;

    @Column(name = "web_kassa_id")
    private String webKassaId;

    @Column(name = "web_kassa_login")
    private String webKassaLogin;

    @Column(name = "web_kassa_password")
    private String webKassaPassword;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "user_role", schema = "crm",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private List<Role> roles;

    public int getColumnCount() {
        return getClass().getDeclaredFields().length;
    }

    public String getName() {
        return firstName + " " + lastName;
    }

}
