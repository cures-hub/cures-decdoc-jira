package de.uhd.ifi.se.decision.management.jira.decisionguidance;

import de.uhd.ifi.se.decision.management.jira.TestSetUp;
import de.uhd.ifi.se.decision.management.jira.decisionguidance.knowledgesources.KnowledgeSource;
import de.uhd.ifi.se.decision.management.jira.decisionguidance.knowledgesources.ProjectSource;
import de.uhd.ifi.se.decision.management.jira.decisionguidance.knowledgesources.RDFSource;
import de.uhd.ifi.se.decision.management.jira.decisionguidance.knowledgesources.Recommendation;
import de.uhd.ifi.se.decision.management.jira.decisionguidance.knowledgesources.recommender.BaseRecommender;
import de.uhd.ifi.se.decision.management.jira.decisionguidance.knowledgesources.recommender.SimpleRecommender;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.testdata.JiraProjects;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestSimpleRecommender extends TestSetUp {

	private ProjectSource projectSource;
	private RDFSource rdfSource;

	@Before
	public void setUp() {
		init();
		projectSource = new ProjectSource(JiraProjects.getTestProject().getKey(), "TestProject1", true);
		rdfSource = new RDFSource(JiraProjects.getTestProject().getKey());
	}

	@Test
	public void testSimpleRecommender() {
		List<KnowledgeSource> knowledgeSources = new ArrayList<>();
		knowledgeSources.add(projectSource);
		knowledgeSources.add(rdfSource);

		BaseRecommender simpleRecommender = new SimpleRecommender("feature");
		simpleRecommender.addKnowledgeSource(knowledgeSources);
		List<Recommendation> recommendations = simpleRecommender.getResults();

		assertEquals(2, recommendations.size());
		assertEquals(1, recommendations.get(0).getRecommendations().size());
		assertEquals(10, recommendations.get(1).getRecommendations().size());
	}

}