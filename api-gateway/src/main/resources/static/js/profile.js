let profileData = null;
let allSpecies = [];
let inspectedPokemon = null;

window.addEventListener('DOMContentLoaded', async () => {
    try {
        await reloadProfileData();
        routeByHash();
        window.addEventListener('hashchange', routeByHash);
    } catch {
        window.location.href = 'index.html';
    }
});

function routeByHash() {
    const hash = (window.location.hash || '#profile').replace('#', '');
    const validTab = ['profile', 'party', 'pokedex'].includes(hash) ? hash : 'profile';

    document.getElementById('view-profile').classList.toggle('hidden', validTab !== 'profile');
    document.getElementById('view-party').classList.toggle('hidden', validTab !== 'party');
    document.getElementById('view-pokedex').classList.toggle('hidden', validTab !== 'pokedex');

    document.getElementById('dock-mount').innerHTML = UiComponents.renderBottomDock(validTab);
}

async function reloadProfileData() {
    [profileData, allSpecies] = await Promise.all([
        ApiService.getCurrentUser(),
        ApiService.getAllSpecies()
    ]);

    const xp = profileData.experience || 0;
    const xpPercent = Math.min(100, Math.max(0, xp % 100));

    document.getElementById('prof-avatar').textContent = profileData.email.charAt(0).toUpperCase();
    document.getElementById('prof-name').textContent = profileData.email;
    document.getElementById('prof-level').textContent = profileData.level;
    document.getElementById('prof-xp').textContent = xp;

    const xpTile = document.getElementById('prof-xp-tile');
    if (xpTile) xpTile.textContent = xp;

    const xpBar = document.getElementById('prof-xp-bar');
    if (xpBar) xpBar.style.width = `${xpPercent}%`;

    document.getElementById('prof-balls').textContent = profileData.pokeballs;
    document.getElementById('prof-total-count').textContent = (profileData.collection || []).length;

    renderPartySlots();
    renderOwnedCollection();
    renderFullPokedex();
}

function renderPartySlots() {
    const activeSlot = profileData.activePartySlot || 1;
    const container = document.getElementById('party-slots-row');
    container.innerHTML = '';

    for (let slot = 1; slot <= 3; slot++) {
        const p = (profileData.party || []).find(item => item.partySlot === slot);
        const pillar = document.createElement('div');
        pillar.className = 'slot-pillar';

        if (p) {
            const isMain = activeSlot === slot;
            pillar.innerHTML = `
                <span class="slot-tag ${isMain ? 'active-tag' : ''}">
                    Слот #${slot} ${isMain ? '• Активен' : ''}
                </span>
                <div class="card-click-area">${UiComponents.createPokemonLandscapeCard(p, isMain)}</div>
                <div class="slot-actions-col">
                    <button class="uiv-btn ${isMain ? 'primary' : ''} btn-select-slot" style="width:100%" data-slot="${slot}">
                        <span class="button_top">${isMain ? 'Активный боец' : 'Сделать основным'}</span>
                    </button>
                    <button class="uiv-btn btn-inspect-abilities" style="width:100%">
                        <span class="button_top">Способности</span>
                    </button>
                </div>
            `;
            pillar.querySelector('.card-click-area').onclick = () => openDossierModal(p, true);
            pillar.querySelector('.btn-inspect-abilities').onclick = () => openDossierModal(p, true);
            pillar.querySelector('.btn-select-slot').onclick = async () => {
                profileData = await ApiService.selectActiveSlot(slot);
                renderPartySlots();
            };
        } else {
            pillar.innerHTML = `
                <span class="slot-tag">Слот #${slot}</span>
                <div class="slot-empty-box">
                    <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.8">
                        <circle cx="12" cy="12" r="9" stroke-dasharray="4 4"/>
                        <line x1="12" y1="8" x2="12" y2="16"/>
                        <line x1="8" y1="12" x2="16" y2="12"/>
                    </svg>
                    <span>Слот свободен</span>
                    <small>Выберите покемона из коллекции ниже</small>
                </div>
            `;
        }
        container.appendChild(pillar);
    }
}

