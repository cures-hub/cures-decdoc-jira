package de.uhd.ifi.se.decision.management.jira.quality.completeness;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uhd.ifi.se.decision.management.jira.filtering.FilterSettings;
import de.uhd.ifi.se.decision.management.jira.filtering.FilteringManager;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeGraph;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;

/**
 * Calculates metrics for the intra-rationale completeness.
 */
public class RationaleCompletenessCalculator {

	@JsonIgnore
	private FilterSettings filterSettings;

	private Map<String, String> issuesSolvedByDecision;
	private Map<String, String> decisionsSolvingIssues;
	private Map<String, String> proArgumentDocumentedForAlternative;
	private Map<String, String> conArgumentDocumentedForAlternative;
	private Map<String, String> proArgumentDocumentedForDecision;
	private Map<String, String> conArgumentDocumentedForDecision;

	@JsonIgnore
	protected static final Logger LOGGER = LoggerFactory.getLogger(RationaleCompletenessCalculator.class);

	public RationaleCompletenessCalculator(FilterSettings filterSettings) {
		this.filterSettings = filterSettings;

		this.issuesSolvedByDecision = calculateElementsWithNeighborsOfOtherType(KnowledgeType.ISSUE,
				KnowledgeType.DECISION);
		this.decisionsSolvingIssues = calculateElementsWithNeighborsOfOtherType(KnowledgeType.DECISION,
				KnowledgeType.ISSUE);
		this.proArgumentDocumentedForAlternative = calculateElementsWithNeighborsOfOtherType(KnowledgeType.ALTERNATIVE,
				KnowledgeType.PRO);
		this.conArgumentDocumentedForAlternative = calculateElementsWithNeighborsOfOtherType(KnowledgeType.ALTERNATIVE,
				KnowledgeType.CON);
		this.proArgumentDocumentedForDecision = calculateElementsWithNeighborsOfOtherType(KnowledgeType.DECISION,
				KnowledgeType.PRO);
		this.conArgumentDocumentedForDecision = calculateElementsWithNeighborsOfOtherType(KnowledgeType.DECISION,
				KnowledgeType.CON);
	}

	private Map<String, String> calculateElementsWithNeighborsOfOtherType(KnowledgeType sourceElementType,
			KnowledgeType targetElementType) {
		LOGGER.info("RequirementsDashboard getElementsWithNeighborsOfOtherType");

		KnowledgeGraph graph = new FilteringManager(filterSettings).getFilteredGraph();
		List<KnowledgeElement> allSourceElements = graph.getElements(sourceElementType);
		StringBuilder sourceElementsWithTargetTypeLinked = new StringBuilder();
		StringBuilder sourceElementsWithoutTargetTypeLinked = new StringBuilder();

		for (KnowledgeElement sourceElement : allSourceElements) {
			if (sourceElement.hasNeighborOfType(targetElementType)) {
				sourceElementsWithTargetTypeLinked.append(sourceElement.getKey()).append(" ");
			} else {
				sourceElementsWithoutTargetTypeLinked.append(sourceElement.getKey()).append(" ");
			}
		}

		Map<String, String> havingLinkMap = new LinkedHashMap<>();
		havingLinkMap.put(sourceElementType + " has " + targetElementType,
				sourceElementsWithTargetTypeLinked.toString().trim());
		havingLinkMap.put(sourceElementType + " has no " + targetElementType,
				sourceElementsWithoutTargetTypeLinked.toString().trim());
		return havingLinkMap;
	}

	@JsonProperty("issuesSolvedByDecision")
	public Map<String, String> getIssuesSolvedByDecision() {
		return issuesSolvedByDecision;
	}

	@JsonProperty("decisionsSolvingIssues")
	public Map<String, String> getDecisionsSolvingIssues() {
		return decisionsSolvingIssues;
	}

	@JsonProperty("proArgumentDocumentedForAlternative")
	public Map<String, String> getProArgumentDocumentedForAlternative() {
		return proArgumentDocumentedForAlternative;
	}

	@JsonProperty("conArgumentDocumentedForAlternative")
	public Map<String, String> getConArgumentDocumentedForAlternative() {
		return conArgumentDocumentedForAlternative;
	}

	@JsonProperty("proArgumentDocumentedForDecision")
	public Map<String, String> getProArgumentDocumentedForDecision() {
		return proArgumentDocumentedForDecision;
	}

	@JsonProperty("conArgumentDocumentedForDecision")
	public Map<String, String> getConArgumentDocumentedForDecision() {
		return conArgumentDocumentedForDecision;
	}
}
