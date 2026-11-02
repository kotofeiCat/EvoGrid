const UiComponents = {
    /**
     * Показывает всплывающее уведомление об ошибке (From Uiverse.io by seyed-mohsen-mousavi)
     */
    showErrorAlert(title = 'Попробуйте снова', description = 'Проверьте введенные данные') {
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
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9 3.75h.008v.008H12v-.008Z"></path>
                    </svg>
                </div>
                <div>
                    <p class="error-alert-title">${title}</p>
                    <p class="error-alert-desc">${description}</p>
                </div>
            </div>
            <button class="error-alert-close" aria-label="Закрыть">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
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

    /**
     * Генерирует карточку покемона (Uiverse by KSAplay) с иконками Google Fonts
     */
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
            </section>

            <section class="content-section">
                <div class="weather-info">
                    <div class="left-side">
                        <div class="icon">
                            ${renderGoogleIcon(elem)}
                        </div>
                        <p>${elem}</p>
                    </div>
                    <div class="right-side">
                        <div class="location">
                            ${VECTOR_PATHS.PIN}
                            <span>${poke.name}</span>
                        </div>
                        <p>${poke.archetype || 'Боец'}</p>
                        <p class="temperature">${hp} HP</p>
                    </div>
                </div>
                <div class="forecast">
                    <div>
                        <p>Атака (ATK)</p>
                        <p>${atk}</p>
                    </div>
                    <div class="separator"></div>
                    <div>
                        <p>Защита (DEF)</p>
                        <p>${def}</p>
                    </div>
                    <div class="separator"></div>
                    <div>
                        <p>Скорость (SPD)</p>
                        <p>${spd}</p>
                    </div>
                </div>
            </section>
        </div>`;
    },

    /**
     * Нижнее меню навигации (Uiverse by firemonste_8052) с иконками Google Fonts
     */
    renderBottomDock(activeTab) {
        return `
        <nav class="bottom-dock-wrap">
            <ul class="example-1">
                <li class="icon-content">
                    <a href="game.html" data-social="spotify" class="link ${activeTab === 'map' ? 'active-nav' : ''}" aria-label="Карта">
                        <span class="material-symbols-outlined">explore</span>
                    </a>
                    <div class="tooltip">Карта</div>
                </li>
                <li class="icon-content">
                    <a href="profile.html#party" data-social="pinterest" class="link ${activeTab === 'party' ? 'active-nav' : ''}" aria-label="Пачка">
                        <span class="material-symbols-outlined">shield</span>
                    </a>
                    <div class="tooltip">Пачка</div>
                </li>
                <li class="icon-content">
                    <a href="profile.html#pokedex" data-social="dribbble" class="link ${activeTab === 'pokedex' ? 'active-nav' : ''}" aria-label="Покедекс">
                        <span class="material-symbols-outlined">menu_book</span>
                    </a>
                    <div class="tooltip">Покедекс</div>
                </li>
                <li class="icon-content">
                    <a href="profile.html#profile" data-social="telegram" class="link ${activeTab === 'profile' ? 'active-nav' : ''}" aria-label="Профиль">
                        <span class="material-symbols-outlined">person</span>
                    </a>
                    <div class="tooltip">Профиль</div>
                </li>
            </ul>
        </nav>`;
    }
};