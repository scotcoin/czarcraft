package nfd.util.NSCAssets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import nxt.util.Logger;

public class APIRequest {

	static {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			;
		}

		javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {

			public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {

				return true;

			}
		});
	}

	public static StringBuffer sendRequest(String urlRequest) {

		URL myurl = null;
		try {
			myurl = new URL(urlRequest);
		} catch (MalformedURLException mue) {
			Logger.logDebugMessage("MalformedURLException: " + urlRequest, mue);
			return null;
		}

		InputStream inputStream = null;

		if (myurl.getProtocol().toLowerCase().equals("https")) {

			HttpsURLConnection con = null;
			int tries = 0;
			while (con == null && tries <= 3) {
				try {
					con = (HttpsURLConnection) myurl.openConnection();
					con.setRequestMethod("POST");
				} catch (IOException e) {
					Logger.logDebugMessage("", e);
					Logger.logWarningMessage("Connection problem, sleep 3 seconds and try again.");
					con = null;
					sleep(3000);
					tries++;
				}
			}

			try {
				inputStream = con.getInputStream();
			} catch (IOException e) {
				Logger.logErrorMessage("", e);
				return null;
			}
		} else if (myurl.getProtocol().toLowerCase().equals("http")) {
			HttpURLConnection con = null;
			int tries = 0;
			while (con == null && tries <= 3) {
				try {
					con = (HttpURLConnection) myurl.openConnection();
					con.setRequestMethod("POST");
				} catch (IOException e) {
					Logger.logDebugMessage("", e);
					Logger.logWarningMessage("Connection problem, sleep 3 seconds and try again.");
					con = null;
					sleep(3000);
					tries++;
				}
			}

			try {
				inputStream = con.getInputStream();
			} catch (IOException e) {
				Logger.logErrorMessage("", e);
				return null;
			}
		} else {
			return null;
		}

		if (inputStream != null) {
			StringBuffer inputBuffer = new StringBuffer();

			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			BufferedReader in = new BufferedReader(inputStreamReader);

			String inputLine;
			try {
				while ((inputLine = in.readLine()) != null) {
					inputBuffer.append(inputLine);
				}
				in.close();
			} catch (IOException ioException) {
				Logger.logDebugMessage("", ioException);
				return null;
			}
			return inputBuffer;
		}

		return null;

	}

	static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ie) {
			// ignore Handle
		}
	}
}
