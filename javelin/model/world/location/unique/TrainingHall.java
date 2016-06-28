package javelin.model.world.location.unique;

import javelin.controller.challenge.ChallengeRatingCalculator;
import javelin.controller.challenge.RewardCalculator;
import javelin.controller.exception.RepeatTurn;
import javelin.controller.fight.Siege;
import javelin.controller.fight.TrainingSession;
import javelin.model.unit.Combatant;
import javelin.model.unit.Monster;
import javelin.model.unit.Squad;
import javelin.model.world.location.fortification.Fortification;
import javelin.view.screen.SquadScreen;
import tyrant.mikera.engine.RPG;
import tyrant.mikera.tyrant.Game;
import tyrant.mikera.tyrant.Game.Delay;

/**
 * Designed as a manner of making the lower levels faster, this receives the
 * amount of treasure in gold as a fee and offers a fight which, if won, will
 * offer a free feat upgrade. There are 4 levels and each is a difficult fight
 * for parties of 4 of the same level. The encounter levels are: 2, 7, 10, 17.
 * 
 * Inspired by the tower in WUtai on Final Fantasy VII.
 * 
 * @author alex
 */
public class TrainingHall extends Fortification {
	static final boolean DEBUG = false;
	private static final String DESCRIPTION = "The Training Hall";
	/**
	 * Actual encounter level for each {@link TrainingSession}, by using the
	 * shifted-to-zero index.
	 */
	public static int[] EL = new int[] { 2, 7, 10, 17 };
	/**
	 * From 1 upwards, the current training session difficulty, supposed to
	 * represent different floors.
	 * 
	 * TODO this is being initialized in 2 places
	 */
	public Integer currentlevel = 1;

	/** Constructor. */
	public TrainingHall() {
		super(DESCRIPTION, DESCRIPTION, 0, 0);
		UniqueLocation.init(this);
		discard = false;
		impermeable = false;
		sacrificeable = true;
	}

	@Override
	protected void generategarrison(int minel, int maxel) {
		generategarrison();
	}

	public void generategarrison() {
		if (currentlevel == null) {
			currentlevel = 1;
		}
		garrison.clear();
		while (ChallengeRatingCalculator
				.calculateel(garrison) < EL[currentlevel - 1]) {
			Monster sensei = null;
			while (sensei == null || !sensei.think(0)) {
				sensei = RPG.pick(SquadScreen.getcandidates());
			}
			garrison.add(new Combatant(null, sensei.clone(), true));
			if (DEBUG) {
				break;
			}
		}
	}

	@Override
	protected Siege fight() {
		int price = RewardCalculator.receivegold(garrison);
		Game.messagepanel.clear();
		if (price > Squad.active.gold) {
			Game.message("Not enough money to pay the training fee of $" + price
					+ "!", null, Delay.NONE);
			Game.getInput();
			throw new RepeatTurn();
		}
		Game.message(
				"Do you want to pay a free of $" + price
						+ " for a lesson?\nPress s to study or any other key to leave",
				null, Delay.NONE);
		if (Game.getInput().getKeyChar() == 's') {
			Squad.active.gold -= price;
			return new TrainingSession(this);
		}
		throw new RepeatTurn();
	}

	@Override
	public boolean drawgarisson() {
		return false;
	}
}