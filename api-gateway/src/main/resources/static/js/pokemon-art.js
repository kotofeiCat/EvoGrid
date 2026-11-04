// Картинки покемонов: id вида → файлы в static/img/pokemon/ (из документа «Коннцепт идея»).
//   src   — иллюстрация для карточки;
//   fit: 'cover'   — картинка на весь верх карточки;
//   fit: 'contain' — персонаж на прозрачном фоне поверх пейзажа.
const POKEMON_ART = {
    1: { src: 'img/pokemon/01-krysyuk.jpg', fit: 'cover' }, // Серый грызун (Крысюк)
    2: { src: 'img/pokemon/02-red-panda.jpg', fit: 'cover' }, // Красная панда
    3: { src: 'img/pokemon/03-fire-brawler.jpg', fit: 'cover' }, // Огнеборец (Пихач)
    4: { src: 'img/pokemon/04-cinderhoot.jpg', fit: 'cover' }, // Пеплоух (Синдерхут)
    5: { src: 'img/pokemon/05-pyromandra.jpg', fit: 'cover' }, // Пиромандра (Шаломандра)
    6: { src: 'img/pokemon/06-river-otter.jpg', fit: 'cover' }, // Речная выдра (Выдрочка)
    7: { src: 'img/pokemon/07-sir-somuel.jpg', fit: 'cover' }, // Сэр Сомуэль
    8: { src: 'img/pokemon/08-splashican.jpg', fit: 'cover' }, // Плескан (Сплэшикан)
    9: { src: 'img/pokemon/09-castorax.jpg', fit: 'cover' }, // Касторакс
    10: { src: 'img/pokemon/10-forest-guardian.jpg', fit: 'cover' }, // Лесной страж (Хранитель леса)
    11: { src: 'img/pokemon/11-forest-boar.jpg', fit: 'cover' }, // Лесной вепрь (Свинохрюк)
    12: { src: 'img/pokemon/12-detective-raven.jpg', fit: 'cover' }, // Ворон-сыщик (Арсен Воронян)
    13: { src: 'img/pokemon/13-cultured-bear.jpg', fit: 'cover' }, // Культурный медведь
};

function getPokemonArt(poke) {
    const speciesId = poke.speciesId ?? poke.id;
    const art = POKEMON_ART[speciesId];
    if (!art) return null;
    return typeof art === 'string' ? { src: art, fit: 'contain' } : { fit: 'contain', ...art };
}

