/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.search.query.sortbydoc;

import com.google.common.collect.ImmutableMap;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.mapper.Uid;
import org.elasticsearch.index.mapper.internal.IdFieldMapper;
import org.elasticsearch.index.mapper.internal.UidFieldMapper;
import org.elasticsearch.index.query.QueryParseContext;
import org.elasticsearch.index.query.QueryParser;
import org.elasticsearch.index.query.QueryParsingException;
import org.elasticsearch.search.internal.SearchContext;
import org.elasticsearch.search.query.sortbydoc.utils.ScoresLookup;
import org.elasticsearch.search.query.sortbydoc.utils.ScoringDocumentCache;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * <pre>
 * "sort_by_doc" : {
 *  "doc_id": "my_id"
 *  "type": "my_type"
 *  "index": "my_index"
 *  "root": "path_to_the_list_of_scores"
 *  "id": "field_for_ids"
 *  "score": "field_for_score"
 *  "query": {...}
 * }
 * </pre>
 */
public class SortByDocQueryParser implements QueryParser {
    public static final String NAME = "sort_by_doc";
    private ScoringDocumentCache scoringDocumentCache;

    public static final String EXECUTION_KEY = "execution";
    public static final String EXECUTION_VALUE_PLAIN = "plain";
    public static final String EXECUTION_VALUE_FIELDDATA = "fielddata";
    public static final String EXECUTION_VALUE_BOOL = "bool";
    public static final String EXECUTION_VALUE_BOOL_NOCACHE = "bool_nocache";
    public static final String EXECUTION_VALUE_AND = "and";
    public static final String EXECUTION_VALUE_AND_NOCACHE = "and_nocache";
    public static final String EXECUTION_VALUE_OR = "or";
    public static final String EXECUTION_VALUE_OR_NOCACHE = "or_nocache";

    @Inject
    public SortByDocQueryParser() {
    }

    @Override
    public String[] names() {
        return new String[]{NAME, "in"};
    }

    @Inject(optional = true)
    public void setScoringDocumentCache(ScoringDocumentCache scoringDocumentCache) {
        this.scoringDocumentCache = scoringDocumentCache;
    }

    @Override
    public Query parse(QueryParseContext parseContext) throws IOException, QueryParsingException {
        XContentParser parser = parseContext.parser();

        MapperService.SmartNameFieldMappers smartNameFieldMappers;
        String currentFieldName = null;

        String lookupIndex = parseContext.index().name();
        String lookupType = null;
        String lookupId = null;
        String rootPath = null;
        String idField = null;
        String scoreField = null;
        String lookupRouting = null;
        Query subQuery = null;

        XContentParser.Token token;

        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT) {
                if ("query".equals(parser.currentName())) {
                    subQuery = parseContext.parseInnerQuery();
                    continue;
                }
            } else if (token.isValue()) {
                if (false) {
                } else if ("index".equals(currentFieldName)) {
                    lookupIndex = parser.text();
                } else if ("type".equals(currentFieldName)) {
                    lookupType = parser.text();
                } else if ("doc_id".equals(currentFieldName)) {
                    lookupId = parser.text();
                } else if ("root".equals(currentFieldName)) {
                    rootPath = parser.text();
                } else if ("id".equals(currentFieldName)) {
                    idField = parser.text();
                } else if ("score".equals(currentFieldName)) {
                    scoreField = parser.text();
                } else if ("routing".equals(currentFieldName)) {
                    lookupRouting = parser.textOrNull();
                } else {
                    throw new QueryParsingException(parseContext.index(), "[sort_by_doc] query does not support [" + currentFieldName + "] within lookup element");
                }
            }
        }
        if (lookupType == null) {
            throw new QueryParsingException(parseContext.index(), "[sort_by_doc] query lookup element requires specifying the type");
        }
        if (lookupId == null) {
            throw new QueryParsingException(parseContext.index(), "[sort_by_doc] query lookup element requires specifying the doc_id");
        }
        if (rootPath == null) {
            throw new QueryParsingException(parseContext.index(), "[sort_by_doc] query lookup element requires specifying the path");
        }
        if (idField == null) {
            throw new QueryParsingException(parseContext.index(), "[sort_by_doc] query lookup element requires specifying the id");
        }
        if (scoreField == null) {
            throw new QueryParsingException(parseContext.index(), "[sort_by_doc] query lookup element requires specifying the score");
        }

        if (subQuery == null) {
            throw new QueryParsingException(parseContext.index(), "[sort_by_doc] query requires a subquery");
        }

        String fieldName = "_id";
        FieldMapper fieldMapper = null;
        smartNameFieldMappers = parseContext.smartFieldMappers(fieldName);
        if (smartNameFieldMappers != null) {
            if (smartNameFieldMappers.hasMapper()) {
                fieldMapper = smartNameFieldMappers.mapper();
                fieldName = fieldMapper.names().indexName();
            }
        }

        if (fieldMapper == null || !(fieldMapper instanceof IdFieldMapper))
            throw new QueryParsingException(parseContext.index(), "[sort_by_doc] the _id field must be a defaultly indexed UID field");

        // external lookup, use it
        ScoresLookup scoresLookup = new ScoresLookup(lookupIndex, lookupType, lookupId, lookupRouting, rootPath, idField, scoreField, parseContext, SearchContext.current());
        ImmutableMap<String, Float> scores = scoringDocumentCache.getScores(scoresLookup);
        Map<Term, Float> termsScores = new HashMap<>();
        for (Map.Entry<String, Float> score : scores.entrySet()) {
            BytesRef[] keyUids = Uid.createTypeUids(parseContext.queryTypes(), score.getKey());
            for (BytesRef keyUid : keyUids) {
                Term t = new Term(UidFieldMapper.NAME, keyUid);
                termsScores.put(t, score.getValue());
            }
        }

        Filter filter = fieldMapper.termsFilter(scores.keySet().asList(), parseContext);

        return new SortByDocQuery(fieldName, subQuery, filter, termsScores);
    }
}