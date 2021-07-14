package de.uhd.ifi.se.decision.management.jira.quality.completeness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import de.uhd.ifi.se.decision.management.jira.TestSetUp;
import de.uhd.ifi.se.decision.management.jira.filtering.FilterSettings;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.testdata.CodeFiles;
import de.uhd.ifi.se.decision.management.jira.testdata.KnowledgeElements;
import net.java.ao.test.jdbc.NonTransactional;

public class TestRationaleCoverageCalculator extends TestSetUp {

	private RationaleCoverageCalculator calculator;

	@Before
	public void setUp() {
		init();
		CodeFiles.addCodeFilesToKnowledgeGraph();
		FilterSettings filterSettings = new FilterSettings("TEST", "");
		String sourceKnowledgeTypes = "Task";
		calculator = new RationaleCoverageCalculator(filterSettings, sourceKnowledgeTypes);
	}

	@Test
	@NonTransactional
	public void testRationaleCoverageCalculator() {
		assertNotNull(calculator);
	}

	@Test
	@NonTransactional
	public void testRationaleCoverageCalculatorNoTypes() {
		FilterSettings filterSettings = new FilterSettings("TEST", "");
		RationaleCoverageCalculator newCalculator = new RationaleCoverageCalculator(filterSettings, "");
		assertNotNull(newCalculator);
	}

	@Test
	@NonTransactional
	public void testGetDecisionsPerSelectedJiraIssue() {
		assertEquals(4, calculator.getDecisionsPerSelectedJiraIssue().size());
	}

	@Test
	@NonTransactional
	public void testGetIssuesPerSelectedJiraIssue() {
		assertEquals(4, calculator.getIssuesPerSelectedJiraIssue().size());
	}

	@Test
	@NonTransactional
	public void testGetDecisionDocumentedForSelectedJiraIssue() {
		assertEquals(3, calculator.getDecisionDocumentedForSelectedJiraIssue().size());
	}

	@Test
	@NonTransactional
	public void testGetIssueDocumentedForSelectedJiraIssue() {
		assertEquals(3, calculator.getIssueDocumentedForSelectedJiraIssue().size());
	}

	@Test
	@NonTransactional
	public void testCalculateNumberOfDecisionKnowledgeElementsForKnowledgeElement() {
		assertEquals(2, calculator.calculateNumberOfDecisionKnowledgeElementsForKnowledgeElement(
				KnowledgeElements.getTestKnowledgeElement(), KnowledgeType.ISSUE));
	}

	@Test
	@NonTransactional
	public void testCalculateNumberOfDecisionKnowledgeElementsForKnowledgeElementNull() {
		assertEquals(0, calculator.calculateNumberOfDecisionKnowledgeElementsForKnowledgeElement(
				KnowledgeElements.getTestKnowledgeElement(), null));
	}

	@Test
	@NonTransactional
	public void testCalculateNumberOfDecisionKnowledgeElementsForKnowledgeElementNoLinksCorrectType() {
		assertEquals(1, calculator.calculateNumberOfDecisionKnowledgeElementsForKnowledgeElement(new KnowledgeElement(),
				KnowledgeType.OTHER));
	}

	@Test
	@NonTransactional
	public void testCalculateNumberOfDecisionKnowledgeElementsForKnowledgeElementNoLinksIncorrectType() {
		assertEquals(0, calculator.calculateNumberOfDecisionKnowledgeElementsForKnowledgeElement(new KnowledgeElement(),
				KnowledgeType.DECISION));
	}

}
