let currentUser = null;
let speciesCatalog = [];
let map = null;
let currentTileLayer = null;
let currentStyleIndex = 0;

let playerMarker = null;
let radarCircle = null;
let accuracyCircle = null;

// Хранилище маркеров по spawnId без моргания DOM
const spawnMarkersMap = new Map();

let selectedWildSpawn = null;
let currentBattle = null;

// Состояние гео-трекинга и плавного перемещения
let isAutoFollow = true;
let animFrameId = null;
let lastServerSyncLat = 0;
let lastServerSyncLon = 0;
let lastServerSyncTime = 0;
let watchId = null;

const MAP_STYLES = [
    {
        name: 'Voyager HD',
        url: 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',
        attribution: '&copy; OpenStreetMap &copy; CARTO',
        maxZoom: 20
    },
    {
        name: 'Детальная OSM',
        url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
        attribution: '&copy; OpenStreetMap',
        maxZoom: 19
    },
    {
        name: 'Спутник Esri',
        url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
        attribution: '&copy; Esri',
        maxZoom: 19
    }
];

window.addEventListener('DOMContentLoaded', async () => {
    document.getElementById('dock-mount').innerHTML = UiComponents.renderBottomDock('map');
    try {
        [currentUser, speciesCatalog] = await Promise.all([
            ApiService.getCurrentUser(),
            ApiService.getAllSpecies()
        ]);
        updateActiveFighterLabel();

        const startLat = currentUser.latitude || 55.7558;
        const startLon = currentUser.longitude || 37.6173;
        lastServerSyncLat = startLat;
        lastServerSyncLon = startLon;
        lastServerSyncTime = Date.now();

        initGameMap(startLat, startLon);
        await loadNearbySpawns(startLat, startLon);
        startLiveGpsTracking();
        initKeyboardWalk();
    } catch {
        window.location.href = 'index.html';
    }
});

function updateActiveFighterLabel() {
    const slot = currentUser.activePartySlot || 1;
    const fighter = (currentUser.party || []).find(p => p.partySlot === slot) || (currentUser.party || [])[0];
    const label = fighter
        ? `Боец: ${fighter.name} (#${fighter.partySlot}) • Радар ${CATCH_RADIUS_METERS} м`
        : `Слот #${slot} • Радар ${CATCH_RADIUS_METERS} м`;
    document.getElementById('active-fighter-indicator').textContent = label;
}

function initGameMap(lat, lon) {
    map = L.map('map', {
        zoomControl: false,
        preferCanvas: true
    }).setView([lat, lon], 18);

    applyMapStyle(0);

    // Если пользователь сдвинул карту руками — ставим автоследование на паузу
    map.on('dragstart', () => {
        setAutoFollow(false);
    });

    const trainerIcon = L.divIcon({
        className: '',
        html: '<div class="trainer-map-pin"></div>',
        iconSize: [22, 22],
        iconAnchor: [11, 11]
    });

    accuracyCircle = L.circle([lat, lon], {
        radius: 0,
        color: '#38bdf8',
        fillColor: '#38bdf8',
        fillOpacity: 0.08,
        weight: 1,
        dashArray: '4, 4'
    }).addTo(map);

    radarCircle = L.circle([lat, lon], {
        radius: CATCH_RADIUS_METERS,
        color: '#0284c7',
        fillColor: '#38bdf8',
        fillOpacity: 0.14,
        weight: 2
    }).addTo(map);

    playerMarker = L.marker([lat, lon], {
        icon: trainerIcon,
        zIndexOffset: 1000
    }).addTo(map);

    // Перемещение по клику / тапу
    map.on('click', (e) => {
        const walkToggle = document.getElementById('click-walk-toggle');
        if (walkToggle && walkToggle.checked) {
            setGpsStatus('Режим тапа', 'gps-warn');
            moveTrainerTo(e.latlng.lat, e.latlng.lng, null, false);
        }
    });

    window.addEventListener('resize', () => {
        if (map) map.invalidateSize();
    });
}

function applyMapStyle(index) {
    const style = MAP_STYLES[index];
    if (currentTileLayer) {
        map.removeLayer(currentTileLayer);
    }
    currentTileLayer = L.tileLayer(style.url, {
        maxZoom: style.maxZoom,
        attribution: style.attribution
    }).addTo(map);
}

function switchMapStyle() {
    currentStyleIndex = (currentStyleIndex + 1) % MAP_STYLES.length;
    applyMapStyle(currentStyleIndex);
}

function setAutoFollow(enabled) {
    isAutoFollow = enabled;
    const btn = document.getElementById('btn-follow-me');
    if (btn) {
        btn.classList.toggle('active-follow', enabled);
    }
}

function centerOnTrainer() {
    setAutoFollow(true);
    if (map && currentUser) {
        map.flyTo([currentUser.latitude, currentUser.longitude], Math.max(map.getZoom(), 18), {
            animate: true,
            duration: 0.6
        });
    }
}

function setGpsStatus(text, stateClass = '') {
    const badge = document.getElementById('gps-status-badge');
    const label = document.getElementById('gps-status-text');
    if (!badge || !label) return;
    badge.className = `gps-badge ${stateClass}`;
    label.textContent = text;
}

/**
 * Плавная интерполяция маркера тренера и круга радара
 */
