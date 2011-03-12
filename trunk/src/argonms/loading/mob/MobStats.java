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

package argonms.loading.mob;

import argonms.character.inventory.InventorySlot;
import argonms.character.inventory.InventoryTools;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author GoldenKevin
 */
public class MobStats {
	private int mobid;
	private short level;
	private int maxHp;
	private int maxMp;
	private int pad;
	private int exp;
	private boolean undead;
	private String elemAttr;
	private int removeAfter;
	private boolean hideHp;
	private boolean hideName;
	private byte hpTagColor;
	private byte hpTagBgColor;
	private boolean boss;
	private int sd;
	private List<Integer> loseItems;
	private boolean invincible;
	private List<Integer> summons;
	private boolean firstAttack;
	private Map<Integer, Attack> attacks;
	private List<Skill> skills;
	private int buff;
	private Map<String, Integer> delays;
	private Map<Integer, Integer> itemDrops;
	private MesoDropChance mesoDrop;

	protected MobStats(int mobid) {
		this.mobid = mobid;
		this.loseItems = new ArrayList<Integer>();
		this.summons = new ArrayList<Integer>();
		this.attacks = new HashMap<Integer, Attack>();
		this.skills = new ArrayList<Skill>();
		this.delays = new HashMap<String, Integer>();
		this.itemDrops = new HashMap<Integer, Integer>();
	}

	protected void setLevel(short level) {
		this.level = level;
	}

	protected void setMaxHp(int value) {
		this.maxHp = value;
	}

	protected void setMaxMp(int value) {
		this.maxMp = value;
	}

	protected void setPhysicalDamage(int pad) {
		this.pad = pad;
	}

	protected void setExp(int exp) {
		this.exp = exp;
	}

	protected void setUndead() {
		this.undead = true;
	}

	protected void setElementAttribute(String attr) {
		this.elemAttr = attr;
	}

	protected void setRemoveAfter(int time) {
		this.removeAfter = time;
	}

	protected void setHideHp() {
		this.hideHp = true;
	}

	protected void setHideName() {
		this.hideName = true;
	}

	protected void setHpTagColor(byte color) {
		this.hpTagColor = color;
	}

	protected void setHpTagBgColor(byte color) {
		this.hpTagBgColor = color;
	}

	protected void setBoss() {
		this.boss = true;
	}

	protected void setSelfDestructHp(int hp) {
		this.sd = hp;
	}

	protected void addLoseItem(int itemid) {
		this.loseItems.add(Integer.valueOf(itemid));
	}

	protected void setInvincible() {
		this.invincible = true;
	}

	protected void addSummon(int mobid) {
		this.summons.add(Integer.valueOf(mobid));
	}

	protected void setFirstAttack() {
		this.firstAttack = true;
	}

	protected void addAttack(int attackid, Attack attack) {
		this.attacks.put(Integer.valueOf(attackid), attack);
	}

	protected void addSkill(Skill skill) {
		this.skills.add(skill);
	}

	protected void setBuffToGive(int buffid) {
		this.buff = buffid;
	}

	protected void addDelay(String name, int delay) {
		this.delays.put(name, Integer.valueOf(delay));
	}

	protected void addItemDrop(int itemid, int chance) {
		this.itemDrops.put(Integer.valueOf(itemid), Integer.valueOf(chance));
	}

	protected void setMesoDrop(int chance, int min, int max) {
		this.mesoDrop = new MesoDropChance(chance, min, max);
	}

	public int getMobId() {
		return mobid;
	}

	public short getLevel() {
		return level;
	}

	public int getMaxHp() {
		return maxHp;
	}

	public int getMaxMp() {
		return maxMp;
	}

	public int getPhysicalDamage() {
		return pad;
	}

	public int getExp() {
		return exp;
	}

	public boolean isUndead() {
		return undead;
	}

	public String getElementAttribute() {
		return elemAttr;
	}

	public int getRemoveAfter() {
		return removeAfter;
	}

	public boolean isHpHidden() {
		return hideHp;
	}

	public boolean isNameHidden() {
		return hideName;
	}

	public byte getHpTagColor() {
		return hpTagColor;
	}

	public byte getHpTagBgColor() {
		return hpTagBgColor;
	}

	public boolean isBoss() {
		return boss;
	}

	public int getSelfDestructHp() {
		return sd;
	}

	public List<Integer> getLoseItems() {
		return Collections.unmodifiableList(loseItems);
	}

	public boolean isInvincible() {
		return invincible;
	}

	public List<Integer> getSummons() {
		return Collections.unmodifiableList(summons);
	}

	public boolean isFirstAttack() {
		return firstAttack;
	}

	public Map<Integer, Attack> getAttacks() {
		return attacks;
	}

	public List<Skill> getSkills() {
		return Collections.unmodifiableList(skills);
	}

	public int getBuffToGive() {
		return buff;
	}

	public Map<String, Integer> getDelays() {
		return Collections.unmodifiableMap(delays);
	}

	/**
	 * Randomly select an amount of mesos for this monster to drop when killed,
	 * using the chances that have been given in setMesoDrop.
	 * @return the amount of mesos that this monster will drop. If this monster
	 * should not drop any mesos, 0 will be returned.
	 */
	public int getMesosToDrop() {
		if (mesoDrop == null) {
			//taken from OdinMS (simplified a bit). It works surprisingly well.
			double factor = Math.pow(0.93, getExp() / 300.0);
			if (factor > 1.0)
				factor = 1.0;
			else if (factor < 0.001)
				factor = 0.005;
			return Math.min(30000, (int) (factor * getExp() * Math.random() * 2.1));
		} else {
			if (Math.random() < mesoDrop.getDropChance()) {
				int min = mesoDrop.getMinMesoDrop();
				int max = mesoDrop.getMaxMesoDrop();
				return (int) (Math.random() * (max - min + 1)) + min;
			}
			return 0;
		}
	}

	public List<InventorySlot> getItemsToDrop() {
		List<InventorySlot> items = new ArrayList<InventorySlot>();
		for (Entry<Integer, Integer> entry : itemDrops.entrySet())
			if (Math.random() < (entry.getValue().intValue() / 1000000.0))
				items.add(InventoryTools.makeItemWithId(entry.getKey().intValue()));
		return items;
	}

	private static class MesoDropChance {
		private int chance;
		private int min;
		private int max;

		public MesoDropChance(int chance, int min, int max) {
			this.chance = chance;
			this.min = min;
			this.max = max;
		}

		public double getDropChance() {
			return chance / 1000000.0;
		}

		public int getMinMesoDrop() {
			return min;
		}

		public int getMaxMesoDrop() {
			return max;
		}
	}
}
