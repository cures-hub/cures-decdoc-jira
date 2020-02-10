package de.uhd.ifi.se.decision.management.jira.extraction.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.CoreConfig.AutoCRLF;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.Issue;
import com.google.common.collect.Lists;

import de.uhd.ifi.se.decision.management.jira.extraction.GitClient;
import de.uhd.ifi.se.decision.management.jira.extraction.versioncontrol.GitRepositoryFSManager;
import de.uhd.ifi.se.decision.management.jira.model.git.Diff;
import de.uhd.ifi.se.decision.management.jira.model.git.impl.ChangedFileImpl;
import de.uhd.ifi.se.decision.management.jira.model.git.impl.DiffImpl;
import de.uhd.ifi.se.decision.management.jira.persistence.ConfigPersistenceManager;

/**
 * @issue How to access commits related to a Jira issue?
 * @decision Use the jGit library to access the git repositories for a Jira
 *           project!
 * @pro The jGit library is open source.
 * @alternative Both, the jgit library and the git integration for Jira plugin
 *              were used to access git repositories!
 * @con An application link and oAuth is needed to call REST API on Java side in
 *      order to access the git repository with the git integration for Jira
 *      plugin.
 *
 *
 *      This implementation works well only with configuration for one remote
 *      git server. Multiple instances of this class are "thread-safe" in the
 *      limited way that the checked-out branch files are stored in dedicated
 *      branch folders and can be read. Modifying files is not safe and not
 *      supported.
 */
public class GitClientImpl implements GitClient {

    private static final long REPO_OUTDATED_AFTER = 15 * 60 * 1000; // ex. 15 minutes = 15 minutes * 60 seconds * 1000
    // miliseconds
    private Map<String, Git> gits;
    private List<String> remoteUris;
    private String projectKey;
    private String defaultDirectory;
    private Map<String, String> defaultBranchFolderNames;
    private boolean repoInitSuccess = false; // will be later made readable with upcoming features
    private Map<String, Ref> defaultBranches; // TODO: should come from configuration of the project
    private Map<Ref, List<RevCommit>> defaultBranchCommits; // will be later needed for upcoming features
    private GitRepositoryFSManager fsManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(GitClientImpl.class);

    public GitClientImpl() {
    }

    public GitClientImpl(List<File> directories) {
	initMaps();
	List<String> uris = ConfigPersistenceManager.getGitUris(projectKey);
	if (directories.size() == uris.size()) {
	    boolean success = true;
	    for (int i = 0; i < directories.size(); i++) {
		success = initRepository(uris.get(i), directories.get(i));
		if (!success) {
		    this.repoInitSuccess = false;
		}
	    }
	}
    }

    public GitClientImpl(List<String> uris, String defaultDirectory, String projectKey) {
	initMaps();
	Map<String, String> defaultBranches = ConfigPersistenceManager.getDefaultBranches(projectKey);
	for (int i = 0; i < uris.size(); i++) {
	    if (defaultBranches != null && defaultBranches.size() != 0 && defaultBranches.get(uris.get(i)) != null) {
		defaultBranchFolderNames.put(uris.get(i), defaultBranches.get(uris.get(i)));
	    } else {
		defaultBranchFolderNames.put(uris.get(i), "develop");
	    }
	}
	this.repoInitSuccess = pullOrCloneRepositories(projectKey, defaultDirectory, uris, defaultBranchFolderNames);
    }

    public GitClientImpl(List<String> uris, String projectKey) {
	initMaps();
	Map<String, String> defaultBranches = ConfigPersistenceManager.getDefaultBranches(projectKey);
	for (int i = 0; i < uris.size(); i++) {
	    if (defaultBranches.get(uris.get(i)) != null) {
		defaultBranchFolderNames.put(uris.get(i), defaultBranches.get(uris.get(i)));
	    } else {
		defaultBranchFolderNames.put(uris.get(i), "develop");
	    }
	}
	this.repoInitSuccess = pullOrCloneRepositories(projectKey, DEFAULT_DIR, uris, defaultBranchFolderNames);
    }

