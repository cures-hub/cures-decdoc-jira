package de.uhd.ifi.se.decision.management.jira.persistence;

import java.util.List;

import org.apache.commons.lang3.text.WordUtils;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.comments.MutableComment;
import com.atlassian.jira.user.ApplicationUser;

import de.uhd.ifi.se.decision.management.jira.ComponentGetter;
import de.uhd.ifi.se.decision.management.jira.extraction.DecXtractEventListener;
import de.uhd.ifi.se.decision.management.jira.extraction.model.Comment;
import de.uhd.ifi.se.decision.management.jira.extraction.model.Sentence;
import de.uhd.ifi.se.decision.management.jira.extraction.model.impl.CommentImpl;
import de.uhd.ifi.se.decision.management.jira.extraction.model.impl.SentenceImpl;
import de.uhd.ifi.se.decision.management.jira.extraction.model.util.CommentSplitter;
import de.uhd.ifi.se.decision.management.jira.extraction.persistence.ActiveObjectsManager;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.DocumentationLocation;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.Link;
import de.uhd.ifi.se.decision.management.jira.persistence.tables.DecisionKnowledgeInCommentEntity;
import net.java.ao.Query;

/**
 * Extends the abstract class AbstractPersistenceManager. Uses JIRA issue
 * comments to store decision knowledge.
 *
 * @see AbstractPersistenceManager
 */
public class JiraIssueCommentPersistenceManager extends AbstractPersistenceManager {

	private static final ActiveObjects ACTIVE_OBJECTS = ComponentGetter.getActiveObjects();

	public JiraIssueCommentPersistenceManager(String projectKey) {
		this.projectKey = projectKey;
		this.documentationLocation = DocumentationLocation.JIRAISSUECOMMENT;
	}

	@Override
	public boolean deleteDecisionKnowledgeElement(long id, ApplicationUser user) {
		boolean isDeleted = false;
		for (DecisionKnowledgeInCommentEntity databaseEntry : ACTIVE_OBJECTS
				.find(DecisionKnowledgeInCommentEntity.class, Query.select().where("ID = ?", id))) {
			GenericLinkManager.deleteLinksForElement("s" + id);
			isDeleted = DecisionKnowledgeInCommentEntity.deleteElement(databaseEntry);
		}
		return isDeleted;
	}

	@Override
	public DecisionKnowledgeElement getDecisionKnowledgeElement(long id) {
		for (DecisionKnowledgeInCommentEntity databaseEntry : ACTIVE_OBJECTS
				.find(DecisionKnowledgeInCommentEntity.class, Query.select().where("ID = ?", id))) {
			return new SentenceImpl(databaseEntry);

		}
		return null;
	}

	@Override
	public DecisionKnowledgeElement getDecisionKnowledgeElement(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DecisionKnowledgeElement> getDecisionKnowledgeElements() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DecisionKnowledgeElement> getElementsLinkedWithInwardLinks(DecisionKnowledgeElement element) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DecisionKnowledgeElement> getElementsLinkedWithOutwardLinks(DecisionKnowledgeElement element) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Link> getInwardLinks(DecisionKnowledgeElement element) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Link> getOutwardLinks(DecisionKnowledgeElement element) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DecisionKnowledgeElement insertDecisionKnowledgeElement(DecisionKnowledgeElement element,
			ApplicationUser user, DecisionKnowledgeElement parentElement) {
		long issueId;
		if (parentElement.getDocumentationLocation() == DocumentationLocation.JIRAISSUECOMMENT) {
			Sentence sentence = (Sentence) this.getDecisionKnowledgeElement(parentElement.getId());
			issueId = sentence.getIssueId();
		} else {
			issueId = parentElement.getId();
		}
		MutableIssue issue = ComponentAccessor.getIssueManager().getIssueObject(issueId);
		if (issue == null) {
			return null;
		}
		String macro = getMacro(element);
		String text = macro + element.getSummary() + "\n" + element.getDescription() + macro;
		com.atlassian.jira.issue.comments.Comment comment = ComponentAccessor.getCommentManager().create(issue, user,
				text, false);
		Comment com = new CommentImpl(comment, true);
		for (Sentence sentence : com.getSentences()) {
			GenericLinkManager.deleteLinksForElement("s" + sentence.getId());
		}
		return com.getSentences().get(0);
	}
	
	private static String getMacro(DecisionKnowledgeElement element) {
		KnowledgeType knowledgeType = element.getType();
		String macro = "{" + knowledgeType.toString() + "}";
		return macro;
	}

	@Override
	public boolean updateDecisionKnowledgeElement(DecisionKnowledgeElement element, ApplicationUser user) {
		DecXtractEventListener.editCommentLock = true;

		if (element.getSummary() != null) {
			// Get corresponding element from ao database
			Sentence databaseEntity = (Sentence) this.getDecisionKnowledgeElement(element.getId());
			int newSentenceEnd = databaseEntity.getEndSubstringCount();
			int newSentenceStart = databaseEntity.getStartSubstringCount();
			String newSentenceBody = element.getDescription();

			if ((newSentenceEnd - newSentenceStart) != newSentenceBody.length()) {
				// Get JIRA Comment instance - Casting fails in unittesting with Mock
				CommentManager commentManager = ComponentAccessor.getCommentManager();
				MutableComment mutableComment = (MutableComment) commentManager
						.getCommentById(databaseEntity.getCommentId());

				if (mutableComment.getBody().length() >= databaseEntity.getEndSubstringCount()) {
					String oldSentenceInComment = mutableComment.getBody().substring(newSentenceStart, newSentenceEnd);
					int indexOfOldSentence = mutableComment.getBody().indexOf(oldSentenceInComment);

					String newType = element.getType().toString();
					String tag = "";
					// Allow changing of manual tags, but no tags for icons
					if (databaseEntity.isTagged() && !CommentSplitter.isCommentIconTagged(oldSentenceInComment)) {
						tag = "{" + WordUtils.capitalize(newType) + "}";
					} else if (CommentSplitter.isCommentIconTagged(oldSentenceInComment)) {
						indexOfOldSentence = indexOfOldSentence + 3; // add icon to text.
					}
					String first = mutableComment.getBody().substring(0, indexOfOldSentence);
					String second = tag + newSentenceBody + tag;
					String third = mutableComment.getBody()
							.substring(indexOfOldSentence + oldSentenceInComment.length());

					mutableComment.setBody(first + second + third);
					commentManager.update(mutableComment, true);
					ActiveObjectsManager.updateSentenceBodyWhenCommentChanged(databaseEntity.getCommentId(),
							element.getId(), second);

				}
			}
		}
		boolean isUpdated = ActiveObjectsManager.updateKnowledgeTypeOfSentence(element.getId(), element.getType());
		DecXtractEventListener.editCommentLock = false;
		return isUpdated;
	}
}
