package com.shopnest.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // banyak address milik 1 profile; membuat kolom FK profile_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private UserProfile profile;

    // label bebas: "Rumah", "Kantor", dll.
    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String city;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;
}
