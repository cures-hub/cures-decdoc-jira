package de.uhd.ifi.se.decision.management.jira.rest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.ImmutableMap;
import de.uhd.ifi.se.decision.management.jira.config.AuthenticationManager;
import de.uhd.ifi.se.decision.management.jira.extraction.GitClient;
import de.uhd.ifi.se.decision.management.jira.extraction.impl.GitClientImpl;
import de.uhd.ifi.se.decision.management.jira.extraction.versioncontrol.CommitMessageToCommentTranscriber;
import de.uhd.ifi.se.decision.management.jira.extraction.versioncontrol.GitDecXtract;
import de.uhd.ifi.se.decision.management.jira.filtering.FilterSettings;
import de.uhd.ifi.se.decision.management.jira.filtering.impl.FilterSettingsImpl;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.DocumentationLocation;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.persistence.KnowledgePersistenceManager;
import de.uhd.ifi.se.decision.management.jira.view.diffviewer.DiffViewer;
import de.uhd.ifi.se.decision.management.jira.view.matrix.Matrix;
import de.uhd.ifi.se.decision.management.jira.view.treant.Treant;
import de.uhd.ifi.se.decision.management.jira.view.treeviewer.TreeViewer;
import de.uhd.ifi.se.decision.management.jira.view.vis.VisDataProvider;
import de.uhd.ifi.se.decision.management.jira.view.vis.VisGraph;
import de.uhd.ifi.se.decision.management.jira.view.vis.VisTimeLine;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST resource for view
 */
