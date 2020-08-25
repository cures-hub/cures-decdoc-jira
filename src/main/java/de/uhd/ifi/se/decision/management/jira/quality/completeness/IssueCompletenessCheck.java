package de.uhd.ifi.se.decision.management.jira.quality.completeness;

import java.util.List;

import org.jgrapht.Graphs;

import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeGraph;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;

public class IssueCompletenessCheck implements CompletenessCheck {

	@Override
	public boolean execute(KnowledgeElement issue) {
		KnowledgeGraph graph = KnowledgeGraph.getOrCreate(issue.getProject());
		List<KnowledgeElement> neighbours = Graphs.neighborListOf(graph, issue);
		for (KnowledgeElement knowledgeElement : neighbours) {
			if (knowledgeElement.getType() == KnowledgeType.DECISION) {
				return true;
			}
		}
		return false;
	}
}
