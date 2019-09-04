package de.uhd.ifi.se.decision.management.jira.view.matrix;

import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElement;

import javax.xml.bind.annotation.XmlElement;
import java.util.*;

public class MatrixRow {
    @XmlElement
    private Map<Long, String> headerRow;

    @XmlElement
    private List<String> row;

    public Map<Long, String> getHeaderRow() { return headerRow; }

    public List<String> getRow() {
        return row;
    }

    public MatrixRow(List<DecisionKnowledgeElement> allDecisions) {
        this.headerRow = new TreeMap<>();
        for (DecisionKnowledgeElement decision : allDecisions) {
            this.headerRow.put(decision.getId(), decision.getSummary());
        }
    }

    public MatrixRow(HashSet<MatrixEntry> allEntries, List<DecisionKnowledgeElement> allDecisions, DecisionKnowledgeElement decision) {
        Map<Long, String> entriesForRow = new TreeMap<>();
        for (MatrixEntry entry : allEntries) {
            if (entry.getIdOfSourceElement().equals(decision.getId())) {
                entriesForRow.put(entry.getIdOfDestinationElement(), entry.getColor());
            }
        }

        this.row = new ArrayList<>();

        this.row.add(decision.getSummary());

        for (DecisionKnowledgeElement element : allDecisions) {
            if (element.getId() == decision.getId()) {
                this.row.add("LightGray");
            } else if (entriesForRow.get(element.getId()) != null) {
                this.row.add(entriesForRow.get(element.getId()));
            } else {
                this.row.add("White");
            }

        }
    }
}
