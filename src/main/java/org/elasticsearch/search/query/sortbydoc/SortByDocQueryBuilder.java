package org.elasticsearch.search.query.sortbydoc;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BaseQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermsFilterParser;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;

/**
 * samuel
 * 19/11/15, 15:57
 */
public class SortByDocQueryBuilder extends BaseQueryBuilder {
    private String lookupIndex;
    private String lookupType;
    private String lookupId;
    private String lookupRouting;

    private String rootPath;
    private String idField;
    private String scoreField;
    private QueryBuilder subQuery;
    private SortOrder sortOrder;

    /**
     * Sets the query to filter & sort
     */
    public SortByDocQueryBuilder query(QueryBuilder subQuery) {
        this.subQuery = subQuery;
        return this;
    }

    /**
     * Sets the routing for the doc to lookup
     */
    public SortByDocQueryBuilder query(String lookupRouting) {
        this.lookupRouting = lookupRouting;
        return this;
    }

    /**
     * Sets the index name to lookup the terms from.
     */
    public SortByDocQueryBuilder lookupIndex(String lookupIndex) {
        this.lookupIndex = lookupIndex;
        return this;
    }

    /**
     * Sets the index type to lookup the terms from.
     */
    public SortByDocQueryBuilder lookupType(String lookupType) {
        this.lookupType = lookupType;
        return this;
    }

    /**
     * Sets the doc id to lookup the terms from.
     */
    public SortByDocQueryBuilder lookupId(String lookupId) {
        this.lookupId = lookupId;
        return this;
    }

    /**
     * Sets the path within the document to lookup the items from.
     */
    public SortByDocQueryBuilder rootPath(String rootPath) {
        this.rootPath = rootPath;
        return this;
    }

    /**
     * Sets the field name to retrieve ids in objects found at rootPath
     */
    public SortByDocQueryBuilder idField(String idField) {
        this.idField = idField;
        return this;
    }

    /**
     * Sets the field name to retrieve scores in objects found at rootPath
     */
    public SortByDocQueryBuilder scoreField(String scoreField) {
        this.scoreField = scoreField;
        return this;
    }

    /**
     * Sets the field name to retrieve scores in objects found at rootPath
     */
    public SortByDocQueryBuilder sortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    @Override
    public void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(SortByDocQueryParser.NAME);

        if (lookupIndex != null) {
            builder.field("index", lookupIndex);
        }
        builder.field("type", lookupType);
        builder.field("doc_id", lookupId);
        if (lookupRouting != null) {
            builder.field("routing", lookupRouting);
        }

        if (subQuery != null) {
            builder.field("query");
            subQuery.toXContent(builder, params);
        }

        builder.field("root", rootPath);
        builder.field("id", idField);
        builder.field("score", scoreField);
        builder.field("sort_order", sortOrder);

        builder.endObject();
    }
}
