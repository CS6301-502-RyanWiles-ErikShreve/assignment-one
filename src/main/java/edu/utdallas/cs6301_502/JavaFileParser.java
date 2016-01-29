package edu.utdallas.cs6301_502;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

/* 
 * Implementation pretty much leaves comments alone.  Those should be handled in a subsequent processing phase.
 * Ignores the package & import statements at roughly their legal locations, but should preserve a string later that matches that format.
 * 
 * Stuff to do:
 * 	look for c & javadoc comments that have their close on the same line as statements and treat the comment different than the statement
 *  better application of JAVA_KEYWORDS based on their location.  currently removing all keywords.  would prefer to strip keywords from legal locations 
 */
public class JavaFileParser {

	private static final String[] JAVA_KEYWORDS = new String[] {
			"abstract", "continue", "for", "new", "switch",
			"assert", "default", "goto", "synchronized",
			"boolean", "do", "if", "private", "this",
			"break", "double", "implements", "protected", "throw",
			"byte", "else", "public", "throws",
			"case", "enum", "instanceof", "return", "transient",
			"catch", "extends", "int", "short", "try",
			"char", "final", "interface", "static", "void",
			"class", "finally", "long", "strictfp", "volatile",
			"const", "float", "native", "super", "while", "null" };

	private boolean debug = false;
	private File file;
	private String bagOWords = "";
	private Set<String> stopWords = new HashSet<String>();

	public JavaFileParser(File file) throws ParseException {
		this(false, file);
	}

	public JavaFileParser(boolean debug, File file) throws ParseException {
		this.debug = debug;

		this.file = file;
		loadStopWords();
		try {
			this.bagOWords = parse();
		} catch (Exception e) {
			throw new ParseException(e.getMessage(), 0);
		}
	}

	public static void main(String... args) {
		try {
			JavaFileParser jfp = new JavaFileParser(true, new File("/Users/rwiles/Documents/workspace/embeddableSearch/src/main/java/net/networkdowntime/search/text/processing/KeywordScrubber.java"));
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private void loadStopWords() {
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource("stop_words.xml").getFile());
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while (reader.ready()) {
				String line = reader.readLine().trim();
				if (line.startsWith("<word>") && line.endsWith("</word>"))
					stopWords.add(line.substring(6, line.length() - 7));

			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void debug(String line) {
		if (this.debug) {
			System.out.println(line);
		}
	}
	
	private String parse() throws FileNotFoundException, IOException {
		StringBuilder builder = new StringBuilder();

		BufferedReader reader = new BufferedReader(new FileReader(file));
		boolean inDoc = false;
		boolean pastPackage = false;
		boolean hasProcessedPackage = false;
		boolean pastImports = false;
		boolean hasProcessedImports = false;

		while (reader.ready()) {
			String line = reader.readLine().trim();

			if (line.isEmpty()) {
				continue;
			}

			// check for line with only // comments
			if (line.startsWith("//")) {
				line = line.substring(2).trim();
				debug(line);
				builder.append(" " + line);
				continue;
			}

			if (inDoc) {
				if (line.contains("*/")) {
					inDoc = false;
					line = line.replace("*/", " ").trim();
				} else if (line.startsWith("*")) {
					line = line.substring(1).trim();
				}
				debug(line);
				builder.append(" " + line);
				continue;
			}

			// check for javadoc style comments
			if (line.startsWith("/**")) {
				inDoc = true;
				line = line.substring(3).trim();
				debug(line);
				builder.append(" " + line);
				continue;
			}

			// check for c style comments
			if (line.startsWith("/*")) {
				inDoc = true;
				line = line.substring(2).trim();
				debug(line);
				builder.append(" " + line);
				continue;
			}

			// package statement must be the first non-comment line in java
			if (!hasProcessedPackage && !pastPackage && line.startsWith("package ")) {
				hasProcessedPackage = true;
				continue;
			}
			pastPackage = true;

			// import statements must follow the package statement, if present, and come before the rest
			// comments are allowed in the imports
			if (!hasProcessedImports && !pastImports && line.startsWith("import ")) {
				continue;
			}
			hasProcessedImports = true;
			pastImports = true;

			for (String keyword : JAVA_KEYWORDS) {
				line = line.replaceAll("^" + keyword + " ", " ").trim();
				line = line.replaceAll(" " + keyword + " ", " ").trim();
			}

			// explode punctuation to a space
			line = line.replaceAll("[\\{|\\}|\\(|\\)|;|,|=|+|\\-|*|\"|'|/|\\?|:|<|\\[|\\]|!|\\>|\\^|\\$|\\&\\&|\\|\\||\\.|`|#|~]", " ").trim();
			line = line.replaceAll("\\\\t", " ").trim();
			line = line.replaceAll("\\\\r", " ").trim();
			line = line.replaceAll("\\\\n", " ").trim();
			line = line.replaceAll("\\\\", " ").trim();
			line = line.replaceAll(" [0-9]+\\.[0-9]+", " ").trim(); // decimal numbers
			line = line.replaceAll(" [0-9]+f", " ").trim(); // integer numbers as a float
			line = line.replaceAll(" [0-9]+d", " ").trim(); // integer numbers as a double
			line = line.replaceAll(" 0[x|X][0-9a-fA-F]+", " ").trim(); // integer numbers as hex
			line = line.replaceAll(" [0-9]+", " ").trim(); // integer numbers
			line = line.replaceAll("\\s+", " ");
			if (!line.isEmpty()) {
				debug(line);
				builder.append(" " + line);
			}
		}
		reader.close();
		return builder.toString();
	}
	/*
	
	 */
}
