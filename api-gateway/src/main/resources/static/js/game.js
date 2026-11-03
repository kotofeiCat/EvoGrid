let currentUser = null;
let speciesCatalog = [];
let map = null;

let playerMarker = null;
let radarCircle = null;

const spawnMarkersMap = new Map();

let selectedWildSpawn = null;
let currentBattle = null;

let animFrameId = null;
let lastServerSyncLat = 0;
let lastServerSyncLon = 0;
let lastServerSyncTime = 0;
let watchId = null;

window.addEventListener('DOMContentLoaded', async () => {
    document.getElementById('dock-mount').innerHTML = UiComponents.renderBottomDock('map');
    try {
        [currentUser, speciesCatalog] = await Promise.all([
            ApiService.getCurrentUser(),
            ApiService.getAllSpecies()
        ]);

        const startLat = currentUser.latitude || 55.7558;
        const startLon = currentUser.longitude || 37.6173;
        lastServerSyncLat = startLat;
        lastServerSyncLon = startLon;
        lastServerSyncTime = Date.now();

        initGameMap(startLat, startLon);
        await loadNearbySpawns(startLat, startLon);
        startLiveGpsTracking();
    } catch {
        window.location.href = 'index.html';
    }
});

function initGameMap(lat, lon) {
    map = L.map('map', {
        zoomControl: false,
        preferCanvas: true
    }).setView([lat, lon], 18);

    // Официальный бесплатный слой OpenStreetMap (работает без API-ключей)
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap'
    }).addTo(map);

    const trainerIcon = L.divIcon({
        className: '',
        html: '<div class="trainer-map-pin"></div>',
        iconSize: [22, 22],
        iconAnchor: [11, 11]
    });

    radarCircle = L.circle([lat, lon], {
        radius: CATCH_RADIUS_METERS,
        color: '#0284c7',
        fillColor: '#38bdf8',
        fillOpacity: 0.12,
        weight: 2
    }).addTo(map);

    playerMarker = L.marker([lat, lon], {
        icon: trainerIcon,
        zIndexOffset: 1000
    }).addTo(map);

    window.addEventListener('resize', () => {
        if (map) map.invalidateSize();
    });
}

function animateMarkerTo(targetLat, targetLon, durationMs = 350) {
    if (!playerMarker || !radarCircle) return;
    if (animFrameId) cancelAnimationFrame(animFrameId);

    const startLatLng = playerMarker.getLatLng();
    const startLat = startLatLng.lat;
    const startLon = startLatLng.lng;
    const startTime = performance.now();

    function step(now) {
        const progress = Math.min(1, (now - startTime) / durationMs);
        const eased = 1 - Math.pow(1 - progress, 3);
        const curLat = startLat + (targetLat - startLat) * eased;
        const curLon = startLon + (targetLon - startLon) * eased;

        playerMarker.setLatLng([curLat, curLon]);
        radarCircle.setLatLng([curLat, curLon]);

        if (progress < 1) {
            animFrameId = requestAnimationFrame(step);
        }
    }

    animFrameId = requestAnimationFrame(step);
}

async function moveTrainerTo(lat, lon) {
    const stepDist = calculateDistanceMeters(
        currentUser.latitude || lat,
        currentUser.longitude || lon,
        lat,
        lon
    );

    if (stepDist < 1.5) return;

    currentUser.latitude = lat;
    currentUser.longitude = lon;

    animateMarkerTo(lat, lon);
    if (map) {
        map.panTo([lat, lon], { animate: true, duration: 0.5 });
    }

    refreshSpawnDistances();

    const distFromLastSync = calculateDistanceMeters(lastServerSyncLat, lastServerSyncLon, lat, lon);
    const now = Date.now();
    if (distFromLastSync >= 10 || now - lastServerSyncTime >= 7000) {
        lastServerSyncLat = lat;
        lastServerSyncLon = lon;
        lastServerSyncTime = now;
        ApiService.updateLocation(currentUser.id, lat, lon).catch(() => {});
        await loadNearbySpawns(lat, lon);
    }
}

