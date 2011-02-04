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

import argonms.tools.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author GoldenKevin
 */
public class McdbMobDataLoader extends MobDataLoader {
	private static final Logger LOG = Logger.getLogger(McdbMobDataLoader.class.getName());

	protected McdbMobDataLoader() {
		
	}

	protected void load(int mobid) {
		Connection con = DatabaseConnection.getWzConnection();
		MobStats stats = null;
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM `mobdata` WHERE `mobid` = ?");
			ps.setInt(1, mobid);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				stats = new MobStats();
				doWork(rs, stats);
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not read MCDB data for mob " + mobid, e);
		}
		mobStats.put(Integer.valueOf(mobid), stats);
	}

	public boolean loadAll() {
		Connection con = DatabaseConnection.getWzConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT * FROM `mobdata`");
			rs = ps.executeQuery();
			while (rs.next()) {
				MobStats stats = new MobStats();
				mobStats.put(doWork(rs, stats), stats);
			}
			return true;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not load all mob data from MCDB.", ex);
			return false;
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			} catch (SQLException ex) {
				//Nothing we can do
			}
		}
	}

	public boolean canLoad(int mobid) {
		Connection con = DatabaseConnection.getWzConnection();
		boolean exists = false;
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM `mobdata` WHERE `mobid` = ?");
			ps.setInt(1, mobid);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				exists = true;
			rs.close();
			ps.close();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not use MCDB to determine whether mob " + mobid + " is valid.", e);
		}
		return exists;
	}

	private int doWork(ResultSet rs, MobStats stats) throws SQLException {
		Connection con = DatabaseConnection.getWzConnection();
		int mobid = rs.getInt(1);
		stats.setLevel(rs.getShort(2));
		stats.setMaxHp(rs.getInt(3));
		stats.setMaxMp(rs.getInt(4));
		stats.setExp(rs.getInt(8));
		if (rs.getInt(13) != 0)
			stats.setUndead();
		stats.setElementAttribute(rs.getString(20));
		stats.setRemoveAfter(rs.getInt(11));
		stats.setHpTagColor(rs.getByte(18));
		stats.setHpTagBgColor(rs.getByte(19));
		if (rs.getInt(12) != 0)
			stats.setBoss();
		stats.setSelfDestructHp(rs.getInt(7));
		PreparedStatement ps = con.prepareStatement("SELECT `summonid` FROM `mobsummondata` where `mobid` = ?");
		ps.setInt(1, mobid);
		ResultSet rs2 = ps.executeQuery();
		while (rs2.next())
			stats.addSummon(rs2.getInt(1));
		rs2.close();
		ps.close();
		ps = con.prepareStatement("SELECT `skillid`,`level` FROM `mobskilldata` WHERE `mobid` = ?");
		ps.setInt(1, mobid);
		rs2 = ps.executeQuery();
		while (rs2.next()) {
			Skill s = new Skill();
			s.setSkill(rs2.getShort(1));
			s.setLevel(rs2.getByte(2));
			stats.addSkill(s);
		}
		rs2.close();
		ps.close();
		stats.setBuffToGive(rs.getInt(10));
		return mobid;
	}
}