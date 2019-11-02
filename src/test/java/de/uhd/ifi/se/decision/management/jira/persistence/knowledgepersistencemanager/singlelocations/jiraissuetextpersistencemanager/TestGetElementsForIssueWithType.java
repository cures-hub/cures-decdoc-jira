package de.uhd.ifi.se.decision.management.jira.persistence.knowledgepersistencemanager.singlelocations.jiraissuetextpersistencemanager;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.atlassian.jira.user.ApplicationUser;

import de.uhd.ifi.se.decision.management.jira.TestSetUp;
import de.uhd.ifi.se.decision.management.jira.model.text.PartOfJiraIssueText;
import de.uhd.ifi.se.decision.management.jira.persistence.impl.JiraIssueTextPersistenceManager;
import de.uhd.ifi.se.decision.management.jira.testdata.JiraIssues;
import de.uhd.ifi.se.decision.management.jira.testdata.JiraUsers;
import net.java.ao.test.jdbc.NonTransactional;

public class TestGetElementsForIssueWithType extends TestSetUp {

	protected static JiraIssueTextPersistenceManager manager;
	protected static ApplicationUser user;

	@Before
	public void setUp() {
		init();
		manager = new JiraIssueTextPersistenceManager("TEST");
		user = JiraUsers.SYS_ADMIN.getApplicationUser();
	}

	@Test
	@NonTransactional
	public void testIdLessSProjectKeyNullTypeNull() {
		assertEquals(0, JiraIssueTextPersistenceManager.getElementsForJiraIssueWithType(-1, null, null).size());
	}

	@Test
	@NonTransactional
	public void testIdZeroSProjectKeyNullTypeNull() {
		assertEquals(0, JiraIssueTextPersistenceManager.getElementsForJiraIssueWithType(0, null, null).size());
	}

	@Test
	@NonTransactional
	public void testIdMoreSProjectKeyNullTypeNull() {
		assertEquals(0, JiraIssueTextPersistenceManager.getElementsForJiraIssueWithType(1, null, null).size());
	}

	@Test
	@NonTransactional
	public void testIdLessSProjectKeyFilledTypeNull() {
		assertEquals(0, JiraIssueTextPersistenceManager.getElementsForJiraIssueWithType(-1, "TEST", null).size());
	}

	@Test
	@NonTransactional
	public void testIdZeroSProjectKeyFilledTypeNull() {
		assertEquals(0, JiraIssueTextPersistenceManager.getElementsForJiraIssueWithType(0, "TEST", null).size());
	}

	@Test
	@NonTransactional
	public void testIdMoreSProjectKeyFilledTypeNull() {
		assertEquals(0, JiraIssueTextPersistenceManager.getElementsForJiraIssueWithType(1, "TEST", null).size());
	}

	@Test
	@NonTransactional
	public void testIdLessSProjectKeyNullTypeFilled() {
		assertEquals(0, JiraIssueTextPersistenceManager.getElementsForJiraIssueWithType(-1, null, "decision").size());
	}

	@Test
	@NonTransactional
	public void testIdZeroSProjectKeyNullTypeFilled() {
		assertEquals(0, JiraIssueTextPersistenceManager.getElementsForJiraIssueWithType(0, null, "decision").size());
	}

	@Test
	@NonTransactional
	public void testIdMoreSProjectKeyNullTypeFilled() {
		assertEquals(0, JiraIssueTextPersistenceManager.getElementsForJiraIssueWithType(1, null, "decision").size());
	}

	@Test
	@NonTransactional
	public void testIdLessSProjectKeyFilledTypeFilled() {
		assertEquals(0, JiraIssueTextPersistenceManager.getElementsForJiraIssueWithType(-1, "TEST", "decision").size());
	}

	@Test
	@NonTransactional
	public void testIdZeroSProjectKeyFilledTypeFilled() {
		assertEquals(0, JiraIssueTextPersistenceManager.getElementsForJiraIssueWithType(0, "TEST", "decision").size());
	}

	@Test
	@NonTransactional
	public void testIdMoreSProjectKeyFilledTypeFilled() {
		List<PartOfJiraIssueText> comment = JiraIssues.getSentencesForCommentText(
				"some sentence in front. {issue} testobject {issue} some sentence in the back.");
		manager.insertDecisionKnowledgeElement(comment.get(1), null);

		assertEquals(1, JiraIssueTextPersistenceManager
				.getElementsForJiraIssueWithType(comment.get(0).getJiraIssueId(), "TEST", "Issue").size());
	}
}
