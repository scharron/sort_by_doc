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

request "Inserting documents" -XPUT localhost:9200/test/my_docs/doc_1 -d '{"a_field": 1}'
request "" -XPUT localhost:9200/test/my_docs/doc_2 -d '{"a_field": 2}'
request "" -XPUT localhost:9200/test/my_docs/doc_3 -d '{"a_field": 3}'
request "" -XPUT localhost:9200/test/my_docs/doc_4 -d '{"a_field": 4}'
request "" -XPUT localhost:9200/test/my_docs/doc_5 -d '{"a_field": 5}'

request "Inserting sorting document" -XPUT localhost:9200/test/docs_to_sort/pouet -d '{
    "sort_field": [ 
        { "id": "doc_5", "score": 1 },
        { "id": "doc_1", "score": 2 },
        { "id": "doc_2", "score": 3 },
        { "id": "doc_3", "score": 4 },
        { "id": "doc_6", "score": 5 }
    ]
}'


echo "Sleeping 1 second (indexation time)..."
sleep 1


request "Performing the query" -XGET localhost:9200/test/my_docs/_search -d '{
    "query": {
        "sort_by_doc": {
            "type": "docs_to_sort",
            "doc_id": "pouet",
            "root": "sort_field",
            "id": "id",
            "score": "score",
            "query": {
                "match_all": {}
            }
        }
    }
}'
