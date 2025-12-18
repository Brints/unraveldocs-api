package com.extractor.unraveldocs.user.model;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.auth.model.UserVerification;
import com.extractor.unraveldocs.documents.model.DocumentCollection;
import com.extractor.unraveldocs.loginattempts.model.LoginAttempts;
import com.extractor.unraveldocs.subscription.model.UserSubscription;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.*;

@Data
@Entity
@Table(name = "users", indexes = {
        @Index(columnList = "email", unique = true),
        @Index(columnList = "is_active"),
        @Index(columnList = "is_verified"),
        @Index(columnList = "is_platform_admin"),
        @Index(columnList = "is_organization_admin"),
        @Index(columnList = "role")
})
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "image_url")
    private String profilePicture;

    @Column(nullable = false, name = "first_name")
    private String firstName;

    @Column(nullable = false, name = "last_name")
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "last_login", columnDefinition = "TIMESTAMP")
    private OffsetDateTime lastLogin;

    @Column(nullable = false, name = "is_active")
    private boolean isActive = false;

    @Column(nullable = false, name = "is_verified")
    private boolean isVerified = false;

    @Column(nullable = false, name = "is_platform_admin")
    private boolean isPlatformAdmin = false;

    @Column(nullable = false, name = "is_organization_admin")
    private boolean isOrganizationAdmin = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'user'")
    private Role role = Role.USER;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "terms_accepted", nullable = false)
    private boolean termsAccepted = false;

    @Column(name = "marketing_opt_in", nullable = false)
    private boolean marketingOptIn = false;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "profession")
    private String profession;

    @Column(name = "organization")
    private String organization;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private LoginAttempts loginAttempts;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserVerification userVerification;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<DocumentCollection> documents;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserSubscription subscription;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private OffsetDateTime updatedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + getRole().name()));
    }

    @Override
    public String getPassword() {
        return password;
    }


    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.isActive && this.isVerified;
    }
}
