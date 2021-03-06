package javelin.controller.db.reader.fields;

import javelin.controller.db.reader.MonsterReader;
import javelin.controller.db.reader.SpecialtiesLog;
import javelin.controller.quality.Quality;

/**
 * @see FieldReader
 */
public class SpecialAttacks extends FieldReader {

	public SpecialAttacks(MonsterReader reader, final String fieldname) {
		super(reader, fieldname);
	}

	@Override
	public void read(final String value) {
		int ignored = 0;
		String[] values = value.split(",");
		reading: for (String attack : values) {
			final String trim = attack.trim().toLowerCase();
			for (final Quality q : Quality.qualities) {
				if (q.apply(trim, reader.monster)) {
					q.add(trim, reader.monster);
					continue reading;
				}
			}
			ignored += 1;
			reader.sAtks.add(trim);
		}
		SpecialtiesLog.log("    Special attacks: " + value + " (used "
				+ (values.length - ignored) + ")");
	}
}