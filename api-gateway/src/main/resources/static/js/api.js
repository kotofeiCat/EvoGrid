const API_BASE = '';
const CATCH_RADIUS_METERS = 70;
const DEFAULT_YANDEX_KEY = 'b2580f1d-2034-4383-8b7b-2b9052bb1e32';

const SVG_ICONS = {
    FIRE: `<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M13.5.67s.74 2.65.74 4.8c0 2.06-1.35 3.73-3.41 3.73-2.07 0-3.63-1.67-3.63-3.73l.03-.36C5.21 7.51 4 10.62 4 14c0 4.42 3.58 8 8 8s8-3.58 8-8C20 8.61 17.41 3.8 13.5.67zM11.71 19c-1.78 0-3.22-1.4-3.22-3.14 0-1.62 1.05-2.76 2.81-3.12 1.77-.36 3.6-1.21 4.62-2.58.39 1.29.59 2.65.59 4.04 0 2.65-2.15 4.8-4.8 4.8z"/></svg>`,
    WATER: `<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/></svg>`,
    FOREST: `<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M16 12L12 4 8 12h2.5L7 18h4v4h2v-4h4l-3.5-6z"/></svg>`,
    NEUTRAL: `<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><circle cx="8.5" cy="8.5" r="2.2"/><circle cx="15.5" cy="8.5" r="2.2"/><circle cx="5" cy="13" r="2"/><circle cx="19" cy="13" r="2"/><path d="M12 12.5c-2.8 0-5 2-5 4.5 0 1.7 1.3 2.5 3 2.5h4c1.7 0 3-.8 3-2.5 0-2.5-2.2-4.5-5-4.5z"/></svg>`
};

function renderGoogleIcon(key, extraClass = '') {
    const svg = SVG_ICONS[key] || SVG_ICONS.NEUTRAL;
    return `<span class="elem-svg-icon ${extraClass}">${svg}</span>`;
}

const VECTOR_PATHS = {
    TREE: `<svg viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg"><path d="M32,0C18.148,0,12,23.188,12,32c0,9.656,6.883,17.734,16,19.594V60c0,2.211,1.789,4,4,4s4-1.789,4-4v-8.406 C45.117,49.734,52,41.656,52,32C52,22.891,46.051,0,32,0z" fill="currentColor"/></svg>`,
    PIN: `<svg viewBox="0 0 64 64" width="12" height="12" fill="currentColor"><path d="M32,0C18.746,0,8,10.746,8,24c0,5.219,1.711,10.008,4.555,13.93l16,24 C29.414,63.332,30.664,64,32,64s2.586-0.668,3.328-1.781l16-24C54.289,34.008,56,29.219,56,24 C56,10.746,45.254,0,32,0z M32,32c-4.418,0-8-3.582-8-8s3.582-8,8-8s8,3.582,8,8S36.418,32,32,32z"/></svg>`
};

function calculateDistanceMeters(lat1, lon1, lat2, lon2) {
    const R = 6371000;
    const toRad = (deg) => (deg * Math.PI) / 180;
    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);
    const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

let ymapsScriptPromise = null;

async function loadYandexMapsScript() {
    if (window.ymaps) {
        await new Promise((resolve) => window.ymaps.ready(resolve));
        return window.ymaps;
    }
    if (ymapsScriptPromise) {
        return ymapsScriptPromise;
    }

    ymapsScriptPromise = (async () => {
        let apiKey = DEFAULT_YANDEX_KEY;
        try {
            const cfg = await ApiService.getPublicConfig();
            if (cfg && cfg.yandexMapsApiKey) {
                apiKey = cfg.yandexMapsApiKey.trim();
            }
        } catch {
            // Резервный ключ
        }

        await new Promise((resolve, reject) => {
            const script = document.createElement('script');
            // Подключаем API 2.1 вместо v3
            script.src = `https://api-maps.yandex.ru/2.1/?apikey=${encodeURIComponent(apiKey)}&lang=ru_RU`;
            script.async = true;
            script.onload = resolve;
            script.onerror = () => reject(new Error('Не удалось загрузить Яндекс Карты API 2.1'));
            document.head.appendChild(script);
        });

        await new Promise((resolve) => window.ymaps.ready(resolve));
        return window.ymaps;
    })();

    return ymapsScriptPromise;
}

async function readError(res, fallback) {
    try {
        const body = await res.json();
        return body.message || body.reason || fallback;
    } catch {
        return fallback;
    }
}