@Path("/view")
public class ViewRest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ViewRest.class);
    private GitClient gitClient;
    private GitDecXtract extractor;

    @Path("/elementsFromBranchesOfProject")
    @GET
    public Response getAllFeatureBranchesTree(@QueryParam("projectKey") String projectKey) {
        Response checkIfProjectKeyIsValidResponse = checkIfProjectKeyIsValid(projectKey);
        if (checkIfProjectKeyIsValidResponse.getStatus() != Status.OK.getStatusCode()) {
            return checkIfProjectKeyIsValidResponse;
        }

        // get all project branches
        return getDiffViewerResponse(projectKey, projectKey);
    }

    //FIXME: Unit test
    @Path("/elementsFromBranchesOfJiraIssue")
    @GET
    public Response getFeatureBranchTree(@Context HttpServletRequest request, @QueryParam("issueKey") String issueKey) throws PermissionException {
        issueKey = normalizeIssueKey(issueKey); // ex: issueKey=ConDec-498
        Issue issue = getIssue(issueKey);
        if (issue == null) {
            return issueKeyIsInvalid();
        }

        String regexFilter = issueKey.toUpperCase() + "\\.|" + issueKey.toUpperCase() + "$";
        // get feature branches of an issue
        return getDiffViewerResponse(getProjectKey(issueKey), regexFilter,
                ComponentAccessor.getIssueManager().getIssueByCurrentKey(issueKey));
    }

    private Response getDiffViewerResponse(String projectKey, String filter, Issue issue) throws PermissionException {
        Response resp = this.getDiffViewerResponse(projectKey, filter);
        Pattern filterPattern = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);
        CommitMessageToCommentTranscriber transcriber;
        // get current branch name
        // iterate over commits to get all messages and post each one as a comment
        // make sure to not post duplicates
        List<Ref> branches = gitClient.getRemoteBranches();
        for (Ref branch : branches) {
            Matcher branchMatcher = filterPattern.matcher(branch.getName());
            if (branchMatcher.find()) {
                for (RevCommit commit : gitClient.getFeatureBranchCommits(GitDecXtract.generateBranchShortName(branch))) {
                    transcriber = new CommitMessageToCommentTranscriber(commit.getFullMessage());
                    transcriber.generateCommentString();
                    transcriber.postComment(issue);
                }
            }
        }
        return resp;
    }

    private Response getDiffViewerResponse(String projectKey, String filter) {
        gitClient = new GitClientImpl(projectKey);
        List<Ref> branches = gitClient.getRemoteBranches();
        Pattern filterPattern = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);

        if (branches.isEmpty()) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        Map<Ref, List<DecisionKnowledgeElement>> ratBranchList = new HashMap<>();
        extractor = new GitDecXtract(projectKey);
        // TODO: move the loop elsewhere or maybe in GitDecXtract
        for (Ref branch : branches) {
            String branchName = branch.getName();
            Matcher branchMatcher = filterPattern.matcher(branchName);
            if (branchMatcher.find()) {
                ratBranchList.put(branch, extractor.getElements(branch));
            }
        }
        extractor.close();
        gitClient.close();
        DiffViewer diffView = new DiffViewer(ratBranchList);
        Response resp = null;
        try {
            Response.ResponseBuilder respBuilder = Response.ok(diffView);
            resp = respBuilder.build();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
        }
        return resp;
    }

    private String normalizeIssueKey(String issueKey) {
        return issueKey.toUpperCase();
    }

    @Path("/getTreeViewer")
    @GET
    public Response getTreeViewer(@QueryParam("projectKey") String projectKey,
                                  @QueryParam("rootElementType") String rootElementType) {
        Response checkIfProjectKeyIsValidResponse = checkIfProjectKeyIsValid(projectKey);
        if (checkIfProjectKeyIsValidResponse.getStatus() != Status.OK.getStatusCode()) {
            return checkIfProjectKeyIsValidResponse;
        }
        if (rootElementType == null) {
            rootElementType = "decision";
        }

        TreeViewer treeViewer = new TreeViewer(projectKey, KnowledgeType.getKnowledgeType(rootElementType));
        return Response.ok(treeViewer).build();
    }

    @Path("/getTreeViewer2")
    @GET
    public Response getTreeViewer2(@QueryParam("issueKey") String issueKey,
                                   @QueryParam("showRelevant") String showRelevant) {
        if (!issueKey.contains("-")) {
            return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Issue Key is not valid."))
                    .build();
        }
        Boolean[] booleanArray = Arrays.stream(showRelevant.split(",")).map(Boolean::parseBoolean)
                .toArray(Boolean[]::new);
        String projectKey = issueKey.substring(0, issueKey.indexOf("-"));
        Response checkIfProjectKeyIsValidResponse = checkIfProjectKeyIsValid(projectKey);
        if (checkIfProjectKeyIsValidResponse.getStatus() != Status.OK.getStatusCode()) {
            return checkIfProjectKeyIsValidResponse;
        }

        TreeViewer treeViewer = new TreeViewer(issueKey, booleanArray);
        return Response.ok(treeViewer).build();
    }

	@Path("/getEvolutionData")
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getEvolutionData(@Context HttpServletRequest request, FilterSettings filterSettings) {
		if (request == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(ImmutableMap.of("error", "HttpServletRequest is null. Timeline could not be created."))
					.build();
		}
		if (filterSettings == null || filterSettings.getProjectKey() == null
				|| filterSettings.getProjectKey().equals("")) {
			return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Project Key is not valid."))
					.build();
		}
		ApplicationUser user = AuthenticationManager.getUser(request);
		VisDataProvider visDataProvider = new VisDataProvider(user, filterSettings);
		VisTimeLine timeLine = visDataProvider.getTimeLine();
		return Response.ok(timeLine).build();

    }

    @Path("/getTreant")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getTreant(@QueryParam("elementKey") String elementKey,
                              @QueryParam("depthOfTree") String depthOfTree, @QueryParam("searchTerm") String searchTerm,
                              @Context HttpServletRequest request) {

        if (elementKey == null) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(ImmutableMap.of("error", "Treant cannot be shown since element key is invalid.")).build();
        }
        String projectKey = getProjectKey(elementKey);
        Response checkIfProjectKeyIsValidResponse = checkIfProjectKeyIsValid(projectKey);
        if (checkIfProjectKeyIsValidResponse.getStatus() != Status.OK.getStatusCode()) {
            return checkIfProjectKeyIsValidResponse;
        }
        int depth = 4; // default value
        try {
            depth = Integer.parseInt(depthOfTree);
        } catch (NumberFormatException e) {
            LOGGER.error(
                    "Depth of tree could not be parsed, the default value of 4 is used. Message: " + e.getMessage());
            return Response.status(Status.BAD_REQUEST)
                    .entity(ImmutableMap.of("error", "Treant cannot be shown since depth of Tree is NaN")).build();
        }
        ApplicationUser user = AuthenticationManager.getUser(request);
        Treant treant = new Treant(projectKey, elementKey, depth, searchTerm, user);
        return Response.ok(treant).build();
    }

    private Issue getIssue(String issueKey) {
        Issue issue = null;
        if (issueKey == null || issueKey.trim().equals(""))
            return null;
        try {
            issue = ComponentAccessor.getIssueManager().getIssueObject(issueKey);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
        }
        return issue;
    }

	@Path("/getVis")
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getVis(@Context HttpServletRequest request, FilterSettings filterSettings,
			@QueryParam("elementKey") String elementKey) {
		if (checkIfElementIsValid(elementKey).getStatus() != Status.OK.getStatusCode()) {
			return checkIfElementIsValid(elementKey);
		}
		if (filterSettings == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(ImmutableMap.of("error", "The filter settings are null. Vis graph could not be created."))
					.build();
		}
		if (request == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(ImmutableMap.of("error", "HttpServletRequest is null. Vis graph could not be created."))
					.build();
		}
		String projectKey = filterSettings.getProjectKey();
		ApplicationUser user = AuthenticationManager.getUser(request);
		VisDataProvider visDataProvider;
		if (filterSettings.getNamesOfSelectedJiraIssueTypes().size() == 0
				|| (filterSettings.getDocumentationLocations().size() == 1
						&& filterSettings.getDocumentationLocations().get(0).equals(DocumentationLocation.UNKNOWN))) {
			visDataProvider = new VisDataProvider(projectKey, elementKey, filterSettings.getSearchString(), user);
		} else {
			visDataProvider = new VisDataProvider(elementKey, user, filterSettings);
		}
		VisGraph visGraph = visDataProvider.getVisGraph();
		return Response.ok(visGraph).build();
	}

	@Path("/getCompareVis")
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getCompareVis(@Context HttpServletRequest request, FilterSettings filterSettings) {
		if (request == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(ImmutableMap.of("error", "HttpServletRequest is null. Vis graph could not be created."))
					.build();
		}
		if (filterSettings == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(ImmutableMap.of("error", "The filter settings are null. Vis graph could not be created."))
					.build();
		}
		ApplicationUser user = AuthenticationManager.getUser(request);
		VisDataProvider visDataProvider = new VisDataProvider(user, filterSettings);
		return Response.ok(visDataProvider.getVisGraph()).build();
	}

    @Path("/getFilterSettings")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getFilterSettings(@Context HttpServletRequest request, @QueryParam("searchTerm") String searchTerm,
                                      @QueryParam("elementKey") String elementKey) {
        if (checkIfElementIsValid(elementKey).getStatus() != Status.OK.getStatusCode()) {
            return checkIfElementIsValid(elementKey);
        }
        ApplicationUser user = AuthenticationManager.getUser(request);
        String projectKey = getProjectKey(elementKey);
        // FilterDataProvider filterDataProvider = new FilterDataProvider(projectKey,
        // searchTerm, user);
        return Response.ok(new FilterSettingsImpl(projectKey, searchTerm, user)).build();
    }

	@Path("/getDecisionMatrix")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getDecisionMatrix(@Context HttpServletRequest request,
			@QueryParam("projectKey") String projectKey) {
		Response checkIfProjectKeyIsValidResponse = checkIfProjectKeyIsValid(projectKey);
		if (checkIfProjectKeyIsValidResponse.getStatus() != Status.OK.getStatusCode()) {
			return checkIfProjectKeyIsValidResponse;
		}
		List<DecisionKnowledgeElement> decisions = getAllDecisions(projectKey);
		Matrix matrix = new Matrix(projectKey, decisions);
		return Response.ok(matrix).build();
	}

	@Path("/getDecisionGraph")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getDecisionGraph(@Context HttpServletRequest request, @QueryParam("projectKey") String projectKey) {
		Response checkIfProjectKeyIsValidResponse = checkIfProjectKeyIsValid(projectKey);
		if (checkIfProjectKeyIsValidResponse.getStatus() != Status.OK.getStatusCode()) {
			return checkIfProjectKeyIsValidResponse;
		}
		List<DecisionKnowledgeElement> decisions = getAllDecisions(projectKey);
		VisGraph graph = new VisGraph(decisions, projectKey);
		return Response.ok(graph).build();
	}

	private List<DecisionKnowledgeElement> getAllDecisions(String projectKey) {
		return KnowledgePersistenceManager.getOrCreate(projectKey).getDecisionKnowledgeElements(KnowledgeType.DECISION);
	}

    private String getProjectKey(String elementKey) {
        return elementKey.split("-")[0];
    }

    private Response checkIfProjectKeyIsValid(String projectKey) {
        if (projectKey == null) {
            return projectKeyIsInvalid();
        }
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        Project project = projectManager.getProjectObjByKey(projectKey);
        if (project == null) {
            return projectKeyIsInvalid();
        }
        return Response.status(Status.OK).build();
    }

    private Response checkIfElementIsValid(String elementKey) {
        if (elementKey == null) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(ImmutableMap.of("error", "Visualization cannot be shown since element key is invalid."))
                    .build();
        }
        String projectKey = getProjectKey(elementKey);
        Response checkIfProjectKeyIsValidResponse = checkIfProjectKeyIsValid(projectKey);
        if (checkIfProjectKeyIsValidResponse.getStatus() != Status.OK.getStatusCode()) {
            return checkIfProjectKeyIsValidResponse;
        }
        return Response.status(Status.OK).build();
    }

    private Response projectKeyIsInvalid() {
        LOGGER.error("Decision knowledge elements cannot be shown since project key is invalid.");
        return Response.status(Status.BAD_REQUEST).entity(
                ImmutableMap.of("error", "Decision knowledge elements cannot be shown since project key is invalid."))
                .build();
    }

    private Response issueKeyIsInvalid() {
        String msg = "Decision knowledge elements cannot be shown" + " since issue key is invalid.";
        LOGGER.error(msg);
        return Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", msg)).build();
    }

}