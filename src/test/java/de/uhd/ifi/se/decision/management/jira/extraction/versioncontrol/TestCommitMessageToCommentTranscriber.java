package de.uhd.ifi.se.decision.management.jira.extraction.versioncontrol;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import de.uhd.ifi.se.decision.management.jira.extraction.GitClient;
import de.uhd.ifi.se.decision.management.jira.extraction.gitclient.TestSetUpGit;
import de.uhd.ifi.se.decision.management.jira.extraction.impl.GitClientImpl;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestCommitMessageToCommentTranscriber extends TestSetUpGit {

    private CommitMessageToCommentTranscriber transcriber;
    private Issue issue;
    private String testIssueKey;
    private GitClient gitClient;
    private Ref branch;

    private static String DEFAULT_EXPECTED_COMMENT_MESSAGE = "{issue}This is an issue!{issue}";

    private static String META_DATA_STRING =
            "\r\n--- Commit meta data --- \r\n" +
                    "Author: gitTest\r\n" +
                    "Branch: refs/remotes/origin/transcriberBranch\r\n" +
                    "Hash: ";


    @Before
    public void setUp() {
        init();
        this.testIssueKey = "TEST-4";
        this.issue = ComponentAccessor.getIssueManager().getIssueByCurrentKey(this.testIssueKey);
        this.gitClient = new GitClientImpl(getExampleUri(), "TEST");
        this.branch = null;
        Iterator<Ref> it = this.gitClient.getRemoteBranches().iterator();
        while (it.hasNext()) {
            Ref value = it.next();
            if (value.getName().endsWith("transcriberBranch")) {
                this.branch = value;
                break;
            }
        }

        this.transcriber = new CommitMessageToCommentTranscriber(issue, branch);
    }

    @Test
    public void testEmptyMessage() {
        RevCommit commit = this.gitClient.getFeatureBranchCommits(this.branch).get(0);
        assertEquals("", transcriber.generateCommentString(commit));
    }


    @Test
    public void testLowercaseIssueMessage() {
        RevCommit commit = this.gitClient.getFeatureBranchCommits(this.branch).get(1);
        assertEquals(DEFAULT_EXPECTED_COMMENT_MESSAGE + META_DATA_STRING + commit.getName(), transcriber.generateCommentString(commit));
    }

    @Test
    public void testUppercaseIssueMessage() {
        int i = 1;
        RevCommit commit = this.gitClient.getFeatureBranchCommits(this.branch).get(2);
        assertEquals(DEFAULT_EXPECTED_COMMENT_MESSAGE + META_DATA_STRING + commit.getName(), transcriber.generateCommentString(commit));
    }

    @Test
    public void testMixedcaseIssueMessage() {
        RevCommit commit = this.gitClient.getFeatureBranchCommits(this.branch).get(3);
        assertEquals(DEFAULT_EXPECTED_COMMENT_MESSAGE + META_DATA_STRING + commit.getName(), transcriber.generateCommentString(commit));
    }

    @Test
    public void testIssueMessageWithAdditionalText() {
        RevCommit commit = this.gitClient.getFeatureBranchCommits(this.branch).get(4);
        assertEquals(DEFAULT_EXPECTED_COMMENT_MESSAGE + "But I love pizza!" + META_DATA_STRING + commit.getName(), transcriber.generateCommentString(commit));
    }

    @Test
    public void testPostComment() {
        try {
            transcriber.postComments();
        } catch (PermissionException e) {
            assertNull(e);
        }
        String additionalMessage = "";
        List<Comment> comments = ComponentAccessor.getCommentManager().getComments(issue);
        for (int i = 0, j = 1; i < comments.size(); i++, j++) {
            RevCommit currentCommit = this.gitClient.getFeatureBranchCommits(this.branch).get(j);
            if(j == comments.size()){
                additionalMessage = " But I love pizza!";
            }
            assertEquals(DEFAULT_EXPECTED_COMMENT_MESSAGE + additionalMessage + META_DATA_STRING + currentCommit.getName(), comments.get(i).getBody());
        }

    }

}
