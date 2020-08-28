package de.uhd.ifi.se.decision.management.jira.quality.completeness;

import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;

import java.util.Map;

import static java.util.Map.entry;

public class CompletionHandler {

	private final Map<KnowledgeType, CompletionCheck> completenessCheckMap = Map.ofEntries(
		entry(KnowledgeType.DECISION, new DecisionKnowledgeElementCompletenessCheck()),
		entry(KnowledgeType.ISSUE, new IssueKnowledgeElementCompletenessCheck()),
		entry(KnowledgeType.ALTERNATIVE, new AlternativeKnowledgeElementCompletenessCheck()),
		entry(KnowledgeType.ARGUMENT, new ArgumentKnowledgeElementCompletenessCheck()),
		entry(KnowledgeType.PRO, new ArgumentKnowledgeElementCompletenessCheck()),
		entry(KnowledgeType.CON, new ArgumentKnowledgeElementCompletenessCheck()));


	/**
	 * @issue Should knowledge elements without definition of done be assumed to be
	 *        complete or incomplete?
	 * @decision If no definition of done can be found, the knowledge element is
	 *           assumed to be complete!
	 */
	public boolean checkForCompletion(KnowledgeElement knowledgeElement) {
		CompletionCheck completionCheck = completenessCheckMap.get(knowledgeElement.getType());
		return completionCheck == null || completionCheck.execute(knowledgeElement);
	}

}
