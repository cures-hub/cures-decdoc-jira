package de.uhd.ifi.se.decision.management.jira.view.matrix;

import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeGraph;
import de.uhd.ifi.se.decision.management.jira.model.Link;

import javax.xml.bind.annotation.XmlElement;
import java.util.*;

public class Matrix {
    @XmlElement
    private Map<Long, DecisionKnowledgeElement> headers;

    @XmlElement
    private Map<Long, List<String>> data;

    @XmlElement
    private List<String> headerArray;

	@XmlElement
	private List<Long> headerIndexArray;

    @XmlElement
    private List<List<String>> dataArray;

    public Matrix(String projectKey, List<DecisionKnowledgeElement> allDecisions) {
        this.setHeaders(allDecisions);
        this.setData(projectKey, allDecisions);

        this.setHeaderArray();
        this.setDataArray();
    }

    public Map<Long, DecisionKnowledgeElement> getHeaders() {
        return headers;
    }

    public void setHeaders(List<DecisionKnowledgeElement> allDecisions) {
        this.headers = new TreeMap<>();
        for (DecisionKnowledgeElement decision : allDecisions) {
            this.headers.put(decision.getId(), decision);
        }
    }

    public Map<Long, List<String>> getData() {
        return data;
    }

    public void setData(String projectKey, List<DecisionKnowledgeElement> allDecisions) {
        this.data = new TreeMap<>();
        var allEntries = this.getMatrixEntries(projectKey);

        for (DecisionKnowledgeElement decision : allDecisions) {
            List<String> row = new ArrayList<>();

            for (DecisionKnowledgeElement headerRowDecision : this.getHeaders().values()) {
                if (headerRowDecision.getSummary().equals(decision.getSummary())) {
                    row.add("LightGray");
                } else if (this.getEntriesForRow(allEntries, decision).get(headerRowDecision.getId()) != null) {
                    row.add(this.getEntriesForRow(allEntries, decision).get(headerRowDecision.getId()));
                } else {
                    row.add("White");
                }
            }
            this.data.put(decision.getId(), row);
        }
    }

    public List<String> getHeaderArray() {
        return headerArray;
    }

    public void setHeaderArray() {
        this.headerArray = new ArrayList<>();
        this.headerIndexArray = new ArrayList<>();
        for (DecisionKnowledgeElement header : this.getHeaders().values()) {
            this.headerArray.add(header.getSummary());
            this.headerIndexArray.add(header.getId());
        }
    }

    public List<List<String>> getDataArray() {
        return dataArray;
    }

    public void setDataArray() {
        this.dataArray = new ArrayList<>();
        for (Map.Entry<Long, List<String>> row : this.getData().entrySet()) {
            this.dataArray.add(row.getValue());
        }
    }

    private Map<Long, String> getEntriesForRow(HashSet<MatrixEntry> allEntries, DecisionKnowledgeElement decision) {
        Map<Long, String> entriesForRow = new TreeMap<>();
		List<String> row = new ArrayList<>();
        for (MatrixEntry entry : allEntries) {
            if (entry.getIdOfSourceElement().equals(decision.getId())) {
                entriesForRow.put(entry.getIdOfDestinationElement(), entry.getColor());
            }

            this.data.put(decision.getId(), row);
        }
        return entriesForRow;
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
