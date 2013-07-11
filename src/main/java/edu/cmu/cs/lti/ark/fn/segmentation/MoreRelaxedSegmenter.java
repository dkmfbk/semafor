/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * MoreRelaxedSegmenter.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.segmentation;

import com.google.common.collect.Lists;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;

import java.util.*;

import static edu.cmu.cs.lti.ark.fn.segmentation.RoteSegmenter.getTestLine;

public class MoreRelaxedSegmenter implements Segmenter {
	public static final int MAX_LEN = 4;

	protected final Set<String> allRelatedWords;
	private DependencyParse[] mNodeList = null;
	private DependencyParse mParse = null;

	public MoreRelaxedSegmenter(Set<String> allRelatedWords) {
		this.allRelatedWords = allRelatedWords;
		mNodeList = null;
		mParse =  null;
	}	

	public List<String> getHighRecallSegmentation(String parse, Set<String> allRelatedWords) {
		StringTokenizer st = new StringTokenizer(parse,"\t");
		int tokensInFirstSent = new Integer(st.nextToken());
		String[][] data = new String[6][tokensInFirstSent];
		for(int k = 0; k < 6; k ++)
		{
			data[k]=new String[tokensInFirstSent];
			for(int j = 0; j < tokensInFirstSent; j ++)
			{
				data[k][j]=""+st.nextToken().trim();
			}
		}
		ArrayList<String> startInds = new ArrayList<String>();
		for(int i = 0; i < data[0].length; i ++)
		{
			startInds.add(""+i);
		}
		List<String> tokNums = Lists.newArrayList();
		for(int i = MAX_LEN; i >= 1; i--)
		{
			for(int j = 0; j <= (data[0].length-i); j ++)
			{
				String ind = ""+j;
				if(!startInds.contains(ind))
					continue;
				String lTok = "";
				for(int k = j; k < j + i; k ++)
				{
					String pos = data[1][k];
					String cPos = pos.substring(0,1);
					String l = data[5][k];    
					lTok+=l+"_"+cPos+" ";
				}
				lTok=lTok.trim();
				if (i > 1) {
					if(allRelatedWords.contains(lTok))
					{
						String tokRep = "";
						for(int k = j; k < j + i; k ++)
						{
							tokRep += k+" ";
							ind = ""+k;
							startInds.remove(ind);
						}
						tokRep=tokRep.trim().replaceAll(" ", "_");
						tokNums.add(tokRep);
					}
				} else {
					String pos = data[1][j];
					String word = data[0][j];
					if (!pos.equals("NNP") && !containsPunc(word)) {
						tokNums.add(j + "");
						ind = "" + j;
						startInds.remove(ind);
					} 
				}
			}			
		}
		return tokNums;
	}	
	
	public boolean containsPunc(String word) {
		char first = word.toLowerCase().charAt(0);
		char last = word.toLowerCase().charAt(word.length()-1);
		return !(Character.isLetter(first) && Character.isLetter(last));
	}
	
