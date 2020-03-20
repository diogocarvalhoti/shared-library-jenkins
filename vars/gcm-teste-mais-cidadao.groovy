#!groovy
import br.gov.mds.pipeline.BranchUtil
import br.gov.mds.pipeline.GitFlow

import java.util.regex.Matcher
import java.util.regex.Pattern

def call(args) {
//    GITLAB_LOGIN_SSH = 'dbbbd793-ea4d-46de-baa0-f807699be020'
    GITLAB_LOGIN_SSH = 'gitlab-login-ssh'
    BUILD_NUMBER = "${BUILD_NUMBER}"
    // def label = "teste-gerar-imagem-${UUID.randomUUID().toString()}"
    // podTemplate(label: label, serviceAccount: 'jenkins') {
    node() {
        stage('Limpando a pasta de trabalho') {
            cleanWs()
        }
        stage('Baixando código fonte') {
            checkout([$class                           : 'GitSCM', branches: [[name: '*/develop']],
                      doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [
                    [
                            credentialsId: GITLAB_LOGIN_SSH,
                            refspec      : '+refs/heads/*:refs/remotes/origin/* +refs/merge-requests/*/head:refs/remotes/origin/merge-requests/*',
                            url          : args.gitRepositorySSH]
            ]]
            )

            sh 'git config --global user.email \"gcm_cgsi@cidadania.gov.br\"'
            sh 'git config --global user.name \"Gerência de Configuração e Mudança\"'
        }
        stage('Listar Tags') {
            namespace = recuperarNamespace(args.gitRepositorySSH)
            def gitflow = new GitFlow()
            Integer idProject = gitflow.getIdProject(namespace)
            List tags = gitflow.getTags(idProject)
//            List tags = new ArrayList()
//            tags.add('1.0.0')
//            tags.add('1.0.1')
            def TAG = input message: 'Escolha a tag', parameters: [choice(choices: tags, description: '', name: '')]
        }
    }
    // }
}

@NonCPS
def recuperarNamespace(repository) {
    final String regex = "\\:(.*?).git";
    final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
    final Matcher matcher = pattern.matcher(repository);
    while (matcher.find()) {
        return matcher.group(1).replace("/", "%2F");
    }
    return ""
}

call(
    gitRepositorySSH: 'git@sugitpd02.mds.net:gcm_cgsi/municipio-mais-cidadao.git'
)