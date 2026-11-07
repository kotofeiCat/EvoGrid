let isLoginMode = false;
let pendingEmail = '';
let pendingPassword = '';
let selectedStarterId = 2;

const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

window.addEventListener('DOMContentLoaded', async () => {
    try {
        await ApiService.getCurrentUser();
        window.location.href = 'game.html';
    } catch {
    }
});

function toggleAuthMode() {
    isLoginMode = !isLoginMode;
    document.getElementById('auth-title').textContent = isLoginMode ? 'Вход в игру' : 'Регистрация';
    document.getElementById('auth-subtitle').textContent = isLoginMode
        ? 'Введи свою почту и пароль для входа'
        : 'Создай аккаунт тренера по электронной почте';
    document.getElementById('auth-btn-label').textContent = isLoginMode
        ? 'Войти в мир'
        : 'Выбрать стартовика';
    document.getElementById('auth-switch-text').innerHTML = isLoginMode
        ? 'Нет аккаунта? <a onclick="toggleAuthMode()">Зарегистрироваться</a>'
        : 'Уже есть аккаунт? <a onclick="toggleAuthMode()">Войти</a>';
}

async function handleAuthForm(e) {
    e.preventDefault();

    pendingEmail = document.getElementById('email-input').value.trim().toLowerCase();
    pendingPassword = document.getElementById('password-input').value;

    if (!EMAIL_REGEX.test(pendingEmail)) {
        UiComponents.showErrorAlert('Некорректная почта', 'Введите настоящий email (например, trainer@mail.ru).');
        return;
    }
    if (pendingPassword.length < 4) {
        UiComponents.showErrorAlert('Слабый пароль', 'Пароль должен содержать минимум 4 символа.');
        return;
    }

    if (isLoginMode) {
        try {
            await ApiService.login(pendingEmail, pendingPassword);
            window.location.href = 'game.html';
        } catch (err) {
            UiComponents.showErrorAlert('Ошибка входа', err.message);
        }
    } else {
        try {
            const isTaken = await ApiService.checkEmailTaken(pendingEmail);
            if (isTaken) {
                UiComponents.showErrorAlert('Почта уже занята', `Аккаунт "${pendingEmail}" уже существует. Переключитесь на вкладку входа.`);
                return;
            }
        } catch {
        }

        await renderStartersSelection();
        document.getElementById('step-auth').classList.add('hidden');
        document.getElementById('step-starter').classList.remove('hidden');
    }
}

async function renderStartersSelection() {
    const container = document.getElementById('starters-row');
    let starters = [];
    try {
        starters = await ApiService.getStarters();
    } catch {
        starters = [
            { id: 2, name: 'Красная панда', element: 'FIRE', archetype: 'Средний ДПС', baseHp: 70, baseAttack: 35, baseDefense: 20, baseSpeed: 25 },
            { id: 6, name: 'Выдрочка', element: 'WATER', archetype: 'Танк', baseHp: 85, baseAttack: 20, baseDefense: 35, baseSpeed: 10 },
            { id: 10, name: 'Рогатый Куколд', element: 'FOREST', archetype: 'Средний ДПС', baseHp: 70, baseAttack: 35, baseDefense: 20, baseSpeed: 25 }
        ];
    }

    container.innerHTML = starters.map(s => `
        <div onclick="selectStarter(${s.id})">
            ${UiComponents.createPokemonLandscapeCard(s, s.id === selectedStarterId)}
        </div>
    `).join('');
}

function selectStarter(id) {
    selectedStarterId = id;
    renderStartersSelection();
}

function backToAuth() {
    document.getElementById('step-starter').classList.add('hidden');
    document.getElementById('step-auth').classList.remove('hidden');
}

function goToGeoStep() {
    document.getElementById('step-starter').classList.add('hidden');
    document.getElementById('step-geo').classList.remove('hidden');
}

function startWithRealGps() {
    const msg = document.getElementById('geo-status-msg');
    if (!navigator.geolocation) {
        startWithDemoCoords();
        return;
    }
    msg.textContent = 'Запрашиваем GPS-координаты устройства...';
    navigator.geolocation.getCurrentPosition(
        (pos) => finishRegistration(pos.coords.latitude, pos.coords.longitude),
        () => startWithDemoCoords(),
        { enableHighAccuracy: true, timeout: 7000 }
    );
}

function startWithDemoCoords() {
    finishRegistration(55.7558, 37.6173);
}

async function finishRegistration(lat, lon) {
    try {
        await ApiService.register({
            email: pendingEmail,
            password: pendingPassword,
            starterSpeciesId: selectedStarterId,
            latitude: lat,
            longitude: lon
        });
        window.location.href = 'game.html';
    } catch (err) {
        UiComponents.showErrorAlert('Ошибка регистрации', err.message);
        document.getElementById('step-geo').classList.add('hidden');
        document.getElementById('step-auth').classList.remove('hidden');
    }
}