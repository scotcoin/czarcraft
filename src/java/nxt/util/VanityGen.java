package nxt.util;

import java.util.Random;

import nxt.crypto.Crypto;
import nxt.util.Convert;

public class VanityGen {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String someDigits = "0123456789";
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		String accountId;
		String randomSecret = null;
		int maxLength = 100;
		long counter = 0;
		while (true) {
			counter++;
			randomSecret = generate(100, someDigits, alphabet);
			long accounIdLong = getId(Crypto.getPublicKey(randomSecret));
			accountId = Convert.toUnsignedLong(accounIdLong);
			if (accountId.length() < maxLength) {
				maxLength = accountId.length();
				System.out.println(counter + "\t" + accountId.length() + "\t"
						+ accountId + "\t" + randomSecret + "\tNFD-"
						+ Crypto.rsEncode(accounIdLong));
			}

		}

	}

	public static Long getId(byte[] publicKey) {
		byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
		return Convert.fullHashToId(publicKeyHash);
	}

	/**
	 * random password generator
	 * http://codekicker.de/fragen/Java-Passwort-generieren-lassen/267
	 * 
	 * 
	 * Random object for choosing and shuffling characters from StringBuffers
	 */
	private static Random random = new Random();

	/**
	 * Creates a String of the given length, containing random characters from
	 * the given alphabets, but at least one character from each alphabet. If
	 * there are more than 'length' alphabets, one random character is chosen
	 * from each of the 'length' first alphabets. <br />
	 * <br />
	 * At least one alphabet must be given. If an empty String or
	 * <code>null</code> is encountered as an alphabet, an
	 * IllegalArgumentException is thrown.
	 * 
	 * @param length
	 *            The length of the string to generate
	 * @param alphabets
	 *            The alphabets to use
	 * @return The generated string
	 * @throws IllegalArgumentException
	 *             if not alphabets are given, or an empty of <code>null</code>
	 *             alphabet is encountered
	 */
	public static String generate(int length, String... alphabets) {
		if (alphabets.length == 0) {
			throw new IllegalArgumentException(
					"At least one alphabet must be given");
		}
		StringBuffer result = new StringBuffer();
		StringBuffer all = new StringBuffer();
		for (int i = 0; i < alphabets.length; i++) {
			if (alphabets[i] == null || alphabets[i].equals("")) {
				throw new IllegalArgumentException("Invalid alphabet: "
						+ alphabets[i]);
			}
			StringBuffer sb = new StringBuffer(alphabets[i]);
			result.append(selectRandom(sb, 1));
			if (result.length() == length) {
				return shuffle(result).toString();
			}
			all.append(sb);
		}
		result.append(selectRandom(all, length - result.length()));
		return shuffle(result).toString();
	}

	/**
	 * Returns a StringBuffer containing 'n' characters chosen randomly from the
	 * given alphabet.
	 * 
	 * @param alphabet
	 *            The alphabet to choose from
	 * @param n
	 *            The number of characters to choose
	 * @return A StringBufer with 'n' characters chosen randomly from the given
	 *         alphabet
	 */
	private static StringBuffer selectRandom(StringBuffer alphabet, int n) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < n; i++) {
			sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
		}
		return sb;
	}

	/**
	 * Shuffles the given StringBuffer in place and returns it
	 * 
	 * @param sb
	 *            The StringBuffer to shuffle and return
	 * @return The given StringBuffer, shuffled randomly
	 */
	private static StringBuffer shuffle(StringBuffer sb) {
		for (int i = 0; i < sb.length(); i++) {
			int i0 = random.nextInt(sb.length());
			int i1 = random.nextInt(sb.length());
			char c = sb.charAt(i0);
			sb.setCharAt(i0, sb.charAt(i1));
			sb.setCharAt(i1, c);
		}
		return sb;
	}

}
