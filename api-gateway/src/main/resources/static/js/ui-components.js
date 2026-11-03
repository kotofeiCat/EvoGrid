const UiComponents = {
    showErrorAlert(title = 'Ошибка', description = 'Попробуйте снова') {
        let stack = document.getElementById('uiv-toast-stack');
        if (!stack) {
            stack = document.createElement('div');
            stack.id = 'uiv-toast-stack';
            stack.className = 'toast-stack';
            document.body.appendChild(stack);
        }

        const alertEl = document.createElement('div');
        alertEl.className = 'error-alert';
        alertEl.innerHTML = `
            <div class="error-alert-left">
                <div class="error-alert-icon">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9 3.75h.008v.008H12v-.008Z"></path>
                    </svg>
                </div>
                <div>
                    <p class="error-alert-title">${title}</p>
                    <p class="error-alert-desc">${description}</p>
                </div>
            </div>
            <button class="error-alert-close" aria-label="Закрыть">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12"></path>
                </svg>
            </button>
        `;

        alertEl.querySelector('.error-alert-close').onclick = () => alertEl.remove();
        stack.appendChild(alertEl);

        setTimeout(() => {
            if (alertEl.parentNode) {
                alertEl.remove();
            }
        }, 4500);
    },

    createPokemonLandscapeCard(poke, isSelected = false) {
        const hp = poke.maxHp ?? poke.baseHp ?? 75;
        const atk = poke.attack ?? poke.baseAttack ?? 25;
        const def = poke.defense ?? poke.baseDefense ?? 25;
        const spd = poke.speed ?? poke.baseSpeed ?? 25;
        const elem = poke.element || 'NEUTRAL';

        return `
        <div class="poke-card ${isSelected ? 'selected-card' : ''}" data-element="${elem}">
            <section class="landscape-section">
                <div class="sky"></div>
                <div class="sun"></div>
                <div class="hill-1"></div>
                <div class="hill-2"></div>
                <div class="ocean">
                    <div class="reflection"></div>
                    <div class="reflection"></div>
                    <div class="reflection"></div>
                </div>
                <div class="hill-3"></div>
                <div class="hill-4"></div>
                <div class="tree-1" style="color:#475569">${VECTOR_PATHS.TREE}</div>
                <div class="tree-2" style="color:#475569">${VECTOR_PATHS.TREE}</div>
                <div class="tree-3" style="color:#334155">${VECTOR_PATHS.TREE}</div>
                <div class="filter"></div>

                <!-- Верхние контрастные плашки стихии и HP -->
                <div class="card-top-badges">
                    <span class="elem-pill">
                        ${renderGoogleIcon(elem)}
                        <span>${elem}</span>
                    </span>
                    <span class="hp-pill">${hp} HP</span>
                </div>

                <!-- Название покемона и класс снизу пейзажа -->
                <div class="card-hero-title">
                    <div class="poke-name-line">${poke.name}</div>
                    <div class="poke-arch-line">${poke.archetype || 'Базовый вид'}</div>
                </div>
            </section>

            <section class="content-section">
                <div class="forecast">
                    <div class="stat-row">
                        <span class="stat-label">Атака (ATK)</span>
                        <span class="stat-val">${atk}</span>
                    </div>
                    <div class="separator"></div>
                    <div class="stat-row">
                        <span class="stat-label">Защита (DEF)</span>
                        <span class="stat-val">${def}</span>
                    </div>
                    <div class="separator"></div>
                    <div class="stat-row">
                        <span class="stat-label">Скорость (SPD)</span>
                        <span class="stat-val">${spd}</span>
                    </div>
                </div>
            </section>
        </div>`;
    },

    renderBottomDock(activeTab) {
        return `
        <nav class="bottom-dock-wrap">
            <ul class="example-1">
                <li class="icon-content">
                    <a href="game.html" data-social="spotify" class="link ${activeTab === 'map' ? 'active-nav' : ''}" aria-label="Карта">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"/></svg>
                    </a>
                    <div class="tooltip">Карта</div>
                </li>
                <li class="icon-content">
                    <a href="profile.html#party" data-social="pinterest" class="link ${activeTab === 'party' ? 'active-nav' : ''}" aria-label="Отряд">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                    </a>
                    <div class="tooltip">Отряд</div>
                </li>
                <li class="icon-content">
                    <a href="profile.html#pokedex" data-social="dribbble" class="link ${activeTab === 'pokedex' ? 'active-nav' : ''}" aria-label="Покедекс">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
                    </a>
                    <div class="tooltip">Покедекс</div>
                </li>
                <li class="icon-content">
                    <a href="profile.html#profile" data-social="telegram" class="link ${activeTab === 'profile' ? 'active-nav' : ''}" aria-label="Профиль">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                    </a>
                    <div class="tooltip">Профиль</div>
                </li>
            </ul>
        </nav>`;
    }
};