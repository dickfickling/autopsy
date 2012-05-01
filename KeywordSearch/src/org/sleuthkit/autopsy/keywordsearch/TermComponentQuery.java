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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.SwingWorker;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.client.solrj.response.TermsResponse.Term;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.windows.TopComponent;
import org.sleuthkit.autopsy.corecomponents.DataResultTopComponent;
import org.sleuthkit.autopsy.keywordsearch.KeywordSearchQueryManager.Presentation;
import org.sleuthkit.datamodel.BlackboardArtifact;
import org.sleuthkit.datamodel.BlackboardArtifact.ARTIFACT_TYPE;
import org.sleuthkit.datamodel.BlackboardAttribute;
import org.sleuthkit.datamodel.BlackboardAttribute.ATTRIBUTE_TYPE;
import org.sleuthkit.datamodel.FsContent;
import org.sleuthkit.datamodel.TskException;

public class TermComponentQuery implements KeywordSearchQuery {

    private static final int TERMS_UNLIMITED = -1;
    //corresponds to field in Solr schema, analyzed with white-space tokenizer only
    private static final String TERMS_SEARCH_FIELD = "content_ws";
    private static final String TERMS_HANDLER = "/terms";
    private static final int TERMS_TIMEOUT = 90 * 1000; //in ms
    private static Logger logger = Logger.getLogger(TermComponentQuery.class.getName());
    private String termsQuery;
    private String queryEscaped;
    private boolean isEscaped;
    private List<Term> terms;
    private Keyword keywordQuery = null;

    public TermComponentQuery(Keyword keywordQuery) {
        this.keywordQuery = keywordQuery;
        this.termsQuery = keywordQuery.getQuery();
        this.queryEscaped = termsQuery;
        isEscaped = false;
        terms = null;
    }

    @Override
    public void escape() {
        queryEscaped = Pattern.quote(termsQuery);
        isEscaped = true;
    }

    @Override
    public boolean validate() {
        if (queryEscaped.equals("")) {
            return false;
        }

        boolean valid = true;
        try {
            Pattern.compile(queryEscaped);
        } catch (PatternSyntaxException ex1) {
            valid = false;
        } catch (IllegalArgumentException ex2) {
            valid = false;
        }
        return valid;
    }

    @Override
    public boolean isEscaped() {
        return isEscaped;
    }

    /*
     * helper method to create a Solr terms component query
     */
    protected SolrQuery createQuery() {
        final SolrQuery q = new SolrQuery();
        q.setQueryType(TERMS_HANDLER);
        q.setTerms(true);
        q.setTermsLimit(TERMS_UNLIMITED);
        q.setTermsRegexFlag("case_insensitive");
        //q.setTermsLimit(200);
        //q.setTermsRegexFlag(regexFlag);
        //q.setTermsRaw(true);
        q.setTermsRegex(queryEscaped);
        q.addTermsField(TERMS_SEARCH_FIELD);
        q.setTimeAllowed(TERMS_TIMEOUT);

        return q;

    }

    /*
     * execute query and return terms, helper method
     */
    protected List<Term> executeQuery(SolrQuery q) throws NoOpenCoreException {
        List<Term> termsCol = null;
        try {
            Server solrServer = KeywordSearch.getServer();
            TermsResponse tr = solrServer.queryTerms(q);
            termsCol = tr.getTerms(TERMS_SEARCH_FIELD);
            return termsCol;
        } catch (SolrServerException ex) {
            logger.log(Level.WARNING, "Error executing the regex terms query: " + termsQuery, ex);
            return null;  //no need to create result view, just display error dialog
        }
    }

    @Override
    public String getEscapedQueryString() {
        return this.queryEscaped;
    }

    @Override
    public String getQueryString() {
        return this.termsQuery;
    }

    @Override
    public Collection<Term> getTerms() {
        return terms;
    }

    @Override
    public KeywordWriteResult writeToBlackBoard(String termHit, FsContent newFsHit, String listName) throws NoOpenCoreException {
        final String MODULE_NAME = KeywordSearchIngestService.MODULE_NAME;

        //snippet
        String snippet = null;
        try {
            snippet = LuceneQuery.querySnippet(KeywordSearchUtil.escapeLuceneQuery(termHit, true, false), newFsHit.getId(), true, true);
        } 
        catch (NoOpenCoreException e) {
            logger.log(Level.WARNING, "Error querying snippet: " + termHit, e);
            throw e;
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Error querying snippet: " + termHit, e);
            return null;
        }

        if (snippet == null || snippet.equals("")) {
            return null;
        }

        //there is match actually in this file, create artifact only then
        BlackboardArtifact bba = null;
        KeywordWriteResult writeResult = null;
        Collection<BlackboardAttribute> attributes = new ArrayList<BlackboardAttribute>();
        try {
            bba = newFsHit.newArtifact(ARTIFACT_TYPE.TSK_KEYWORD_HIT);
            writeResult = new KeywordWriteResult(bba);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error adding bb artifact for keyword hit", e);
            return null;
        }


        //regex match
        attributes.add(new BlackboardAttribute(ATTRIBUTE_TYPE.TSK_KEYWORD.getTypeID(), MODULE_NAME, "", termHit));
        //list
        if (listName == null) {
            listName = "";
        }
        attributes.add(new BlackboardAttribute(ATTRIBUTE_TYPE.TSK_KEYWORD_SET.getTypeID(), MODULE_NAME, "", listName));

        //preview
        attributes.add(new BlackboardAttribute(ATTRIBUTE_TYPE.TSK_KEYWORD_PREVIEW.getTypeID(), MODULE_NAME, "", snippet));

        //regex keyword
        attributes.add(new BlackboardAttribute(ATTRIBUTE_TYPE.TSK_KEYWORD_REGEXP.getTypeID(), MODULE_NAME, "", termsQuery));

        //selector TODO move to general info artifact
            /*
        if (keywordQuery != null) {
        BlackboardAttribute.ATTRIBUTE_TYPE selType = keywordQuery.getType();
        if (selType != null) {
        BlackboardAttribute selAttr = new BlackboardAttribute(selType.getTypeID(), MODULE_NAME, "", regexMatch);
        attributes.add(selAttr);
        }
        } */

        try {
            bba.addAttributes(attributes);
            writeResult.add(attributes);
            return writeResult;
        } catch (TskException e) {
            logger.log(Level.WARNING, "Error adding bb attributes for terms search artifact", e);
        }

        return null;

    }

