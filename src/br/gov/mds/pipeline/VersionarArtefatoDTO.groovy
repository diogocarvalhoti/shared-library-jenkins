package br.gov.mds.pipeline

import com.cloudbees.groovy.cps.NonCPS

class VersionarArtefatoDTO implements Serializable {

    private String versao;
    private String pathArtefatoBackend;
    private String pathArtefatoFrontend;

    VersionarArtefatoDTO(String versao, String pathArtefatoBackend, String pathArtefatoFrontend) {
        this.versao = versao
        this.pathArtefatoBackend = pathArtefatoBackend
        this.pathArtefatoFrontend = pathArtefatoFrontend
    }

    @NonCPS
    String getVersao() {
        return versao
    }

    @NonCPS
    String getPathArtefatoBackend() {
        return pathArtefatoBackend
    }

    @NonCPS
    String getPathArtefatoFrontend() {
        return pathArtefatoFrontend
    }
}
