let currentUser = null;
let speciesCatalog = [];
let map = null;
let playerMarker = null;
let radarCircle = null;
const spawnMarkersMap = new Map();
let selectedWildSpawn = null;
let currentBattle = null;

// Переменные плавной анимации маркера и фильтрации GPS
let animFrameId = null;
let renderedLat = 55.7558;
let renderedLon = 37.6173;
let lastServerSyncLat = 0;
let lastServerSyncLon = 0;
let lastServerSyncTime = 0;
let lastGpsFixTime = 0;
let watchId = null;

window.addEventListener('DOMContentLoaded', async () => {
    document.getElementById('dock-mount').innerHTML = UiComponents.renderBottomDock('map');

    // 1. ПРОВЕРКА АВТОРИЗАЦИИ: только при 401 Unauthorized выкидываем на index.html
    try {
        currentUser = await ApiService.getCurrentUser();
        updateActiveFighterLabel();
    } catch (authErr) {
        console.warn('Требуется авторизация:', authErr);
        window.location.href = 'index.html';
        return;
    }

    const startLat = currentUser.latitude || 55.7558;
    const startLon = currentUser.longitude || 37.6173;
    renderedLat = startLat;
    renderedLon = startLon;
    lastServerSyncLat = startLat;
    lastServerSyncLon = startLon;
    lastServerSyncTime = Date.now();
    lastGpsFixTime = Date.now();

    // 2. ЗАГРУЗКА СПРАВОЧНИКА ПОКЕМОНОВ
    try {
        speciesCatalog = await ApiService.getAllSpecies();
    } catch (e) {
        console.error('Не удалось загрузить каталог покемонов:', e);
        speciesCatalog = [];
    }

    // 3. ИНИЦИАЛИЗАЦИЯ КАРТЫ ЯНДЕКСА (API 2.1)
    try {
        await initYandexGameMap(startLat, startLon);
        await loadNearbySpawns(startLat, startLon);
        startLiveGpsTracking();
    } catch (mapErr) {
        console.error('Ошибка инициализации Яндекс Карт 2.1:', mapErr);
        UiComponents.showErrorAlert(
            'Ошибка загрузки карты',
            'Не удалось загрузить карту Яндекса. Проверьте ключ API или отключите блокировщик рекламы.'
        );
    }
});

function updateActiveFighterLabel() {
    const el = document.getElementById('active-fighter-indicator');
    if (!el || !currentUser) return;
    const slot = currentUser.activePartySlot || 1;
    const fighter = (currentUser.party || []).find(p => p.partySlot === slot) || (currentUser.party || [])[0];
    el.textContent = fighter
        ? `Боец: ${fighter.name} (#${fighter.partySlot}) • ${CATCH_RADIUS_METERS} м`
        : `Слот #${slot} • Радар ${CATCH_RADIUS_METERS} м`;
}

async function initYandexGameMap(lat, lon) {
    const ymaps = await loadYandexMapsScript();

    map = new ymaps.Map(
        'map',
        {
            center: [lat, lon],
            zoom: 17,
            controls: []
        },
        {
            suppressMapOpenBlock: true
        }
    );

    // 70-метровый круг обнаружения вокруг игрока
    radarCircle = new ymaps.Circle(
        [[lat, lon], CATCH_RADIUS_METERS],
        {},
        {
            fillColor: '#38bdf8',
            fillOpacity: 0.14,
            strokeColor: '#0284c7',
            strokeOpacity: 1,
            strokeWidth: 2,
            interactivityModel: 'default#transparent'
        }
    );
    map.geoObjects.add(radarCircle);

    // Кастомный маркер игрока (HTML из CSS)
    const TrainerLayout = ymaps.templateLayoutFactory.createClass(
        '<div class="ymap-trainer-pin-wrap"><div class="trainer-map-pin"></div></div>'
    );

    playerMarker = new ymaps.Placemark(
        [lat, lon],
        {},
        {
            iconLayout: TrainerLayout,
            interactivityModel: 'default#transparent',
            zIndex: 1000
        }
    );
    map.geoObjects.add(playerMarker);

    // Клик по карте для передвижения
    map.events.add('click', (e) => {
        const walkToggle = document.getElementById('click-walk-toggle');
        if (walkToggle && walkToggle.checked) {
            const coords = e.get('coords');
            if (Array.isArray(coords)) {
                moveTrainerTo(coords[0], coords[1], true);
            }
        }
    });
}

