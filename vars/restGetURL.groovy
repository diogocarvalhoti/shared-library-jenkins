#! /home/diogo/.sdkman/candidates/groovy/2.5.8/bin/groovy

import groovy.json.JsonSlurperClassic
import com.cloudbees.groovy.cps.NonCPS

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    if (config.url == null) {
        throw new RuntimeException("No URL found")
    }

    retry(3) {
        return getResult(config.url, config.token)
    }
}

def getIdProject(body) {
	Map resultMap = call(body)
	return resultMap.get("id")
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