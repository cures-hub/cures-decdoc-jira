package de.uhd.ifi.se.decision.management.jira.quality.completeness;

import static java.util.Map.entry;

import java.util.Map;

import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeGraph;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;

/**
 * Checks whether a given {@link KnowledgeElement} is documented completely
 * according to the definition of done.
 * 
 * For example, an argument needs to be linked to at least one solution option
 * (decision or alternative) in the {@link KnowledgeGraph}. Otherwise, it is
 * incomplete, i.e., its documentation needs to be improved.
 */
public class KnowledgeCompletenessChecker {

	private static final Map<KnowledgeType, CompletenessCheck> completenessCheckMap = Map.ofEntries(
			entry(KnowledgeType.DECISION, new DecisionCompletenessCheck()),
			entry(KnowledgeType.ISSUE, new IssueCompletenessCheck()),
			entry(KnowledgeType.ALTERNATIVE, new AlternativeCompletenessCheck()));

	public static boolean isElementComplete(KnowledgeElement element) {
		CompletenessCheck completenessCheck = completenessCheckMap.get(element.getType());
		return completenessCheck != null && completenessCheck.execute(element);
	}
}