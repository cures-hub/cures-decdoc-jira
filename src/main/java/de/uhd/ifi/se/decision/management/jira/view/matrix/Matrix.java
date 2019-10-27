package de.uhd.ifi.se.decision.management.jira.view.matrix;

import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeGraph;
import de.uhd.ifi.se.decision.management.jira.model.Link;

import javax.xml.bind.annotation.XmlElement;
import java.util.*;

public class Matrix {
    @XmlElement
    private Map<Long, String> matrixHeaderRow;

    @XmlElement
    private Map<Long, List<String>> matrixData;

    public Matrix(String projectKey, List<DecisionKnowledgeElement> allDecisions) {
        this.setMatrixHeaderRow(allDecisions);
        this.setMatrixData(projectKey, allDecisions);
    }

    public Map<Long, String> getMatrixHeaderRow() {
        return matrixHeaderRow;
    }

    public void setMatrixHeaderRow(List<DecisionKnowledgeElement> allDecisions) {
        this.matrixHeaderRow = new TreeMap<>();
        this.matrixHeaderRow.putAll(new MatrixRow(allDecisions).getHeaderRow());
    }

    public Map<Long, List<String>> getMatrixData() {
        return matrixData;
    }

    public void setMatrixData(String projectKey, List<DecisionKnowledgeElement> allDecisions) {
        this.matrixData = new TreeMap<>();
        for (DecisionKnowledgeElement decision : allDecisions) {
            List<String> row = new MatrixRow(this.getMatrixEntries(projectKey), this.getMatrixHeaderRow(), decision).getRow();
            this.matrixData.put(decision.getId(), row);
        }
    }

    private HashSet<MatrixEntry> getMatrixEntries(String projectKey) {
        KnowledgeGraph graph = KnowledgeGraph.getOrCreate(projectKey);
        Set<Link> links = graph.edgeSet();
        HashSet<MatrixEntry> entries = new HashSet<>();
        for (Link link : links) {
            entries.add(new MatrixEntry(link.getSource().getId(), link.getTarget().getId(), link.getType()));
        }
        return entries;
    }
}
