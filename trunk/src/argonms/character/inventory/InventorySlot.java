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

package argonms.character.inventory;

/**
 *
 * @author GoldenKevin
 */
public abstract class InventorySlot implements Comparable<InventorySlot>, Cloneable {
	public enum ItemType { EQUIP, ITEM, PET, RING }

	private int id;
	private long expire;
	private int uid;
	private String owner;

	protected InventorySlot(int itemid) {
		this.id = itemid;
	}

	public abstract ItemType getType();
	public abstract short getQuantity();
	public abstract void setQuantity(short value);

	public int getItemId() {
		return id;
	}

	public long getExpiration() {
		return expire;
	}

	public void setExpiration(long expire) {
		this.expire = expire;
	}

	public int getUniqueId() {
		return uid;
	}

	public void setUniqueId(int id) {
		this.uid = id;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public int compareTo(InventorySlot item) {
		return this.getItemId() - item.getItemId();
	}

	public abstract InventorySlot clone();
}