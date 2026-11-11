package com.sabrigur.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Abstract base class of all system users (see class diagram).
 * Members, librarians and administrators are stored in one table.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    private String phoneNumber;

    @Column(nullable = false)
    private String passwordHash;

    @Builder.Default
    private boolean active = true;

    /** Role name without the ROLE_ prefix: MEMBER, LIBRARIAN or ADMIN. */
    public abstract String getRole();

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
