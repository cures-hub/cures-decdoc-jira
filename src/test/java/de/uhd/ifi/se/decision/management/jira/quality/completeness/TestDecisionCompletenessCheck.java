package de.uhd.ifi.se.decision.management.jira.quality.completeness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.uhd.ifi.se.decision.management.jira.TestSetUp;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.testdata.KnowledgeElements;
import net.java.ao.test.jdbc.NonTransactional;

public class TestDecisionCompletenessCheck extends TestSetUp {
	private List<KnowledgeElement> elements;
	private KnowledgeElement decisionElement;

	@Before
	public void setUp() {
		init();
		elements = KnowledgeElements.getTestKnowledgeElements();
		decisionElement = elements.get(6);
	}

	@Test
	@NonTransactional
	public void testIsLinkedToIssue() {
		assertEquals(decisionElement.getType(), KnowledgeType.DECISION);
		assertEquals(decisionElement.getId(), 4);
		KnowledgeElement issue = elements.get(3);
		assertEquals(issue.getType(), KnowledgeType.ISSUE);
		assertEquals(issue.getId(), 2);
		assertNotNull(decisionElement.getLink(issue));
		assertTrue(new DecisionCompletenessCheck().execute(decisionElement));
	}

	// TODO write test to check when decision is not linked to an issue
}