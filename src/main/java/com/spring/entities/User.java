package com.spring.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.spring.constants.CommonStatus;
import com.spring.constants.Gender;
import com.spring.constants.UserType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "users")
public class User implements Serializable, UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    @JsonIgnore
    private String password;

    @Column(name = "firstName")
    private String firstName;

    @Column(name = "lastName")
    private String lastName;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "dob")
    private LocalDate birthDay;

    @Column(name = "resetKey")
    @JsonIgnore
    private String resetKey; // OTP

    @Column(name = "resetDueDate")
    @JsonIgnore
    private Instant resetDueDate; // OTP due date

    @Column(name = "status")
    @JsonIgnore
    private Integer status;

    @Column(name = "user_type")
    @Enumerated(EnumType.STRING)
    private UserType userType;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "avatar", columnDefinition = "text")
    private String avatar;

    @Column
    private Long createdBy;

    @Column
    private Long lastModifiedBy;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdDate;

    @LastModifiedDate
    @Column
    private Instant lastModifiedDate;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserFile> userFiles;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistoryListen> historyListens;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefreshToken> refreshTokens;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications;

    @OneToMany(mappedBy = "artistUserFollowId.user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtistUserFollow> artistUserFollows;

    @OneToMany(mappedBy = "userSongLikeId.user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSongLike> userSongLikes;

    @OneToMany(mappedBy = "userSongDownloadId.user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSongDownload> userSongDownloads;

    @OneToMany(mappedBy = "userSongCountId.user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSongCount> userSongCounts;

    @OneToMany(mappedBy = "userPlaylistId.user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPlaylist> userPlaylists;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(userType.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
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
        return status == CommonStatus.ACTIVE.getStatus();
    }
}
