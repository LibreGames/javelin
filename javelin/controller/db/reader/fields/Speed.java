package javelin.controller.db.reader.fields;

import java.beans.PropertyVetoException;

import javelin.controller.ai.BattleAi;
import javelin.controller.db.reader.MonsterReader;
import javelin.model.unit.Monster;

/**
 * @see FieldReader
 */
public class Speed extends FieldReader {
	/**
	 * It really isn't fun watching the AI waste movement because it can gain 20
	 * squares in a single move-action. Perhaps this could be later tweaked in
	 * {@link BattleAi}.
	 */
	private static final int MAXSPEED = 50;

	public Speed(MonsterReader monsterReader, final String fieldname) {
		super(monsterReader, fieldname);
	}

	@Override
	public void read(String value) throws PropertyVetoException {
		try {
			final int commentBegin = value.lastIndexOf("(");
			if (value.substring(commentBegin, value.lastIndexOf(")"))
					.contains(",")) {
				value = value.substring(0, commentBegin).trim();
			}
		} catch (final StringIndexOutOfBoundsException e) {
			// doesn't have commentaries
		}

		final Monster m = reader.monster;
		for (String speedType : value.split(",")) {
			speedType = speedType.toLowerCase().replace(" feet.", " ft.")
					.replace(" ft.", "").trim();
			final int or = speedType.indexOf(" or");
			if (or != -1) {
				speedType = speedType.substring(0, or).trim();
			}
			if (speedType.contains("(") && !speedType.contains(" (")) {
				speedType = speedType.replaceAll("\\(", " \\(");
			}
			if (speedType.contains("climb ")) {
				// speed.setClimb(Long.parseLong(speedType.replace("climb ",
				// "")));
			} else if (speedType.contains("fly ")) {
				final String maneuverability = speedType.substring(
						speedType.indexOf("(") + 1, speedType.indexOf(")"));
				m.fly = Integer.parseInt(speedType.replace("fly ", "")
						.replace(" (" + maneuverability + ")", "").trim());
			} else if (speedType.contains("swim ")) {
				m.swim = Integer.parseInt(
						reader.cleanArmor(speedType).replace("swim ", ""));
			} else if (speedType.contains("burrow ")) {
				m.burrow = Integer.parseInt(speedType.replace("burrow ", ""));
			} else if (speedType.contains("base")) {
				// ignores base value
				continue;
			} else {
				m.walk = Integer.parseInt(reader.cleanArmor(speedType));
			}
		}
		if (m.fly == 0 && m.walk == 0) {
			reader.errorhandler.setInvalid("Cannot move out of water!");
		}
		if (m.fly > 0) {
			m.walk = 0;
		}
		m.walk = limit(m.walk);
		m.fly = limit(m.fly);
		m.swim = limit(m.swim);
	}

	/** @see #MAXSPEED */
	static int limit(final int x) {
		return x > MAXSPEED ? 50 : x;
	}
}