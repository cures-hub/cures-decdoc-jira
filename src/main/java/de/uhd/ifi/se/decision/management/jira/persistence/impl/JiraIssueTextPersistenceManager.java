package de.uhd.ifi.se.decision.management.jira.persistence.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.MutableComment;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.user.ApplicationUser;

import de.uhd.ifi.se.decision.management.jira.ComponentGetter;
import de.uhd.ifi.se.decision.management.jira.classification.implementation.OnlineFileTrainerImpl;
import de.uhd.ifi.se.decision.management.jira.eventlistener.JiraIssueTextExtractionEventListener;
import de.uhd.ifi.se.decision.management.jira.model.DocumentationLocation;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeGraph;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeStatus;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.Link;
import de.uhd.ifi.se.decision.management.jira.model.impl.KnowledgeElementImpl;
import de.uhd.ifi.se.decision.management.jira.model.text.PartOfJiraIssueText;
import de.uhd.ifi.se.decision.management.jira.model.text.impl.PartOfJiraIssueTextImpl;
import de.uhd.ifi.se.decision.management.jira.model.text.impl.TextSplitterImpl;
import de.uhd.ifi.se.decision.management.jira.persistence.KnowledgePersistenceManager;
import de.uhd.ifi.se.decision.management.jira.persistence.tables.PartOfJiraIssueTextInDatabase;
import de.uhd.ifi.se.decision.management.jira.view.macros.AbstractKnowledgeClassificationMacro;
import net.java.ao.Query;

/**
 * Extends the abstract class
 * {@link AbstractPersistenceManagerForSingleLocation}. Uses Jira issue comments
 * or the description to store decision knowledge.
 *
 * @see AbstractPersistenceManagerForSingleLocation
 */
public class JiraIssueTextPersistenceManager extends AbstractPersistenceManagerForSingleLocation {
	private static final Logger LOGGER = LoggerFactory.getLogger(JiraIssueTextPersistenceManager.class);
	private static final ActiveObjects ACTIVE_OBJECTS = ComponentGetter.getActiveObjects();
	private static OnlineFileTrainerImpl classificationTrainer = new OnlineFileTrainerImpl();

	public JiraIssueTextPersistenceManager(String projectKey) {
		this.projectKey = projectKey;
		this.documentationLocation = DocumentationLocation.JIRAISSUETEXT;
	}

	@Override
	public boolean deleteDecisionKnowledgeElement(long id, ApplicationUser user) {
		if (id <= 0 || user == null) {
			LOGGER.error(
					"Element cannot be deleted since it does not exist (id is less than zero) or the user is null.");
			return false;
		}
		boolean isDeleted = false;
		for (PartOfJiraIssueTextInDatabase databaseEntry : ACTIVE_OBJECTS.find(PartOfJiraIssueTextInDatabase.class,
				Query.select().where("ID = ?", id))) {
			GenericLinkManager.deleteLinksForElement(id, DocumentationLocation.JIRAISSUETEXT);
			KnowledgeGraph.getOrCreate(projectKey).removeVertex(new PartOfJiraIssueTextImpl(databaseEntry));
			isDeleted = PartOfJiraIssueTextInDatabase.deleteElement(databaseEntry);
		}
		return isDeleted;
	}

	/**
	 * Deletes all decision knowledge elements and their links documented in a
	 * certain comment of a Jira issue in database. Does not delete or change the
	 * comment itself.
	 *
	 * @param comment
	 *            of the Jira issue that the decision knowledge elements are
	 *            documented in.
	 * @return true if deletion was successfull.
	 */
	public boolean deleteElementsInComment(Comment comment) {
		if (comment == null) {
			LOGGER.error("Decision knowledge elements in comment cannot be deleted since the comment is null.");
			return false;
		}
		return deletePartsOfText(comment.getIssue().getId(), comment.getId());
	}

	/**
	 * Deletes all decision knowledge elements and their links documented in the
	 * description of a Jira issue. Does not delete the text or change the
	 * description itself.
	 *
	 * @param jiraIssue
	 *            that the decision knowledge elements are documented in.
	 * @return true if deletion was successfull.
	 */
	public boolean deleteElementsInDescription(Issue jiraIssue) {
		if (jiraIssue == null) {
			LOGGER.error("Decision knowledge elements in description cannot be deleted since the Jira issue is null.");
			return false;
		}
		return deletePartsOfText(jiraIssue.getId(), 0);
	}

