package br.ufmt.periscope.indexer.resources.analysis;

import java.io.Serializable;

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
 *
 *
 * @author mattyws
 */
@Named
public class DataSignaturesAnalyzer extends Analyzer  implements Serializable{

    private static final long serialVersionUID = 1L;

    @Inject
    private CommonDescriptorsSet descriptorSet;

    public DataSignaturesAnalyzer() {
    }

    /** For unit tests without CDI. */
    public DataSignaturesAnalyzer(CommonDescriptorsSet descriptorSet) {
        this.descriptorSet = descriptorSet;
    }
    
    @Override
    protected TokenStreamComponents createComponents(String field) {
        // Tokenizes the string by withespace
        Tokenizer source = new WhitespaceTokenizer();
        TokenStream sink = null;

        LowerCaseFilter lowerCaseFilter = new LowerCaseFilter(source);
        
        ASCIIFoldingFilter aSCIIFoldingFilter = new ASCIIFoldingFilter(lowerCaseFilter);
        
        StopFilter stopFilter = new StopFilter(aSCIIFoldingFilter, EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
        
        CommonDescriptorsTokenFilter commonDescriptorsTokenFilter = new CommonDescriptorsTokenFilter(stopFilter, descriptorSet);
        
        sink = new DataSignaturesTokenFilter(commonDescriptorsTokenFilter);

        return new TokenStreamComponents(source, sink);
        
    }

}
