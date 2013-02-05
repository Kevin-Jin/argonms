/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
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

package argonms.game.field;

import argonms.common.character.Skills;
import argonms.common.field.MonsterStatusEffect;
import argonms.common.loading.StatusEffectsData;
import argonms.common.loading.StatusEffectsData.MonsterStatusEffectsData;
import argonms.common.util.Rng;
import argonms.common.util.Scheduler;
import argonms.game.character.DiseaseTools;
import argonms.game.character.GameCharacter;
import argonms.game.field.entity.Mist;
import argonms.game.field.entity.Mob;
import argonms.game.loading.mob.MobDataLoader;
import argonms.game.loading.skill.MobSkillEffectsData;
import argonms.game.loading.skill.PlayerSkillEffectsData;
import argonms.game.net.external.GamePackets;
import java.awt.Point;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author GoldenKevin
 */
public final class MonsterStatusEffectTools {
	private static byte[] getCastEffect(Mob m, MonsterStatusEffectsData e, Map<MonsterStatusEffect, Short> updatedStats) {
		switch (e.getSourceType()) {
			case MOB_SKILL:
				return GamePackets.writeMonsterBuff(m, updatedStats, (short) e.getDataId(), e.getLevel(), (short) 900);
			case PLAYER_SKILL:
				return GamePackets.writeMonsterDebuff(m, updatedStats, e.getDataId(), (short) 900);
		}
		return null;
	}

	private static byte[] getDispelEffect(Mob m, MonsterStatusEffectsData e) {
		return GamePackets.writeMonsterCancelStatusEffect(m, e.getMonsterEffects());
	}

	private static int applyEffects(Map<MonsterStatusEffect, Short> updatedStats, Mob m, GameCharacter p, MonsterStatusEffectsData e) {
		if (m.areEffectsActive(e))
			m.removeCancelEffectTask(e);
		for (MonsterStatusEffect buff : e.getMonsterEffects()) {
			MonsterStatusEffectValues v = m.getEffectValue(buff);
			if (v != null)
				dispelEffect(m, buff, v);
			v = applyEffect(m, e, buff);
			updatedStats.put(buff, Short.valueOf(v.getModifier()));
			m.addToActiveEffects(buff, v);
		}
		switch (e.getSourceType()) {
			case MOB_SKILL: {
				//check if it is a mob attack skill
				MobSkillEffectsData skill = ((MobSkillEffectsData) e);
				switch (e.getDataId()) {
					case MobSkills.MIST:
						Mist mist = new Mist(m, skill);
						m.getMap().spawnMist(mist, skill.getX() * 10, null);
						return e.getDuration();
					case MobSkills.SUMMON:
						short limit = skill.getSummonLimit();
						if (limit == 5000)
							limit = (short) (30 + m.getMap().getPlayerCount() * 2);
						if (m.getSpawnedSummons() < limit) {
							Random generator = Rng.getGenerator();
							for (Integer oMobId : skill.getSummons().values()) {
								int mobId = oMobId.intValue();
								Mob summon = new Mob(MobDataLoader.getInstance().getMobStats(mobId), m.getMap());
								int ypos, xpos;
								xpos = m.getPosition().x;
								ypos = m.getPosition().y;
								switch (mobId) {
									case 8500003: // Pap bomb high
										summon.setFoothold((short)Math.ceil(generator.nextDouble() * 19.0));
										ypos = -590; //no break?
									case 8500004: // Pap bomb
										//Spawn between -500 and 500 from the monsters X position
										xpos = (int)(m.getPosition().x + Math.ceil(generator.nextDouble() * 1000.0) - 500);
										if (ypos != -590)
											ypos = m.getPosition().y;
										break;
									case 8510100: //Pianus bomb
										if (Math.ceil(generator.nextDouble() * 5) == 1) {
											ypos = 78;
											xpos = (int)(0 + Math.ceil(generator.nextDouble() * 5)) + ((Math.ceil(generator.nextDouble() * 2) == 1) ? 180 : 0);
										} else
											xpos = (int)(m.getPosition().x + Math.ceil(generator.nextDouble() * 1000.0) - 500);
										break;
								}
								// Get spawn coordinates (This fixes monster lock)
								// TODO get map left and right wall. Any suggestions?
								switch (m.getMap().getDataId()) {
									case 220080001: //Pap map
										if (xpos < -890)
											xpos = (int)(-890 + Math.ceil(generator.nextDouble() * 150));
										else if (xpos > 230)
											xpos = (int)(230 - Math.ceil(generator.nextDouble() * 150));
										break;
									case 230040420: // Pianus map
										if (xpos < -239)
											xpos = (int)(-239 + Math.ceil(generator.nextDouble() * 150));
										else if (xpos > 371)
											xpos = (int)(371 - Math.ceil(generator.nextDouble() * 150));
										break;
								}
								summon.setPosition(new Point(xpos, ypos));
								summon.setSpawnEffect(skill.getSummonEffect());
								m.getMap().spawnMonster(summon);
								m.addToSpawnedSummons();
							}
						}
						return 0;
					default:
						if (!e.getEffects().isEmpty())
							DiseaseTools.applyDebuff(p, (short) skill.getDataId(), skill.getLevel());
						return e.getDuration();
				}
			}
			case PLAYER_SKILL: {
				int duration;
				switch (e.getDataId()) {
					case Skills.BLIND:
					case Skills.HAMSTRING:
						duration = e.getY();
						break;
					case Skills.SWORD_ICE_CHARGE:
					case Skills.BW_BLIZZARD_CHARGE:
						//freeze skills have weird times...
						duration = e.getY() * 2;
						break;
					case Skills.POISON_MIST: {
						MonsterStatusEffectValues value = applyEffect(m, e, MonsterStatusEffect.POISON);
						updatedStats.put(MonsterStatusEffect.POISON, Short.valueOf((short) 1));
						m.addToActiveEffects(MonsterStatusEffect.POISON, value);
						duration = e.getX();
						break;
					}
					default:
						duration = e.getDuration();
						break;
				}
				return duration;
			}
			default:
				return 0;
		}
	}

