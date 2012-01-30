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

package argonms.common.net;

import argonms.common.util.Rng;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 *
 * @author GoldenKevin
 */
public class HashFunctions {
	public static final Charset ASCII = Charset.forName("US-ASCII");

	private static ThreadLocal<MessageDigest> sha1digest = new ThreadLocal<MessageDigest>() {
		@Override
		public MessageDigest initialValue() {
			try {
				return MessageDigest.getInstance("SHA-1");
			} catch (NoSuchAlgorithmException ex) {
				//assert false; //should never happen
				return null;
			}
		}
	};

	private static ThreadLocal<MessageDigest> sha512digest = new ThreadLocal<MessageDigest>() {
		@Override
		public MessageDigest initialValue() {
			try {
				return MessageDigest.getInstance("SHA-512");
			} catch (NoSuchAlgorithmException ex) {
				//assert false; //should never happen
				return null;
			}
		}
	};

	private HashFunctions() {
		
	}

	private static byte[] hashWithDigest(byte[] in, MessageDigest digester) {
		digester.update(in, 0, in.length);
		return digester.digest();
	}

	private static byte[] hexSha1(byte[] in) {
		return hashWithDigest(in, sha1digest.get());
	}

	private static byte[] hexSha512(byte[] in) {
		return hashWithDigest(in, sha512digest.get());
	}

	public static boolean checkSha1Hash(byte[] actualHash, String check) {
		return Arrays.equals(actualHash, hexSha1(check.getBytes(ASCII)));
	}

	public static boolean checkSha512Hash(byte[] actualHash, String check) {
		return Arrays.equals(actualHash, hexSha512(check.getBytes(ASCII)));
	}

	private static byte[] concat(String password, byte[] salt) {
		byte[] concat = new byte[password.length() + salt.length];
		System.arraycopy(password.getBytes(ASCII), 0, concat, 0, password.length());
		System.arraycopy(salt, 0, concat, password.length(), salt.length);
		return concat;
	}

	public static boolean checkSaltedSha1Hash(byte[] actualHash, String check, byte[] salt) {
		return Arrays.equals(actualHash, hexSha1(concat(check, salt)));
	}

	public static boolean checkSaltedSha512Hash(byte[] actualHash, String check, byte[] salt) {
		return Arrays.equals(actualHash, hexSha512(concat(check, salt)));
	}

	public static byte[] makeSalt() {
		byte[] salt = new byte[16];
		Rng.getGenerator().nextBytes(salt);
		return salt;
	}

	public static byte[] makeSaltedSha512Hash(String password, byte[] salt) {
		return hexSha512(concat(password, salt));
	}
}