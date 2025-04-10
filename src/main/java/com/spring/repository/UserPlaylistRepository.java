package com.spring.repository;

import com.spring.entities.UserPlaylist;
import com.spring.entities.UserPlaylistId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPlaylistRepository extends JpaRepository<UserPlaylist, UserPlaylistId> {

}