	private boolean deletePartsOfText(long jiraIssueId, long commentId) {
		boolean isDeleted = false;
		PartOfJiraIssueTextInDatabase[] databaseEntries = ACTIVE_OBJECTS.find(PartOfJiraIssueTextInDatabase.class,
				Query.select().where("PROJECT_KEY = ? AND JIRA_ISSUE_ID = ? AND COMMENT_ID = ?", projectKey,
						jiraIssueId, commentId));
		for (PartOfJiraIssueTextInDatabase databaseEntry : databaseEntries) {
			GenericLinkManager.deleteLinksForElement(databaseEntry.getId(), DocumentationLocation.JIRAISSUETEXT);
			isDeleted = PartOfJiraIssueTextInDatabase.deleteElement(databaseEntry);
		}
		return isDeleted;
	}

	@Override
	public KnowledgeElement getDecisionKnowledgeElement(long id) {
		PartOfJiraIssueText sentence = null;
		for (PartOfJiraIssueTextInDatabase databaseEntry : ACTIVE_OBJECTS.find(PartOfJiraIssueTextInDatabase.class,
				Query.select().where("ID = ?", id))) {
			sentence = new PartOfJiraIssueTextImpl(databaseEntry);
		}
		return sentence;
	}

	@Override
	public KnowledgeElement getDecisionKnowledgeElement(String key) {
		if (key == null) {
			return null;
		}
		long id = Long.parseLong(key.split(":")[1]);
		return getDecisionKnowledgeElement(id);
	}

	@Override
	public List<KnowledgeElement> getDecisionKnowledgeElements() {
		List<KnowledgeElement> decisionKnowledgeElements = new ArrayList<KnowledgeElement>();
		for (PartOfJiraIssueTextInDatabase databaseEntry : ACTIVE_OBJECTS.find(PartOfJiraIssueTextInDatabase.class,
				Query.select().where("PROJECT_KEY = ?", projectKey))) {
			decisionKnowledgeElements.add(new PartOfJiraIssueTextImpl(databaseEntry));
		}
		return decisionKnowledgeElements;
	}

	/**
	 * Returns all decision knowledge elements documented in the description or
	 * comments of a Jira issue.
	 *
	 * @param jiraIssueId
	 *            id of the Jira issue that the decision knowledge elements are
	 *            documented in.
	 * @return list of all decision knowledge elements documented in the description
	 *         or comments of a Jira issue. Does not return irrelevant parts of
	 *         text.
	 */
	public List<KnowledgeElement> getElementsInJiraIssue(long jiraIssueId) {
		List<KnowledgeElement> elements = new ArrayList<KnowledgeElement>();
		for (PartOfJiraIssueTextInDatabase databaseEntry : ACTIVE_OBJECTS.find(PartOfJiraIssueTextInDatabase.class,
				Query.select().where("PROJECT_KEY = ? AND JIRA_ISSUE_ID = ? AND RELEVANT = TRUE", projectKey,
						jiraIssueId))) {
			elements.add(new PartOfJiraIssueTextImpl(databaseEntry));
		}
		return elements;
	}

	/**
	 * Returns all decision knowledge elements documented in a certain comment of a
	 * Jira issue.
	 *
	 * @param commentId
	 *            id of the comment that the decision knowledge element(s) is/are
	 *            documented in.
	 * @return list of all decision knowledge elements documented in the a certain
	 *         comment of a Jira issue. Does also return irrelevant parts of text.
	 */
	public List<KnowledgeElement> getElementsInComment(long commentId) {
		List<KnowledgeElement> elements = new ArrayList<KnowledgeElement>();
		for (PartOfJiraIssueTextInDatabase databaseEntry : ACTIVE_OBJECTS.find(PartOfJiraIssueTextInDatabase.class,
				Query.select().where("PROJECT_KEY = ? AND COMMENT_ID = ?", projectKey, commentId))) {
			elements.add(new PartOfJiraIssueTextImpl(databaseEntry));
		}
		return elements;
	}

