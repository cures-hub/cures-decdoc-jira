package de.uhd.ifi.se.decision.management.jira.rest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.ImmutableMap;
import de.uhd.ifi.se.decision.management.jira.classification.ClassificationManagerForJiraIssueText;
import de.uhd.ifi.se.decision.management.jira.classification.ClassifierType;
import de.uhd.ifi.se.decision.management.jira.classification.TextClassificationConfiguration;
import de.uhd.ifi.se.decision.management.jira.classification.TextClassifier;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeProject;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.PartOfJiraIssueText;
import de.uhd.ifi.se.decision.management.jira.persistence.ConfigPersistenceManager;
import de.uhd.ifi.se.decision.management.jira.persistence.singlelocations.JiraIssuePersistenceManager;
import de.uhd.ifi.se.decision.management.jira.persistence.singlelocations.JiraIssueTextPersistenceManager;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.validation.ClassificationMetrics;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST resource for text classification and its configuration.
 */
@Path("/classification")
public class TextClassificationRest {
	private static final Logger LOGGER = LoggerFactory.getLogger(TextClassificationRest.class);

	@Path("/setTextClassifierEnabled")
	@POST
	public Response setTextClassifierEnabled(@Context HttpServletRequest request,
			@QueryParam("projectKey") String projectKey, @QueryParam("isTextClassifierEnabled") boolean isActivated) {
		Response checkIfProjectKeyIsValidResponse = RestParameterChecker.checkIfProjectKeyIsValid(projectKey);
		if (checkIfProjectKeyIsValidResponse.getStatus() != Status.OK.getStatusCode()) {
			return checkIfProjectKeyIsValidResponse;
		}
		ConfigPersistenceManager.setTextClassifierActivated(projectKey, isActivated);
		return Response.ok().build();
	}

