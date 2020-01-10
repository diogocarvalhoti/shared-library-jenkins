package br.gov.mds

import com.cloudbees.groovy.cps.NonCPS

@Grab('com.github.zafarkhaja:java-semver:0.9.0')
import com.github.zafarkhaja.semver.Version

public class GitFlow implements Serializable {

	private static final String BASE_URL = "http://sugitpd02.mds.net"
	private static final String PRIVATE_TOKEN = "u3xBWdP3KUxxG7PQYm_t"

	private static final String RC = "-rc"

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
	public String getNextVersion(Integer idProject, String semVerType, boolean isRC) {
		def ultimaTag = this.getUltimaTag(idProject)

		if(isRC) {
			if(ultimaTag.contains(RC)) {
				Version version = Version.valueOf(ultimaTag);
				return version.incrementPreReleaseVersion(); 
			} else {
				String nextVersion = incrementarVersao(ultimaTag, semVerType);
				Version version = Version.valueOf(nextVersion.concat(RC));
				return version.incrementPreReleaseVersion();
			}
		} else if(BranchUtil.TypesVersion.PRODUCTION.toString().equals(semVerType)) {
			return ultimaTag.split(RC)[0]
		}

		return incrementarVersao(ultimaTag, semVerType)
	}

	@NonCPS
	private String incrementarVersao(String ultimaVersao, String semVerType) {
		Version version = Version.valueOf(ultimaVersao);
		if(BranchUtil.TypesVersion.MAJOR.toString().equals(semVerType)) {
			return version.incrementMajorVersion()
		} else if(BranchUtil.TypesVersion.MINOR.toString().equals(semVerType)) {
			return version.incrementMinorVersion()
		} else if(BranchUtil.TypesVersion.PATCH.toString().equals(semVerType)) {
			return version.incrementPatchVersion()
		}
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
				steps.sh "mvn -f ${pathArtefato} versions:set -DgenerateBackupPoms=false -DnewVersion=${nextVersion} -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true"
			}
		} else if("PHP" == linguagem) {
			steps.sh "awk -F\"=\" -v OFS=\'=\' \'/APP_VERSION/{\$2=\"${nextVersion}\";print;next}1\' ${pathArtefato} > ${pathArtefato}.new"
			steps.sh "mv ${pathArtefato}.new ${pathArtefato}"
		} else if("NODE" == linguagem) {
			steps.nodejs('NodeJS - 10.x') {
				steps.sh 'npm --prefix ' + pathArtefato +' version ' +nextVersion + ' --force'
			}
		}
		return steps
	}
	
	public static void main(String[] args) {
		def local = new GitFlow()
		def next = local.getNextVersion(560, "MINOR", true)
		println next
	}
}
