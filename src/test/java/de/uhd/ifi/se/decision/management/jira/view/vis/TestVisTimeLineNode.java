package de.uhd.ifi.se.decision.management.jira.view.vis;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.atlassian.jira.component.ComponentAccessor;

import de.uhd.ifi.se.decision.management.jira.TestSetUp;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.impl.KnowledgeElementImpl;
import de.uhd.ifi.se.decision.management.jira.persistence.tables.LinkInDatabase;
import de.uhd.ifi.se.decision.management.jira.persistence.tables.PartOfJiraIssueTextInDatabase;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.DatabaseUpdater;

public class TestVisTimeLineNode extends TestSetUp {
	private KnowledgeElement element;
	private VisTimeLineNode timeNode;
	private String createdString;
	private String closedString;

	@Before
	public void setUp() {
		init();
		element = new KnowledgeElementImpl(ComponentAccessor.getIssueManager().getIssueObject((long) 14));
		element.setCreated(new Date(System.currentTimeMillis() - 1000));
		element.setClosed(new Date(System.currentTimeMillis()));
		timeNode = new VisTimeLineNode(element);
		createdString = timeNode.createDateString(element.getCreated());
		closedString = timeNode.createDateString(element.getClosed());
	}

	public static final class AoSentenceTestDatabaseUpdater implements DatabaseUpdater {
		@SuppressWarnings("unchecked")
		@Override
		public void update(EntityManager entityManager) throws Exception {
			entityManager.migrate(PartOfJiraIssueTextInDatabase.class);
			entityManager.migrate(LinkInDatabase.class);
		}
	}

	@Test
	public void testConstructorNull() {
		VisTimeLineNode node = new VisTimeLineNode(null);
		assertEquals(0, node.getId());
	}

	@Test
	public void testConstructorFilled() {
		VisTimeLineNode node = new VisTimeLineNode(element);
		assertEquals(element.getId(), node.getId());
	}

	@Test
	public void testGetId() {
		assertEquals(element.getId(), timeNode.getId());
	}

	@Test
	public void testSetId() {
		VisTimeLineNode node = new VisTimeLineNode(element);
		node.setId(12345);
		assertEquals(12345, node.getId());
	}

	@Test
	public void testGetContent() {
		assertEquals("<img src=" + '"' + element.getType().getIconUrl() + '"' + "> " + element.getSummary(),
				timeNode.getContent());
	}

	@Test
	public void testSetContent() {
		timeNode.setContent("Test new Content");
		assertEquals("Test new Content", timeNode.getContent());
	}

	@Test
	public void testGetStart() {
		assertEquals(createdString, timeNode.getStart());
	}

	@Test
	public void testSetStart() {
		Date date = new Date(System.currentTimeMillis() - 1000);
		createdString = timeNode.createDateString(date);
		timeNode.setStart(createdString);
		assertEquals(createdString, timeNode.getStart());
	}

	@Test
	public void testGetEnd() {
		assertEquals(closedString, timeNode.getEnd());
	}

	@Test
	public void testSetEnd() {
		Date date = new Date(System.currentTimeMillis() - 1000);
		closedString = timeNode.createDateString(date);
		timeNode.setEnd(closedString);
		assertEquals(closedString, timeNode.getEnd());
	}

	@Test
	public void testGetClassName() {
		assertEquals(element.getTypeAsString().toLowerCase(), timeNode.getClassName());
	}

	@Test
	public void testSetClassName() {
		timeNode.setClassName("Test Class name");
		assertEquals("Test Class name", timeNode.getClassName());
	}

	@Test
	public void testGetAndSetGroup() {
		timeNode.setGroup(19012);
		assertEquals(19012, timeNode.getGroup(), 0.0);
	}

	@Test
	public void testGetAndSetTitle() {
		timeNode.setTitle("TestTitle");
		assertEquals("TestTitle", timeNode.getTitle());
	}
}
