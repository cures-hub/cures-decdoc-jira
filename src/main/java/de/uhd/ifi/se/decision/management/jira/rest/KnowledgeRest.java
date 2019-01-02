package de.uhd.ifi.se.decision.management.jira.rest;

import java.util.List;

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

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.ImmutableMap;

import de.uhd.ifi.se.decision.management.jira.config.AuthenticationManager;
import de.uhd.ifi.se.decision.management.jira.extraction.model.Sentence;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElementImpl;
import de.uhd.ifi.se.decision.management.jira.model.DocumentationLocation;
import de.uhd.ifi.se.decision.management.jira.model.Graph;
import de.uhd.ifi.se.decision.management.jira.model.GraphImpl;
import de.uhd.ifi.se.decision.management.jira.model.GraphImplFiltered;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.Link;
import de.uhd.ifi.se.decision.management.jira.persistence.AbstractPersistenceManager;
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
		
		JiraIssueCommentPersistenceManager persistenceManager = new JiraIssueCommentPersistenceManager(decisionKnowledgeElement.getProject().getProjectKey());
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
			GenericLinkManager.deleteLinksForElement("s" + sentence.getId());
			JiraIssueCommentPersistenceManager.createLinksForNonLinkedElementsForIssue(sentence.getIssueId());
			return Response.status(Status.OK).build();
		}
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity(ImmutableMap.of("error", "Setting element irrelevant failed.")).build();
	}

	@Path("getAllElementsMatchingQuery")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getAllElementsMatchingQuery(@QueryParam("projectKey") String projectKey,
			@QueryParam("query") String query, @Context HttpServletRequest request) {
		if (projectKey == null || query == null || request == null) {
			return Response.status(Status.BAD_REQUEST).entity(
					ImmutableMap.of("error", "Getting elements matching the query failed due to a bad request."))
					.build();
		}
		ApplicationUser user = AuthenticationManager.getUser(request);
		GraphFiltering filter = new GraphFiltering(projectKey, query, user);
		filter.produceResultsFromQuery();
		List<DecisionKnowledgeElement> queryResult = filter.getAllElementsMatchingQuery();
		if (queryResult == null) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(ImmutableMap.of("error", "Getting elements matching the query failed.")).build();
		}
		return Response.ok(queryResult).build();

	}

	@Path("/getAllElementsLinkedToElement")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getAllElementsLinkedToElement(@QueryParam("elementKey") String elementKey,
			@QueryParam("URISearch") String uriSearch, @Context HttpServletRequest request) {
		if (request == null) {
			return Response.status(Status.BAD_REQUEST).entity(
					ImmutableMap.of("error", "Getting elements matching the query failed due to a bad request."))
					.build();
		}
		String projectKey = getProjectKey(elementKey);
		ApplicationUser user = AuthenticationManager.getUser(request);

		Graph graph;
		if ((uriSearch.matches("\\?jql=(.)+")) || (uriSearch.matches("\\?filter=(.)+"))) {
			GraphFiltering filter = new GraphFiltering(projectKey, uriSearch, user);
			filter = new GraphFiltering(projectKey, uriSearch, user);
			filter.produceResultsFromQuery();
			graph = new GraphImplFiltered(projectKey, elementKey, filter);
		} else {
			graph = new GraphImpl(projectKey, elementKey);
		}
		List<DecisionKnowledgeElement> filteredElements = graph.getAllElements();

		return Response.ok(filteredElements).build();
	}

	private String getProjectKey(String elementKey) {
		return elementKey.split("-")[0];
	}
}