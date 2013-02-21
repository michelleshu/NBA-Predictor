import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtil {

	/**
	 * Send an HTTP GET request, and return the response data
	 * 
	 * @param serverUrl
	 * @return
	 */
	public static String getData(String serverUrl) {
		HttpURLConnection conn = null;
		String response = null;
		try {
			URL url = new URL(serverUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("charset", "utf-8");
			conn.setUseCaches(false);
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Language", "en-US");
			conn.setReadTimeout(10000);
//			conn.connect();

			// read response from the server
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			StringBuffer sb = new StringBuffer();
			String line = null;
			while ((line = rd.readLine()) != null) {
				sb.append(line + '\n');
			}
			rd.close();
			response = sb.toString();
		} catch (Exception e) {
			System.out.println("Failed to get data from HTTP request: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
		return response;
	}

	/**
	 * Send an HTTP POST request with list of parameters, return the response message.
	 * Use this method if parameter list is too long for an HTTP GET request
	 * 
	 * @param serverUrl the HTTP server URL
	 * @param parameters the request parameters, e.g., terms=no_value&terms=201209&terms=201301&depts=no_value&depts=COSC&depts=PSYC&sortorder=dept
	 * @return
	 */
	public static String postParameters(String serverUrl, String parameters) {
		HttpURLConnection conn = null;
		String response = null;
		try {
			URL url = new URL(serverUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setConnectTimeout(5000);
			conn.setRequestProperty("Connection", "close");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("Content-Length", ""
					+ parameters.getBytes().length);
			// conn.setRequestProperty("Content-Language", "en-US");
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			// connection.setReadTimeout(10000);
			// conn.connect();

			// post data to the server
			DataOutputStream os = new DataOutputStream(conn.getOutputStream());
			os.writeBytes(parameters);
			os.flush();
			os.close();

			// read response from the server
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			StringBuffer sb = new StringBuffer();
			String line = null;
			while ((line = rd.readLine()) != null) {
				sb.append(line + '\n');
			}
			rd.close();
			response = sb.toString();
		} catch (Exception e) {
			System.out.println("Failed to post HTTP with parameters: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
		return response;
	}
	
	/**
	 * Send an HTTP POST request to invoke SOAP web-service, return the response message.
	 * Update the request properties to support other types of POST requests.
	 * 
	 * @param serverUrl
	 * @param content
	 * @return
	 */
	public static String postData(String serverUrl, String content) {
		HttpURLConnection conn = null;
		String response = null;
		try {
			URL url = new URL(serverUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setConnectTimeout(5000);
			conn.setRequestProperty("Connection", "close");
			conn.setRequestProperty("Content-Type", "application/soap+xml");
			conn.setRequestProperty("SoapAction", "");
			conn.setRequestProperty("Content-Length", ""
					+ content.getBytes().length);
			// conn.setRequestProperty("Content-Language", "en-US");
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			// connection.setReadTimeout(10000);
			// conn.connect();

			// post data to the server
			DataOutputStream os = new DataOutputStream(conn.getOutputStream());
			os.writeBytes(content);
			os.flush();
			os.close();

			// read response from the server
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			StringBuffer sb = new StringBuffer();
			String line = null;
			while ((line = rd.readLine()) != null) {
				sb.append(line + '\n');
			}
			rd.close();
			response = sb.toString();
		} catch (Exception e) {
			System.out.println("Failed to post HTTP request: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
		return response;
	}

	/**
	 * Unit tests
	 * @param args
	 */
	public static void main(String[] args) {

	}
}
