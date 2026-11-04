package com.EvoGrid.user.profile;

import com.EvoGrid.common.dto.BattleRewardRequest;
import com.EvoGrid.common.dto.ItemDto;
import com.EvoGrid.common.dto.UserProfileDto;
import com.EvoGrid.user.account.OwnedPokemon;
import com.EvoGrid.user.account.OwnedPokemonRepository;
import com.EvoGrid.user.account.UserAccount;
import com.EvoGrid.user.account.UserAccountRepository;
import com.EvoGrid.user.inventory.Inventory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.IntStream;

/** Профиль игрока: позиция, отряд, здоровье эвонов, награды за бои. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    public static final int PARTY_SIZE = 3;

    private final UserAccountRepository users;
    private final OwnedPokemonRepository pokemons;
    private final Inventory inventory;
    private final PlayerSettings settings;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Long userId) {
        return toProfile(findUser(userId));
    }

    @Transactional
    public UserProfileDto moveTo(Long userId, double lat, double lon) {
        UserAccount user = findUser(userId);
        user.moveTo(lat, lon);
        return toProfile(user);
    }

    @Transactional
    public UserProfileDto selectActiveSlot(Long userId, int slot) {
        UserAccount user = findUser(userId);
        user.selectActiveSlot(slot);
        return toProfile(user);
    }

    /** Ставит эвона в слот отряда; прежний обитатель слота уходит в коллекцию. */
    @Transactional
    public UserProfileDto assignToParty(Long userId, Long ownedPokemonId, int slot) {
        UserAccount user = findUser(userId);
        OwnedPokemon pokemon = findOwned(userId, ownedPokemonId);

        pokemons.findByUserIdAndPartySlot(userId, slot).ifPresent(current -> current.assignToSlot(null));
        pokemon.assignToSlot(slot);
        return toProfile(user);
    }

    /** Лечение или оживление вне боя предметом из сумки. */
    @Transactional
    public UserProfileDto useItem(Long userId, Long ownedPokemonId, String itemCode) {
        UserAccount user = findUser(userId);
        OwnedPokemon target = findOwned(userId, ownedPokemonId);
        ItemDto item = inventory.item(itemCode);

        Instant now = Instant.now();
        int hp = target.hpAt(now, settings.fullRegenTime());
        int newHp = ItemEffects.hpAfter(item, target.getName(), hp, target.getMaxHp());

        // Сначала проверили, что предмет подходит, и только потом списываем его
        inventory.consume(userId, UUID.randomUUID().toString(), itemCode);
        target.setHp(newHp, now);
        log.info("Игрок {} применил «{}» на {}: {} → {} HP", userId, item.name(), target.getName(), hp, newHp);
        return toProfile(user);
    }

    /** Итог боя от battle-service: опыт, здоровье отряда и пойманный эвон. */
    @Transactional
    public UserProfileDto applyBattleReward(Long userId, BattleRewardRequest reward) {
        UserAccount user = findUser(userId);
        user.addExperience(reward.experience());

        Instant now = Instant.now();
        if (reward.partyHp() != null) {
            reward.partyHp().forEach(fighter -> pokemons.findById(fighter.ownedPokemonId())
                    .filter(p -> p.getUser().getId().equals(userId))
                    .ifPresent(p -> p.setHp(fighter.hp(), now)));
        }

        BattleRewardRequest.CaughtPokemon caught = reward.caught();
        if (caught != null) {
            pokemons.save(new OwnedPokemon(user, caught.speciesId(), caught.name(), caught.element(), caught.archetype(),
                    caught.maxHp(), Math.max(1, caught.currentHp()), caught.attack(), caught.defense(), caught.speed(),
                    firstFreeSlot(userId)));
        }
        log.info("Итог боя для игрока {}: +{} XP, пойман: {}",
                userId, reward.experience(), caught == null ? "никто" : caught.name());
        return toProfile(user);
    }

    private Integer firstFreeSlot(Long userId) {
        return IntStream.rangeClosed(1, PARTY_SIZE)
                .filter(slot -> pokemons.findByUserIdAndPartySlot(userId, slot).isEmpty())
                .boxed()
                .findFirst()
                .orElse(null);
    }

    private OwnedPokemon findOwned(Long userId, Long ownedPokemonId) {
        return pokemons.findById(ownedPokemonId)
                .filter(p -> p.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Эвон не найден в вашей коллекции"));
    }

    private UserAccount findUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игрок не найден"));
    }

    private UserProfileDto toProfile(UserAccount user) {
        return ProfileMapper.toProfile(user, pokemons.findByUserIdOrderByIdAsc(user.getId()), settings.fullRegenTime());
    }
}
