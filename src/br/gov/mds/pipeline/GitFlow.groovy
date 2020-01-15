package br.gov.mds.pipeline

import com.cloudbees.groovy.cps.NonCPS

@Grab('com.github.zafarkhaja:java-semver:0.9.0')
import com.github.zafarkhaja.semver.Version

class GitFlow implements Serializable {

    private static final String BASE_URL = "http://sugitpd02.mds.net"

    //TOKEN GERADO PARA O USUARIO gcm_cgsi
    private static final String PRIVATE_TOKEN = "u3xBWdP3KUxxG7PQYm_t"
    private static final String RC = "-rc"

    @NonCPS
    Integer getIdProject(String namespace) {
        String uri = new StringBuilder(this.BASE_URL)
                .append("/api/v4/projects/").append(namespace).toString()
        Map resultMap = (Map) GitRestClient.get(uri, this.PRIVATE_TOKEN)
        return resultMap.get("id")
    }

    @NonCPS
    String createMR(Integer idProject, String sourceBranch, String targetBranch = "develop") {
        Map<String, String> params = new HashMap();
        params.put("remove_source_branch", "true");
        params.put("source_branch", sourceBranch);
        params.put("target_branch", targetBranch);
        params.put("title", "Merge Request da branch: " + sourceBranch);

        String uri = new StringBuilder(this.BASE_URL)
                .append("/api/v4/projects/").append(idProject).append("/merge_requests").toString();

        return GitRestClient.post(uri, this.PRIVATE_TOKEN, params)
    }

    @NonCPS
    List<String> getBranchesPorTipo(Integer idProject, String branchType) {
        String uri = new StringBuilder(this.BASE_URL)
                .append("/api/v4/projects/").append(idProject).append("/repository/branches").toString()
        List branches = GitRestClient.get(uri, this.PRIVATE_TOKEN)

        List<String> features = new ArrayList();

        for (branch in branches) {
            String nomeBranch = branch.getAt("name");
            if (nomeBranch.toUpperCase().startsWith(branchType.toString()) &&
                    !nomeBranch.toUpperCase().endsWith("fabrica")) {
                features.add(nomeBranch);
            }
        }
        return features;
    }

    @NonCPS
    String getNextVersion(Integer idProject, String semVerType, String releaseType) {
        def ultimaTag = this.getUltimaTagPorTipo(idProject)

        if (BranchUtil.ReleaseTypes.PRODUCTION.toString().equals(releaseType)) {
            return ultimaTag.split(RC)[0]
        } else {
            if (BranchUtil.ReleaseTypes.INCREMENT_CANDIDATE.toString().equals(releaseType)) {
                Version version = Version.valueOf(ultimaTag);
                return version.incrementPreReleaseVersion();
            } else {
                String nextVersion = incrementarVersao(ultimaTag, semVerType);
                Version version = Version.valueOf(nextVersion.concat(RC));
                return version.incrementPreReleaseVersion();
            }
        }
    }

    @NonCPS
    String incrementarVersao(String ultimaVersao, String semVerType) {
        Version version = Version.valueOf(ultimaVersao);
        if (BranchUtil.VersionTypes.MAJOR.toString().equals(semVerType)) {
            return version.incrementMajorVersion()
        } else if (BranchUtil.VersionTypes.MINOR.toString().equals(semVerType)) {
            return version.incrementMinorVersion()
        } else if (BranchUtil.VersionTypes.PATCH.toString().equals(semVerType)) {
            return version.incrementPatchVersion()
        }
    }

    @NonCPS
    String getUltimaTagPorTipo(Integer idProject, String releaseType = null) {
        String uri = new StringBuilder(this.BASE_URL)
                .append("/api/v4/projects/").append(idProject)
                .append("/repository/tags").append("?order_by=name").toString();

        Map<String, String> params = new HashMap()
        params.put("order_by", "name")

        List tags = GitRestClient.get(uri, this.PRIVATE_TOKEN)

        if (!tags.empty) {
            for (Map tag : tags) {
                String name = tag.get("name")
                if (BranchUtil.ReleaseTypes.PRODUCTION.toString().equals(releaseType)) {
                    if (name.contains(RC)) {
                        continue
                    } else {
                        return name
                    }
                } else if (BranchUtil.ReleaseTypes.CANDIDATE.toString().equals(releaseType)) {
                    if (name.contains(RC)) {
                        return name
                    } else {
                        continue
                    }
                } else {
                    return name
                }
            }
        }
        return "0.0.0"
    }

    def versionarArtefato(steps, linguagem, pathArtefato, nextVersion) {
        if ("JAVA" == linguagem) {
            steps.withMaven(maven: 'Maven 3.6.2') {
                steps.sh "mvn -f ${pathArtefato} versions:set -DgenerateBackupPoms=false -DnewVersion=${nextVersion} -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true"
            }
        } else if ("PHP" == linguagem) {
            steps.sh "awk -F\"=\" -v OFS=\'=\' \'/APP_VERSION/{\$2=\"${nextVersion}\";print;next}1\' ${pathArtefato} > ${pathArtefato}.new"
            steps.sh "mv ${pathArtefato}.new ${pathArtefato}"
        } else if ("NODE" == linguagem) {
            steps.nodejs('NodeJS - 10.x') {
                steps.sh 'npm --prefix ' + pathArtefato + ' version ' + nextVersion + ' --force'
            }
        }
        return steps
    }

//	static void main(String[] args) {
//		def local = new GitFlow()
//		def next = local.getNextVersion(560, "MINOR", true)
//		println next
//	}
}
