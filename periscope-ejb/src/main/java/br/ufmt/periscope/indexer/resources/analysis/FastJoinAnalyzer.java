package br.ufmt.periscope.indexer.resources.analysis;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;

/**
 * The PatenteeAnalyzer class will make the patentee names pre-processing, the
 * operations is : Normalize (pass the characters to lowercase); Pass the chars
 * to your ASCII equivalents; Removes the English Stopwords; Removes the Common
 * Descriptors of the names; Condense the name, removing all the withespaces;
 * Create the Acronym for the name
 *
 * @author mattyws
 */
@Named
public class FastJoinAnalyzer extends Analyzer {
    
    @Inject
    private CommonDescriptorsSet descriptorSet;

    public FastJoinAnalyzer() {
    }

    /** For unit tests without CDI. */
    public FastJoinAnalyzer(CommonDescriptorsSet descriptorSet) {
        this.descriptorSet = descriptorSet;
    }
    
    @Override
    protected TokenStreamComponents createComponents(String field) {
        // Tokenizes the string by withespace
        Tokenizer source = new WhitespaceTokenizer();
        
        LowerCaseFilter lowerCaseFilter = new LowerCaseFilter(source);
        
        ASCIIFoldingFilter aSCIIFoldingFilter = new ASCIIFoldingFilter(lowerCaseFilter);
        
        StopFilter stopFilter = new StopFilter(aSCIIFoldingFilter, EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
        
        CommonDescriptorsTokenFilter commonDescriptorsTokenFilter = new CommonDescriptorsTokenFilter(stopFilter, descriptorSet);

        return new TokenStreamComponents(source, commonDescriptorsTokenFilter);
        
    }

}
