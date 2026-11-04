let profileData = null;
let allSpecies = [];
let inspectedPokemon = null;
/** Сумка из inventory-service; null — сервис недоступен (профиль всё равно работает). */
let bagData = null;

window.addEventListener('DOMContentLoaded', async () => {
    try {
        await reloadProfileData();
        routeByHash();
        window.addEventListener('hashchange', routeByHash);
    } catch {
        window.location.href = 'index.html';
        return;
    }

    // Свайп вниз закрывает досье, тап по затемнению — тоже
    const detailModal = document.getElementById('modal-poke-details');
    Gestures.enableSwipeToClose(detailModal, closeDetailModal);
    detailModal.addEventListener('click', (e) => {
        if (e.target === detailModal) closeDetailModal();
    });

    // Свайп влево/вправо — соседняя вкладка нижнего меню (Карта, Отряд, Эводекс, Магазин, Профиль)
    Gestures.enableTabSwipe(document.body, () => TAB_ORDER.indexOf(currentTab()), (index, direction) => {
        const tab = TAB_ORDER[index];
        if (tab === 'map' || tab === 'shop') {
            window.location.href = tab === 'map' ? 'game.html' : 'shop.html';
            return;
        }
        pendingSlide = direction;
        window.location.hash = '#' + tab;
    }, TAB_ORDER.length);
});

const TAB_ORDER = ['map', 'party', 'pokedex', 'shop', 'profile'];
let pendingSlide = null;

function currentTab() {
    const hash = (window.location.hash || '#profile').replace('#', '');
    return ['profile', 'party', 'pokedex'].includes(hash) ? hash : 'profile';
}

function routeByHash() {
    const hash = (window.location.hash || '#profile').replace('#', '');
    const validTab = ['profile', 'party', 'pokedex'].includes(hash) ? hash : 'profile';

    document.getElementById('view-profile').classList.toggle('hidden', validTab !== 'profile');
    document.getElementById('view-party').classList.toggle('hidden', validTab !== 'party');
    document.getElementById('view-pokedex').classList.toggle('hidden', validTab !== 'pokedex');

    document.getElementById('dock-mount').innerHTML = UiComponents.renderBottomDock(validTab);

    if (pendingSlide) {
        const view = document.getElementById('view-' + validTab);
        view.classList.remove('slide-from-left', 'slide-from-right');
        void view.offsetWidth;
        view.classList.add(pendingSlide === 'left' ? 'slide-from-right' : 'slide-from-left');
        pendingSlide = null;
        window.scrollTo({ top: 0 });
    }
}

