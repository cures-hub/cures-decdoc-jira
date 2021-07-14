package de.uhd.ifi.se.decision.management.jira.quality.completeness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.jgrapht.Graphs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.atlassian.jira.user.ApplicationUser;

import de.uhd.ifi.se.decision.management.jira.TestSetUp;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeGraph;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.Link;
import de.uhd.ifi.se.decision.management.jira.persistence.ConfigPersistenceManager;
import de.uhd.ifi.se.decision.management.jira.persistence.KnowledgePersistenceManager;
import de.uhd.ifi.se.decision.management.jira.testdata.JiraIssues;
import de.uhd.ifi.se.decision.management.jira.testdata.JiraUsers;
import de.uhd.ifi.se.decision.management.jira.testdata.KnowledgeElements;
import net.java.ao.test.jdbc.NonTransactional;

public class TestAlternativeCheck extends TestSetUp {

	private KnowledgeElement alternative;
	private ApplicationUser user;
	private AlternativeCheck alternativeCompletenessCheck;

	@Before
	public void setUp() {
		init();
		user = JiraUsers.SYS_ADMIN.getApplicationUser();
		alternative = KnowledgeElements.getAlternative();
		alternativeCompletenessCheck = new AlternativeCheck();
	}

	@Test
	@NonTransactional
	public void testIsLinkedToIssue() {
		assertEquals(KnowledgeType.ALTERNATIVE, alternative.getType());
		assertEquals(3, alternative.getId());
		KnowledgeElement issue = KnowledgeElements.getSolvedDecisionProblem();
		assertEquals(KnowledgeType.ISSUE, issue.getType());
		assertEquals(2, issue.getId());
		assertNotNull(alternative.getLink(issue));
		assertTrue(alternativeCompletenessCheck.execute(alternative));
	}

	@Test
	@NonTransactional
	public void testIsNotLinkedToIssue() {
		assertEquals(KnowledgeType.ALTERNATIVE, alternative.getType());
		assertEquals(3, alternative.getId());
		KnowledgeElement issue = KnowledgeElements.getSolvedDecisionProblem();
		assertEquals(KnowledgeType.ISSUE, issue.getType());
		assertEquals(2, issue.getId());
		Link linkToIssue = alternative.getLink(issue);
		KnowledgeGraph.getInstance("TEST").removeEdge(linkToIssue);
		linkToIssue = alternative.getLink(issue);
		assertNull(linkToIssue);
		KnowledgeGraph graph = KnowledgeGraph.getInstance(alternative.getProject());
		assertEquals(3, Graphs.neighborSetOf(graph, alternative).size());
		assertFalse(alternativeCompletenessCheck.execute(alternative));
	}

	@Test
	@NonTransactional
	public void testGetFailedCriteria() {
		// set criteria "alternative has to be linked to argument" in definition of done
		DefinitionOfDone definitionOfDone = new DefinitionOfDone();
		definitionOfDone.setAlternativeLinkedToArgument(true);
		ConfigPersistenceManager.saveDefinitionOfDone("TEST", definitionOfDone);
		assertTrue(alternativeCompletenessCheck.getQualityProblems(alternative, definitionOfDone).isEmpty());

		KnowledgeElement alternative = JiraIssues.addElementToDataBase(42, KnowledgeType.ALTERNATIVE);
		assertFalse(alternativeCompletenessCheck.getQualityProblems(alternative, definitionOfDone).isEmpty());
	}

	@Test
	@NonTransactional
	public void testIsLinkedToArgument() {
		// set criteria "alternative has to be linked to argument" in definition of done
		DefinitionOfDone definitionOfDone = new DefinitionOfDone();
		definitionOfDone.setAlternativeLinkedToArgument(true);
		ConfigPersistenceManager.saveDefinitionOfDone("TEST", definitionOfDone);
		assertTrue(alternativeCompletenessCheck.execute(alternative));

		KnowledgeElement alternative = JiraIssues.addElementToDataBase(42, KnowledgeType.ALTERNATIVE);
		assertFalse(alternativeCompletenessCheck.execute(alternative));
	}

	@Test
	@NonTransactional
	public void testIsLinkedToProArgument() {
		// set criteria "alternative has to be linked to argument" in definition of done
		DefinitionOfDone definitionOfDone = new DefinitionOfDone();
		definitionOfDone.setAlternativeLinkedToArgument(true);
		ConfigPersistenceManager.saveDefinitionOfDone("TEST", definitionOfDone);

		// link alternative to a pro-argument
		KnowledgeElement issue = KnowledgeElements.getSolvedDecisionProblem();
		KnowledgeElement alternative = JiraIssues.addElementToDataBase(42, KnowledgeType.ALTERNATIVE);
		KnowledgePersistenceManager.getOrCreate("TEST").insertLink(issue, alternative, user);
		KnowledgeElement proArgument = JiraIssues.addElementToDataBase(342, KnowledgeType.PRO);
		KnowledgePersistenceManager.getOrCreate("TEST").insertLink(proArgument, alternative, user);
		assertNotNull(alternative.getLink(proArgument));
		assertTrue(alternativeCompletenessCheck.execute(alternative));
	}

	@Test
	@NonTransactional
	public void testIsLinkedToConArgument() {
		// set criteria "alternative has to be linked to argument" in definition of done
		DefinitionOfDone definitionOfDone = new DefinitionOfDone();
		definitionOfDone.setAlternativeLinkedToArgument(true);
		ConfigPersistenceManager.saveDefinitionOfDone("TEST", definitionOfDone);

		// link alternative to a con-argument
		KnowledgeElement issue = KnowledgeElements.getSolvedDecisionProblem();
		KnowledgeElement alternative = JiraIssues.addElementToDataBase(42, KnowledgeType.ALTERNATIVE);
		KnowledgePersistenceManager.getOrCreate("TEST").insertLink(issue, alternative, user);
		KnowledgeElement conArgument = JiraIssues.addElementToDataBase(344, KnowledgeType.CON);
		KnowledgePersistenceManager.getOrCreate("TEST").insertLink(conArgument, alternative, user);
		assertNotNull(alternative.getLink(conArgument));
		assertTrue(alternativeCompletenessCheck.execute(alternative));
	}

	@After
	public void tearDown() {
		// restore default
		ConfigPersistenceManager.saveDefinitionOfDone("TEST", new DefinitionOfDone());
	}
}
