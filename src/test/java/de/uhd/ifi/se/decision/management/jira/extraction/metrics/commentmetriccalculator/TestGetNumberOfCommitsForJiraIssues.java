package de.uhd.ifi.se.decision.management.jira.extraction.metrics.commentmetriccalculator;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.uhd.ifi.se.decision.management.jira.TestSetUpWithIssues;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.NonTransactional;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(TestSetUpWithIssues.AoSentenceTestDatabaseUpdater.class)
public class TestGetNumberOfCommitsForJiraIssues extends TestSetupCalculator {

	@Test
	@NonTransactional
	@Ignore
	// TODO Mock ApplicationLinkService
	public void testCase() {
		assertEquals(1, calculator.getNumberOfCommitsForJiraIssues().size(), 0.0);
	}
}
