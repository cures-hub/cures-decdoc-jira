package de.uhd.ifi.se.decision.management.jira.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.MutableComment;
import com.atlassian.jira.user.ApplicationUser;

import de.uhd.ifi.se.decision.management.jira.ComponentGetter;
import de.uhd.ifi.se.decision.management.jira.extraction.DecXtractEventListener;
import de.uhd.ifi.se.decision.management.jira.extraction.model.Comment;
import de.uhd.ifi.se.decision.management.jira.extraction.model.Sentence;
import de.uhd.ifi.se.decision.management.jira.extraction.model.impl.CommentImpl;
import de.uhd.ifi.se.decision.management.jira.extraction.model.impl.SentenceImpl;
import de.uhd.ifi.se.decision.management.jira.extraction.persistence.ActiveObjectsManager;
import de.uhd.ifi.se.decision.management.jira.extraction.view.macros.AbstractKnowledgeClassificationMacro;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElementImpl;
import de.uhd.ifi.se.decision.management.jira.model.DocumentationLocation;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.Link;
import de.uhd.ifi.se.decision.management.jira.model.LinkType;
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
		Sentence sentence = null;
		for (DecisionKnowledgeInCommentEntity databaseEntry : ACTIVE_OBJECTS
				.find(DecisionKnowledgeInCommentEntity.class, Query.select().where("ID = ?", id))) {
			sentence = new SentenceImpl(databaseEntry);
		}
		return sentence;
	}

	public DecisionKnowledgeElement getDecisionKnowledgeElement(Sentence sentence) {
		if (sentence == null) {
			return null;
		}
		if (sentence.getId() > 0) {
			return this.getDecisionKnowledgeElement(sentence.getId());
		}

		Sentence sentenceInDatabase = null;
		for (DecisionKnowledgeInCommentEntity databaseEntry : ACTIVE_OBJECTS.find(
				DecisionKnowledgeInCommentEntity.class,
				Query.select().where(
						"PROJECT_KEY = ? AND COMMENT_ID = ? AND END_SUBSTRING_COUNT = ? AND START_SUBSTRING_COUNT = ?",
						sentence.getProject().getProjectKey(), sentence.getCommentId(), sentence.getEndSubstringCount(),
						sentence.getStartSubstringCount()))) {
			sentenceInDatabase = new SentenceImpl(databaseEntry);
		}
		return sentenceInDatabase;
	}

	public static DecisionKnowledgeElement searchForLast(Sentence sentence, KnowledgeType typeToSearch) {
		Sentence lastSentence = null;
		DecisionKnowledgeInCommentEntity[] sententenceList = ACTIVE_OBJECTS.find(DecisionKnowledgeInCommentEntity.class,
				Query.select().where("ISSUE_ID = ?", sentence.getIssueId()).order("ID DESC"));

		for (DecisionKnowledgeInCommentEntity databaseEntry : sententenceList) {
			if (databaseEntry.getType().equals(typeToSearch.toString())) {
				lastSentence = new SentenceImpl(databaseEntry);
				break;
			}
		}
		return lastSentence;
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

	public static List<DecisionKnowledgeElement> getElementsForIssue(long issueId, String projectKey) {
		List<DecisionKnowledgeElement> elements = new ArrayList<DecisionKnowledgeElement>();
		for (DecisionKnowledgeInCommentEntity databaseEntry : ACTIVE_OBJECTS.find(
				DecisionKnowledgeInCommentEntity.class,
				Query.select().where("PROJECT_KEY = ? AND ISSUE_ID = ?", projectKey, issueId))) {
			elements.add(new SentenceImpl(databaseEntry));
		}
		return elements;
	}

	/**
	 * Works more efficient than "getElementsForIssue" for Sentence ID searching in
	 * Macros
	 * 
	 * @param issueId
	 * @param projectKey
	 * @param type
	 * @return A list of all fitting Sentence objects
	 */
	public static List<DecisionKnowledgeElement> getElementsForIssueWithType(long issueId, String projectKey,
			String type) {
		List<DecisionKnowledgeElement> elements = new ArrayList<DecisionKnowledgeElement>();
		for (DecisionKnowledgeInCommentEntity databaseEntry : ACTIVE_OBJECTS.find(
				DecisionKnowledgeInCommentEntity.class,
				Query.select().where("PROJECT_KEY = ? AND ISSUE_ID = ? AND TYPE = ?", projectKey, issueId, type))) {
			elements.add(new SentenceImpl(databaseEntry));
		}
		return elements;
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
		String tag = AbstractKnowledgeClassificationMacro.getTag(element.getTypeAsString());
		String text = tag + element.getSummary() + "\n" + element.getDescription() + tag;
		com.atlassian.jira.issue.comments.Comment comment = ComponentAccessor.getCommentManager().create(issue, user,
				text, false);
		Comment com = new CommentImpl(comment, true);
		for (Sentence sentence : com.getSentences()) {
			GenericLinkManager.deleteLinksForElement("s" + sentence.getId());
		}
		return com.getSentences().get(0);
	}

	public static long insertDecisionKnowledgeElement(Sentence sentence, ApplicationUser user) {
		DecisionKnowledgeElement existingElement = new JiraIssueCommentPersistenceManager("")
				.getDecisionKnowledgeElement(sentence);
		if (existingElement != null) {
			JiraIssueCommentPersistenceManager.checkIfSentenceHasAValidLink(existingElement.getId(),
					sentence.getIssueId(), LinkType.getLinkTypeForKnowledgeType(existingElement.getType()));
			return existingElement.getId();
		}

		sentence.setTagged(false);
		sentence.setRelevant(false);
		sentence.setType("");

		DecisionKnowledgeInCommentEntity databaseEntry = ACTIVE_OBJECTS.create(DecisionKnowledgeInCommentEntity.class);
		setParameters(sentence, databaseEntry);
		databaseEntry.save();
		ActiveObjectsManager.LOGGER.debug("\naddNewSentenceintoAo:\nInsert Sentence " + databaseEntry.getId()
				+ " into database from comment " + databaseEntry.getCommentId());
		return databaseEntry.getId();
	}

	private static void setParameters(Sentence element, DecisionKnowledgeInCommentEntity databaseEntry) {
		databaseEntry.setProjectKey(element.getProject().getProjectKey());
		databaseEntry.setCommentId(element.getCommentId());
		databaseEntry.setType(element.getTypeAsString());
		databaseEntry.setRelevant(element.isRelevant());
		databaseEntry.setTagged(element.isTagged());
		databaseEntry.setStartSubstringCount(element.getStartSubstringCount());
		databaseEntry.setEndSubstringCount(element.getEndSubstringCount());
		databaseEntry.setIssueId(element.getIssueId());
	}

	@Override
	public boolean updateDecisionKnowledgeElement(DecisionKnowledgeElement element, ApplicationUser user) {
		Sentence sentence = new SentenceImpl();
		sentence.setId(element.getId());
		sentence.setType(element.getType());
		sentence.setSummary(element.getSummary());
		sentence.setDescription(element.getDescription());
		sentence.setProject(element.getProject());
		sentence.setTagged(true);

		return this.updateDecisionKnowledgeElement(sentence, user);
	}

	public boolean updateDecisionKnowledgeElement(Sentence element, ApplicationUser user) {
		// Get corresponding element from database
		Sentence sentence = (Sentence) this.getDecisionKnowledgeElement(element.getId());
		if (sentence == null) {
			return false;
		}

		MutableComment mutableComment = element.getComment();

		String changedPartOfComment = "";
		if (element.getSummary() == null) {
			// only knowledge type is changed
			changedPartOfComment = mutableComment.getBody().substring(sentence.getStartSubstringCount(),
					sentence.getEndSubstringCount());
			if (element.getType() == KnowledgeType.OTHER) {
				changedPartOfComment = changedPartOfComment.replaceAll("\\{.*?\\}", "");
			} else {
				changedPartOfComment = changedPartOfComment.replaceAll("(?i)" + sentence.getType().toString() + "}",
						element.getType().toString() + "}");
			}
		} else {
			// description and maybe also knowledge type are changed
			String tag = AbstractKnowledgeClassificationMacro.getTag(element.getType());
			changedPartOfComment = tag + element.getDescription() + tag;
		}

		String firstPartOfComment = mutableComment.getBody().substring(0, sentence.getStartSubstringCount());
		String lastPartOfComment = mutableComment.getBody().substring(sentence.getEndSubstringCount());

		DecXtractEventListener.editCommentLock = true;
		mutableComment.setBody(firstPartOfComment + changedPartOfComment + lastPartOfComment);
		ComponentAccessor.getCommentManager().update(mutableComment, true);
		DecXtractEventListener.editCommentLock = false;

		int lengthDifference = changedPartOfComment.length() - sentence.getLength();
		updateSentenceLengthForOtherSentencesInSameComment(sentence, lengthDifference);

		sentence.setEndSubstringCount(sentence.getStartSubstringCount() + changedPartOfComment.length());
		sentence.setType(element.getType());
		sentence.setTagged(element.isTagged());
		sentence.setRelevant(element.isRelevant());

		boolean isUpdated = updateInDatabase(sentence);
		return isUpdated;
	}

	public static int countCommentsForIssue(long issueId) {
		DecisionKnowledgeInCommentEntity[] commentSentences = ACTIVE_OBJECTS
				.find(DecisionKnowledgeInCommentEntity.class, Query.select().where("ISSUE_ID = ?", issueId));
		Set<Long> treeSet = new TreeSet<Long>();
		for (DecisionKnowledgeInCommentEntity sentence : commentSentences) {
			treeSet.add(sentence.getCommentId());
		}
		return treeSet.size();
	}

	public static DecisionKnowledgeElement compareForLaterElement(DecisionKnowledgeElement first,
			DecisionKnowledgeElement second) {
		if (first == null) {
			return second;
		} else if (second == null) {
			return first;
		} else if (first.getId() > second.getId()) {
			return first;
		} else {
			return second;
		}
	}

	public static boolean checkLastElementAndCreateLink(DecisionKnowledgeElement lastElement, Sentence sentence) {
		if (lastElement == null) {
			return false;
		}
		Link link = Link.instantiateDirectedLink(lastElement, sentence);
		GenericLinkManager.insertLink(link, null);
		return true;
	}

	public static void createSmartLinkForSentence(Sentence sentence) {
		if (sentence == null || AbstractPersistenceManager.isElementLinked(sentence)) {
			return;
		}
		boolean smartLinkCreated = false;
		KnowledgeType knowledgeType = sentence.getType();
		if (knowledgeType == KnowledgeType.ARGUMENT || knowledgeType == KnowledgeType.PRO
				|| knowledgeType == KnowledgeType.CON) {
			DecisionKnowledgeElement lastElement = JiraIssueCommentPersistenceManager.compareForLaterElement(
					searchForLast(sentence, KnowledgeType.ALTERNATIVE),
					searchForLast(sentence, KnowledgeType.DECISION));
			smartLinkCreated = JiraIssueCommentPersistenceManager.checkLastElementAndCreateLink(lastElement, sentence);
		} else if (knowledgeType == KnowledgeType.DECISION || knowledgeType == KnowledgeType.ALTERNATIVE) {
			DecisionKnowledgeElement lastElement = searchForLast(sentence, KnowledgeType.ISSUE);
			smartLinkCreated = JiraIssueCommentPersistenceManager.checkLastElementAndCreateLink(lastElement, sentence);
		}
		if (!smartLinkCreated) {
			checkIfSentenceHasAValidLink(sentence.getId(), sentence.getIssueId(),
					LinkType.getLinkTypeForKnowledgeType(sentence.getTypeAsString()));
		}
	}

	public static void checkIfSentenceHasAValidLink(long sentenceId, long issueId, LinkType linkType) {
		if (!AbstractPersistenceManager.isElementLinked(sentenceId, DocumentationLocation.JIRAISSUECOMMENT)) {
			DecisionKnowledgeElement parentElement = new DecisionKnowledgeElementImpl();
			parentElement.setId(issueId);
			parentElement.setDocumentationLocation("i");

			DecisionKnowledgeElement childElement = new DecisionKnowledgeElementImpl();
			childElement.setId(sentenceId);
			childElement.setDocumentationLocation("s");

			Link link = Link.instantiateDirectedLink(parentElement, childElement, linkType);
			GenericLinkManager.insertLinkWithoutTransaction(link);
		}
	}

	public static boolean updateInDatabase(Sentence sentence) {
		boolean isUpdated = false;
		for (DecisionKnowledgeInCommentEntity databaseEntry : ACTIVE_OBJECTS
				.find(DecisionKnowledgeInCommentEntity.class)) {
			if (databaseEntry.getId() == sentence.getId()) {
				setParameters(sentence, databaseEntry);
				databaseEntry.save();
				isUpdated = true;
			}
		}
		return isUpdated;
	}

	public static void updateSentenceLengthForOtherSentencesInSameComment(Sentence sentence, int lengthDifference) {
		for (DecisionKnowledgeInCommentEntity otherSentenceInComment : ACTIVE_OBJECTS
				.find(DecisionKnowledgeInCommentEntity.class, "COMMENT_ID = ?", sentence.getCommentId())) {
			if (otherSentenceInComment.getStartSubstringCount() > sentence.getStartSubstringCount()
					&& otherSentenceInComment.getId() != sentence.getId()) {
				otherSentenceInComment
						.setStartSubstringCount(otherSentenceInComment.getStartSubstringCount() + lengthDifference);
				otherSentenceInComment
						.setEndSubstringCount(otherSentenceInComment.getEndSubstringCount() + lengthDifference);
				otherSentenceInComment.save();
			}
		}
	}

	public static int addTagsToCommentWhenAutoClassified(DecisionKnowledgeInCommentEntity sentenceEntity) {
		Sentence sentence = new SentenceImpl(sentenceEntity);
		MutableComment mutableComment = sentence.getComment();
		String newBody = mutableComment.getBody().substring(sentenceEntity.getStartSubstringCount(),
				sentenceEntity.getEndSubstringCount());

		newBody = "{" + sentenceEntity.getType() + "}" + newBody + "{" + sentenceEntity.getType() + "}";
		int lengthDiff = (sentenceEntity.getType().length() + 2) * 2;

		DecXtractEventListener.editCommentLock = true;
		mutableComment.setBody(mutableComment.getBody().substring(0, sentenceEntity.getStartSubstringCount()) + newBody
				+ mutableComment.getBody().substring(sentenceEntity.getEndSubstringCount()));
		ComponentAccessor.getCommentManager().update(mutableComment, true);
		DecXtractEventListener.editCommentLock = false;
		return lengthDiff;
	}

	public static void cleanSentenceDatabaseForProject(String projectKey) {
		for (DecisionKnowledgeInCommentEntity databaseEntry : ACTIVE_OBJECTS
				.find(DecisionKnowledgeInCommentEntity.class, Query.select().where("PROJECT_KEY = ?", projectKey))) {
			Sentence sentence = null;
			boolean deleteFlag = false;
			try {
				sentence = new SentenceImpl(databaseEntry);
				ComponentAccessor.getCommentManager().getCommentById(sentence.getCommentId());
				if (sentence.getEndSubstringCount() == 0 && sentence.getStartSubstringCount() == 0) {
					deleteFlag = true;
				}
			} catch (Exception e) {
				deleteFlag = true;
			}
			if (deleteFlag) {
				DecisionKnowledgeInCommentEntity.deleteElement(databaseEntry);
				GenericLinkManager.deleteLinksForElementWithoutTransaction("s" + databaseEntry.getId());
			}
		}
	}

}
