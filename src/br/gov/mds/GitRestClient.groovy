package br.gov.mds;

import com.cloudbees.groovy.cps.NonCPS

import groovy.json.JsonSlurperClassic

public final class GitRestClient {
	
	private GitRestClient() {
		super();
	}
	
	@NonCPS
	public static Map get(String uri, String token) {
		HttpURLConnection connection = new URL(uri).openConnection()
		connection.setRequestProperty("Private-Token", token)
		connection.setRequestMethod("GET")
		connection.setDoInput(true)
		connection.setDoOutput(true)
		def rs = null
		try {
			connection.connect()
			rs = new JsonSlurperClassic().parse(new InputStreamReader(connection.getInputStream(), "UTF-8"))
		} finally {
			connection.disconnect()
		}
		return rs
	}

	@NonCPS
	public static String post(String uri, String token, Map params) {
		String response = "";
		try {
			HttpURLConnection connection = new URL(uri).openConnection();
			connection.setReadTimeout(15000);
			connection.setConnectTimeout(15000);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Private-Token", token)
			connection.setDoInput(true);
			connection.setDoOutput(true);

			OutputStream os = connection.getOutputStream();
			BufferedWriter writer = new BufferedWriter(
					new OutputStreamWriter(os, "UTF-8"));

			writer.write(getPostDataString(params));

			writer.flush();
			writer.close();
			os.close();
			int responseCode = connection.getResponseCode();

			if (responseCode == HttpURLConnection.HTTP_OK) {
				String line;
				BufferedReader br=new BufferedReader(new InputStreamReader(connection.getInputStream()));
				while ((line=br.readLine()) != null) {
					response+=line;
				}
			}
			else {
				response="";
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return response;
	}

	@NonCPS
	private static String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException{
		StringBuilder result = new StringBuilder();
		boolean first = true;
		for(Map.Entry<String, String> entry : params.entrySet()){
			if (first) {
				first = false;
			}else {
				result.append("&");
			}
			result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
		}

		return result.toString();
	}
}