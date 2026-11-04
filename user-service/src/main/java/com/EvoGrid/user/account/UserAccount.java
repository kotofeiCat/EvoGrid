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

/** Аккаунт тренера. @DynamicUpdate — в UPDATE попадают только изменённые колонки. */
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

    /**
     * Сферы, заработанные за уровни, которые ещё не доставлены в inventory-service.
     * Колонка «pokeballs» осталась от старой версии: старые покеболы игроков доставляются так же.
     */
    @Column(name = "pokeballs")
    private int pendingSpheres;

    /**
     * Сколько сфер уже доставлено — из этого числа строится уникальный id операции доставки.
     * Integer без columnDefinition: колонка добавится в существующую таблицу как nullable,
     * а null читается как 0.
     */
    private Integer spheresDelivered;
    private double latitude;
    private double longitude;

    /** Каким слотом отряда игрок вступает в бой (1–3). */
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

    /** Начисляет опыт; каждые 100 XP — новый уровень и 3 сферы. */
    public void addExperience(int xp) {
        experience += xp;
        while (experience >= XP_PER_LEVEL) {
            experience -= XP_PER_LEVEL;
            level++;
            pendingSpheres += SPHERES_PER_LEVEL;
        }
    }

}
