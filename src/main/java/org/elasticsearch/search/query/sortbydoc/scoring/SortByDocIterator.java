package org.elasticsearch.search.query.sortbydoc.scoring;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSetIterator;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * samuel
 * 23/10/15, 18:11
 */
class SortByDocIterator extends DocIdSetIterator {
    private Map<Term, Float> scores;
    private final AtomicReaderContext context;
    private final Iterator<Map.Entry<Term, Float>> iterator;
    private Float currentScore;
    private DocsEnum docIterator;
    private int currentDoc = NO_MORE_DOCS;
    private String fieldName;

    SortByDocIterator(String fieldName, Map<Term, Float> scores, AtomicReaderContext context) {
        this.scores = scores;
        this.context = context;
        iterator = scores.entrySet().iterator();
        this.fieldName = fieldName;
    }

    @Override
    public int docID() {
        return currentDoc;
    }

    public Float docScore() {
        return currentScore;
    }

    @Override
    public int nextDoc() throws IOException {
        while (true) {
            if (docIterator != null) {
                int nextDoc = docIterator.nextDoc();
                if (nextDoc != NO_MORE_DOCS) {
                    currentDoc = nextDoc;
                    return nextDoc;
                }
            }
            if (!iterator.hasNext()) {
                return NO_MORE_DOCS;
            }

            Map.Entry<Term, Float> currentScoreEntry = iterator.next();

            docIterator = context.reader().termDocsEnum(currentScoreEntry.getKey());
            currentScore = currentScoreEntry.getValue();
        }
    }

    @Override
    public int advance(int target) throws IOException {
        return this.slowAdvance(target);
    }

    @Override
    public long cost() {
        return scores.size();
    }
}
