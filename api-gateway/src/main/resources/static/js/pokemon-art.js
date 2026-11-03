// Картинки покемонов: id вида → файлы в static/img/pokemon/ (из документа «Коннцепт идея»).
//   src   — иллюстрация для карточки;
//   sheet — концепт-лист, показывается в окне досье;
//   fit: 'cover'   — картинка на весь верх карточки;
//   fit: 'contain' — персонаж на прозрачном фоне поверх пейзажа.
const POKEMON_ART = {
    1: { src: 'img/pokemon/01-krysyuk.jpg', fit: 'cover' }, // Серый грызун (Крысюк)
    2: { src: 'img/pokemon/02-red-panda.jpg', sheet: 'img/pokemon/sheets/02-red-panda.jpg', fit: 'cover' }, // Красная панда
    3: { src: 'img/pokemon/03-fire-brawler.jpg', sheet: 'img/pokemon/sheets/03-fire-brawler.jpg', fit: 'cover' }, // Огнеборец (Пихач)
    4: { src: 'img/pokemon/04-cinderhoot.jpg', sheet: 'img/pokemon/sheets/04-cinderhoot.jpg', fit: 'cover' }, // Пеплоух (Синдерхут)
    5: { src: 'img/pokemon/05-pyromandra.jpg', sheet: 'img/pokemon/sheets/05-pyromandra.jpg', fit: 'cover' }, // Пиромандра (Шаломандра)
    6: { src: 'img/pokemon/06-river-otter.jpg', sheet: 'img/pokemon/sheets/06-river-otter.jpg', fit: 'cover' }, // Речная выдра (Выдрочка)
    7: { src: 'img/pokemon/07-sir-somuel.jpg', sheet: 'img/pokemon/sheets/07-sir-somuel.jpg', fit: 'cover' }, // Сэр Сомуэль
    8: { src: 'img/pokemon/08-splashican.jpg', sheet: 'img/pokemon/sheets/08-splashican.jpg', fit: 'cover' }, // Плескан (Сплэшикан)
    9: { src: 'img/pokemon/09-castorax.jpg', sheet: 'img/pokemon/sheets/09-castorax.jpg', fit: 'cover' }, // Касторакс
    10: { src: 'img/pokemon/10-forest-guardian.jpg', sheet: 'img/pokemon/sheets/10-forest-guardian.jpg', fit: 'cover' }, // Лесной страж (Хранитель леса)
    11: { src: 'img/pokemon/11-forest-boar.jpg', sheet: 'img/pokemon/sheets/11-forest-boar.jpg', fit: 'cover' }, // Лесной вепрь (Свинохрюк)
    12: { src: 'img/pokemon/12-detective-raven.jpg', sheet: 'img/pokemon/sheets/12-detective-raven.jpg', fit: 'cover' }, // Ворон-сыщик (Арсен Воронян)
    13: { src: 'img/pokemon/13-cultured-bear.jpg', sheet: 'img/pokemon/sheets/13-cultured-bear.jpg', fit: 'cover' }, // Культурный медведь
};

function getPokemonArt(poke) {
    const speciesId = poke.speciesId ?? poke.id;
    const art = POKEMON_ART[speciesId];
    if (!art) return null;
    return typeof art === 'string' ? { src: art, fit: 'contain' } : { fit: 'contain', ...art };
}

// Показывает концепт-лист покемона в окне досье (если он есть)
function showSheet(imgId, poke) {
    const img = document.getElementById(imgId);
    if (!img) return;
    const art = getPokemonArt(poke);
    if (art && art.sheet) {
        img.src = art.sheet;
        img.classList.remove('hidden');
    } else {
        img.removeAttribute('src');
        img.classList.add('hidden');
    }
}
