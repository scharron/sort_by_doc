request() {
    echo $1
    shift
    curl "$@"
    echo
}

request "Deleting index" -XDELETE localhost:9200/test

request "Creating index" -XPOST localhost:9200/test -d '{
    "mappings": {
        "docs_to_sort": {
            "properties": {
                "sort_field": {
                    "type": "object",
                    "properties": {
                        "id": { "type" : "string", "index" : "not_analyzed" },
                        "score": { "type" : "float" }
                    }
                }
            }
        },
        "my_docs" : {
            "properties" : {
                "a_field" : { "type" : "string", "index" : "not_analyzed" }
            }
        }
    }
}'

request "Inserting documents" -XPUT localhost:9200/test/my_docs/doc_1 -d '{"a_field": "a"}'
request "" -XPUT localhost:9200/test/my_docs/doc_2 -d '{"a_field": "b"}'
request "" -XPUT localhost:9200/test/my_docs/doc_3 -d '{"a_field": "a"}'
request "" -XPUT localhost:9200/test/my_docs/doc_4 -d '{"a_field": "a"}'
request "" -XPUT localhost:9200/test/my_docs/doc_5 -d '{"a_field": "a"}'

request "Inserting sorting document" -XPUT localhost:9200/test/docs_to_sort/pouet -d '{
    "sort_field": [ 
        { "id": "doc_5", "score": 1 }
    ]
}'


echo "Sleeping 1 second (indexation time)..."
sleep 3


#request "Performing the query" -XGET localhost:9200/test/my_docs/_search -d '{
#    "explain": true,
#    "query": {
#        "sort_by_doc": {
#            "type": "docs_to_sort",
#            "doc_id": "pouet",
#            "root": "sort_field",
#            "id": "id",
#            "score": "score",
#            "query": {
#                "term": {
#                    "a_field": "a"
#                }
#            }
#        }
#    }
#}'

request "Cleaning cache" -XPOST 'localhost:9200/test/_cache/clear?filter_keys=pouet'

request "Performing the query" -XGET localhost:9200/test/my_docs/_search?pretty=1 -d '{
    "explain": true,
    "query": {
        "filtered": {
            "filter": {
                "terms": {
                    "_id": {
                        "type": "docs_to_sort",
                        "id": "pouet",
                        "path": "sort_field.id"
                    },
                    "_cache": false,
                    "_cache_key" : "pouet"
                }
            }
        }
    }
}'
