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


package com.ge.research.semtk.services.sparql;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.json.simple.JSONObject;

import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.parallel.SparqlParallelQueries;

/**
 * Service to execute SPARQL queries
 * 
 * 
 * Sample select query response:
 * {"message":"operations succeeded.","status":"success","table":{"@table":{"col_names":["number"],"rows":[["1272"],["1274"],["1276"],["1277"],["1278"],["1279"],["1280"],["1281"],["1283"],["1284"]],"col_type":["http://www.w3.org/2001/XMLSchema#integer"],"col_count":1,"row_count":10}}}
 *
 * Sample construct query response:
 * {"message":"operations succeeded.","status":"success","NodeGroup":{"@graph":[{"@type":[{"@id":"http:\/\/research.ge.com\/configuration#TestType"}],"@id":"http:\/\/research.ge.com\/configuration#Test"},{"@type":[{"@id":"http:\/\/research.ge.com\/configuration#Request"}]...
 * 
 * Sample auth query response:
 * {"message":"operations succeeded.","status":"success","simpleresults":{"@message":"Insert into <http://research.ge.com/sandbox>, 1 (or less) triples -- done"}}
 *
 * Sample error response:
 * {"message":"operations failed.","rationale":"This query must be run using the /queryAuth endpoint","status":"failed"}
 *
 * 
 * TODO add properties file with default serverType
 */
@RestController
@RequestMapping("/sparqlQueryService")
public class SparqlQueryServiceRestController {			
	
