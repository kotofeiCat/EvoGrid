/**
 * Иконки предметов магазина: сферы окрашены по силе/стихии, лечилки — флаконы, оживление — искра.
 * Рисуются SVG прямо в коде, без картинок.
 */
const ItemArt = (() => {
    const SPHERE_COLORS = {
        SPHERE_BASIC: ['#1f5eff', '#e3ebff'],
        SPHERE_GREAT: ['#7a3cff', '#efe6ff'],
        SPHERE_STAR: ['#e0a100', '#fff4d1'],
        SPHERE_FIRE: ['#e4572e', '#ffe6dc'],
        SPHERE_WATER: ['#1c8fd6', '#ddf1ff'],
        SPHERE_FOREST: ['#2f9e57', '#def5e6']
    };

    function sphere(code) {
        const [main, light] = SPHERE_COLORS[code] || SPHERE_COLORS.SPHERE_BASIC;
        const star = code === 'SPHERE_STAR'
            ? '<path d="M24 13l2.4 4.9 5.4.8-3.9 3.8.9 5.4-4.8-2.6-4.8 2.6.9-5.4-3.9-3.8 5.4-.8z" fill="#fff" opacity=".9"/>'
            : '';
        return `
        <svg viewBox="0 0 48 48" aria-hidden="true">
            <circle cx="24" cy="24" r="20" fill="${light}" stroke="${main}" stroke-width="3"/>
            <path d="M4 24a20 20 0 0 1 40 0z" fill="${main}"/>
            <rect x="4" y="22.5" width="40" height="3" fill="#0f1f3d"/>
            <circle cx="24" cy="24" r="6" fill="#fff" stroke="#0f1f3d" stroke-width="3"/>
            ${star}
        </svg>`;
    }

    function potion(item) {
        const color = item.fullHeal ? '#e0a100' : (item.healHp >= 80 ? '#7a3cff' : '#e4572e');
        return `
        <svg viewBox="0 0 48 48" aria-hidden="true">
            <rect x="19" y="4" width="10" height="8" rx="2" fill="#8b97b0"/>
            <path d="M18 12h12v6l7 10v10a6 6 0 0 1-6 6H17a6 6 0 0 1-6-6V28l7-10z" fill="#ffffff" stroke="#0f1f3d" stroke-width="2.5"/>
            <path d="M12.5 30h23v8a4.5 4.5 0 0 1-4.5 4.5H17a4.5 4.5 0 0 1-4.5-4.5z" fill="${color}"/>
            <path d="M24 31.5v8M20 35.5h8" stroke="#fff" stroke-width="2.5" stroke-linecap="round"/>
        </svg>`;
    }

    function revive() {
        return `
        <svg viewBox="0 0 48 48" aria-hidden="true">
            <circle cx="24" cy="24" r="20" fill="#fff4d1"/>
            <path d="M24 6l4 12 12 1-9.5 7.5L34 40l-10-7-10 7 3.5-13.5L8 19l12-1z" fill="#ffc93c" stroke="#9a6400" stroke-width="2" stroke-linejoin="round"/>
        </svg>`;
    }

    function icon(item) {
        let svg;
        if (item.kind === 'CAPTURE') svg = sphere(item.code);
        else if (item.kind === 'REVIVE') svg = revive();
        else svg = potion(item);
        return `<span class="item-icon">${svg}</span>`;
    }

    return { icon };
})();
