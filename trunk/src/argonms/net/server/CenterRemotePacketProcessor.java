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

package argonms.net.server;

import argonms.LocalServer;
import argonms.tools.input.LittleEndianReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public abstract class CenterRemotePacketProcessor {
	private static final Logger LOG = Logger.getLogger(CenterRemotePacketProcessor.class.getName());

	public abstract void process(LittleEndianReader packet, RemoteCenterInterface s);

	protected void processAuthResponse(LittleEndianReader packet, LocalServer ls) {
		String error = packet.readLengthPrefixedString();
		if (error.length() != 0)
			LOG.log(Level.SEVERE, "Unable to auth with center server: {0}", error);
		else
			ls.centerConnected();
	}
}
