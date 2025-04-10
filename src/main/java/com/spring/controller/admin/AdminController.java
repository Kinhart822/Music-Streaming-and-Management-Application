package com.spring.controller.admin;

import com.spring.dto.request.account.AdminPresentation;
import com.spring.dto.request.account.CreateArtist;
import com.spring.dto.request.account.CreateArtistFromList;
import com.spring.dto.response.ApiResponse;
import com.spring.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AccountService accountService;

    /*
    TODO: ARTIST specific
    */
    @PostMapping("/createArtist")
    public ResponseEntity<ApiResponse> createArtist(@RequestBody @Valid CreateArtist request) {
        return ResponseEntity.ok(accountService.createArtist(request));
    }

    @PostMapping("/createArtist/batch")
    public ResponseEntity<ApiResponse> createArtistFromList(@RequestBody @Valid CreateArtistFromList request) {
        return ResponseEntity.ok(accountService.createArtistFromList(request));
    }

    @GetMapping("/countArtist")
    public ResponseEntity<Long> getTotalArtists() {
        return ResponseEntity.ok(accountService.countArtists());
    }

    @GetMapping("/countUser")
    public ResponseEntity<Long> getTotalUser() {
        return ResponseEntity.ok(accountService.countUsers());
    }
}
