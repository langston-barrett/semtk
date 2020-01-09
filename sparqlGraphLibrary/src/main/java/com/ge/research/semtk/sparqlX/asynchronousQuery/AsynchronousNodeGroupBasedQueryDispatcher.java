/**
 ** Copyright 2016 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package com.ge.research.semtk.sparqlX.asynchronousQuery;

import java.net.ConnectException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import org.json.simple.JSONObject;

import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClient;
import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.Returnable;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.EndpointNotFoundException;
import com.ge.research.semtk.edc.client.OntologyInfoClient;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.resultSet.NodeGroupResultSet;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.client.SparqlQueryAuthClientConfig;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClient;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;
import com.ge.research.semtk.utility.LocalLogger;
/**
 * @author Justin
 *
 * Base class for dispatchers.
 *
 */
public abstract class AsynchronousNodeGroupBasedQueryDispatcher {

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	protected NodeGroup queryNodeGroup;
	protected OntologyInfoClient oInfoClient;
	protected NodeGroupStoreRestClient ngStoreClient;
	protected JobTracker jobTracker;
	protected ResultsClient resultsClient;
	
	protected SparqlEndpointInterface querySei;

	protected String jobID;
	protected OntologyInfo oInfo;
	protected String domain;
	
	public final static String FLAG_DISPATCH_RETURN_QUERIES = "DISPATCH_RETURN_QUERIES";
	
	public AsynchronousNodeGroupBasedQueryDispatcher(String jobId, SparqlGraphJson sgJson, StatusClient sClient, ResultsClient rClient, SparqlEndpointInterface extConfigSei, boolean unusedFlag, OntologyInfoClient oInfoClient, NodeGroupStoreRestClient ngStoreClient) throws Exception{
		this.jobID = jobId;
		
		this.oInfoClient = oInfoClient;
		this.ngStoreClient = ngStoreClient;
		
		// change sClient into a jobs Sei borrowing the username password from the extConfigSei.
		SparqlEndpointInterface jobSei = sClient.getJobTrackerSei();
		jobSei.setUserAndPassword(extConfigSei.getUserName(), extConfigSei.getPassword());
		this.jobTracker = new JobTracker(jobSei);
		
		this.resultsClient = rClient;
		// get nodegroup and sei from json
		LocalLogger.logToStdErr("processing incoming nodegroup - in base class");

		LocalLogger.logToStdErr("about to get the nodegroup");
		this.queryNodeGroup = sgJson.getNodeGroup();

		LocalLogger.logToStdErr("about to get the default qry interface");
		this.querySei = sgJson.getSparqlConn().getDefaultQueryInterface();
		
		// security question: for virtuoso sake, we pass on the authorization "dba" "dba" 
		//     from extConfigSei to on the querySei in case it is an insert or delete
		this.querySei.setUserAndPassword(extConfigSei.getUserName(), extConfigSei.getPassword());
		
		SparqlConnection nodegroupConn = sgJson.getSparqlConn();
		this.domain = nodegroupConn.getDomain();
		this.oInfo = oInfoClient.getOntologyInfo(nodegroupConn);
		this.queryNodeGroup.validateAgainstModel(oInfo);
		
		this.jobTracker.setJobPercentComplete(this.jobID, 1);
	}
	
	/**
	 * return the JobID. the clients will need this.
	 * @return
	 */
	public String getJobId(){
		return this.jobID;
	}
	
	public abstract void execute(Object executionSpecificObject1, Object executionSpecificObject2, DispatcherSupportedQueryTypes qt, String targetSparqlID);
	
