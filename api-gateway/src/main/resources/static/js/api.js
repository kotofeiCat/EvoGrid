const API_BASE = '';
const CATCH_RADIUS_METERS = 70;

const GOOGLE_ICONS = {
    FIRE: 'local_fire_department',
    WATER: 'water_drop',
    FOREST: 'forest',
    NEUTRAL: 'pets',
    TRAINER: 'military_tech',
    XP: 'bolt',
    POKEBALL: 'catching_pokemon',
    COLLECTION: 'grid_view'
};

function getGoogleIconName(key) {
    return GOOGLE_ICONS[key] || 'pets';
}

function renderGoogleIcon(key, extraClass = '') {
    const name = getGoogleIconName(key);
    return `<span class="material-symbols-outlined ${extraClass}">${name}</span>`;
}

const VECTOR_PATHS = {
    TREE: `<svg viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg"><path d="M32,0C18.148,0,12,23.188,12,32c0,9.656,6.883,17.734,16,19.594V60c0,2.211,1.789,4,4,4s4-1.789,4-4v-8.406 C45.117,49.734,52,41.656,52,32C52,22.891,46.051,0,32,0z" fill="currentColor"/></svg>`,
    PIN: `<svg viewBox="0 0 64 64" width="13" height="13" fill="currentColor"><path d="M32,0C18.746,0,8,10.746,8,24c0,5.219,1.711,10.008,4.555,13.93l16,24 C29.414,63.332,30.664,64,32,64s2.586-0.668,3.328-1.781l16-24C54.289,34.008,56,29.219,56,24 C56,10.746,45.254,0,32,0z M32,32c-4.418,0-8-3.582-8-8s3.582-8,8-8s8,3.582,8,8S36.418,32,32,32z"/></svg>`
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

const ApiService = {
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
            throw new Error('Неверная почта или пароль. Проверьте введенные данные.');
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
        if (res.status === 409) {
            throw new Error('Аккаунт с такой почтой уже зарегистрирован.');
        }
        if (!res.ok) {
            throw new Error('Ошибка регистрации. Проверьте корректность почты и пароля.');
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
        const res = await fetch(`${API_BASE}/api/users/active-slot?slot=${slot}`, {
            method: 'PUT',
            credentials: 'include'
        });
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

    async updateLocation(userId, lat, lon) {
        const res = await fetch(`${API_BASE}/api/users/${userId}/location?lat=${lat}&lon=${lon}`, {
            method: 'PUT',
            credentials: 'include'
        });
        return res.json();
    },

    async assignPartySlot(userId, slot, ownedPokemonId) {
        const res = await fetch(`${API_BASE}/api/users/${userId}/party`, {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ slot, ownedPokemonId })
        });
        return res.json();
    },

    async getSpawnsNear(lat, lon) {
        const res = await fetch(`${API_BASE}/api/world/spawns?lat=${lat}&lon=${lon}`, {
            credentials: 'include'
        });
        return res.json();
    },

    async startBattle(userId, spawnId, partySlot) {
        const res = await fetch(`${API_BASE}/api/battles/start`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId, spawnId, partySlot })
        });
        if (!res.ok) throw new Error('Этот дикий покемон уже недоступен');
        return res.json();
    },

    async attackBattle(battleId) {
        const res = await fetch(`${API_BASE}/api/battles/${battleId}/attack`, {
            method: 'POST',
            credentials: 'include'
        });
        return res.json();
    },

    async catchPokemon(battleId) {
        const res = await fetch(`${API_BASE}/api/battles/${battleId}/catch`, {
            method: 'POST',
            credentials: 'include'
        });
        return res.json();
    }
};