	public List<String> trimPrepositions(List<String> tokNums, String[][] pData)
	{
		String[] forbiddenWords1 = {"of course", "in particular", "as", "for", "so", "with","a","an","the","it","i"};
		String[] precedingWordsOf = {"only", "member", "one", "most", "many", "some", "few", "part",
									"majority", "minority", "proportion", "half", "third", "quarter", 
									"all", "none", "share", "much", "%", "face","more"};
		String[] followingWordsOf = {"all", "group", "them", "us", "their"};
						
		Arrays.sort(forbiddenWords1);
		Arrays.sort(precedingWordsOf);
		Arrays.sort(followingWordsOf);

		List<String> result = Lists.newArrayList();
		for(String candTok: tokNums)
		{
			if(candTok.contains("_"))
			{
				result.add(candTok);
				continue;
			}
			int start = Integer.parseInt(candTok);
			int end = Integer.parseInt(candTok);
			/*
			 *forbidden words
			 */
			String token = "";
			String pos = "";
			String tokenLemma = "";
			for(int i = start; i <= end; i ++)
			{
				String tok = pData[0][i].toLowerCase();
				String p = pData[1][i];
				tokenLemma = pData[5][i] + " ";
				token += tok+" ";
				pos += p+" ";
			}
			token=token.trim();
			pos = pos.trim();
			tokenLemma = tokenLemma.trim();
			if(Arrays.binarySearch(forbiddenWords1, token)>=0)
			{
				continue;
			}		
						
			if(start==end)
			{
				String POS = pData[1][start];
				if(POS.startsWith("PR")||
						POS.startsWith("CC")||
						POS.startsWith("IN")||
						POS.startsWith("TO")||
						POS.startsWith("LS")||
						POS.startsWith("FW")||
						POS.startsWith("UH")||
						POS.startsWith("W"))
					continue;				
				if(token.equals("course"))
				{
					if(start>=1)
					{
						String precedingWord = pData[0][start-1].toLowerCase();
						if(precedingWord.equals("of"))
							continue;
					}
				}
				if(token.equals("particular"))
				{
					if(start>=1)
					{
						String precedingWord = pData[0][start-1].toLowerCase();
						if(precedingWord.equals("in"))
							continue;
					}
				}
			}			
			/*
			 * the of case
			 */
			if(token.equals("of"))
			{
				String precedingWord = null;
				String precedingPOS = null;
				String precedingNE = null;
				if(start>=1)
				{
					precedingWord = pData[0][start-1].toLowerCase();
					precedingPOS = pData[1][start-1];
					precedingWord = pData[5][start-1];
					precedingNE = pData[4][start-1];
				}
				else
				{
					precedingWord = "";
					precedingPOS = "";
					precedingNE = "";
				}
				if(Arrays.binarySearch(precedingWordsOf, precedingWord)>=0)
				{
					result.add(candTok);
					continue;
				}
				String followingWord = null;
				String followingPOS = null;
				String followingNE = null;
				if(start<pData[0].length-1)
				{
					followingWord = pData[0][start+1].toLowerCase();
					followingPOS = pData[1][start+1];
					followingNE = pData[4][start+1];
				}
				else
				{
					followingWord = "";
					followingPOS = "";
					followingNE = "";
				}
				if(Arrays.binarySearch(followingWordsOf, followingWord)>=0)
				{
					result.add(candTok);
					continue;
				}
				
				if(precedingPOS.startsWith("JJ") || precedingPOS.startsWith("CD"))
				{
					result.add(candTok);
					continue;
				}
					
				if(followingPOS.startsWith("CD"))
				{
					result.add(candTok);
					continue;
				}
				
				if(followingPOS.startsWith("DT"))
				{
					if(start<pData[0].length-1)
					{
						followingPOS = pData[1][start+2];
						if(followingPOS.startsWith("CD"))
						{
							result.add(candTok);
							continue;
						}
					}
				}
				if(followingNE.startsWith("GPE")||followingNE.startsWith("LOCATION"))
				{
					result.add(candTok);
					continue;
				}
				if(precedingNE.startsWith("CARDINAL"))
				{
					result.add(candTok);
					continue;
				}
				
				continue;
			}
			
			/*
			 * the will case
			 */
			if(token.equals("will"))
			{
				if(pos.equals("MD"))
				{
					continue;
				}
				else
				{
					result.add(candTok);
					continue;
				}
			}
			
			
			if(start==end)
			{
				DependencyParse headNode = mNodeList[start+1];
				/*
				 * the have case
				 *
				 */
				String lToken = tokenLemma;
				String hLemma = "have";
				if(lToken.equals(hLemma))
				{
					List<DependencyParse> children = headNode.getChildren();
					boolean found = false;
					for(DependencyParse parse: children)
					{
						String lab = parse.getLabelType();
						if(lab.equals("OBJ"))
						{
							found = true;
						}
					}
					if(found)
					{
						result.add(candTok);
						continue;
					}
					else
					{
						continue;
					}
				}
				
				/*
				 * the be case
				 */
				lToken = tokenLemma;
				hLemma = "be";
				if(lToken.equals(hLemma))
				{
					continue;
				}
			}		
			result.add(candTok);
		}
		return result;
	}

	@Override
	public List<String> getSegmentations(Iterable<Integer> sentenceIdxs, List<String> parses) {
		ArrayList<String> result = new ArrayList<String>();
		for(int sentenceIdx: sentenceIdxs) {
			String parse = parses.get(sentenceIdx);
			List<String> tokNums = getHighRecallSegmentation(parse, allRelatedWords);
			StringTokenizer st = new StringTokenizer(parse.trim(),"\t");
			int tokensInFirstSent = new Integer(st.nextToken());
			String[][] data = new String[6][tokensInFirstSent];
			for(int k = 0; k < 6; k ++) {
				data[k]=new String[tokensInFirstSent];
				for(int j = 0; j < tokensInFirstSent; j ++) {
					data[k][j]=""+st.nextToken().trim();
				}
			}
			mParse = DependencyParse.processFN(data, 0.0);
			mNodeList = mParse.getIndexSortedListOfNodes();
			mParse.processSentence();
			final List<String> withoutPreps = trimPrepositions(tokNums, data);
			result.add(getTestLine(withoutPreps) + "\t" + sentenceIdx);
		}		
		return result;
	}
}