function renderOwnedCollection() {
    const colContainer = document.getElementById('owned-collection-row');
    colContainer.innerHTML = '';
    if (!(profileData.collection || []).length) {
        colContainer.innerHTML = '<div class="empty-hint">Пока никого не поймано. Открой карту и подойди к дикому покемону ближе чем на 70 м.</div>';
        return;
    }
    (profileData.collection || []).forEach(p => {
        const wrapper = document.createElement('div');
        wrapper.className = 'grid-card-item';
        wrapper.innerHTML = UiComponents.createPokemonLandscapeCard(p, Boolean(p.partySlot));
        wrapper.onclick = () => openDossierModal(p, true);
        colContainer.appendChild(wrapper);
    });
}

function renderFullPokedex() {
    const dexContainer = document.getElementById('pokedex-catalog-row');
    dexContainer.innerHTML = '';
    allSpecies.forEach(sp => {
        const caught = (profileData.collection || []).some(c => c.speciesId === sp.id);
        const wrapper = document.createElement('div');
        wrapper.className = 'grid-card-item';
        wrapper.innerHTML = UiComponents.createPokemonLandscapeCard(sp, caught);
        wrapper.onclick = () => openDossierModal(sp, false);
        dexContainer.appendChild(wrapper);
    });
}

function formatDossierAbilityMeta(ab) {
    if (ab.category === 'DEFENSE') {
        return ab.blockPercent > 0 ? `БЛОК ${ab.blockPercent}%` : `УКЛОНЕНИЕ ${ab.evasionChancePercent}%`;
    }
    if (ab.category === 'UTILITY') {
        return ab.healPercentMaxHp > 0 ? `ХИЛ +${ab.healPercentMaxHp}%` : 'УСИЛЕНИЕ';
    }
    const stabLabel = ab.stab ? ' • STAB' : '';
    return `PWR ${ab.power}${stabLabel}`;
}

async function openDossierModal(pokeObj, isOwned) {
    inspectedPokemon = isOwned ? pokeObj : null;
    const speciesId = pokeObj.speciesId ?? pokeObj.id;
    const species = allSpecies.find(s => s.id === speciesId) || pokeObj;

    document.getElementById('detail-card-box').innerHTML = UiComponents.createPokemonLandscapeCard(pokeObj);
    document.getElementById('detail-poke-name').textContent = `${pokeObj.name} • ${pokeObj.archetype}`;
    document.getElementById('detail-poke-lore').textContent =
        species.description || 'Обитатель мира EvoGrid.';
    document.getElementById('slot-assignment-controls').classList.toggle('hidden', !isOwned);

    const abilitiesListEl = document.getElementById('detail-poke-abilities-list');
    abilitiesListEl.innerHTML = `<div class="ability-loading">Загрузка способностей...</div>`;
    document.getElementById('modal-poke-details').classList.remove('hidden');

    try {
        const abilities = await ApiService.getPokemonAbilities(speciesId, pokeObj.name);
        abilitiesListEl.innerHTML = abilities.map(ab => {
            const cdText = ab.maxCooldown > 0 ? `КД ${ab.maxCooldown}` : 'Без КД';
            return `
                <div class="dossier-ability-card">
                    <div class="ability-header-row">
                        <span class="ability-name">${ab.slot}. ${ab.name}</span>
                        <span class="cd-pill">${cdText}</span>
                    </div>
                    <div class="ability-desc">${ab.description || ''}</div>
                    <div class="ability-meta-row">
                        <span class="ability-type-tag">${formatDossierAbilityMeta(ab)}</span>
                    </div>
                </div>
            `;
        }).join('');
    } catch {
        abilitiesListEl.innerHTML = `<div class="ability-loading">Не удалось получить список способностей</div>`;
    }
}

async function assignToSlot(slotNumber) {
    if (!inspectedPokemon) return;
    await ApiService.assignPartySlot(profileData.id, slotNumber, inspectedPokemon.id);
    closeDetailModal();
    await reloadProfileData();
}

function closeDetailModal() {
    document.getElementById('modal-poke-details').classList.add('hidden');
}