	private static void applyEffectsAndShowVisualsInternal(final Mob m, final GameCharacter p, final MonsterStatusEffectsData e) {
		Map<MonsterStatusEffect, Short> updatedStats = new EnumMap<MonsterStatusEffect, Short>(MonsterStatusEffect.class);
		int duration = applyEffects(updatedStats, m, p, e);
		byte[] effect = getCastEffect(m, e, updatedStats);
		if (m.isVisible() && effect != null)
			m.getMap().sendToAll(effect);
		m.addCancelEffectTask(e, Scheduler.getInstance().runAfterDelay(new Runnable() {
			@Override
			public void run() {
				dispelEffectsAndShowVisuals(m, e);
			}
		}, duration));
	}

	public static void applyEffectsAndShowVisuals(final Mob m, final GameCharacter p, final MonsterStatusEffectsData e) {
		if (e.getSourceType() == StatusEffectsData.EffectSource.MOB_SKILL && ((MobSkillEffectsData) e).isAoe())
			for (MapEntity neighbor : m.getMap().getMapEntitiesInRect(e.getBoundingBox(m.getPosition(), m.getStance() % 2 != 0), EnumSet.of(MapEntity.EntityType.MONSTER)))
				applyEffectsAndShowVisualsInternal((Mob) neighbor, p, e);
		else
			applyEffectsAndShowVisualsInternal(m, p, e);
	}