    public GitClientImpl(GitClientImpl originalClient) {
	initMaps();
	this.repoInitSuccess = pullOrCloneRepositories(originalClient.getProjectKey(),
		originalClient.getDefaultDirectory(), originalClient.getRemoteUris(),
		originalClient.getDefaultBranchFolderNames());
    }

    public GitClientImpl(String projectKey) {
	initMaps();
	List<String> uris = ConfigPersistenceManager.getGitUris(projectKey);
	Map<String, String> defaultBranches = ConfigPersistenceManager.getDefaultBranches(projectKey);
	for (int i = 0; i < uris.size(); i++) {
	    if (defaultBranches.get(uris.get(i)) != null) {
		defaultBranchFolderNames.put(uris.get(i), defaultBranches.get(uris.get(i)));
	    } else {
		defaultBranchFolderNames.put(uris.get(i), "develop");
	    }
	}
	this.repoInitSuccess = pullOrCloneRepositories(projectKey, DEFAULT_DIR, uris, defaultBranchFolderNames);
    }

    private void initMaps() {
	gits = new HashMap<String, Git>();
	defaultBranchFolderNames = new HashMap<String, String>();
	defaultBranches = new HashMap<String, Ref>();
	defaultBranchCommits = new HashMap<Ref, List<RevCommit>>();
    }

    private boolean pullOrCloneRepositories(String projectKey, String defaultDirectory, List<String> uris,
	    Map<String, String> defaultBranchFolderNames) {
	fsManager = new GitRepositoryFSManager(defaultDirectory, projectKey, uris, defaultBranchFolderNames);
	Map<String, String> defaultBranchPaths = fsManager.getDefaultBranchPaths();
	this.remoteUris = uris;
	this.projectKey = projectKey;
	this.defaultDirectory = defaultDirectory;
	this.defaultBranchFolderNames = defaultBranchFolderNames;
	for (String uri : uris) {
	    File directory = new File(defaultBranchPaths.get(uri));
	    if (!pullOrClone(uri, directory)) {
		return false;
	    }
	}
	return true;
    }

    private boolean pullOrClone(String repoUri, File directory) {
	if (isGitDirectory(directory)) {
	    if (openRepository(repoUri, directory)) {
		if (!pull(repoUri)) {
		    LOGGER.error("failed Git pull " + directory);
		    return false;
		}
	    } else {
		LOGGER.error("Could not open repository: " + directory.getAbsolutePath());
		return false;
	    }
	} else {
	    if (!cloneRepository(repoUri, directory)) {
		LOGGER.error("Could not clone repository " + repoUri + " to " + directory.getAbsolutePath());
		return false;
	    }
	}
	if (defaultBranchFolderNames.get(repoUri) == null) {
	    return false;
	}
	Ref defaultBranch = getBranch(defaultBranchFolderNames.get(repoUri), repoUri);
	defaultBranches.put(repoUri, defaultBranch);
	defaultBranchCommits.put(defaultBranch, getCommitsFromDefaultBranch(repoUri));
	return true;
    }

    private boolean isGitDirectory(File directory) {
	File gitDir = new File(directory, ".git/");
	return directory.exists() && (gitDir.isDirectory());
    }

    private Ref getCurrentBranch(String repoUri) {
	String branchName = null;
	Ref branch = null;
	Repository repository;

	try {
	    repository = this.getRepository(repoUri);
	} catch (Exception e) {
	    LOGGER.error("getRepository: " + e.getMessage());
	    return null;
	}

	if (repository == null) {
	    LOGGER.error("Git repository does not seem to exist");
	    return null;
	}
	try {
	    branchName = repository.getFullBranch();
	    branch = repository.findRef(branchName);
	    if (branch == null) {
		LOGGER.error("Git repository does not seem to be on a branch");
		return null;
	    }
	} catch (Exception e) {
	    LOGGER.error("Git client has thrown error while getting branch name. " + e.getMessage());
	    return null;
	}
	return branch;
    }

    private boolean openRepository(String repoUri, File directory) {
	try {
	    Git git = Git.open(directory);
	    gits.put(repoUri, git);
	} catch (IOException e) {
	    LOGGER.error(
		    "Git repository could not be opened: " + directory.getAbsolutePath() + "\n\t" + e.getMessage());
	    return false;
	}
	return true;
    }

