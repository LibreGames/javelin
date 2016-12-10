package javelin.model.world.location.town.labor;

import javelin.controller.Point;
import javelin.model.world.location.Location;
import javelin.model.world.location.fortification.Fortification;
import javelin.model.world.location.town.District;

public abstract class BuildingUpgrade extends Build {
	int upgradelevel;

	public BuildingUpgrade(String newname, int cost, int upgradelevel, Location previous) {
		super("Upgrade " + previous.toString().toLowerCase() + " to " + newname.toLowerCase(), cost, previous);
		this.upgradelevel = upgradelevel;
	}

	@Override
	abstract public Location getgoal();

	@Override
	public void start() {
		previous.remove();
		super.start();
	}

	@Override
	public boolean validate(District d) {
		if (!town.ishostile() && previous.ishostile()) {
			return false;
		}
		return site != null || d.getlocations().contains(previous);
	}

	@Override
	protected Point getsitelocation() {
		return previous.getlocation();
	}

	@Override
	public void done() {
		super.done();
		if (site.goal instanceof Fortification) {
			((Fortification) site.goal).raiselevel(upgradelevel);
		}
		done(site.goal);
	}

	abstract protected void done(Location goal);
}