	/**
	 * send the collected results to the results service. 
	 * this will just return a true/false about whether the results were likely sent.
	 * @throws Exception 
	 * @throws EndpointNotFoundException 
	 * @throws ConnectException 
	 */
	protected void sendResultsToService(TableResultSet currResults) throws ConnectException, EndpointNotFoundException, Exception{
				
		HashMap<String, Integer> colInstCounter = new HashMap<String, Integer>();
			
		try{
			
			// repair column headers in the event that a duplicate header is encountered. by convention (established and existing only here), the first instance of a column name 
			// will remain unchanged, all future instances will be postfixed with "[X]" where X is the count encountered so far. this count will start at 1. 
			Table resTable = currResults.getTable();
			String[] unModColnames = resTable.getColumnNames(); 	// pre-modification column names
			String[] modColnames = new String[unModColnames.length];
			
			int posCount = 0;
			for(String uCol : unModColnames){
				if(colInstCounter.containsKey( uCol.toLowerCase() )){
					// seen this one already. update the counter and add it to the new header list.
					int update = colInstCounter.get( uCol.toLowerCase() ) + 1;
					colInstCounter.put( uCol.toLowerCase() , update);
					
					modColnames[posCount] = uCol + "[" + update + "]";
				}
				else{
					// never seen this column.
					modColnames[posCount] = uCol;
					// add to the hash
					colInstCounter.put( uCol.toLowerCase(), 0 );
				}
				
				posCount+=1;
			}
			resTable.replaceColumnNames(modColnames);
			
			this.resultsClient.execStoreTableResults(this.jobID, resTable);
		}
		catch(Exception e){
			this.jobTracker.setJobFailure(this.jobID, "Failed to write results: " + e.getMessage());
			LocalLogger.printStackTrace(e);
			throw new Exception("Unable to write results", e);
		}
	}
	
	
	private void sendResultsToService(NodeGroupResultSet preRet)  throws ConnectException, EndpointNotFoundException, Exception{
		try{
			JSONObject resJSON = preRet.getResultsJSON();
			this.resultsClient.execStoreGraphResults(this.jobID, resJSON);
		}
		catch(Exception e){
			this.jobTracker.setJobFailure(this.jobID, "Failed to write results: " + e.getMessage());
			LocalLogger.printStackTrace(e);
			throw new Exception("Unable to write results", e);
		}
	}

	
	/**
	 * send updates to the status service. 
	 * @param statusPercentNumber
	 * @throws UnableToSetStatusException
	 */
	protected void updateStatus(int statusPercentNumber) throws UnableToSetStatusException{
		
		try {
		// if statusPercentNumber >= 100, instead, set the success or failure.
			if(statusPercentNumber >= 100){
				this.jobTracker.setJobSuccess(this.jobID);
			}
			// else, try to set a value.
			else{
				this.jobTracker.setJobPercentComplete(this.jobID, statusPercentNumber);
			}
			
		} catch (Exception e) {
			throw new UnableToSetStatusException(e.getMessage());
		}
	}
	
	protected void incrementStatus(int increment, int max) throws UnableToSetStatusException{
		
		try {
			this.jobTracker.incrementPercentComplete(this.jobID, increment, max);
		} catch (Exception e) {
			throw new UnableToSetStatusException(e.getMessage());
		}
	}
	
	
	/**
	 * the work failed. let the callers know via the status service. 
	 * @param rationale
	 * @throws UnableToSetStatusException - no longer.  Paul modified 8/2019
	 */
	protected void updateStatusToFailed(String rationale) { // throws UnableToSetStatusException {
		try{
			this.jobTracker.setJobFailure(this.jobID, rationale != null ? rationale : "Exception with e.getMessage()==null");
			LocalLogger.logToStdErr("wrote failure message to status service");
		}
		catch(Exception eee){
			LocalLogger.logToStdErr("failed to write failure message to status service");
			//throw new UnableToSetStatusException(eee.getMessage());
		}
	}
	
	/**
	 * used for testing the service/ probably not useful in practice
	 * @return
	 */
	/**
	 * used for testing the service/ probably not useful in practice
	 * @return
	 */
	
	public abstract String getConstraintType() throws Exception;
	
	public abstract String[] getConstraintVariableNames() throws Exception;
	
