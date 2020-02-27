package br.gov.mds.pipeline

import com.cloudbees.groovy.cps.NonCPS

class VersionarArtefatoDTO implements Serializable {

    private String versao;
    private String linguagemBackend;
    private String linguagemFrontend;
    private String pathArtefatoBackend;
    private String pathArtefatoFrontend;

    @NonCPS
    String getVersao() {
        return versao
    }

    @NonCPS
    void setVersao(String versao) {
        this.versao = versao
    }

    @NonCPS
    String getLinguagemBackend() {
        return linguagemBackend
    }

    @NonCPS
    void setLinguagemBackend(String linguagemBackend) {
        this.linguagemBackend = linguagemBackend
    }

    @NonCPS
    String getLinguagemFrontend() {
        return linguagemFrontend
    }

    @NonCPS
    void setLinguagemFrontend(String linguagemFrontend) {
        this.linguagemFrontend = linguagemFrontend
    }

    @NonCPS
    String getPathArtefatoBackend() {
        return pathArtefatoBackend
    }

    @NonCPS
    void setPathArtefatoBackend(String pathArtefatoBackend) {
        this.pathArtefatoBackend = pathArtefatoBackend
    }

    @NonCPS
    String getPathArtefatoFrontend() {
        return pathArtefatoFrontend
    }

    @NonCPS
    void setPathArtefatoFrontend(String pathArtefatoFrontend) {
        this.pathArtefatoFrontend = pathArtefatoFrontend
    }
}
