package de.uhd.ifi.se.decision.management.jira.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.uhd.ifi.se.decision.management.jira.config.GitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.user.UserManager;
import com.google.common.collect.ImmutableMap;

import de.uhd.ifi.se.decision.management.jira.config.PluginInitializer;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.persistence.ConfigPersistence;

/**
 * REST resource for plug-in configuration
 */
@Path("/config")
@Scanned
public class ConfigRest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigRest.class);

	@ComponentImport
	private final UserManager userManager;

	@Inject
	public ConfigRest(UserManager userManager) {
		this.userManager = userManager;
	}

	@Path("/setActivated")
	@POST
	public Response setActivated(@Context HttpServletRequest request, @QueryParam("projectKey") String projectKey,
			@QueryParam("isActivated") String isActivatedString) {
		Response isValidDataResponse = checkIfDataIsValid(request, projectKey);
		if (isValidDataResponse.getStatus() != Status.OK.getStatusCode()) {
			return isValidDataResponse;
		}
		if (isActivatedString == null) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "isActivated = null")).build();
		}
		try {
			boolean isActivated = Boolean.valueOf(isActivatedString);
			ConfigPersistence.setActivated(projectKey, isActivated);
			setDefaultKnowledgeTypesEnabled(projectKey, isActivated, ConfigPersistence.isIssueStrategy(projectKey));
			return Response.ok(Status.ACCEPTED).build();
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			return Response.status(Status.CONFLICT).build();
		}
	}

	public static void setDefaultKnowledgeTypesEnabled(String projectKey, boolean isActivated,
			boolean isIssueStrategy) {
		Set<KnowledgeType> defaultKnowledgeTypes = KnowledgeType.getDefaulTypes();
		for (KnowledgeType knowledgeType : defaultKnowledgeTypes) {
			ConfigPersistence.setKnowledgeTypeEnabled(projectKey, knowledgeType.toString(), isActivated);
		}
	}

	@Path("/isIssueStrategy")
	@GET
	public Response isIssueStrategy(@QueryParam("projectKey") final String projectKey) {
		Response checkIfProjectKeyIsValidResponse = checkIfProjectKeyIsValid(projectKey);
		if (checkIfProjectKeyIsValidResponse.getStatus() != Status.OK.getStatusCode()) {
			return checkIfProjectKeyIsValidResponse;
		}
		Boolean isIssueStrategy = ConfigPersistence.isIssueStrategy(projectKey);
		return Response.ok(isIssueStrategy).build();
	}

	@Path("/setIssueStrategy")
	@POST
	public Response setIssueStrategy(@Context HttpServletRequest request, @QueryParam("projectKey") String projectKey,
			@QueryParam("isIssueStrategy") String isIssueStrategyString) {
		Response isValidDataResponse = checkIfDataIsValid(request, projectKey);
		if (isValidDataResponse.getStatus() != Status.OK.getStatusCode()) {
			return isValidDataResponse;
		}
		if (isIssueStrategyString == null) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "isIssueStrategy = null"))
					.build();
		}
		try {
			boolean isIssueStrategy = Boolean.valueOf(isIssueStrategyString);
			ConfigPersistence.setIssueStrategy(projectKey, isIssueStrategy);
			manageDefaultIssueTypes(projectKey, isIssueStrategy);
			return Response.ok(Status.ACCEPTED).build();
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			return Response.status(Status.CONFLICT).build();
		}
	}

	public static void manageDefaultIssueTypes(String projectKey, boolean isIssueStrategy) {
		Set<KnowledgeType> defaultKnowledgeTypes = KnowledgeType.getDefaulTypes();
		for (KnowledgeType knowledgeType : defaultKnowledgeTypes) {
			if (isIssueStrategy) {
				ConfigPersistence.setKnowledgeTypeEnabled(projectKey, knowledgeType.toString(), true);
				PluginInitializer.createIssueType(knowledgeType.toString());
				PluginInitializer.addIssueTypeToScheme(knowledgeType.toString(), projectKey);
			} else {
				PluginInitializer.removeIssueTypeFromScheme(knowledgeType.toString(), projectKey);
			}
		}
	}

	@Path("/isKnowledgeExtractedFromGit")
	@GET
	public Response isKnowledgeExtractedFromGit(@QueryParam("projectKey") final String projectKey) {
		Response checkIfProjectKeyIsValidResponse = checkIfProjectKeyIsValid(projectKey);
		if (checkIfProjectKeyIsValidResponse.getStatus() != Status.OK.getStatusCode()) {
			return checkIfProjectKeyIsValidResponse;
		}
		Boolean isKnowledgeExtractedFromGit = ConfigPersistence.isKnowledgeExtractedFromGit(projectKey);
		return Response.ok(isKnowledgeExtractedFromGit).build();
	}

	@Path("/setKnowledgeExtractedFromGit")
	@POST
	public Response setKnowledgeExtractedFromGit(@Context HttpServletRequest request,
			@QueryParam("projectKey") String projectKey,
			@QueryParam("isKnowledgeExtractedFromGit") String isKnowledgeExtractedFromGit) {
		Response isValidDataResponse = checkIfDataIsValid(request, projectKey);
		if (isValidDataResponse.getStatus() != Status.OK.getStatusCode()) {
			return isValidDataResponse;
		}
		if (isKnowledgeExtractedFromGit == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(ImmutableMap.of("error", "isKnowledgeExtractedFromGit = null")).build();
		}
		try {
			ConfigPersistence.setKnowledgeExtractedFromGit(projectKey, Boolean.valueOf(isKnowledgeExtractedFromGit));
			return Response.ok(Status.ACCEPTED).build();
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			return Response.status(Status.CONFLICT).build();
		}
	}

	@Path("/isKnowledgeExtractedFromIssues")
	@GET
	public Response isKnowledgeExtractedFromIssues(@QueryParam("projectKey") final String projectKey) {
		Response checkIfProjectKeyIsValidResponse = checkIfProjectKeyIsValid(projectKey);
		if (checkIfProjectKeyIsValidResponse.getStatus() != Status.OK.getStatusCode()) {
			return checkIfProjectKeyIsValidResponse;
		}
		Boolean isKnowledgeExtractedFromIssues = ConfigPersistence.isKnowledgeExtractedFromIssues(projectKey);
		return Response.ok(isKnowledgeExtractedFromIssues).build();
	}

	@Path("/setKnowledgeExtractedFromIssues")
	@POST
	public Response setKnowledgeExtractedFromIssues(@Context HttpServletRequest request,
			@QueryParam("projectKey") String projectKey,
			@QueryParam("isKnowledgeExtractedFromIssues") String isKnowledgeExtractedFromIssues) {
		Response isValidDataResponse = checkIfDataIsValid(request, projectKey);
		if (isValidDataResponse.getStatus() != Status.OK.getStatusCode()) {
			return isValidDataResponse;
		}
		if (isKnowledgeExtractedFromIssues == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(ImmutableMap.of("error", "isKnowledgeExtractedFromIssues = null")).build();
		}
		try {
			ConfigPersistence.setKnowledgeExtractedFromIssues(projectKey,
					Boolean.valueOf(isKnowledgeExtractedFromIssues));
			return Response.ok(Status.ACCEPTED).build();
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			return Response.status(Status.CONFLICT).build();
		}
	}

	@Path("/isKnowledgeTypeEnabled")
	@GET
	public Response isKnowledgeTypeEnabled(@QueryParam("projectKey") final String projectKey,
			@QueryParam("knowledgeType") String knowledgeType) {
		Response checkIfProjectKeyIsValidResponse = checkIfProjectKeyIsValid(projectKey);
		if (checkIfProjectKeyIsValidResponse.getStatus() != Status.OK.getStatusCode()) {
			return checkIfProjectKeyIsValidResponse;
		}
		Boolean isKnowledgeTypeEnabled = ConfigPersistence.isKnowledgeTypeEnabled(projectKey, knowledgeType);
		return Response.ok(isKnowledgeTypeEnabled).build();
	}

	@Path("/setKnowledgeTypeEnabled")
	@POST
	public Response setKnowledgeTypeEnabled(@Context HttpServletRequest request,
			@QueryParam("projectKey") String projectKey,
			@QueryParam("isKnowledgeTypeEnabled") String isKnowledgeTypeEnabledString,
			@QueryParam("knowledgeType") String knowledgeType) {
		Response isValidDataResponse = checkIfDataIsValid(request, projectKey);
		if (isValidDataResponse.getStatus() != Status.OK.getStatusCode()) {
			return isValidDataResponse;
		}
		if (isKnowledgeTypeEnabledString == null || knowledgeType == null) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "isKnowledgeTypeEnabled = null"))
					.build();
		}
		try {
			boolean isKnowledgeTypeEnabled = Boolean.valueOf(isKnowledgeTypeEnabledString);
			ConfigPersistence.setKnowledgeTypeEnabled(projectKey, knowledgeType, isKnowledgeTypeEnabled);
			if (ConfigPersistence.isIssueStrategy(projectKey)) {
				if (isKnowledgeTypeEnabled) {
					PluginInitializer.createIssueType(knowledgeType);
					PluginInitializer.addIssueTypeToScheme(knowledgeType, projectKey);
				} else {
					PluginInitializer.removeIssueTypeFromScheme(knowledgeType, projectKey);
				}
			}
			return Response.ok(Status.ACCEPTED).build();
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			return Response.status(Status.CONFLICT).build();
		}
	}

	@Path("/getKnowledgeTypes")
	@GET
	public Response getKnowledgeTypes(@QueryParam("projectKey") final String projectKey) {
		Response checkIfProjectKeyIsValidResponse = checkIfProjectKeyIsValid(projectKey);
		if (checkIfProjectKeyIsValidResponse.getStatus() != Status.OK.getStatusCode()) {
			return checkIfProjectKeyIsValidResponse;
		}
		List<String> knowledgeTypes = new ArrayList<String>();
		for (KnowledgeType knowledgeType : KnowledgeType.values()) {
			boolean isEnabled = ConfigPersistence.isKnowledgeTypeEnabled(projectKey, knowledgeType);
			if (isEnabled) {
				knowledgeTypes.add(knowledgeType.toString());
			}
		}
		return Response.ok(knowledgeTypes).build();
	}

	@Path("/getDefaultKnowledgeTypes")
	@GET
	public Response getDefaultKnowledgeTypes(@QueryParam("projectKey") final String projectKey) {
		Response checkIfProjectKeyIsValidResponse = checkIfProjectKeyIsValid(projectKey);
		if (checkIfProjectKeyIsValidResponse.getStatus() != Status.OK.getStatusCode()) {
			return checkIfProjectKeyIsValidResponse;
		}
		List<String> defaultKnowledgeTypes = new ArrayList<String>();
		for (KnowledgeType knowledgeType : KnowledgeType.getDefaulTypes()) {
			defaultKnowledgeTypes.add(knowledgeType.toString());
		}
		return Response.ok(defaultKnowledgeTypes).build();
	}

	@Path("/setGitConnector")
	@POST
	public Response setGitConnector(@Context HttpServletRequest request,
			@QueryParam("projectKey") final String projectKey,
			@QueryParam("gitAddress") final String gitAddress){
		Response isValidDataResponse = checkIfDataIsValid(request, projectKey);
		if (isValidDataResponse.getStatus() != Status.OK.getStatusCode()) {
			return isValidDataResponse;
		}
		if (gitAddress == null ) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "gitAddress = null"))
					.build();
		}
		try {
			ConfigPersistence.setGitAddress(projectKey, gitAddress);
			GitConfig gitConfig = new GitConfig(projectKey ,gitAddress);
			//TODO
			return Response.ok(Status.ACCEPTED).build();
		}catch (Exception e) {
			LOGGER.error(e.getMessage());
			return Response.status(Status.CONFLICT).build();
		}
	}

	@Path("/getGitAddress")
	@GET
	public Response getGitAddress(@QueryParam("projectKey") final String projectKey){
		Response checkIfProjectKeyIsValidResponse = checkIfProjectKeyIsValid(projectKey);
		if (checkIfProjectKeyIsValidResponse.getStatus() != Status.OK.getStatusCode()) {
			return checkIfProjectKeyIsValidResponse;
		}
		String gitAddress = ConfigPersistence.getGitAddress(projectKey);
		return Response.ok(gitAddress).build();
	}

	private Response checkIfDataIsValid(HttpServletRequest request, String projectKey) {
		if (request == null) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "request = null")).build();
		}

		Response userResponse = checkIfUserIsAuthorized(request);
		if (userResponse.getStatus() != Status.OK.getStatusCode()) {
			return userResponse;
		}

		Response projectResponse = checkIfProjectKeyIsValid(projectKey);
		if (projectResponse.getStatus() != Status.OK.getStatusCode()) {
			return projectResponse;
		}

		return Response.status(Status.OK).build();
	}

	private Response checkIfUserIsAuthorized(HttpServletRequest request) {
		String username = userManager.getRemoteUsername(request);
		if (username == null || !userManager.isSystemAdmin(username)) {
			LOGGER.warn("Unauthorized user (name:{}) tried to change configuration.", username);
			return Response.status(Status.UNAUTHORIZED).build();
		}
		return Response.status(Status.OK).build();
	}

	private Response checkIfProjectKeyIsValid(String projectKey) {
		if (projectKey == null || projectKey.equals("")) {
			LOGGER.error("Project configuration could not be changed since the project key is invalid.");
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Project key is invalid."))
					.build();
		}
		return Response.status(Status.OK).build();
	}
}