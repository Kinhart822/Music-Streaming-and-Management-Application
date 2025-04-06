package com.spring.service.impl;

import com.spring.dto.request.music.artist.SongUploadRequest;
import com.spring.dto.response.ApiResponse;
import com.spring.entities.Artist;
import com.spring.entities.ArtistSong;
import com.spring.entities.ArtistSongId;
import com.spring.entities.Song;
import com.spring.repository.ArtistRepository;
import com.spring.repository.ArtistSongRepository;
import com.spring.repository.SongRepository;
import com.spring.service.SongUploadService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SongUploadServiceImpl implements SongUploadService {
    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;
    private final ArtistSongRepository artistSongRepository;

    @Value("${file.upload-dir}")
    private String uploadDirPath;

    private Path uploadDir;

    @PostConstruct
    public void init() throws IOException {
        uploadDir = Paths.get(uploadDirPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
    }

    @Override
    public ApiResponse uploadSong(SongUploadRequest songUploadRequest) {
        try {
            MultipartFile file = songUploadRequest.getFile();
            String title = songUploadRequest.getTitle();
            Long artistId = songUploadRequest.getArtistId();

            if (file.isEmpty()) {
                return ApiResponse.error("File is empty");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".mp3")) {
                return ApiResponse.error("Only .mp3 files are allowed");
            }

            String fileName = UUID.randomUUID() + "_" + originalFilename;
            Path targetPath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Create and save song
            Song song = Song.builder()
                    .title(title)
                    .releaseDate(Date.from(Instant.now()))
                    .countListen(0L)
                    .mediaUrl(fileName)
                    .build();

            songRepository.save(song);

            // Link with artist
            Artist artist = artistRepository.findById(artistId)
                    .orElseThrow(() -> new RuntimeException("Artist not found with ID: " + artistId));

            ArtistSongId artistSongId = new ArtistSongId();
            artistSongId.setArtist(artist);
            artistSongId.setSong(song);

            ArtistSong artistSong = new ArtistSong();
            artistSong.setArtistSongId(artistSongId);

            artistSongRepository.save(artistSong);

            return ApiResponse.ok("Upload successful! File saved at: " + targetPath.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Upload failed: " + e.getMessage());
        }
    }
}