	/**
	 * Returns all decision knowledge elements documented in the description of a
	 * Jira issue. The comment id of such elements is zero.
	 *
	 * @param jiraIssueId
	 *            id of the Jira issue that the decision knowledge elements are
	 *            documented in.
	 * @return list of all decision knowledge elements documented in the description
	 *         of a Jira issue.
	 */
	public List<KnowledgeElement> getElementsInDescription(long jiraIssueId) {
		List<KnowledgeElement> elements = getElementsInJiraIssue(jiraIssueId);
		elements.removeIf(e -> (((PartOfJiraIssueText) e).getCommentId() != 0));
		return elements;
	}

	/**
	 * Returns the id of the decision knowledge element documented in the
	 * description or comments of a Jira issue with the summary and type.
	 * 
	 * @param summary
	 *            of the decision knowledge element.
	 * @param jiraIssueId
	 *            id of the Jira issue that the decision knowledge element is
	 *            documented in.
	 * @param type
	 *            {@link KnowledgeType} of the element.
	 * @return id of the decision knowledge element documented in the description or
	 *         comments of a Jira issue with the summary and type.
	 */
	public long getIdOfElement(String summary, long jiraIssueId, KnowledgeType type) {
		if (summary == null || jiraIssueId <= 0 || type == null || summary.isBlank()) {
			return 0;
		}
		List<KnowledgeElement> sentences = getElementsWithTypeInJiraIssue(jiraIssueId, type);
		for (KnowledgeElement sentence : sentences) {
			if (sentence.getSummary().trim().equalsIgnoreCase(summary)) {
				return sentence.getId();
			}
		}
		LOGGER.debug("Nothing found for: " + summary);
		return 0;
	}

	/**
	 * Returns all decision knowledge elements with a certain type documented in the
	 * description or comments of a Jira issue.
	 *
	 * @param jiraIssueId
	 *            id of the Jira issue that the decision knowledge elements are
	 *            documented in.
	 * @param type
	 *            {@link KnowledgeType} of the element.
	 * @return list of all decision knowledge elements with a certain type
	 *         documented in the description or comments of a Jira issue.
	 */
	public List<KnowledgeElement> getElementsWithTypeInJiraIssue(long jiraIssueId, KnowledgeType type) {
		List<KnowledgeElement> elements = getElementsInJiraIssue(jiraIssueId);
		elements.removeIf(e -> (e.getType() != type));
		return elements;
	}

	@Override
	public List<Link> getInwardLinks(KnowledgeElement element) {
		return GenericLinkManager.getInwardLinks(element);
	}

	@Override
	public List<Link> getOutwardLinks(KnowledgeElement element) {
		return GenericLinkManager.getOutwardLinks(element);
	}

	@Override
	public KnowledgeElement insertDecisionKnowledgeElement(KnowledgeElement element, ApplicationUser user,
			KnowledgeElement parentElement) {
		if (element == null || user == null) {
			return null;
		}
		if (parentElement == null) {
			return insertDecisionKnowledgeElement(element, user);
		}
		Issue jiraIssue = parentElement.getJiraIssue();
		if (jiraIssue == null) {
			return null;
		}
		Comment comment = createCommentInJiraIssue(element, jiraIssue, user);
		return insertDecisionKnowledgeElement(new PartOfJiraIssueTextImpl(comment), user);
	}

	/**
	 * Returns the Jira issue that decision knowledge element is documented in
	 * (either in a comment or the description of this Jira issue).
	 * 
	 * @param id
	 *            of the decision knowledge element.
	 * @return Jira issue as an {@link Issue} object.
	 */
	public Issue getJiraIssue(long id) {
		PartOfJiraIssueText sentence = (PartOfJiraIssueTextImpl) this.getDecisionKnowledgeElement(id);
		if (sentence == null) {
			return null;
		}
		return sentence.getJiraIssue();
	}

	private Comment createCommentInJiraIssue(KnowledgeElement element, Issue jiraIssue, ApplicationUser user) {
		String tag = AbstractKnowledgeClassificationMacro.getTag(element.getTypeAsString());
		String text = tag + element.getSummary() + "\n" + element.getDescription() + tag;
		return ComponentAccessor.getCommentManager().create(jiraIssue, user, text, false);
	}

