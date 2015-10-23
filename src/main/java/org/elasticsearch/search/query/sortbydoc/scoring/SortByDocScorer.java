package org.elasticsearch.search.query.sortbydoc.scoring;

import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

import java.io.IOException;

/**
 * samuel
 * 23/10/15, 15:18
 */
class SortByDocScorer extends Scorer {
    private final SortByDocIterator iterator;

    public SortByDocScorer(SortByDocIterator iterator, Weight weight) {
        super(weight);
        this.iterator = iterator;
    }

    @Override
    public int docID() {
        return iterator.docID();
    }

    @Override
    public int nextDoc() throws IOException {
        return iterator.nextDoc();
    }

    @Override
    public int advance(int target) throws IOException {
        return iterator.advance(target);
    }

    @Override
    public long cost() {
        return iterator.cost();
    }

    @Override
    public int freq() throws IOException {
        return 1;
    }

    @Override
    public float score() throws IOException {
        return iterator.docScore();
    }
}
