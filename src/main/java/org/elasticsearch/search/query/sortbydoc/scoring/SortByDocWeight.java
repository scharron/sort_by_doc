package org.elasticsearch.search.query.sortbydoc.scoring;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.*;
import org.elasticsearch.index.mapper.internal.UidFieldMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * samuel
 * 22/10/15, 14:55
 */
public class SortByDocWeight extends Weight {
    private final String fieldName;
    private Weight subWeight;
    private Map<Term, Float> scores;
    private Query query;

    public SortByDocWeight(Query query, String fieldName, Map<Term, Float> scores, Weight subWeight) {
        super(query);
        this.scores = scores;
        this.query = query;
        this.fieldName = fieldName;
        this.subWeight = subWeight;
    }

    @Override
    public Explanation explain(LeafReaderContext context, int doc) throws IOException {
        Float score = getScores(context).get(doc);
        final Explanation explanation = Explanation.match(score, "sort_by_doc", subWeight.explain(context, doc));
        return explanation;
    }

    @Override
    public void extractTerms(Set<Term> set) {
    }

    @Override
    public float getValueForNormalization() throws IOException {
        return 1;
    }

    @Override
    public void normalize(float norm, float topLevelBoost) {
        // Do nothing since we are assigning a custom score to each doc
        // subWeight.normalize(norm, topLevelBoost);
    }


    @Override
    public Scorer scorer(LeafReaderContext context) throws IOException {
        return new SortByDocScorer(subWeight.scorer(context), getScores(context), this);
    }

    private Map<Integer, Float> getScores(LeafReaderContext context) throws IOException {
        Map<Integer, Float> scores = new HashMap<>();
        TermsEnum termsIterator = context.reader().fields().terms(UidFieldMapper.NAME).iterator();
        for (Map.Entry<Term, Float> score : this.scores.entrySet()) {
            if (!termsIterator.seekExact(score.getKey().bytes())) {
                // Term not found
                continue;
            }
            DocsEnum doc = termsIterator.docs(null, null);
            if (doc.nextDoc() == DocIdSetIterator.NO_MORE_DOCS)
                continue;
            scores.put(doc.docID(), score.getValue());
            assert doc.nextDoc() == DocIdSetIterator.NO_MORE_DOCS;
        }

        return scores;
    }

}
