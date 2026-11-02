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

    document.getElementById('prof-avatar').textContent = profileData.email.charAt(0).toUpperCase();
    document.getElementById('prof-name').textContent = profileData.email;
    document.getElementById('prof-level').textContent = profileData.level;
    document.getElementById('prof-xp').textContent = profileData.experience;
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
                <span class="slot-tag">Слот #${slot} ${isMain ? '• Активный в бою' : ''}</span>
                <div class="card-click-area">${UiComponents.createPokemonLandscapeCard(p, isMain)}</div>
                <button class="uiv-btn ${isMain ? 'primary' : ''}" data-slot="${slot}">
                    <span class="button_top">${isMain ? 'Выбран для боя' : 'Сделать основным'}</span>
                </button>
            `;
            pillar.querySelector('.card-click-area').onclick = () => openDossierModal(p, true);
            pillar.querySelector('button').onclick = async () => {
                profileData = await ApiService.selectActiveSlot(slot);
                renderPartySlots();
            };
        } else {
            pillar.innerHTML = `
                <span class="slot-tag">Слот #${slot}</span>
                <div style="width:220px;height:350px;display:flex;align-items:center;justify-content:center;color:#94a3b8;font-size:13px;font-weight:600">
                    Свободный слот
                </div>
            `;
        }
        container.appendChild(pillar);
    }
}

function renderOwnedCollection() {
    const colContainer = document.getElementById('owned-collection-row');
    colContainer.innerHTML = '';
    (profileData.collection || []).forEach(p => {
        const wrapper = document.createElement('div');
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
        wrapper.innerHTML = UiComponents.createPokemonLandscapeCard(sp, caught);
        wrapper.onclick = () => openDossierModal(sp, false);
        dexContainer.appendChild(wrapper);
    });
}

function openDossierModal(pokeObj, isOwned) {
    inspectedPokemon = isOwned ? pokeObj : null;
    const speciesId = pokeObj.speciesId ?? pokeObj.id;
    const species = allSpecies.find(s => s.id === speciesId) || pokeObj;

    document.getElementById('detail-card-box').innerHTML = UiComponents.createPokemonLandscapeCard(pokeObj);
    document.getElementById('detail-poke-name').textContent = `${pokeObj.name} • ${pokeObj.archetype}`;
    document.getElementById('detail-poke-lore').textContent =
        species.description || 'Существо из мира EvoGrid.';
    document.getElementById('slot-assignment-controls').classList.toggle('hidden', !isOwned);
    document.getElementById('modal-poke-details').classList.remove('hidden');
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