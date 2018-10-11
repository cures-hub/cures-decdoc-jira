package de.uhd.ifi.se.decision.management.jira.persistence.issuestrategy;

import org.junit.Before;

import de.uhd.ifi.se.decision.management.jira.TestSetUpWithIssues;
import de.uhd.ifi.se.decision.management.jira.persistence.IssueStrategy;

public class TestIssueStrategySetUp extends TestSetUpWithIssues {

	protected IssueStrategy issueStrategy;
	protected int numberOfElements;

	@Before
	public void setUp() {
		initialization();
		issueStrategy = new IssueStrategy("TEST");
		numberOfElements = issueStrategy.getDecisionKnowledgeElements().size();
	}
}
