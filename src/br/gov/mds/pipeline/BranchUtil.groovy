package br.gov.mds.pipeline

class BranchUtil {

    enum Types {
        FEATURE, HOTFIX, RELEASE
    }

    enum Actions {
        START, FINISH
    }

    enum VersionTypes {
        MAJOR, MINOR, PATCH
    }

    enum ReleaseTypes {
        PRODUCTION, CANDIDATE, INCREMENT_CANDIDATE
    }

}
