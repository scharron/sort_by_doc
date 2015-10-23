package org.elasticsearch.search.query.sortbydoc.scoring;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Bits;

import java.io.IOException;
import java.util.Map;

/**
 * samuel
 * 22/10/15, 14:55
 */
public class SortByDocWeight extends Weight {
    private final String fieldName;
    private Map<Term, Float> scores;
    private Query query;

    public SortByDocWeight(Query query, String fieldName, Map<Term, Float> scores) {
        this.scores = scores;
        this.query = query;
        this.fieldName = fieldName;
    }

    @Override
    public Explanation explain(AtomicReaderContext context, int doc) throws IOException {
        return null;
    }

    @Override
    public Query getQuery() {
        return query;
    }

    @Override
    public float getValueForNormalization() throws IOException {
        return 1;
    }

    @Override
    public void normalize(float norm, float topLevelBoost) {
        // Do nothing since we are assigning a custom score to each doc
    }



    @Override
    public Scorer scorer(AtomicReaderContext context, Bits acceptDocs) throws IOException {
        return new SortByDocScorer(new SortByDocIterator(fieldName, scores, context), this);
    }

}
