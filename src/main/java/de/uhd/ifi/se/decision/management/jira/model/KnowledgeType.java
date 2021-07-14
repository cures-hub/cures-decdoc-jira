package de.uhd.ifi.se.decision.management.jira.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;

import de.uhd.ifi.se.decision.management.jira.ComponentGetter;
import de.uhd.ifi.se.decision.management.jira.view.macros.AbstractKnowledgeClassificationMacro;

/**
 * Models the possible types of decision knowledge elements. The decision
 * knowledge types most often used are decision, issue, argument, and
 * alternative. In addition, code is modeled as a knowledge type. (Code is
 * system knowledge, not decision knowledge).
 * 
 * Knowledge elements can also be of other types. For example, knowledge
 * elements of other types are requirements and development tasks (=work items)
 * but they are not included in this enum (only as OTHER).
 */
public enum KnowledgeType {
	ALTERNATIVE("#fff6e8", "(on)", true), //
	ASSUMPTION, //
	ASSESSMENT, //
	ARGUMENT("#f5f5f5"), //
	PRO("#defade", "(+)", true), //
	CON("#ffe7e7", "(-)", true), //
	CLAIM, CONTEXT("#ffffdd"), //
	CONSTRAINT, //
	DECISION("#fce3be", "(/)", true), //
	GOAL, //
	ISSUE("#ffffcc", "(!)", true), //
	IMPLICATION, //
	PROBLEM("#ffffcc"), //
	RATIONALE("#f5f5f5"), //
	SOLUTION("#fce3be"), //
	QUESTION("#ffffcc"), //
	CODE("#f2f2f2"), // code is system knowledge
	OTHER; // other system knowledge (requirements) and project knowledge (work items)

	private String color;
	private String icon;
	private boolean canBeDocumentedInJiraIssueText;

	private KnowledgeType() {
		this("#ffffff");
	}

	private KnowledgeType(String color) {
		this("#ffffff", "", false);
	}

	private KnowledgeType(String color, String icon, boolean canBeDocumentedInJiraIssueText) {
		this.color = color;
		this.icon = icon;
		this.canBeDocumentedInJiraIssueText = canBeDocumentedInJiraIssueText;
	}

	/**
	 * @return minimal set of decision knowledge types for the management of
	 *         decision knowledge (decision, issue, argument, alternative).
	 */
	public static Set<KnowledgeType> getDefaultTypes() {
		return EnumSet.of(DECISION, ISSUE, ARGUMENT, ALTERNATIVE);
	}

	/**
	 * Converts a string to a knowledge type.
	 *
	 * @param type
	 *            of knowledge as a String.
	 * @return knowledge type as a {@link KnowledgeType} object.
	 */
	public static KnowledgeType getKnowledgeType(String type) {
		if (type == null || type.isEmpty()) {
			return KnowledgeType.OTHER;
		}
		for (KnowledgeType knowledgeType : KnowledgeType.values()) {
			if (knowledgeType.name().toLowerCase(Locale.ENGLISH).matches(type.toLowerCase(Locale.ENGLISH) + "(.)*")) {
				return knowledgeType;
			}
		}
		if (type.contains("Pro")) {
			return PRO;
		}
		if (type.contains("Con")) {
			return CON;
		}
		return OTHER;
	}

	/**
	 * @return the decision knowledge type. If it is a pro-argument or con-argument,
	 *         the argument knowledge type is returned.
	 */
	public KnowledgeType replaceProAndConWithArgument() {
		return replaceProAndConWithArgument(this);
	}

	/**
	 * @param type
	 *            of decision knowledge.
	 * @return the decision knowledge type. If it is a pro-argument or con-argument,
	 *         the argument knowledge type is returned.
	 */
	public static KnowledgeType replaceProAndConWithArgument(KnowledgeType type) {
		switch (type) {
		case PRO:
			return KnowledgeType.ARGUMENT;
		case CON:
			return KnowledgeType.ARGUMENT;
		default:
			return type;
		}
	}

	/**
	 * @param type
	 *            of decision knowledge.
	 * @return parent knowledge type of a knowledge type for link creation in the
	 *         knowledge graph. For example, the parent type of argument is decision
	 *         or alternative. The parent type of decision or alternative is issue.
	 */
	public static List<KnowledgeType> getParentTypes(KnowledgeType type) {
		List<KnowledgeType> parentTypes = new ArrayList<KnowledgeType>();
		if (type == null) {
			return parentTypes;
		}
		if (type == ARGUMENT || type == PRO || type == CON) {
			parentTypes.add(ALTERNATIVE);
			parentTypes.add(DECISION);
		} else if (type == DECISION || type == ALTERNATIVE) {
			parentTypes.add(ISSUE);
		}
		return parentTypes;
	}