    private boolean pull(String repoUri) {
	if (!isPullNeeded(repoUri)) {
	    return true;
	}
	try {
	    List<RemoteConfig> remotes = gits.get(repoUri).remoteList().call();
	    for (RemoteConfig remote : remotes) {
		gits.get(repoUri).fetch().setRemote(remote.getName()).setRefSpecs(remote.getFetchRefSpecs())
			.setRemoveDeletedRefs(true).call();
		LOGGER.info("Fetched branches in " + gits.get(repoUri).getRepository().getDirectory());
	    }
	    gits.get(repoUri).pull().call();
	} catch (GitAPIException e) {
	    LOGGER.error("Issue occurred while pulling from a remote." + "\n\t" + e.getMessage());
	    return false;
	}
	LOGGER.info("Pulled from remote in " + gits.get(repoUri).getRepository().getDirectory());
	return true;
    }

    /**
     * Based on file timestamp, the method decides if pull is necessary.
     *
     * @return decision whether to make or not make the git pull call.
     */
    private boolean isPullNeeded(String repoUri) {
	String trackerFilename = "condec.pullstamp.";
	Repository repository = this.getRepository(repoUri);
	File file = new File(repository.getDirectory(), trackerFilename);

	if (!file.isFile()) {
	    file.setWritable(true);
	    try {
		file.createNewFile();
	    } catch (IOException ex) {
		LOGGER.error("Could not create a file, repositories will be fetched on each request.");
	    }
	    return true;
	} else {
	    if (isRepoOutdated(file.lastModified())) {
		updateFileModifyTime(file);
		return true;
	    }
	    return false;
	}
    }

    private boolean isRepoOutdated(long lastModified) {
	Date date = new Date();
	long fileLifespan = date.getTime() - lastModified;
	return fileLifespan > REPO_OUTDATED_AFTER;
    }

    private boolean updateFileModifyTime(File file) {
	Date date = new Date();
	if (!file.setLastModified(date.getTime())) {
	    LOGGER.error("Could not modify a file modify time, repositories will be fetched on each request.");
	    return false;
	}
	return true;
    }

    private boolean cloneRepository(String uri, File directory) {
	if (uri == null || uri.isEmpty()) {
	    return false;
	}
	try {
	    gits.put(uri, Git.cloneRepository().setURI(uri).setDirectory(directory).setCloneAllBranches(true).call());
	    setConfig(uri);
	} catch (GitAPIException e) {
	    LOGGER.error("Git repository could not be cloned: " + uri + " " + directory.getAbsolutePath() + "\n\t"
		    + e.getMessage());
	    return false;
	}
	// TODO checkoutDefault branch
	return true;
    }

    private boolean initRepository(String repoUri, File directory) {
	try {
	    Git git = Git.init().setDirectory(directory).call();
	    gits.put(repoUri, git);
	} catch (IllegalStateException | GitAPIException e) {
	    LOGGER.error("Bare git repository could not be initiated: " + directory.getAbsolutePath());
	    return false;
	}
	return true;
    }

    private boolean setConfig(String repoUri) {
	Repository repository = this.getRepository(repoUri);
	StoredConfig config = repository.getConfig();
	/**
	 * @issue The internal representation of a file might add system dependent new
	 *        line statements, for example CR LF in Windows. How to deal with
	 *        different line endings?
	 * @decision Disable system dependent new line statements!
	 * @pro It is a common approach
	 * @alternative Ignore the problem! The plug-in in hosted on one machine, which
	 *              most likely is not Windows.
	 * @con It is painful for developers to work with local Windows setups.
	 */
	config.setEnum(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_AUTOCRLF, AutoCRLF.TRUE);
	try {
	    config.save();
	} catch (IOException e) {
	    LOGGER.error("Git configuration could not be set. Message: " + e.getMessage());
	    return false;
	}
	return true;
    }

