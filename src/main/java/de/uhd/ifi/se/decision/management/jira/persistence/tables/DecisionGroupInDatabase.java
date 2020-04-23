package de.uhd.ifi.se.decision.management.jira.persistence.tables;

import java.sql.SQLException;

import de.uhd.ifi.se.decision.management.jira.model.Link;
import net.java.ao.RawEntity;
import net.java.ao.schema.AutoIncrement;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.Table;

/**
 * Interface for groups of decisison and solution options (alternatives).
 * Determines which table columns are used for object relational mapping of link
 * objects to the database.
 * 
 * @see Link
 */
@Table("CondecDecGroup")
public interface DecisionGroupInDatabase extends RawEntity<Integer> {
	@AutoIncrement
	@PrimaryKey("ID")
	long getId();

	void setId(long id);

	String getGroup();

	void setGroup(String group);

	long getSourceId();

	void setSourceId(long id);

	String getSourceDocumentationLocation();

	void setSourceDocumentationLocation(String documentationLocation);

	String getProjectKey();

	void setProjectKey(String projectKey);

	/**
	 * Deletes the {@group DecisionGroupInDatabase } object, i.e., removes it from
	 * database.
	 * 
	 * @param groupToDelete
	 *            {@group DecisionGroupInDatabase} object.
	 * @return true if deletion was successful, false otherwise.
	 */
	static boolean deleteGroup(DecisionGroupInDatabase groupAssignmentToDelete) {
		try {
			groupAssignmentToDelete.getEntityManager().delete(groupAssignmentToDelete);
			return true;
		} catch (SQLException e) {
			return false;
		}
	}
}
