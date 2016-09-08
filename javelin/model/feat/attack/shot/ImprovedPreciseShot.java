package javelin.model.feat.attack.shot;

import javelin.model.feat.Feat;
import javelin.model.unit.Combatant;

/**
 * See the d20 SRD for more info.
 */
public class ImprovedPreciseShot extends Feat {
	/** Unique instance of this {@link Feat}. */
	public static final Feat SINGLETON = new ImprovedPreciseShot();

	/** Constructor. */
	private ImprovedPreciseShot() {
		super("Improved Precise Shot");
		prerequisite = javelin.model.feat.attack.shot.PreciseShot.SINGLETON;
	}

	@Override
	public String inform(Combatant m) {
		return "";
	}

	@Override
	public boolean apply(Combatant m) {
		return m.source.dexterity >= 19 && m.source.getbaseattackbonus() >= 11
				&& super.apply(m);
	}
}