	public static void dispelEffectsAndShowVisuals(Mob m, MonsterStatusEffectsData e) {
		m.removeCancelEffectTask(e);
		switch (e.getSourceType()) {
			case MOB_SKILL: {
				for (MonsterStatusEffect buff : e.getMonsterEffects()) {
					MonsterStatusEffectValues v = m.removeFromActiveEffects(buff);
					if (v != null)
						dispelEffect(m, buff, v);
				}
				break;
			}
			case PLAYER_SKILL: {
				for (MonsterStatusEffect buff : e.getMonsterEffects()) {
					if (buff != null) {
						MonsterStatusEffectValues v = m.removeFromActiveEffects(buff);
						if (v != null)
							dispelEffect(m, buff, v);
					}
				}
			}
		}
		byte[] effect = getDispelEffect(m, e);
		if (m.isVisible() && effect != null)
			m.getMap().sendToAll(effect);
	}

	private static MonsterStatusEffectValues applyEffect(Mob m, MonsterStatusEffectsData e, MonsterStatusEffect buff) {
		short mod = 0;
		switch (buff) {
			case WATK:
				mod = (short) (e.getX() - 100);
				break;
			case WDEF:
				mod = (short) e.getX();
				break;
			case MATK:
				mod = (short) e.getX();
				break;
			case MDEF:
				mod = (short) e.getX();
				break;
			case ACC:
				mod = (short) e.getX();
				break;
			case AVOID:
				mod = (short) e.getX();
				break;
			case SPEED:
				mod = (short) e.getX();
				break;
			case STUN:
				mod = 1;
				break;
			case FREEZE:
				mod = 1;
				break;
			case POISON:
				mod = 1;
				break;
			case SEAL:
				mod = 1;
				break;
			case TAUNT_1:
				mod = (short) (100 - e.getX());
				break;
			case TAUNT_2:
				mod = (short) (100 - e.getX());
				break;
			case DOOM:
				mod = 1;
				break;
			case SHADOW_WEB:
				mod = 1;
				break;
			case WEAPON_IMMUNITY:
				mod = (short) e.getX();
				break;
			case MAGIC_IMMUNITY:
				mod = (short) e.getX();
				break;
			case NINJA_AMBUSH:
				mod = 1;
				break;
			case INERTMOB:
				mod = 1;
				break;
		}
		return new MonsterStatusEffectValues(e, mod);
	}

	private static void dispelEffect(Mob m, MonsterStatusEffect key, MonsterStatusEffectValues value) {
		switch (key) {
			case WATK:
				break;
			case WDEF:
				break;
			case MATK:
				break;
			case MDEF:
				break;
			case ACC:
				break;
			case AVOID:
				break;
			case SPEED:
				break;
			case STUN:
				break;
			case FREEZE:
				break;
			case POISON:
				break;
			case SEAL:
				break;
			case TAUNT_1:
				break;
			case TAUNT_2:
				break;
			case DOOM:
				break;
			case SHADOW_WEB:
				break;
			case WEAPON_IMMUNITY:
				break;
			case MAGIC_IMMUNITY:
				break;
			case NINJA_AMBUSH:
				break;
			case INERTMOB:
				break;
		}
	}

	public static void applyDispel(Mob m, PlayerSkillEffectsData e) {
		//"Dispel can override: W.Attack+, M.Att+, W.Defence+, M.Defence+, Accuracy+, Avoidability+, Speed+ and Super Avoidability
		//Dispel can NOT override: Damage Reflection, Cancel W.Attack and Cancel M.Attack"
		for (MonsterStatusEffect buff : new MonsterStatusEffect[] { MonsterStatusEffect.WATK, MonsterStatusEffect.WDEF, MonsterStatusEffect.MATK, MonsterStatusEffect.MDEF, MonsterStatusEffect.ACC, MonsterStatusEffect.AVOID, MonsterStatusEffect.SPEED }) {
			MonsterStatusEffectValues v = m.getEffectValue(buff);
			if (v != null)
				MonsterStatusEffectTools.dispelEffectsAndShowVisuals(m, v.getEffectsData());
		}
	}

	private MonsterStatusEffectTools() {
		//uninstantiable...
	}
}