function animateMarkerTo(targetLat, targetLon, durationMs = 380) {
    if (!playerMarker || !radarCircle) return;
    if (animFrameId) cancelAnimationFrame(animFrameId);

    const startLat = renderedLat;
    const startLon = renderedLon;
    const startTime = performance.now();

    function step(now) {
        const progress = Math.min(1, (now - startTime) / durationMs);
        const eased = 1 - Math.pow(1 - progress, 3);

        renderedLat = startLat + (targetLat - startLat) * eased;
        renderedLon = startLon + (targetLon - startLon) * eased;

        playerMarker.geometry.setCoordinates([renderedLat, renderedLon]);
        radarCircle.geometry.setCoordinates([renderedLat, renderedLon]);

        if (progress < 1) {
            animFrameId = requestAnimationFrame(step);
        }
    }

    animFrameId = requestAnimationFrame(step);
}

function panMapTo(lat, lon, zoom = null, duration = 400) {
    if (!map) return;
    if (zoom !== null) {
        map.setCenter([lat, lon], Math.round(zoom), { duration });
    } else {
        map.panTo([lat, lon], { flying: false, duration });
    }
}

async function moveTrainerTo(rawLat, rawLon, isManualClick = false) {
    const prevLat = currentUser.latitude || rawLat;
    const prevLon = currentUser.longitude || rawLon;
    const rawDist = calculateDistanceMeters(prevLat, prevLon, rawLat, rawLon);

    if (!isManualClick && rawDist < 1.2) return;

    let nextLat = rawLat;
    let nextLon = rawLon;

    if (!isManualClick) {
        const now = Date.now();
        const dtSec = Math.max(0.5, (now - lastGpsFixTime) / 1000);
        const speedMps = rawDist / dtSec;

        if (rawDist > 120 && speedMps > 12) {
            nextLat = prevLat + (rawLat - prevLat) * 0.25;
            nextLon = prevLon + (rawLon - prevLon) * 0.25;
        } else {
            nextLat = prevLat + (rawLat - prevLat) * 0.65;
            nextLon = prevLon + (rawLon - prevLon) * 0.65;
        }
        lastGpsFixTime = now;
    }

    currentUser.latitude = nextLat;
    currentUser.longitude = nextLon;

    animateMarkerTo(nextLat, nextLon);
    panMapTo(nextLat, nextLon, null, 400);
    refreshSpawnDistances();

    const distFromLastSync = calculateDistanceMeters(lastServerSyncLat, lastServerSyncLon, nextLat, nextLon);
    const now = Date.now();

    if (isManualClick || distFromLastSync >= 8 || (now - lastServerSyncTime >= 6000)) {
        lastServerSyncLat = nextLat;
        lastServerSyncLon = nextLon;
        lastServerSyncTime = now;

        ApiService.updateLocation(currentUser.id, nextLat, nextLon).catch(() => {});
        await loadNearbySpawns(nextLat, nextLon);
    }
}

function startLiveGpsTracking() {
    if (!navigator.geolocation) return;
    if (watchId !== null) {
        navigator.geolocation.clearWatch(watchId);
    }
    watchId = navigator.geolocation.watchPosition(
        (pos) => {
            if (pos.coords.accuracy && pos.coords.accuracy > 65) {
                return;
            }
            moveTrainerTo(pos.coords.latitude, pos.coords.longitude, false);
        },
        () => {},
        {
            enableHighAccuracy: true,
            maximumAge: 2000,
            timeout: 8000
        }
    );
}

function centerOnTrainerGps() {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                moveTrainerTo(pos.coords.latitude, pos.coords.longitude, true);
                panMapTo(pos.coords.latitude, pos.coords.longitude, 17.8, 450);
            },
            () => {
                if (currentUser) {
                    panMapTo(currentUser.latitude, currentUser.longitude, 17.8, 450);
                }
            },
            { enableHighAccuracy: true, timeout: 6000 }
        );
    } else if (currentUser) {
        panMapTo(currentUser.latitude, currentUser.longitude, 17.8, 450);
    }
}

function buildSpawnBadgeHtml(sp, dist) {
    const inRange = dist <= CATCH_RADIUS_METERS;
    return `
        <div class="wild-map-badge ${inRange ? 'in-range' : ''}" data-elem="${sp.element}">
            ${renderGoogleIcon(sp.element)}
            <span class="spawn-name">${sp.name}</span>
            <span class="dist-label">${dist} м</span>
        </div>
    `;
}

function refreshSpawnDistances() {
    spawnMarkersMap.forEach((entry) => {
        const { marker, spawn } = entry;
        const dist = Math.round(
            calculateDistanceMeters(currentUser.latitude, currentUser.longitude, spawn.latitude, spawn.longitude)
        );
        if (marker) {
            marker.properties.set('badgeHtml', buildSpawnBadgeHtml(spawn, dist));
        }
    });
}

