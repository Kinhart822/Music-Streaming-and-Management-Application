package com.spring.repository;

import com.spring.entities.UserSavedAlbum;
import com.spring.entities.UserSavedAlbumId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSavedAlbumRepository extends JpaRepository<UserSavedAlbum, UserSavedAlbumId> {

}
