package nxt.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import nxt.crypto.Crypto;
import nxt.crypto.Curve25519;
import nxt.util.Convert;

public class NFDDistribution {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		final long ONE_NFD = 1; // for testing normally should be 100000000
		final String secret = "richwang"; // account with nfd for distribution
		final boolean check = false;

		//expected file UTF-8, tab separated, 3 columns, name paaword hash amountNFD
		//NFD fractional allowed
		try {
			File fileDir = new File(args[0]);

			BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(fileDir), "UTF8"));

			String str;
			byte[] privateKeyHash;

			while ((str = in.readLine()) != null) {

				String[] investors = str.split("\t");

				String sha256 = investors[1];
				try {
					privateKeyHash = Convert.parseHexString(sha256);
				} catch (NumberFormatException nfe) {
					//TODO write in error file for further investigations
					System.out.println(investors[0] + " wrong hashcode");
					continue;
				}
				
				// Get public key from hash of private key
				byte[] publicKey = new byte[32];
				try {
					Curve25519.keygen(publicKey, null, privateKeyHash);
				} catch (IndexOutOfBoundsException ioobe) {
					System.out.println(investors[0] + " wrong hashcode!");
					continue;
				}

				// To generate account ID, we need hash of public key
				byte[] publicKeyHash = Crypto.sha256().digest(publicKey);

				Long accountIdLong = Convert.fullHashToId(publicKeyHash);

				// Make the Long human-readable
				String accountId = Convert.toUnsignedLong(accountIdLong);
				if (!check) {
					try {
						sendPostRequest(
								secret,
								accountId,
								(long) Math.round(Double
										.parseDouble(investors[2]) * ONE_NFD));
					} catch (Exception e) {
						//TODO write in error file for further investigations
						System.out.println(investors[0]
								+ "\tcouldn't sent transaction");
					}
				} else {
					System.out.println(accountId
							+ " gets\t\t"
							+ Double.parseDouble(investors[2])
							+ " NFD send: "
							+ (long) Math.round(Double
									.parseDouble(investors[2]) * ONE_NFD));
				}
			}

			in.close();
		} catch (UnsupportedEncodingException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	private static void sendPostRequest(String secretPhrase, String accountID,
			Long amount) throws Exception {

		String httpURL = "http://localhost:8876/nxt?requestType=sendMoney&secretPhrase="
				+ secretPhrase
				+ "&recipient="
				+ accountID
				+ "&amountNQT="
				+ amount
				+ "&feeNQT="
				+ 100000000
				+ "&deadline=1440&referencedTransaction=&publicKey=";
		URL myurl = new URL(httpURL);
		HttpURLConnection con = (HttpURLConnection) myurl.openConnection();
		InputStream inputStream = con.getInputStream();
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		BufferedReader in = new BufferedReader(inputStreamReader);

		String inputLine;

		while ((inputLine = in.readLine()) != null) {
			System.out.println(inputLine);
		}

		in.close();

	}

	public static Long getId(byte[] publicKey) {
		byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
		return Convert.fullHashToId(publicKeyHash);
	}

}