	@Override
	public KnowledgeElement insertDecisionKnowledgeElement(KnowledgeElement element, ApplicationUser user) {
		KnowledgeElement existingElement = checkIfElementExistsInDatabase(element);
		if (existingElement != null) {
			return existingElement;
		}

		PartOfJiraIssueTextInDatabase databaseEntry = ACTIVE_OBJECTS.create(PartOfJiraIssueTextInDatabase.class);

		setParameters((PartOfJiraIssueText) element, databaseEntry);
		databaseEntry.save();

		PartOfJiraIssueText sentence = new PartOfJiraIssueTextImpl(databaseEntry);
		if (sentence.getId() > 0 && sentence.isRelevant()) {
			KnowledgeGraph.getOrCreate(projectKey).addVertex(sentence);
		}
		return sentence;
	}

	private KnowledgeElement checkIfElementExistsInDatabase(KnowledgeElement element) {
		if (element.getDocumentationLocation() != DocumentationLocation.JIRAISSUETEXT) {
			return null;
		}
		KnowledgeElement existingElement = getDecisionKnowledgeElement(element);
		if (existingElement != null) {
			ensureThatElementIsLinked(existingElement);
			return existingElement;
		}
		return null;
	}

	public KnowledgeElement getDecisionKnowledgeElement(KnowledgeElement element) {
		if (element == null) {
			return null;
		}
		if (element.getId() > 0) {
			return this.getDecisionKnowledgeElement(element.getId());
		}

		PartOfJiraIssueText sentence = (PartOfJiraIssueText) element;
		PartOfJiraIssueText sentenceInDatabase = null;
		for (PartOfJiraIssueTextInDatabase databaseEntry : ACTIVE_OBJECTS.find(PartOfJiraIssueTextInDatabase.class,
				Query.select().where("PROJECT_KEY = ? AND COMMENT_ID = ? AND END_POSITION = ? AND START_POSITION = ?",
						sentence.getProject().getProjectKey(), sentence.getCommentId(), sentence.getEndPosition(),
						sentence.getStartPosition()))) {
			sentenceInDatabase = new PartOfJiraIssueTextImpl(databaseEntry);
		}
		return sentenceInDatabase;
	}

	private static void setParameters(PartOfJiraIssueText element, PartOfJiraIssueTextInDatabase databaseEntry) {
		databaseEntry.setProjectKey(element.getProject().getProjectKey());
		databaseEntry.setCommentId(element.getCommentId());
		databaseEntry.setType(element.getTypeAsString());
		databaseEntry.setRelevant(element.isRelevant());
		databaseEntry.setValidated(element.isValidated());
		if (element.isValidated()) {
			classificationTrainer.update(element);
		}
		databaseEntry.setStartPosition(element.getStartPosition());
		databaseEntry.setEndPosition(element.getEndPosition());
		databaseEntry.setJiraIssueId(element.getJiraIssueId());
		databaseEntry.setStatus(element.getStatusAsString());
	}

	@Override
	public boolean updateDecisionKnowledgeElement(KnowledgeElement element, ApplicationUser user) {
		if (element == null) {
			return false;
		}
		PartOfJiraIssueText sentence = new PartOfJiraIssueTextImpl(element);
		sentence.setValidated(true);
		return updatePartOfJiraIssueText(sentence, user);
	}

	public static boolean updatePartOfJiraIssueText(PartOfJiraIssueText newElement, ApplicationUser user) {
		if (newElement == null || newElement.getProject() == null) {
			return false;
		}

		// Get corresponding element from database
		JiraIssueTextPersistenceManager persistenceManager = KnowledgePersistenceManager
				.getOrCreate(newElement.getProject()).getJiraIssueTextManager();
		PartOfJiraIssueText formerElement = (PartOfJiraIssueText) persistenceManager
				.getDecisionKnowledgeElement(newElement);
		if (formerElement == null) {
			return false;
		}
		// only the knowledge type or status has changed
		if (newElement.getSummary() == null) {
			newElement.setSummary(formerElement.getSummary());
			newElement.setDescription(formerElement.getDescription());
		}
		return updateElementInDatabase(newElement, formerElement, user);
	}

