package de.uhd.ifi.se.decision.management.jira.model;

import java.util.Date;

import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.comments.MutableComment;
import com.atlassian.jira.user.ApplicationUser;

import de.uhd.ifi.se.decision.management.jira.persistence.tables.PartOfJiraIssueTextInDatabase;

/**
 * Models a textual part (substrings/sentence) of a Jira issue comment or the
 * description. These parts can either be relevant decision knowledge elements
 * or irrelevant text.
 */
public class PartOfJiraIssueText extends KnowledgeElement {

	private int startPosition;
	private int endPosition;
	private boolean isRelevant;
	private boolean isValidated;
	private Issue jiraIssue;
	private long commentId;

	protected static final Logger LOGGER = LoggerFactory.getLogger(PartOfJiraIssueText.class);

	public PartOfJiraIssueText() {
		this.documentationLocation = DocumentationLocation.JIRAISSUETEXT;
	}

	public PartOfJiraIssueText(int startPosition, int endPosition, String textWithinDescriptionOrComment) {
		this();
		this.startPosition = startPosition;
		this.endPosition = endPosition;
		this.setDescription(textWithinDescriptionOrComment.substring(startPosition, endPosition)
				.replaceAll("\\{.*?\\}", "").trim());
	}

	public PartOfJiraIssueText(String textOfEntireDescriptionOrComment) {
		this(0, textOfEntireDescriptionOrComment.length(), textOfEntireDescriptionOrComment);
	}

	public PartOfJiraIssueText(PartOfJiraIssueTextInDatabase databaseEntry) {
		this();
		this.setId(databaseEntry.getId());
		this.setStartPosition(databaseEntry.getStartPosition());
		this.setEndPosition(databaseEntry.getEndPosition());
		this.setValidated(databaseEntry.isValidated());
		this.setType(databaseEntry.getType());
		this.setRelevant(databaseEntry.isRelevant());
		this.setProject(databaseEntry.getProjectKey());
		this.setCommentId(databaseEntry.getCommentId());
		this.setJiraIssue(databaseEntry.getJiraIssueId());
		this.setStatus(databaseEntry.getStatus());
		String text = getText();
		this.setDescription(text);
		determineOrigin();
	}

	/**
	 * @return true if the text is decision knowledge. This attribute is necessary
	 *         for binary text classification.
	 */
	public boolean isRelevant() {
		return isRelevant;
	}

	/**
	 * Sets whether the part of the text is decision knowledge, i.e., relevant. This
	 * attribute is necessary for binary text classification.
	 * 
	 * @param isRelevant
	 *            true of the text is decision knowledge.
	 */
	public void setRelevant(boolean isRelevant) {
		this.isRelevant = isRelevant;
	}

	/**
	 * @return true if the classification of the text within the Jira issue comment
	 *         is validated, i.e., is manually performed, updated, or checked by a
	 *         human beeing (=manually approved).
	 */
	public boolean isValidated() {
		return isValidated;
	}

	/**
	 * @param isValidated
	 *            true if the classification of the text within the Jira issue
	 *            comment is validated. That means that the classification is
	 *            manually performed, updated, or checked by a human beeing
	 *            (=manually approved).
	 */
	public void setValidated(boolean isValidated) {
		this.isValidated = isValidated;
	}

	/**
	 * Determines whether the part of the text is relevant decision knowledge with a
	 * knowledge type different than KnowledgeType.OTHER. Then, the part of the text
	 * is tagged with the pattern: {knowledge type} text {knowledge type}.
	 * 
	 * @see KnowledgeType
	 * @return true if the part of the text is relevant decision knowledge with a
	 *         knowledge type different than KnowledgeType.OTHER.
	 */
	public boolean isTagged() {
		return getType() != KnowledgeType.OTHER;
	}

	/**
	 * @return start position (number of characters) of the decision knowledge
	 *         element or the irrelevant text within the entire text.
	 */
	public int getStartPosition() {
		return startPosition;
	}

	/**
	 * @param startPosition
	 *            number of characters after that the decision knowledge element or
	 *            the irrelevant text starts within the entire text.
	 */
	public void setStartPosition(int startPosition) {
		this.startPosition = startPosition;
	}

	/**
	 * @return end position (number of characters) of the decision knowledge element
	 *         or the irrelevant text within the entire text.
	 */
	public int getEndPosition() {
		return endPosition;
	}

	/**
	 * @param endPosition
	 *            number of characters after that the decision knowledge element or
	 *            the irrelevant text ends within the entire text.
	 */
	public void setEndPosition(int endPosition) {
		this.endPosition = endPosition;
	}

	/**
	 * Gets the length of the part of the text (substring). This is a derived
	 * method.
	 * 
	 * @return end position - start position.
	 */
	public int getLength() {
		return endPosition - startPosition;
	}