async function loadNearbySpawns(lat, lon) {
    if (!map || !window.ymaps) return;

    let spawns = [];
    try {
        spawns = await ApiService.getSpawnsNear(lat, lon);
    } catch {
        return;
    }

    const activeIds = new Set();

    spawns.forEach((sp) => {
        if (!sp.active) return;
        activeIds.add(sp.spawnId);

        const dist = Math.round(
            calculateDistanceMeters(currentUser.latitude, currentUser.longitude, sp.latitude, sp.longitude)
        );

        if (spawnMarkersMap.has(sp.spawnId)) {
            const existing = spawnMarkersMap.get(sp.spawnId);
            existing.spawn = sp;
            existing.marker.geometry.setCoordinates([sp.latitude, sp.longitude]);
            existing.marker.properties.set('badgeHtml', buildSpawnBadgeHtml(sp, dist));
        } else {
            const SpawnLayout = window.ymaps.templateLayoutFactory.createClass(
                '<div class="spawn-marker-wrapper">$[properties.badgeHtml]</div>'
            );

            const marker = new window.ymaps.Placemark(
                [sp.latitude, sp.longitude],
                {
                    badgeHtml: buildSpawnBadgeHtml(sp, dist)
                },
                {
                    iconLayout: SpawnLayout,
                    iconShape: {
                        type: 'Rectangle',
                        coordinates: [[-75, -22], [75, 22]]
                    },
                    zIndex: 500
                }
            );

            marker.events.add('click', (e) => {
                e.stopPropagation();
                const latest = spawnMarkersMap.get(sp.spawnId)?.spawn || sp;
                tryInspectWildPokemon(latest);
            });

            map.geoObjects.add(marker);
            spawnMarkersMap.set(sp.spawnId, { marker, spawn: sp });
        }
    });

    Array.from(spawnMarkersMap.keys()).forEach((id) => {
        if (!activeIds.has(id)) {
            const entry = spawnMarkersMap.get(id);
            if (entry && entry.marker) {
                map.geoObjects.remove(entry.marker);
            }
            spawnMarkersMap.delete(id);
        }
    });

    refreshSpawnDistances();
}

function tryInspectWildPokemon(spawn) {
    const distance = calculateDistanceMeters(
        currentUser.latitude,
        currentUser.longitude,
        spawn.latitude,
        spawn.longitude
    );

    if (distance > CATCH_RADIUS_METERS) {
        UiComponents.showErrorAlert(
            'Слишком далеко',
            `До покемона ${spawn.name} сейчас ${Math.round(distance)} м (нужно не более ${CATCH_RADIUS_METERS} м).`
        );
        return;
    }

    selectedWildSpawn = spawn;
    const speciesInfo = speciesCatalog.find(s => s.id === spawn.speciesId);
    const activeSlot = currentUser.activePartySlot || 1;

    document.getElementById('wild-card-holder').innerHTML = UiComponents.createPokemonLandscapeCard(spawn);
    document.getElementById('wild-title').textContent = `${spawn.name} (${spawn.archetype})`;
    document.getElementById('wild-lore').textContent = speciesInfo?.description || 'Обитатель мира EvoGrid.';
    document.getElementById('wild-active-slot-label').textContent = `Слот #${activeSlot} (${Math.round(distance)} м)`;

    document.getElementById('modal-wild-inspect').classList.remove('hidden');
}

function closeWildModal() {
    document.getElementById('modal-wild-inspect').classList.add('hidden');
}

async function confirmStartBattle() {
    closeWildModal();
    try {
        currentBattle = await ApiService.startBattle(
            currentUser.id,
            selectedWildSpawn.spawnId,
            currentUser.activePartySlot || 1
        );
        renderBattleState();
        document.getElementById('modal-battle').classList.remove('hidden');
    } catch (err) {
        UiComponents.showErrorAlert('Ошибка боя', err.message);
    }
}

function renderStatusBadges(statuses = []) {
    if (!statuses || statuses.length === 0) {
        return `<span class="status-badge empty-status">Нет эффектов</span>`;
    }
    return statuses.map(st => {
        const isNegative = /ожог|яд|сон|оглуш|замедл|ослеп/i.test(st);
        return `<span class="status-badge ${isNegative ? 'debuff' : 'buff'}">${st}</span>`;
    }).join('');
}

function formatAbilityMetaBadge(ab) {
    if (ab.category === 'DEFENSE') {
        return ab.blockPercent > 0 ? `БЛОК ${ab.blockPercent}%` : `УКЛОНЕНИЕ ${ab.evasionChancePercent}%`;
    }
    if (ab.category === 'UTILITY') {
        return ab.healPercentMaxHp > 0 ? `ХИЛ +${ab.healPercentMaxHp}%` : 'УСИЛЕНИЕ';
    }
    const stabLabel = ab.stab ? ' • STAB' : '';
    return `PWR ${ab.power}${stabLabel}`;
}

