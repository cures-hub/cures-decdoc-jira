package de.uhd.ifi.se.decision.management.jira.quality.completeness;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.uhd.ifi.se.decision.management.jira.filtering.FilterSettings;
import de.uhd.ifi.se.decision.management.jira.git.model.ChangedFile;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.Link;
import de.uhd.ifi.se.decision.management.jira.persistence.ConfigPersistenceManager;
import de.uhd.ifi.se.decision.management.jira.recommendation.decisionguidance.ElementRecommendation;

public final class DefinitionOfDoneChecker {

	private static final Map<KnowledgeType, KnowledgeElementCheck> knowledgeElementCheckMap = Map.ofEntries(
			entry(KnowledgeType.DECISION, new DecisionCheck()), //
			entry(KnowledgeType.ISSUE, new IssueCheck()), //
			entry(KnowledgeType.ALTERNATIVE, new AlternativeCheck()), //
			entry(KnowledgeType.ARGUMENT, new ArgumentCheck()), //
			entry(KnowledgeType.PRO, new ArgumentCheck()), //
			entry(KnowledgeType.CON, new ArgumentCheck()), //
			entry(KnowledgeType.CODE, new CodeCheck()));

	private DefinitionOfDoneChecker() {
	}

	/**
	 * Checks if the definition of done has been violated for a {@link KnowledgeElement}.
	 *
	 * @issue Should knowledge elements without definition of done be assumed to be
	 *        complete or incomplete?
	 * @decision If no definition of done can be found, the knowledge element is
	 *           assumed to be complete!
	 * 
	 * @param knowledgeElement
	 *            instance of {@link KnowledgeElement}.
	 * @return true if the element is completely documented according to the default
	 *         and configured rules of the {@link DefinitionOfDone}.
	 */
	public static boolean checkDefinitionOfDone(KnowledgeElement knowledgeElement, FilterSettings filterSettings) {
		return !hasIncompleteKnowledgeLinked(knowledgeElement)
				&& !doesNotHaveMinimumCoverage(knowledgeElement, KnowledgeType.DECISION, filterSettings);
	}

	/**
	 * Checks if the documentation of this {@link KnowledgeElement} is incomplete.
	 *
	 * @return true if this knowledge element is incompletely documented,
	 *         else it returns false.
	 */
	public static boolean isIncomplete(KnowledgeElement knowledgeElement) {
		if (knowledgeElement instanceof ElementRecommendation) {
			return false;
		}
		KnowledgeElementCheck knowledgeElementCheck = knowledgeElementCheckMap.get(knowledgeElement.getType());
		return !(knowledgeElementCheck == null || knowledgeElementCheck.execute(knowledgeElement));
	}

