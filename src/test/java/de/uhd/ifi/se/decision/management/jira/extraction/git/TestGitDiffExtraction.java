package de.uhd.ifi.se.decision.management.jira.extraction.git;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.json.JSONException;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import net.java.ao.test.junit.ActiveObjectsJUnitRunner;

@RunWith(ActiveObjectsJUnitRunner.class)
public class TestGitDiffExtraction extends TestSetUpGit {

	@Ignore
	@Test
	public void getNoDiffsForNoCommits() throws IOException, GitAPIException, JSONException, InterruptedException {
		String commits = "{" + "\"commits\":[" + "" + "]" + "}";
		GitClient gitClient = new GitClientImpl(projectKey);
		Map<DiffEntry, EditList> gitDiffs = gitClient.getDiff(commits);
		assertTrue(gitDiffs == null);
	}

	@Ignore
	@Test
	public void getDiffsForOneCommitInMaster()
			throws IOException, GitAPIException, JSONException, InterruptedException {
		// Create a new file
		git.checkout().setCreateBranch(false).setName("master").call();
		File newFile = new File(cloneDir.getAbsolutePath(), "myNewFile.txt");
		newFile.createNewFile();
		FileUtils.writeStringToFile(newFile, "Test content file");
		// Commit the new file
		git.add().addFilepattern(newFile.getName()).call();
		git.commit().setMessage("First commit").setAuthor("gildas", "gildas@example.com").call();

		// Push the commit on the bare repository
		refSpec = new RefSpec("master");
		git.push().setRemote("origin").setRefSpecs(refSpec).call();
		Iterable<RevCommit> revCommits = git.log().all().call();
		String commits = "{" + "\"commits\":[";
		for (RevCommit commit : revCommits) {
			if (commit.getFullMessage().equals("First commit")) {
				commits = commits + "{ commitId: \"" + commit.getName() + "\", },";
			}
		}
		commits = commits + "]" + "}";
		GitClient gitClient = new GitClientImpl(projectKey);
		Map<DiffEntry, EditList> gitDiffs = gitClient.getDiff(commits);
		assertEquals(gitDiffs.size(), 1);
	}

	@Ignore
	@Test
	public void getDiffsForTenCommitsInMaster()
			throws IOException, GitAPIException, JSONException, InterruptedException {
		// Create a new files
		git.checkout().setCreateBranch(false).setName("master").call();
		for (int i = 1; i <= 10; i++) {
			File newFile = new File(cloneDir, "myNewFile" + i + ".txt");
			newFile.createNewFile();
			FileUtils.writeStringToFile(newFile, "Test content file");
			// Commit the new file
			git.add().addFilepattern(newFile.getName()).call();
			git.commit().setMessage("Ten commits in Master").setAuthor("gildas", "gildas@example.com").call();
		}

		// Push the commit on the bare repository
		refSpec = new RefSpec("master");
		git.push().setRemote("origin").setRefSpecs(refSpec).call();
		Iterable<RevCommit> revCommits = git.log().all().call();
		String commits = "{" + "\"commits\":[";
		for (RevCommit commit : revCommits) {
			if (commit.getFullMessage().equals("Ten commits in Master")) {
				commits = commits + "{ commitId: \"" + commit.getName() + "\", },";
			}
		}
		commits = commits + "]" + "}";
		GitClient gitClient = new GitClientImpl(projectKey);
		Map<DiffEntry, EditList> gitDiffs = gitClient.getDiff(commits);
		assertEquals(gitDiffs.size(), 10);
	}

	@Ignore
	@Test
	public void getDiffsForOneCommitInBranch()
			throws IOException, GitAPIException, JSONException, InterruptedException {
		// Create a new file
		git.checkout().setCreateBranch(false).setName("Branch").call();
		File newFile = new File(cloneDir, "myNewFileForBranch.txt");
		newFile.createNewFile();
		FileUtils.writeStringToFile(newFile, "No content");
		// Commit the new file
		git.add().addFilepattern(newFile.getName()).call();
		git.commit().setMessage("First commit in branch").setAuthor("gildas", "gildas@example.com").call();

		// Push the commit on the bare repository
		refSpec = new RefSpec("Branch");
		git.push().setRemote("origin").setRefSpecs(refSpec).call();
		Iterable<RevCommit> revCommits = git.log().all().call();
		String commits = "{" + "\"commits\":[";
		for (RevCommit commit : revCommits) {
			if (commit.getFullMessage().equals("First commit in branch")) {
				commits = commits + "{ commitId: \"" + commit.getName() + "\", },";
			}
		}
		commits = commits + "]" + "}";
		GitClient gitClient = new GitClientImpl(projectKey);
		Map<DiffEntry, EditList> gitDiffs = gitClient.getDiff(commits);
		assertEquals(gitDiffs.size(), 1);
	}

	@Ignore
	@Test
	public void getDiffsForTenCommitsInBranch()
			throws IOException, GitAPIException, JSONException, InterruptedException {
		// Create a new files
		git.checkout().setCreateBranch(false).setName("Branch").call();
		for (int i = 1; i <= 10; i++) {
			File newFile = new File(cloneDir, "myNewFileForBranch" + i + ".txt");
			newFile.createNewFile();
			FileUtils.writeStringToFile(newFile, "No content");
			// Commit the new file
			git.add().addFilepattern(newFile.getName()).call();
			git.commit().setMessage("Ten commits in branch").setAuthor("gildas", "gildas@example.com").call();
		}

		// Push the commit on the bare repository
		refSpec = new RefSpec("Branch");
		git.push().setRemote("origin").setRefSpecs(refSpec).call();
		Iterable<RevCommit> revCommits = git.log().all().call();
		String commits = "{" + "\"commits\":[";
		for (RevCommit commit : revCommits) {
			if (commit.getFullMessage().equals("Ten commits in branch")) {
				commits = commits + "{ commitId: \"" + commit.getName() + "\", },";
			}
		}
		commits = commits + "]" + "}";
		GitClient gitClient = new GitClientImpl(projectKey);
		Map<DiffEntry, EditList> gitDiffs = gitClient.getDiff(commits);
		assertEquals(gitDiffs.size(), 10);
	}

	@Ignore
	@AfterClass
	public static void tearDown() throws InterruptedException {
		Thread.sleep(2000);
		// gitClient.closeAndDeleteRepo();
	}
}
