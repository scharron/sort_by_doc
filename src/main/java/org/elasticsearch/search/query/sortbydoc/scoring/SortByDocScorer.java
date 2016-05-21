package org.elasticsearch.search.query.sortbydoc.scoring;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

import java.io.IOException;
import java.util.Map;

/**
 * samuel
 * 23/10/15, 15:18
 */
class SortByDocScorer extends Scorer {
    private final Scorer scorer;
    private final DocIdSetIterator iterator;
    private Map<Integer, Float> scores;

    public SortByDocScorer(Scorer scorer, Map<Integer, Float> scores, Weight weight) {
        super(weight);
        this.scorer = scorer;
        this.scores = scores;
        this.iterator = scorer.iterator();
    }

    @Override
    public int docID() {
        return scorer.docID();
    }

    @Override
    public DocIdSetIterator iterator() {
        return new DocIdSetIterator() {
            @Override
            public int docID() {
                return 0;
            }

            @Override
            public int nextDoc() throws IOException {
                while (true) {
                    if (scorer == null)
                        return NO_MORE_DOCS;
                    int docId = iterator.nextDoc();
                    if (docId == NO_MORE_DOCS)
                        return NO_MORE_DOCS;
                    if (scores.containsKey(docId)) {
                        return docId;
                    }
                }
            }

            @Override
            public int advance(int target) throws IOException {
                if (scorer == null)
                    return NO_MORE_DOCS;
                int docId = iterator.advance(target);
                if (docId == NO_MORE_DOCS)
                    return NO_MORE_DOCS;
                // We advanced, but if the document was not in our score set (for whatever reason)
                // then we go to the next valid document by calling nextDoc
                if (scores.containsKey(docId))
                    return docId;
                return nextDoc();
            }

            @Override
            public long cost() {
                if (scorer == null)
                    return 0;
                return iterator().cost();
            }
        };
    }

    @Override
    public int freq() throws IOException {
        return 1;
    }

    @Override
    public float score() throws IOException {
        if (scorer == null)
            return 0;
        return scores.get(scorer.docID());
    }
}
