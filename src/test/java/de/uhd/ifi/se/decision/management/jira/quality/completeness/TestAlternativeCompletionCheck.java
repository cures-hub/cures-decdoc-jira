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

public class TestAlternativeCompletionCheck extends TestSetUp {

	private List<KnowledgeElement> elements;
	private KnowledgeElement alternativeElement;

	@Before
	public void setUp() {
		init();
		elements = KnowledgeElements.getTestKnowledgeElements();
		alternativeElement = elements.get(5);
	}

	@Test
	@NonTransactional
	public void testIsLinkedToIssue() {
		assertEquals(alternativeElement.getType(), KnowledgeType.ALTERNATIVE);
		assertEquals(alternativeElement.getId(), 3);
		KnowledgeElement issue = elements.get(3);
		assertEquals(issue.getType(), KnowledgeType.ISSUE);
		assertEquals(issue.getId(), 2);
		assertNotNull(alternativeElement.getLink(issue));
		assertTrue(new AlternativeCompletionCheck().execute(alternativeElement));
	}

	// TODO write test to check when alternative is not linked to an issue
}
