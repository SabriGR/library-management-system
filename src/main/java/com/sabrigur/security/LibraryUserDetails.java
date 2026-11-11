package com.sabrigur.security;

import com.sabrigur.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Spring Security view of a {@link User}. It also carries the user id for ownership checks. */
public class LibraryUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final List<GrantedAuthority> authorities;

    public LibraryUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.active = user.isActive();
        // An administrator is a librarian, so he or she gets both roles (FR-003).
        this.authorities = "ADMIN".equals(user.getRole())
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_LIBRARIAN"))
                : List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    /** Deactivated accounts cannot log in. */
    @Override
    public boolean isEnabled() {
        return active;
    }
}
