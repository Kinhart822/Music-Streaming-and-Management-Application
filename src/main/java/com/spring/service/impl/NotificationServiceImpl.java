package com.spring.service.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.spring.constants.ApiResponseCode;
import com.spring.constants.TargetNotifications;
import com.spring.dto.request.ArtistNotificationsDto;
import com.spring.dto.request.UserNotificationsDto;
import com.spring.entities.Artist;
import com.spring.entities.User;
import com.spring.entities.NotificationToken;
import com.spring.exceptions.BusinessException;
import com.spring.repository.ArtistRepository;
import com.spring.repository.NotificationRepository;
import com.spring.repository.NotificationTokenRepository;
import com.spring.repository.UserRepository;
import com.spring.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final ArtistRepository artistRepository;
    private final UserRepository userRepository;
    private final NotificationTokenRepository notificationTokenRepository;
    private final FirebaseMessaging firebaseMessaging;

    @Override
    public void notifyArtistSongAccepted(Long artistId, String songTitle) {
        String title = "Song Accepted";
        String content = String.format("Your song '%s' was accepted!", songTitle);

        sendArtistNotification(artistId, title, content);
    }

    @Override
    public void notifyArtistSongRejected(Long artistId, String songTitle, String reason) {
        String title = "Song Rejected";
        String content = String.format("Your song '%s' was rejected. Reason: %s", songTitle, reason);

        sendArtistNotification(artistId, title, content);
    }

    @Override
    public void notifyArtistSongDeclined(Long artistId, String songTitle) {
        String title = "Song Declined";
        String content = String.format("Your song '%s' was declined. If you have any questions, please send an email to the platform email!", songTitle);

        sendArtistNotification(artistId, title, content);
    }

    @Override
    public void notifyArtistPlaylistAccepted(Long artistId, String playlistTitle) {
        String title = "Playlist Accepted";
        String content = String.format("Your playlist '%s' was accepted!", playlistTitle);

        sendArtistNotification(artistId, title, content);
    }

    @Override
    public void notifyArtistPlaylistDeclined(Long artistId, String playlistTitle) {
        String title = "Playlist Declined";
        String content = String.format("Your playlist '%s' was declined. If you have any questions, please send an email to the platform email!", playlistTitle);

        sendArtistNotification(artistId, title, content);
    }

    @Override
    public void notifyArtistAlbumAccepted(Long artistId, String albumTitle) {
        String title = "Album Accepted";
        String content = String.format("Your album '%s' was accepted!", albumTitle);

        sendArtistNotification(artistId, title, content);
    }

    @Override
    public void notifyArtistAlbumDeclined(Long artistId, String albumTitle) {
        String title = "Album Declined";
        String content = String.format("Your album '%s' was declined. If you have any questions, please send an email to the platform email!", albumTitle);

        sendArtistNotification(artistId, title, content);
    }

    @Override
    public void notifyArtistSongMilestone(Long artistId, String songTitle, Long streams, String milestoneType) {
        String title = "Song Milestone Achieved";
        String content = String.format("Congratulations! Your song '%s' has reached %d %s!", songTitle, streams, milestoneType);

        sendArtistNotification(artistId, title, content);
    }

    @Override
    public void notifyUserNewSong(Long userId, Long artistId, String songTitle) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        String title = "New Song from Followed Artist";
        String content = String.format("A new song '%s' is from '%s'!", songTitle, artist.getArtistName());

        sendUserNotification(userId, artistId, title, content);
    }

    @Override
    public void notifyUserNewPlaylist(Long userId, Long artistId, String playlistTitle) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        String title = "New Playlist from Followed Artist";
        String content = String.format("A new playlist '%s' is from '%s'!", playlistTitle, artist.getArtistName());

        sendUserNotification(userId, artistId, title, content);
    }

    @Override
    public void notifyUserNewAlbum(Long userId, Long artistId, String albumTitle) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
        String title = "New Album from Followed Artist";
        String content = String.format("A new album '%s' is from '%s'!", albumTitle, artist.getArtistName());

        sendUserNotification(userId, artistId, title, content);
    }

    private void sendArtistNotification(Long artistId, String title, String content) {
        try {
            Artist artist = artistRepository.findById(artistId)
                    .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
            NotificationToken notificationToken = notificationTokenRepository.findByUserId(artistId);

            // Save to the database
            com.spring.entities.Notification notification = com.spring.entities.Notification.builder()
                    .title(title)
                    .content(content)
                    .targetNotifications(TargetNotifications.ARTIST_ONLY)
                    .user(artist)
                    .createdDate(Instant.now())
                    .build();

            notificationRepository.save(notification);

            // Create DTO for Firebase
            ArtistNotificationsDto artistDto = new ArtistNotificationsDto();
            artistDto.setNotificationId(notification.getId());
            artistDto.setTargetArtistId(artistId);

            // Send via Firebase
            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(content)
                            .build())
                    .setToken(notificationToken.getDeviceToken())
                    .build();

            firebaseMessaging.send(message);

            log.info("Sent notification to artist {}: {}", artistId, content);
        } catch (Exception e) {
            log.error("Error sending artist notification: {}", e.getMessage());
        }
    }

    private void sendUserNotification(Long userId, Long artistId, String title, String content) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));
            NotificationToken notificationToken = notificationTokenRepository.findByUserId(userId);

            // Save to the database
            com.spring.entities.Notification notification = com.spring.entities.Notification.builder()
                    .title(title)
                    .content(content)
                    .targetNotifications(TargetNotifications.USER_ONLY)
                    .user(user)
                    .createdDate(Instant.now())
                    .build();

            notificationRepository.save(notification);

            // Create DTO for Firebase
            UserNotificationsDto userDto = new UserNotificationsDto();
            userDto.setNotificationId(notification.getId());
            userDto.setFollowedArtistId(artistId);
            userDto.setTargetUserId(userId);

            // Send via Firebase
            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(content)
                            .build())
                    .setToken(notificationToken.getDeviceToken())
                    .build();

            firebaseMessaging.send(message);

            log.info("Sent notification to user {}: {}", userId, content);
        } catch (Exception e) {
            log.error("Error sending user notification: {}", e.getMessage());
        }
    }
}