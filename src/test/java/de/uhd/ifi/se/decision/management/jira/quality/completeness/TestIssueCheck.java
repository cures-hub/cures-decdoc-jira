package de.uhd.ifi.se.decision.management.jira.quality.completeness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.uhd.ifi.se.decision.management.jira.TestSetUp;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeGraph;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeStatus;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.Link;
import de.uhd.ifi.se.decision.management.jira.persistence.ConfigPersistenceManager;
import de.uhd.ifi.se.decision.management.jira.testdata.KnowledgeElements;
import net.java.ao.test.jdbc.NonTransactional;

public class TestIssueCheck extends TestSetUp {

	private KnowledgeElement issue;
	private IssueCheck issueCompletenessCheck;

	@Before
	public void setUp() {
		init();
		issueCompletenessCheck = new IssueCheck();
		issue = KnowledgeElements.getSolvedDecisionProblem();
	}

	@Test
	@NonTransactional
	public void testIsLinkedToDecision() {
		assertEquals(KnowledgeType.ISSUE, issue.getType());
		assertEquals(2, issue.getId());
		KnowledgeElement decision = KnowledgeElements.getDecision();
		assertEquals(KnowledgeType.DECISION, decision.getType());
		assertEquals(4, decision.getId());
		assertNotNull(issue.getLink(decision));
		issue.setStatus(KnowledgeStatus.RESOLVED);
		assertTrue(issueCompletenessCheck.execute(issue));
	}

	@Test
	@NonTransactional
	public void testIsLinkedToAlternative() {
		KnowledgeElement alternative = KnowledgeElements.getAlternative();
		assertEquals(KnowledgeType.ALTERNATIVE, alternative.getType());
		assertEquals(3, alternative.getId());
		assertNotNull(issue.getLink(alternative));
		issue.setStatus(KnowledgeStatus.RESOLVED);
		assertTrue(new IssueCheck().execute(issue));
	}

	@Test
	@NonTransactional
	public void testIsNotLinkedToDecision() {
		assertEquals(KnowledgeType.ISSUE, issue.getType());
		assertEquals(2, issue.getId());
		KnowledgeElement decision = KnowledgeElements.getDecision();
		assertEquals(KnowledgeType.DECISION, decision.getType());
		assertEquals(4, decision.getId());

		Link linkToDecision = issue.getLink(decision);
		assertNotNull(linkToDecision);

		linkToDecision = KnowledgeElements.getUnsolvedDecisionProblem().getLink(decision);
		assertNull(linkToDecision);

		KnowledgeGraph graph = KnowledgeGraph.getInstance(issue.getProject());
		assertFalse(graph.containsEdge(linkToDecision));
		assertFalse(issueCompletenessCheck.execute(KnowledgeElements.getUnsolvedDecisionProblem()));
	}

	@Test
	@NonTransactional
	public void testIsCompleteAccordingToSettings() {
		// set criteria "issue has to be linked to alternative" in definition of done
		DefinitionOfDone definitionOfDone = new DefinitionOfDone();
		definitionOfDone.setIssueLinkedToAlternative(true);
		ConfigPersistenceManager.saveDefinitionOfDone("TEST", definitionOfDone);
		issue.setStatus(KnowledgeStatus.RESOLVED);
		assertTrue(issueCompletenessCheck.execute(issue));
		assertFalse(issueCompletenessCheck.execute(KnowledgeElements.getUnsolvedDecisionProblem()));
	}

	@Test
	@NonTransactional
	public void testGetFailedCriteria() {
		// set criteria "issue has to be linked to alternative" in definition of done
		DefinitionOfDone definitionOfDone = new DefinitionOfDone();
		definitionOfDone.setIssueLinkedToAlternative(true);
		ConfigPersistenceManager.saveDefinitionOfDone("TEST", definitionOfDone);
		issue.setStatus(KnowledgeStatus.RESOLVED);
		assertTrue(issueCompletenessCheck.getQualityProblems(issue, definitionOfDone).isEmpty());
		assertFalse(issueCompletenessCheck.getQualityProblems(KnowledgeElements.getUnsolvedDecisionProblem(), definitionOfDone).isEmpty());
	}

	@After
	public void tearDown() {
		// restore default
		ConfigPersistenceManager.saveDefinitionOfDone("TEST", new DefinitionOfDone());
	}
}