	/**
	 * Execute (non-auth) query 
	 */
	@CrossOrigin
	@RequestMapping(value="/query", method= RequestMethod.POST)
	public JSONObject query(@RequestBody SparqlQueryRequestBody requestBody){	
		
		GeneralResultSet resultSet = null;
		SparqlEndpointInterface sei = null;
		logToStdout("\n================== Sparql Query Service start query ========================");

		try{
			
			// disallow running drop graph query here (keep this check first, before the subsequent check)
			if(SparqlResultTypes.isDropGraphQuery(requestBody.query)){ 
				return (new SimpleResultSet(false, "This query must be run using the /dropGraph endpoint")).toJson();
			}
			// disallow running auth queries here (they'll get rejected later anyway)
			if(SparqlResultTypes.valueOf(requestBody.resultType) == SparqlResultTypes.CONFIRM){ 
				return (new SimpleResultSet(false, "A query expecting resultType " + requestBody.resultType + " must be run using the /queryAuth endpoint")).toJson();
			}
		
			requestBody.printInfo(); 	// print info to console			
			requestBody.validate(); 	// check inputs 			
			sei = SparqlEndpointInterface.getInstance(requestBody.serverType, requestBody.serverAndPort, requestBody.dataset);
		
		} catch (Exception e) {			
			e.printStackTrace();	
			return (new SimpleResultSet(false, e.getMessage())).toJson();
		}
			
		resultSet = sei.executeQueryAndBuildResultSet(requestBody.query, SparqlResultTypes.valueOf(requestBody.resultType));
		return resultSet.toJson();
		
	}		
	
	
	/**
	 * Execute auth query 
	 */
	@CrossOrigin
	@RequestMapping(value="/queryAuth", method= RequestMethod.POST)
	public JSONObject queryAuth(@RequestBody SparqlQueryAuthRequestBody requestBody){
		
		
		GeneralResultSet resultSet = null;
		SparqlEndpointInterface sei = null;
		logToStdout("Sparql Query Service start queryAuth");

		try{
			// disallow running drop graph query here - require client to explicitly use /dropGraph to avoid accidental drops
			if(SparqlResultTypes.isDropGraphQuery(requestBody.query)){ 
				return (new SimpleResultSet(false, "This query must be run using the /dropGraph endpoint")).toJson();
			}
			requestBody.printInfo(); 	// print info to console			
			requestBody.validate(); 	// check inputs 		
			sei = SparqlEndpointInterface.getInstance(requestBody.serverType, requestBody.serverAndPort, requestBody.dataset, requestBody.user, requestBody.password);	
		
		} catch (Exception e) {			
			e.printStackTrace();	
			return (new SimpleResultSet(false, e.getMessage())).toJson();
		}		
		resultSet = sei.executeQueryAndBuildResultSet(requestBody.query, SparqlResultTypes.valueOf(requestBody.resultType));
		System.out.println("Result code:" + resultSet.getResultCodeString());
		return resultSet.toJson();
	}	
	
	
	/**
	 * Execute drop graph query 
	 */
	@CrossOrigin
	@RequestMapping(value="/dropGraph", method= RequestMethod.POST)
	public JSONObject dropGraph(@RequestBody SparqlAuthRequestBody requestBody){
		GeneralResultSet resultSet = null;
		SparqlEndpointInterface sei = null;
		logToStdout("Sparql Query Service start dropGraph");

		try {			
			requestBody.printInfo(); 	// print info to console			
			requestBody.validate(); 	// check inputs 		
			sei = SparqlEndpointInterface.getInstance(requestBody.serverType, requestBody.serverAndPort, requestBody.dataset, requestBody.user, requestBody.password);	
		} catch (Exception e) {			
			e.printStackTrace();	
			return (new SimpleResultSet(false, e.getMessage())).toJson();
		}		
		String dropGraphQuery = "drop graph <" + requestBody.dataset + ">";  // drop query
		resultSet = sei.executeQueryAndBuildResultSet(dropGraphQuery, SparqlResultTypes.CONFIRM);
		return resultSet.toJson();
	}	
	
	
	/**
	 * Executes multiple subqueries in parallel and fuses the results.  
	 * Returns a table result set in JSON format.
	 * 
	 * subqueriesJson		JSON containing triple store connection and subqueries
	 * subqueryType			"select" supported for now, perhaps others in the future
	 * isSubqueryOptional	true to return records from one subquery result that do not match another subquery result
	 * columnsToFuseOn		columns to merge subquery results, comma-separated
	 * columnsToReturn		columns to return from the overall query, comma-separated
	 */
	@CrossOrigin
	@RequestMapping(value="/parallelQuery", method= RequestMethod.POST)
	public JSONObject parallelQuery(@RequestBody SparqlParallelQueryRequestBody requestBody) {	

		logToStdout("Sparql Query Service start parallelQuery");

		try{
			SparqlParallelQueries spq = new SparqlParallelQueries (requestBody.subqueriesJson, requestBody.subqueryType, requestBody.isSubqueryOptional, requestBody.columnsToFuseOn, requestBody.columnsToReturn);
			spq.runQueries ();
			JSONObject tableResultSetJSON = spq.returnFusedResults();
			return tableResultSetJSON;			
		} catch (Exception e) {			
			e.printStackTrace();	
			return (new SimpleResultSet(false, e.getMessage())).toJson();
		} catch (Throwable e) {
			e.printStackTrace();	
			return (new SimpleResultSet(false, e.getMessage())).toJson();
		}
	}	

/*
	@CrossOrigin
	@RequestMapping(value="/parallelQueryX", method= RequestMethod.POST, consumes= "application/x-www-form-urlencoded")
	public JSONObject parallelQueryX(String subqueriesJson, String subqueryType, Boolean isSubqueryOptional, String columnsToFuseOn, String columnsToReturn) {	
//System.out.println ("In Parallel Query X!");

//System.out.println ("Queries:\n" + subqueriesJson);
		try{
			SparqlParallelQueries spq = new SparqlParallelQueries (subqueriesJson, subqueryType, isSubqueryOptional, columnsToFuseOn, columnsToReturn);
			spq.runQueries ();
			JSONObject tableResultSetJSON = spq.returnFusedResults();
//return (new SimpleResultSet(false, "Returning here")).toJson();
			return tableResultSetJSON;			
		} catch (Exception e) {			
			e.printStackTrace();	
			return (new SimpleResultSet(false, e.getMessage())).toJson();
		} catch (Throwable e) {
			e.printStackTrace();	
			return (new SimpleResultSet(false, e.getMessage())).toJson();
		}
	}	
*/

