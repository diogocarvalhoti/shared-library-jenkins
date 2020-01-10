#!groovy
import br.gov.mds.GitFlow

import java.util.regex.Matcher
import java.util.regex.Pattern

import br.gov.mds.BranchUtil

def validarParametros(args) {
    if (!args.gitRepositorySSH) {
        println "O parâmetro gitRepositorySSH é obrigatório"
        return
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

def call(args) {

    validarParametros(args)

    def namespace = recuperarNamespace(args.gitRepositorySSH)

    args.linguagem = args.linguagem ?: 'JAVA'
    args.pathArtefato = args.pathArtefato ?: './pom.xml'

    node {
        stage('Checkout') {
            cleanWs()
            checkout([$class                           : 'GitSCM', branches: [[name: '*/develop']],
                      doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [
                    [
                            credentialsId: '3eaff500-4fdb-46ac-9abb-7a1fbbd88f5f',
                            refspec      : '+refs/heads/*:refs/remotes/origin/* +refs/merge-requests/*/head:refs/remotes/origin/merge-requests/*',
                            url          : args.gitRepositorySSH]
            ]]
            )

            sh 'git config --global user.email \"gcm_cgsi@cidadania.gov.br\"'
            sh 'git config --global user.name \"Gerência de Configuração e Mudança\"'
        }

        stage('Escolha o tipo de Branch \n FEATURE/HOTFIX/RELEASE') {
            timeout(5) {
                TIPO = input message: 'Escolha o tipo de branch:',
                        parameters: [
                                choice(choices: BranchUtil.Types.values().toList(),
                                        description: '', name: 'tipo')
                        ]
            }
        }

        stage('Executando ação') {
            def RELEASE_CANDIDATE
            def TYPE_VERSION

            if (TIPO == "RELEASE") {
                stage('Escolha o tipo de versionamento') {
                    timeout(5) {

                        RELEASE_CANDIDATE = input message: 'Release Candidate:',
                                parameters: [
                                        choice(choices: BranchUtil.ReleaseTypes.values().toList(),
                                                description: 'Escolha a opção de Release candidate, caso não se aplique, selecione \"NA\"', name: 'release_candidate')
                                ]

                        if (RELEASE_CANDIDATE != "PRODUCTION") {
                            TYPE_VERSION = input message: 'Escolha o tipo de versionamento:',
                                    parameters: [
                                            choice(choices: BranchUtil.VersionTypes.values().toList(),
                                                    description: '', name: 'typeVersion')
                                    ]
                        }
                        def gitflow = new GitFlow()
                        Integer idProject = gitflow.getIdProject(namespace)
                        def nextVersion = gitflow.getNextVersion(idProject, TYPE_VERSION, RELEASE_CANDIDATE)

                        sshagent([
                                '3eaff500-4fdb-46ac-9abb-7a1fbbd88f5f'
                        ]) {
                            sh 'git config --global http.sslVerify false'
                            sh 'git checkout develop'
                            sh 'git flow init -d'
                            sh 'git flow release start ' + nextVersion

                            gitflow.versionarArtefato(this, args.linguagem, args.pathArtefato, nextVersion)

                            sh 'export GIT_MERGE_AUTOEDIT=no'
                            sh 'git add .'
                            sh 'git commit -m \"Versionando aplicação para a versão ' + nextVersion + '\"'
                            sh 'git flow release finish -k ' + nextVersion + ' -p -m \"Fechando versão \"'
                            sh 'unset GIT_MERGE_AUTOEDIT'

                            sh 'git branch -a'

                            if (RELEASE_CANDIDATE == "PRODUCTION") {
                                sh 'git checkout stable'
                                sh 'git merge ' + nextVersion
                            }
                            sh 'git branch -D release/' + nextVersion
                            sh 'git push'
                        }
                    }
                }
            } else if (TIPO == "FEATURE" || TIPO == "HOTFIX") {
                timeout(5) {
                    ACAO = input message: 'Escolha a ação:',
                            parameters: [
                                    choice(choices: BranchUtil.Actions.values().toList(),
                                            description: '', name: 'acao')
                            ]
                    if (TIPO == "FEATURE") {
                        if (ACAO == "START") {
                            FEATURE_NAME = input(
                                    id: 'userInput', message: 'Nome da feature',
                                    parameters: [
                                            string(
                                                    description: 'Nome da feature',
                                                    name: 'Nome da feature'
                                            )
                                    ])
                            sshagent([
                                    '3eaff500-4fdb-46ac-9abb-7a1fbbd88f5f'
                            ]) {
                                sh 'git config --global http.sslVerify false'
                                sh 'git checkout develop'
                                sh 'git flow init -d'
                                sh 'git flow feature start ' + FEATURE_NAME + ' develop'
                                sh 'git flow feature publish ' + FEATURE_NAME
                            }
                        } else {
                            def gitflow = new GitFlow()
                            Integer idProject = gitflow.getIdProject(namespace)

                            def FEATURE_NAME = input message: 'Escolha a feature:',
                                    parameters: [
                                            choice(choices: gitflow.getFeatures(idProject),
                                                    description: '', name: 'feature')
                                    ]

                            gitflow.createMR(idProject, FEATURE_NAME)
                        }
                    }
                }

            }

        }
    }
}

