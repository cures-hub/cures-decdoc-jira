package de.uhd.ifi.se.decision.management.jira.persistence.consistency;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import de.uhd.ifi.se.decision.management.jira.TestSetUp;
import de.uhd.ifi.se.decision.management.jira.testdata.JiraIssues;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.List;

import static de.uhd.ifi.se.decision.management.jira.persistence.ConsistencyPersistenceHelper.*;
import static org.junit.Assert.assertEquals;

public class TestDiscardSuggestions extends TestSetUp {

	private List<MutableIssue> issues;

	@Before
	public void setUp() {
		init();
		issues = JiraIssues.getTestJiraIssues();
	}

	@Test
	public void testInsertAndGetDiscardedSuggestion() {
		List<Issue> discardedSuggestions = getDiscardedSuggestions(issues.get(0));

		assertEquals("Before insertion one discarded suggestion should exist.", 0, discardedSuggestions.size());

		addDiscardedSuggestions(issues.get(0), issues.get(1));
		discardedSuggestions = getDiscardedSuggestions(issues.get(0));
		assertEquals("After insertion one discarded suggestion should exist.", 1, discardedSuggestions.size());

		assertEquals("The discarded suggestion should be the inserted issue.", issues.get(1).getKey(), discardedSuggestions.get(0).getKey());
	}

	@Test
	public void testInsertNullAstDiscardedSuggestion() {
		List<Issue> discardedSuggestions = getDiscardedSuggestions(issues.get(0));
		int discardedSuggestionsBeforeNullInsertion = discardedSuggestions.size();
		addDiscardedSuggestions(issues.get(0), null);
		discardedSuggestions = getDiscardedSuggestions(issues.get(0));
		assertEquals("After insertion of null as a discarded suggestion, no additional discarded issue should exist.",
			discardedSuggestionsBeforeNullInsertion, discardedSuggestions.size());

	}

	@AfterEach
	public void cleanDatabase(){
		resetDiscardedSuggestions();
	}
}