	private static boolean updateElementInDatabase(PartOfJiraIssueText newElement, PartOfJiraIssueText formerElement,
			ApplicationUser user) {
		String tag = AbstractKnowledgeClassificationMacro.getTag(newElement.getType());
		String changedPartOfText = tag + newElement.getDescription() + tag;

		String text = "";
		MutableComment mutableComment = formerElement.getComment();
		if (mutableComment == null) {
			text = formerElement.getJiraIssueDescription();
		} else {
			text = mutableComment.getBody();
		}

		String firstPartOfText = text.substring(0, formerElement.getStartPosition());
		String lastPartOfText = text.substring(formerElement.getEndPosition());

		String newBody = firstPartOfText + changedPartOfText + lastPartOfText;

		JiraIssueTextExtractionEventListener.editCommentLock = true;
		if (mutableComment == null) {
			MutableIssue jiraIssue = (MutableIssue) formerElement.getJiraIssue();
			jiraIssue.setDescription(newBody);
			JiraIssuePersistenceManager.updateJiraIssue(jiraIssue, user);
		} else {
			mutableComment.setBody(newBody);
			ComponentAccessor.getCommentManager().update(mutableComment, true);
		}
		JiraIssueTextExtractionEventListener.editCommentLock = false;

		int lengthDifference = changedPartOfText.length() - formerElement.getLength();
		updateSentenceLengthForOtherSentencesInSameComment(formerElement, lengthDifference);

		KnowledgeType newType = KnowledgeStatus.getNewKnowledgeTypeForStatus(newElement);
		KnowledgeStatus newStatus = KnowledgeStatus.getNewKnowledgeStatusForType(formerElement, newElement);

		formerElement.setEndPosition(formerElement.getStartPosition() + changedPartOfText.length());
		formerElement.setType(newElement.getType());
		formerElement.setValidated(newElement.isValidated());
		formerElement.setRelevant(newElement.getType() != KnowledgeType.OTHER);
		formerElement.setStatus(newStatus);
		formerElement.setType(newType);
		// sentence.setCommentId(element.getCommentId());

		return updateInDatabase(formerElement);
	}

	public static boolean updateInDatabase(PartOfJiraIssueText sentence) {
		boolean isUpdated = false;
		for (PartOfJiraIssueTextInDatabase databaseEntry : ACTIVE_OBJECTS.find(PartOfJiraIssueTextInDatabase.class)) {
			if (databaseEntry.getId() == sentence.getId()) {
				setParameters(sentence, databaseEntry);
				databaseEntry.save();
				isUpdated = true;
			}
		}
		return isUpdated;
	}

	public static void updateSentenceLengthForOtherSentencesInSameComment(PartOfJiraIssueText sentence,
			int lengthDifference) {
		for (PartOfJiraIssueTextInDatabase otherSentenceInComment : ACTIVE_OBJECTS.find(
				PartOfJiraIssueTextInDatabase.class, "COMMENT_ID = ? AND JIRA_ISSUE_ID = ?", sentence.getCommentId(),
				sentence.getJiraIssueId())) {
			if (otherSentenceInComment.getStartPosition() > sentence.getStartPosition()
					&& otherSentenceInComment.getId() != sentence.getId()) {
				otherSentenceInComment.setStartPosition(otherSentenceInComment.getStartPosition() + lengthDifference);
				otherSentenceInComment.setEndPosition(otherSentenceInComment.getEndPosition() + lengthDifference);
				otherSentenceInComment.save();
			}
		}
	}

	@Override
	public ApplicationUser getCreator(KnowledgeElement element) {
		PartOfJiraIssueText sentence = (PartOfJiraIssueText) getDecisionKnowledgeElement(element.getId());
		if (sentence == null) {
			LOGGER.error("Element could not be found.");
			return null;
		}
		return sentence.getCreator();
	}

	public boolean createLinksForNonLinkedElements(long jiraIssueId) {
		return createLinksForNonLinkedElements(getElementsInJiraIssue(jiraIssueId));
	}

