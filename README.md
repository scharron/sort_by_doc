Sort Results using another Document
===================================

Whenever you have a large set of documents, and you want your user to be able to search on a subset, you can use the [Term Filter](https://www.elastic.co/guide/en/elasticsearch/reference/1.7/query-dsl-terms-filter.html).
However, sometimes you also want to have a sort order dependant on your user, without having to duplicate your documents.
This plugin enables you to define a mapping of document ids to scores, so that only documents present in the mapping are returned, and they are returned in the order given by the scores.
This kind of query also enables you to add your own search criterias to furthermore restrict the number of documents returned.


1. Build this plugin:

        mvn clean compile test package 
        PLUGIN_PATH=`pwd`/target/releases/sort-by-doc-0.0.1-SNAPSHOT.zip

2. Install the PLUGIN

        cd $ELASTICSEARCH_HOME
        ./bin/plugin -url file:/$PLUGIN_PATH -install sort-by-doc

3. Updating the plugin

        cd $ELASTICSEARCH_HOME
        ./bin/plugin -remove sort-by-doc
        ./bin/plugin -url file:/$PLUGIN_PATH -install sort-by-doc


Usage
==========

##### Version

ElasticSearch version 1.7.1



##### Expected format of sort document

The document must contain a list of objects.
These objects must contains two fields: an id and a score.

        {
          "_id": "sort_doc_for_user_1",
          "sort_object": [
            { "id": "doc_id_1", "score": 1 },
            { "id": "doc_id_2", "score": 2 },
            ...
          ]
          "other_fields": ...
        }


##### Query Parameters
* query - A subquery that will be filtered and scored
* index - The index name where to find the sort document 
* type - The type where to find the sort document 
* doc_id - The id of the sort document
* root - The path to the list of objets
* id - the field name of document ids in the objects
* score - the field name of score values in the objects



###### Example

See test.sh
