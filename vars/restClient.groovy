#! /home/diogo/.sdkman/candidates/groovy/2.5.8/bin/groovy

import groovy.json.JsonSlurperClassic
import com.cloudbees.groovy.cps.NonCPS




def getIdProject(url, token) {
	return resultMap.get("id")
}

def createMR(url, token, sourceBranch, targetBranch) {
	
}

@NonCPS
def getResult(url, token) {
    HttpURLConnection connection = url.openConnection()
	
    if (token != null && token.length() > 0) {
        connection.setRequestProperty("Private-Token", "${token}")
    }
	
    connection.setRequestMethod("GET")
    connection.setDoInput(true)
	connection.setDoOutput(true)
    def rs = null
    try {
        connection.connect()
		def responseCode = connection.getResponseCode()
		println "Response Code: "+responseCode
        rs = new JsonSlurperClassic().parse(new InputStreamReader(connection.getInputStream(), "UTF-8"))
    } finally {
        connection.disconnect()
    }
    return rs
}