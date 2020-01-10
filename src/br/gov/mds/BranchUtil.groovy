package br.gov.mds

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
        PRODUCTION, NEW_CANDIDATE, INCREMENT_CANDIDATE
    }

}