function renderAbilitiesGrid(abilities = [], isBattleOver = false) {
    const grid = document.getElementById('battle-abilities-grid');
    if (!grid) return;

    if (!abilities || abilities.length === 0) {
        grid.innerHTML = `
            <button class="uiv-btn primary" style="grid-column: 1 / -1" ${isBattleOver ? 'disabled' : ''} onclick="doAttack(1)">
                <span class="button_top">Атаковать</span>
            </button>
        `;
        return;
    }

    grid.innerHTML = abilities.map(ab => {
        const onCooldown = (ab.currentCooldown || 0) > 0;
        const disabled = isBattleOver || onCooldown;

        const cdBadge = onCooldown
            ? `<span class="cd-pill active-cd">КД: ${ab.currentCooldown} х.</span>`
            : (ab.maxCooldown > 0 ? `<span class="cd-pill">КД ${ab.maxCooldown}</span>` : `<span class="cd-pill ready-cd">Готово</span>`);

        return `
            <button class="uiv-btn ability-btn"
                    ${disabled ? 'disabled' : ''}
                    onclick="doAttack(${ab.slot})">
                <span class="button_top ability-top">
                    <span class="ability-header-row">
                        <span class="ability-name">${ab.slot}. ${ab.name}</span>
                        ${cdBadge}
                    </span>
                    <span class="ability-desc">${ab.description || ''}</span>
                    <span class="ability-meta-row">
                        <span class="ability-type-tag">${formatAbilityMetaBadge(ab)}</span>
                    </span>
                </span>
            </button>
        `;
    }).join('');
}

function renderBattleState() {
    const b = currentBattle;
    document.getElementById('battle-status-title').textContent = `Ход ${b.turnNumber} • ${b.status}`;

    const playerSpdVal = b.playerEffectiveSpeed !== undefined && b.playerEffectiveSpeed !== b.playerSpeed
        ? `${b.playerEffectiveSpeed} (${b.playerSpeed})`
        : b.playerSpeed;

    const enemySpdVal = b.enemyEffectiveSpeed !== undefined && b.enemyEffectiveSpeed !== b.enemySpeed
        ? `${b.enemyEffectiveSpeed} (${b.enemySpeed})`
        : b.enemySpeed;

    const playerCardObj = {
        name: b.playerPokemonName,
        element: b.playerElement,
        archetype: 'Ваш боец',
        maxHp: `${b.playerCurrentHp}/${b.playerMaxHp}`,
        attack: b.playerAttack,
        defense: b.playerDefense,
        speed: playerSpdVal
    };

    const enemyCardObj = {
        name: b.enemyPokemonName,
        element: b.enemyElement,
        archetype: b.enemyArchetype,
        maxHp: `${b.enemyCurrentHp}/${b.enemyMaxHp}`,
        attack: b.enemyAttack,
        defense: b.enemyDefense,
        speed: enemySpdVal
    };

    document.getElementById('battle-player-card').innerHTML = UiComponents.createPokemonLandscapeCard(playerCardObj);
    document.getElementById('battle-enemy-card').innerHTML = UiComponents.createPokemonLandscapeCard(enemyCardObj);

    const playerStatusesEl = document.getElementById('battle-player-statuses');
    if (playerStatusesEl) {
        playerStatusesEl.innerHTML = renderStatusBadges(b.playerActiveStatuses);
    }

    const enemyStatusesEl = document.getElementById('battle-enemy-statuses');
    if (enemyStatusesEl) {
        enemyStatusesEl.innerHTML = renderStatusBadges(b.enemyActiveStatuses);
    }

    const logBox = document.getElementById('battle-log-container');
    logBox.innerHTML = (b.combatLog || [])
        .map(line => `<div>${line.replace(/[^\p{L}\p{N}\p{P}\p{Z}\p{S}]/gu, '')}</div>`)
        .join('');
    logBox.scrollTop = logBox.scrollHeight;

    const isOver = b.status !== 'IN_PROGRESS';
    renderAbilitiesGrid(b.playerAbilities, isOver);

    const catchBtn = document.getElementById('btn-do-catch');
    if (catchBtn) catchBtn.disabled = isOver;
}

async function doAttack(slot = 1) {
    if (!currentBattle) return;
    currentBattle = await ApiService.attackBattle(currentBattle.battleId, slot);
    renderBattleState();
    await checkBattleEnd();
}

async function doCatch() {
    if (!currentBattle) return;
    currentBattle = await ApiService.catchPokemon(currentBattle.battleId);
    renderBattleState();
    await checkBattleEnd();
}

async function checkBattleEnd() {
    if (currentBattle.status !== 'IN_PROGRESS') {
        currentUser = await ApiService.getCurrentUser();
        updateActiveFighterLabel();
        await loadNearbySpawns(currentUser.latitude, currentUser.longitude);
    }
}

function closeBattleWindow() {
    document.getElementById('modal-battle').classList.add('hidden');
}