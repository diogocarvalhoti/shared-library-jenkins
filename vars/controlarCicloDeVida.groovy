#!groovy
import br.gov.mds.BranchUtil
import br.gov.mds.GitFlow

import java.util.regex.Matcher
import java.util.regex.Pattern

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
        stage('Checkout código fonte') {
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
                    '3eaff500-4fdb-46ac-9abb-7a1fbbd88f5f'
            ]) {
                sh 'git config --global http.sslVerify false'
                sh 'git checkout develop'
                sh 'git flow init -d'
                sh 'git flow feature start redmine-' + NUMERO_REDMINE + ' develop'
                sh 'git flow feature publish redmine-' + NUMERO_REDMINE
            }
        } else {
            def gitflow = new GitFlow()
            Integer idProject = gitflow.getIdProject(namespace)

            def FEATURE_NAME = input message: 'Escolha a feature:',
                    parameters: [
                            choice(choices: gitflow.getBranchesPorTipo(idProject, BranchUtil.Types.FEATURE.toString()),
                                    description: '', name: 'feature')
                    ]

            gitflow.createMR(idProject, FEATURE_NAME)
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

    println 'RELEASE_TYPEE: '+RELEASE_TYPE
    if (!BranchUtil.ReleaseTypes.PRODUCTION.toString().equals(RELEASE_TYPE)) {
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

        if (BranchUtil.ReleaseTypes.PRODUCTION.toString().equals(RELEASE_TYPE)) {
            sh 'git flow release finish ' + nextVersion + ' -p -m \"Fechando versão \"'
        } else {
            sh 'git flow release finish ' + nextVersion + ' --pushdevelop --pushtag -m \"Fechando versão \"'
        }

        sh 'unset GIT_MERGE_AUTOEDIT'
        sh 'git push'
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
                '3eaff500-4fdb-46ac-9abb-7a1fbbd88f5f'
        ]) {
            sh 'git config --global http.sslVerify false'
            sh 'git checkout master'
            sh 'git flow init -d'
            sh 'git flow hotfix start ' + nextVersion
            sh 'git flow hotfix publish ' + nextVersion

            sh 'git checkout -b hotfix/' + nextVersion + '-fabrica'
            sh 'git push --set-upstream origin hotfix/' + nextVersion + '-fabrica'
        }
    } else if (BranchUtil.Actions.FINISH.toString().equals(ACAO)) {
        String hotfixName = gitflow.getBranchesPorTipo(idProject, BranchUtil.Types.HOTFIX.toString()).get(0);
        String version = hotfixName.replace("hotfix/", "")

        sshagent([
                '3eaff500-4fdb-46ac-9abb-7a1fbbd88f5f'
        ]) {
            sh 'git config --global http.sslVerify false'
            sh 'git checkout ' + hotfixName

            gitflow.versionarArtefato(this, args.linguagem, args.pathArtefato, version)
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
            sh 'git push origin --delete ' + hotfixName + '-fabrica'
            sh 'git push origin ' + version
        }
    } else {
        String hotfixName = gitflow.getBranchesPorTipo(idProject, BranchUtil.Types.HOTFIX.toString()).get(0);
        gitflow.createMR(idProject, hotfixName + '-fabrica', hotfixName)
    }
}