    @Override
    public Diff getDiff(List<RevCommit> commits, String repoUri) {
	if (commits == null || commits.size() == 0) {
	    return null;
	}
	// TODO Check if this is always correct
	RevCommit firstCommit = commits.get(commits.size() - 1);
	RevCommit lastCommit = commits.get(0);
	return getDiff(firstCommit, lastCommit, repoUri);
    }

    @Override
    public Diff getDiff(Issue jiraIssue, String repoUri) {
	if (jiraIssue == null) {
	    return null;
	}
	List<RevCommit> commits = getCommits(jiraIssue, repoUri);
	return getDiff(commits, repoUri);
    }

    @Override
    public Diff getDiff(RevCommit firstCommit, RevCommit lastCommit, String repoUri) {
	Diff diff = new DiffImpl();
	List<DiffEntry> diffEntries = new ArrayList<DiffEntry>();

	DiffFormatter diffFormatter = getDiffFormater(repoUri);
	try {
	    RevCommit parentCommit = getParent(firstCommit, repoUri);
	    if (parentCommit != null) {
		diffEntries = diffFormatter.scan(parentCommit.getTree(), lastCommit.getTree());
	    }
	} catch (IOException e) {
	    LOGGER.error("Git diff could not be retrieved. Message: " + e.getMessage());
	}

	File directory = getDirectory(repoUri);
	String baseDirectory = "";
	if (directory != null) {
	    baseDirectory = getDirectory(repoUri).toString().replace(".git", "");
	}
	for (DiffEntry diffEntry : diffEntries) {
	    try {
		EditList editList = diffFormatter.toFileHeader(diffEntry).toEditList();
		diff.addChangedFile(new ChangedFileImpl(diffEntry, editList, baseDirectory));
	    } catch (IOException e) {
		LOGGER.error("Git diff for the file " + diffEntry.getNewPath() + " could not be retrieved. Message: "
			+ e.getMessage());
	    }
	}
	diffFormatter.close();
	return diff;
    }

    @Override
    public Diff getDiff(RevCommit revCommit, String repoUri) {
	return getDiff(revCommit, revCommit, repoUri);
    }

