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

package argonms.net.client;

/**
 *
 * @author GoldenKevin
 */
public final class ClientSendOps {
	public static final short
		LOGIN_RESULT = 0x00,
		SERVERLOAD_MSG = 0x03,
		GENDER_DONE = 0x04,
		PIN_RESPONSE = 0x06,
		PIN_ASSIGNED = 0x07,
		ALL_CHARLIST = 0x08,
		WORLD_ENTRY = 0x0A,
		CHARLIST = 0x0B,
		CHANNEL_ADDRESS = 0x0C,
		CHECK_NAME_RESP = 0x0D,
		CHAR_CREATED = 0x0E,
		DELETE_CHAR_RESPONSE = 0x0F,
		PING = 0x11,
		RELOG_RESPONSE = 0x16
	;
	
	private ClientSendOps() {
		//uninstantiable...
	}
}