const ApiService = {
    async getPublicConfig() {
        const res = await fetch(`${API_BASE}/api/config/public`, {
            credentials: 'include'
        });
        if (!res.ok) return { yandexMapsApiKey: DEFAULT_YANDEX_KEY };
        return res.json();
    },

    async checkEmailTaken(email) {
        const res = await fetch(`${API_BASE}/api/users/check-email?email=${encodeURIComponent(email)}`, {
            credentials: 'include'
        });
        if (!res.ok) return false;
        const data = await res.json();
        return Boolean(data.exists);
    },

    async getCurrentUser() {
        const res = await fetch(`${API_BASE}/api/users/me`, {
            credentials: 'include'
        });
        if (!res.ok) throw new Error('Не авторизован');
        return res.json();
    },

    async login(email, password) {
        const res = await fetch(`${API_BASE}/api/users/login`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        if (!res.ok) {
            throw new Error('Неверный email или пароль.');
        }
        return res.json();
    },

    async register(payload) {
        const res = await fetch(`${API_BASE}/api/users/register`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!res.ok) {
            throw new Error(await readError(res, 'Ошибка регистрации'));
        }
        return res.json();
    },

    async logout() {
        await fetch(`${API_BASE}/api/users/logout`, {
            method: 'POST',
            credentials: 'include'
        }).catch(() => {});
        window.location.href = 'index.html';
    },

    async selectActiveSlot(slot) {
        const res = await fetch(`${API_BASE}/api/users/me/active-slot?slot=${slot}`, {
            method: 'PUT',
            credentials: 'include'
        });
        if (!res.ok) throw new Error(await readError(res, 'Не удалось выбрать бойца'));
        return res.json();
    },

    async getAllSpecies() {
        const res = await fetch(`${API_BASE}/api/pokemons`, { credentials: 'include' });
        return res.json();
    },

    async getStarters() {
        const res = await fetch(`${API_BASE}/api/pokemons/starters`, { credentials: 'include' });
        return res.json();
    },

    async getPokemonAbilities(speciesId) {
        const res = await fetch(`${API_BASE}/api/pokemons/${encodeURIComponent(speciesId)}/abilities`, {
            credentials: 'include'
        });
        if (!res.ok) throw new Error('Не удалось загрузить способности');
        return res.json();
    },

    async updateLocation(lat, lon) {
        const res = await fetch(`${API_BASE}/api/users/me/location?lat=${lat}&lon=${lon}`, {
            method: 'PUT',
            credentials: 'include'
        });
        if (!res.ok) throw new Error(await readError(res, 'Не удалось обновить позицию'));
        return res.json();
    },

    async assignPartySlot(slot, ownedPokemonId) {
        const res = await fetch(`${API_BASE}/api/users/me/party`, {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ slot, ownedPokemonId })
        });
        if (!res.ok) throw new Error(await readError(res, 'Не удалось изменить отряд'));
        return res.json();
    },

    async getSpawnsNear(lat, lon) {
        const res = await fetch(`${API_BASE}/api/world/spawns?lat=${lat}&lon=${lon}`, {
            credentials: 'include'
        });
        if (!res.ok) throw new Error(await readError(res, 'Не удалось загрузить эвонов рядом'));
        return res.json();
    },

    async startBattle(spawnId, partySlot) {
        const res = await fetch(`${API_BASE}/api/battles/start`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ spawnId, partySlot })
        });
        if (!res.ok) throw new Error(await readError(res, 'Не удалось начать бой'));
        return res.json();
    },

    async attackBattle(battleId, slot = 1) {
        const res = await fetch(`${API_BASE}/api/battles/${battleId}/attack?slot=${encodeURIComponent(slot)}`, {
            method: 'POST',
            credentials: 'include'
        });
        if (!res.ok) throw new Error(await readError(res, 'Не удалось сделать ход'));
        return res.json();
    },

    async battleTimeout(battleId) {
        const res = await fetch(`${API_BASE}/api/battles/${battleId}/timeout`, {
            method: 'POST',
            credentials: 'include'
        });
        if (!res.ok) throw new Error('Не удалось завершить ход');
        return res.json();
    },

    async throwSphere(battleId, sphereCode = 'SPHERE_BASIC') {
        const res = await fetch(`${API_BASE}/api/battles/${battleId}/catch?sphere=${encodeURIComponent(sphereCode)}`, {
            method: 'POST',
            credentials: 'include'
        });
        if (!res.ok) throw new Error(await readError(res, 'Не удалось бросить сферу'));
        return res.json();
    },

    async switchFighter(battleId, index) {
        const res = await fetch(`${API_BASE}/api/battles/${battleId}/switch?index=${encodeURIComponent(index)}`, {
            method: 'POST',
            credentials: 'include'
        });
        if (!res.ok) throw new Error(await readError(res, 'Не удалось сменить бойца'));
        return res.json();
    },

    async useBattleItem(battleId, itemCode, targetIndex) {
        const res = await fetch(`${API_BASE}/api/battles/${battleId}/item`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ itemCode, targetIndex })
        });
        if (!res.ok) throw new Error(await readError(res, 'Не удалось применить предмет'));
        return res.json();
    },

    // ---------- магазин и сумка (inventory-service) ----------

    async getShop() {
        const res = await fetch(`${API_BASE}/api/shop`, { credentials: 'include' });
        if (!res.ok) throw new Error(await readError(res, 'Магазин недоступен'));
        return res.json();
    },

    async getInventory() {
        const res = await fetch(`${API_BASE}/api/inventory`, { credentials: 'include' });
        if (!res.ok) throw new Error(await readError(res, 'Сумка недоступна'));
        return res.json();
    },

    async buyItem(itemCode, quantity = 1) {
        const res = await fetch(`${API_BASE}/api/shop/buy`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ itemCode, quantity })
        });
        if (!res.ok) throw new Error(await readError(res, 'Не удалось купить'));
        return res.json();
    },

    async useItemOnPokemon(ownedPokemonId, itemCode) {
        const res = await fetch(`${API_BASE}/api/users/me/pokemons/${encodeURIComponent(ownedPokemonId)}/items`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ itemCode })
        });
        if (!res.ok) throw new Error(await readError(res, 'Не удалось применить предмет'));
        return res.json();
    }
};