	public void executePlainSparqlQuery(String sparqlQuery, DispatcherSupportedQueryTypes supportedQueryType) throws Exception{
		TableResultSet retval = null;
		this.jobTracker.incrementPercentComplete(this.jobID, 1, 10);

		try{

			LocalLogger.logToStdErr("Job " + this.jobID + ": AsynchronousNodeGroupExecutor start");

			LocalLogger.logToStdErr("Sparql Query to execute: ");
			LocalLogger.logToStdErr(sparqlQuery);
			
			// run the actual query and get a result. 
			GeneralResultSet preRet = null;
			
			if(supportedQueryType == DispatcherSupportedQueryTypes.CONSTRUCT || supportedQueryType == DispatcherSupportedQueryTypes.CONSTRUCT_FOR_INSTANCE_DATA_MANIPULATION){
				// constructs require particular support for a different result set.
				preRet = this.querySei.executeQueryAndBuildResultSet(sparqlQuery, SparqlResultTypes.GRAPH_JSONLD);
				retval = new TableResultSet(true);
			}
			else if (supportedQueryType == DispatcherSupportedQueryTypes.DELETE || supportedQueryType == DispatcherSupportedQueryTypes.RAW_SPARQL_UPDATE) {
				SimpleResultSet simpleRes = (SimpleResultSet) this.querySei.executeQueryAndBuildResultSet(sparqlQuery, SparqlResultTypes.CONFIRM);
				retval = new TableResultSet(simpleRes);
				
			} else {
				// all other types
				preRet = this.querySei.executeQueryAndBuildResultSet(sparqlQuery, SparqlResultTypes.TABLE);
				retval = (TableResultSet) preRet;
			}

			
			if (retval.getSuccess()) {
				
				LocalLogger.logToStdErr("about to write results for " + this.jobID);
				if(supportedQueryType == DispatcherSupportedQueryTypes.CONSTRUCT || supportedQueryType == DispatcherSupportedQueryTypes.CONSTRUCT_FOR_INSTANCE_DATA_MANIPULATION){
					// constructs require particular support in the results client and the results service. this support would start here.
					this.sendResultsToService((NodeGroupResultSet) preRet);
				}
				else {
					// all other types
					LocalLogger.logToStdErr("Query returned " + retval.getTable().getNumRows() + " results.");
					this.sendResultsToService(retval);
				}
				this.updateStatus(100);		// work's done
			}
			else {
				this.updateStatusToFailed("Query client returned error to dispatch client: \n" + retval.getRationaleAsString("\n"));
			}
			
			LocalLogger.logToStdErr("Job " + this.jobID + ": AsynchronousNodeGroupExecutor end ");

		}
		catch(Exception e){
			// something went awry. set the job to failure. 
			this.updateStatusToFailed(e.getMessage());
			LocalLogger.printStackTrace(e);
			throw new Exception("Query failed: " + e.getMessage() );
		}
		
	}

	protected String getSparqlQuery(DispatcherSupportedQueryTypes qt, String targetSparqlID) throws Exception{
		String retval = null;
		
		// select 
		if(qt.equals(DispatcherSupportedQueryTypes.SELECT_DISTINCT)){
			retval = this.queryNodeGroup.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, null, null);
		}
		else if(qt.equals(DispatcherSupportedQueryTypes.COUNT)){
			retval = this.queryNodeGroup.generateSparql(AutoGeneratedQueryTypes.QUERY_COUNT, false, null, null);
		}
		else if(qt.equals(DispatcherSupportedQueryTypes.FILTERCONSTRAINT)){
			
			// find our returnable and pass it on.
			Returnable rt = null;
			rt = this.queryNodeGroup.getNodeBySparqlID(targetSparqlID);
			if(rt == null){
				rt = this.queryNodeGroup.getPropertyItemBySparqlID(targetSparqlID);
			}
			
			// use the rt
			retval = this.queryNodeGroup.generateSparql(AutoGeneratedQueryTypes.QUERY_CONSTRAINT, false, null, rt);
		}
		else if(qt.equals(DispatcherSupportedQueryTypes.CONSTRUCT)){
			retval = this.queryNodeGroup.generateSparqlConstruct(false);
		}
		else if(qt.equals(DispatcherSupportedQueryTypes.CONSTRUCT_FOR_INSTANCE_DATA_MANIPULATION)){
			retval = this.queryNodeGroup.generateSparqlConstruct(true);
		}
		else if(qt.equals(DispatcherSupportedQueryTypes.DELETE)) {
			retval = this.queryNodeGroup.generateSparqlDelete(null);
		}
		
		else{
			// never seen this one. panic.
			throw new Exception("Dispatcher passed and unrecognized query type. it does not know how to build a " + qt.name() +  " query");
		}
		
		return retval;
	}
	
	public JobTracker getJobTracker() {
		return this.jobTracker;
	}

}