    @Override
    public Map<String, List<FsContent>> performQuery() throws NoOpenCoreException{
        Map<String, List<FsContent>> results = new HashMap<String, List<FsContent>>();

        final SolrQuery q = createQuery();
        terms = executeQuery(q);


        for (Term term : terms) {
            final String termS = KeywordSearchUtil.escapeLuceneQuery(term.getTerm(), true, false);

            StringBuilder filesQueryB = new StringBuilder();
            filesQueryB.append(TERMS_SEARCH_FIELD).append(":").append(termS);
            final String queryStr = filesQueryB.toString();

            LuceneQuery filesQuery = new LuceneQuery(queryStr);
            try {
                Map<String, List<FsContent>> subResults = filesQuery.performQuery();
                Set<FsContent> filesResults = new HashSet<FsContent>();
                for (String key : subResults.keySet()) {
                    filesResults.addAll(subResults.get(key));
                }
                results.put(term.getTerm(), new ArrayList<FsContent>(filesResults));
            } 
            catch (NoOpenCoreException e) {
                logger.log(Level.WARNING, "Error executing Solr query,", e);
                throw e;
            }
            catch (RuntimeException e) {
                logger.log(Level.WARNING, "Error executing Solr query,", e);
            }

        }


        return results;
    }

    @Override
    public void execute() {
        SolrQuery q = createQuery();

        logger.log(Level.INFO, "Executing TermsComponent query: " + q.toString());

        final SwingWorker<List<Term>, Void> worker = new TermsQueryWorker(q);
        worker.execute();
    }

    /**
     * map Terms to generic Nodes with key/value pairs properties
     * @param terms 
     */
    private void publishNodes(List<Term> terms) {

        Collection<KeyValueQuery> things = new ArrayList<KeyValueQuery>();

        Iterator<Term> it = terms.iterator();
        int termID = 0;
        //long totalMatches = 0;
        while (it.hasNext()) {
            Term term = it.next();
            Map<String, Object> kvs = new LinkedHashMap<String, Object>();
            //long matches = term.getFrequency();
            final String match = term.getTerm();
            KeywordSearchResultFactory.setCommonProperty(kvs, KeywordSearchResultFactory.CommonPropertyTypes.MATCH, match);
            //setCommonProperty(kvs, CommonPropertyTypes.MATCH_RANK, Long.toString(matches));
            //things.add(new KeyValue(match, kvs, ++termID));
            things.add(new KeyValueQuery(match, kvs, ++termID, this));
            //totalMatches += matches;
        }

        Node rootNode = null;
        if (things.size() > 0) {
            Children childThingNodes =
                    Children.create(new KeywordSearchResultFactory(new Keyword(termsQuery, false), things, Presentation.DETAIL), true);

            rootNode = new AbstractNode(childThingNodes);
        } else {
            rootNode = Node.EMPTY;
        }

        final String pathText = "Term query";
        // String pathText = "RegEx query: " + termsQuery
        //+ "    Files with exact matches: " + Long.toString(totalMatches) + " (also listing approximate matches)";

        TopComponent searchResultWin = DataResultTopComponent.createInstance("Keyword search", pathText, rootNode, things.size());
        searchResultWin.requestActive(); // make it the active top component

    }

    class TermsQueryWorker extends SwingWorker<List<Term>, Void> {

        private SolrQuery q;
        private ProgressHandle progress;

        TermsQueryWorker(SolrQuery q) {
            this.q = q;
        }

        @Override
        protected List<Term> doInBackground() throws Exception {
            progress = ProgressHandleFactory.createHandle("Terms query task");
            progress.start();
            progress.progress("Running Terms query.");

            terms = executeQuery(q);

            progress.progress("Terms query completed.");

            return terms;
        }

        @Override
        protected void done() {
            if (!this.isCancelled()) {
                try {
                    List<Term> terms = get();
                    if (terms.isEmpty()) {
                        KeywordSearchUtil.displayDialog("Keyword Search", "No results for regex search: " + termsQuery, KeywordSearchUtil.DIALOG_MESSAGE_TYPE.INFO);

                    } else {
                        publishNodes(terms);
                    }
                } catch (InterruptedException e) {
                    logger.log(Level.INFO, "Exception while executing regex query,", e);

                } catch (ExecutionException e) {
                    logger.log(Level.INFO, "Exception while executing regex query,", e);
                } finally {
                    progress.finish();
                }
            }
        }
    }
}
