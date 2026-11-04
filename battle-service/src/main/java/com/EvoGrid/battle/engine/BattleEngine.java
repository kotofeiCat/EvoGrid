package com.EvoGrid.battle.engine;

import com.EvoGrid.battle.session.BattleSession;
import com.EvoGrid.common.dto.AbilityDto;
import com.EvoGrid.common.game.StatusEffectType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import static com.EvoGrid.common.game.StatusEffectType.*;

/**
 * Правила боя: порядок ходов, урон, статусы, тики конца раунда.
 * Ничего не знает о БД, HTTP и других сервисах — только меняет бойцов и пишет лог.
 */
@Component
public class BattleEngine {

    static final double STAB_MULTIPLIER = 1.25;
    static final double CRIT_MULTIPLIER = 1.50;
    static final int BASE_CRIT_CHANCE = 10;
    static final int EVASION_CHANCE = 65;

    private static final Set<String> ADVANTAGE = Set.of("FIRE->FOREST", "FOREST->WATER", "WATER->FIRE");

    /** Эффекты, которые просто истекают в конце раунда (−1 ход). */
    private static final Set<StatusEffectType> EXPIRING = EnumSet.of(
            SLOW, BLIND, CRIT_IMMUNE, CC_IMMUNE, ATK_UP, DEF_UP, THORNS, RAGE_SPEED_PIERCE);

    /** Дебаффы, которые накладываются на противника (действуют N ходов). */
    private static final Set<StatusEffectType> TIMED_DEBUFFS = EnumSet.of(BURN, POISON, SLOW, BLIND);

    /** Контроль: пропуск хода. */
    private static final Set<StatusEffectType> CONTROL = EnumSet.of(SLEEP, STUN);

    /** Баффы на себя с длительностью. */
    private static final Set<StatusEffectType> TIMED_BUFFS = EnumSet.of(
            REGEN, ATK_UP, DEF_UP, THORNS, RAGE_SPEED_PIERCE, NEXT_HIT_BURN,
            WATER_BOOST, GUARANTEED_CRIT, CRIT_IMMUNE, CC_IMMUNE);

    private final Supplier<Random> random;

    public BattleEngine() {
        this(ThreadLocalRandom::current);
    }

    public BattleEngine(Supplier<Random> random) {
        this.random = random;
    }

    // ==================== РАУНДЫ ====================

    /** Полный раунд: оба бойца действуют в порядке скорости, затем тики статусов. */
    public void playRound(BattleSession b, AbilityDto playerAbility, AbilityDto enemyAbility) {
        Combatant player = b.player();
        Combatant enemy = b.enemy();

        int pSpd = player.effectiveSpeed();
        int eSpd = enemy.effectiveSpeed();
        boolean playerFirst = pSpd != eSpd ? pSpd > eSpd : random.get().nextBoolean();

        b.log(String.format("— Ход %d · скорость: %s %d, %s %d —",
                b.getTurnNumber(), player.name, pSpd, enemy.name, eSpd));

        if (playerFirst) {
            act(b, player, enemy, playerAbility, true);
            if (bothAlive(b)) act(b, enemy, player, enemyAbility, false);
        } else {
            act(b, enemy, player, enemyAbility, false);
            if (bothAlive(b)) act(b, player, enemy, playerAbility, true);
        }

        if (bothAlive(b)) endOfRound(b);
    }

    /** Раунд, в котором игрок не атаковал (сменил бойца, применил предмет или бросил сферу): ходит только враг. */
    public void enemyOnlyRound(BattleSession b, AbilityDto enemyAbility) {
        act(b, b.enemy(), b.player(), enemyAbility, false);
        if (bothAlive(b)) endOfRound(b);
    }

    private boolean bothAlive(BattleSession b) {
        return b.player().alive() && b.enemy().alive();
    }

    // ==================== ДЕЙСТВИЕ ОДНОГО БОЙЦА ====================

