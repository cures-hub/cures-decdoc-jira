package de.uhd.ifi.se.decision.management.jira.rest;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.uhd.ifi.se.decision.management.jira.model.*;
import de.uhd.ifi.se.decision.management.jira.persistence.ConfigPersistenceManager;
import de.uhd.ifi.se.decision.management.jira.quality.generalmetrics.GeneralMetricCalculator;
import de.uhd.ifi.se.decision.management.jira.view.dashboard.GeneralMetricsDashboardItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.ImmutableMap;

import de.uhd.ifi.se.decision.management.jira.config.AuthenticationManager;
import de.uhd.ifi.se.decision.management.jira.persistence.singlelocations.JiraIssuePersistenceManager;
import de.uhd.ifi.se.decision.management.jira.quality.generalmetrics.CommentMetricCalculator;

/**
 * REST resource for dashboards
 */
@Path("/dashboard")
public class DashboardRest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ViewRest.class);

	@Path("/numberOfCommentsPerJiraIssue")
	@GET
	public Response getNumberOfCommentsPerJiraIssue(@Context HttpServletRequest request, @QueryParam("projectKey") String projectKey) {
		if (request == null || projectKey == null) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "There is no project selected"))
				.build();
		}

		ApplicationUser user = AuthenticationManager.getUser(request);
		List<Issue> jiraIssues = JiraIssuePersistenceManager.getAllJiraIssuesForProject(user, projectKey);
		CommentMetricCalculator commentCalculator = new CommentMetricCalculator(jiraIssues);

		Map<String, Integer> numberOfCommentsPerIssue = commentCalculator.getNumberOfCommentsPerIssue();

		return Response.status(Status.OK).entity(numberOfCommentsPerIssue).build();
	}

	@Path("/numberOfCommitsPerJiraIssue")
	@GET
	public Response getNumberOfCommitsPerJiraIssue(@Context HttpServletRequest request, @QueryParam("projectKey") String projectKey) {
		if (request == null || projectKey == null) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "There is no project selected"))
				.build();
		}

		ApplicationUser user = AuthenticationManager.getUser(request);
		List<Issue> jiraIssues = JiraIssuePersistenceManager.getAllJiraIssuesForProject(user, projectKey);
		CommentMetricCalculator commentCalculator = new CommentMetricCalculator(jiraIssues);

		if (!ConfigPersistenceManager.isKnowledgeExtractedFromGit(projectKey)) {
			return Response.status(Status.OK).entity(new HashMap<>()).build();
		}

		Map<String, Integer> numberOfCommitsPerIssue = commentCalculator.getNumberOfCommitsPerIssue();

		return Response.status(Status.OK).entity(numberOfCommitsPerIssue).build();
	}

	@Path("/distributionOfKnowledgeTypes")
	@GET
	public Response getDistributionOfKnowledgeTypes(@Context HttpServletRequest request, @QueryParam("projectKey") String projectKey) {
		if (request == null || projectKey == null) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "There is no project selected"))
				.build();
		}

		KnowledgeGraph graph = KnowledgeGraph.getOrCreate(projectKey);

		Map<String, Integer> distributionOfKnowledgeTypes = new HashMap<String, Integer>();
		for (KnowledgeType type : KnowledgeType.getDefaultTypes()) {
			List<KnowledgeElement> elements = graph.getElements(type);
			distributionOfKnowledgeTypes.put(type.toString(), elements.size());
		}

		return Response.status(Status.OK).entity(distributionOfKnowledgeTypes).build();
	}

	@Path("/requirementsAndCodeFiles")
	@GET
	public Response getRequirementsAndCodeFiles(@Context HttpServletRequest request, @QueryParam("projectKey") String projectKey) {
		if (request == null || projectKey == null) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "There is no project selected"))
				.build();
		}

		ApplicationUser user = AuthenticationManager.getUser(request);
		List<Issue> jiraIssues = JiraIssuePersistenceManager.getAllJiraIssuesForProject(user, projectKey);
		KnowledgeGraph graph = KnowledgeGraph.getOrCreate(projectKey);

		Map<String, Integer> summaryMap = new HashMap<String, Integer>();
		int numberOfRequirements = 0;
		List<String> requirementsTypes = KnowledgeType.getRequirementsTypes();
		for (Issue issue : jiraIssues) {
			if (requirementsTypes.contains(issue.getIssueType().getName())) {
				numberOfRequirements++;
			}
		}
		summaryMap.put("Requirements", numberOfRequirements);
		summaryMap.put("Code Files", graph.getElements(KnowledgeType.CODE).size());

		return Response.status(Status.OK).entity(summaryMap).build();
	}

	@Path("/numberOfElementsPerDocumentationLocation")
	@GET
	public Response getNumberOfElementsPerDocumentationLocation(@Context HttpServletRequest request, @QueryParam("projectKey") String projectKey) {
		if (request == null || projectKey == null) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "There is no project selected"))
				.build();
		}

		KnowledgeGraph graph = KnowledgeGraph.getOrCreate(projectKey);

		Map<String, String> originMap = new HashMap<>();

		String elementsInJiraIssues = "";
		String elementsInJiraIssueText = "";
		String elementsInCommitMessages = "";
		String elementsInCodeComments = "";
		Set<KnowledgeElement> elements = graph.vertexSet();
		for (KnowledgeElement element : elements) {
			if (element.getType() == KnowledgeType.CODE || element.getType() == KnowledgeType.OTHER) {
				continue;
			}
			if (element.getDocumentationLocation() == DocumentationLocation.JIRAISSUE) {
				elementsInJiraIssues += element.getKey() + " ";
				continue;
			}
			if (element.getDocumentationLocation() == DocumentationLocation.JIRAISSUETEXT) {
				if (element.getOrigin() == Origin.COMMIT) {
					elementsInCommitMessages += element.getKey() + " ";
				} else {
					elementsInJiraIssueText += element.getKey() + " ";
				}
			}
			if (element.getDocumentationLocation() == DocumentationLocation.CODE) {
				elementsInCodeComments += element.getKey() + " ";
			}
		}
		originMap.put("Jira Issue Description or Comment", elementsInJiraIssueText.trim());
		originMap.put("Entire Jira Issue", elementsInJiraIssues.trim());
		originMap.put("Commit Message", elementsInCommitMessages.trim());
		originMap.put("Code Comment", elementsInCodeComments.trim());

		return Response.status(Status.OK).entity(originMap).build();
	}

	@Path("/numberOfRelevantComments")
	@GET
	public Response getNumberOfRelevantComments(@Context HttpServletRequest request, @QueryParam("projectKey") String projectKey) {
		if (request == null || projectKey == null) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "There is no project selected"))
				.build();
		}

		ApplicationUser user = AuthenticationManager.getUser(request);
		List<Issue> jiraIssues = JiraIssuePersistenceManager.getAllJiraIssuesForProject(user, projectKey);
		CommentMetricCalculator commentCalculator = new CommentMetricCalculator(jiraIssues);

		Map<String, Integer> numberOfRelevantComments = commentCalculator.getNumberOfRelevantComments();

		return Response.status(Status.OK).entity(numberOfRelevantComments).build();
	}

	@Path("/generalMetrics")
	@GET
	public Response getGeneralMetrics(@Context HttpServletRequest request, @QueryParam("projectKey") String projectKey) {
		if (request == null || projectKey == null) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "There is no project selected"))
				.build();
		}

		ApplicationUser user = AuthenticationManager.getUser(request);

		Map<String, Object> metrics = new LinkedHashMap<>();
		GeneralMetricCalculator metricCalculator = new GeneralMetricCalculator(user, projectKey);

		metrics.put("numberOfCommentsPerJiraIssue", metricCalculator.numberOfCommentsPerIssue());
		metrics.put("numberOfCommitsPerJiraIssue", metricCalculator.getNumberOfCommits());

		metrics.put("distributionOfKnowledgeTypes", metricCalculator.getDistributionOfKnowledgeTypes());
		metrics.put("requirementsAndCodeFiles", metricCalculator.getReqAndClassSummary());
		metrics.put("numberOfElementsPerDocumentationLocation", metricCalculator.getElementsFromDifferentOrigins());
		metrics.put("numberOfRelevantComments", metricCalculator.getNumberOfRelevantComments());

		return Response.status(Status.OK).entity(metrics).build();
	}
}
