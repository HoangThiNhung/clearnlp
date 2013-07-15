/**
* Copyright 2012-2013 University of Massachusetts Amherst
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.googlecode.clearnlp.demo;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.List;

import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.nlp.NLPDecode;
import com.googlecode.clearnlp.nlp.NLPLib;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.segmentation.AbstractSegmenter;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;

/**
 * @since 1.1.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class DemoNLPDecoder
{
	final String language = AbstractReader.LANG_EN;
	
	public DemoNLPDecoder(String dictFile, String modelFile, String inputFile, String outputFile) throws Exception
	{
		AbstractTokenizer tokenizer  = EngineGetter.getTokenizer(language, new FileInputStream(dictFile));
		AbstractComponent tagger     = EngineGetter.getComponent(new FileInputStream(modelFile) , language, NLPLib.MODE_POS);
		AbstractComponent analyzer   = EngineGetter.getComponent(new FileInputStream(dictFile)     , language, NLPLib.MODE_MORPH);
		AbstractComponent parser     = EngineGetter.getComponent(new FileInputStream(modelFile) , language, NLPLib.MODE_DEP);
		AbstractComponent identifier = EngineGetter.getComponent(new FileInputStream(modelFile), language, NLPLib.MODE_PRED);
		AbstractComponent classifier = EngineGetter.getComponent(new FileInputStream(modelFile), language, NLPLib.MODE_ROLE);
		AbstractComponent labeler    = EngineGetter.getComponent(new FileInputStream(modelFile) , language, NLPLib.MODE_SRL);
		
		AbstractComponent[] components = {tagger, analyzer, parser, identifier, classifier, labeler};
		
		String sentence = "I'd like to meet Dr. Choi.";
		process(tokenizer, components, sentence);
		process(tokenizer, components, UTInput.createBufferedFileReader(inputFile), UTOutput.createPrintBufferedFileStream(outputFile));
	}
	
	public void process(AbstractTokenizer tokenizer, AbstractComponent[] components, String sentence)
	{
		NLPDecode nlp = new NLPDecode();
		DEPTree tree = nlp.toDEPTree(tokenizer.getTokens(sentence));
		
		for (AbstractComponent component : components)
			component.process(tree);

		System.out.println(tree.toStringSRL()+"\n");
	}
	
	public void process(AbstractTokenizer tokenizer, AbstractComponent[] components, BufferedReader reader, PrintStream fout)
	{
		AbstractSegmenter segmenter = EngineGetter.getSegmenter(language, tokenizer);
		NLPDecode nlp = new NLPDecode();
		DEPTree tree;
		
		for (List<String> tokens : segmenter.getSentences(reader))
		{
			tree = nlp.toDEPTree(tokens);
			
			for (AbstractComponent component : components)
				component.process(tree);
			
			fout.println(tree.toStringSRL()+"\n");
		}
		
		fout.close();
	}

	public static void main(String[] args)
	{
		String dictFile   = args[0];	// e.g., dictionary.zip
		String modelFile  = args[1];	// e.g., ontonotes-en-version.tgz
		String inputFile  = args[2];
		String outputFile = args[3];

		try
		{
			new DemoNLPDecoder(dictFile, modelFile, inputFile, outputFile);
		}
		catch (Exception e) {e.printStackTrace();}
	}
}
