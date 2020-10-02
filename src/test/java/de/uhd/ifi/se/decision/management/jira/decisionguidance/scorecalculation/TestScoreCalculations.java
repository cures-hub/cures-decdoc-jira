package de.uhd.ifi.se.decision.management.jira.decisionguidance.scorecalculation;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.uhd.ifi.se.decision.management.jira.TestSetUp;
import de.uhd.ifi.se.decision.management.jira.decisionguidance.knowledgesources.KnowledgeSourceType;
import de.uhd.ifi.se.decision.management.jira.decisionguidance.scorecalculator.DBPediaScoreCalculator;
import de.uhd.ifi.se.decision.management.jira.decisionguidance.scorecalculator.ProjectScoreCalculator;
import de.uhd.ifi.se.decision.management.jira.decisionguidance.scorecalculator.ScoreCalculator;
import de.uhd.ifi.se.decision.management.jira.decisionguidance.scorecalculator.ScoreCalculatorFactory;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.testdata.KnowledgeElements;
import de.uhd.ifi.se.decision.management.jira.view.decisionguidance.DBPediaRecommendation;
import de.uhd.ifi.se.decision.management.jira.view.decisionguidance.ProjectRecommendation;
import de.uhd.ifi.se.decision.management.jira.view.decisionguidance.Recommendation;

public class TestScoreCalculations extends TestSetUp {

	ScoreCalculator scoreCalculator;
	ScoreCalculatorFactory scoreCalculatorFactory;

	@Before
	public void setUp() {
		init();
	}

	@Test
	public void testScoreCalculatorFactory() {
		scoreCalculatorFactory = new ScoreCalculatorFactory(KnowledgeSourceType.PROJECT);
		scoreCalculator = scoreCalculatorFactory.createScoreCalculator();
		assertNotEquals(null, scoreCalculator);
		assertEquals(ProjectScoreCalculator.class, scoreCalculator.getClass());

		scoreCalculatorFactory = new ScoreCalculatorFactory(KnowledgeSourceType.RDF);
		scoreCalculator = scoreCalculatorFactory.createScoreCalculator();
		assertNotEquals(null, scoreCalculator);
		assertEquals(DBPediaScoreCalculator.class, scoreCalculator.getClass());

		scoreCalculatorFactory = new ScoreCalculatorFactory(KnowledgeSourceType.UNDEFINED);
		scoreCalculator = scoreCalculatorFactory.createScoreCalculator();
		assertEquals(null, scoreCalculator);

		scoreCalculatorFactory = new ScoreCalculatorFactory(null);
		scoreCalculator = scoreCalculatorFactory.createScoreCalculator();
		assertEquals(null, scoreCalculator);
	}

	@Test
	public void testCalculateProjectScore() {
		scoreCalculator = new ProjectScoreCalculator();
		KnowledgeElement rootIssue = KnowledgeElements.getTestKnowledgeElements().get(3);

		List<String> keywords = new ArrayList<>();
		keywords.add("feature");

		ProjectRecommendation recommendation = new ProjectRecommendation("TEST", "Test recommendation", keywords,
				rootIssue, "");

		assertEquals(100, scoreCalculator.calculateScore(recommendation));

		keywords.add("feature otherword");

		recommendation.setKeywords(keywords);

		assertEquals(50, scoreCalculator.calculateScore(recommendation));
	}

	@Test
	public void testCalculateDBPedia() {
		scoreCalculator = new DBPediaScoreCalculator();

		Recommendation recommendation = new DBPediaRecommendation("TEST", "Test recommendation", "MYSQL_NOSQL",
				"How could we use MYSQL Database?", "");

		assertEquals(17, scoreCalculator.calculateScore(recommendation));

	}

}