	/**
	 * @param type
	 *            of decision knowledge.
	 * @return super decision knowledge type of the decision knowledge element in
	 *         the decision documentation model by Hesse and Paech (2013). For
	 *         example, the super type of argument is rationale and the super type
	 *         of issue is problem.
	 */
	public static KnowledgeType getSuperType(KnowledgeType type) {
		if (type == null) {
			return null;
		}
		switch (type) {
		case ISSUE:
			return KnowledgeType.PROBLEM;
		case GOAL:
			return KnowledgeType.PROBLEM;
		case ALTERNATIVE:
			return KnowledgeType.SOLUTION;
		case DECISION:
			return KnowledgeType.SOLUTION;
		case CLAIM:
			return KnowledgeType.SOLUTION;
		case CONSTRAINT:
			return KnowledgeType.CONTEXT;
		case ASSUMPTION:
			return KnowledgeType.CONTEXT;
		case IMPLICATION:
			return KnowledgeType.CONTEXT;
		case CONTEXT:
			return KnowledgeType.CONTEXT;
		case ARGUMENT:
			return KnowledgeType.RATIONALE;
		case ASSESSMENT:
			return KnowledgeType.RATIONALE;
		default:
			return type;
		}
	}

	/**
	 * @return super decision knowledge type of the decision knowledge element in
	 *         the decision documentation model by Hesse and Paech (2013). For
	 *         example, the super type of argument is rationale and the super type
	 *         of issue is problem.
	 */
	public KnowledgeType getSuperType() {
		return getSuperType(this);
	}

	/**
	 * @return decision knowledge type as a String starting with a capital letter,
	 *         e.g., Argument, Decision, or Alternative.
	 */
	@Override
	public String toString() {
		return StringUtils.capitalize(name().toLowerCase(Locale.ENGLISH));
	}

	/**
	 * @return list of decision knowledge types as Strings starting with a capital
	 *         letter.
	 */
	public static List<String> toStringList() {
		List<String> knowledgeTypes = new ArrayList<String>();
		for (KnowledgeType knowledgeType : KnowledgeType.values()) {
			knowledgeTypes.add(knowledgeType.toString());
		}
		return knowledgeTypes;
	}

	public String getIconUrl() {
		switch (this) {
		case PRO:
			return ComponentGetter.getUrlOfImageFolder() + "argument_pro.png";
		case CON:
			return ComponentGetter.getUrlOfImageFolder() + "argument_con.png";
		default:
			return ComponentGetter.getUrlOfImageFolder() + this.name().toLowerCase(Locale.ENGLISH) + ".png";
		}
	}

	public static String getIconUrl(KnowledgeElement element) {
		if (element == null) {
			return "";
		}
		if (element instanceof PartOfJiraIssueText && !((PartOfJiraIssueText) element).isRelevant()) {
			return ComponentGetter.getUrlOfImageFolder() + "other.png";
		}
		if (element.getType() == OTHER) {
			if (element instanceof PartOfJiraIssueText) {
				return ComponentGetter.getUrlOfImageFolder() + "other.png";
			}
			IssueManager issueManager = ComponentAccessor.getIssueManager();
			Issue issue = issueManager.getIssueByCurrentKey(element.getKey());
			if (issue == null) {
				return ComponentGetter.getUrlOfImageFolder() + "other.png";
			}
			return issue.getIssueType().getCompleteIconUrl();
		}
		return element.getType().getIconUrl();
	}

	public static String getIconUrl(KnowledgeElement element, String linkType) {
		if (linkType == null) {
			return getIconUrl(element);
		}
		switch (element.getType()) {
		case ARGUMENT:
			if (linkType.equals("support")) {
				return ComponentGetter.getUrlOfImageFolder() + "argument_pro.png";
			} else if (linkType.equals("attack")) {
				return ComponentGetter.getUrlOfImageFolder() + "argument_con.png";
			}
		default:
			return getIconUrl(element);
		}
	}

	public String getColor() {
		return color;
	}

	public String getIconString() {
		return icon;
	}

	public boolean canBeDocumentedInJiraIssueText() {
		return canBeDocumentedInJiraIssueText;
	}

	public static List<String> getRequirementsTypes() {
		List<String> requirementsTypes = new ArrayList<>();
		requirementsTypes.add("System Function");
		requirementsTypes.add("Nonfunctional Requirement");
		requirementsTypes.add("Persona");
		requirementsTypes.add("Usertask");
		requirementsTypes.add("Subtask");
		requirementsTypes.add("User Role");
		requirementsTypes.add("Quality Requirement");
		requirementsTypes.add("Story");
		requirementsTypes.add("Epic");
		requirementsTypes.add("Workspace");
		return requirementsTypes;
	}

	public boolean isDecisionKnowledge() {
		return this != CODE && this != OTHER;
	}

	public String getTag() {
		if (this == KnowledgeType.OTHER || name().isBlank()) {
			return "";
		}
		return "{" + name().toLowerCase() + "}";
	}

	/**
	 * @return knowledge types that (currently) can be documented in Jira issue
	 *         description or comments using
	 *         {@link AbstractKnowledgeClassificationMacro}s.
	 */
	public static Set<KnowledgeType> macroTypes() {
		Set<KnowledgeType> macroTypes = new LinkedHashSet<>();
		for (KnowledgeType type : KnowledgeType.values()) {
			if (type.canBeDocumentedInJiraIssueText) {
				macroTypes.add(type);
			}
		}
		return macroTypes;
	}
}