	@Path("/useTrainedClassifier")
	@POST
	public Response useTrainedClassifier(@Context HttpServletRequest request,
			@QueryParam("projectKey") String projectKey, @QueryParam("trainedClassifier") String trainedClassifier,
			@QueryParam("isOnlineLearningActivated") boolean isOnlineLearningActivated) {
		Response isValidDataResponse = RestParameterChecker.checkIfDataIsValid(request, projectKey);
		if (isValidDataResponse.getStatus() != Status.OK.getStatusCode()) {
			return isValidDataResponse;
		}
		if (trainedClassifier == null || trainedClassifier.isEmpty()) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(ImmutableMap.of("error", "The classifier could not be set since the file name is invalid."))
					.build();
		}
		TextClassificationConfiguration config = ConfigPersistenceManager
				.getTextClassificationConfiguration(projectKey);
		config.setSelectedTrainedClassifier(trainedClassifier);
		config.setOnlineLearningActivated(isOnlineLearningActivated);
		ConfigPersistenceManager.saveTextClassificationConfiguration(projectKey, config);
		TextClassifier.getInstance(projectKey).setSelectedTrainedClassifier(trainedClassifier);
		return Response.ok().build();
	}

	@Path("/trainClassifier")
	@POST
	public Response trainClassifier(@Context HttpServletRequest request, @QueryParam("projectKey") String projectKey,
			@QueryParam("trainingFileName") String trainingFileName,
			@QueryParam("binaryClassifierType") String binaryClassifierType,
			@QueryParam("fineGrainedClassifierType") String fineGrainedClassifierType) {
		Response isValidDataResponse = RestParameterChecker.checkIfDataIsValid(request, projectKey);
		if (isValidDataResponse.getStatus() != Status.OK.getStatusCode()) {
			return isValidDataResponse;
		}
		if (trainingFileName == null || trainingFileName.isEmpty()) {
			return Response.status(Response.Status.BAD_REQUEST).entity(ImmutableMap.of("error",
					"The classifier could not be trained since the training file name is invalid.")).build();
		}
		ConfigPersistenceManager.setTrainingFileForClassifier(projectKey, trainingFileName);
		TextClassifier classifier = TextClassifier.getInstance(projectKey);
		if (classifier.train(trainingFileName, ClassifierType.valueOfOrDefault(binaryClassifierType),
				ClassifierType.valueOfOrDefault(fineGrainedClassifierType))) {
			return Response.ok().build();
		}
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity(ImmutableMap.of("error", "The classifier could not be trained.")).build();
	}

	@Path("/evaluateTextClassifier")
	@POST
	public Response evaluateTextClassifier(@Context HttpServletRequest request,
			@QueryParam("projectKey") String projectKey, @QueryParam("trainingFileName") String trainingFileName,
			@QueryParam("numberOfFolds") int numberOfFolds,
			@QueryParam("binaryClassifierType") String binaryClassifierType,
			@QueryParam("fineGrainedClassifierType") String fineGrainedClassifierType) {
		Response isValidDataResponse = RestParameterChecker.checkIfDataIsValid(request, projectKey);
		if (isValidDataResponse.getStatus() != Status.OK.getStatusCode()) {
			return isValidDataResponse;
		}

		TextClassifier classifier = TextClassifier.getInstance(projectKey);
		classifier.setGroundTruthFile(trainingFileName);
		Map<String, ClassificationMetrics> evaluationResults = classifier.evaluate(numberOfFolds,
				ClassifierType.valueOfOrDefault(binaryClassifierType),
				ClassifierType.valueOfOrDefault(fineGrainedClassifierType));
		String evaluationResultsMessage = "Ground truth file name: " + trainingFileName + System.lineSeparator();
		String trainedClassifierName = "Name of trained classifier: " + ConfigPersistenceManager
				.getTextClassificationConfiguration(projectKey).getSelectedTrainedClassifier();
		evaluationResultsMessage += numberOfFolds > 1
				? "Trained and evaluated using " + numberOfFolds + "-fold cross-validation"
				: trainedClassifierName;
		evaluationResultsMessage += System.lineSeparator() + evaluationResults.toString();
		TextClassificationConfiguration config = ConfigPersistenceManager
				.getTextClassificationConfiguration(projectKey);
		config.setLastEvaluationResults(evaluationResultsMessage);
		ConfigPersistenceManager.saveTextClassificationConfiguration(projectKey, config);
		return Response.ok(ImmutableMap.of("content", evaluationResultsMessage)).build();
	}

	@Path("/classifyText")
	@POST
	public Response classifyText(@Context HttpServletRequest request, @QueryParam("projectKey") String projectKey,
			@QueryParam("text") String text) {
		Response isValidDataResponse = RestParameterChecker.checkIfDataIsValid(request, projectKey);
		if (isValidDataResponse.getStatus() != Status.OK.getStatusCode()) {
			return isValidDataResponse;
		}
		TextClassifier classifier = TextClassifier.getInstance(projectKey);
		boolean isRelevant = classifier.getBinaryClassifier().predict(text);
		String classificationResult = isRelevant ? "Relevant" : "Irrelevant";
		if (isRelevant) {
			KnowledgeType type = classifier.getFineGrainedClassifier().predict(text);
			classificationResult += ": " + type.toString();
		}
		return Response.ok(ImmutableMap.of("classificationResult", classificationResult)).build();
	}

	@Path("/saveTrainingFile")
	@POST
	public Response saveTrainingFileForTextClassifier(@Context HttpServletRequest request,
			@QueryParam("projectKey") String projectKey) {
		Response isValidDataResponse = RestParameterChecker.checkIfDataIsValid(request, projectKey);
		if (isValidDataResponse.getStatus() != Status.OK.getStatusCode()) {
			return isValidDataResponse;
		}
		TextClassifier trainer = TextClassifier.getInstance(projectKey);
		File trainingFile = trainer.saveTrainingFile();

		if (trainingFile != null) {
			String fileContent = "";
			try {
				fileContent = FileUtils.readFileToString(trainingFile, StandardCharsets.UTF_8);
			} catch (IOException e) {
				LOGGER.error("Training file content could not be read. " + e.getMessage());
			}
			return Response.ok(ImmutableMap.of("trainingFile", trainingFile.getPath(), "content", fileContent)).build();
		}
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity(ImmutableMap.of("error",
						"Training file for text classifier could not be created because of an internal server error."))
				.build();
	}

	@Path("/classifyWholeProject")
	@POST
	public Response classifyWholeProject(@Context HttpServletRequest request,
			@QueryParam("projectKey") String projectKey) {
		Response isValidDataResponse = RestParameterChecker.checkIfDataIsValid(request, projectKey);
		if (isValidDataResponse.getStatus() != Status.OK.getStatusCode()) {
			return isValidDataResponse;
		}
		TextClassificationConfiguration config = new DecisionKnowledgeProject(projectKey)
			.getTextClassificationConfiguration();
		if (!config.isActivated()) {
			return Response.status(Status.FORBIDDEN)
				.entity(ImmutableMap.of("error", "Automatic classification is disabled for this project.")).build();
		}
		ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
		ClassificationManagerForJiraIssueText classificationManager = new ClassificationManagerForJiraIssueText(
			projectKey);
		for (Issue issue : JiraIssuePersistenceManager.getAllJiraIssuesForProject(user, projectKey)) {
			classificationManager.classifyDescriptionAndAllComments(issue);
		}
		return Response.ok().build();
	}

	@Path("/getNonValidatedElements")
	@GET
	public Response getNonValidatedElements(@Context HttpServletRequest request, @QueryParam("projectKey") String projectKey, @QueryParam("issueKey") String issueKey) {

		if (request == null || projectKey == null || issueKey == null) {
			return Response.status(Response.Status.BAD_REQUEST)
				.entity(ImmutableMap.of("error", "Non-validated elements could not be found due to a bad request."))
				.build();
		}

		Issue jiraIssue = JiraIssuePersistenceManager.getJiraIssue(issueKey);
		long id = jiraIssue.getId();

		JiraIssueTextPersistenceManager manager = new JiraIssueTextPersistenceManager(projectKey);
		List<KnowledgeElement> elements = manager.getElementsInJiraIssue(id);
		List<KnowledgeElement> nonValidatedElements = new ArrayList<KnowledgeElement>();
		for (KnowledgeElement element : elements) {
			PartOfJiraIssueText issueTextPart = (PartOfJiraIssueText) element;
			if (!issueTextPart.isValidated()) {
				nonValidatedElements.add(issueTextPart);
			}
		}
		return Response.ok().entity(ImmutableMap.of("nonValidatedElements", nonValidatedElements)).build();
	}

	/**
	 * if no issue key is provided, gets all the issues
	 * @param request
	 * @param projectKey
	 * @return
	 */
	@Path("/getAllNonValidatedElements")
	@GET
	public Response getAllNonValidatedElements(@Context HttpServletRequest request, @QueryParam("projectKey") String projectKey) {

		if (request == null || projectKey == null) {
			return Response.status(Response.Status.BAD_REQUEST)
				.entity(ImmutableMap.of("error", "Non-validated elements could not be found due to a bad request."))
				.build();
		}
		ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

		List<Issue> issues = JiraIssuePersistenceManager.getAllJiraIssuesForProject(user, projectKey);
		JiraIssueTextPersistenceManager manager = new JiraIssueTextPersistenceManager(projectKey);
		List<KnowledgeElement> nonValidatedElements = new ArrayList<KnowledgeElement>();

		for (Issue issue : issues) {
			List<KnowledgeElement> elements = manager.getElementsInJiraIssue(issue.getId());
			for (KnowledgeElement element : elements) {
				PartOfJiraIssueText issueTextPart = (PartOfJiraIssueText) element;
				if (!issueTextPart.isValidated()) {
					nonValidatedElements.add(issueTextPart);
				}
			}
		}
		return Response.ok().entity(ImmutableMap.of("nonValidatedElements", nonValidatedElements)).build();
	}

	@Path("/validateAllElements")
	@POST
	public Response validateAllElements(@Context HttpServletRequest request, @QueryParam("projectKey") String projectKey, @QueryParam("issueKey") String issueKey) {

		if (request == null || projectKey == null || issueKey == null) {
			return Response.status(Response.Status.BAD_REQUEST)
				.entity(ImmutableMap.of("error", "Elements could not be set to validated due to a bad request."))
				.build();
		}

		Issue jiraIssue = JiraIssuePersistenceManager.getJiraIssue(issueKey);
		long id = jiraIssue.getId();

		JiraIssueTextPersistenceManager manager = new JiraIssueTextPersistenceManager(projectKey);
		List<KnowledgeElement> elements = manager.getElementsInJiraIssue(id);

		for (KnowledgeElement element : elements) {
			PartOfJiraIssueText issueTextPart = (PartOfJiraIssueText) element;
			if (!issueTextPart.isValidated()) {
				issueTextPart.setValidated(true);
				manager.updateInDatabase(issueTextPart);
			}
		}
		return Response.status(Status.OK).build();

	}
}