	/**
	 * Iterates recursively over the knowledge graph of the
	 * {@link KnowledgeElement}.
	 * 
	 * @return true if there is at least one incompletely documented knowledge
	 *         element, else it returns false.
	 */
	public static boolean hasIncompleteKnowledgeLinked(KnowledgeElement knowledgeElement) {
		if (isIncomplete(knowledgeElement)) {
			return true;
		}
		for (Link link : knowledgeElement.getLinks()) {
			KnowledgeElement oppositeElement = link.getOppositeElement(knowledgeElement);
			if (isIncomplete(oppositeElement)) {
				return true;
			} else if (hasIncompleteKnowledgeLinked(oppositeElement, knowledgeElement)) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasIncompleteKnowledgeLinked(KnowledgeElement oppositeElement,
			KnowledgeElement knowledgeElement) {
		for (Link link : oppositeElement.getLinks()) {
			KnowledgeElement nextElement = link.getOppositeElement(oppositeElement);
			if (nextElement.getId() == knowledgeElement.getId()) {
				continue;
			} else if (isIncomplete(nextElement)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Iterates recursively over the knowledge graph of the
	 * {@link KnowledgeElement} and checks if it fulfills the minimum coverage.
	 * {@link ChangedFile} that are test files or with less lines of codes
	 * than defined in the {@link DefinitionOfDone} don't require any coverage.
	 *
	 * @return true if there are at least as many elements of the specified
	 *         {@link KnowledgeType} as the minimum coverage demands, else it
	 *         returns false.
	 */
	public static boolean doesNotHaveMinimumCoverage(KnowledgeElement knowledgeElement, KnowledgeType knowledgeType,
			FilterSettings filterSettings) {
		if (!checkIfCodeFileRequiresCoverage(knowledgeElement, filterSettings)) {
			return false;
		}

		int linkDistance = filterSettings.getDefinitionOfDone().getMaximumLinkDistanceToDecisions();
		int minimumCoverage = filterSettings.getDefinitionOfDone().getMinimumDecisionsWithinLinkDistance();
		Set<KnowledgeElement> linkedElements = knowledgeElement.getLinkedElements(linkDistance);
		for (KnowledgeElement linkedElement : linkedElements) {
			if (linkedElement.getType() == knowledgeType) {
				minimumCoverage--;
			}
			if (minimumCoverage <= 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Iterates recursively over the knowledge graph of the
	 * {@link KnowledgeElement} and checks if it has any coverage.
	 * {@link ChangedFile} that are test files or with less lines of codes
	 * than defined in the {@link DefinitionOfDone} don't require any coverage.
	 *
	 * @return true if there is at least one element of the specified
	 *         {@link KnowledgeType}, else it returns false.
	 */
	public static boolean hasNoCoverage(KnowledgeElement knowledgeElement, KnowledgeType knowledgeType,
			FilterSettings filterSettings) {
		if (!checkIfCodeFileRequiresCoverage(knowledgeElement, filterSettings)) {
			return false;
		}

		int linkDistance = filterSettings.getDefinitionOfDone().getMaximumLinkDistanceToDecisions();
		Set<KnowledgeElement> linkedElements = knowledgeElement.getLinkedElements(linkDistance);
		for (KnowledgeElement linkedElement : linkedElements) {
			if (linkedElement.getType() == knowledgeType) {
				return false;
			}
		}
		return true;
	}

	private static boolean checkIfCodeFileRequiresCoverage(KnowledgeElement knowledgeElement, FilterSettings filterSettings) {
		if (knowledgeElement instanceof ChangedFile) {
			int lineNumbersInCodeFile = filterSettings.getDefinitionOfDone().getLineNumbersInCodeFile();
			ChangedFile codeFile = (ChangedFile) knowledgeElement;
			return codeFile.getLineCount() >= lineNumbersInCodeFile && !codeFile.isTestCodeFile();
		}
		return true;
	}

	/**
	 * @return a list of {@link QualityProblem} of the {@link KnowledgeElement}.
	 */
	public static List<QualityProblem> getQualityProblems(KnowledgeElement knowledgeElement,
			FilterSettings filterSettings) {
		List<QualityProblem> qualityProblems = new ArrayList<>();

		if (DefinitionOfDoneChecker.doesNotHaveMinimumCoverage(knowledgeElement, KnowledgeType.DECISION,
				filterSettings)) {
			if (DefinitionOfDoneChecker.hasNoCoverage(knowledgeElement, KnowledgeType.DECISION, filterSettings)) {
				qualityProblems.add(QualityProblem.NO_DECISION_COVERAGE);
			} else {
				qualityProblems.add(QualityProblem.DECISION_COVERAGE_TOO_LOW);
			}
		}

		if (DefinitionOfDoneChecker.hasIncompleteKnowledgeLinked(knowledgeElement)) {
			qualityProblems.add(QualityProblem.INCOMPLETE_KNOWLEDGE_LINKED);
		}

		if (knowledgeElement.getType().isDecisionKnowledge()) {
			DefinitionOfDone definitionOfDone = ConfigPersistenceManager
					.getDefinitionOfDone(knowledgeElement.getProject().getProjectKey());
			KnowledgeElementCheck knowledgeElementCheck = knowledgeElementCheckMap.get(knowledgeElement.getType());
			qualityProblems.addAll(knowledgeElementCheck.getQualityProblems(knowledgeElement, definitionOfDone));
		}
		return qualityProblems;
	}

	/**
	 * @return a string detailing all {@link QualityProblem} of the
	 *         {@link KnowledgeElement}.
	 */
	public static String getQualityProblemExplanation(KnowledgeElement knowledgeElement,
			FilterSettings filterSettings) {
		List<QualityProblem> qualityProblems = getQualityProblems(knowledgeElement, filterSettings);
		StringBuilder text = new StringBuilder();
		for (QualityProblem problem : qualityProblems) {
			if (problem.equals(QualityProblem.NO_DECISION_COVERAGE)) {
				text.append(problem.getDescription()).append(System.lineSeparator()).append(System.lineSeparator());
			} else if (problem.equals(QualityProblem.DECISION_COVERAGE_TOO_LOW)) {
				text.append(problem.getDescription()).append(System.lineSeparator()).append(System.lineSeparator());
			} else if (problem.equals(QualityProblem.INCOMPLETE_KNOWLEDGE_LINKED)) {
				text.append(problem.getDescription()).append(System.lineSeparator()).append(System.lineSeparator());
			} else {
				text.append(problem.getDescription()).append(System.lineSeparator());
			}
		}
		return text.toString().strip();
	}

	/**
	 * @return an ArrayNode of ObjectNodes detailing all {@link QualityProblem} of
	 *         the {@link KnowledgeElement}.
	 */
	public static ArrayNode getQualityProblemsAsJson(KnowledgeElement knowledgeElement, FilterSettings filterSettings) {
		List<QualityProblem> qualityProblems = getQualityProblems(knowledgeElement, filterSettings);
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode qualityProblemsJson = mapper.createArrayNode();
		for (QualityProblem problem : qualityProblems) {
			qualityProblemsJson.add(problem.getJson());
		}

		return qualityProblemsJson;
	}
}
