package javelin.controller.action.world;

import java.util.ArrayList;
import java.util.List;

import javelin.model.item.Item;
import javelin.model.unit.Combatant;
import javelin.model.world.Squad;
import javelin.model.world.Town;
import javelin.model.world.WorldMap;
import javelin.view.screen.BattleScreen;
import javelin.view.screen.world.WorldScreen;
import tyrant.mikera.tyrant.Game;
import tyrant.mikera.tyrant.Game.Delay;
import tyrant.mikera.tyrant.InfoScreen;

/**
 * Split squad into two.
 * 
 * @author alex
 */
public class Divide extends WorldAction {

	public Divide() {
		super("Divide a group of monsters into 2", new int[] {},
				new String[] { "d" });
	}

	@Override
	public void perform(final WorldScreen screen) {
		clear();
		final String in =
				"Press each member's number to switch his destination squad.\n"
						+ "Press c to cancel or ENTER when done.\n"
						+ "The left column is your current squad, the right one is the new squad.\n"
						+ "To join two squads later just place them in the same square.\n";
		input(in);
		char input = ' ';
		final ArrayList<Combatant> indexreference =
				new ArrayList<Combatant>(Squad.active.members);
		final ArrayList<Combatant> oldsquad =
				new ArrayList<Combatant>(Squad.active.members);
		final ArrayList<Combatant> newsquad = new ArrayList<Combatant>();
		while (input != '\n') {
			clear();
			final ArrayList<String> oldcolumn = new ArrayList<String>();
			final ArrayList<String> newcolumn = new ArrayList<String>();
			log(indexreference, oldsquad, oldcolumn);
			log(indexreference, newsquad, newcolumn);
			String text = "";
			for (int i = 0; i < Math.max(oldcolumn.size(),
					newcolumn.size()); i++) {
				String oldtd = i < oldcolumn.size() ? oldcolumn.get(i) : "";
				while (oldtd.length() < WorldScreen.SPACER.length()) {
					oldtd += " ";
				}
				final String newtd =
						i < newcolumn.size() ? newcolumn.get(i) : "";
				text += oldtd + newtd + "\n";
			}
			input = input(text);
			if (input == 'c') {
				return;
			}

			Combatant swap;
			try {
				swap = indexreference
						.get(Integer.parseInt(Character.toString(input)) - 1);
			} catch (final IndexOutOfBoundsException e) {
				continue;
			} catch (final NumberFormatException e) {
				continue;
			}
			List<Combatant> from;
			List<Combatant> to;
			if (oldsquad.contains(swap)) {
				from = oldsquad;
				to = newsquad;
			} else {
				to = oldsquad;
				from = newsquad;
			}
			from.remove(swap);
			to.add(swap);
		}
		if (oldsquad.isEmpty() || newsquad.isEmpty()) {
			return;
		}
		input = ' ';
		int gold = Squad.active.gold / 2;
		final int increment = Squad.active.gold / 10;
		while (input != '\n') {
			clear();
			input = input(
					"How much gold do you want to transfer to the new squad? Use the + and - keys to change and ENTER to confirm.\n"
							+ gold);
			if (input == '+') {
				gold += increment;
				if (gold > Squad.active.gold) {
					gold = Squad.active.gold;
				}
			} else if (input == '-') {
				gold -= increment;
				if (gold < 0) {
					gold = 0;
				}
			}
		}
		Town nearto = findtown(Squad.active.x, Squad.active.y);
		int x, y;
		Squad s = null;
		placement: for (x = Squad.active.x - 1; x <= Squad.active.x + 1; x++) {
			for (y = Squad.active.y - 1; y <= Squad.active.y + 1; y++) {
				if (!WorldMap.isoccupied(x, y, false)
						&& (nearto == null || findtown(x, y) instanceof Town)) {
					s = new Squad(x, y,
							Squad.active.hourselapsed + WorldMove.TIMECOST);
					break placement;
				}
			}
		}
		s.members = newsquad;
		s.gold = gold;
		Squad.active.members = oldsquad;
		Squad.active.gold -= gold;
		s.place();
		for (final Combatant m : newsquad) {
			final String name = m.toString();
			final ArrayList<Item> items = Squad.active.equipment.get(m.id);
			Squad.active.equipment.remove(name);
			s.equipment.put(m.id, items);
		}
		Squad.active.updateavatar();
	}

	public Town findtown(int xp, int yp) {
		for (int x = xp - 1; x <= xp + 1; x++) {
			for (int y = yp - 1; y <= yp + 1; y++) {

				// if (Javelin.DEBUG && WorldScreen.getactor(x, y) != null) {
				// System.out.println(WorldScreen.getactor(x, y));
				// }

				if (WorldScreen.getactor(x, y) instanceof Town) {
					return (Town) WorldScreen.getactor(x, y);
				}
			}
		}
		return null;
	}

	public Character input(final String prompt) {
		Game.message(prompt, null, Delay.NONE);
		return InfoScreen.feedback();
	}

	public void clear() {
		BattleScreen.active.messagepanel.clear();
	}

	public void log(final List<Combatant> indexreference,
			final List<Combatant> oldsquad, final List<String> oldcolumn) {
		if (oldsquad.isEmpty()) {
			oldcolumn.add("Empty");
		} else {
			for (final Combatant m : oldsquad) {
				oldcolumn.add("[" + (indexreference.indexOf(m) + 1) + "] " + m);
			}
		}
	}
}