function startLiveGpsTracking() {
    if (!navigator.geolocation) return;

    if (watchId !== null) {
        navigator.geolocation.clearWatch(watchId);
    }

    watchId = navigator.geolocation.watchPosition(
        (pos) => {
            moveTrainerTo(pos.coords.latitude, pos.coords.longitude);
        },
        () => {},
        {
            enableHighAccuracy: true,
            maximumAge: 2500,
            timeout: 10000
        }
    );
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
        const el = marker.getElement();
        if (el) {
            const badge = el.querySelector('.wild-map-badge');
            const distLabel = el.querySelector('.dist-label');
            if (badge) badge.classList.toggle('in-range', dist <= CATCH_RADIUS_METERS);
            if (distLabel) distLabel.textContent = `${dist} м`;
        }
    });
}

async function loadNearbySpawns(lat, lon) {
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
            spawnMarkersMap.get(sp.spawnId).spawn = sp;
        } else {
            const icon = L.divIcon({
                className: 'spawn-marker-wrapper',
                html: buildSpawnBadgeHtml(sp, dist),
                iconSize: [0, 0]
            });
            const marker = L.marker([sp.latitude, sp.longitude], { icon }).addTo(map);
            marker.on('click', () => {
                const latest = spawnMarkersMap.get(sp.spawnId)?.spawn || sp;
                tryInspectWildPokemon(latest);
            });
            spawnMarkersMap.set(sp.spawnId, { marker, spawn: sp });
        }
    });

    Array.from(spawnMarkersMap.keys()).forEach((id) => {
        if (!activeIds.has(id)) {
            map.removeLayer(spawnMarkersMap.get(id).marker);
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
            `Подойдите ближе! До ${spawn.name} сейчас ${Math.round(distance)} м (нужно ≤ ${CATCH_RADIUS_METERS} м).`
        );
        return;
    }

    selectedWildSpawn = spawn;
    const speciesInfo = speciesCatalog.find(s => s.id === spawn.speciesId);
    const activeSlot = currentUser.activePartySlot || 1;

    document.getElementById('wild-card-holder').innerHTML = UiComponents.createPokemonLandscapeCard(spawn);
    document.getElementById('wild-title').textContent = `${spawn.name} (${spawn.archetype})`;
    document.getElementById('wild-lore').textContent = speciesInfo?.description || 'Дикое существо мира EvoGrid.';
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

function renderBattleState() {
    const b = currentBattle;
    document.getElementById('battle-status-title').textContent = `Ход ${b.turnNumber} • ${b.status}`;

    const playerCardObj = {
        name: b.playerPokemonName,
        element: b.playerElement,
        archetype: 'Ваш боец',
        maxHp: `${b.playerCurrentHp}/${b.playerMaxHp}`,
        attack: b.playerAttack,
        defense: b.playerDefense,
        speed: b.playerSpeed
    };
    const enemyCardObj = {
        name: b.enemyPokemonName,
        element: b.enemyElement,
        archetype: b.enemyArchetype,
        maxHp: `${b.enemyCurrentHp}/${b.enemyMaxHp}`,
        attack: b.enemyAttack,
        defense: b.enemyDefense,
        speed: b.enemySpeed
    };

    document.getElementById('battle-player-card').innerHTML = UiComponents.createPokemonLandscapeCard(playerCardObj);
    document.getElementById('battle-enemy-card').innerHTML = UiComponents.createPokemonLandscapeCard(enemyCardObj);

    const logBox = document.getElementById('battle-log-container');
    logBox.innerHTML = (b.combatLog || [])
        .map(line => `<div>${line.replace(/[^\p{L}\p{N}\p{P}\p{Z}]/gu, '')}</div>`)
        .join('');
    logBox.scrollTop = logBox.scrollHeight;

    const isOver = b.status !== 'IN_PROGRESS';
    document.getElementById('btn-do-attack').disabled = isOver;
    document.getElementById('btn-do-catch').disabled = isOver;
}

async function doAttack() {
    currentBattle = await ApiService.attackBattle(currentBattle.battleId);
    renderBattleState();
    await checkBattleEnd();
}

async function doCatch() {
    currentBattle = await ApiService.catchPokemon(currentBattle.battleId);
    renderBattleState();
    await checkBattleEnd();
}

async function checkBattleEnd() {
    if (currentBattle.status !== 'IN_PROGRESS') {
        currentUser = await ApiService.getCurrentUser();
        await loadNearbySpawns(currentUser.latitude, currentUser.longitude);
    }
}

function closeBattleWindow() {
    document.getElementById('modal-battle').classList.add('hidden');
}