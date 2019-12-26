package br.gov.mds

import java.beans.FeatureDescriptor

public class GitFlow {

	private String baseUrl;
	private String token;

	public GitFlow(String baseUrl, String token) {
		this.baseUrl = baseUrl;
		this.token = token;
	}

	public Integer getIdProject(String namespace) {
		String uri = new StringBuilder(this.baseUrl)
				.append("/api/v4/projects/").append(namespace).toString()
		Map resultMap = (Map) GitRestClient.get(uri, this.token)
		return resultMap.get("id")
	}

	public String createMR(Integer idProject, String sourceBranch) {
		Map<String, String> params = new HashMap();
		params.put("remove_source_branch", "true");
		params.put("source_branch", sourceBranch);
		params.put("target_branch", "develop");
		params.put("title", "Merge Request da branch: " + sourceBranch);

		String uri = new StringBuilder(this.baseUrl)
				.append("/api/v4/projects/").append(idProject).append("/merge_requests").toString();

		return GitRestClient.post(uri, this.token, params)
	}

	public List<String> getFeatures(Integer idProject){
		String uri = new StringBuilder(this.baseUrl)
				.append("/api/v4/projects/").append(idProject).append("/repository/branches").toString()
		List branches = GitRestClient.get(uri, this.token)

		List<String> features = new ArrayList();
		
		for (branch in branches) {
			String nomeBranch = branch.getAt("name");
			if(nomeBranch.startsWith("feature/")) {
				features.add(nomeBranch);
			}
		}
		return features;
	}

//	public static void main(String[] args) {
//		def baseUrl = 'http://sugitpd02.mds.net'
//		def privateToken = 'u3xBWdP3KUxxG7PQYm_t'
//		GitFlow flow = new GitFlow(baseUrl, privateToken)
//		def idProject = 559
//		def features = flow.getFeatures(idProject);
//		for (var in features) {
//			System.out.println(var);
//		}
//	}
}
