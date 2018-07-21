package de.uhd.ifi.se.decision.management.jira.model;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * Test class for a JIRA project with the configuration settings used in this plug-in
 */
public class TestDecisionKnowledgeProject {

	private DecisionKnowledgeProject project;
	private String projectKey;
	private String projectName;
	private boolean isActivated;
	private boolean isIssueStrategy;

	@Before
	public void setUp() {
		this.projectKey = "TestKey";
		this.projectName = "TestName";
		this.isActivated = true;
		this.isIssueStrategy = true;
		this.project = new DecisionKnowledgeProjectImpl(projectKey, projectName);
	}

	@Test
	public void testGetProjectKey() {
		assertEquals(this.projectKey, this.project.getProjectKey());
	}

	@Test
	public void testGetProjectName() {
		assertEquals(this.projectName, this.project.getProjectName());
	}

	@Test
	public void testIsActivated() {
		assertEquals(this.isActivated, this.project.isActivated());
	}

	@Test
	public void testIsIssueStrategy() {
		assertEquals(this.isIssueStrategy, this.project.isIssueStrategy());
	}

	@Test
	public void testSetProjectKey() {
		this.project.setProjectKey(this.projectKey + "New");
		assertEquals(this.projectKey + "New", this.project.getProjectKey());
	}

	@Test
	public void testSetProjectName() {
		this.project.setProjectName(this.projectName + "New");
		assertEquals(this.projectName + "New", this.project.getProjectName());
	}

	@Test
	public void testSetActivated() {
		this.project.setActivated(this.isActivated);
		assertEquals(this.isActivated, this.project.isActivated());
	}

	@Test
	public void testSetIssueStrategy() {
		this.project.setIssueStrategy(this.isIssueStrategy);
		assertEquals(this.isIssueStrategy, this.project.isIssueStrategy());
	}
}