    public String getRepoUriFromBranch(Ref featureBranch) {
	if (featureBranch != null) {
	    for (String uri : remoteUris) {
		Git git = gits.get(uri);
		try {
		    if (git.getRepository() != null && git.getRepository().exactRef(featureBranch.getName()) != null) {
			return uri;
		    }
		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }
	}
	return null;
    }

    /**
     * Temporally switches git client's directory to feature branch directory to
     * fetch commits, afterwards returns to default branch directory after.
     * 
     * @param featureBranch ref of the feature branch, Uri of Git Repository
     * @return list of unique commits.
     */
    @Override
    public List<RevCommit> getFeatureBranchCommits(Ref featureBranch) {
	String repoUri = getRepoUriFromBranch(featureBranch);
	List<RevCommit> branchUniqueCommits = new ArrayList<RevCommit>();
	List<RevCommit> branchCommits = getCommits(featureBranch);
	RevCommit lastCommonAncestor = null;
	if (!defaultBranchCommits.containsKey(defaultBranches.get(repoUri))
		|| defaultBranchCommits.get(defaultBranches.get(repoUri)) == null) {
	    defaultBranchCommits.put(defaultBranches.get(repoUri), getCommitsFromDefaultBranch(repoUri));
	}
	for (RevCommit commit : branchCommits) {
	    if (defaultBranchCommits.get(defaultBranches.get(repoUri)) != null && commit != null
		    && defaultBranchCommits.get(defaultBranches.get(repoUri)).contains(commit)) {
		LOGGER.info("Found last common commit " + commit.toString());
		lastCommonAncestor = commit;
		break;
	    }
	    branchUniqueCommits.add(commit);
	}
	if (lastCommonAncestor == null) {
	    branchUniqueCommits = null;
	} else if (branchUniqueCommits.size() > 0) {
	    branchUniqueCommits = Lists.reverse(branchUniqueCommits);
	} else {
	    branchUniqueCommits = branchCommits;
	}

	return branchUniqueCommits;
    }

    @Override
    public List<RevCommit> getFeatureBranchCommits(String featureBranchName, String repoUri) {
	Ref featureBranch = getBranch(featureBranchName, repoUri);
	if (null == featureBranch) {
	    /**
	     * @issue What is the return value of methods that would normally return a
	     *        collection (e.g. list) with an invalid input parameter?
	     * @alternative Methods with an invalid input parameter return an empty list!
	     * @pro Would prevent a null pointer exception.
	     * @con Is misleading since it is not clear whether the list is empty but has a
	     *      valid input parameter or because of an invalid parameter.
	     * @alternative Methods with an invalid input parameter return null!
	     * @con null values might be intended as result.
	     * @decision Return emtpyList to compensate for branch being in another
	     *           repository
	     */
	    return Collections.emptyList();
	}
	return getFeatureBranchCommits(featureBranch);
    }

    private Ref getBranch(String featureBranchName, String repoUri) {
	if (featureBranchName == null || featureBranchName.length() == 0) {
	    LOGGER.info("Null or empty branch name was passed.");
	    return null;
	}
	List<Ref> remoteBranches = getRemoteBranches(repoUri);
	if (remoteBranches != null) {
	    for (Ref branch : remoteBranches) {
		String branchName = branch.getName();
		if (branchName.endsWith(featureBranchName)) {
		    return branch;
		}
	    }
	}
	LOGGER.info("Could not find branch " + featureBranchName);
	return null;
    }

    private DiffFormatter getDiffFormater(String repoUri) {
	DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
	Repository repository = this.getRepository(repoUri);
	if (repository == null) {
	    return diffFormatter;
	}
	diffFormatter.setRepository(repository);
	diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
	diffFormatter.setDetectRenames(true);
	return diffFormatter;
    }

    public Repository getRepository(String repoUri) {
	if (gits == null || !this.gits.containsKey(repoUri)) {
	    return null;
	}
	return gits.get(repoUri).getRepository();
    }

    @Override
    public File getDirectory(String repoUri) {
	Repository repository = this.getRepository(repoUri);
	if (repository == null) {
	    return null;
	}
	return repository.getDirectory();
    }

    private RevCommit getParent(RevCommit revCommit, String repoUri) {
	RevCommit parentCommit = null;
	try {
	    Repository repository = this.getRepository(repoUri);
	    RevWalk revWalk = new RevWalk(repository);
	    parentCommit = revWalk.parseCommit(revCommit.getParent(0).getId());
	    revWalk.close();
	} catch (Exception e) {
	    LOGGER.error("Could not get the parent commit. Message: " + e.getMessage());
	}
	return parentCommit;
    }

    @Override
    public void close(String repoUri) {
	if (gits.get(repoUri) == null) {
	    return;
	}
	gits.get(repoUri).getRepository().close();
	gits.get(repoUri).close();
	gits.remove(repoUri);
    }

    public void closeAll() {
	if (remoteUris != null) {
	    for (String repoUri : this.remoteUris) {
		if (gits.get(repoUri) == null) {
		    return;
		}
		gits.get(repoUri).getRepository().close();
		gits.get(repoUri).close();
		gits.remove(repoUri);
	    }
	}
    }

    @Override
    public void deleteRepository(String repoUri) {
	if (gits == null || gits.get(repoUri) == null | this.getDirectory(repoUri) != null) {
	    return;
	}
	close(repoUri);
	File directory = this.getDirectory(repoUri).getParentFile();
	deleteFolder(directory);
    }

    private static void deleteFolder(File directory) {
	for (File file : directory.listFiles()) {
	    if (file.isDirectory()) {
		deleteFolder(file);
	    }
	    file.delete();
	}
	directory.delete();
    }

    /**
     * Switches git client's directory to feature branch directory, i.e., DOES NOT
     * go back to the default branch directory.
     */
    @Override
    public boolean checkoutFeatureBranch(String featureBranchShortName, String repoUri) {
	Ref featureBranch = getBranch(featureBranchShortName, repoUri);
	if (null == featureBranch) {
	    return false;
	}
	return checkoutFeatureBranch(featureBranch);

    }

    /**
     * Switches git client's directory to commit directory, checks out files in
     * working dir for the commit. I.e., DOES NOT go back to default branch
     * directory.
     */
    @Override
    public boolean checkoutCommit(RevCommit commit, String repoUri) {
	String commitName = commit.getName();

	// will copy default branch folder
	File directory = new File(fsManager.prepareBranchDirectory(commitName, repoUri));

	return (switchGitDirectory(directory, repoUri) && checkout(commit.getName(), repoUri, true));
    }

    private boolean checkout(String branchShortName, String repoUri) {
	return checkout(branchShortName, repoUri, false);
    }

    /**
     * Switches git client's directory to feature branch directory, i.e., DOES NOT
     * go back to default branch directory.
     */
    @Override
    public boolean checkoutFeatureBranch(Ref featureBranch) {
	String repoUri = getRepoUriFromBranch(featureBranch);
	String[] branchNameComponents = featureBranch.getName().split("/");
	String branchShortName = branchNameComponents[branchNameComponents.length - 1];
	File directory = new File(fsManager.prepareBranchDirectory(branchShortName, repoUri));

	return (switchGitDirectory(directory, repoUri) && pull(repoUri) && checkout(branchShortName, repoUri));
    }

    @Override
    public List<RevCommit> getCommits(Issue jiraIssue, String repoUri) {
	if (jiraIssue == null) {
	    return new LinkedList<RevCommit>();
	}
	String jiraIssueKey = jiraIssue.getKey();
	List<RevCommit> commitsForJiraIssue = new LinkedList<RevCommit>();
	if (gits == null || gits.get(repoUri) == null || jiraIssueKey == null) {
	    LOGGER.error("Commits cannot be retrieved since git object is null.");
	    return commitsForJiraIssue;
	}
	/**
	 * @issue How to get the commits for branches that are not on the master branch?
	 * @alternative Assume that the branch name begins with the JIRA issue key!
	 * @pro This simple rule will work just fine.
	 * @con The rule is too simple. Issues with low key numbers could collect
	 *      branches of much higher issues also. ex. search for "CONDEC-1" would
	 *      find branches beginning with CONDEC-1 BUT as well the ones for issues
	 *      with keys "CONDEC-10", "CONDEC-11" , "CONDEC-100" etc.
	 * @decision Assume the branch name begins with the JIRA issue key and a dot
	 *           character follows directly afterwards!
	 * @pro issues with low key number (ex. CONDEC-1) and higher key numbers (ex.
	 *      CONDEC-1000) will not be confused.
	 */
	Ref branch = getRef(jiraIssueKey, repoUri);
	List<RevCommit> commits = getCommits(branch);
	for (RevCommit commit : commits) {
	    // TODO Improve identification of jira issue key in commit message
	    String jiraIssueKeyInCommitMessage = GitClient.getJiraIssueKey(commit.getFullMessage());
	    if (jiraIssueKeyInCommitMessage.equalsIgnoreCase(jiraIssueKey)) {
		commitsForJiraIssue.add(commit);
		LOGGER.info("Commit message for key " + jiraIssueKey + ": " + commit.getShortMessage());
	    }
	}
	return commitsForJiraIssue;
    }

    @Override
    public List<RevCommit> getCommits(String repoUri) {
	List<RevCommit> commits = new ArrayList<RevCommit>();
	for (Ref branch : getRemoteBranches(repoUri)) {
	    /**
	     * @issue All branches will be created in separate file system folders for this
	     *        method's loop. How can this be prevented?
	     * @alternative remove this method completely!
	     * @pro Fetching commits from all branches is not sensible.
	     * @con Fetching commits from all branches may still be needed in some use
	     *      cases. *
	     * @pro this method seems to be used only for code testing (TestGetCommits)
	     * @con scraping it would require coding improvement in test code
	     *      (TestGetCommits), but who wants to spend time on that ;-)
	     * @alternative We could check whether the JIRA issue key is part of the branch
	     *              name and - if so - only use the commits from this branch!
	     * @con it is not clear what is meant with this alternative.
	     * @decision release branch folders if possible, so that in best case only one
	     *           folder will be used!
	     * @pro implementation does not seem to be complex at all.
	     * @pro until discussion are not finished, seems like a good trade-off.
	     * @con still some more code will be written. Scraping it, would require coding
	     *      improvement in test code (TestGetCommits).
	     */
	    commits.addAll(getCommits(branch));
	}
	return commits;
    }

    /*
     * TODO: This method and getCommits(Issue jiraIssue) need refactoring and deeper
     * discussions!
     */
    private Ref getRef(String jiraIssueKey, String repoUri) {
	List<Ref> refs = getAllRefsForOneRepo(repoUri);
	Ref branch = null;
	for (Ref ref : refs) {
	    if (ref.getName().contains(jiraIssueKey)) {
		return ref;
	    } else if (ref.getName().equalsIgnoreCase("refs/heads/" + defaultBranchFolderNames.get(repoUri))) {
		branch = ref;
	    }
	}
	return branch;
    }

    @Override
    public List<Ref> getAllRemoteBranches() {
	return getAllRefs(ListBranchCommand.ListMode.REMOTE);
    }

    @Override
    public List<Ref> getRemoteBranches(String repoUri) {
	return getRefs(ListBranchCommand.ListMode.REMOTE, repoUri);
    }

    private List<Ref> getAllRefsForOneRepo(String repoUri) {
	return getRefs(ListBranchCommand.ListMode.ALL, repoUri);
    }

    private List<Ref> getRefs(ListBranchCommand.ListMode listMode, String repoUri) {
	List<Ref> refs = new ArrayList<Ref>();
	try {
	    refs = gits.get(repoUri).branchList().setListMode(listMode).call();
	} catch (GitAPIException | NullPointerException e) {
	    LOGGER.error("Git could not get references. Message: " + e.getMessage());
	}
	return refs;
    }

    private List<Ref> getAllRefs(ListBranchCommand.ListMode listMode) {
	List<Ref> allRefs = new ArrayList<Ref>();
	for (String uri : remoteUris) {
	    try {
		List<Ref> refs = gits.get(uri).branchList().setListMode(listMode).call();
		allRefs.addAll(refs);
	    } catch (GitAPIException | NullPointerException e) {
		LOGGER.error("Git could not get references. Message: " + e.getMessage());
	    }
	}

	return allRefs;
    }

    public List<RevCommit> getAllRelatedCommits(Issue jiraIssue) {
	List<RevCommit> allCommits = new ArrayList<RevCommit>();
	for (String repoUri : remoteUris) {
	    List<RevCommit> com = getCommits(jiraIssue, repoUri);
	    allCommits.addAll(com);
	}
	return allCommits;
    }

    private List<RevCommit> getCommitsFromDefaultBranch(String repoUri) {
	return getCommits(repoUri, defaultBranches.get(repoUri), true);
    }

    private List<RevCommit> getCommits(Ref branch) {
	String repoUri = getRepoUriFromBranch(branch);
	return getCommits(repoUri, branch, false);
    }

    private List<RevCommit> getCommits(String repoUri, Ref branch, boolean isDefaultBranch) {
	List<RevCommit> commits = new ArrayList<RevCommit>();
	if (branch == null || fsManager == null) {
	    return commits;
	}
	File directory;
	String[] branchNameComponents = branch.getName().split("/");
	String branchShortName = branchNameComponents[branchNameComponents.length - 1];
	boolean canReleaseRepoDirectory = false;

	if (isDefaultBranch) {
	    Map<String, String> defaultBranchPaths = fsManager.getDefaultBranchPaths();
	    directory = new File(defaultBranchPaths.get(repoUri));
	} else {
	    canReleaseRepoDirectory = !fsManager.isBranchDirectoryInUse(branchShortName);
	    directory = new File(fsManager.prepareBranchDirectory(branchShortName, repoUri));
	}

	if (switchGitDirectory(directory, repoUri) && pull(repoUri) && checkout(branchShortName, repoUri)) {
	    Iterable<RevCommit> iterable = null;
	    try {
		iterable = gits.get(repoUri).log().call();
	    } catch (GitAPIException e) {
		LOGGER.error("Git could not get commits for the branch: " + branch.getName() + " Message: "
			+ e.getMessage());
	    }
	    if (iterable != null) {
		for (RevCommit commit : iterable) {
		    commits.add(commit);
		}
	    }
	}
	if (canReleaseRepoDirectory) {
	    fsManager.releaseBranchDirectoryNameToTemp(branchShortName, repoUri);
	}
	switchGitClientBackToDefaultDirectory(repoUri);
	return commits;
    }

    private boolean checkout(String checkoutObjectName, String repoUri, boolean isCommitWithinBranch) {

	// checkout only remote branch
	if (!isCommitWithinBranch) {
	    String checkoutName = "origin/" + checkoutObjectName;
	    try {
		gits.get(repoUri).checkout().setName(checkoutName).call();
	    } catch (GitAPIException | JGitInternalException e) {

		LOGGER.error("Could not checkout " + checkoutName + ". " + e.getMessage());
		return false;
	    }
	    // create local branch
	    if (!createLocalBranchIfNotExists(checkoutObjectName, repoUri)) {
		LOGGER.error("Could delete and create local branch");
		return false;

	    }
	}

	// checkout local branch/commit
	try {
	    gits.get(repoUri).checkout().setName(checkoutObjectName).call();
	} catch (GitAPIException | JGitInternalException e) {

	    LOGGER.error("Could not checkout " + checkoutObjectName + ". " + e.getMessage());
	    return false;
	}
	return true;
    }

    private boolean createLocalBranchIfNotExists(String branchShortName, String repoUri) {
	try {
	    gits.get(repoUri).branchCreate().setName(branchShortName).call();
	} catch (RefAlreadyExistsException e) {
	    return true;
	} catch (InvalidRefNameException | RefNotFoundException e) {
	    LOGGER.error("Could not create local branch. " + e.getMessage());
	    return false;
	} catch (GitAPIException e) {
	    LOGGER.error("Could not create local branch. " + e.getMessage());
	    return false;
	}
	return true;
    }

    private boolean switchGitDirectory(File gitDirectory, String repoUri) {
	gits.get(repoUri).close();
	try {
	    Git git = Git.open(gitDirectory);
	    gits.put(repoUri, git);
	} catch (IOException e) {
	    LOGGER.error(
		    "Could not switch into git directory " + gitDirectory.getAbsolutePath() + "\r\n" + e.getMessage());
	    return false;
	}
	return true;
    }

    private void switchGitClientBackToDefaultDirectory(String repoUri) {
	Map<String, String> defaultBranchPaths = fsManager.getDefaultBranchPaths();
	File directory = new File(defaultBranchPaths.get(repoUri));
	try {
	    gits.get(repoUri).close();
	    gits.remove(repoUri);
	    gits.put(repoUri, Git.open(directory));
	} catch (IOException e) {
	    LOGGER.error("Git could not get back to default branch. Message: " + e.getMessage());
	}
    }

    @Override
    public int getNumberOfCommits(Issue jiraIssue, String repoUri) {
	if (jiraIssue == null) {
	    return 0;
	}
	List<RevCommit> commits = getCommits(jiraIssue, repoUri);
	return commits.size();
    }

    @Override
    public Git getGit(String repoUri) {
	return gits.get(repoUri);
    }

    @Override
    public List<Git> getAllGit() {
	List<Git> allGits = new ArrayList<Git>();
	for (String key : gits.keySet()) {
	    allGits.add(gits.get(key));
	}
	return allGits;
    }

    @Override
    public void setGit(Git git, String repoUri) {
	this.gits.put(repoUri, git);
    }

    public List<String> getRemoteUris() {
	return remoteUris;
    }

    public String getProjectKey() {
	return projectKey;
    }

    public String getDefaultDirectory() {
	return defaultDirectory;
    }

    public Map<String, String> getDefaultBranchFolderNames() {
	return defaultBranchFolderNames;
    }

    public Ref getDefaultBranch(String repoUri) {
	return defaultBranches.get(repoUri);
    }

    public boolean isRepoInitSuccess() {
	return repoInitSuccess;
    }

    public List<RevCommit> getDefaultBranchCommits(String repoUri) {
	return defaultBranchCommits.get(defaultBranches.get(repoUri));
    }

}