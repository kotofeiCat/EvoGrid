package com.EvoGrid.user.profile;

import com.EvoGrid.common.dto.PokemonSpeciesDto;
import com.EvoGrid.common.dto.UserProfileDto;
import com.EvoGrid.user.account.OwnedPokemon;
import com.EvoGrid.user.account.OwnedPokemonRepository;
import com.EvoGrid.user.account.UserAccount;
import com.EvoGrid.user.account.UserAccountRepository;
import com.EvoGrid.user.species.SpeciesLookup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final double DEFAULT_LAT = 55.7558;
    private static final double DEFAULT_LON = 37.6173;

    private final UserAccountRepository users;
    private final OwnedPokemonRepository pokemons;
    private final SpeciesLookup speciesLookup;
    private final PasswordEncoder passwordEncoder;
    private final PlayerSettings settings;

    @Transactional(readOnly = true)
    public boolean isEmailTaken(String email) {
        return users.existsByEmailIgnoreCase(normalize(email));
    }

    @Transactional
    public UserProfileDto register(String email, String password, Long starterSpeciesId, Double lat, Double lon) {
        String normalizedEmail = normalize(email);
        if (users.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Этот email уже зарегистрирован");
        }

        PokemonSpeciesDto starter = speciesLookup.getSpecies(starterSpeciesId);
        if (!starter.starter()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Этого эвона нельзя выбрать стартовым");
        }

        UserAccount user = users.save(new UserAccount(
                normalizedEmail, passwordEncoder.encode(password),
                lat != null ? lat : DEFAULT_LAT,
                lon != null ? lon : DEFAULT_LON));
        OwnedPokemon first = pokemons.save(OwnedPokemon.starter(user, starter));

        log.info("Новый игрок {} (id {}), стартер: {}", normalizedEmail, user.getId(), starter.name());
        return ProfileMapper.toProfile(user, List.of(first), settings.fullRegenTime());
    }

    @Transactional(readOnly = true)
    public UserProfileDto login(String email, String password) {
        UserAccount user = users.findByEmailIgnoreCase(normalize(email))
                .filter(u -> passwordEncoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> {
                    log.warn("Неудачный вход: {}", normalize(email));
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный email или пароль");
                });
        log.info("Вход игрока {} (id {})", user.getEmail(), user.getId());
        return ProfileMapper.toProfile(user, pokemons.findByUserIdOrderByIdAsc(user.getId()), settings.fullRegenTime());
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
