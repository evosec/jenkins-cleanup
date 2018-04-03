package de.evosec.jenkinscleanup;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jenkins-cleanup")
public class JenkinsCleanupProperties {

	private String directory = "/tmp";

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

}
