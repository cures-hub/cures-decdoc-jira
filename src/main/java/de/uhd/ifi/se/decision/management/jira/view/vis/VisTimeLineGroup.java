package de.uhd.ifi.se.decision.management.jira.view.vis;

import javax.xml.bind.annotation.XmlElement;

import com.atlassian.jira.user.ApplicationUser;

public class VisTimeLineGroup {

	@XmlElement
	private long id;

	@XmlElement
	private String content;

	public VisTimeLineGroup(ApplicationUser user) {
		this.content = user.getName();
		this.id = user.getId();
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	@Override
	public boolean equals(Object object) {
		if (object == null) {
			return false;
		}
		if (object == this) {
			return true;
		}
		if (!(object instanceof VisTimeLineGroup)) {
			return false;
		}
		VisTimeLineGroup group = (VisTimeLineGroup) object;
		return this.id == group.getId();
	}
}
