let shopItems = [];
let shopBag = null;
let shopKind = 'CAPTURE';
let buying = false;

const TAB_ORDER = ['game.html', 'profile.html#party', 'profile.html#pokedex', 'shop.html', 'profile.html#profile'];

window.addEventListener('DOMContentLoaded', async () => {
    document.getElementById('dock-mount').innerHTML = UiComponents.renderBottomDock('shop');

    try {
        await ApiService.getCurrentUser();
    } catch {
        window.location.href = 'index.html';
        return;
    }

    document.querySelectorAll('.shop-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            shopKind = tab.dataset.kind;
            document.querySelectorAll('.shop-tab').forEach(t => t.classList.toggle('active', t === tab));
            renderShop();
        });
    });

    Gestures.enableTabSwipe(document.body, () => 3, (index) => {
        window.location.href = TAB_ORDER[index];
    }, TAB_ORDER.length);

    await loadShop();
});

async function loadShop() {
    try {
        [shopItems, shopBag] = await Promise.all([ApiService.getShop(), ApiService.getInventory()]);
    } catch (err) {
        document.getElementById('shop-list').innerHTML =
            `<div class="empty-hint">${err.message}. Попробуйте обновить страницу через несколько секунд.</div>`;
        return;
    }
    renderShop();
}

function owned(code) {
    const slot = (shopBag?.items || []).find(s => s.item.code === code);
    return slot ? slot.quantity : 0;
}

function renderShop() {
    document.getElementById('shop-coins').textContent = shopBag ? shopBag.coins : '—';
    const coins = shopBag ? shopBag.coins : 0;
    const visible = shopItems.filter(item =>
        shopKind === 'CAPTURE' ? item.kind === 'CAPTURE' : item.kind !== 'CAPTURE');

    document.getElementById('shop-list').innerHTML = visible.map(item => {
        const affordable = coins >= item.price;
        const have = owned(item.code);
        return `
        <article class="shop-card">
            <div class="shop-card-art">${ItemArt.icon(item)}</div>
            <div class="shop-card-body">
                <h3>${item.name}</h3>
                <p>${item.description || ''}</p>
                <span class="shop-card-owned">${have > 0 ? `В сумке: ${have}` : 'В сумке нет'}</span>
            </div>
            <div class="shop-card-buy">
                <span class="shop-price">${item.price} <small>монет</small></span>
                <div class="shop-buy-row">
                    <button type="button" class="uiv-btn primary" ${affordable && !buying ? '' : 'disabled'}
                            onclick="buy('${item.code}', 1)">
                        <span class="button_top">Купить</span>
                    </button>
                    <button type="button" class="uiv-btn" ${coins >= item.price * 5 && !buying ? '' : 'disabled'}
                            onclick="buy('${item.code}', 5)" title="Купить 5 штук">
                        <span class="button_top">×5</span>
                    </button>
                </div>
            </div>
        </article>`;
    }).join('');
}

async function buy(code, quantity) {
    if (buying) return;
    buying = true;
    renderShop();
    try {
        shopBag = await ApiService.buyItem(code, quantity);
        const item = shopItems.find(i => i.code === code);
        showPurchaseToast(`${item ? item.name : 'Предмет'} ×${quantity} — в сумке`);
    } catch (err) {
        UiComponents.showErrorAlert('Покупка не прошла', err.message);
    } finally {
        buying = false;
        renderShop();
    }
}

function showPurchaseToast(text) {
    const toast = document.createElement('div');
    toast.className = 'purchase-toast';
    toast.textContent = text;
    document.body.appendChild(toast);
    setTimeout(() => toast.classList.add('leaving'), 1600);
    setTimeout(() => toast.remove(), 2000);
}
