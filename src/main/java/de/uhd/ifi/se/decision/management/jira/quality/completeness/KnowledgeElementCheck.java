package de.uhd.ifi.se.decision.management.jira.quality.completeness;

import java.util.List;

import de.uhd.ifi.se.decision.management.jira.git.model.ChangedFile;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeGraph;
import de.uhd.ifi.se.decision.management.jira.model.PartOfJiraIssueText;
import de.uhd.ifi.se.decision.management.jira.recommendation.Recommendation;

/**
 * Checks whether a given {@link KnowledgeElement} is documented completely
 * according to the {@link DefinitionOfDone} (DoD).
 *
 * For example, an argument needs to be linked to at least one solution option
 * (decision or alternative) in the {@link KnowledgeGraph}. Otherwise, it is
 * incomplete, i.e., its documentation needs to be improved.
 */
public interface KnowledgeElementCheck {

	/**
	 * Executes the completeness check for the given knowledge element.
	 * 
	 * @param knowledgeElement
	 *            instance of {@link KnowledgeElement} or of a subclass, e.g.
	 *            {@link ChangedFile}, {@link PartOfJiraIssueText}, or
	 *            {@link Recommendation}.
	 * @return true if the element is completely documented according to the default
	 *         and configured rules of the {@link DefinitionOfDone}.
	 */
	boolean execute(KnowledgeElement knowledgeElement);

	/**
	 * Checks the default rules that a knowledge element needs to fulfill to be
	 * completely documented. For example, a default rule is that each decision
	 * problem (=issue) needs to be linked to a decision to be complete.
	 * 
	 * @return true if all defaults rules are fulfilled.
	 */
	boolean isCompleteAccordingToDefault();

	/**
	 * Checks the configurable rules that a knowledge element needs to fulfill to be
	 * completely documented according to the {@link DefinitionOfDone}. For example,
	 * a configurable rule is that each decision needs to be linked to at least one
	 * pro-argument to be complete.
	 * 
	 * @param definitionOfDone
	 *            instance of {@link DefinitionOfDone}.
	 * 
	 * @return true if all configured rules are fulfilled.
	 */
	boolean isCompleteAccordingToSettings(DefinitionOfDone definitionOfDone);

	/**
	 * Executes the completeness check for the given knowledge element and return a
	 * set of quality problems.
	 *
	 * @param knowledgeElement
	 *            instance of {@link KnowledgeElement} or of a subclass, e.g.
	 *            {@link ChangedFile}, {@link PartOfJiraIssueText}, or
	 *            {@link Recommendation}.
	 * @param definitionOfDone
	 *            instance of {@link DefinitionOfDone}.
	 * @return a set of {@link QualityProblem} according to the default and
	 *         configured rules of the {@link DefinitionOfDone}.
	 */
	List<QualityProblem> getQualityProblems(KnowledgeElement knowledgeElement, DefinitionOfDone definitionOfDone);
}