function animateMarkerTo(targetLat, targetLon, durationMs = 320) {
    if (!playerMarker || !radarCircle) return;
    if (animFrameId) cancelAnimationFrame(animFrameId);

    const startLatLng = playerMarker.getLatLng();
    const startLat = startLatLng.lat;
    const startLon = startLatLng.lng;
    const startTime = performance.now();

    function step(now) {
        const progress = Math.min(1, (now - startTime) / durationMs);
        const eased = 1 - Math.pow(1 - progress, 3); // easeOutCubic
        const curLat = startLat + (targetLat - startLat) * eased;
        const curLon = startLon + (targetLon - startLon) * eased;

        playerMarker.setLatLng([curLat, curLon]);
        radarCircle.setLatLng([curLat, curLon]);
        if (accuracyCircle) accuracyCircle.setLatLng([curLat, curLon]);

        if (progress < 1) {
            animFrameId = requestAnimationFrame(step);
        }
    }

    animFrameId = requestAnimationFrame(step);
}

async function moveTrainerTo(lat, lon, accuracy = null, fromGps = false) {
    const stepDist = calculateDistanceMeters(
        currentUser.latitude || lat,
        currentUser.longitude || lon,
        lat,
        lon
    );

    // Фильтрация мелкого GPS-шума (< 1.5 м)
    if (fromGps && stepDist < 1.5) {
        return;
    }

    currentUser.latitude = lat;
    currentUser.longitude = lon;

    animateMarkerTo(lat, lon);

    if (accuracyCircle) {
        accuracyCircle.setRadius(accuracy && accuracy < 150 ? accuracy : 0);
    }

    if (isAutoFollow && map) {
        map.panTo([lat, lon], { animate: true, duration: 0.45 });
    }

    // Мгновенно обновляем дистанции до существующих покемонов без перерисовки
    refreshSpawnDistances();

    // Синхронизируем с сервером при смещении >= 10 м или раз в 7 секунд
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
    if (!navigator.geolocation) {
        setGpsStatus('GPS недоступен', 'gps-warn');
        return;
    }

    if (watchId !== null) {
        navigator.geolocation.clearWatch(watchId);
    }

    watchId = navigator.geolocation.watchPosition(
        (pos) => {
            const acc = Math.round(pos.coords.accuracy || 0);
            setGpsStatus(`GPS ±${acc} м`, 'gps-active');
            moveTrainerTo(pos.coords.latitude, pos.coords.longitude, acc, true);
        },
        () => {
            setGpsStatus('Ходьба тапом', 'gps-warn');
        },
        {
            enableHighAccuracy: true,
            maximumAge: 2000,
            timeout: 10000
        }
    );
}

/**
 * Поддержка перемещения с клавиатуры (WASD / Стрелки) для удобного теста
 */
function initKeyboardWalk() {
    window.addEventListener('keydown', (e) => {
        if (!currentUser || !document.getElementById('modal-battle').classList.contains('hidden')) return;
        const step = 0.00012; // ~13 метров
        let dLat = 0;
        let dLon = 0;

        if (e.key === 'ArrowUp' || e.key === 'w' || e.key === 'W' || e.key === 'ц' || e.key === 'Ц') dLat = step;
        else if (e.key === 'ArrowDown' || e.key === 's' || e.key === 'S' || e.key === 'ы' || e.key === 'Ы') dLat = -step;
        else if (e.key === 'ArrowLeft' || e.key === 'a' || e.key === 'A' || e.key === 'ф' || e.key === 'Ф') dLon = -step;
        else if (e.key === 'ArrowRight' || e.key === 'd' || e.key === 'D' || e.key === 'в' || e.key === 'В') dLon = step;
        else return;

        e.preventDefault();
        moveTrainerTo(currentUser.latitude + dLat, currentUser.longitude + dLon, null, false);
    });
}

function buildSpawnBadgeHtml(sp, dist) {
    const inRange = dist <= CATCH_RADIUS_METERS;
    return `
        <div class="wild-map-badge ${inRange ? 'in-range' : ''}">
            ${renderGoogleIcon(sp.element)}
            <span>${sp.name}</span>
            <span class="dist-label" style="color:#64748b">${dist} м</span>
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
            const existing = spawnMarkersMap.get(sp.spawnId);
            existing.spawn = sp;
        } else {
            const icon = L.divIcon({
                className: '',
                html: buildSpawnBadgeHtml(sp, dist),
                iconAnchor: [55, 16]
            });
            const marker = L.marker([sp.latitude, sp.longitude], { icon }).addTo(map);
            marker.on('click', () => {
                const latest = spawnMarkersMap.get(sp.spawnId)?.spawn || sp;
                tryInspectWildPokemon(latest);
            });
            spawnMarkersMap.set(sp.spawnId, { marker, spawn: sp });
        }
    });

    // Удаляем только неактивные или исчезнувшие спавны
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
            'Вне радиуса 70 м',
            `Подойдите ближе! До ${spawn.name} сейчас ${Math.round(distance)} м.`
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
        updateActiveFighterLabel();
        await loadNearbySpawns(currentUser.latitude, currentUser.longitude);
    }
}

function closeBattleWindow() {
    document.getElementById('modal-battle').classList.add('hidden');
}