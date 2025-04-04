package com.spring.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.spring.constants.Gender;
import com.spring.constants.UserType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class User implements Serializable, UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    @JsonIgnore
    private String password;

    @Column(name = "firstName")
    private String firstName;

    @Column(name = "lastName")
    private String lastName;

    @Column(name = "gender")
    @Enumerated(EnumType.ORDINAL)
    private Gender gender;

    @Column(name = "dob")
    private Instant birthDay;

    @Column
    @JsonIgnore
    private String resetKey; // OTP

    @Column
    @JsonIgnore
    private Instant resetDueDate; // OTP due date

    @Column(columnDefinition = "TINYINT")
    @JsonIgnore
    private Integer status;

    @Column(name = "user_type")
    @Enumerated(EnumType.ORDINAL)
    private UserType userType;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "avatar")
    private String avatar;

    @Column(updatable = false)
    private Long createdBy;

    @Column
    private Long lastModifiedBy;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdDate;

    @LastModifiedDate
    @Column
    private Instant lastModifiedDate;

    @OneToMany(mappedBy = "users", fetch = FetchType.LAZY)
    private List<UserFile> userFiles;

    @OneToMany(mappedBy = "users", fetch = FetchType.LAZY)
    private List<HistoryListen> historyListens;

    @OneToMany(mappedBy = "users", fetch = FetchType.LAZY)
    private List<Playlist> playlists;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

//    @Override
//    public boolean isAccountNonExpired() {
//        return true;
//    }
//
//    @Override
//    public boolean isAccountNonLocked() {
//        return true;
//    }
//
//    @Override
//    public boolean isCredentialsNonExpired() {
//        return true;
//    }
//
//    @Override
//    public boolean isEnabled() {
//        return true;
//    }
}
