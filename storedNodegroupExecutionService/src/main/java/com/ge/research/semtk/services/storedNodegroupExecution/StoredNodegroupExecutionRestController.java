package com.ge.research.semtk.services.storedNodegroupExecution;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.research.semtk.api.storedQueryExecution.StoredQueryExecutor;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.edc.client.StatusClientConfig;
import com.ge.research.semtk.load.client.IngestorClientConfig;
import com.ge.research.semtk.load.client.IngestorRestClient;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreConfig;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.resultSet.RecordProcessResults;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.dispatch.client.DispatchClientConfig;
import com.ge.research.semtk.sparqlX.dispatch.client.DispatchRestClient;

/**
 * service to run stored nodegroups. 
 * @author 200018594
 *
 */
@RestController
@RequestMapping("/nodegroupExecution")
public class StoredNodegroupExecutionRestController {

	@Autowired
	NodegroupExecutionProperties prop;
	
	@CrossOrigin
	@RequestMapping(value="/jobStatus", method=RequestMethod.POST)
	public JSONObject getJobStatus(@RequestBody StatusRequestBody requestBody){
		SimpleResultSet retval = new SimpleResultSet();
		
		try{
			// create a new StoredQueryExecutor
			StoredQueryExecutor sqe = this.getExecutor(prop, requestBody.getJobID() );
			// try to get a job status
			String results = sqe.getJobStatus();
			retval.setSuccess(true);
			retval.addResult("status", results);
		}
		catch(Exception e){
			e.printStackTrace();
			retval = new SimpleResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(e.getMessage());
		}
	
		return retval.toJson();
	} 
	
	@CrossOrigin
	@RequestMapping(value="/jobStatusMessage", method=RequestMethod.POST)
	public JSONObject getJobStatusMessage(@RequestBody StatusRequestBody requestBody){
		SimpleResultSet retval = new SimpleResultSet();
		
		try{
			// create a new StoredQueryExecutor
			StoredQueryExecutor sqe = this.getExecutor(prop, requestBody.getJobID() );
			// try to get a job status
			String results = sqe.getJobStatusMessage();
			retval.setSuccess(true);
			retval.addResult("message", results);
		}
		catch(Exception e){
			e.printStackTrace();
			retval = new SimpleResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(e.getMessage());
		}
	
		return retval.toJson();
	} 
	
	@CrossOrigin
	@RequestMapping(value="/getJobCompletionCheck", method=RequestMethod.POST)
	public JSONObject getJobCompletion(@RequestBody StatusRequestBody requestBody ){
		SimpleResultSet retval = new SimpleResultSet();
		
		try{
			// create a new StoredQueryExecutor
			StoredQueryExecutor sqe = this.getExecutor(prop, requestBody.getJobID() );
			// try to get a job status
			Boolean results = sqe.getJobCompletion();
			retval.setSuccess(true);
			if(results){
				retval.addResult("completed", "true");
			}
			else{
				retval.addResult("completed", "false");
			}
		}
		catch(Exception e){
			e.printStackTrace();
			retval = new SimpleResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(e.getMessage());
		}
	
		return retval.toJson();
	}
	
	@CrossOrigin
	@RequestMapping(value="/getJobCompletionPercentage", method=RequestMethod.POST)
	public JSONObject getJobCompletionPercent(@RequestBody StatusRequestBody requestBody ){
		SimpleResultSet retval = new SimpleResultSet();
		
		try{
			// create a new StoredQueryExecutor
			StoredQueryExecutor sqe = this.getExecutor(prop, requestBody.getJobID() );
			// try to get a job status
			int results = sqe.getJobPercentCompletion();
			retval.setSuccess(true);
			retval.addResult("percent", results);

		}
		catch(Exception e){
			e.printStackTrace();
			retval = new SimpleResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(e.getMessage());
		}
	
		return retval.toJson();
	}
	
