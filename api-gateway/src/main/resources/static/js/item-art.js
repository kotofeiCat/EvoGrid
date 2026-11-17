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
        const isStar = code === 'SPHERE_STAR';
        const starEmblem = isStar
            ? `<polygon points="24,6.8 25.3,9.6 28.4,10 26.1,12.2 26.7,15.3 24,13.8 21.3,15.3 21.9,12.2 19.6,10 22.7,9.6" fill="#ffffff"/>
               <circle cx="24" cy="24" r="2.3" fill="#e0a100"/>`
            : '';
        return `
        <svg viewBox="0 0 48 48" aria-hidden="true">
            <circle cx="24" cy="24" r="20" fill="${light}" stroke="${main}" stroke-width="3"/>
            <path d="M4 24a20 20 0 0 1 40 0z" fill="${main}"/>
            <rect x="4" y="22.5" width="40" height="3" fill="#0f1f3d"/>
            <circle cx="24" cy="24" r="6" fill="#fff" stroke="#0f1f3d" stroke-width="3"/>
            ${starEmblem}
        </svg>`;
    }

    function potion(item) {
        const color = item.fullHeal ? '#e0a100' : (item.healHp >= 80 ? '#7a3cff' : '#e4572e');
        return `
        <svg viewBox="0 0 48 48" aria-hidden="true">
            <rect x="20" y="5" width="8" height="5" rx="2" fill="#d99b52" stroke="#0f1f3d" stroke-width="2.2"/>
            <rect x="19" y="10" width="10" height="5" rx="1.5" fill="#ffffff" stroke="#0f1f3d" stroke-width="2.2"/>
            <circle cx="24" cy="29" r="14" fill="${color}" stroke="#0f1f3d" stroke-width="2.5"/>
            <path d="M24 23v12M18 29h12" stroke="#ffffff" stroke-width="3.6" stroke-linecap="round"/>
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