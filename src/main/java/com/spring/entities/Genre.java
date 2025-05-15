package com.spring.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "genres")
public class Genre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "genre_name")
    private String genresName;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(name = "brief_description", columnDefinition = "text")
    private String briefDescription;

    @Column(name = "full_description", columnDefinition = "text")
    private String fullDescription;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdDate;

    @LastModifiedDate
    @Column
    private Instant lastModifiedDate;

    @Column
    private Long createdBy;

    @Column
    private Long lastModifiedBy;

    @OneToMany(mappedBy = "genreSongId.genre", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GenreSong> genreSongs;
}