	/**
	 * Execute auth query 
	 */
	@CrossOrigin
	@RequestMapping(value="/clearPrefix", method= RequestMethod.POST)
	public JSONObject clearPrefix(@RequestBody SparqlPrefixAuthRequestBody requestBody){
		
		
		GeneralResultSet resultSet = null;
		SparqlEndpointInterface sei = null;
		logToStdout("Sparql Query Service start clearPrefix");
		String query = "";
		try{
			requestBody.printInfo(); 	// print info to console			
			requestBody.validate(); 	// check inputs 	
			query = String.format(
					"delete {" +
					"?x ?y ?z." +
					"}" +
					"where {" +
					" ?x ?y ?z  FILTER ( strstarts(str(?x), \"%s\") || strstarts(str(?y), \"%s\") || strstarts(str(?z), \"%s\") )." +
					"}", 
					requestBody.prefix, requestBody.prefix, requestBody.prefix);
			sei = SparqlEndpointInterface.getInstance(requestBody.serverType, requestBody.serverAndPort, requestBody.dataset, requestBody.user, requestBody.password);	
		} catch (Exception e) {			
			e.printStackTrace();	
			return (new SimpleResultSet(false, e.getMessage())).toJson();
		}	
		
		resultSet = sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.CONFIRM);
		return resultSet.toJson();
	}	
	/**
	 * Execute clear all query 
	 */
	@CrossOrigin
	@RequestMapping(value="/clearAll", method= RequestMethod.POST)
	public JSONObject clearAll(@RequestBody SparqlAuthRequestBody requestBody){
		GeneralResultSet resultSet = null;
		SparqlEndpointInterface sei = null;
    	logToStdout("Sparql Query Service start clearAll");

		try {			
			requestBody.printInfo(); 	// print info to console			
			requestBody.validate(); 	// check inputs 		
			sei = SparqlEndpointInterface.getInstance(requestBody.serverType, requestBody.serverAndPort, requestBody.dataset, requestBody.user, requestBody.password);	
		} catch (Exception e) {			
			e.printStackTrace();	
			return (new SimpleResultSet(false, e.getMessage())).toJson();
		}		
		String query = "clear all";  // drop query
		resultSet = sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.CONFIRM);
		return resultSet.toJson();
	}	
	
	//public JSONObject uploadOwl(@RequestBody SparqlAuthRequestBody requestBody, @RequestParam("owlFile") MultipartFile owlFile){
    // We can't use a @RequestBody with a @RequestParam,
	// So the SparqlAuthRequestBody is broken into individual string @RequestParams
	
	/**
	 * Execute clear all query 
	 */
	@CrossOrigin
	@RequestMapping(value="/uploadOwl", method= RequestMethod.POST)
	public JSONObject uploadOwl(@RequestParam("serverAndPort") String serverAndPort, 
								@RequestParam("serverType") String serverType, 
								@RequestParam("dataset") String dataset, 
								@RequestParam("user") String user, 
								@RequestParam("password") String password, 
								@RequestParam("owlFile") MultipartFile owlFile){
		SimpleResultSet resultSet = null;
		JSONObject simpleResultSetJson = null;
		SparqlEndpointInterface sei = null;
    	logToStdout("Sparql Query Service start uploadOwl");

		try {	
			if (serverAndPort == null || serverAndPort.trim().isEmpty() ) throw new Exception("serverAndPort is empty.");
			if (serverType == null || serverType.trim().isEmpty() ) throw new Exception("serverType is empty.");
			if (dataset == null || dataset.trim().isEmpty() ) throw new Exception("dataset is empty.");
			if (user == null || user.trim().isEmpty() ) throw new Exception("user is empty.");
			if (password == null || password.trim().isEmpty() ) throw new Exception("password is empty.");

			sei = SparqlEndpointInterface.getInstance(serverType, serverAndPort, dataset, user, password);
			simpleResultSetJson = sei.executeAuthUploadOwl(owlFile.getBytes());
			 
		} catch (Exception e) {			
			e.printStackTrace();	
			return (new SimpleResultSet(false, e.getMessage())).toJson();
		}		
		
		return simpleResultSetJson;
		
		
		
		
		
	}	
	
	private void logToStdout (String message) {
		System.out.println(message);
	}
}