	@CrossOrigin
	@RequestMapping(value="/getResultsLocation", method=RequestMethod.POST)
	public JSONObject getResultsLocation(@RequestBody StatusRequestBody requestBody ){
		TableResultSet retval = new TableResultSet();
		
		// note: make sure the response is sane when results do not yet exist. the failure should be as graceful as we can make them.
		
		try{
			// create a new StoredQueryExecutor
			StoredQueryExecutor sqe = this.getExecutor(prop, requestBody.getJobID() );
			// try to get a job status
			URL[] results = sqe.getResultsLocation();
			retval.setSuccess(true);
	
			// a little diagnostic print:
			System.err.println("results info for job (" + requestBody.getJobID() + ") : " + results.length + " records.");
			for(URL i : results){
				System.err.println("        record: " + i.toString());
			}
			
			
			// turn this into a table result.
			String[] cols = {"URL_Location", "Result_Type"};
			String[] colTypes = {"http://www.w3.org/2001/XMLSchema#string", "http://www.w3.org/2001/XMLSchema#string"};
			
			// the first is the sample. the second is the complete result.
			ArrayList<String> row0 = new ArrayList<String>();
			row0.add(results[0].toString());
			row0.add("sample");
			ArrayList<String> row1 = new ArrayList<String>();
			row1.add(results[1].toString());
			row1.add("full");
			
			ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
			
			rows.add(row0);
			rows.add(row1);
			
			Table retTable = new Table(cols, colTypes, rows);
			retval.addResults(retTable);

		}
		catch(Exception e){
			e.printStackTrace();
			retval = new TableResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(e.getMessage());
		}
	
		return retval.toJson();
	}
	
	@CrossOrigin
	@RequestMapping(value="/dispatchById", method=RequestMethod.POST)
	public JSONObject dispatchJobById(@RequestBody DispatchByIdRequestBody requestBody){
		SimpleResultSet retval = new SimpleResultSet();
		
		try{
			// create a new StoredQueryExecutor
			StoredQueryExecutor sqe = this.getExecutor(prop, null );
			// try to create a sparql connection
			SparqlConnection connection = new SparqlConnection(requestBody.getSparqlConnection());			
			// create a json object from the external data constraints. 
			
			JSONObject edcConstraints = null;
			if(requestBody.getExternalDataConnectionConstraints() != null && !requestBody.getExternalDataConnectionConstraints().equals("")){
				// attempt to process the constraints to json.
				JSONParser jParse = new JSONParser();
				edcConstraints = (JSONObject)jParse.parse(requestBody.getExternalDataConnectionConstraints());
			}

			// try to get the runtime constraints
			JSONArray runtimeConstraints = this.getRuntimeConstraintsAsJsonArray(requestBody.getRuntimeConstraints());
			
			// dispatch the job. 
			sqe.dispatchJob(connection, requestBody.getNodeGroupId(), edcConstraints, runtimeConstraints );
			String id = sqe.getJobID();
			
			retval.setSuccess(true);
			retval.addResult("JobId", id); 

		}
		catch(Exception e){
			e.printStackTrace();
			retval = new SimpleResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(e.getMessage());
		}
	
		return retval.toJson();

	}
	
	@CrossOrigin
	@RequestMapping(value="/dispatchFromNodegroup", method=RequestMethod.POST)
	public JSONObject dispatchJobFromNodegroup(@RequestBody DispatchFromNodegroupRequestBody requestBody ){

		SimpleResultSet retval = new SimpleResultSet();
		
		try{
			// create a new StoredQueryExecutor
			StoredQueryExecutor sqe = this.getExecutor(prop, null );
			// try to create a sparql connection
			SparqlConnection connection = new SparqlConnection(requestBody.getSparqlConnection());			
			// create a json object from the external data constraints. 
			
			JSONObject edcConstraints = null;
			if(requestBody.getExternalDataConnectionConstraints() != null && !requestBody.getExternalDataConnectionConstraints().equals("")){
				// attempt to process the constraints to json.
				JSONParser jParse = new JSONParser();
				edcConstraints = (JSONObject)jParse.parse(requestBody.getExternalDataConnectionConstraints());
			}
			
			// get the nodegroup. we are assuming that the user should send a node group complete with original connection info, since we 
			// store them that way. we'll perform a quick check to find out though
			JSONObject encodedNodeGroup = requestBody.getJsonNodeGroup();
			NodeGroup ng = new NodeGroup();
			
			// check that sNodeGroup is a key in the json. if so, this has a connection and the rest.
			if(encodedNodeGroup.containsKey("sNodeGroup")){
				System.err.println("located key: sNodeGroup");
				ng.addJsonEncodedNodeGroup((JSONObject) encodedNodeGroup.get("sNodeGroup"));
			}
			
			// otherwise, check for a truncated one that is only the nodegroup proper.
			else if(encodedNodeGroup.containsKey("sNodeList")){
				ng.addJsonEncodedNodeGroup(encodedNodeGroup);
			}
			else{
				// no idea what this is...
				throw new Exception("Value given for encoded node group does not seem to be a node group as it has neither sNodeGroup or sNodeList keys");
			}
		
			// try to get the runtime constraints
			JSONArray runtimeConstraints = this.getRuntimeConstraintsAsJsonArray(requestBody.getRuntimeConstraints());
			
			// dispatch the job. 
			sqe.dispatchJob(connection, ng, edcConstraints, runtimeConstraints);
			String id = sqe.getJobID();
			
			retval.setSuccess(true);
			retval.addResult("JobId", id);

		}
		catch(Exception e){
			e.printStackTrace();
			retval = new SimpleResultSet();
			retval.setSuccess(false);
			retval.addRationaleMessage(e.getMessage());
		}
	
		return retval.toJson();

	}
	
