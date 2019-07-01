package de.uhd.ifi.se.decision.management.jira.extraction.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import de.uhd.ifi.se.decision.management.jira.extraction.ChangedFile;

public class ChangedFileImpl implements ChangedFile {

	private Set<String> methodDeclarations;
	private float probabilityOfCorrectness;

	private static final Logger LOGGER = LoggerFactory.getLogger(ChangedFile.class);

	// @issue How to model whether a changed file is correctly linked to a
	// requirement/work item/knowledge element?
	// @decision Add the isCorrect boolean attribute to the changed file class.
	// @con Changed files might be correctly linked to one requirement but
	// incorrectly linked to another requirement, so it should not be an attribute
	// of the object.
	// @alternative Add class to represent a link between a changed file and a
	// knowledge element.
	private boolean isCorrect;
	@JsonIgnore
	private File file;
	@JsonIgnore
	private int packageDistance;
	@JsonIgnore
	private CompilationUnit compilationUnit;

	public ChangedFileImpl(File file) {
		this.file = file;
		this.packageDistance = 0;
		this.methodDeclarations = parseMethods();
		this.setCorrect(true);
	}

	@Override
	@JsonProperty("className")
	public String getName() {
		return this.file.getName();
	}

	@Override
	public float getProbabilityOfCorrectness() {
		return probabilityOfCorrectness;
	}

	@Override
	public void setProbabilityOfCorrectness(float probabilityOfCorrectness) {
		this.probabilityOfCorrectness = probabilityOfCorrectness;
	}

	@Override
	public int getPackageDistance() {
		return packageDistance;
	}

	@Override
	public void setPackageDistance(int packageDistance) {
		this.packageDistance = packageDistance;
	}

	@Override
	public Set<String> getMethodDeclarations() {
		return methodDeclarations;
	}

	private Set<String> parseMethods() {
		Set<String> methodsInClass = new LinkedHashSet<String>();

		if (!isExistingJavaClass()) {
			return methodsInClass;
		}

		MethodVisitor methodVistor = getMethodVisitor();
		for (MethodDeclaration methodDeclaration : methodVistor.getMethodDeclarations()) {
			methodsInClass.add(methodDeclaration.getNameAsString());
		}

		return methodsInClass;
	}

	private MethodVisitor getMethodVisitor() {
		this.compilationUnit = parseJavaFile(file);
		MethodVisitor methodVistor = new MethodVisitor();
		compilationUnit.accept(methodVistor, null);
		return methodVistor;
	}

	private static CompilationUnit parseJavaFile(File inspectedFile) {
		CompilationUnit compilationUnit = null;
		try {
			FileInputStream fileInputStream = new FileInputStream(inspectedFile.toString());
			compilationUnit = JavaParser.parse(fileInputStream);
			fileInputStream.close();
		} catch (ParseProblemException | IOException e) {
			LOGGER.error(e.getMessage());
		}
		return compilationUnit;
	}

	@Override
	public File getFile() {
		return file;
	}

	@Override
	public void addMethodDeclaration(String methodDeclaration) {
		this.methodDeclarations.add(methodDeclaration);
	}

	@Override
	public boolean isExistingJavaClass() {
		return exists() && isJavaClass();
	}

	@Override
	public boolean exists() {
		return file.exists();
	}

	@Override
	public boolean isJavaClass() {
		return file.getName().endsWith("java");
	}

	public boolean isCorrect() {
		return isCorrect;
	}

	public void setCorrect(boolean isCorrect) {
		this.isCorrect = isCorrect;
	}

	@Override
	public CompilationUnit getCompilationUnit() {
		return this.compilationUnit;
	}

	@Override
	public List<String> getPackageName() {
		Optional<PackageDeclaration> optional = getCompilationUnit().getPackageDeclaration();
		if (optional == null || optional.get() == null) {
			return new ArrayList<String>();
		}
		return new ArrayList<String>(
				Arrays.asList(optional.get().toString().replaceAll("\n", "").replaceAll(";", "").split("\\.")));
	}
}
