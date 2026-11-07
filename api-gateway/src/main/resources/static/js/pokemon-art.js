const POKEMON_ART = {
    1: { src: 'img/pokemon/01-krysyuk.jpg', fit: 'cover' },
    2: { src: 'img/pokemon/02-red-panda.jpg', fit: 'cover' },
    3: { src: 'img/pokemon/03-fire-brawler.jpg', fit: 'cover' },
    4: { src: 'img/pokemon/04-cinderhoot.jpg', fit: 'cover' },
    5: { src: 'img/pokemon/05-pyromandra.jpg', fit: 'cover' },
    6: { src: 'img/pokemon/06-river-otter.jpg', fit: 'cover' },
    7: { src: 'img/pokemon/07-sir-somuel.jpg', fit: 'cover' },
    8: { src: 'img/pokemon/08-splashican.jpg', fit: 'cover' },
    9: { src: 'img/pokemon/09-castorax.jpg', fit: 'cover' },
    10: { src: 'img/pokemon/10-forest-guardian.jpg', fit: 'cover' },
    11: { src: 'img/pokemon/11-forest-boar.jpg', fit: 'cover' },
    12: { src: 'img/pokemon/12-detective-raven.jpg', fit: 'cover' },
    13: { src: 'img/pokemon/13-cultured-bear.jpg', fit: 'cover' },
};

function getPokemonArt(poke) {
    const speciesId = poke.speciesId ?? poke.id;
    const art = POKEMON_ART[speciesId];
    if (!art) return null;
    return typeof art === 'string' ? { src: art, fit: 'contain' } : { fit: 'contain', ...art };
}

