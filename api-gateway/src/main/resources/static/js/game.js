let currentUser = null;
let speciesCatalog = [];
let map = null;
let playerMarker = null;
let radarCircle = null;
let spawnMarkers = [];
let selectedWildSpawn = null;
let currentBattle = null;

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
        initGameMap(startLat, startLon);
        await loadNearbySpawns(startLat, startLon);
        startLiveGpsTracking();
    } catch {
        window.location.href = 'index.html';
    }
});

function updateActiveFighterLabel() {
    const slot = currentUser.activePartySlot || 1;
    const fighter = (currentUser.party || []).find(p => p.partySlot === slot) || (currentUser.party || [])[0];
    const label = fighter
        ? `Боец: ${fighter.name} (#${fighter.partySlot}) • Радар ${CATCH_RADIUS_METERS}м`
        : `Слот #${slot} • Радар ${CATCH_RADIUS_METERS}м`;
    document.getElementById('active-fighter-indicator').textContent = label;
}

function initGameMap(lat, lon) {
    map = L.map('map', { zoomControl: false }).setView([lat, lon], 18);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap'
    }).addTo(map);

    const trainerIcon = L.divIcon({
        className: '',
        html: '<div class="trainer-map-pin"></div>',
        iconSize: [20, 20],
        iconAnchor: [10, 10]
    });

    playerMarker = L.marker([lat, lon], { icon: trainerIcon }).addTo(map);
    radarCircle = L.circle([lat, lon], {
        radius: CATCH_RADIUS_METERS,
        color: '#0284c7',
        fillColor: '#38bdf8',
        fillOpacity: 0.14,
        weight: 2
    }).addTo(map);
}

async function moveTrainerTo(lat, lon) {
    currentUser.latitude = lat;
    currentUser.longitude = lon;
    playerMarker.setLatLng([lat, lon]);
    radarCircle.setLatLng([lat, lon]);

    ApiService.updateLocation(currentUser.id, lat, lon).catch(() => {});
    await loadNearbySpawns(lat, lon);
}

function startLiveGpsTracking() {
    if (!navigator.geolocation) return;
    navigator.geolocation.watchPosition(
        (pos) => moveTrainerTo(pos.coords.latitude, pos.coords.longitude),
        () => {},
        { enableHighAccuracy: true, maximumAge: 4000 }
    );
}

async function loadNearbySpawns(lat, lon) {
    const spawns = await ApiService.getSpawnsNear(lat, lon);
    spawnMarkers.forEach(m => map.removeLayer(m));
    spawnMarkers = [];

    spawns.forEach(sp => {
        if (!sp.active) return;
        const dist = Math.round(calculateDistanceMeters(currentUser.latitude, currentUser.longitude, sp.latitude, sp.longitude));

        const icon = L.divIcon({
            className: '',
            html: `<div class="wild-map-badge">${renderGoogleIcon(sp.element)}<span>${sp.name}</span><span style="color:#64748b">${dist}м</span></div>`,
            iconAnchor: [55, 16]
        });

        const marker = L.marker([sp.latitude, sp.longitude], { icon }).addTo(map);
        marker.on('click', () => tryInspectWildPokemon(sp));
        spawnMarkers.push(marker);
    });
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
            'Цель вне радиуса 70 м',
            `До покемона ${spawn.name} сейчас ${Math.round(distance)} м. Подойдите ближе к синему кругу радара.`
        );
        return;
    }

    selectedWildSpawn = spawn;
    const speciesInfo = speciesCatalog.find(s => s.id === spawn.speciesId);
    const activeSlot = currentUser.activePartySlot || 1;

    document.getElementById('wild-card-holder').innerHTML = UiComponents.createPokemonLandscapeCard(spawn);
    document.getElementById('wild-title').textContent = `${spawn.name} (${spawn.archetype})`;
    document.getElementById('wild-lore').textContent = speciesInfo?.description || 'Дикий обитатель мира EvoGrid.';
    document.getElementById('wild-active-slot-label').textContent = `Слот #${activeSlot} (${Math.round(distance)} м от вас)`;
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
        UiComponents.showErrorAlert('Не удалось начать бой', err.message);
    }
}

function renderBattleState() {
    const b = currentBattle;
    document.getElementById('battle-status-title').textContent = `Ход ${b.turnNumber} • ${b.status}`;

    const playerCardObj = {
        name: b.playerPokemonName,
        element: b.playerElement,
        archetype: 'Твой боец',
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