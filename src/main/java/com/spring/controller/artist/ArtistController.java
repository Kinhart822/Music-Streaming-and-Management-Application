package com.spring.controller.artist;

import com.spring.dto.request.account.ArtistPresentation;
import com.spring.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/artist")
@RequiredArgsConstructor
public class ArtistController {
    private final AccountService accountService;

    @GetMapping("/profile")
    public ResponseEntity<ArtistPresentation> getArtist(){
        return ResponseEntity.ok(accountService.getArtist());
    }

}
