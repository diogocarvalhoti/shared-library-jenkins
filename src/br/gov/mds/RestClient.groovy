package br.gov.mds

import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonSlurperClassic
import groovy.transform.MapConstructor

class RestClient {

	private String baseUrl;
	private String token;

	public RestClient(baseUrl, token) {
		this.baseUrl = baseUrl;
		this.token = token;
	}

	public Integer getIdProject(String namespace) {
		Map resultMap = get(this.baseUrl.concat("/api/v4/projects/").concat(namespace))
		return resultMap.get("id")
	}

	public void createMR(Integer idProject, String sourceBranch, String targetBranch) {
		Map<String, String> params = new HashMap();
		params.put("source_branch", sourceBranch);
		params.put("target_branch", targetBranch);
		params.put("title", "Teste MR");
		
		String uri = this.baseUrl.concat("/api/v4/projects/").concat(idProject).concat("merge_requests")
		
		post(uri, params)
	}

	@NonCPS
	private Map get(String uri) {
		HttpURLConnection connection = new URL(uri).openConnection()
		connection.setRequestProperty("Private-Token", this.token)
		connection.setRequestMethod("GET")
		connection.setDoInput(true)
		connection.setDoOutput(true)
		def rs = null
		try {
			connection.connect()
			def responseCode = connection.getResponseCode()
			println "Response Code: "+responseCode
			rs = new JsonSlurperClassic().parse(new InputStreamReader(connection.getInputStream(), "UTF-8"))
		} finally {
			connection.disconnect()
		}
		return rs
	}

	@NonCPS
	private String post(String uri, Map params) {
		String response = "";
		HttpURLConnection connection = new URL(uri).openConnection();
		connection.setReadTimeout(15000);
		connection.setConnectTimeout(15000);
		connection.setRequestMethod("POST");
		connection.setDoInput(true);
		connection.setDoOutput(true);

		try {
			OutputStream os = connection.getOutputStream();
			BufferedWriter writer = new BufferedWriter(
					new OutputStreamWriter(os, "UTF-8"));


			writer.write(getPostDataString(params));

			writer.flush();
			writer.close();
			os.close();
			int responseCode = connection.getResponseCode();

			if (responseCode == HttpsURLConnection.HTTP_OK) {
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
			e.printStackTrace();
		} finally {
			connection.disconnect();
		}
		return response;
	}

	private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException{
		StringBuilder result = new StringBuilder();
		boolean first = true;
		for(Map.Entry<String, String> entry : params.entrySet()){
			if (first)
				first = false;
			else
				result.append("&");

			result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
		}

		return result.toString();
	}
}