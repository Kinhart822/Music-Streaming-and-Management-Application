package com.spring.entities;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @OneToMany(mappedBy = "genreSongId.genre", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GenreSong> genreSongs;

    @Column(name = "count_listen")
    private Long countListen;
}
