package br.gov.mds;

public class Logger {

	// Standard output
	private static out = System.out

	public static setOutput(out) {
		this.out = out
	}


	public static log(String message) {
		Logger.out.println(message)
	}
}