    private void act(BattleSession b, Combatant actor, Combatant target, AbilityDto ability, boolean actorIsPlayer) {
        String who = label(actor, actorIsPlayer);

        // Защитная стойка держится только до следующего действия бойца
        actor.blockPercent = 0;
        actor.clearEffect(EVASION);

        if (actor.has(SLEEP)) {
            actor.decrementEffect(SLEEP);
            b.log(who + " крепко спит и пропускает ход.");
            return;
        }
        if (actor.has(STUN)) {
            actor.decrementEffect(STUN);
            b.log(who + " оглушён и пропускает ход.");
            return;
        }

        // +1, потому что в конце этого же раунда перезарядка уменьшится на 1
        if (ability.maxCooldown() > 0) {
            actor.startCooldown(ability.slot(), ability.maxCooldown() + 1);
        }

        if (ability.targetsSelf()) {
            useOnSelf(b, actor, target, ability, who);
        } else {
            attack(b, actor, target, ability, who, label(target, !actorIsPlayer));
        }
    }

    private void useOnSelf(BattleSession b, Combatant actor, Combatant opponent, AbilityDto ab, String who) {
        List<String> notes = new ArrayList<>();

        if (ab.selfDamagePercentMaxHp() > 0) {
            int sacrifice = actor.percentOfMaxHp(ab.selfDamagePercentMaxHp());
            actor.hp = Math.max(1, actor.hp - sacrifice);
            notes.add("жертвует " + sacrifice + " HP");
        }

        if (ab.healPercentMaxHp() > 0) {
            int pct = ab.healPercentMaxHp();
            if (ab.doubleHealLowHp() && actor.hp * 4 <= actor.maxHp) {
                pct *= 2;
                notes.add("второе дыхание — лечение x2");
            }
            int healed = actor.heal(actor.percentOfMaxHp(pct));
            notes.add(String.format("восстанавливает %d HP (%d/%d)", healed, actor.hp, actor.maxHp));
        }

        if (ab.blockPercent() > 0) {
            actor.blockPercent = ab.blockPercent();
            notes.add("ставит блок " + ab.blockPercent() + "%");
        }

        if (ab.evasionChancePercent() > 0) {
            actor.setEffect(EVASION, 1);
            notes.add("готовится увернуться (" + EVASION_CHANCE + "%)");
        }

        applyEffect(actor, opponent, ab.primaryEffect(), ab.primaryEffectChancePercent(), ab.primaryEffectDuration(), notes);
        applyEffect(actor, opponent, ab.secondaryEffect(), 100, ab.secondaryEffectDuration(), notes);

        String details = notes.isEmpty() ? ab.description() : String.join(", ", notes);
        b.log(String.format("%s использует «%s»: %s.", who, ab.name(), details));
    }

