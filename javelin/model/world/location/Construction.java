package javelin.model.world.location;

import java.util.List;

import javelin.Javelin;
import javelin.model.unit.Combatant;
import javelin.model.world.WorldActor;
import javelin.model.world.location.town.labor.Labor;

public class Construction extends Location {
	public Location goal;
	private Labor progress;
	private WorldActor previous;

	public Construction(Location project, WorldActor previous, Labor progress) {
		super("Construction site: " + project.toString().toLowerCase());
		this.goal = project;
		this.previous = previous;
		this.progress = progress;
		sacrificeable = true;
		discard = false;
		gossip = false;
		allowentry = false;
	}

	@Override
	protected Integer getel(int attackerel) {
		return Integer.MIN_VALUE;
	}

	@Override
	public List<Combatant> getcombatants() {
		return null;
	}

	@Override
	public void remove() {
		super.remove();
		progress.cancel();
		if (previous != null) {
			previous.place();
		}
	}

	@Override
	public boolean interact() {
		if (Javelin.prompt("This is a construction site, building: "
				+ goal.toString().toLowerCase() + ".\n"//
				+ "Progress is currently at " + progress.getprogress()
				+ "%.\n\n"
				+ "Press c to cancel this project or any other key to leave...") == 'c') {
			remove();
		}
		return true;
	}
}
