package nfd.util.NSCAssets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import nxt.util.Logger;

public class HttpsAPIRequests {

	static {
	TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public void checkClientTrusted(X509Certificate[] certs,
				String authType) {
		}

		public void checkServerTrusted(X509Certificate[] certs,
				String authType) {
		}
	} };

	// Install the all-trusting trust manager
	try {
		SSLContext sc = SSLContext.getInstance("TLS");
		sc.init(null, trustAllCerts, new SecureRandom());
		HttpsURLConnection
				.setDefaultSSLSocketFactory(sc.getSocketFactory());
	} catch (Exception e) {
		;
	}

	javax.net.ssl.HttpsURLConnection
			.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {

				public boolean verify(String hostname,
						javax.net.ssl.SSLSession sslSession) {

					return true;

				}
			});
	}
	
	public static StringBuffer sendHTTPSPostRequest(String urlRequest) {

		if (!(urlRequest.toLowerCase().startsWith("https://"))) return null;
		
		URL myurl = null;
		try {
			myurl = new URL(urlRequest);
		} catch (MalformedURLException mue) {
			Logger.logDebugMessage("MalformedURLException: "+urlRequest, mue);
			return null;
		}

		HttpsURLConnection con = null;

			try {
				con = (HttpsURLConnection) myurl.openConnection();
				con.setRequestMethod("POST");
			} catch (IOException e) {
				Logger.logDebugMessage("",e);
				return null;
			}

		InputStream inputStream = null;
		try {
			inputStream = con.getInputStream();
		} catch (IOException e) {
			Logger.logDebugMessage("",e);			
			return null;
		}

		if (inputStream != null) {
			StringBuffer inputBuffer=new StringBuffer();
			
			InputStreamReader inputStreamReader = new InputStreamReader(
					inputStream);
			BufferedReader in = new BufferedReader(inputStreamReader);

			String inputLine;
			
			try {
				while ((inputLine = in.readLine()) != null) {
					inputBuffer.append(inputLine);
				}
				in.close();
			} catch (IOException e) {
				Logger.logDebugMessage("",e);				
				return null;
			}
			return inputBuffer;

		}
		return null;

	}
}