	@CrossOrigin
	@RequestMapping(value="/ingestFromCsvStringsNewConnection", method=RequestMethod.POST)
	public JSONObject ingestFromTemplateIdAndCsvString(@RequestBody IngestByIdCsvStrRequestBody requestBody) throws Exception{
		RecordProcessResults retval = null;
		
		StoredQueryExecutor sqe = this.getExecutor(prop, null );
		// try to create a sparql connection
		SparqlConnection connection = new SparqlConnection(requestBody.getSparqlConnection());			
	
		// call for the ingestion.
		retval = sqe.ingestFromTemplateIdAndCsvString(connection, requestBody.getTemplateId(), requestBody.getCsvContent());
		
		// set the rest of the results.
		return retval.toJson();
	}
	
	// get the runtime constraints, if any.
	private JSONArray getRuntimeConstraintsAsJsonArray(String potentialConstraints) throws Exception{
		JSONArray retval = null;
		
		try{
			if(potentialConstraints != null && potentialConstraints.length() > 0 && !potentialConstraints.isEmpty()){
				// we have something of meaning in the constraints. 
				JSONParser jParse = new JSONParser();
				retval = (JSONArray) jParse.parse(potentialConstraints);
			}
		}
		catch(Exception ez){
			throw new Exception("getRuntimeConstraintsAsJsonArray :: Unable to deserialize runtime constraints. error recieved was: " + ez.getMessage());
		}
		// TODO: add a method for consistency checking the JSON once some constraints are made from the string.
		
		return retval;
	}
	
	// create the required StoredQueryExecutor
	private StoredQueryExecutor getExecutor(NodegroupExecutionProperties prop, String jobID) throws Exception{
		NodeGroupStoreConfig ngcConf = new NodeGroupStoreConfig(prop.getNgStoreProtocol(), prop.getNgStoreServer(), prop.getNgStorePort());
		DispatchClientConfig dsConf  = new DispatchClientConfig(prop.getDispatchProtocol(), prop.getDispatchServer(), prop.getDispatchPort());
		ResultsClientConfig  rConf   = new ResultsClientConfig(prop.getResultsProtocol(), prop.getResultsServer(), prop.getResultsPort());
		StatusClientConfig   sConf   = new StatusClientConfig(prop.getStatusProtocol(), prop.getStatusServer(), prop.getStatusPort(), jobID);
		IngestorClientConfig iConf   = new IngestorClientConfig(prop.getIngestProtocol(), prop.getIngestServer(), prop.getIngestPort());
		
		// create the other components we need. 
		NodeGroupStoreRestClient nodegroupstoreclient = new NodeGroupStoreRestClient(ngcConf);
		DispatchRestClient dispatchclient = new DispatchRestClient(dsConf);
		ResultsClient resultsclient = new ResultsClient(rConf);
		StatusClient statusclient = new StatusClient(sConf);
		IngestorRestClient ingestClient = new IngestorRestClient(iConf);
		
		// create the actual executor
		StoredQueryExecutor retval = new StoredQueryExecutor(nodegroupstoreclient, dispatchclient, resultsclient, statusclient, ingestClient);
		if(jobID != null){ retval.setJobID(jobID); }
		return retval;
	}
}