	public boolean createLinksForNonLinkedElements() {
		return createLinksForNonLinkedElements(getDecisionKnowledgeElements());
	}

	public boolean createLinksForNonLinkedElements(List<KnowledgeElement> elements) {
		boolean areElementsLinked = true;
		for (KnowledgeElement element : elements) {
			areElementsLinked = areElementsLinked && ensureThatElementIsLinked(element);
		}
		return areElementsLinked;
	}

	public boolean ensureThatElementIsLinked(KnowledgeElement element) {
		if (element.isLinked() > 0) {
			return true;
		}
		KnowledgeElement parentElement = new KnowledgeElementImpl(((PartOfJiraIssueText) element).getJiraIssue());
		long linkId = KnowledgePersistenceManager.getOrCreate(projectKey).insertLink(parentElement, element, null);
		return linkId > 0;
	}

	/**
	 * Deletes elements in database that are broken (are neither stored in
	 * description nor in a comment or have zero length).
	 * 
	 * @param user
	 *            Jira {@link ApplicationUser}.
	 * @return true if any element was deleted.
	 */
	public boolean deleteInvalidElements(ApplicationUser user) {
		boolean isAnyElementDeleted = false;
		for (KnowledgeElement element : getDecisionKnowledgeElements()) {
			if (!((PartOfJiraIssueText) element).isValid()) {
				deleteDecisionKnowledgeElement(element, user);
				isAnyElementDeleted = true;
			}
		}
		return isAnyElementDeleted;
	}

	public Issue createJiraIssueFromSentenceObject(long aoId, ApplicationUser user) {
		if (aoId <= 0 || user == null) {
			LOGGER.error("Parameter are Invalid");
			return null;
		}

		PartOfJiraIssueText element = (PartOfJiraIssueText) this.getDecisionKnowledgeElement(aoId);

		JiraIssuePersistenceManager persistenceManager = KnowledgePersistenceManager.getOrCreate(this.projectKey)
				.getJiraIssueManager();
		KnowledgeElement decElement = persistenceManager.insertDecisionKnowledgeElement(element, user);

		MutableIssue issue = ComponentAccessor.getIssueService().getIssue(user, decElement.getId()).getIssue();

		IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
		long linkTypeId = JiraIssuePersistenceManager.getLinkTypeId("contain");

		try {
			issueLinkManager.createIssueLink(element.getJiraIssueId(), issue.getId(), linkTypeId, (long) 0, user);
		} catch (CreateException e) {
			LOGGER.error("Creating JIRA issue from part of text failed. Message: " + e.getMessage());
			return null;
		}

		// delete sentence in comment
		int length = JiraIssueTextPersistenceManager.removeSentenceFromComment(element) * -1; // -1 because we
		// decrease the total number of letters
		updateSentenceLengthForOtherSentencesInSameComment(element, length);

		// delete ao sentence entry
		KnowledgePersistenceManager.getOrCreate(projectKey).deleteDecisionKnowledgeElement(element, null);

		createLinksForNonLinkedElements(element.getJiraIssueId());

		return issue;
	}

	public static List<KnowledgeElement> updateComment(Comment comment) {
		String projectKey = comment.getIssue().getProjectObject().getKey();
		List<PartOfJiraIssueText> partsOfText = new TextSplitterImpl().getPartsOfText(comment.getBody(), projectKey);

		JiraIssueTextPersistenceManager persistenceManager = KnowledgePersistenceManager.getOrCreate(projectKey)
				.getJiraIssueTextManager();

		List<KnowledgeElement> knowledgeElementsInText = persistenceManager.getElementsInComment(comment.getId());

		// @issue Elements used to be deleted and new ones were created afterwards.
		// How to enable a "real" update?
		// @decision Overwrite parts of Jira issue text in AO database if they exist!
		// @con If a new knowledge element is inserted at the beginning of the text, the
		// links in the knowledge graph might be wrong.
		int numberOfTextPartsInComment = knowledgeElementsInText.size();

		// Update AO entries
		for (int i = 0; i < partsOfText.size(); i++) {
			PartOfJiraIssueText sentence = new PartOfJiraIssueTextImpl(partsOfText.get(i), comment);
			if (i < numberOfTextPartsInComment) {
				sentence.setId(knowledgeElementsInText.get(i).getId());
				updateInDatabase(sentence);
			} else {
				sentence = (PartOfJiraIssueText) persistenceManager.insertDecisionKnowledgeElement(sentence, null);
			}
			if (sentence.isRelevant()) {
				AutomaticLinkCreator.createSmartLinkForElement(sentence);
			}
			knowledgeElementsInText.set(i, sentence);
		}
		return knowledgeElementsInText;
	}

