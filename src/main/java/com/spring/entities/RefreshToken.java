package com.spring.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Builder(toBuilder = true)
@AllArgsConstructor
public class RefreshToken{
    @Id
    private String refreshToken;

    @Column(columnDefinition = "TINYINT")
    @JsonIgnore
    private Integer status;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column
    private Date createdDate;

    @Column
    private Date expirationDate;
}
