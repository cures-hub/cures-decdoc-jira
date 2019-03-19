package de.uhd.ifi.se.decision.management.jira.model.text;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.atlassian.gzipfilter.org.apache.commons.lang.ArrayUtils;

import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;

public interface TextSplitter {

	public static final String[] EXCLUDED_TAGS = new String[] { "{code}", "{quote}", "{noformat}", "{panel}" };

	/** List of all knowledge types as tags. Sequence matters! */
	public static final String[] RATIONALE_TAGS = new String[] { "{issue}", "{alternative}", "{decision}", "{pro}", "{con}" };

	/** List of all knowledge types as icons. Sequence matters! */
	public static final String[] RATIONALE_ICONS = new String[] { "(!)", "(?)", "(/)", "(y)", "(n)" };

	public static final String[] EXCLUDED_STRINGS = (String[]) ArrayUtils.addAll(ArrayUtils.addAll(EXCLUDED_TAGS, RATIONALE_TAGS),
			RATIONALE_ICONS);

	public static final Set<KnowledgeType> KNOWLEDGE_TYPES = EnumSet.of(KnowledgeType.DECISION, KnowledgeType.ISSUE, KnowledgeType.PRO,
			KnowledgeType.CON, KnowledgeType.ALTERNATIVE);

	/**
	 * Split a text into parts (substrings).
	 * 
	 * @see PartOfText
	 * @param text
	 *            text to be split.
	 * @param projectKey
	 *            of the JIRA project.
	 * @return parts of text (substrings) as a list.
	 */
	List<PartOfText> getPartsOfText(String text, String projectKey);

	static String parseIconsToTags(String commentBody) {
		for (int i = 0; i < RATIONALE_ICONS.length; i++) {
			String icon = RATIONALE_ICONS[i];
			while (commentBody.contains(icon)) {
				commentBody = commentBody.replaceFirst(icon.replace("(", "\\(").replace(")", "\\)"),
						RATIONALE_TAGS[i]);
				if (commentBody.split(System.getProperty("line.separator")).length == 1
						&& !commentBody.endsWith("\r\n")) {
					commentBody = commentBody + RATIONALE_TAGS[i];
				}
				commentBody = commentBody.replaceFirst("\r\n", RATIONALE_TAGS[i]);
			}
		}
		return commentBody;
	}
}