    private void attack(BattleSession b, Combatant actor, Combatant target, AbilityDto ab, String who, String whom) {
        Random rnd = random.get();

        // 1. Точность (ослепление −35%)
        double hitChance = ab.accuracy() / 100.0 * (actor.has(BLIND) ? 0.65 : 1.0);
        if (rnd.nextDouble() > hitChance) {
            b.log(String.format("%s использует «%s», но промахивается.", who, ab.name()));
            return;
        }

        // 2. Уворот цели (одноразовый)
        if (target.has(EVASION)) {
            target.clearEffect(EVASION);
            if (rnd.nextInt(100) < EVASION_CHANCE) {
                b.log(String.format("%s использует «%s», но %s уворачивается!", who, ab.name(), whom));
                return;
            }
        }

        // 3. Ракушечный барьер цели поглощает удар целиком
        if (target.has(SHELL_BARRIER)) {
            target.clearEffect(SHELL_BARRIER);
            b.log(String.format("%s использует «%s», но барьер %s поглощает удар.", who, ab.name(), whom));
            return;
        }

        // 4. Урон
        String moveElement = moveElement(ab);
        boolean stab = hasStab(ab, actor);
        double elementMult = elementMultiplier(moveElement, target.element);

        double waterBoost = 1.0;
        if (actor.has(WATER_BOOST) && "WATER".equalsIgnoreCase(moveElement)) {
            waterBoost = 1.35;
            actor.clearEffect(WATER_BOOST);
        }

        boolean crit = false;
        boolean critImmune = target.has(CRIT_IMMUNE);
        if (actor.has(GUARANTEED_CRIT)) {
            actor.clearEffect(GUARANTEED_CRIT);
            crit = !critImmune;
        } else if (!critImmune && rnd.nextInt(100) < BASE_CRIT_CHANCE) {
            crit = true;
        }

        double blockMult = 1.0;
        String blockNote = null;
        if (target.blockPercent > 0) {
            if (actor.has(RAGE_SPEED_PIERCE)) {
                blockNote = "блок пробит";
            } else {
                blockMult = 1.0 - target.blockPercent / 100.0;
                blockNote = "блок −" + target.blockPercent + "%";
            }
            target.blockPercent = 0;
        }

        int damage = Math.max(1, (int) Math.round(baseDamage(actor, target, ab)
                * elementMult * (stab ? STAB_MULTIPLIER : 1.0) * waterBoost
                * (crit ? CRIT_MULTIPLIER : 1.0) * blockMult));
        target.takeDamage(damage);

        List<String> tags = new ArrayList<>();
        if (stab) tags.add("STAB");
        if (elementMult > 1.0) tags.add("стихия x1.5");
        if (elementMult < 1.0) tags.add("стихия x0.67");
        if (waterBoost > 1.0) tags.add("заряд x1.35");
        if (crit) tags.add("КРИТ");
        if (blockNote != null) tags.add(blockNote);

        // 5. Статусы от атаки
        List<String> effects = new ArrayList<>();
        applyEffect(actor, target, ab.primaryEffect(), ab.primaryEffectChancePercent(), ab.primaryEffectDuration(), effects);
        if (actor.has(NEXT_HIT_BURN)) {
            actor.clearEffect(NEXT_HIT_BURN);
            applyEffect(actor, target, BURN, 100, 2, effects);
        }

        b.log(String.format("%s применяет «%s»%s: −%d HP (%s: %d/%d)%s",
                who, ab.name(),
                tags.isEmpty() ? "" : " [" + String.join(", ", tags) + "]",
                damage, target.name, target.hp, target.maxHp,
                effects.isEmpty() ? "." : " → " + String.join(", ", effects) + "."));

        // 6. Шипы цели отвечают атакующему
        if (target.has(THORNS)) {
            int thorns = actor.percentOfMaxHp(10);
            actor.takeDamage(thorns);
            b.log(String.format("Огненные шипы: %s получает −%d HP.", who, thorns));
        }
    }

    // ==================== СТАТУСЫ ====================

    private void applyEffect(Combatant actor, Combatant opponent, StatusEffectType effect,
                             int chancePercent, int duration, List<String> notes) {
        if (effect == null || effect == NONE || chancePercent <= 0) return;
        if (chancePercent < 100 && random.get().nextInt(100) >= chancePercent) return;

        if (TIMED_DEBUFFS.contains(effect) || CONTROL.contains(effect)) {
            if (opponent.has(CC_IMMUNE)) {
                notes.add("иммунитет блокирует «" + effectName(effect) + "»");
                return;
            }
            // Длительные дебаффы тикают в конце раунда, поэтому +1; контроль тратится при пропуске хода
            opponent.setEffect(effect, CONTROL.contains(effect) ? duration : duration + 1);
            notes.add(effectName(effect) + " (" + duration + " х.)");
            return;
        }

        if (effect == CLEANSE) {
            actor.cleanseDebuffs();
            notes.add("снимает негативные статусы");
        } else if (effect == SHELL_BARRIER) {
            actor.setEffect(SHELL_BARRIER, 1);
            notes.add("барьер поглотит следующий удар");
        } else if (TIMED_BUFFS.contains(effect)) {
            actor.setEffect(effect, duration + 1);
            notes.add(effectName(effect) + " (" + duration + " х.)");
        }
        // BLOCK и EVASION задаются полями способности (blockPercent / evasionChancePercent)
    }

