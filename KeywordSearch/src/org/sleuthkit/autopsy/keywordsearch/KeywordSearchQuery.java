/*
 * Autopsy Forensic Browser
 *
 * Copyright 2011 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.keywordsearch;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.solr.client.solrj.response.TermsResponse.Term;
import org.sleuthkit.datamodel.FsContent;

public interface KeywordSearchQuery {

    /**
     * validate the query pre execution
     * @return true if the query passed validation
     */
    public boolean validate();

    
    /**
     * execute query and return results without publishing them
     * return results for all matching terms
     * @throws NoOpenCoreException if query failed due to server error, this could be a notification to stop processing
     * @return 
     */
    public Map<String,List<FsContent>> performQuery() throws NoOpenCoreException;
    
  
    
    /**
     * execute the query and publish results
     */
    public void execute();
    
    /**
     * escape the query string and use the escaped string in the query
     */
    public void escape();
    
    /**
     * 
     * @return true if query was escaped
     */
    public boolean isEscaped();
    
    /**
     * return original query string
     * @return the query String supplied originally
     */
    public String getQueryString();
    
    /**
     * return escaped query string if escaping was done
     * @return the escaped query string, or original string if no escaping done
     */
    public String getEscapedQueryString();
    
    /**
     * get terms associated with the query if any
     * @return collection of terms associated with the query
     */
    public Collection<Term>getTerms();
    
    /**
     * write results to blackboard per single term and file hit
     * this method is useful if something else should keep track of partial results to write
     * @param termHit term for only which to write results
     * @param newFsHit fscontent for which to write results for this hit
     * @param listName listname
     * @return collection of results (with cached bb artifacts/attributes) created and written
     * @throws NoOpenCoreException if could not write to bb because required query failed due to server error, this could be a notification to stop processing
     */
    public KeywordWriteResult writeToBlackBoard(String termHit, FsContent newFsHit, String listName) throws NoOpenCoreException;
       
}

