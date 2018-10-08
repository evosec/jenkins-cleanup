package de.evosec.jenkinscleanup;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JenkinsCleanupProperties.class)
public class JenkinsCleanupApplication implements ApplicationRunner {

	private static final Logger LOG =
	        LoggerFactory.getLogger(JenkinsCleanupApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(JenkinsCleanupApplication.class, args);
	}

	@Autowired
	private JenkinsCleanupProperties properties;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		Path directory = Paths.get(properties.getDirectory());
		if (!directory.isAbsolute()) {
			directory = Paths.get(System.getProperty("user.dir"))
			    .resolve(directory)
			    .toAbsolutePath();
		}
		if (Files.notExists(directory)) {
			LOG.warn("Directory {} does not exist. Exiting.", directory);
			return;
		}
		FileTime sixHoursAgo =
		        FileTime.from(Instant.now().minus(6, ChronoUnit.HOURS));

		List<String> filesStartingWith = Arrays.asList("cargo-jvm-version-",
		    "winstone", "gwt", "cargo-jvm-version", "sonar-scanner-api-batch",
		    "jffi", "+~JF", "tmp", "evoccs", "dse_blanko_d2eentwi_", "imageio",
		    "hudson", "*.jpg", "desugar_deploy.jar", "haste-map-",
		    "stockimport", "junit", "maven-build");

		List<String> filesEndingWith = Arrays.asList(".jpg");

		List<String> directoriesStartingWith = Arrays.asList("jenkins-remoting",
		    "tomcat", "jetty-", "jbd_tmp_", "jbd_classpath_", "npm-",
		    "ansible_", "ansible-tmp-", "metro-cache-", "metro-bundler-cache-",
		    "resource-");
		List<String> directoriesMatching = Arrays.asList("[a-f0-9-]{0,63}");

		for (Path path : Files.newDirectoryStream(directory)) {
			if (lastModifiedBefore(path, sixHoursAgo)) {
				if (Files.isRegularFile(path)) {
					String fileName = Optional.ofNullable(path.getFileName())
					    .map(Path::toString)
					    .orElse("");
					if (filesStartingWith.stream()
					    .anyMatch(fileName::startsWith)) {
						delete(path);
					}
					if (filesEndingWith.stream().anyMatch(fileName::endsWith)) {
						delete(path);
					}
				} else if (Files.isDirectory(path)) {
					String directoryName =
					        Optional.ofNullable(path.getFileName())
					            .map(Path::toString)
					            .orElse("");
					if (directoriesStartingWith.stream()
					    .anyMatch(directoryName::startsWith)
					        || directoriesMatching.stream()
					            .anyMatch(directoryName::matches)) {
						delete(path);
					}
				}
			}
		}
	}

	private boolean lastModifiedBefore(Path f, FileTime time) {
		try {
			return Files.getLastModifiedTime(f).compareTo(time) <= 0;
		} catch (IOException e) {
			return false;
		}
	}

	private void delete(Path path) {
		LOG.info("Deleting {}", path);
		if (path == null || !Files.exists(path)) {
			return;
		}
		try {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(Path file,
				        BasicFileAttributes attrs) throws IOException {
					Files.deleteIfExists(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir,
				        IOException exc) throws IOException {
					Files.deleteIfExists(dir);
					return FileVisitResult.CONTINUE;
				}

			});
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
