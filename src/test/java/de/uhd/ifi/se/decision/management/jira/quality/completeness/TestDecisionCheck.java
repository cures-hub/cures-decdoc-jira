package de.uhd.ifi.se.decision.management.jira.quality.completeness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

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

public class TestDecisionCheck extends TestSetUp {
	private KnowledgeElement decision;
	private ApplicationUser user;
	private DecisionCheck decisionCompletenessCheck;

	@Before
	public void setUp() {
		init();
		user = JiraUsers.SYS_ADMIN.getApplicationUser();
		decision = KnowledgeElements.getDecision();
		decisionCompletenessCheck = new DecisionCheck();
	}

	@Test
	@NonTransactional
	public void testIsLinkedToIssue() {
		assertEquals(KnowledgeType.DECISION, decision.getType());
		assertEquals(4, decision.getId());
		KnowledgeElement issue = KnowledgeElements.getSolvedDecisionProblem();
		assertEquals(KnowledgeType.ISSUE, issue.getType());
		assertEquals(2, issue.getId());
		assertNotNull(decision.getLink(issue));
		assertTrue(decisionCompletenessCheck.execute(decision));
	}

	@Test
	@NonTransactional
	public void testIsNotLinkedToIssue() {
		assertEquals(KnowledgeType.DECISION, decision.getType());
		assertEquals(4, decision.getId());

		Set<Link> links = decision.getLinks();
		for (Link link : links) {
			if (link.getOppositeElement(decision).getType() == KnowledgeType.ISSUE) {
				KnowledgeGraph.getInstance("TEST").removeEdge(link);
			}
		}
		assertFalse(decisionCompletenessCheck.execute(decision));
	}

	@Test
	@NonTransactional
	public void testGetFailedCriteria() {
		// set criteria "decision has to be linked to pro argument" in definition of done
		DefinitionOfDone definitionOfDone = new DefinitionOfDone();
		definitionOfDone.setDecisionLinkedToPro(true);
		ConfigPersistenceManager.saveDefinitionOfDone("TEST", definitionOfDone);
		assertFalse(decisionCompletenessCheck.getQualityProblems(decision, definitionOfDone).isEmpty());

		KnowledgeElement pro = JiraIssues.addElementToDataBase(123, KnowledgeType.PRO);
		KnowledgePersistenceManager.getOrCreate("TEST").insertLink(decision, pro, user);

		assertTrue(decisionCompletenessCheck.getQualityProblems(decision, definitionOfDone).isEmpty());

		// restore default
		ConfigPersistenceManager.saveDefinitionOfDone("TEST", new DefinitionOfDone());
	}

	@Test
	@NonTransactional
	public void testIsNotLinkedToPro() {
		DefinitionOfDone definitionOfDone = new DefinitionOfDone();
		definitionOfDone.setDecisionLinkedToPro(true);
		ConfigPersistenceManager.saveDefinitionOfDone("TEST", definitionOfDone);
		assertFalse(decisionCompletenessCheck.execute(decision));

		// restore default
		ConfigPersistenceManager.saveDefinitionOfDone("TEST", new DefinitionOfDone());
	}

	@Test
	@NonTransactional
	public void testIsLinkedToPro() {
		DefinitionOfDone definitionOfDone = new DefinitionOfDone();
		definitionOfDone.setDecisionLinkedToPro(true);
		ConfigPersistenceManager.saveDefinitionOfDone("TEST", definitionOfDone);

		KnowledgeElement pro = JiraIssues.addElementToDataBase(123, KnowledgeType.PRO);
		KnowledgePersistenceManager.getOrCreate("TEST").insertLink(decision, pro, user);

		assertTrue(decisionCompletenessCheck.execute(decision));
	}

	@After
	public void tearDown() {
		// restore default
		ConfigPersistenceManager.saveDefinitionOfDone("TEST", new DefinitionOfDone());
	}
}
