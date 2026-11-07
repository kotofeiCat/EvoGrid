package com.EvoGrid.user.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@DynamicUpdate
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccount {

    public static final int XP_PER_LEVEL = 100;
    public static final int SPHERES_PER_LEVEL = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private int level;
    private int experience;

    @Column(name = "pokeballs")
    private int pendingSpheres;

    private Integer spheresDelivered;
    private double latitude;
    private double longitude;

    @Column(columnDefinition = "integer default 1")
    private Integer activePartySlot;

    public UserAccount(String email, String passwordHash, double latitude, double longitude) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.level = 1;
        this.latitude = latitude;
        this.longitude = longitude;
        this.activePartySlot = 1;
    }

    public int getSpheresDelivered() {
        return spheresDelivered == null ? 0 : spheresDelivered;
    }

    public int getActivePartySlot() {
        return activePartySlot == null ? 1 : activePartySlot;
    }

    public void selectActiveSlot(int slot) {
        this.activePartySlot = slot;
    }

    public void moveTo(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void addExperience(int xp) {
        experience += xp;
        while (experience >= XP_PER_LEVEL) {
            experience -= XP_PER_LEVEL;
            level++;
            pendingSpheres += SPHERES_PER_LEVEL;
        }
    }

}