	public static List<KnowledgeElement> updateDescription(Issue jiraIssue) {
		String projectKey = jiraIssue.getProjectObject().getKey();
		List<PartOfJiraIssueText> partsOfText = new TextSplitterImpl().getPartsOfText(jiraIssue.getDescription(),
				projectKey);

		JiraIssueTextPersistenceManager persistenceManager = KnowledgePersistenceManager.getOrCreate(projectKey)
				.getJiraIssueTextManager();

		List<KnowledgeElement> parts = persistenceManager.getElementsInDescription(jiraIssue.getId());
		int numberOfTextParts = parts.size();

		for (int i = 0; i < partsOfText.size(); i++) {
			PartOfJiraIssueText sentence = new PartOfJiraIssueTextImpl(partsOfText.get(i), jiraIssue);
			if (i < numberOfTextParts) {
				// Update AO entry
				sentence.setId(parts.get(i).getId());
				updateInDatabase(sentence);
				parts.set(i, sentence);
			} else {
				// Create new AO entry
				sentence = (PartOfJiraIssueText) persistenceManager.insertDecisionKnowledgeElement(sentence, null);
				parts.add(sentence);
			}
			sentence = (PartOfJiraIssueText) persistenceManager.getDecisionKnowledgeElement(sentence);
			if (sentence.isRelevant()) {
				AutomaticLinkCreator.createSmartLinkForElement(sentence);
			}
			KnowledgeGraph.getOrCreate(projectKey).updateElement(sentence);
		}
		return parts;
	}

	private static int removeSentenceFromComment(PartOfJiraIssueText element) {
		MutableComment mutableComment = element.getComment();
		String newBody = mutableComment.getBody();
		newBody = newBody.substring(0, element.getStartPosition()) + newBody.substring(element.getEndPosition());

		JiraIssueTextExtractionEventListener.editCommentLock = true;
		mutableComment.setBody(newBody);
		ComponentAccessor.getCommentManager().update(mutableComment, true);
		JiraIssueTextExtractionEventListener.editCommentLock = false;
		return element.getEndPosition() - element.getStartPosition();
	}

	public List<KnowledgeElement> getUserValidatedPartsOfText(String projectKey) {
		List<KnowledgeElement> validatedPartsOfText = new ArrayList<KnowledgeElement>();
		if (projectKey == null || projectKey.isEmpty()) {
			return validatedPartsOfText;
		}
		PartOfJiraIssueTextInDatabase[] databaseEntries = ACTIVE_OBJECTS.find(PartOfJiraIssueTextInDatabase.class,
				Query.select().where("PROJECT_KEY = ? and VALIDATED = ?", projectKey, true));

		for (PartOfJiraIssueTextInDatabase databaseEntry : databaseEntries) {
			PartOfJiraIssueText validatedPartOfText = new PartOfJiraIssueTextImpl(databaseEntry);
			validatedPartsOfText.add(validatedPartOfText);
		}
		return validatedPartsOfText;
	}

	public List<KnowledgeElement> getUnvalidatedPartsOfText(String projectKey) {
		List<KnowledgeElement> unvalidatedPartsOfText = new ArrayList<KnowledgeElement>();
		if (projectKey == null || projectKey.isEmpty()) {
			return unvalidatedPartsOfText;
		}
		PartOfJiraIssueTextInDatabase[] databaseEntries = ACTIVE_OBJECTS.find(PartOfJiraIssueTextInDatabase.class,
				Query.select().where("PROJECT_KEY = ? and VALIDATED = ?", projectKey, false));

		for (PartOfJiraIssueTextInDatabase databaseEntry : databaseEntries) {
			PartOfJiraIssueText validatedPartOfText = new PartOfJiraIssueTextImpl(databaseEntry);
			unvalidatedPartsOfText.add(validatedPartOfText);
		}
		return unvalidatedPartsOfText;
	}