    private void endOfRound(BattleSession b) {
        tickStatuses(b, b.player(), true);
        tickStatuses(b, b.enemy(), false);
        b.player().tickCooldowns();
        b.enemy().tickCooldowns();
    }

    private void tickStatuses(BattleSession b, Combatant c, boolean isPlayer) {
        String who = label(c, isPlayer);
        if (c.has(BURN) && c.alive()) {
            int dmg = c.percentOfMaxHp(8);
            c.takeDamage(dmg);
            c.decrementEffect(BURN);
            b.log(String.format("%s горит: −%d HP (%d/%d).", who, dmg, c.hp, c.maxHp));
        }
        if (c.has(POISON) && c.alive()) {
            int dmg = c.percentOfMaxHp(10);
            c.takeDamage(dmg);
            c.decrementEffect(POISON);
            b.log(String.format("%s отравлен: −%d HP (%d/%d).", who, dmg, c.hp, c.maxHp));
        }
        if (c.has(REGEN) && c.alive()) {
            int healed = c.heal(c.percentOfMaxHp(10));
            c.decrementEffect(REGEN);
            b.log(String.format("%s восстанавливает %d HP (%d/%d).", who, healed, c.hp, c.maxHp));
        }
        EXPIRING.forEach(c::decrementEffect);
    }

    // ==================== РАСЧЁТЫ ====================

    /** Примерный урон без случайностей — нужен ИИ для проверки добивания. */
    public int estimateDamage(Combatant actor, Combatant target, AbilityDto ab) {
        double mult = elementMultiplier(moveElement(ab), target.element) * (hasStab(ab, actor) ? STAB_MULTIPLIER : 1.0);
        double base = Math.max(6.0, (actor.attack + ab.power() * 0.65) - target.defense * 0.45);
        return Math.max(1, (int) Math.round(base * mult));
    }

    private double baseDamage(Combatant actor, Combatant target, AbilityDto ab) {
        double atk = actor.attack * (actor.has(ATK_UP) ? 1.30 : 1.0) + ab.power() * 0.65;
        double def = target.defense * (target.has(DEF_UP) ? 1.35 : 1.0) * 0.45;
        return Math.max(6.0, atk - def);
    }

    public static double elementMultiplier(String attackElement, String defendElement) {
        if (attackElement == null || defendElement == null
                || attackElement.equals(defendElement)
                || "NEUTRAL".equals(attackElement) || "NEUTRAL".equals(defendElement)) {
            return 1.0;
        }
        return ADVANTAGE.contains(attackElement + "->" + defendElement) ? 1.5 : 0.67;
    }

    private static String moveElement(AbilityDto ab) {
        return ab.element() == null || "NEUTRAL".equalsIgnoreCase(ab.element()) ? "NEUTRAL" : ab.element();
    }

    private static boolean hasStab(AbilityDto ab, Combatant actor) {
        String el = moveElement(ab);
        return ab.stab() || (!"NEUTRAL".equals(el) && el.equalsIgnoreCase(actor.element));
    }

    private static String label(Combatant c, boolean isPlayer) {
        return isPlayer ? c.name : "Дикий " + c.name;
    }

    private static String effectName(StatusEffectType t) {
        return switch (t) {
            case BURN -> "Горение";
            case POISON -> "Яд";
            case SLEEP -> "Сон";
            case STUN -> "Оглушение";
            case SLOW -> "Замедление −35% SPD";
            case BLIND -> "Ослепление −35% точности";
            case REGEN -> "Регенерация";
            case ATK_UP -> "Атака +30%";
            case DEF_UP -> "Защита +35%";
            case THORNS -> "Огненные шипы";
            case RAGE_SPEED_PIERCE -> "Скорость x2 и пробитие блока";
            case NEXT_HIT_BURN -> "следующий удар подожжёт";
            case WATER_BOOST -> "следующая водная атака +35%";
            case GUARANTEED_CRIT -> "гарантированный крит";
            case CRIT_IMMUNE -> "защита от критов";
            case CC_IMMUNE -> "иммунитет к контролю";
            default -> t.name();
        };
    }
}
