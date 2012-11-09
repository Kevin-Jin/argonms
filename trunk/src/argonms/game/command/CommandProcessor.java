/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2012  GoldenKevin
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

package argonms.game.command;

import argonms.common.UserPrivileges;
import argonms.common.character.PlayerStatusEffect;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.command.CommandDefinition.CommandAction;
import argonms.game.field.GameMap;
import argonms.game.field.MapEntity;
import argonms.game.field.MapEntity.EntityType;
import argonms.game.field.entity.ItemDrop;
import argonms.game.field.entity.Mob;
import argonms.game.field.entity.PlayerNpc;
import argonms.game.loading.skill.SkillDataLoader;
import argonms.game.loading.skill.SkillStats;
import java.awt.Point;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 *
 * @author GoldenKevin
 */
public class CommandProcessor {
	private static final Pattern argSplit;
	private static final CommandProcessor singleton;
	private final Map<String, AbstractCommandDefinition> definitions;

	static {
		argSplit = Pattern.compile(" ");
		singleton = new CommandProcessor();
	}

	private CommandProcessor() {
		definitions = new LinkedHashMap<String, AbstractCommandDefinition>();
		populateDefinitions();
	}

	private void populateDefinitions() {
		definitions.put("!id", new SearchCommandHandler());
		definitions.put("!stat", new StatCommandHandler());
		definitions.put("!heal", new CommandDefinition(new CommandAction() {
			@Override
			public String getUsage() {
				return "Usage: !heal [<target>]";
			}

			@Override
			public void doAction(GameCharacter p, CommandArguments args, ClientNoticeStream resp) {
				String targetName = args.extractTarget(null, p.getName());
				if (targetName == null) {
					resp.printErr(getUsage());
					return;
				}
				CommandTarget target = args.getTargetByName(targetName, p);
				if (target == null) {
					resp.printErr("The character " + targetName + " does not exist.");
					resp.printErr(getUsage());
					return;
				}

				List<CommandTarget.CharacterManipulation> changes = new ArrayList<CommandTarget.CharacterManipulation>();
				changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.CANCEL_DEBUFFS, new PlayerStatusEffect[] {
						PlayerStatusEffect.CURSE, PlayerStatusEffect.DARKNESS,
						PlayerStatusEffect.POISON, PlayerStatusEffect.SEAL,
						PlayerStatusEffect.WEAKEN
				}));
				changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_HP, Short.valueOf((short) 30000))); //receiver will limit this to their max HP
				changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_MP, Short.valueOf((short) 30000))); //receiver will limit this to their max MP
				target.mutate(changes);
			}
		}, "Cures a player of all diseases and restores all of his/her HP and MP", UserPrivileges.GM));
		definitions.put("!tp", new CommandDefinition(new CommandAction() {
			@Override
			public String getUsage() {
				return "Usage: !tp <target> <destination>";
			}

			@Override
			public void doAction(GameCharacter p, CommandArguments args, ClientNoticeStream resp) {
				String warpeeName = args.extractTarget(null, null);
				if (warpeeName == null) {
					resp.printErr(getUsage());
					return;
				}
				CommandTarget warpee = args.getTargetByName(warpeeName, p);
				if (warpee == null) {
					resp.printErr("The character " + warpeeName + " does not exist.");
					resp.printErr(getUsage());
					return;
				}

				String warpToName = args.extractTarget(null, null);
				if (warpToName == null) {
					resp.printErr(getUsage());
					return;
				}
				CommandTarget warpTo = args.getTargetByName(warpToName, p);
				if (warpTo == null) {
					resp.printErr("The character " + warpToName + " does not exist.");
					resp.printErr(getUsage());
					return;
				}

				CommandTarget.MapValue destination = (CommandTarget.MapValue) warpTo.access(CommandTarget.CharacterProperty.MAP);
				byte destChannel = ((Byte) warpTo.access(CommandTarget.CharacterProperty.CHANNEL)).byteValue();
				warpee.mutate(Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.CHANGE_MAP, destination)));
				if (destChannel == 0) {
					resp.printOut(warpToName + " is currently offline. " + warpeeName + " has been warped to where he/she will spawn on next login.");
				} else {
					byte sourceChannel = ((Byte) warpee.access(CommandTarget.CharacterProperty.CHANNEL)).byteValue();
					if (sourceChannel == 0) {
						resp.printOut(warpeeName + " is currently offline He/she will spawn on next login where " + warpToName + " is now.");
					} else if (sourceChannel != destChannel) {
						warpee.mutate(Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.CHANGE_CHANNEL, Byte.valueOf(destChannel))));
						resp.printOut(warpeeName + " has been transferred to channel " + destChannel + ".");
					}
				}
			}
		}, "Teleport a player to another", UserPrivileges.GM));
		definitions.put("!town", new TownCommandHandler());
		definitions.put("!map", new CommandDefinition(new CommandAction() {
			@Override
			public String getUsage() {
				return "Usage: !map [-t <target>] <mapid> [<spawn point>]";
			}

			@Override
			public void doAction(GameCharacter p, CommandArguments args, ClientNoticeStream resp) {
				String targetName = args.extractOptionalTarget(p.getName());
				if (targetName == null) {
					resp.printErr(getUsage());
					return;
				}
				CommandTarget target = args.getTargetByName(targetName, p);
				if (target == null) {
					resp.printErr("The character " + targetName + " does not exist.");
					resp.printErr(getUsage());
					return;
				}

				if (!args.hasNext()) {
					resp.printErr(getUsage());
					return;
				}
				int mapId;
				try {
					mapId = Integer.parseInt(args.next());
				} catch (NumberFormatException ex) {
					resp.printErr("Invalid map id.");
					resp.printErr(getUsage());
					return;
				}
				GameMap map = GameServer.getChannel(p.getClient().getChannel()).getMapFactory().getMap(mapId);
				if (map == null) {
					resp.printErr("Invalid map id.");
					resp.printErr(getUsage());
					return;
				}

				byte spawnPoint = 0;
				if (args.hasNext()) {
					String param = args.next();
					try {
						spawnPoint = Byte.parseByte(param);
						if (!map.getStaticData().getPortals().containsKey(Byte.valueOf(spawnPoint))) {
							resp.printErr("Invalid spawn point.");
							resp.printErr(getUsage());
							return;
						}
					} catch (NumberFormatException ex) {
						spawnPoint = map.getPortalIdByName(param);
						if (spawnPoint == -1) {
							resp.printErr("Invalid spawn point.");
							resp.printErr(getUsage());
							return;
						}
					}
				}

				target.mutate(Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.CHANGE_MAP, new CommandTarget.MapValue(mapId, spawnPoint))));
			}
		}, "Warp a player to a specific map and spawn point", UserPrivileges.GM));
		definitions.put("!skill", new CommandDefinition(new CommandAction() {
			@Override
			public String getUsage() {
				return "Usage: !skill [-t <target>] <skillid> <level> [<master level>]";
			}

			@Override
			public void doAction(GameCharacter p, CommandArguments args, ClientNoticeStream resp) {
				int skillId;
				byte skillLevel, masterLevel = -1;
				SkillStats s;

				String targetName = args.extractOptionalTarget(p.getName());
				if (targetName == null) {
					resp.printErr(getUsage());
					return;
				}
				CommandTarget target = args.getTargetByName(targetName, p);
				if (target == null) {
					resp.printErr("The character " + targetName + " does not exist.");
					resp.printErr(getUsage());
					return;
				}

				if (!args.hasNext()) {
					resp.printErr(getUsage());
					return;
				}
				String param = args.next();
				try {
					skillId = Integer.parseInt(param);
					s = SkillDataLoader.getInstance().getSkill(skillId);
					if (s == null)
						throw new NumberFormatException();
				} catch (NumberFormatException e) {
					resp.printErr(param + " is not a valid skillid.");
					resp.printErr(getUsage());
					return;
				}

				if (!args.hasNext()) {
					resp.printErr(getUsage());
					return;
				}
				param = args.next();
				try {
					skillLevel = Byte.parseByte(param);
					if (skillLevel != 0 && s.getLevel(skillLevel) == null)
						throw new NumberFormatException();
				} catch (NumberFormatException e) {
					resp.printErr(param + " is not a valid level of skill " + skillId + ".");
					resp.printErr(getUsage());
					return;
				}

				if (args.hasNext()) {
					param = args.next();
					try {
						masterLevel = Byte.parseByte(param);
						if (masterLevel != 0 && s.getLevel(masterLevel) == null)
							throw new NumberFormatException();
					} catch (NumberFormatException e) {
						resp.printErr(param + " is not a valid master level of skill " + skillId + ".");
						resp.printErr(getUsage());
						return;
					}
				}

				target.mutate(Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_SKILL_LEVEL, new CommandTarget.SkillValue(skillId, skillLevel, masterLevel))));
			}
		}, "Change a player's skill level", UserPrivileges.GM));
		definitions.put("!maxequips", new MaxStatCommandHandlers.MaxEquipStatsHandler());
		definitions.put("!maxskills", new MaxStatCommandHandlers.MaxSkillsHandler());
		definitions.put("!maxall", new MaxStatCommandHandlers.MaxAllHandler());
		definitions.put("!give", new CommandDefinition(new CommandAction() {
			@Override
			public String getUsage() {
				return "Usage: !give [-t <target>] <itemid> [<quantity>]";
			}

			@Override
			public void doAction(GameCharacter p, CommandArguments args, ClientNoticeStream resp) {
				int itemId;
				int quantity = 1;

				String targetName = args.extractOptionalTarget(p.getName());
				if (targetName == null) {
					resp.printErr(getUsage());
					return;
				}
				CommandTarget target = args.getTargetByName(targetName, p);
				if (target == null) {
					resp.printErr("The character " + targetName + " does not exist.");
					resp.printErr(getUsage());
					return;
				}

				if (!args.hasNext()) {
					resp.printErr(getUsage());
					return;
				}
				String param = args.next();
				try {
					itemId = Integer.parseInt(param);
					//TODO: check if item is valid. Throw NumberFormatException if not
				} catch (NumberFormatException e) {
					resp.printErr(param + " is not a valid itemid.");
					resp.printErr(getUsage());
					return;
				}

				if (args.hasNext()) {
					param = args.next();
					try {
						quantity = Integer.parseInt(param);
					} catch (NumberFormatException e) {
						resp.printErr(param + " is not a valid item quantity.");
						resp.printErr(getUsage());
						return;
					}
				}

				target.mutate(Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.ADD_ITEM, new CommandTarget.ItemValue(itemId, quantity))));
			}
		}, "Give a player an item", UserPrivileges.GM));
		definitions.put("!spawn", new SpawnCommandHandler());
		definitions.put("!playernpc", new CommandDefinition(new CommandAction() {
			@Override
			public String getUsage() {
				return "Usage: !playernpc [<scriptid>]";
			}

			@Override
			public void doAction(GameCharacter p, CommandArguments args, ClientNoticeStream resp) {
				int scriptId = 9901000;
				
				if (args.hasNext()) {
					String param = args.next();
					try {
						scriptId = Integer.parseInt(param);
						if (scriptId < 9901000 || scriptId > 9901319) {
							resp.printErr("Scriptid must be between 9901000 and 9901319.");
							return;
						}
					} catch (NumberFormatException e) {
						resp.printErr(param + " is not a valid scriptid.");
						resp.printErr(getUsage());
						return;
					}
				}

				PlayerNpc npc = new PlayerNpc(p, scriptId);
				p.getMap().spawnPlayerNpc(npc);
			}
		}, "Spawn a temporary player NPC of yourself.", UserPrivileges.GM));
		definitions.put("!cleardrops", new CommandDefinition(new CommandAction() {
			@Override
			public String getUsage() {
				return "Usage: !cleardrops";
			}

			private void clearDrop(ItemDrop drop, GameCharacter p) {
				drop.expire();
				p.getMap().destroyEntity(drop);
			}

			@Override
			public void doAction(final GameCharacter p, CommandArguments args, ClientNoticeStream resp) {
				Collection<MapEntity> drops = p.getMap().getAllEntities(EntityType.DROP);
				for (MapEntity ent : drops)
					clearDrop((ItemDrop) ent, p);
			}
		}, "Expires all dropped items on the map.", UserPrivileges.GM));
		definitions.put("!clearmobs", new CommandDefinition(new CommandAction() {
			@Override
			public String getUsage() {
				return "Usage: !clearmobs [-k]";
			}

			@Override
			public void doAction(final GameCharacter p, final CommandArguments args, ClientNoticeStream resp) {
				GameCharacter killer;
				if (args.hasOpt("-k"))
					killer = p;
				else
					killer = null;

				for (MapEntity ent : p.getMap().getAllEntities(EntityType.MONSTER)) {
					Mob mob = (Mob) ent;
					mob.hurt(killer, mob.getHp());
					p.getMap().killMonster(mob, killer);
				}
			}
		}, "Removes all monsters on the map, either killing them for drops and an exp reward (specify with -k) or simply just wipe them out.", UserPrivileges.GM));
		definitions.put("!info", new CommandDefinition(new CommandAction() {
			@Override
			public String getUsage() {
				return "Usage: !info [<target>]";
			}

			@Override
			public void doAction(GameCharacter p, CommandArguments args, ClientNoticeStream resp) {
				String targetName = args.extractTarget(null, p.getName());
				if (targetName == null) {
					resp.printErr(getUsage());
					return;
				}
				CommandTarget target = args.getTargetByName(targetName, p);
				if (target == null) {
					resp.printErr("The character " + targetName + " does not exist.");
					resp.printErr(getUsage());
					return;
				}
				Point pos = (Point) target.access(CommandTarget.CharacterProperty.POSITION);
				byte channel = ((Byte) target.access(CommandTarget.CharacterProperty.CHANNEL)).byteValue();
				StringBuilder sb = new StringBuilder();
				sb.append("PlayerId=").append(target.access(CommandTarget.CharacterProperty.PLAYER_ID));
				if (channel != 0)
					sb.append("; Channel=").append(channel);
				else
					sb.append("; Offline");
				sb.append("; Map=").append(((CommandTarget.MapValue) target.access(CommandTarget.CharacterProperty.MAP)).mapId);
				sb.append("; Position(").append(pos.x).append(",").append(pos.y).append(")");
				resp.printOut(sb.toString());
			}
		}, "Show location info of a player", UserPrivileges.GM));
		definitions.put("!rate", new CommandDefinition(new CommandAction() {
			@Override
			public String getUsage() {
				return "Usage: !rate exp|meso|drop <new rate>";
			}

			@Override
			public void doAction(GameCharacter p, CommandArguments args, ClientNoticeStream resp) {
				short rate;

				if (!args.hasNext()) {
					resp.printErr(getUsage());
					return;
				}
				String key = args.next();

				if (!args.hasNext()) {
					resp.printErr(getUsage());
					return;
				}
				String value = args.next();
				try {
					rate = (short) Math.min(Integer.parseInt(value), Short.MAX_VALUE);
				} catch (NumberFormatException ex) {
					resp.printErr(getUsage());
					return;
				}

				if (key.equalsIgnoreCase("exp")) {
					GameServer.getVariables().setExpRate(rate);
					resp.printOut("The exp rate of this game server has been set to "
							+ rate + ". Changes will be reverted on the next server restart.");
				} else if (key.equalsIgnoreCase("meso")) {
					GameServer.getVariables().setMesoRate(rate);
					resp.printOut("The meso rate of this game server has been set to "
							+ rate + ". Changes will be reverted on the next server restart.");
				} else if (key.equalsIgnoreCase("drop")) {
					GameServer.getVariables().setDropRate(rate);
					resp.printOut("The drop rate of this game server has been set to "
							+ rate + ". Changes will be reverted on the next server restart.");
				} else {
					resp.printErr(getUsage());
				}
			}
		}, "Change the exp, meso, or drop rate of this game server.",
				UserPrivileges.SUPER_GM));
		definitions.put("!who", new CommandDefinition(new CommandAction() {
			@Override
			public String getUsage() {
				return "Usage: !who [<minimum privilege level>]";
			}

			@Override
			public void doAction(GameCharacter p, CommandArguments args, ClientNoticeStream resp) {
				byte privilegeLevelLimit = UserPrivileges.USER;
				if (args.hasNext()) {
					String param = args.next();
					try {
						privilegeLevelLimit = Byte.parseByte(param);
					} catch (NumberFormatException e) {
						resp.printErr(getUsage());
						return;
					}
				}

				StringBuilder sb = new StringBuilder();
				for (GameCharacter c : GameServer.getChannel(p.getClient().getChannel()).getConnectedPlayers())
					if (c.getPrivilegeLevel() >= privilegeLevelLimit)
						sb.append(c.getName()).append(",");
				if (sb.length() > 0) //remove terminal delimiter
					sb.delete(sb.length() - 1, sb.length());
				resp.printOut("Connected users: " + sb);
			}
		}, "Lists all online users in this channel (and optionally filter them by privilege level).",
				UserPrivileges.GM));
		definitions.put("!uptime", new CommandDefinition(new CommandAction() {
			@Override
			public String getUsage() {
				return "Usage: !uptime [-gc]";
			}

			@Override
			public void doAction(GameCharacter p, CommandArguments args, ClientNoticeStream resp) {
				if (args.hasOpt("-gc"))
					System.gc();

				long startMillis = GameServer.getChannel(p.getClient().getChannel()).getTimeStarted();
				long upTimeMillis = System.currentTimeMillis() - startMillis;
				DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.LONG);
				resp.printOut("This game server was started on " + fmt.format(startMillis) + ".");

				long upDays = upTimeMillis / (1000 * 60 * 60 * 24);
				long upHours = (upTimeMillis % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
				long upMinutes = ((upTimeMillis % (1000 * 60 * 60 * 24)) % (1000 * 60 * 60)) / (1000 * 60);
				long upSeconds = (((upTimeMillis % (1000 * 60 * 60 * 24)) % (1000 * 60 * 60)) % (1000 * 60)) / 1000;
				String upTimeStr = "It has been up for ";
				if (upDays > 0)
					upTimeStr += upDays + (upDays != 1 ? " days, " : " day, ");
				if (upHours > 0)
					upTimeStr += upHours + (upHours != 1 ? " hours, " : " hour, ");
				if (upMinutes > 0)
					upTimeStr += upMinutes + (upMinutes != 1 ? " minutes, " : " minute, ");
				if (upSeconds > 0)
					upTimeStr += upSeconds + (upSeconds != 1 ? " seconds, " : " second, ");
				resp.printOut(upTimeStr.substring(0, upTimeStr.length() - 2) + ".");

				long heapNow = Runtime.getRuntime().totalMemory() / (1024 * 1024);
				long heapMax = Runtime.getRuntime().maxMemory() / (1024 * 1024);
				long heapFree = Runtime.getRuntime().freeMemory() / (1024 * 1024);
				resp.printOut("Current heap usage: " + (heapNow - heapFree) + "MB/" + heapNow + "MB. "
						+ "Can add a max of " + (heapMax - (heapNow - heapFree)) + "MB to heap without OutOfMemoryError.");
			}
		}, "Print general info about the server's resource usage. Pass -gc flag to attempt to run the garbage collector before collecting heap info.", UserPrivileges.ADMIN));
		definitions.put("!shutdown", new ShutdownCommandHandler());
		definitions.put("!help", new CommandDefinition(new HelpCommandHandler(),
				"Lists available commands and their descriptions. Specify a command to read only its description.", UserPrivileges.USER));
	}

	public void process(GameCharacter p, String line) {
		String[] args = argSplit.split(line);
		AbstractCommandDefinition def = definitions.get(args[0].toLowerCase());
		ClientNoticeStream resp = new ClientNoticeStream(p.getClient());
		if (def != null && p.getPrivilegeLevel() >= def.minPrivilegeLevel()) {
			CommandArguments argsContainer = new CommandArguments(args);
			if (!argsContainer.hasOpt("--help")) {
				def.execute(p, argsContainer, resp);
			} else {
				resp.printOut(def.getUsage());
				resp.printOut(def.getHelpMessage());
			}
		} else {
			resp.printErr(args[0] + " is not a valid command. Type !help to get a list of valid commands.");
		}
	}

	public static CommandProcessor getInstance() {
		return singleton;
	}

	private class HelpCommandHandler implements CommandAction {
		@Override
		public String getUsage() {
			return "Usage: !help [<command>]";
		}

		@Override
		public void doAction(GameCharacter p, CommandArguments args, ClientNoticeStream resp) {
			if (!args.hasNext()) {
				for (Entry<String, AbstractCommandDefinition> entry : definitions.entrySet())
					if (p.getPrivilegeLevel() >= entry.getValue().minPrivilegeLevel())
						resp.printOut(entry.getKey() + " - " + entry.getValue().getHelpMessage());
			} else {
				String param = args.next();
				AbstractCommandDefinition def = definitions.get(param.toLowerCase());
				if (def == null || p.getPrivilegeLevel() < def.minPrivilegeLevel()) {
					resp.printErr(param + " is not a valid command.");
					resp.printErr(getUsage());
					return;
				}
				resp.printOut(param + " - " + def.getHelpMessage());
			}
		}
	}
}