	/**
	 * @return sentence with macro tags, e.g. {issue} How to... {issue} for a
	 *         decision knowledge element or {code:java} public static ... {code}
	 *         for irrelevant text.
	 */
	public String getTextWithTags() {
		String textWithTags = getTextOfEntireDescriptionOrComment();
		if (textWithTags == null || startPosition > textWithTags.length()) {
			return "";
		}
		if (endPosition < textWithTags.length() && endPosition >= startPosition) {
			return textWithTags.substring(startPosition, endPosition);
		} else {
			return textWithTags.substring(startPosition);
		}
	}

	/**
	 * @param jiraIssueId
	 *            of the Jira issue that the decision knowledge element or
	 *            irrelevant text is part of.
	 */
	public void setJiraIssue(long jiraIssueId) {
		setJiraIssue(ComponentAccessor.getIssueManager().getIssueObject(jiraIssueId));
	}

	/**
	 * @param jiraIssue
	 *            Jira issue that the decision knowledge element or irrelevant text
	 *            is part of.
	 */
	@JsonProperty("jiraIssue")
	public void setJiraIssue(Issue jiraIssue) {
		this.jiraIssue = jiraIssue;
	}

	/**
	 * @return Jira issue that the decision knowledge element or irrelevant text is
	 *         part of.
	 */
	@Override
	public Issue getJiraIssue() {
		return jiraIssue;
	}

	@Override
	public String getKey() {
		Issue jiraIssue = getJiraIssue();
		if (jiraIssue != null) {
			return jiraIssue.getKey() + ":" + getId();
		}
		return "";
	}

	@Override
	public Date getCreationDate() {
		Comment comment = getComment();
		if (comment != null) {
			return comment.getCreated();
		}
		Issue issue = getJiraIssue();
		if (issue != null) {
			return issue.getCreated();
		}
		return super.getCreationDate();
	}

	@Override
	public Date getUpdatingDate() {
		Comment comment = getComment();
		if (comment != null) {
			return comment.getUpdated();
		}
		Issue issue = getJiraIssue();
		if (issue != null) {
			return issue.getUpdated();
		}
		return super.getUpdatingDate();
	}

	/**
	 * @return Jira issue comment as a {@link MutableComment} object that the
	 *         decision knowledge element or irrelevant text is part of. Returns
	 *         null if the part of text is within the description.
	 */
	public MutableComment getComment() {
		long commentId = getCommentId();
		if (commentId <= 0) {
			return null;
		}
		CommentManager commentManager = ComponentAccessor.getCommentManager();
		return commentManager.getMutableComment(commentId);
	}

	/**
	 * @return part of the text without macro tags and trimmed.
	 */
	public String getText() {
		return getTextWithTags().replaceAll("\\{.*?\\}", "").trim();
	}

	@Override
	public void setDescription(String body) {
		this.setSummary(body);
	}

	@Override
	public void setSummary(String body) {
		super.setDescription(body);
		super.setSummary(body);
	}

	/**
	 * @return id of the Jira issue comment that the decision knowledge element or
	 *         irrelevant text is part of. Returns 0 if the part of text is within
	 *         the description.
	 */
	public long getCommentId() {
		return commentId;
	}

	/**
	 * @param id
	 *            of the Jira issue comment that the decision knowledge element or
	 *            irrelevant text is part of.
	 */
	public void setCommentId(long id) {
		this.commentId = id;
	}

	/**
	 * Sets the {@link Origin} to {@link Origin#COMMIT} if the commit message was
	 * transcribed into the Jira issue comment.
	 */
	private void determineOrigin() {
		Comment comment = getComment();
		origin = Origin.determineOrigin(comment);
	}

	/**
	 * @param comment
	 *            the Jira issue comment that the decision knowledge element or
	 *            irrelevant text is part of.
	 */
	public void setComment(Comment comment) {
		if (comment != null) {
			setCommentId(comment.getId());
			setJiraIssue(comment.getIssue());
			setCreationDate(comment.getCreated());
		}
	}

	/**
	 * @return text of either the entire description or the entire comment that the
	 *         decision knowledge element or the irrelevant sentence is part of.
	 */
	public String getTextOfEntireDescriptionOrComment() {
		Comment comment = getComment();
		if (comment != null) {
			return comment.getBody();
		}
		return getJiraIssueDescription();
	}

	/**
	 * @return Jira issue description that the decision knowledge element or
	 *         irrelevant text is part of.
	 */
	public String getJiraIssueDescription() {
		Issue issue = getJiraIssue();
		if (issue == null) {
			return "";
		}
		return issue.getDescription();
	}

	@Override
	public ApplicationUser getCreator() {
		Comment comment = getComment();
		if (comment != null) {
			return comment.getAuthorApplicationUser();
		}
		Issue issue = getJiraIssue();
		if (issue != null) {
			return issue.getReporter();
		}
		return null;
	}

	/**
	 * @return true if the textual parts (substrings) of Jira issue comments or the
	 *         description is valid.
	 */
	public boolean isValid() {
		if (getEndPosition() == 0 || getLength() == 0) {
			return false;
		}
		if (getJiraIssue() == null) {
			return false;
		}
		if (getSummary().isBlank() && getDescription().isBlank()) {
			return false;
		}
		if (getCommentId() <= 0) {
			// documented in Jira issue description
			return true;
		}
		return getComment() != null;
	}
}
