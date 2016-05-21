package org.elasticsearch.search.query.sortbydoc;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.util.ToStringUtils;
import org.elasticsearch.search.query.sortbydoc.scoring.SortByDocWeight;

import java.io.IOException;
import java.util.Map;

/**
 * samuel
 * 22/10/15, 14:11
 */
public class SortByDocQuery extends Query {
    private final String fieldName;
    private Query subQuery;
    private Map<Term, Float> scores;

    SortByDocQuery(String fieldName, Query subQuery, Query filter, Map<Term, Float> scores) {
        this.fieldName = fieldName;
        this.subQuery = new BooleanQuery.Builder()
                .add(subQuery, BooleanClause.Occur.MUST)
                .add(filter, BooleanClause.Occur.FILTER).build();
        this.scores = scores;
    }

    @Override
    public Query rewrite(IndexReader reader) throws IOException {
        Query newSubQuery = subQuery.rewrite(reader);
        if (newSubQuery == subQuery)
            return this;
        SortByDocQuery newQuery = (SortByDocQuery) this.clone();
        newQuery.subQuery = newSubQuery;
        return newQuery;
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
        return new SortByDocWeight(this, fieldName, scores, subQuery.createWeight(searcher, needsScores));
    }

    @Override
    public String toString(String field) {
        StringBuilder sb = new StringBuilder();
        sb.append("sort by doc (")
                .append(subQuery.toString(field));
        sb.append(ToStringUtils.boost(getBoost()));
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        SortByDocQuery other = (SortByDocQuery) o;
        if (!this.subQuery.equals(other.subQuery)) {
            return false;
        }
        if (!this.scores.equals(other.scores)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return subQuery.hashCode() + 31 * scores.hashCode() ^ Float.floatToIntBits(getBoost());
    }
}
