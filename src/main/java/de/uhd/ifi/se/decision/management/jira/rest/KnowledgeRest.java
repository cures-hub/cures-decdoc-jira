package de.uhd.ifi.se.decision.management.jira.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.EditList;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.ImmutableMap;

import de.uhd.ifi.se.decision.management.jira.config.AuthenticationManager;
import de.uhd.ifi.se.decision.management.jira.extraction.git.GitClient;
import de.uhd.ifi.se.decision.management.jira.extraction.git.GitClientImpl;
import de.uhd.ifi.se.decision.management.jira.extraction.git.TaskCodeSummarizer;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.DocumentationLocation;
import de.uhd.ifi.se.decision.management.jira.model.Graph;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.Link;
import de.uhd.ifi.se.decision.management.jira.model.Sentence;
import de.uhd.ifi.se.decision.management.jira.model.impl.DecisionKnowledgeElementImpl;
import de.uhd.ifi.se.decision.management.jira.model.impl.GraphImpl;
import de.uhd.ifi.se.decision.management.jira.model.impl.GraphImplFiltered;
import de.uhd.ifi.se.decision.management.jira.persistence.AbstractPersistenceManager;
import de.uhd.ifi.se.decision.management.jira.persistence.ConfigPersistenceManager;
import de.uhd.ifi.se.decision.management.jira.persistence.GenericLinkManager;
import de.uhd.ifi.se.decision.management.jira.persistence.JiraIssueCommentPersistenceManager;
import de.uhd.ifi.se.decision.management.jira.view.GraphFiltering;

/**
 * REST resource: Enables creation, editing, and deletion of decision knowledge
 * elements and their links
 */
@Path("/decisions")
public class KnowledgeRest {

