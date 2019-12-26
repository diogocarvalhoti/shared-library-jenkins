package br.gov.mds

public class GitFlow {

	private String baseUrl;
	private String token;

	public GitFlow(baseUrl, token) {
		this.baseUrl = baseUrl;
		this.token = token;
		System.out.println(this.baseUrl);
	}

	public Integer getIdProject(String namespace) {
		String uri = new StringBuilder(this.baseUrl)
				.append("/api/v4/projects/").append(namespace).toString()
		Map resultMap = RestClient.get(uri)
		return resultMap.get("id")
	}

	public String createMR(Integer idProject, String sourceBranch) {
		Map<String, String> params = new HashMap();
		params.put("remove_source_branch", "true");
		params.put("source_branch", sourceBranch);
		params.put("target_branch", "develop");
		params.put("title", "MR da branch: " + sourceBranch);

		String uri = new StringBuilder(this.baseUrl)
				.append("/api/v4/projects/").append(idProject).append("/merge_requests").toString();

		return RestClient.post(uri, params)
	}
}
