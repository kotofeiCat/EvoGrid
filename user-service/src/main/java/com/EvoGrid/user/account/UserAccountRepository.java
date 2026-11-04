package com.EvoGrid.user.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<UserAccount> findTop50ByPendingSpheresGreaterThan(int amount);

    /**
     * Отмечает доставку сфер одним атомарным UPDATE: если за это время игрок получил новый уровень,
     * его новые сферы не потеряются — вычитается ровно доставленное количество.
     */
    @Modifying
    @Query("""
            update UserAccount u
               set u.pendingSpheres = u.pendingSpheres - :amount,
                   u.spheresDelivered = coalesce(u.spheresDelivered, 0) + :amount
             where u.id = :userId and coalesce(u.spheresDelivered, 0) = :deliveredBefore""")
    int markSpheresDelivered(@Param("userId") Long userId, @Param("amount") int amount,
                             @Param("deliveredBefore") int deliveredBefore);
}
