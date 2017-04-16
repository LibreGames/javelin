package javelin.controller.action;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javelin.controller.exception.RepeatTurn;
import javelin.controller.fight.Fight;
import javelin.controller.old.Game;
import javelin.controller.old.Game.Delay;
import javelin.model.unit.Combatant;

public class AutoAttack extends Action {
	private static final Comparator<Combatant> SORTBYSTATUS = new Comparator<Combatant>() {
		@Override
		public int compare(Combatant o1, Combatant o2) {
			return o1.getnumericstatus() - o2.getnumericstatus();
		}
	};

	public AutoAttack() {
		super("Auto-attack nearest visible enemy", new String[] { "\t" });
	}

	@Override
	public boolean perform(Combatant active) {
		ArrayList<Combatant> melee = Fight.state.getsurroundings(active);
		melee.removeAll(active.getteam(Fight.state));
		if (!melee.isEmpty() && !active.source.melee.isEmpty()) {
			melee.sort(SORTBYSTATUS);
			active.meleeattacks(melee.get(0), Fight.state);
			return true;
		}
		List<Combatant> ranged = Fight.state.gettargets(active);
		ranged.removeAll(melee);
		if (!ranged.isEmpty() && !active.source.ranged.isEmpty()) {
			ranged.sort(SORTBYSTATUS);
			active.meleeattacks(ranged.get(0), Fight.state);
			return true;
		}
		Game.message("No targets in range...", Delay.WAIT);
		throw new RepeatTurn();
	}

	@Override
	public String[] getDescriptiveKeys() {
		return new String[] { "TAB" };
	}
}