async function reloadProfileData() {
    [profileData, allSpecies, bagData] = await Promise.all([
        ApiService.getCurrentUser(),
        ApiService.getAllSpecies(),
        ApiService.getInventory().catch(() => null)
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

    document.getElementById('prof-coins').textContent = bagData ? bagData.coins : '—';
    renderProfileBag();
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
                <div class="card-click-area ${p.currentHp <= 0 ? 'is-fainted' : ''}">${UiComponents.createPokemonLandscapeCard(withHp(p), isMain)}</div>
                ${renderHpLine(p)}
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
                try {
                    profileData = await ApiService.selectActiveSlot(slot);
                    renderPartySlots();
                } catch (err) {
                    UiComponents.showErrorAlert('Не получилось', err.message);
                }
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
                    <small>Выберите эвона из коллекции ниже</small>
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
        colContainer.innerHTML = '<div class="empty-hint">Пока никого не поймано. Открой карту и подойди к дикому эвону ближе чем на 70 м.</div>';
        return;
    }
    (profileData.collection || []).forEach(p => {
        const wrapper = document.createElement('div');
        wrapper.className = 'grid-card-item';
        wrapper.className = `grid-card-item ${p.currentHp <= 0 ? 'is-fainted' : ''}`;
        wrapper.innerHTML = UiComponents.createPokemonLandscapeCard(withHp(p), Boolean(p.partySlot));
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

    document.getElementById('detail-card-box').innerHTML =
        UiComponents.createPokemonLandscapeCard(isOwned ? withHp(pokeObj) : pokeObj);
    renderHealSection(isOwned ? pokeObj : null);
    document.getElementById('detail-poke-name').textContent = `${pokeObj.name} • ${pokeObj.archetype}`;
    document.getElementById('detail-poke-lore').textContent =
        species.description || 'Обитатель мира EvoGrid.';
    document.getElementById('slot-assignment-controls').classList.toggle('hidden', !isOwned);

    const abilitiesListEl = document.getElementById('detail-poke-abilities-list');
    abilitiesListEl.innerHTML = `<div class="ability-loading">Загрузка способностей...</div>`;
    document.getElementById('modal-poke-details').classList.remove('hidden');

    try {
        const abilities = await ApiService.getPokemonAbilities(speciesId);
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
    try {
        await ApiService.assignPartySlot(slotNumber, inspectedPokemon.id);
        closeDetailModal();
        await reloadProfileData();
    } catch (err) {
        UiComponents.showErrorAlert('Не получилось', err.message);
    }
}

// ==================== ЗДОРОВЬЕ И СУМКА ====================

/** Для карточки: в плашке HP показываем текущее здоровье «42/70». */
function withHp(p) {
    return { ...p, maxHp: `${p.currentHp}/${p.maxHp}` };
}

function hpClass(p) {
    const pct = p.maxHp > 0 ? (p.currentHp / p.maxHp) * 100 : 0;
    return pct <= 25 ? 'low' : (pct <= 55 ? 'mid' : '');
}

function renderHpLine(p) {
    const pct = p.maxHp > 0 ? Math.round((p.currentHp / p.maxHp) * 100) : 0;
    const label = p.currentHp <= 0 ? 'Без сил' : `${p.currentHp}/${p.maxHp} HP`;
    return `
        <div class="slot-hp-line">
            <div class="hp-bar"><div class="hp-bar-fill ${hpClass(p)}" style="width:${pct}%"></div></div>
            <span>${label}</span>
        </div>`;
}

function renderProfileBag() {
    const box = document.getElementById('profile-bag');
    if (!bagData) {
        box.innerHTML = '<div class="empty-hint">Сумка временно недоступна.</div>';
        return;
    }
    if (!bagData.items.length) {
        box.innerHTML = '<div class="empty-hint">Сумка пуста. Загляните в магазин.</div>';
        return;
    }
    box.innerHTML = bagData.items.map(({ item, quantity }) => `
        <div class="bag-item static">
            ${ItemArt.icon(item)}
            <span class="bag-item-body">
                <span class="bag-item-name">${item.name}</span>
                <span class="bag-item-desc">${item.description || ''}</span>
            </span>
            <span class="bag-item-qty">×${quantity}</span>
        </div>`).join('');
}

/** В досье своего эвона: полоска здоровья и лечилки из сумки, которые к нему подходят. */
function renderHealSection(owned) {
    const wrap = document.getElementById('detail-heal-wrap');
    wrap.classList.toggle('hidden', !owned);
    if (!owned) return;

    const pct = owned.maxHp > 0 ? (owned.currentHp / owned.maxHp) * 100 : 0;
    const fill = document.getElementById('detail-hp-fill');
    fill.style.width = `${pct}%`;
    fill.className = `hp-bar-fill ${hpClass(owned)}`;
    document.getElementById('detail-hp-text').textContent =
        owned.currentHp <= 0 ? 'Без сил' : `${owned.currentHp}/${owned.maxHp} HP`;

    const list = document.getElementById('detail-heal-items');
    const fainted = owned.currentHp <= 0;
    const full = owned.currentHp >= owned.maxHp;
    if (full) {
        list.innerHTML = '<div class="bag-panel-hint">Полностью здоров.</div>';
        return;
    }
    const usable = (bagData?.items || []).filter(({ item }) =>
        fainted ? item.kind === 'REVIVE' : item.kind === 'HEAL');
    if (!usable.length) {
        list.innerHTML = `<div class="bag-panel-hint">${fainted
            ? 'Нужна «Искра жизни» — она есть в магазине. Или подождите: силы возвращаются сами примерно за полчаса.'
            : 'Лечилок нет. Здоровье восстанавливается само со временем или купите настой в магазине.'}</div>`;
        return;
    }
    list.innerHTML = usable.map(({ item, quantity }) => `
        <button type="button" class="bag-item" onclick="healInspected('${item.code}')">
            ${ItemArt.icon(item)}
            <span class="bag-item-body">
                <span class="bag-item-name">${item.name}</span>
                <span class="bag-item-desc">${item.description || ''}</span>
            </span>
            <span class="bag-item-qty">×${quantity}</span>
        </button>`).join('');
}

async function healInspected(itemCode) {
    if (!inspectedPokemon) return;
    try {
        await ApiService.useItemOnPokemon(inspectedPokemon.id, itemCode);
        await reloadProfileData();
        const updated = (profileData.collection || []).find(p => p.id === inspectedPokemon.id);
        if (updated) {
            inspectedPokemon = updated;
            document.getElementById('detail-card-box').innerHTML = UiComponents.createPokemonLandscapeCard(withHp(updated));
            renderHealSection(updated);
        }
    } catch (err) {
        UiComponents.showErrorAlert('Не получилось', err.message);
    }
}

function closeDetailModal() {
    document.getElementById('modal-poke-details').classList.add('hidden');
}