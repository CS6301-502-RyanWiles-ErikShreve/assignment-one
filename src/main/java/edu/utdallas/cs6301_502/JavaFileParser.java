package edu.utdallas.cs6301_502;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

/*
 * Implementation pretty much leaves comments alone. Those should be handled in a subsequent processing phase.
 * Ignores the package & import statements at roughly their legal locations, but should preserve a string later that matches that format.
 * 
 * Stuff to do:
 * look for c & javadoc comments that have their close on the same line as statements and treat the comment different than the statement
 * better application of JAVA_KEYWORDS based on their location. currently removing all keywords. would prefer to strip keywords from legal locations
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

	public JavaFileParser(boolean debug, File inputFile) throws ParseException {
		this.debug = debug;

		this.file = inputFile;
		loadStopWords();
		try {
			this.bagOWords = parse();

		} catch (Exception e) {
			throw new ParseException(e.getMessage(), 0);
		}
	}

	public String getBagOWords() {
		return bagOWords;
	}
	
	public static void main(String... args) {
		try {
			JavaFileParser jfp;
			BufferedReader readerFileList;

			if (args.length > 0) {
				readerFileList = new BufferedReader(new FileReader(args[0]));

				try {
					while (readerFileList.ready()) {
						String line = readerFileList.readLine().trim();

						if (line.isEmpty()) {
							continue;
						}

						jfp = new JavaFileParser(true, new File(line));
						
						
						FileWriter writer;
						try {
							String outDir = "";
							if (args.length > 1 && args[1].length() > 0)
							{
								outDir = args[1];
							}
							
							File outFile = new File(outDir + line);
							outFile.getParentFile().mkdirs();
							
							writer = new FileWriter(outFile);
							writer.write(jfp.getBagOWords());
							writer.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				jfp = new JavaFileParser(true, new File("/Users/rwiles/Documents/workspace/embeddableSearch/src/main/java/net/networkdowntime/search/text/processing/KeywordScrubber.java"));
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			System.out.println("Could not open: " + args[0]);
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
		boolean hasProcessedPackage = false;

		debug(file.getAbsolutePath());
		
		while (reader.ready()) {
			String line = reader.readLine().trim();

			if (line.isEmpty()) {
				continue;
			}

			// package statement must be the first non-comment line in java
			if (!hasProcessedPackage && line.startsWith("package ")) {
				hasProcessedPackage = true;

				// Remove package part and process remainder of line (could be a comment)
				line = line.replaceFirst("package .+;", " ").trim();
			}

			// import statements must follow the package statement, if present, and come before the rest
			// comments are allowed in the imports
			if (line.startsWith("import ")) {
				// Remove import part and process remainder of line (could be a comment)
				line = line.replaceFirst("import .+;", "");
			}

			// check for line with only // comments
			if (line.startsWith("//")) {
				line = line.substring(2).trim();
				debug(line);
			}

			if (inDoc) {
				if (line.contains("*/")) {
					inDoc = false;
					line = line.replace("*/", " ").trim();
				} else if (line.startsWith("*")) {
					line = line.substring(1).trim();
				}
				debug(line);
			}

			// check for javadoc style comments
			if (line.startsWith("/**")) {
				inDoc = true;
				line = line.substring(3).trim();
				debug(line);
			}

			// check for c style comments
			if (line.startsWith("/*")) {
				inDoc = true;
				line = line.substring(2).trim();
				debug(line);
			}

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

			// Remove 1 and 2 character words
			String prevLine;
			do 
			{
				prevLine = line;
				line = prevLine.replaceAll("(\\s|^).{1,2}(\\s|$)", " ").trim();
			} while (!line.equals(prevLine));
			

			// Split CamelCase
			if (!line.isEmpty()) {
				StringBuilder lineBuilder = new StringBuilder();

				for (String word : line.split("\\s+")) {
					lineBuilder.append(word);
					lineBuilder.append(" ");

					String[] explodedWord = word.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");

					if (explodedWord.length > 1) {
						for (String w : explodedWord) {
							if (w.length() > 2) // Don't include 1 and 2 character words
							{
								lineBuilder.append(w);
								lineBuilder.append(" ");
							}
						}
					}
				}
				
				debug(lineBuilder.toString());
				builder.append(" " + lineBuilder.toString());
			}
			

			
		}
		reader.close();
		return builder.toString();
	}
	/*
	
	 */
}