	@Path("/getDecisionKnowledgeElement")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getDecisionKnowledgeElement(@QueryParam("id") long id, @QueryParam("projectKey") String projectKey,
			@QueryParam("documentationLocation") String documentationLocation) {
		if (projectKey == null || id <= 0) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error",
					"Decision knowledge element could not be received due to a bad request (element id or project key was missing)."))
					.build();
		}
		AbstractPersistenceManager persistenceManager = AbstractPersistenceManager.getPersistenceManager(projectKey,
				documentationLocation);

		DecisionKnowledgeElement decisionKnowledgeElement = persistenceManager.getDecisionKnowledgeElement(id);
		if (decisionKnowledgeElement != null) {
			return Response.status(Status.OK).entity(decisionKnowledgeElement).build();
		}
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity(ImmutableMap.of("error", "Decision knowledge element was not found for the given id.")).build();
	}

	@Path("/getLinkedElements")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getLinkedElements(@QueryParam("id") long id, @QueryParam("projectKey") String projectKey,
			@QueryParam("documentationLocation") String documentationLocation) {
		if (projectKey == null || id <= 0) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error",
					"Linked decision knowledge elements could not be received due to a bad request (element id or project key was missing)."))
					.build();
		}
		AbstractPersistenceManager persistenceManager = AbstractPersistenceManager.getPersistenceManager(projectKey,
				documentationLocation);
		List<DecisionKnowledgeElement> linkedDecisionKnowledgeElements = persistenceManager.getLinkedElements(id);
		return Response.ok(linkedDecisionKnowledgeElements).build();
	}

	@Path("/getUnlinkedElements")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getUnlinkedElements(@QueryParam("id") long id, @QueryParam("projectKey") String projectKey,
			@QueryParam("documentationLocation") String documentationLocation) {
		if (projectKey == null || id <= 0) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error",
					"Unlinked decision knowledge elements could not be received due to a bad request (element id or project key was missing)."))
					.build();
		}
		AbstractPersistenceManager persistenceManager = AbstractPersistenceManager.getPersistenceManager(projectKey,
				documentationLocation);
		List<DecisionKnowledgeElement> unlinkedDecisionKnowledgeElements = persistenceManager.getUnlinkedElements(id);
		return Response.ok(unlinkedDecisionKnowledgeElements).build();
	}

	@Path("/createDecisionKnowledgeElement")
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public Response createDecisionKnowledgeElement(@Context HttpServletRequest request,
			DecisionKnowledgeElement element, @QueryParam("idOfExistingElement") long idOfExistingElement,
			@QueryParam("documentationLocationOfExistingElement") String documentationLocationOfExistingElement) {
		if (element == null || request == null) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error",
					"Creation of decision knowledge element failed due to a bad request (element or request is null)."))
					.build();
		}

		ApplicationUser user = AuthenticationManager.getUser(request);

		DecisionKnowledgeElement existingElement = new DecisionKnowledgeElementImpl();
		existingElement.setId(idOfExistingElement);
		existingElement.setDocumentationLocation(documentationLocationOfExistingElement);
		existingElement.setProject(element.getProject().getProjectKey());

		AbstractPersistenceManager persistenceManager = AbstractPersistenceManager.getPersistenceManager(element);
		DecisionKnowledgeElement elementWithId = persistenceManager.insertDecisionKnowledgeElement(element, user,
				existingElement);

		if (elementWithId == null) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(ImmutableMap.of("error", "Creation of decision knowledge element failed.")).build();
		}

		if (idOfExistingElement == 0) {
			return Response.status(Status.OK).entity(elementWithId).build();
		}
		Link link = Link.instantiateDirectedLink(existingElement, elementWithId);
		long linkId = AbstractPersistenceManager.insertLink(link, user);
		if (linkId == 0) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(ImmutableMap.of("error", "Creation of link failed.")).build();
		}

		return Response.status(Status.OK).entity(elementWithId).build();
	}

	@Path("/updateDecisionKnowledgeElement")
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public Response updateDecisionKnowledgeElement(@Context HttpServletRequest request,
			DecisionKnowledgeElement element, @QueryParam("idOfParentElement") long idOfParentElement,
			@QueryParam("documentationLocationOfParentElement") String documentationLocationOfParentElement) {
		if (request == null || element == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(ImmutableMap.of("error", "Element could not be updated due to a bad request.")).build();
		}
		ApplicationUser user = AuthenticationManager.getUser(request);
		AbstractPersistenceManager persistenceManager = AbstractPersistenceManager.getPersistenceManager(element);

		DecisionKnowledgeElement formerElement = persistenceManager.getDecisionKnowledgeElement(element.getId());
		if (formerElement == null) {
			return Response.status(Status.NOT_FOUND)
					.entity(ImmutableMap.of("error", "Decision knowledge element could not be found in database."))
					.build();
		}

		boolean isUpdated = persistenceManager.updateDecisionKnowledgeElement(element, user);

		if (!isUpdated) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(ImmutableMap.of("error", "Element could not be updated due to an internal server error."))
					.build();
		}

		long linkId = AbstractPersistenceManager.updateLink(element, formerElement.getType(), idOfParentElement,
				documentationLocationOfParentElement, user);
		if (linkId == 0) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(ImmutableMap.of("error", "Link could not be updated.")).build();
		}
		return Response.status(Status.OK).build();
	}

	@Path("/deleteDecisionKnowledgeElement")
	@DELETE
	@Produces({ MediaType.APPLICATION_JSON })
	public Response deleteDecisionKnowledgeElement(@Context HttpServletRequest request,
			DecisionKnowledgeElement decisionKnowledgeElement) {
		if (decisionKnowledgeElement == null || request == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(ImmutableMap.of("error", "Deletion of decision knowledge element failed.")).build();
		}
		AbstractPersistenceManager persistenceManager = AbstractPersistenceManager
				.getPersistenceManager(decisionKnowledgeElement);
		ApplicationUser user = AuthenticationManager.getUser(request);

		boolean isDeleted = persistenceManager.deleteDecisionKnowledgeElement(decisionKnowledgeElement.getId(), user);
		if (isDeleted) {
			return Response.status(Status.OK).entity(true).build();
		}
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity(ImmutableMap.of("error", "Deletion of decision knowledge element failed.")).build();
	}

	@Path("/createLink")
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public Response createLink(@Context HttpServletRequest request, @QueryParam("projectKey") String projectKey,
			@QueryParam("knowledgeTypeOfChild") String knowledgeTypeOfChild, @QueryParam("idOfParent") long idOfParent,
			@QueryParam("documentationLocationOfParent") String documentationLocationOfParent,
			@QueryParam("idOfChild") long idOfChild,
			@QueryParam("documentationLocationOfChild") String documentationLocationOfChild) {
		if (request == null || projectKey == null || idOfChild <= 0 || idOfParent <= 0) {
			return Response.status(Status.BAD_REQUEST)
					.entity(ImmutableMap.of("error", "Link could not be created due to a bad request.")).build();
		}
		ApplicationUser user = AuthenticationManager.getUser(request);

		DecisionKnowledgeElement parentElement = new DecisionKnowledgeElementImpl();
		parentElement.setId(idOfParent);
		parentElement.setDocumentationLocation(documentationLocationOfParent);
		parentElement.setProject(projectKey);

		DecisionKnowledgeElement childElement = new DecisionKnowledgeElementImpl();
		childElement.setId(idOfChild);
		childElement.setDocumentationLocation(documentationLocationOfChild);
		childElement.setType(knowledgeTypeOfChild);
		childElement.setProject(projectKey);

		Link link = Link.instantiateDirectedLink(parentElement, childElement);
		long linkId = AbstractPersistenceManager.insertLink(link, user);
		if (linkId == 0) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(ImmutableMap.of("error", "Creation of link failed.")).build();
		}
		return Response.status(Status.OK).entity(ImmutableMap.of("id", linkId)).build();
	}

	@Path("/deleteLink")
	@DELETE
	@Produces({ MediaType.APPLICATION_JSON })
	public Response deleteLink(@QueryParam("projectKey") String projectKey, @Context HttpServletRequest request,
			Link link) {
		if (projectKey == null || request == null || link == null) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Deletion of link failed."))
					.build();
		}
		link.getSourceElement().setProject(projectKey);
		ApplicationUser user = AuthenticationManager.getUser(request);
		boolean isDeleted = AbstractPersistenceManager.deleteLink(link, user);

		if (isDeleted) {
			return Response.status(Status.OK).build();
		}
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity(ImmutableMap.of("error", "Deletion of link failed.")).build();
	}

	@Path("/createIssueFromSentence")
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public Response createIssueFromSentence(@Context HttpServletRequest request,
			DecisionKnowledgeElement decisionKnowledgeElement) {
		if (decisionKnowledgeElement == null || request == null) {
			return Response.status(Status.BAD_REQUEST).entity(
					ImmutableMap.of("error", "The documentation location could not be changed due to a bad request."))
					.build();
		}

		ApplicationUser user = AuthenticationManager.getUser(request);

		JiraIssueCommentPersistenceManager persistenceManager = new JiraIssueCommentPersistenceManager(
				decisionKnowledgeElement.getProject().getProjectKey());
		Issue issue = persistenceManager.createJIRAIssueFromSentenceObject(decisionKnowledgeElement.getId(), user);

		if (issue != null) {
			return Response.status(Status.OK).entity(issue).build();
		}
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity(ImmutableMap.of("error", "The documentation location could not be changed.")).build();

	}

	@Path("/setSentenceIrrelevant")
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public Response setSentenceIrrelevant(@Context HttpServletRequest request,
			DecisionKnowledgeElement decisionKnowledgeElement) {
		if (request == null || decisionKnowledgeElement == null || decisionKnowledgeElement.getId() <= 0) {
			return Response.status(Status.BAD_REQUEST)
					.entity(ImmutableMap.of("error", "Setting element irrelevant failed due to a bad request."))
					.build();
		}
		AbstractPersistenceManager persistenceManager = AbstractPersistenceManager
				.getPersistenceManager(decisionKnowledgeElement);
		if (decisionKnowledgeElement.getDocumentationLocation() != DocumentationLocation.JIRAISSUECOMMENT) {
			return Response.status(Status.SERVICE_UNAVAILABLE)
					.entity(ImmutableMap.of("error", "Only sentence elements can be set to irrelevant.")).build();
		}
		Sentence sentence = (Sentence) persistenceManager.getDecisionKnowledgeElement(decisionKnowledgeElement.getId());
		if (sentence == null) {
			return Response.status(Status.NOT_FOUND)
					.entity(ImmutableMap.of("error", "Element could not be found in database.")).build();
		}

		sentence.setRelevant(false);
		sentence.setType(KnowledgeType.OTHER);
		sentence.setSummary(null);
		boolean isUpdated = persistenceManager.updateDecisionKnowledgeElement(sentence, null);
		if (isUpdated) {
			GenericLinkManager.deleteLinksForElement(sentence.getId(), DocumentationLocation.JIRAISSUECOMMENT);
			JiraIssueCommentPersistenceManager.createLinksForNonLinkedElementsForIssue(sentence.getJiraIssueId());
			return Response.status(Status.OK).build();
		}
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity(ImmutableMap.of("error", "Setting element irrelevant failed.")).build();
	}

	@Path("/getSummarizedCode")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getSummarizedCode(@QueryParam("id") String id, @QueryParam("projectKey") String projectKey,
			@QueryParam("documentationLocation") String documentationLocation, @Context HttpServletRequest request) {
		if (projectKey == null || id == null || request == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(ImmutableMap.of("error", "Getting summarized code failed due to a bad request.")).build();
		}

		Long elementId = Long.parseLong(id);

		IssueManager issueManager = ComponentAccessor.getIssueManager();
		Issue jiraIssue = issueManager.getIssueObject(elementId);

		String jiraIssueKey = "";
		if (jiraIssue == null) {
			jiraIssueKey = JiraIssueCommentPersistenceManager.getJiraIssueKey(elementId);
		} else {
			jiraIssueKey = jiraIssue.getKey();
		}

		if (!ConfigPersistenceManager.isKnowledgeExtractedFromGit(projectKey)) {
			return Response.status(Status.SERVICE_UNAVAILABLE)
					.entity(ImmutableMap.of("error",
							"Getting summarized code failed since git extraction is disabled for this project."))
					.build();
		}

		String queryResult = "";
		try {
			GitClient gitClient = new GitClientImpl(projectKey);
			Map<DiffEntry, EditList> diff = gitClient.getDiff(jiraIssueKey);
			if (diff == null) {
				queryResult = "This JIRA issue does not have any code committed.";
			} else {
				queryResult += TaskCodeSummarizer.summarizer(diff, projectKey, true);
			}
		} catch (IOException | GitAPIException e) {
			e.printStackTrace();
		}

		return Response.ok(queryResult).build();
	}

	/**
	 * @param Enum
	 *            resultType["ELEMENTS_QUERY","ELEMENTS_LINKED","ELEMENTS_QUERY_LINKED"]
	 * @param String
	 *            projectKey
	 * @param String
	 *            query
	 * @param String
	 *            elementKey
	 * @param String
	 *            request
	 * @return List of Objects or List of Lists with Objects
	 */
	@Path("getAllElementsMatchingQuery")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getAllElementsMatchingQuery(@QueryParam("resultType") String resultType,
			@QueryParam("projectKey") String iProjectKey, @QueryParam("query") String query,
			@QueryParam("elementKey") String iElementKey, @Context HttpServletRequest request) {
		if (resultType == null || query == null || request == null) {
			return Response.status(Status.BAD_REQUEST).entity(
					ImmutableMap.of("error", "Getting elements matching the query failed due to a bad request."))
					.build();
		}
		String elementKey = helperCheckIfNotNullThenSetValue(iElementKey);

		String projectKey = helperCheckIfNullThenGetProjectKey(iProjectKey, elementKey);

		ApplicationUser user = AuthenticationManager.getUser(request);
		List<DecisionKnowledgeElement> queryResult = new ArrayList<>();
		List<List<DecisionKnowledgeElement>> elementsQueryLinked = new ArrayList<List<DecisionKnowledgeElement>>();
		try {
			switch (resultType) {
			case "ELEMENTS_QUERY":
				queryResult = getHelperMatchedQueryElements(user, projectKey, query);
				break;
			case "ELEMENTS_LINKED":
				queryResult = getHelperAllElementsLinkedToElement(user, projectKey, query, elementKey);
				break;
			case "ELEMENTS_QUERY_LINKED":
				elementsQueryLinked = getHelperAllElementsMatchingQueryAndLinked(user, projectKey, query);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(
					ImmutableMap.of("error", "Getting elements matching the query failed due to an internal error."))
					.build();
		}
		if (queryResult.size() == 0 && elementsQueryLinked.size() == 0) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
					ImmutableMap.of("error", "Getting elements matching the query failed. No Results were found"))
					.build();
		} else if (elementsQueryLinked.size() > 0) {
			return Response.ok(elementsQueryLinked).build();
		} else {
			return Response.ok(queryResult).build();
		}
	}

	private String getProjectKey(String elementKey) {
		return elementKey.split("-")[0];
	}

	/**
	 * REST HELPERS to avoid doubled code:
	 **/

	private List<DecisionKnowledgeElement> getHelperMatchedQueryElements(ApplicationUser user, String projectKey,
			String query) {
		GraphFiltering filter = new GraphFiltering(projectKey, query, user);
		filter.produceResultsFromQuery();
		return filter.getAllElementsMatchingQuery();
	}

	private List<DecisionKnowledgeElement> getHelperAllElementsLinkedToElement(ApplicationUser user, String projectKey,
			String query, String elementKey) {
		Graph graph;
		if ((query.matches("\\?jql=(.)+")) || (query.matches("\\?filter=(.)+"))) {
			GraphFiltering filter = new GraphFiltering(projectKey, query, user);
			filter.produceResultsFromQuery();
			graph = new GraphImplFiltered(projectKey, elementKey, filter);
		} else {
			graph = new GraphImpl(projectKey, elementKey);
		}
		return graph.getAllElements();
	}

	private List<List<DecisionKnowledgeElement>> getHelperAllElementsMatchingQueryAndLinked(ApplicationUser user,
			String projectKey, String query) {
		List<DecisionKnowledgeElement> tempQueryResult = getHelperMatchedQueryElements(user, projectKey, query);
		List<DecisionKnowledgeElement> addedElements = new ArrayList<DecisionKnowledgeElement>();
		List<List<DecisionKnowledgeElement>> elementsQueryLinked = new ArrayList<List<DecisionKnowledgeElement>>();

		// now iti over query result
		for (DecisionKnowledgeElement current : tempQueryResult) {
			// check if in addedElements list
			if (!addedElements.contains(current)) {
				// if not get the connected tree
				String currentElementKey = current.getKey();
				if ("".equals(projectKey)) {
					projectKey = current.getProject().getProjectKey();
				}
				List<DecisionKnowledgeElement> filteredElements = getHelperAllElementsLinkedToElement(user, projectKey,
						query, currentElementKey);
				// add each element to the list
				addedElements.addAll(filteredElements);
				// add list to the big list
				elementsQueryLinked.add(filteredElements);
			}
		}
		return elementsQueryLinked;
	}

	private String helperCheckIfNotNullThenSetValue(String iValue) {
		String result;
		if (iValue != null) {
			result = iValue;
		} else {
			result = "";
		}
		return result;
	}

	private String helperCheckIfNullThenGetProjectKey(String iProjectKey, String elementKey) {
		String projectKey;
		if (iProjectKey == null) {
			projectKey = getProjectKey(elementKey);
		} else {
			projectKey = iProjectKey;
		}
		return projectKey;
	}
}
