package br.gov.mds

import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonSlurperClassic

class RestClient {
	
	private String baseUrl;
	private String token;
	
	public RestClient(baseUrl, token) {
		this.baseUrl = baseUrl;
		this.token = token;
	}
	
	public Integer getIdProject(String namespace, String nameProject) {
		Map resultMap = get(this.baseUrl.concat("/api/v4/projects/gcm_cgsi%2Fsispaa"))
		return resultMap.get("id")
	}
	
	public void createMR(url, token, sourceBranch, targetBranch) {
		
		
	}
	
	@NonCPS
	private Map get(url) {
		HttpURLConnection connection = url.openConnection()
		
		println this.token
		
		if (this.token != null && this.token.length() > 0) {
			connection.setRequestProperty("Private-Token", this.token)
		}
		
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
	
}
