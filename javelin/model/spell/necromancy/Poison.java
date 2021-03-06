package javelin.model.spell.necromancy;

import javelin.controller.challenge.factor.SpellsFactor;
import javelin.model.Realm;
import javelin.model.condition.Condition.Effect;
import javelin.model.condition.Poisoned;
import javelin.model.spell.Touch;
import javelin.model.state.BattleState;
import javelin.model.unit.Combatant;
import javelin.model.unit.Monster;

/**
 * Deals 1d10 points of temporary Constitution damage immediately and another
 * 1d10 points of temporary Constitution damage 1 hour later.
 * 
 * @see Poisoned
 * @author alex
 */
public class Poison extends Touch {

	public static Poison instance = new Poison();
	boolean nonmagical = false;

	/** Constructor. */
	public Poison() {
		super("Poison", 4, SpellsFactor.ratespelllikeability(4), Realm.EVIL);
		castinbattle = true;
	}

	@Override
	public String cast(Combatant caster, Combatant target, BattleState s,
			boolean saved) {
		if (saved) {
			return target + " resists!";
		}
		final int dc = getdc(caster);
		float ratio = (dc - target.source.fortitude()) / new Float(dc);
		if (ratio < 0) {
			ratio = 0;
		} else if (ratio > 1) {
			ratio = 1;
		}
		final int damage = Math.round(3 * ratio);
		if (damage <= 0) {
			return target + " resists!";
		}
		Poisoned p = new Poisoned(Float.MAX_VALUE, target, Effect.NEGATIVE,
				damage, dc, nonmagical ? null : casterlevel);
		if (p.neutralized) {
			return target + " is immune to poison.";
		}
		target.addcondition(p);
		if (target.hp < 1) {
			target.hp = 1;
		}
		return target + " is poisoned!";
	}

	@Override
	public int save(Combatant caster, Combatant target) {
		int fort = target.source.fortitude();
		if (fort == Integer.MAX_VALUE || target.source.immunitytopoison) {
			return -Integer.MAX_VALUE;
		}
		return getdc(caster) - fort;
	}

	int getdc(Combatant caster) {
		return 10 + casterlevel / 2 + Monster.getbonus(caster.source.wisdom);
	}

	@Override
	public void setdamageeffect() {
		nonmagical = true;
	}
}
