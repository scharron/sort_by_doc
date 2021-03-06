/* Copyright 2013 Endgame, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  
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
package org.elasticsearch.plugin.sortbydoc;

import org.elasticsearch.indices.IndicesModule;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.search.query.sortbydoc.SortByDocQueryParser;


public class SortByDocPlugin extends Plugin {

    @Override
    public String name() {
        return "sort-by-doc-plugin";
    }

    @Override
    public String description() {
        return "Sort documents by the content of another document";
    }

    public void onModule(IndicesModule indicesModule) {
        indicesModule.registerQueryParser(SortByDocQueryParser.class);
    }

}
