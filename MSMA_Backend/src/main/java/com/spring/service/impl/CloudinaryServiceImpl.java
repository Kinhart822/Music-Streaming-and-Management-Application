package com.spring.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.spring.dto.response.SongUploadResponse;
import com.spring.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {
    private final Cloudinary cloudinary;

    private String formatDuration(Double seconds) {
        if (seconds == null) return "00:00";
        int totalSeconds = (int) Math.round(seconds);
        int minutes = totalSeconds / 60;
        int remainingSeconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    @Override
    public String uploadImageToCloudinary(MultipartFile image) {
        try {
            if (image == null || image.isEmpty()) return null;

            List<String> allowedExtensions = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".tiff");
            String imageOriginalFilename = image.getOriginalFilename();

            boolean isValidImage = allowedExtensions.stream()
                    .anyMatch(ext -> imageOriginalFilename.toLowerCase().endsWith(ext));

            if (!isValidImage) {
                throw new IllegalArgumentException("Ảnh phải là định dạng hợp lệ");
            }

            String originalFilename = image.getOriginalFilename();
            Map uploadResult = cloudinary.uploader().upload(
                    image.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "public_id", "covers" + "/" + UUID.randomUUID() + "_" + originalFilename
                    )
            );
            return (String) uploadResult.get("secure_url");
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi upload ảnh: " + e.getMessage(), e);
        }
    }

    @Override
    public SongUploadResponse uploadAudioToCloudinary(MultipartFile file) {
        try {
            // Validate audio file
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File mp3 không được để trống!");
            }

            String originalFilename = file.getOriginalFilename();
            if (!originalFilename.toLowerCase().endsWith(".mp3")) {
                throw new IllegalArgumentException("File phải có định dạng .mp3!");
            }

            // Upload audio to Cloudinary
            Map<?, ?> audioUploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "video",
                            "public_id", "songs/" + UUID.randomUUID() + "_" + originalFilename
                    )
            );

            String audioUrl = (String) audioUploadResult.get("secure_url");
            Double durationSeconds = (Double) audioUploadResult.get("duration");
            String formattedDuration = formatDuration(durationSeconds);

            return SongUploadResponse.builder()
                    .mp3File(file)
                    .mp3Url(audioUrl)
                    .duration(formattedDuration)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi upload bài hát: " + e.getMessage(), e);
        }
    }
}
