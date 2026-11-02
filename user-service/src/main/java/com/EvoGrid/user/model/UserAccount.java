package com.EvoGrid.user.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private int level;
    private int experience;
    private int pokeballs;
    private double latitude;
    private double longitude;

    public UserAccount() {}

    public UserAccount(String email, String passwordHash, double latitude, double longitude) {
        this.email = email.toLowerCase().trim();
        this.passwordHash = passwordHash;
        this.level = 1;
        this.experience = 0;
        this.pokeballs = 15;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void addExperience(int xp) {
        this.experience += xp;
        while (this.experience >= 100) {
            this.experience -= 100;
            this.level++;
            this.pokeballs += 3;
        }
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getPokeballs() { return pokeballs; }
    public void setPokeballs(int pokeballs) { this.pokeballs = pokeballs; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}