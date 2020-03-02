#!groovy
import br.gov.mds.pipeline.BranchUtil
import br.gov.mds.pipeline.GitFlow

import java.util.regex.Matcher
import java.util.regex.Pattern

def call(args) {

    validarParametros(args)

    GITLAB_LOGIN_SSH = '3eaff500-4fdb-46ac-9abb-7a1fbbd88f5f'
    BUILD_NUMBER = "${BUILD_NUMBER}"
//    def GITLAB_LOGIN_SSH = 'gitlab-login-ssh'

    def label = "release-${UUID.randomUUID().toString()}"

//    podTemplate(label: label, serviceAccount: 'jenkins') {


    def namespace = recuperarNamespace(args.gitRepositorySSH)

//    node(label) {
    node() {
        stage('Checkout código fonte') {
            cleanWs()
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

        stage('Escolha o tipo de Branch \n FEATURE/HOTFIX/RELEASE') {
            TIPO = input message: 'Escolha o tipo de branch:',
                    parameters: [
                            choice(choices: BranchUtil.Types.values().toList(),
                                    description: '', name: 'tipo')
                    ]
        }

        stage('Aplicando fluxo') {
            if (BranchUtil.Types.RELEASE.toString().equals(TIPO)) {
                flowRelease(namespace, args)
            } else if (BranchUtil.Types.FEATURE.toString().equals(TIPO)) {
                flowFeature(namespace)
            } else {
                flowHotfix(namespace, args)
            }
        }
    }
//    }
}

def validarParametros(args) {
    if (!args.gitRepositorySSH) {
        throw new Exception("O parâmetro \'args.gitRepositorySSH\' é obrigatório");
    }

    if (!args.pathArtefatoBackend && !args.pathArtefatoFrontend) {
        throw new Exception("Ao menos um dos parâmetros \'args.pathArtefatoBackend\' ou \'args.pathArtefatoFrontend\' é obrigatório");
    }
}

def recuperarNamespace(repository) {
    final String regex = "\\:(.*?).git";

    final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
    final Matcher matcher = pattern.matcher(repository);

    while (matcher.find()) {
        return matcher.group(1).replace("/", "%2F");
    }
    return ""
}

void flowFeature(namespace) {
    ACAO = input message: 'Escolha a ação:',
            parameters: [
                    choice(choices: BranchUtil.Actions.values().toList(),
                            description: '', name: 'acao')
            ]
    if (BranchUtil.Types.FEATURE.toString().equals(TIPO)) {
        if (BranchUtil.Actions.START.toString().equals(ACAO)) {
            NUMERO_REDMINE = input(
                    id: 'userInput', message: 'Informe o número do Redmine',
                    parameters: [
                            string(
                                    description: '',
                                    name: 'Número do Redmine'
                            )
                    ])
            sshagent([
                    GITLAB_LOGIN_SSH
            ]) {
                sh 'git config --global http.sslVerify false'
                sh 'git checkout develop'
                sh 'git checkout -b feature/redmine-' + NUMERO_REDMINE
                sh 'git push origin feature/redmine-' + NUMERO_REDMINE
            }
        } else {
            def gitflow = new GitFlow()
            Integer idProject = gitflow.getIdProject(namespace)

            def FEATURE_NAME = input message: 'Escolha a feature:',
                    parameters: [
                            choice(choices: gitflow.getBranchesPorTipo(idProject, BranchUtil.Types.FEATURE.toString()),
                                    description: '', name: 'feature')
                    ]

            gitflow.createMR(idProject, BUILD_NUMBER, FEATURE_NAME)
        }
    }
}

void flowRelease(namespace, args) {
    String TYPE_VERSION
    String RELEASE_TYPE = input message: 'Escolha o tipo de RELEASE:',
            parameters: [
                    choice(choices: BranchUtil.ReleaseTypes.values().toList(),
                            description: 'Escolha a opção \"CANDIDATE\" para uma nova versão para o ambiente de homologação e \"INCREMENTE_CANDIDATE\" para incrementar uma release candidate aberta',
                            name: 'releaseType')
            ]

    if (BranchUtil.ReleaseTypes.CANDIDATE.toString().equals(RELEASE_TYPE)) {
        TYPE_VERSION = input message: 'Escolha o tipo de versionamento:',
                parameters: [
                        choice(choices: BranchUtil.VersionTypes.values().toList(),
                                description: '', name: 'typeVersion')
                ]
    }
    def gitflow = new GitFlow()
    Integer idProject = gitflow.getIdProject(namespace)
    def nextVersion = gitflow.getNextVersion(idProject, TYPE_VERSION, RELEASE_TYPE)

    sshagent([
            GITLAB_LOGIN_SSH
    ]) {
        sh 'git config --global http.sslVerify false'
        sh 'git checkout develop'
        sh 'git checkout -b release/' + nextVersion

        versionarArtefatos(gitflow, args, nextVersion);

        sh 'export GIT_MERGE_AUTOEDIT=no'
        sh 'git add .'
        sh 'git commit -m \"Versionando aplicação para a versão ' + nextVersion + '\"'

        sh 'git checkout develop'
        sh 'git merge release/' + nextVersion

        if (BranchUtil.ReleaseTypes.PRODUCTION.toString().equals(RELEASE_TYPE)) {
            sh 'git checkout master'
            sh 'git merge release/' + nextVersion
        }

        sh 'unset GIT_MERGE_AUTOEDIT'
        sh 'git branch -D release/' + nextVersion
        sh 'git tag -a ' + nextVersion + ' -m \"Fechando versão ' + nextVersion + '\"'
        sh 'git push --all origin'
        sh 'git push --tags origin '
    }
}

void versionarArtefatos(GitFlow gitFlow, args, String nextVersion) {
    if (!args.pathArtefatoBackend) {
        gitFlow.versionarArtefato(this, args.pathArtefatoBackend, nextVersion)
    }
    if (!args.pathArtefatoFrontend) {
        gitFlow.versionarArtefato(this, args.pathArtefatoFrontend, nextVersion)
    }
}

void flowHotfix(namespace, args) {
    ACAO = input message: 'Escolha a ação:',
            parameters: [
                    choice(choices: ['START', 'FINISH_FABRICA', 'FINISH'],
                            description: '', name: 'acao')
            ]

    def gitflow = new GitFlow()
    Integer idProject = gitflow.getIdProject(namespace)

    if (BranchUtil.Actions.START.toString().equals(ACAO)) {
        String typeVersion = BranchUtil.VersionTypes.PATCH.toString()

        def ultimaTagProduction = gitflow.getUltimaTagPorTipo(idProject, BranchUtil.ReleaseTypes.PRODUCTION.toString())
        def nextVersion = gitflow.incrementarVersao(ultimaTagProduction, typeVersion)

        sshagent([
                GITLAB_LOGIN_SSH
        ]) {
            sh 'git config --global http.sslVerify false'
            sh 'git checkout master'
            sh 'git checkout -b hotfix/' + nextVersion
            sh 'git checkout -b hotfix/' + nextVersion + '-fabrica'
            sh 'git push --all'
        }
    } else if (BranchUtil.Actions.FINISH.toString().equals(ACAO)) {
        String hotfixName = gitflow.getBranchesPorTipo(idProject, BranchUtil.Types.HOTFIX.toString()).get(0);
        String version = hotfixName.replace("hotfix/", "")

        sshagent([
                GITLAB_LOGIN_SSH
        ]) {
            sh 'git config --global http.sslVerify false'
            sh 'git checkout ' + hotfixName

            versionarArtefatos(gitflow, args, version)

            sh 'git add .'
            sh 'git commit -m \"Versionando aplicação para a versão ' + version + '\"'
            sh 'git push'

            sh 'git tag -a ' + version + ' -m \"Fechando versão ' + version + ' (hotfix)\"'
            sh 'git checkout master'
            sh 'git merge -X theirs ' + hotfixName
            sh 'git checkout develop'
            sh 'git merge -X theirs ' + hotfixName
            sh 'git branch -D ' + hotfixName

            sh 'git push --all origin'
            sh 'git push origin --delete ' + hotfixName
            sh 'git push origin ' + version
        }
    } else {
        String hotfixName = gitflow.getBranchesPorTipo(idProject, BranchUtil.Types.HOTFIX.toString()).get(0);
        gitflow.createMR(idProject, BUILD_NUMBER, hotfixName + '-fabrica', hotfixName)
    }
}