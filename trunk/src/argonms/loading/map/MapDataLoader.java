/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011  GoldenKevin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package argonms.loading.map;

import argonms.loading.DataFileType;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public abstract class MapDataLoader {
	private static MapDataLoader instance;

	protected Map<Integer, MapStats> mapStats;

	protected MapDataLoader() {
		mapStats = new HashMap<Integer, MapStats>();
	}

	protected abstract void load(int mapid);

	public abstract boolean loadAll();

	public MapStats getMapStats(int id) {
		Integer oId;
		MapStats stats;
		//do {
			oId = Integer.valueOf(id);
			if (!mapStats.containsKey(oId))
				load(id);
			stats = mapStats.get(oId);
			//id = stats != null ? stats.getLink() : 0;
		//} while (id != 0);
		return stats;
	}

	public static MapDataLoader setInstance(DataFileType wzType, String wzPath) {
		switch (wzType) {
			case KVJ:
				instance = new KvjMapDataLoader(wzPath);
				break;
		}
		return instance;
	}

	public static MapDataLoader getInstance() {
		return instance;
	}
}
