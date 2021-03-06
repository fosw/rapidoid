package org.rapidoid.plugin.deploy;

/*
 * #%L
 * Rapidoid Build Plugin
 * %%
 * Copyright (C) 2014 - 2016 Nikolche Mihajlovski and contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.*;
import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.http.HttpReq;
import org.rapidoid.http.HttpResp;
import org.rapidoid.io.IO;
import org.rapidoid.u.U;
import org.rapidoid.util.Msc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Authors("Nikolche Mihajlovski")
@Since("5.3.0")
public abstract class AbstractRapidoidMojo extends AbstractMojo {

	protected static final String ABORT = "Aborting the build!";

	protected void failIf(boolean failureCondition, String msg, Object... args) throws MojoExecutionException {
		if (failureCondition) {
			throw new MojoExecutionException(U.frmt(msg, args));
		}
	}

	protected boolean request(HttpReq req) {
		HttpResp resp = req.execute();

		switch (resp.code()) {
			case 200:
				return true;

			case 404:
				getLog().error(U.frmt("Couldn't find: %s", req.url()));
				return false;

			default:
				String msg = "Unexpected response received from: %s! Response code: %s, full response:\n\n%s\n";
				getLog().error(U.frmt(msg, req.url(), resp.code(), resp.body()));
				return false;
		}
	}

	protected String createTempFile(String prefix, String suffix, FileAttribute<?>... attrs) throws MojoExecutionException {
		String assemblyFile;

		try {
			assemblyFile = Files.createTempFile(prefix, suffix, attrs).toAbsolutePath().toString();

		} catch (IOException e) {
			throw new MojoExecutionException("Couldn't create temporary file! " + ABORT, e);
		}

		return assemblyFile;
	}

	protected void invoke(MavenSession session, List<String> goals, boolean updateSnapshots, Map<String, String> properties) throws MojoExecutionException {

		InvocationRequest request = new DefaultInvocationRequest();

		request.setPomFile(session.getRequest().getPom());
		request.setGoals(goals);
		request.setAlsoMake(true);
		request.setUpdateSnapshots(updateSnapshots);

		Properties props = new Properties();
		props.putAll(properties);
		request.setProperties(props);

		Invoker invoker = new DefaultInvoker();

		boolean success;

		try {
			InvocationResult result = invoker.execute(request);
			success = result.getExitCode() == 0 && result.getExecutionException() == null;

		} catch (MavenInvocationException e) {
			throw new MojoExecutionException("Invocation error! " + ABORT, e);
		}

		failIf(!success, "An error occurred. " + ABORT);
	}

	protected String buildUberJar(MavenProject project, MavenSession session) throws MojoExecutionException {

		List<String> goals = U.list("package", "org.apache.maven.plugins:maven-assembly-plugin:2.6:single");

		String assemblyFile = createTempFile("app-assembly-", ".xml");

		IO.save(assemblyFile, IO.load("uber-jar.xml"));

		Map<String, String> properties = U.map();
		properties.put("skipTests", "true");
		properties.put("descriptor", assemblyFile);
		properties.put("assembly.appendAssemblyId", "true");
		properties.put("assembly.attach", "false");

		invoke(session, goals, false, properties);

		boolean deleted = new File(assemblyFile).delete();
		if (!deleted) getLog().warn("Couldn't delete the temporary assembly descriptor file!");

		List<String> appJars = IO.find("*-uber-jar.jar").in(project.getBuild().getDirectory()).getNames();

		failIf(appJars.size() != 1, "Cannot find the deployment JAR (found %s candidates)! " + ABORT, appJars.size());

		String uberJar = U.first(appJars);

		try {
			Path uberJarPath = Paths.get(uberJar);
			Path appJar = uberJarPath.getParent().resolve("app.jar");
			Files.move(uberJarPath, appJar);
			uberJar = appJar.toFile().getAbsolutePath();
		} catch (IOException e) {
			throw new MojoExecutionException("Couldn't rename the file! " + ABORT, e);
		}

		String size = Msc.fileSizeReadable(uberJar);

		getLog().info("");
		getLog().info("Successfully packaged the application with dependencies:");
		getLog().info(U.frmt("%s (size: %s).", uberJar, size));
		getLog().info("");

		return uberJar;
	}

}
