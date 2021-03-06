package javelin.controller.quality.resistance;

import javelin.controller.quality.Quality;
import javelin.controller.upgrade.Upgrade;
import javelin.controller.upgrade.UpgradeHandler;
import javelin.model.unit.Combatant;
import javelin.model.unit.Monster;

/**
 * Reader and {@link Upgrade} for paralysis immunity.
 * 
 * @see Monster#will()
 */
public class ParalysisImmunity extends Quality {
	static class ParalysisImmunityUpgrade extends Upgrade {
		ParalysisImmunityUpgrade() {
			super("Paralysis immunity");
		}

		@Override
		public String info(Combatant c) {
			return "";
		}

		@Override
		protected boolean apply(Combatant c) {
			if (c.source.immunitytoparalysis) {
				return false;
			}
			c.source.immunitytoparalysis = true;
			return true;
		}

	}

	/** Constructor. */
	public ParalysisImmunity() {
		super("paralysis immunity");
	}

	@Override
	public void add(String declaration, Monster m) {
		m.immunitytoparalysis = true;
	}

	@Override
	public boolean has(Monster monster) {
		return monster.immunitytoparalysis;
	}

	@Override
	public float rate(Monster monster) {
		return 0.1f;
	}

	@Override
	public void listupgrades(UpgradeHandler handler) {
		handler.good.add(new ParalysisImmunityUpgrade());
	}
}
