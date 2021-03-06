/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * ParsePreparation.java is part of SEMAFOR 2.0.
 * 
 * SEMAFOR 2.0 is free software: you can redistribute it and/or modify  it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * SEMAFOR 2.0 is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. 
 * 
 * You should have received a copy of the GNU General Public License along
 * with SEMAFOR 2.0.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package edu.cmu.cs.lti.ark.fn.data.prep;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


/**
 * Utilities for preprocessing data, converting between different formats, etc.
 */
public class ParsePreparation {
	public static void main(String[] args) throws IOException {
		framenetStuff();
	}
	
	public static void framenetStuff() throws IOException {
		String prefix = "/mal2/dipanjan/experiments/FramenetParsing/framenet_1.3/ddData";
		String input[] = {"semeval.fulltrain.sentences.tokenized", "semeval.fulldev.sentences.tokenized", "semeval.fulltest.sentences.tokenized"};
		String posOutput[] = {"semeval.fulltrain.sentences.pos.tagged", "semeval.fulldev.sentences.pos.tagged", "semeval.fulltest.sentences.pos.tagged"};
		String conllInput[] = {"semeval.fulltrain.sentences.conll.input", "semeval.fulldev.sentences.conll.input", "semeval.fulltest.sentences.conll.input"};
		
		int length = input.length;
		for(int i = 0; i < length; i ++) {
			String inputFile = prefix+"/"+input[i];
			String posOutputFile = prefix+"/"+posOutput[i];
			posTagSentences(inputFile, posOutputFile);
			String conllInputFile = prefix+"/"+conllInput[i];
			printCoNLLTypeInput(posOutputFile, conllInputFile);
		}
	}
	
	public static String replaceSentenceWithPTBWords(String sentence) {
		sentence = sentence.replace("-LRB-_", "(_");
		sentence = sentence.replace("-RRB-_", ")_");
		sentence = sentence.replace("-LSB-_", "[_");
		sentence = sentence.replace("-RSB-_", "]_");
		sentence = sentence.replace("-LCB-_", "{_");
		sentence = sentence.replace("-RCB-_", "}_");
		return sentence;
	}

	/**
	 * Converts a POS tagged file into conll format
	 * @param posFile
	 * @param conllInputFile
	 */
	public static void printCoNLLTypeInput(String posFile, String conllInputFile) throws IOException {
		List<String> posSentences = readSentencesFromFile(posFile);
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(conllInputFile));
		try {
			for(String posSentence: posSentences) {
				posSentence = replaceSentenceWithPTBWords(posSentence);
				ArrayList<String> words = new ArrayList<String>();
				ArrayList<String> pos = new ArrayList<String>();
				ArrayList<String> parents = new ArrayList<String>();
				ArrayList<String> labels = new ArrayList<String>();
				StringTokenizer st = new StringTokenizer(posSentence.trim());
				while(st.hasMoreTokens()) {
					String token = st.nextToken();
					int lastIndex = token.lastIndexOf('_');
					String word = token.substring(0, lastIndex);
					String POS = token.substring(lastIndex+1);
					words.add(word);
					pos.add(POS);
					parents.add("0");
					labels.add("SUB");
				}
				writeStuff(bWriter, words, pos, parents, labels);
			}
		} finally {
			IOUtils.closeQuietly(bWriter);
		}
	}
	
	private static void writeStuff(BufferedWriter bWriter,
								   List<String> words,
								   List<String> pos,
								   List<String> parent,
								   List<String> label) throws IOException {
		int size = words.size();
		for(int i = 0; i < size; i ++) {
			String line = "";
			line+=(i+1)+"\t";
			line+=words.get(i).toLowerCase()+"\t";
			line+=words.get(i).toLowerCase()+"\t";
			line+=pos.get(i)+"\t";
			line+=pos.get(i)+"\t";
			line+="_\t";
			line+=parent.get(i)+"\t";
			line+=label.get(i);
			bWriter.write(line+"\n");
		}
		bWriter.write("\n");
	}
	
	
	public static void posTagSentences(String tokenizedFile, String posTaggedFile) {
		runExternalCommand("scripts/runPosTagger.sh /usr0/dipanjan/work/spring2009/FramenetParsing/FrameStructureExtraction "+tokenizedFile+" "+posTaggedFile);
	}

	public static void runExternalCommand(String command)
	{
		String s;
		try {
			Process p = Runtime.getRuntime().exec(command);
			PrintStream errStream=System.err;
			System.setErr(System.out);
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			// read any errors from the attempted command
			System.out.println("Here is the standard error of the command (if any):");
			while ((s = stdError.readLine()) != null) {
				System.out.println(s);
			}
			p.destroy();
			System.setErr(errStream);
		}
		catch (IOException e) {
			System.out.println("exception happened - here's what I know: ");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 * @param file Path to the file
	 * @return List of all lines from the given file
	 */
	public static ArrayList<String> readSentencesFromFile(String file) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(file));
			return readLinesFromFile(reader, 0, true);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Reads up to a certain number of lines of a file, starting at the current position of the {@link BufferedReader}
	 * @param reader
	 * @param n Nonnegative number of lines to read ({@code 0} to read all lines).
	 * @param closeAtEOF If {@code true} and either {@code n==0} or fewer than {@code n} line could be fetched 
	 * due to the end of the file, the stream will be closed. Otherwise, closing the stream is the caller's responsibility.
	 * @return List of the lines in the range
	 */
	public static ArrayList<String> readLinesFromFile(BufferedReader reader, int n, boolean closeAtEOF) {
		int j = 0;
		ArrayList<String> result = new ArrayList<String>();
		try
		{
			String line = null;
			while((n==0 || j<n) && (line=reader.readLine())!=null)
			{
				result.add(line.trim());
				j++;
			}
			if (closeAtEOF && (n==0 || j<n))
				reader.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * @param file Path to the file
	 * @param n Maximum number of lines to read; if 0, all lines will be read. Must be nonnegative.
	 * @param start Index of first line to include (first line is 0)
	 * @return List of lines of the specified file in the specified range--i.e. lines {@code start} through {@code (start+n-1)}
	 */
	public static ArrayList<String> readSentencesFromFile(String file, int n, int start)	// TODO
	{
		ArrayList<String> result = new ArrayList<String>();
		try
		{
			BufferedReader bReader = new BufferedReader(new FileReader(file));
			String line = null;
			while((line=bReader.readLine())!=null)
			{
				result.add(line.trim());
			}
			bReader.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Writes the given sentences to the given file
	 *
	 * @param outputFile the file to write to
	 * @param sentences the sentences to write
	 */
	public static void writeSentencesToFile(String outputFile, List<String> sentences) {
		try {
			final BufferedWriter bWriter = new BufferedWriter(new FileWriter(outputFile));
			for(String sentence : sentences)  {
				bWriter.write(sentence.trim() + "\n");
			}
			bWriter.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