	/**
	 * Returns the youngest decision knowledge element with a certain knowledge type
	 * that is documented in the description or the comments of a certain Jira
	 * issue.
	 * 
	 * @param jiraIssueId
	 *            id of the Jira issue that the decision knowledge elements are
	 *            documented in.
	 * @param knowledgeType
	 *            {@link KnowledgeType} of the parent element.
	 * @return youngest decision knowledge element with a certain knowledge type
	 *         that is documented in the description or the comments of a certain
	 *         Jira issue.
	 */
	public static KnowledgeElement getYoungestElementForJiraIssue(long jiraIssueId, KnowledgeType knowledgeType) {
		PartOfJiraIssueText youngestElement = null;
		PartOfJiraIssueTextInDatabase[] databaseEntries = ACTIVE_OBJECTS.find(PartOfJiraIssueTextInDatabase.class,
				Query.select().where("JIRA_ISSUE_ID = ?", jiraIssueId).order("ID DESC"));

		for (PartOfJiraIssueTextInDatabase databaseEntry : databaseEntries) {
			if (databaseEntry.getType().equalsIgnoreCase(knowledgeType.toString())) {
				youngestElement = new PartOfJiraIssueTextImpl(databaseEntry);
				break;
			}
		}
		return youngestElement;
	}

	/**
	 * Splits a comment into parts (substrings) and inserts these parts into the
	 * database table.
	 *
	 * @param comment
	 *            Jira issue comment.
	 * @return list of comment sentences with ids in database.
	 * @see PartOfJiraIssueText
	 */
	public static List<PartOfJiraIssueText> insertPartsOfComment(Comment comment) {
		String projectKey = comment.getIssue().getProjectObject().getKey();
		List<PartOfJiraIssueText> partsOfComment = new TextSplitterImpl().getPartsOfText(comment.getBody(), projectKey);

		List<PartOfJiraIssueText> partsOfCommentWithIdInDatabase = new ArrayList<PartOfJiraIssueText>();

		JiraIssueTextPersistenceManager persistenceManager = KnowledgePersistenceManager.getOrCreate(projectKey)
				.getJiraIssueTextManager();

		// Create entries in the active objects (AO) database
		for (PartOfJiraIssueText partOfComment : partsOfComment) {
			partsOfCommentWithIdInDatabase
					.add((PartOfJiraIssueText) persistenceManager.insertDecisionKnowledgeElement(partOfComment, null));
		}
		return partsOfCommentWithIdInDatabase;
	}

	/**
	 * Splits a Jira issue description into parts (substrings) and inserts these
	 * parts into the database table.
	 *
	 * @param issue
	 *            Jira issue.
	 * @return list of sentences with ids in database.
	 * @see PartOfJiraIssueText
	 */
	public static List<PartOfJiraIssueText> insertPartsOfDescription(MutableIssue issue) {
		String projectKey = issue.getProjectObject().getKey();
		// Convert description String to a list of PartOfJiraIssueText
		List<PartOfJiraIssueText> partsOfDescription = new TextSplitterImpl()
				.getPartsOfText(issue.getDescription(), projectKey).stream()
				// The PartOfJiraIssueTextImpl constructor with signature (PartOfText, Issue)
				// creates a Part of Description by setting the ID to 0
				.map(part -> new PartOfJiraIssueTextImpl(part, issue)).collect(Collectors.toList());

		JiraIssueTextPersistenceManager persistenceManager = KnowledgePersistenceManager.getOrCreate(projectKey)
				.getJiraIssueTextManager();

		List<PartOfJiraIssueText> partsOfDescriptionWithIdInDatabase = new ArrayList<PartOfJiraIssueText>();
		for (PartOfJiraIssueText partOfDescription : partsOfDescription) {
			partsOfDescriptionWithIdInDatabase.add((PartOfJiraIssueText) persistenceManager
					.insertDecisionKnowledgeElement(partOfDescription, issue.getReporter()));
		}
		return partsOfDescriptionWithIdInDatabase;
	}

}
