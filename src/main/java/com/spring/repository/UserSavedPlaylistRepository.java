package com.spring.repository;

import com.spring.entities.UserSavedPlaylist;
import com.spring.entities.UserSavedPlaylistId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSavedPlaylistRepository extends JpaRepository<UserSavedPlaylist, UserSavedPlaylistId> {

}
