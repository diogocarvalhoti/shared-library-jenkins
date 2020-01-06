package br.gov.mds

import com.cloudbees.groovy.cps.NonCPS

@Grab('com.github.zafarkhaja:java-semver:0.9.0')
import com.github.zafarkhaja.semver.Version

public class GitFlow implements Serializable {

	private static final String BASE_URL = "http://sugitpd02.mds.net"
	private static final String PRIVATE_TOKEN = "u3xBWdP3KUxxG7PQYm_t"

	public Integer getIdProject(String namespace) {
		String uri = new StringBuilder(this.BASE_URL)
				.append("/api/v4/projects/").append(namespace).toString()
		Map resultMap = (Map) GitRestClient.get(uri, this.PRIVATE_TOKEN)
		return resultMap.get("id")
	}

	public String createMR(Integer idProject, String sourceBranch) {
		Map<String, String> params = new HashMap();
		params.put("remove_source_branch", "true");
		params.put("source_branch", sourceBranch);
		params.put("target_branch", "develop");
		params.put("title", "Merge Request da branch: " + sourceBranch);

		String uri = new StringBuilder(this.BASE_URL)
				.append("/api/v4/projects/").append(idProject).append("/merge_requests").toString();

		return GitRestClient.post(uri, this.PRIVATE_TOKEN, params)
	}

	public List<String> getFeatures(Integer idProject){
		String uri = new StringBuilder(this.BASE_URL)
				.append("/api/v4/projects/").append(idProject).append("/repository/branches").toString()
		List branches = GitRestClient.get(uri, this.PRIVATE_TOKEN)

		List<String> features = new ArrayList();

		for (branch in branches) {
			String nomeBranch = branch.getAt("name");
			if(nomeBranch.startsWith("feature/")) {
				features.add(nomeBranch);
			}
		}
		return features;
	}

	@NonCPS
	public String getNextVersion(Integer idProject, String semVerType) {
		def ultimaTag = this.getUltimaTag(idProject)
		Version version = Version.valueOf(ultimaTag);

		if(BranchUtil.TypesVersion.MAJOR.toString().equals(semVerType)) {
			return version.incrementMajorVersion()
		} else if(BranchUtil.TypesVersion.MINOR.toString().equals(semVerType)) {
			return version.incrementMinorVersion()
		} else if(BranchUtil.TypesVersion.PATCH.toString().equals(semVerType)) {
			return version.incrementPatchVersion()
		}
		return version.incrementPreReleaseVersion();
	}

	@NonCPS
	private String getUltimaTag(Integer idProject){
		String uri = new StringBuilder(this.BASE_URL)
				.append("/api/v4/projects/").append(idProject)
				.append("/repository/tags").append("?order_by=name").toString();

				Map<String, String> params = new HashMap();
				params.put("order_by", "name");

		List tags = GitRestClient.get(uri, this.PRIVATE_TOKEN)

		if(!tags.empty) {
			Map tag = tags.get(0);
			return tag.get("name")
		}
		return "0.0.0"
	}

	def versionarArtefato(steps, linguagem, pathArtefato, nextVersion){
		if("JAVA" == linguagem) {
			steps.withMaven(maven: 'Maven 3.6.2') {
				steps.sh "mvn versions:set -DgenerateBackupPoms=false -DnewVersion=${nextVersion} -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true"
			}
		} else if("PHP" == linguagem) {
			steps.contentReplace(
			    configs: [
			        steps.fileContentReplaceConfig(
			            configs: [
			                steps.fileContentReplaceItemConfig(
			                    search: '(APP_VERSION=)\\d+.\\d+.\\d+',
			                    replace: nextVersion,
			                    matchCount: 1)
			                ],
			            fileEncoding: 'UTF-8',
			            filePath: pathArtefato)
			        ])
		} else if("NODE" == linguagem) {
			//TODO A implementar
		}
		return steps
	}


	//	public static void main(String[] args) {
	//		def baseUrl = 'http://sugitpd02.mds.net'
	//		def privateToken = 'u3xBWdP3KUxxG7PQYm_t'
	//		GitFlow flow = new GitFlow(baseUrl, privateToken)
	//		def tag = flow.getNextVersion(559, SemVerTypeEnum.PATCH);
	//		System.out.println(tag);
	//	}
}
