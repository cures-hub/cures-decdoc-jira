package de.uhd.ifi.se.decision.management.jira.view.treeviewer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.user.ApplicationUser;

import de.uhd.ifi.se.decision.management.jira.TestComponentGetter;
import de.uhd.ifi.se.decision.management.jira.TestSetUpWithIssues;
import de.uhd.ifi.se.decision.management.jira.extraction.model.Comment;
import de.uhd.ifi.se.decision.management.jira.extraction.model.TestComment;
import de.uhd.ifi.se.decision.management.jira.extraction.model.impl.CommentImpl;
import de.uhd.ifi.se.decision.management.jira.mocks.MockTransactionTemplate;
import de.uhd.ifi.se.decision.management.jira.mocks.MockUserManager;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElementImpl;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.persistence.AbstractPersistenceStrategy;
import de.uhd.ifi.se.decision.management.jira.persistence.StrategyProvider;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.NonTransactional;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;

@net.java.ao.test.jdbc.Data(TestSetUpWithIssues.AoSentenceTestDatabaseUpdater.class)
@RunWith(ActiveObjectsJUnitRunner.class)
public class TestTreeViewer extends TestSetUpWithIssues {
	private EntityManager entityManager;
	private AbstractPersistenceStrategy persistenceStrategy;

	private boolean multiple;
	private boolean checkCallback;
	private Map<String, Boolean> themes;
	private Set<Data> data;
	private TreeViewer treeViewer;

	@Before
	public void setUp() {
		TestComponentGetter.init(new TestActiveObjects(entityManager), new MockTransactionTemplate(),
				new MockUserManager());
		initialization();
		multiple = false;
		checkCallback = true;
		themes = new HashMap<>();
		themes.put("Test", false);
		data = new HashSet<Data>();
		data.add(new Data());
		treeViewer = new TreeViewer("TEST");
		treeViewer.setMultiple(multiple);
		treeViewer.setCheckCallback(checkCallback);
		treeViewer.setThemes(themes);
		treeViewer.setData(data);
		persistenceStrategy = StrategyProvider.getPersistenceStrategy("TEST");
	}

	@Test
	@NonTransactional
	public void testIsMultiple() {
		assertEquals(treeViewer.isMultiple(), multiple);
	}

	@Test
	@NonTransactional
	public void testisCheckCallBack() {
		assertEquals(treeViewer.isCheckCallback(), checkCallback);
	}

	@Test
	@NonTransactional
	public void testGetThemes() {
		assertEquals(treeViewer.getThemes(), themes);
	}

	@Test
	@NonTransactional
	public void testGetData() {
		assertEquals(treeViewer.getData(), data);
	}

	@Test
	@NonTransactional
	public void testSetMultiple() {
		treeViewer.setMultiple(true);
		assertEquals(treeViewer.isMultiple(), true);
	}

	@Test
	@NonTransactional
	public void testSetCheckCallback() {
		treeViewer.setCheckCallback(true);
		assertEquals(treeViewer.isCheckCallback(), true);
	}

	@Test
	@NonTransactional
	public void testSetThemes() {
		Map<String, Boolean> newThemes = new ConcurrentHashMap<>();
		treeViewer.setThemes(newThemes);
		assertEquals(treeViewer.getThemes(), newThemes);
	}

	@Test
	@NonTransactional
	public void testSetData() {
		HashSet<Data> newData = new HashSet<Data>();
		treeViewer.setData(newData);
		assertEquals(treeViewer.getData(), newData);
	}

	@Test
	@NonTransactional
	public void testGetDataStructureNull() {
		assertEquals(Data.class, treeViewer.getDataStructure(null).getClass());
	}

	@Test(expected = NullPointerException.class)
	@NonTransactional
	public void testGetDataStructureEmpty() {
		DecisionKnowledgeElement element = new DecisionKnowledgeElementImpl();
		System.out.println(treeViewer.getDataStructure(element));
	}

	@Test
	@NonTransactional
	public void testGetDataStructureFilled() {
		DecisionKnowledgeElement element = persistenceStrategy.getDecisionKnowledgeElement((long) 14);
		assertEquals("14", treeViewer.getDataStructure(element).getId());
	}

	@Test
	@NonTransactional
	public void testEmptyConstructor() {
		assertNotNull(new TreeViewer());
	}

	@Test
	@NonTransactional
	public void testEmptyGraphGetDataStructure() {
		TreeViewer tree = new TreeViewer();
		DecisionKnowledgeElement element = persistenceStrategy.getDecisionKnowledgeElement((long) 14);
		assertEquals("14", tree.getDataStructure(element).getId());
	}

	@Test
	@NonTransactional
	public void testTreeViewerWithComment() {
		TreeViewer tree = new TreeViewer();
		TestComment tc = new TestComment();
		Comment comment = tc.getComment("This is a testcomment with some text");
		comment.getSentences().get(0).setKnowledgeTypeString(KnowledgeType.ALTERNATIVE.toString());
		assertNotNull(tree.getDataStructure(comment.getSentences().get(0)));
	}

	// TODO Why does this test fail?
	@Test
	@NonTransactional
	@Ignore
	public void testTreeViewerCalledFromTabpanel() {
		//1) Check if Tree Element has no Children - Important! 
		DecisionKnowledgeElement element = persistenceStrategy.getDecisionKnowledgeElement((long) 14);
		TreeViewer tv = new TreeViewer(element.getKey(), true);
		assertNotNull(tv);
		assertEquals(0, tv.getDataStructure(element).getChildren().size());
		
		//2) Add comment to issue
		MutableIssue issue = ComponentAccessor.getIssueManager().getIssueByCurrentKey("14");
		ComponentAccessor.getCommentManager().deleteCommentsForIssue(issue);
		ApplicationUser currentUser = ComponentAccessor.getUserManager().getUserByName("NoFails");
		CommentManager commentManager = ComponentAccessor.getCommentManager();
		com.atlassian.jira.issue.comments.Comment comment1 = commentManager.create(issue, currentUser, "This is a testsentence for test purposes", true);
		
		//3) Manipulate Sentence object so it will be shown in the tree viewer
		Comment comment = new CommentImpl(comment1);
		comment.getSentences().get(0).setRelevant(true);
		comment.getSentences().get(0).setKnowledgeTypeString(KnowledgeType.ALTERNATIVE.toString());
		element = persistenceStrategy.getDecisionKnowledgeElement((long) 14);
		tv = new TreeViewer(element.getKey(), true);
		
		//4) Check if TreeViewer has one element
		assertNotNull(tv);
		assertEquals(1, tv.getData().size());

		ComponentAccessor.getCommentManager().deleteCommentsForIssue(issue);
	}

	@Test
	@NonTransactional
	public void testTreeViewerCalledFromTabpanelNullData() {
		TreeViewer tv = new TreeViewer(null, true);
		assertNotNull(tv);
		assertEquals(tv.getData(), null);
	}

	@Test
	@NonTransactional
	public void testTreeViewerCalledFromTabpanelEmptyData() {
		TreeViewer tv = new TreeViewer("", true);
		assertNotNull(tv);
	}

}
