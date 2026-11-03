package com.EvoGrid.user.service;

import com.EvoGrid.user.client.PokemonClient;
import com.EvoGrid.user.dto.*;
import com.EvoGrid.user.model.OwnedPokemon;
import com.EvoGrid.user.model.UserAccount;
import com.EvoGrid.user.repository.OwnedPokemonRepository;
import com.EvoGrid.user.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserAccountRepository userRepository;
    private final OwnedPokemonRepository pokemonRepository;
    private final PokemonClient pokemonClient;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserAccountRepository userRepository,
                       OwnedPokemonRepository pokemonRepository,
                       PokemonClient pokemonClient,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.pokemonRepository = pokemonRepository;
        this.pokemonClient = pokemonClient;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean isEmailTaken(String email) {
        return email != null && !email.isBlank()
                && userRepository.findByEmailIgnoreCase(email.trim()).isPresent();
    }

    @Transactional
    public UserProfileDto register(RegisterRequest req, int activeSlot) {
        if (req.email() == null || !EMAIL_PATTERN.matcher(req.email().trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный формат email");
        }
        if (req.password() == null || req.password().length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пароль должен быть не менее 4 символов");
        }
        String normalizedEmail = req.email().trim().toLowerCase();
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email уже занят");
        }

        double lat = req.latitude() != null ? req.latitude() : 55.7558;
        double lon = req.longitude() != null ? req.longitude() : 37.6173;

        UserAccount user = userRepository.save(
                new UserAccount(normalizedEmail, passwordEncoder.encode(req.password()), lat, lon)
        );

        Long starterId = (req.starterSpeciesId() != null && List.of(2L, 6L, 10L).contains(req.starterSpeciesId()))
                ? req.starterSpeciesId() : 2L;
        PokemonSpeciesDto starter = fetchStarter(starterId);

        pokemonRepository.save(new OwnedPokemon(
                user, starter.id(), starter.name(), starter.element(), starter.archetype(),
                starter.baseHp(), starter.baseAttack(), starter.baseDefense(), starter.baseSpeed(), 1
        ));

        return buildProfileDto(user, activeSlot);
    }

    public UserProfileDto login(LoginRequest req, int activeSlot) {
        if (req.email() == null || req.password() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Введите email и пароль");
        }
        UserAccount user = userRepository.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный email или пароль"));

        return buildProfileDto(user, activeSlot);
    }

    public UserProfileDto getUserProfile(Long userId, int activeSlot) {
        return buildProfileDto(findUserOrThrow(userId), activeSlot);
    }

    @Transactional
    public UserProfileDto updateLocation(Long userId, double lat, double lon, int activeSlot) {
        UserAccount user = findUserOrThrow(userId);
        user.setLatitude(lat);
        user.setLongitude(lon);
        return buildProfileDto(user, activeSlot);
    }

    @Transactional
    public UserProfileDto assignPartySlot(Long userId, PartySlotRequest req, int activeSlot) {
        if (req.slot() < 1 || req.slot() > 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Слот должен быть 1, 2 или 3");
        }
        UserAccount user = findUserOrThrow(userId);
        OwnedPokemon target = pokemonRepository.findById(req.ownedPokemonId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Покемон не найден"));

        if (!target.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Чужой покемон");
        }

        pokemonRepository.findByUserIdAndPartySlot(userId, req.slot())
                .ifPresent(occupant -> occupant.setPartySlot(null));

        target.setPartySlot(req.slot());
        return buildProfileDto(user, activeSlot);
    }

    @Transactional
    public UserProfileDto applyBattleReward(Long userId, BattleRewardRequest req) {
        UserAccount user = findUserOrThrow(userId);
        if (req.pokeballUsed() && user.getPokeballs() > 0) {
            user.setPokeballs(user.getPokeballs() - 1);
        }
        if (req.expGained() > 0) {
            user.addExperience(req.expGained());
        }
        if (req.captured()) {
            Integer freeSlot = findFirstFreePartySlot(userId);
            pokemonRepository.save(new OwnedPokemon(
                    user, req.speciesId(), req.name(), req.element(), req.archetype(),
                    req.maxHp(), req.attack(), req.defense(), req.speed(), freeSlot
            ));
        }
        return buildProfileDto(user, 1);
    }

    private PokemonSpeciesDto fetchStarter(Long speciesId) {
        try {
            return pokemonClient.getSpeciesById(speciesId);
        } catch (Exception t) {
            log.warn("pokemon-service недоступен ({}). Выдаём резервного стартера!", t.getMessage());
            return new PokemonSpeciesDto(2L, "Резервный Стартер", "FIRE", "Атака",
                    "Fallback-покемон", 70, 35, 20, 25, true);
        }
    }

    private Integer findFirstFreePartySlot(Long userId) {
        for (int slot = 1; slot <= 3; slot++) {
            if (pokemonRepository.findByUserIdAndPartySlot(userId, slot).isEmpty()) {
                return slot;
            }
        }
        return null;
    }

    private UserAccount findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден: " + userId));
    }

    private UserProfileDto buildProfileDto(UserAccount user, int activeSlot) {
        List<OwnedPokemonDto> collection = pokemonRepository.findByUserIdOrderByIdAsc(user.getId())
                .stream().map(this::toPokemonDto).toList();

        List<OwnedPokemonDto> party = collection.stream()
                .filter(p -> p.partySlot() != null)
                .sorted(Comparator.comparingInt(OwnedPokemonDto::partySlot))
                .toList();

        return new UserProfileDto(
                user.getId(), user.getEmail(), user.getLevel(), user.getExperience(),
                user.getPokeballs(), activeSlot, user.getLatitude(), user.getLongitude(),
                party, collection
        );
    }

    private OwnedPokemonDto toPokemonDto(OwnedPokemon p) {
        return new OwnedPokemonDto(
                p.getId(), p.getSpeciesId(), p.getName(), p.getElement(), p.getArchetype(),
                p.getMaxHp(), p.getAttack(), p.getDefense(), p.getSpeed(), p.getPartySlot()
        );
    }
}