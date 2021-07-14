package de.uhd.ifi.se.decision.management.jira.quality.generalmetrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import de.uhd.ifi.se.decision.management.jira.filtering.FilterSettings;
import de.uhd.ifi.se.decision.management.jira.git.gitclient.TestSetUpGit;

import org.junit.Before;
import org.junit.Test;

import com.atlassian.jira.user.ApplicationUser;

import de.uhd.ifi.se.decision.management.jira.testdata.JiraUsers;
import net.java.ao.test.jdbc.NonTransactional;

public class TestMetricCalculator extends TestSetUpGit {

	private GeneralMetricCalculator calculator;

	@Override
	@Before
	public void setUp() {
		init();
		ApplicationUser user = JiraUsers.SYS_ADMIN.getApplicationUser();
		String projectKey = "TEST";
		FilterSettings filterSettings = new FilterSettings(projectKey, "");
		calculator = new GeneralMetricCalculator(user, filterSettings);
	}

	@Test
	@NonTransactional
	public void testGeneralMetricsCalculator() {
		assertNotNull(calculator);
	}

	@Test
	@NonTransactional
	public void testGetNumberOfCommentsPerIssue() {
		assertEquals(calculator.getNumberOfCommentsPerIssue().size(), 0);
	}

	@Test
	@NonTransactional
	public void testGetDistributionOfKnowledgeTypes() {
		assertEquals(calculator.getDistributionOfKnowledgeTypes().size(), 4);
	}

	@Test
	@NonTransactional
	public void testGetReqAndClassSummary() {
		assertEquals(calculator.getReqAndClassSummary().size(), 2);
	}

	@Test
	@NonTransactional
	public void testGetElementsFromDifferentOrigins() {
		assertEquals(calculator.getElementsFromDifferentOrigins().size(), 4);
	}

	@Test
	@NonTransactional
	public void testGetNumberOfRelevantComments() {
		assertEquals(calculator.getNumberOfRelevantComments().size(), 2);
	}

	@Test
	@NonTransactional
	public void testGetNumberOfCommits() {
		assertEquals(calculator.getNumberOfCommits().size(), 0);
	}

	@Test
	@NonTransactional
	public void testGetDefinitionOfDoneCheckResults() {
		assertEquals(calculator.getDefinitionOfDoneCheckResults().size(), 2);
	}

}
