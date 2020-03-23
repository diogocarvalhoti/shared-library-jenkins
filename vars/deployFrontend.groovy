#!groovy
def call(args) {

    args.gitBranchParam = args.gitBranchParam ? args.gitBranchParam.toBoolean() : false
    args.dockerBuildImage = args.dockerBuildImage ?: 'basisti/build-frontend-npm:node-8.9.3-u1000'
    args.dockerBuildScriptPath = args.dockerBuildScriptPath ?: 'docker/build-frontend.sh'
    args.dockerFile = args.dockerFile ?: 'Dockerfile'
    args.dockerRegistry = args.dockerRegistry ?: 'docker-registry.default.svc:5000'
    args.gitBranch = args.gitBranch ?: 'master'
    args.dockerImageTag = args.dockerImageTag ?: 'latest'
    args.mvnCompileWorkDir = args.mvnCompileWorkDir ?: '.'
    args.dockerBuildCompressed = args.dockerBuildCompressed ? args.dockerBuildCompressed.toBoolean() : true
    args.removerWorkspace = args.removerWorkspace ? args.removerWorkspace.toBoolean() : false

    def label = "front-build-${UUID.randomUUID().toString()}"

    podTemplate(label: label, serviceAccount: 'jenkins', yaml: """
kind: Pod
metadata:
  name: kaniko
spec:
  containers:
  - name: kaniko
    image: gcr.io/kaniko-project/executor:debug-v0.15.0
    imagePullPolicy: Always
    command:
    - /busybox/cat
    tty: true
    volumeMounts:
      - name: jenkins-docker-cfg
        mountPath: /kaniko/.docker
  - name: front-build
    image: ${args.dockerBuildImage}
    imagePullPolicy: Always
    command:
    - cat
    tty: true
    volumeMounts:
      - name: jenkins-mvnrepos
        mountPath: /root/.m2/repository
      - name: maven-settings
        mountPath: /root/.m2/
  - name: kubectl
    image: lachlanevenson/k8s-kubectl:v1.14.2
    imagePullPolicy: Always
    command:
    - cat
    tty: true
  volumes:
  - name: jenkins-docker-cfg
    projected:
      sources:
      - secret:
          name: regcred
          items:
            - key: .dockerconfigjson
              path: config.json
  - name: jenkins-mvnrepos
    persistentVolumeClaim:
      claimName: jenkins-mvnrepos-pvc
  - name: maven-settings
    configMap:
      name: maven-settings
    """
    )

            {

                node(label) {

                    if(args.gitRepositoryUrl == null || args.gitRepositoryUrl == ""){
                        error("""===========\nNão foi informado repositório da construção! Parâmetro: gitRepositoryUrl\n===========""")
                    }

                    if(args.gitBranchParam){
                        stage ('Selecionar Branch') {
                            branches = sh (
                                    script: "git ls-remote --heads --tags " + args.gitRepositoryUrl + " | sed 's:.*refs/tags/\\|.*refs/heads/::'",
                                    returnStdout: true
                            ).trim()
                            try {
                                timeout(time: 30, unit: 'SECONDS') {
                                    args.gitBranch = input(
                                            id: 'gitBranch', message: 'Favor selecionar a branch', parameters: [
                                            [$class: 'hudson.model.ChoiceParameterDefinition', choices: branches, description: 'Git branch', name: 'GIT_BRANCH']
                                    ])
                                }
                            } catch(err) {
                                def user = err.getCauses()[0].getUser()
                                currentBuild.result = 'FAILURE'
                                if('SYSTEM' == user.toString()) { // SYSTEM = timeout.
                                    error("""===========\nBuild abortada por timeout aguardando entrada. Favor selecionar a branch no tempo concedido!\n===========""")
                                } else {
                                    error("Abortada por: [${user}]")
                                }
                            }
                        }
                    }

                    stage('Git Pull') {
                        git([
                                poll: true,
                                credentialsId: 'gitlab-login',
                                url: args.gitRepositoryUrl,
                                branch: args.gitBranch
                        ])
                    }

                    stage('Construção do Código-fonte') {
                        container('front-build') {
                            dir(args.mvnCompileWorkDir){
                                sh(args.dockerBuildScriptPath)
                                if(args.dockerBuildCompressed){
                                    sh("find $args.dockerWorkDir -maxdepth 1 -name *.tar.gz | tee findResult")
                                    String findResult = readFile('findResult')
                                    sh('rm -f findResult')
                                    if(!findResult?.trim()) {
                                        error('Ocorreu alguma falha na execução do build. O pacote tar.gz não foi criado.')
                                    }
                                }
                            }
                        }
                    }

                    if(args.dockerRegistry != null && args.dockerRegistry != "" && args.dockerImageName != null && args.dockerImageName != ""){
                        stage('Construção Docker Kaniko') {
                            container(name: 'kaniko', shell: '/busybox/sh') {
                                withEnv(['PATH+EXTRA=/busybox:/kaniko']) {
                                    sh """#!/busybox/sh
                     /kaniko/executor -f `pwd`/${args.dockerWorkDir}/${args.dockerFile} -c `pwd`/${args.dockerWorkDir} --skip-tls-verify --destination=${args.dockerRegistry}/${args.dockerImageName}:${args.dockerImageTag}
                  """
                                }
                            }
                        }
                    }
                    else{
                        echo """===========\nNão foi informada a registry da imagem (parâmetro dockerRegistry), ou o nome da mesma (parâmetro dockerImageName). Imagem não será construída!\n==========="""
                    }

                    if(args.kubeWorkload != null && args.kubeWorkload != "" && args.kubeNamespace != null && args.kubeNamespace != ""){
                        stage('Atualização Kubernetes'){
                            container('kubectl'){
                                sh(script: "kubectl delete pod --wait=false -n $args.kubeNamespace \$(kubectl get pods -n $args.kubeNamespace | grep $args.kubeWorkload | awk -F\" \" '{print \$1}')")
                            }
                        }
                    }
                    else{
                        echo """===========\nNão foi informado o workload do kubernetes a ser usado (parâmetro kubeWorkload), ou o namespace do mesmo (parâmetro kubeNamespace). Deploy não será realizado!\n==========="""
                    }
                    if(args.removerWorkspace){
                        stage('Limpeza do Workspace') {
                            cleanWs()
                        }
                    }
                }
            }
}
