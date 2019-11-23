package de.uhd.ifi.se.decision.management.jira.persistence.knowledgepersistencemanager.singlelocations;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import de.uhd.ifi.se.decision.management.jira.TestSetUp;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.DocumentationLocation;
import de.uhd.ifi.se.decision.management.jira.model.impl.DecisionKnowledgeElementImpl;
import de.uhd.ifi.se.decision.management.jira.persistence.KnowledgePersistenceManager;
import de.uhd.ifi.se.decision.management.jira.persistence.impl.ActiveObjectPersistenceManager;
import de.uhd.ifi.se.decision.management.jira.persistence.impl.JiraIssuePersistenceManager;
import de.uhd.ifi.se.decision.management.jira.persistence.impl.JiraIssueTextPersistenceManager;

public class TestAbstractPersistenceManagerForSingleLocation extends TestSetUp {

	private static DecisionKnowledgeElement element;

	@BeforeClass
	public static void setUp() {
		init();
		element = new DecisionKnowledgeElementImpl();
		element.setProject("TEST");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testProjectKeyNull() {
		KnowledgePersistenceManager.getOrCreate((String) null).getDefaultPersistenceManager();
	}

	@Test
	public void testGetPersistenceStrategyProjectKeyNonExistent() {
		assertTrue(KnowledgePersistenceManager.getOrCreate("TESTNOT")
				.getDefaultPersistenceManager() instanceof JiraIssuePersistenceManager);
	}

	@Test
	public void testGetPersistenceStrategyProjectKeyExistent() {
		assertTrue(KnowledgePersistenceManager.getOrCreate("TEST")
				.getDefaultPersistenceManager() instanceof JiraIssuePersistenceManager);
	}

	@Test
	public void testGetPersistenceManagerElementExistentJiraIssue() {
		element.setDocumentationLocation(DocumentationLocation.JIRAISSUE);
		assertTrue(KnowledgePersistenceManager.getPersistenceManager(element) instanceof JiraIssuePersistenceManager);
	}

	@Test
	public void testGetPersistenceManagerElementExistentActiveObject() {
		element.setDocumentationLocation(DocumentationLocation.ACTIVEOBJECT);
		assertTrue(
				KnowledgePersistenceManager.getPersistenceManager(element) instanceof ActiveObjectPersistenceManager);
	}

	@Test
	public void testGetPersistenceManagerElementExistentJiraIssueComment() {
		element.setDocumentationLocation(DocumentationLocation.JIRAISSUETEXT);
		assertTrue(
				KnowledgePersistenceManager.getPersistenceManager(element) instanceof JiraIssueTextPersistenceManager);
	}
}