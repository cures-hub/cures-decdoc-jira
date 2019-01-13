package de.uhd.ifi.se.decision.management.jira.persistence.jiraissuecommentpersistencemanager;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.user.ApplicationUser;

import de.uhd.ifi.se.decision.management.jira.TestComponentGetter;
import de.uhd.ifi.se.decision.management.jira.TestSetUpWithIssues;
import de.uhd.ifi.se.decision.management.jira.extraction.model.TestComment;
import de.uhd.ifi.se.decision.management.jira.mocks.MockTransactionTemplate;
import de.uhd.ifi.se.decision.management.jira.mocks.MockUserManager;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.DocumentationLocation;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.Sentence;
import de.uhd.ifi.se.decision.management.jira.model.impl.DecisionKnowledgeElementImpl;
import de.uhd.ifi.se.decision.management.jira.model.impl.SentenceImpl;
import de.uhd.ifi.se.decision.management.jira.persistence.JiraIssueCommentPersistenceManager;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.NonTransactional;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(TestSetUpWithIssues.AoSentenceTestDatabaseUpdater.class)
public class TestJiraIssueCommentPersistenceManagerSetUp extends TestSetUpWithIssues {

	private EntityManager entityManager;

	protected JiraIssueCommentPersistenceManager manager;
	protected ApplicationUser user;
	protected Sentence element;
	protected Comment comment1;
	protected DecisionKnowledgeElement decisionKnowledgeElement;

	@Before
	public void setUp() {
		initialization();
		TestComponentGetter.init(new TestActiveObjects(entityManager), new MockTransactionTemplate(),
				new MockUserManager());
		createLocalIssue();
		manager = new JiraIssueCommentPersistenceManager("TEST");
		user = ComponentAccessor.getUserManager().getUserByName("NoFails");
		addElementToDataBase();
		addDecisionKnowledgeElement();
	}

	protected void addElementToDataBase() {
		element = new SentenceImpl();
		element.setProject("TEST");
		element.setIssueId(12);
		element.setId(1);
		element.setKey("TEST-12231");
		element.setType("Argument");
		element.setProject("TEST");
		element.setDescription("Old");
		element.setDocumentationLocation(DocumentationLocation.JIRAISSUECOMMENT);
		JiraIssueCommentPersistenceManager.insertDecisionKnowledgeElement(element, user);
	}

	private void addDecisionKnowledgeElement() {
		decisionKnowledgeElement = new DecisionKnowledgeElementImpl();
		decisionKnowledgeElement.setProject("TEST");
		decisionKnowledgeElement.setId(1232);
		decisionKnowledgeElement.setKey("TEST-1232");
		decisionKnowledgeElement.setType("DECISION");
		decisionKnowledgeElement.setProject("TEST");
		decisionKnowledgeElement.setDescription("Old");
	}

	protected void addCommentsToIssue(String comment) {

		ComponentAccessor.getCommentManager().deleteCommentsForIssue(issue);
		// Get the current logged in user
		ApplicationUser currentUser = ComponentAccessor.getUserManager().getUserByName("NoFails");
		// Get access to the Jira comment and component manager
		CommentManager commentManager = ComponentAccessor.getCommentManager();
		// Get the last comment entered in on the issue to a String
		comment1 = commentManager.create(issue, currentUser, comment, true);
	}

	@Test
	@NonTransactional
	public void testGetAllElementsFromAoByType() {
		TestComment tc = new TestComment();
		List<Sentence> comment = tc.getSentencesForCommentText(
				"some sentence in front. {issue} testobject {issue} some sentence in the back.");
		JiraIssueCommentPersistenceManager.insertDecisionKnowledgeElement(comment.get(1), null);

		List<DecisionKnowledgeElement> listWithObjects = new JiraIssueCommentPersistenceManager("TEST")
				.getDecisionKnowledgeElements(KnowledgeType.ISSUE);
		assertEquals(1, listWithObjects.size());
	}

	@Test
	@NonTransactional
	public void testGetAllElementsFromAoByArgumentType() {
		TestComment tc = new TestComment();
		List<Sentence> comment = tc.getSentencesForCommentText(
				"some sentence in front. {pro} testobject {pro} some sentence in the back.");
		JiraIssueCommentPersistenceManager.insertDecisionKnowledgeElement(comment.get(1), null);

		List<DecisionKnowledgeElement> listWithObjects = new JiraIssueCommentPersistenceManager("TEST")
				.getDecisionKnowledgeElements(KnowledgeType.PRO);
		assertEquals(1, listWithObjects.size());
	}

	@Test
	@NonTransactional
	public void testGetAllElementsFromAoByEmptyType() {
		TestComment tc = new TestComment();
		List<Sentence> comment = tc.getSentencesForCommentText(
				"some sentence in front.  {pro} testobject {pro} some sentence in the back.");
		JiraIssueCommentPersistenceManager.insertDecisionKnowledgeElement(comment.get(1), null);

		List<DecisionKnowledgeElement> listWithObjects = new JiraIssueCommentPersistenceManager("TEST")
				.getDecisionKnowledgeElements(KnowledgeType.OTHER);
		assertEquals(3, listWithObjects.size());
	}
}
