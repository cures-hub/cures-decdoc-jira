package de.uhd.ifi.se.decision.management.jira.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.uhd.ifi.se.decision.management.jira.quality.completeness.IssueCheck;

/**
 * Status of decision problems (=issues) and solution options (=decisions and
 * alternatives). Issues can have the state "resolved" or "unresolved".
 */
public enum KnowledgeStatus {
	IDEA, DISCARDED, DECIDED, CHALLENGED, REJECTED, RESOLVED, RECOMMENDED, UNRESOLVED, UNDEFINED;

	/**
	 * Converts a string to a knowledge status.
	 *
	 * @param status
	 *            as a String.
	 * @return knowledge status.
	 */
	public static KnowledgeStatus getKnowledgeStatus(String status) {
		if (status == null || status.isEmpty()) {
			return UNDEFINED;
		}
		for (KnowledgeStatus knowledgeStatus : KnowledgeStatus.values()) {
			if (knowledgeStatus.name().toLowerCase(Locale.ENGLISH).matches(status.toLowerCase(Locale.ENGLISH))) {
				return knowledgeStatus;
			}
		}
		return UNDEFINED;
	}

	/**
	 * @return list of all knowledge status as Strings in lower case.
	 */
	public static List<String> toStringList() {
		List<String> knowledgeStatus = new ArrayList<String>();
		for (KnowledgeStatus status : KnowledgeStatus.values()) {
			knowledgeStatus.add(status.toString());
		}
		return knowledgeStatus;
	}

	@Override
	public String toString() {
		return this.name().toLowerCase(Locale.ENGLISH);
	}

	/**
	 * @param type
	 *            {@link KnowledgeType} object.
	 * @return default status that is set when a new decision knowledge element is
	 *         created. The default status for alternatives is "idea", for decision
	 *         is "decided", and for issues is "unresolved".
	 */
	public static KnowledgeStatus getDefaultStatus(KnowledgeType type) {
		if (type == null) {
			return UNDEFINED;
		}
		switch (type) {
		case ISSUE:
			return UNRESOLVED;
		case DECISION:
			return DECIDED;
		case ALTERNATIVE:
			return IDEA;
		default:
			return UNDEFINED;
		}
	}

	/**
	 * @return list of all valid knowledge status.
	 */
	public static List<KnowledgeStatus> getAllKnowledgeStatus() {
		List<KnowledgeStatus> statuses = new ArrayList<KnowledgeStatus>();
		for (KnowledgeStatus status : values()) {
			statuses.add(status);
		}
		return statuses;
	}

	public static KnowledgeStatus getNewStatus(KnowledgeElement element) {
		if (element.getType().getSuperType() == KnowledgeType.PROBLEM) {
			if (IssueCheck.isValidDecisionLinkedToDecisionProblem(element)) {
				return KnowledgeStatus.RESOLVED;
			}
			return KnowledgeStatus.UNRESOLVED;
		}
		return element.getStatus();
	}

	public String getColor() {
		switch (this) {
		case UNRESOLVED:
		case CHALLENGED:
			return "crimson";
		case DISCARDED:
		case REJECTED:
			return "gray";
		default:
			return "";
		}
	}
}
