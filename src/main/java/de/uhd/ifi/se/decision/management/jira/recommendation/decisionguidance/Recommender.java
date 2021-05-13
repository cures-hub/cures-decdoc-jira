package de.uhd.ifi.se.decision.management.jira.recommendation.decisionguidance;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.atlassian.jira.user.ApplicationUser;

import de.uhd.ifi.se.decision.management.jira.model.DocumentationLocation;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeGraph;
import de.uhd.ifi.se.decision.management.jira.persistence.ConfigPersistenceManager;
import de.uhd.ifi.se.decision.management.jira.persistence.KnowledgePersistenceManager;
import de.uhd.ifi.se.decision.management.jira.recommendation.decisionguidance.projectsource.ProjectSource;
import de.uhd.ifi.se.decision.management.jira.recommendation.decisionguidance.projectsource.ProjectSourceRecommender;
import de.uhd.ifi.se.decision.management.jira.recommendation.decisionguidance.rdfsource.RDFSource;
import de.uhd.ifi.se.decision.management.jira.recommendation.decisionguidance.rdfsource.RDFSourceRecommender;

/**
 * Takes the input from the UI and passes it to the knowledge sources.
 */
public abstract class Recommender<T extends KnowledgeSource> {

	protected String projectKey;
	protected T knowledgeSource;

	/**
	 * @param projectKey
	 *            of the current project (not of the external knowledge source).
	 * @param knowledgeSource
	 *            {@link KnowledgeSource}, either {@link RDFSource} or
	 *            {@link ProjectSource}.
	 */
	protected Recommender(String projectKey, T knowledgeSource) {
		this.knowledgeSource = knowledgeSource;
		this.projectKey = projectKey;
	}

	/**
	 * @param projectKey
	 *            of a Jira project.
	 * @param knowledgeSource
	 *            object of either {@link RDFSource} or {@link ProjectSource}.
	 * @return concrete Recommender, either {@link RDFSourceRecommender} or
	 *         {@link ProjectSourceRecommender}.
	 */
	public static Recommender<?> getRecommenderForKnowledgeSource(String projectKey, KnowledgeSource knowledgeSource) {
		if (knowledgeSource instanceof ProjectSource) {
			return new ProjectSourceRecommender(projectKey, (ProjectSource) knowledgeSource);
		}
		return new RDFSourceRecommender(projectKey, (RDFSource) knowledgeSource);
	}

	/**
	 * @param keywords
	 *            used to query the {@link KnowledgeSource} (either
	 *            {@link RDFSource} or {@link ProjectSource}).
	 * @return list of {@link ElementRecommendation}s matching the keywords.
	 */
	public abstract List<ElementRecommendation> getRecommendations(String keywords);

	public List<ElementRecommendation> getRecommendations(KnowledgeElement decisionProblem) {
		if (decisionProblem == null) {
			return new ArrayList<>();
		}
		List<ElementRecommendation> recommendations = new ArrayList<>();
		for (KnowledgeElement linkedElement : decisionProblem.getLinkedSolutionOptions()) {
			List<ElementRecommendation> recommendationFromAlternative = getRecommendations(linkedElement.getSummary());
			recommendations.addAll(recommendationFromAlternative);
		}
		return recommendations.stream().distinct().collect(Collectors.toList());
	}

	/**
	 * @param keywords
	 * @param decisionProblem
	 * @return list of {@link ElementRecommendation}s matching the keywords.
	 */
	public List<ElementRecommendation> getRecommendations(String keywords, KnowledgeElement decisionProblem) {
		List<ElementRecommendation> recommendations = new ArrayList<>();
		recommendations.addAll(getRecommendations(decisionProblem));
		if (!keywords.equalsIgnoreCase(decisionProblem.getSummary())) {
			recommendations.addAll(getRecommendations(keywords));
		}
		return recommendations.stream().distinct().collect(Collectors.toList());
	}

	public static List<ElementRecommendation> getAllRecommendations(String projectKey, KnowledgeElement decisionProblem,
			String keywords) {
		DecisionGuidanceConfiguration config = ConfigPersistenceManager.getDecisionGuidanceConfiguration(projectKey);
		List<KnowledgeSource> knowledgeSources = config.getAllActivatedKnowledgeSources();
		return getAllRecommendations(projectKey, knowledgeSources, decisionProblem, keywords);
	}

	public static List<ElementRecommendation> getAllRecommendations(String projectKey, List<KnowledgeSource> knowledgeSources,
			KnowledgeElement decisionProblem, String keywords) {
		List<ElementRecommendation> recommendations = new ArrayList<>();
		for (KnowledgeSource knowledgeSource : knowledgeSources) {
			Recommender<?> recommender = Recommender.getRecommenderForKnowledgeSource(projectKey, knowledgeSource);
			recommendations.addAll(recommender.getRecommendations(keywords, decisionProblem));
		}
		return recommendations.stream().distinct().collect(Collectors.toList());
	}

	/**
	 * Adds all recommendation to the knowledge graph with the status "recommended".
	 * The recommendations will be appended to the root element
	 *
	 * @param decisionProblem
	 *            to which the recommended solution options should be linked in the
	 *            {@link KnowledgeGraph}.
	 * @param user
	 *            authenticated Jira {@link ApplicationUser}.
	 * @param projectKey
	 *            of a Jira project.
	 * @param recommendations
	 *            list of recommended solution options ({@link ElementRecommendation}s)
	 *            that should be linked in the {@link KnowledgeGraph}.
	 */
	public static void addToKnowledgeGraph(KnowledgeElement decisionProblem, ApplicationUser user,
			List<ElementRecommendation> recommendations) {
		String projectKey = decisionProblem.getProject().getProjectKey();
		KnowledgePersistenceManager manager = KnowledgePersistenceManager.getOrCreate(projectKey);
		for (ElementRecommendation recommendation : recommendations) {
			recommendation.setProject(projectKey);
			recommendation.setDocumentationLocation(DocumentationLocation.JIRAISSUETEXT);
			KnowledgeElement insertedElement = manager.insertKnowledgeElement(recommendation, user, decisionProblem);
			manager.insertLink(decisionProblem, insertedElement, user);
		}
	}

	public String getProjectKey() {
		return projectKey;
	}

	/**
	 * @return either {@link RDFSource} or {@link ProjectSource}.
	 */
	public T getKnowledgeSource() {
		return knowledgeSource;
	}

	/**
	 * @param knowledgeSource
	 *            either {@link RDFSource} or {@link ProjectSource}.
	 */
	public void setKnowledgeSource(T knowledgeSource) {
		this.knowledgeSource = knowledgeSource;
	}
}