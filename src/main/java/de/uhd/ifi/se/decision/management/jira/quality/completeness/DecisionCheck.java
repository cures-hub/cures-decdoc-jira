package de.uhd.ifi.se.decision.management.jira.quality.completeness;

import java.util.ArrayList;
import java.util.List;

import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.persistence.ConfigPersistenceManager;

public class DecisionCheck implements KnowledgeElementCheck {

	private KnowledgeElement decision;
	private String projectKey;

	@Override
	public boolean execute(KnowledgeElement decision) {
		this.decision = decision;
		projectKey = decision.getProject().getProjectKey();
		DefinitionOfDone definitionOfDone = ConfigPersistenceManager.getDefinitionOfDone(projectKey);
		return isCompleteAccordingToDefault() && isCompleteAccordingToSettings(definitionOfDone);
	}

	@Override
	public boolean isCompleteAccordingToDefault() {
		return decision.hasNeighborOfType(KnowledgeType.ISSUE);
	}

	@Override
	public boolean isCompleteAccordingToSettings(DefinitionOfDone definitionOfDone) {
		boolean hasToBeLinkedToProArgument = definitionOfDone.isDecisionIsLinkedToPro();
		if (hasToBeLinkedToProArgument) {
			return decision.hasNeighborOfType(KnowledgeType.PRO);
		}
		return true;
	}

	@Override
	public List<QualityProblem> getQualityProblems(KnowledgeElement decision, DefinitionOfDone definitionOfDone) {
		List<QualityProblem> qualityProblems = new ArrayList<>();

		if (!decision.hasNeighborOfType(KnowledgeType.ISSUE)) {
			qualityProblems.add(QualityProblem.DECISION_DOESNT_HAVE_ISSUE);
		}

		boolean hasToBeLinkedToProArgument = definitionOfDone.isDecisionIsLinkedToPro();
		if (hasToBeLinkedToProArgument && !decision.hasNeighborOfType(KnowledgeType.PRO)) {
			qualityProblems.add(QualityProblem.DECISION_DOESNT_HAVE_PRO);
		}

		return qualityProblems;
	}

}
