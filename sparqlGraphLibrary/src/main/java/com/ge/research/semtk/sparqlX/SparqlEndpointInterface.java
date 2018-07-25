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



package com.ge.research.semtk.sparqlX;

import java.io.IOException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.net.URLCodec;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.resultSet.NodeGroupResultSet;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.FusekiSparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.VirtuosoSparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * Interface to SPARQL endpoint.
 * This is an abstract class - create a subclass per implementation (Virtuoso, etc)
 */
public abstract class SparqlEndpointInterface {

	// NOTE: more than one thread cannot safely share a SparqlEndpointInterface.
	// this is because of the state maintained for the results vars and connection 
	// details. doing so may lead to unexpected results

	private final static String QUERY_SERVER = "kdl";
	private final static String FUSEKI_SERVER = "fuseki";
	private final static String VIRTUOSO_SERVER = "virtuoso";
	private final static String NEPTUNE_SERVER = "neptune";
	
	// results types to request
	private static final String CONTENTTYPE_SPARQL_QUERY_RESULT_JSON = "application/sparql-results+json"; 
	private static final String CONTENTTYPE_JSON_LD = "application/x-json+ld";
	private static final String CONTENTTYPE_HTML = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
	
	private static final int MAX_QUERY_TRIES = 4;

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private static URLStreamHandler handler = null; // set only by unit tests

	private JSONObject response = null;
	private JSONArray resVars = null;
	private JSONArray resBindings = null;
	protected String userName = null;
	protected String password = null;
	protected String server = null;
	protected String port = null;
	protected String graph = "";


	/**
	 * Constructor
	 * @param serverAndPort e.g. "http://localhost:2420"
	 * @param dataset e.g. "http://research.ge.com/energy/dataset"
	 * @throws Exception
	 */
	public SparqlEndpointInterface(String serverAndPort, String graph) throws Exception {
		this(serverAndPort, graph, null, null);
	}


	/**
	 * Constructor used for authenticated connections... like insert/update/delete/clear
	 */
	public SparqlEndpointInterface(String serverAndPort, String graph, String user, String pass) throws Exception {
		this.graph = graph;		
		this.userName = user;
		this.password = pass;
		
		this.setServerAndPort(serverAndPort);
		
	}

	public String getServerAndPort() {		
		return this.server + ":" + this.port;
	}
	
	public void setServerAndPort(String serverAndPort) throws Exception {
		String[] serverAndPortSplit = serverAndPort.split(":");  // protocol:server:port
		if(serverAndPortSplit.length < 2){
			throw new Exception("Error: must provide connection in format protocol:server:port (e.g. http://localhost:2420)");
		}
		this.server = serverAndPortSplit[0] + ":" + serverAndPortSplit[1]; // e.g. http://localhost
		
		if(serverAndPortSplit.length < 3){
			throw new Exception("Error: no port provided for " + this.server);
		}
		
		String[] portandendpoint = serverAndPortSplit[2].split("/");
		this.port = portandendpoint[0];
	}
	
	public String getServer() {
		return server;
	}

	public int getPort() {
		return (int) Integer.parseInt(port);
	}

	public String getGraph() {
		return this.graph;
	}
	
	// deprecated
	public String getDataset() {
		return this.graph;
	}
	
	public String getUserName() {
		return userName;
	}


	public String getPassword() {
		return password;
	}

	public void setGraph(String graph) {
		this.graph = graph;
	}
	
	// deprecated
	public void setDataset(String graph) {
		this.graph = graph;
	}
	
	public abstract String getServerType();
	
	/**
	 * Build a GET URL (implementation-specific)
	 */
	public abstract String getPostURL();
	
	/**
	 * Build an upload URL (implementation-specific)
	 */
	public abstract String getUploadURL() throws Exception;


	/**
	 * Build a POST URL  (implementation-specific)
	 */
	public abstract String getGetURL();


	/**
	 * Handle an empty response (implementation-specific)
	 */
	public abstract void handleEmptyResponse() throws Exception;
	

	/**
	 * Get a default result type
	 */
	private static SparqlResultTypes getDefaultResultType() {
		return SparqlResultTypes.TABLE;  // if no result type, assume it's a SELECT
	}

	
	public static void setUrlStreamHandler(URLStreamHandler handler) {
		SparqlEndpointInterface.handler = handler;
	}

	/**
	 * Set the username and password
	 */
	public void setUserAndPassword(String user, String pass){
		this.userName = user;
		this.password = pass;
	}	
	
	
	
	/**
	 * Static method to get an instance of this abstract class
	 */
	public static SparqlEndpointInterface getInstance(String serverTypeString, String server, String graph) throws Exception{
		return getInstance(serverTypeString, server, graph, null, null);
	}	
	
	/**
	 * Static method to get an instance of this abstract class
	 */
	public static SparqlEndpointInterface getInstance(String serverTypeString, String server, String graph, String user, String password) throws Exception{
		if(serverTypeString.equalsIgnoreCase(VIRTUOSO_SERVER)){
			if(user != null && ! user.isEmpty() && password != null && ! password.isEmpty()){
				return new VirtuosoSparqlEndpointInterface(server, graph, user, password);				
			}else{
				return new VirtuosoSparqlEndpointInterface(server, graph);
			}
		}else if(serverTypeString.equalsIgnoreCase(FUSEKI_SERVER)){
			if(user != null && ! user.isEmpty() && password != null && ! password.isEmpty()){
				return new FusekiSparqlEndpointInterface(server, graph, user, password);				
			}else{			
				return new FusekiSparqlEndpointInterface(server, graph);
			}
		}else if(serverTypeString.equalsIgnoreCase(NEPTUNE_SERVER)){
			if(user != null && ! user.isEmpty() && password != null && ! password.isEmpty()){
				return new NeptuneSparqlEndpointInterface(server, graph, user, password);				
			}else{			
				return new NeptuneSparqlEndpointInterface(server, graph);
			}
		}else{
			throw new Exception("Invalid SPARQL server type : " + serverTypeString);
		}	
	}
	
	
	// I renamed this function because it was not clear which Endpoint it was getting
	// And labeled it deprecated because it is only two lines of code and the first one is usually shared with calls to retrieve other things from the json.
	// And re-implemented it with the preferred code.
	// - Paul  5/26/2016
	public static SparqlEndpointInterface getDataInterfaceFromJsonDEPRECATE(JSONObject json) throws Exception{
		SparqlGraphJson sgJson = new SparqlGraphJson(json);
		SparqlEndpointInterface sei = sgJson.getSparqlConn().getDataInterface(0);
		return sei;
	}
	
	
	/**
	 * Static method to create an endpoint and execute a query
	 */
	public static SparqlEndpointInterface executeQuery(String server, String graph, String serverTypeString, String query, SparqlResultTypes resultType) throws Exception {
		return executeQuery(server, graph, serverTypeString, query, null, null, resultType);
	}
  
	/**
	 * Static method to create an endpoint and execute a query
	 */
	public static SparqlEndpointInterface executeQuery(String server, String graph, String serverTypeString, String query, String user, String pass, SparqlResultTypes resultType) throws Exception {
		try {
			SparqlEndpointInterface endpoint;
			if(user != null && pass != null){
				endpoint = SparqlEndpointInterface.getInstance(serverTypeString, server, graph, user, pass);
			}else{
				endpoint = SparqlEndpointInterface.getInstance(serverTypeString, server, graph);
			}
			endpoint.executeQuery(query, resultType);
			return endpoint;
		} catch (Exception e) {
			LocalLogger.printStackTrace(e);
			throw new Exception("Error executing semantic query:\n\t" + e.getMessage());
		}
	}		
	
	
	/**
	 * Execute query and assemble a GeneralResultSet object.
	 * 
	 * Returns:
	 * 	  TableResultSet for a select query (success or failure)
	 * 	  NodeGroupResultSet for a construct query (success or failure)
	 *    SimpleResultSet for an auth query (success or failure)
	 * 
	 * @throws - exception if not successful
	 */
	public GeneralResultSet executeQueryAndBuildResultSet(String query, SparqlResultTypes resultType) throws Exception {
		
		// construct an empty result object to store the results
		GeneralResultSet resultSet = null;
				
		if(resultType == SparqlResultTypes.GRAPH_JSONLD){ 
			resultSet = new NodeGroupResultSet();  // construct queries produce graphs
		}else if(resultType == SparqlResultTypes.CONFIRM){
			resultSet = new SimpleResultSet();		// auth queries produce messages
		}else if(resultType == SparqlResultTypes.TABLE){	
			resultSet = new TableResultSet(); // non-construct queries produce tables
		}else if (resultType == SparqlResultTypes.HTML) {
			resultSet = new SimpleResultSet();
		}
		
		// execute the query
		JSONObject result = executeQuery(query, resultType); 			
		resultSet.setSuccess(true);
		resultSet.addResultsJSON(result);
		
		return resultSet;
	}	
	
	public Table executeQueryToTable(String query) throws Exception {
		TableResultSet res = (TableResultSet) this.executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
		res.throwExceptionIfUnsuccessful();
		return res.getTable();
	}
	
	public void executeQueryAndConfirm(String query) throws Exception {
		GeneralResultSet res = this.executeQueryAndBuildResultSet(query, SparqlResultTypes.CONFIRM);
		res.throwExceptionIfUnsuccessful();
	}
	
	/**
	 * Execute a query. Uses http POST.  
	 * Depending on the type of query (select, insert, construct, etc), decides whether to use auth or non-auth.
	 * 
	 * Sample result from a SELECT query:
	 *  
	 * Sample result from a CONSTRUCT query:
	 * 
	 * Sample results from an AUTH query:
	 * {"@message":"Insert into <http://research.ge.com/graph>, 1 (or less) triples -- done"}
	 * 
	 * @param query
	 * @return a JSONObject wrapping the results
	 */
	public JSONObject executeQuery(String query, SparqlResultTypes resultType) throws Exception {

		int tryCount = 0;
		// Keep trying the query until it succeeds or reaches a 
		// maximum number of tries, which is checked for in the 
		// exception catch
		while (true) {
			tryCount++;
			try {
				if(this.userName !=null && this.password != null){
					return executeQueryPost(query, resultType);
				}else{
//					if(resultType == SparqlResultTypes.CONFIRM){ 
//						throw new Exception("Username and password are required to execute a query with resultType " + resultType.toString());
//					}
					return executeQueryPost(query, resultType);
				}
			} catch (Exception e) {
				if (tryCount >= MAX_QUERY_TRIES) {
					LocalLogger.logToStdOut (String.format("SPARQL query failed after %d tries.  Giving up.", tryCount));
					LocalLogger.logToStdErr(e.getMessage());
					throw e;
				} else {	// else unnecessary, but makes code easier to read
					int sleepSec = 2 * tryCount;
					// if we're overwhelming a server, really throttle
					if (e.getMessage().contains("Address already in use: connect")) {
						sleepSec = 10 * tryCount;
					}
					LocalLogger.logToStdOut (String.format("SPARQL query failed.  Sleeping %d seconds and trying again...", sleepSec));
					LocalLogger.logToStdErr(e.getMessage());
					TimeUnit.SECONDS.sleep(sleepSec); // sleep and try again
				}
			}
		}
	}

	public JSONObject executeTestQuery() throws Exception {
		final String sparql = "select ?Concept where {[] a ?Concept} LIMIT 1";
		try {
			return executeQuery(sparql, SparqlResultTypes.TABLE);
		} catch (Exception e) {
			throw new Exception("Failure executing test query.", e);
		}
	}

	/**
	 * Deprecated in favor of executeQueryPost
	 * Uses Authentication iff this.userName is not null.
	 * @param query
	 * @param resultType
	 * @return
	 * @throws Exception
	 */
	public JSONObject executeQueryAuthPost(String query, SparqlResultTypes resultType) throws Exception {
		// deprecated in favor of:
		return this.executeQueryPost(query, resultType);
	}	
	

	/**
	 * Execute a query using POST
	 * Adds Auth elements ONLY IF this.userName != null
	 * @return a JSONObject wrapping the results. in the event the results were tabular, they can be obtained in the JsonArray "@Table". if the results were a graph, use "@Graph" for json-ld
	 * @throws Exception
	 */
	public JSONObject executeQueryPost(String query, SparqlResultTypes resultType) throws Exception{
		
        // get client, adding userName/password credentials if any exist
        CloseableHttpClient httpclient = this.buildHttpClient();
		HttpHost targetHost = this.buildHttpHost();
		// get context with digest auth if there is a userName, else null context
		BasicHttpContext localcontext = this.buildHttpContext(targetHost);

		// create the HttpPost
		HttpPost httppost = new HttpPost(this.getPostURL());
		
		this.addHeaders(httppost, resultType);
		this.addParams(httppost, query, resultType);
		
		// parse the response
		HttpEntity entity = null;
		try {
			// execute
			HttpResponse response_http = httpclient.execute(targetHost, httppost, localcontext);
			entity = response_http.getEntity();
			String responseTxt = EntityUtils.toString(entity, "UTF-8");
		
			// parse response
			return this.parseResponse(resultType, responseTxt);
		} finally {
			httpclient.close();
			if (entity != null) {
				entity.getContent().close();
			}
		}
	}
	
	public void clearGraph() throws Exception {
		this.executeQueryAndConfirm("clear graph <" + this.getGraph() + ">");
	}
	
	/**
	 * Get an CloseableHttpClient, adding credentials if userName exists
	 * @return
	 */
	protected CloseableHttpClient buildHttpClient() {
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		
		// add userName and password, if any
		if (this.userName != null) {
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(this.userName, this.password));
	        clientBuilder.setDefaultCredentialsProvider(credsProvider);
	        return clientBuilder.build();
	        
		} else {
			return clientBuilder.build();
		}
	}
	
	/**
	 * build the host from this.server and this.port
	 * @return
	 */
	protected HttpHost buildHttpHost() {
		String[] serverNoProtocol = this.server.split("://");
		return new HttpHost(serverNoProtocol[1], Integer.valueOf(this.port), serverNoProtocol[0]);

	}
	
	/**
	 * build the HttpPost with headers
	 * @param resultsFormat
	 * @return
	 */
	protected void addHeaders(HttpPost httppost, SparqlResultTypes resultType) throws Exception {
		
		httppost.addHeader("Accept", this.getContentType(resultType));
		httppost.addHeader("X-Sparql-default-graph", this.graph);
	}
	
	protected void addParams(HttpPost httppost, String query, SparqlResultTypes resultType) throws Exception {
		// add params
		List<NameValuePair> params = new ArrayList<NameValuePair>(3);
		params.add(new BasicNameValuePair("query", query));
		params.add(new BasicNameValuePair("format", this.getContentType(resultType)));
		params.add(new BasicNameValuePair("default-graph-uri", this.graph));

		// set entity
		httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
	}
	
	/**
	 * Return a context with digest Auth if there is a userName, otherwise null context
	 * @param targetHost
	 * @return
	 */
	protected BasicHttpContext buildHttpContext(HttpHost targetHost) {
		if (this.userName != null) {
			DigestScheme digestAuth = new DigestScheme();
			AuthCache authCache = new BasicAuthCache();
			digestAuth.overrideParamter("realm", "SPARQL");
			// Suppose we already know the expected nonce value
			digestAuth.overrideParamter("nonce", "whatever");
			authCache.put(targetHost, digestAuth);
			BasicHttpContext ret = new BasicHttpContext();
			ret.setAttribute(HttpClientContext.AUTH_CACHE, authCache);
			return ret;
		} else {
			return null;
		}
	}
	
	/**
	 * Get explanations (header \n) of known triple store errors
	 * @param responseTxt
	 * @return
	 */
	protected String getResposeTextExplanation(String responseTxt) {
		return "Non-JSON error was returned from SPARQL endpoint.\n";
	}
	
	/**
	 * Parse the response to JSON
	 * @param resultType
	 * @param responseTxt
	 * @return
	 * @throws Exception
	 */
	protected JSONObject parseResponse(SparqlResultTypes resultType, String responseTxt) throws Exception {
		// parse json response
		JSONObject resp;
		
		if(responseTxt == null || responseTxt.trim().isEmpty()) {
			this.handleEmptyResponse(); 
		}
				
		try {
			resp = (JSONObject) JSONValue.parse(responseTxt);
		} catch(Exception e){
			throw new Exception("Cannot parse query result into JSON: " + responseTxt);
		}

		if (resp == null) {
			// null response might be ok?
			LocalLogger.logToStdErr("the response could not be transformed into json: " + responseTxt);

			if(responseTxt.contains("Error")) {
				throw new Exception(this.getResposeTextExplanation(responseTxt) + responseTxt); 
			}
			return null;
			
		} else {
			// extract results object from non-null response
			JSONObject procResp = this.getResultsFromResponse(resp, resultType);
			return procResp;
		}
	}

	/**
	 * Deprecated in favor of executeAuthUpload
	 * @return a JSONObject wrapping the results. in the event the results were tabular, they can be obtained in the JsonArray "@Table". if the results were a graph, use "@Graph" for json-ld
	 * @throws Exception
	 */

	public JSONObject executeAuthUploadOwl(byte[] owl) throws Exception{
		return this.executeAuthUpload(owl);
	}

	/**
	 * Upload an owl file, using Auth if this.userName != null
	 * @param owl
	 * @return
	 * @throws Exception
	 */
	public abstract JSONObject executeUpload(byte[] owl) throws Exception;

	/**
	 * Execute an auth query using POST, and using AUTH if this.userName != null
	 * @return a JSONObject wrapping the results. in the event the results were tabular, they can be obtained in the JsonArray "@Table". if the results were a graph, use "@Graph" for json-ld
	 * @throws Exception
	 */
	public JSONObject executeAuthUpload(byte[] owl) throws Exception{
        return this.executeUpload(owl);
	}
	

	/**
	 * Execute query using GET (use should be rare - in cases where POST is not supported) 
	 * @return a JSONObject wrapping the results. in the event the results were tabular, they can be obtained in the JsonArray "@Table". if the results were a graph, use "@Graph" for json-ld
	 * @throws Exception
	 */
	public JSONObject executeQueryGet(String query, SparqlResultTypes resultType) throws Exception {
		// encode the query and build a real URL
		// left over from ancient times like 2016
		URLCodec encoder = new URLCodec();
		String cleanURL = getGetURL() + encoder.encode(query);
		URL url = new URL(null, cleanURL, handler);
		String queryAndUrl = url.toString();
		
		return this.executeQueryAuthGet(queryAndUrl, resultType);
	}	
	
	/**
	 * Execute an auth query using GET (use should be rare - in cases where POST is not supported)
	 * @return a JSONObject wrapping the results. in the event the results were tabular, they can be obtained in the JsonArray "@Table". if the results were a graph, use "@Graph" for json-ld
	 * @throws Exception
	 */
	private JSONObject executeQueryAuthGet(String queryAndUrl, SparqlResultTypes resultType) throws Exception{
		
		// buid resultsFormat
		String resultsFormat = null;
		if(resultType == null){
			resultsFormat = this.getContentType(getDefaultResultType());
		} else {
			resultsFormat = this.getContentType(resultType);
		}
		
		CloseableHttpClient httpclient = this.buildHttpClient();
		HttpHost targetHost = this.buildHttpHost();
		BasicHttpContext localcontext = this.buildHttpContext(targetHost);

		LocalLogger.logToStdErr(queryAndUrl);
		
		HttpGet httpget = new HttpGet(queryAndUrl);
		httpget.addHeader("Accept", resultsFormat);
		
		LocalLogger.logToStdOut("executing request" + httpget.getRequestLine());

		//        String responseTxt = httpclient.execute(httpget, responseHandler);
		HttpResponse response_http = httpclient.execute(targetHost, httpget, localcontext);
		httpclient.close();
		HttpEntity entity = response_http.getEntity();
		String responseTxt = EntityUtils.toString(entity, "UTF-8");

		// parse the response
		try {
			return this.parseResponse(resultType, responseTxt);
		} finally {
			entity.getContent().close();
		}
	}
	
	/**
	 * Execute a query, retrieving a HashMap of data for a given set of headers.
	 */
	public HashMap<String,String[]> executeQuery(String query, String[] resultHeaders) throws Exception {
		return executeQuery(query, resultHeaders, false);
	}
	

	/**
	 * Execute a query, retrieving a HashMap of data for a given set of headers.
	 *
	 * @param query	the query
	 * @param resultHeaders	the headers of the result columns desired.  If null, use all column headers.
	 * @param expectOneResult if true, expect a single row result per column - if zero or multiple results are returned, then error
	 * @return a hashmap: keys are resultHeaders, values are String arrays with contents
	 *
	 */
	public HashMap<String,String[]> executeQuery(String query, String[] resultHeaders, boolean expectOneResult) throws Exception {

		executeQuery(query, SparqlResultTypes.TABLE);

		HashMap<String,String[]> results = new HashMap<String,String[]>();
		
		String colArr[] = null;
		if (resultHeaders != null) {
			colArr = resultHeaders;
		} else {
			List<String> colList = getResultsColumnName();
			colArr = colList.toArray(new String[colList.size()]);
			
		}
		
		for (String resultHeader: colArr){

			String[] column = getStringResultsColumn(resultHeader);

			if(expectOneResult){
				// expect each column to have exactly one entry
				if(column.length > 1){
					throw new Exception("Expected 1 result for " + resultHeader + ", but retrieved multiple results");
				}else if (column.length < 1){
					throw new Exception("Expected 1 result for " + resultHeader + ", but retrieved zero results");
				}
			}

			results.put(resultHeader, column);
		}

		return results;
	}


	public JSONObject getResponse() {
		return this.response;
	}

	/**
	 * Get query results as ints
	 */
	public Integer [] getIntResultsColumn(String colName) throws Exception {
		Integer ret[] = null;
		String s = null;

		this.checkResultsCol(colName);

		// declare the array and populate it
		ret = new Integer[this.resBindings.size()];
		for (int i=0; i < this.resBindings.size(); i++) {
			if (((JSONObject)this.resBindings.get(i)).containsKey(colName) && 
					((JSONObject)((JSONObject)this.resBindings.get(i)).get(colName)).containsKey("value")) {
				s = (String) ((JSONObject)((JSONObject)this.resBindings.get(i)).get(colName)).get("value");
				ret[i] = Integer.parseInt(s);
			} else {
				ret[i] = null;
			}
		}
		return ret;
	}

	/**
	 * Get query results as Strings
	 */
	public String [] getStringResultsColumn(String colName) throws Exception {
		String ret[] = null;

		this.checkResultsCol(colName);

		// declare the array and populate it
		ret = new String[this.resBindings.size()];
		for (int i=0; i < this.resBindings.size(); i++) {
			if (((JSONObject)this.resBindings.get(i)).containsKey(colName) && 
					((JSONObject)((JSONObject)this.resBindings.get(i)).get(colName)).containsKey("value")) {
					
				ret[i] = (String) ((JSONObject)((JSONObject)this.resBindings.get(i)).get(colName)).get("value");
			} else {
				ret[i] = null;
			}
		}
		return ret;
	}

	/**
	 * Get query results as doubles
	 */	
	public Double [] getDoubleResultsColumn(String colName) throws Exception {
		Double ret[] = null;
		String s = null;

		this.checkResultsCol(colName);

		// declare the array and populate it
		ret = new Double[this.resBindings.size()];
		for (int i=0; i < this.resBindings.size(); i++) {
			if (((JSONObject)this.resBindings.get(i)).containsKey(colName) && 
					((JSONObject)((JSONObject)this.resBindings.get(i)).get(colName)).containsKey("value")) {
				s = (String) ((JSONObject)((JSONObject)this.resBindings.get(i)).get(colName)).get("value");
				ret[i] = Double.parseDouble(s);
			} else {
				ret[i] = null;
			}
		}
		return ret;
	}

	/**
	 * Get query results as Dates
	 */
	public Date [] getDateResultsColumn(String colName) throws Exception {
		Date[] ret = null;
		String s = null;

		this.checkResultsCol(colName);

		// declare the array and populate it
		ret = new Date[this.resBindings.size()];
		for (int i=0; i < this.resBindings.size(); i++) {
			if (((JSONObject)this.resBindings.get(i)).containsKey(colName) && 
					((JSONObject)((JSONObject)this.resBindings.get(i)).get(colName)).containsKey("value")) {
				s = (String) ((JSONObject)((JSONObject)this.resBindings.get(i)).get(colName)).get("value");
				ret[i] = SparqlEndpointInterface.DATE_FORMAT.parse(s);
			} else {
				ret[i] = null;
			}
		}
		return ret;
	}

	/**
	 * Check if the query results contain a given column name (throw exception if not)
	 */
	public void checkResultsCol(String colName) throws Exception {
		int col = -1;

		// find column
		for (int i=0; i < this.resVars.size(); i++) {
			if (((String)this.resVars.get(i)).equals(colName)) {
				col = i;
				break;
			}
		}
		if (col == -1)
			throw new Exception(String.format("SparqlEndpointInterface: Asked for column %s which was not returned by the Sparql query.", colName));
	}


	/**
	 * Return all result column names.
	 */
	public ArrayList<String> getResultsColumnName(){
		ArrayList<String> retval = new ArrayList<String>();
		for (int i=0; i < this.resVars.size(); i++) {
			retval.add((String)(this.resVars.get(i)));
		}
		return retval;
	}


	/**
	 * Get a results content type to be set in the HTTP header.
	 */
	protected String getContentType(SparqlResultTypes resultType) throws Exception{
		if (resultType == null) {
			return this.getContentType(getDefaultResultType());
			
		} else if (resultType == SparqlResultTypes.TABLE || resultType == SparqlResultTypes.CONFIRM) { 
			return CONTENTTYPE_SPARQL_QUERY_RESULT_JSON; 
			
		} else if (resultType == SparqlResultTypes.GRAPH_JSONLD) { 
			return CONTENTTYPE_JSON_LD; 
		} else if (resultType == SparqlResultTypes.HTML) { 
			return CONTENTTYPE_HTML; 
		} 
		
		// fail and throw an exception if the value was not valid.
		throw new Exception("Cannot get content type for query type " + resultType);
	}
	
	// handle results based on expected type
	@SuppressWarnings("unchecked")
	private JSONObject getResultsFromResponse(JSONObject resp, SparqlResultTypes resultType) throws Exception{
		
		JSONObject retval = null;		
		this.response = resp;

		// check for a result type that creates a table.
		// null is default assumption that one meant a tabular result.
		if(resultType == SparqlResultTypes.TABLE || resultType == SparqlResultTypes.CONFIRM){
			JSONObject head = (JSONObject)response.get("head");
			if (head == null) {
				throw new Exception("Sparql server did not return a 'head' object");
			}
			this.resVars = (JSONArray) head.get("vars");
			if (this.resVars == null){
				throw new Exception("Sparql server response 'head' did not include a 'vars' array of column names");
			}
			JSONObject results = (JSONObject) response.get("results");
			if (results == null) {
				throw new Exception("Sparql server did not return a 'results' object");
			}
			this.resBindings = (JSONArray) results.get("bindings");
			if (this.resBindings == null){
				throw new Exception("Sparql server response 'results' did not include a 'bindings' array of result rows");
			}
			
			retval = new JSONObject();
			if(this.resBindings == null){
				this.resBindings = new JSONArray();
			}
		
			if(resultType == SparqlResultTypes.TABLE){
				retval.put(TableResultSet.TABLE_JSONKEY, getTableJSONFromResponse(this.resVars, this.resBindings));	// @table		
			}else{
				retval.put(SimpleResultSet.MESSAGE_JSONKEY, getAuthMessageFromResponse(this.resBindings)); // @message
			}
			
		}
		else if(resultType == SparqlResultTypes.GRAPH_JSONLD){
			retval = new JSONObject(resp);
		}
		else{
			throw new Exception("an unknown results type was passed to \"getResultsBasedOnExpectedType\". don't know how to handle type: " + resultType);
		}
		return retval;
	}

	
	/**
	 * Parse an auth query confirmation.  The message will the first entry in the rows array.
	 * @param rowsJsonArray
	 * @return
	 */
	private static String getAuthMessageFromResponse(JSONArray rowsJsonArray){
		
		String message = "";
		for(int i = 0; i < rowsJsonArray.size(); i++){
			JSONObject row = (JSONObject) rowsJsonArray.get(i); // e.g. {"callret-0":{"type":"literal","value":"Insert into <http:\/\/research.ge.com\/graph>, 1 (or less) triples -- done"}}
			JSONObject value;
			String key, valueValue;
			
			@SuppressWarnings("unchecked")
			Iterator<String> iter = row.keySet().iterator();			
			while(iter.hasNext()){
				key = (String)iter.next();
				value = (JSONObject) row.get(key);
				valueValue = (String) value.get("value");
				message += valueValue;
			}
		}
		
		return message;
	}
	
	
	/**
	 * Convert response table json to SemTK Table json
	 * 
	 * Sample input SPARQL table json:
	 *{"Test":{"type":"literal","value":"http:\/\/research.ge.com\/dataset#Test_1"},"number":{"datatype":"http:\/\/www.w3.org\/2001\/XMLSchema#integer","type":"typed-literal","value":"1272"}},
	 *{"Test":{"type":"literal","value":"http:\/\/research.ge.com\/dataset#Test_2"},"number":{"datatype":"http:\/\/www.w3.org\/2001\/XMLSchema#integer","type":"typed-literal","value":"1274"}},
	 *{"Test":{"type":"literal","value":"http:\/\/research.ge.com\/dataset#Test_3"},"number":{"datatype":"http:\/\/www.w3.org\/2001\/XMLSchema#integer","type":"typed-literal","value":"1276"}},
	 *
	 * Sample output SemTK table json:
	 * {
	 * "col_names":["Test","testnum"],
	 * "rows":[["http://research.ge.com/energy/dataset#Test_1272","1272"],["http://research.ge.com/dataset#Test_1274","1274"],["http://research.ge.com/dataset#Test_1276","1276"]],
	 * "col_type":["uri","http:\/\/www.w3.org\/2001\/XMLSchema#integer"], // TODO is this correct?
	 * "col_count":2,
	 * "row_count":3
	 * }
	 * 
	 * @param colNamesJsonArray the JSONArray containing the column names
	 * @param rowsJsonArray the JSONArray containing the data rows
	 * @return
	 * @throws Exception 
	 */
	private static JSONObject getTableJSONFromResponse(JSONArray colNamesJsonArray, JSONArray rowsJsonArray) throws Exception{

		String key, valueValue, valueType, valueDataType;
		JSONObject jsonCell;
		
		ArrayList<String> colsForNewTable = new ArrayList<String>();
		ArrayList<String> colTypesForNewTable = new ArrayList<String>();
		ArrayList<String> rowForNewTable;
		ArrayList<ArrayList<String>> rowsForNewTable = new ArrayList<ArrayList<String>>();
		HashMap<String, Integer> colNumHash = new HashMap<String, Integer>();
		HashMap<String, String> colTypeHash = new HashMap<String, String>();
		String curType = null;
		final String UNKNOWN = "unknown";
		final String MIXED = "http://www.w3.org/2001/XMLSchema#string";
		
		// **** Column Names and types.   Parallel arrays.  Types still unknown. ****
		for (Object colObj : colNamesJsonArray ) {
			String colStr = (String) colObj;
			colsForNewTable.add(colStr);
			colTypesForNewTable.add(UNKNOWN);	
			
			colNumHash.put(colStr, colsForNewTable.size()-1);   // hash the column indices
			colTypeHash.put(colStr, UNKNOWN);                 // hash the column types
		}
		
		// **** Rows ****
		for(int i = 0; i < rowsJsonArray.size(); i++){
			// sample row: {"Test":{"type":"literal","value":"http:\/\/research.ge.com\/dataset#Test_1276"},"testnum":{"datatype":"http:\/\/www.w3.org\/2001\/XMLSchema#integer","type":"typed-literal","value":"1276"}}
			JSONObject row = (JSONObject) rowsJsonArray.get(i);		
			
			rowForNewTable = new ArrayList<String>();  // start new row
			
			//**** loop through columns in the correct order ****
			for (Object colObj : colNamesJsonArray ) {
				key = (String) colObj;
				jsonCell = (JSONObject) row.get(key);
				
				if (jsonCell == null) {
					rowForNewTable.add(""); 
					
				} else {
					valueValue = (String) jsonCell.get("value");	
					valueType =  (String) jsonCell.get("type");
					valueDataType = (valueType.equals("typed-literal")) ? (String) jsonCell.get("datatype") : valueType;  // e.g. "http:\/\/www.w3.org\/2001\/XMLSchema#integer", but only if type is "typed-literal" 
					
					// add the value to the row
					rowForNewTable.add(valueValue);
					
					// check the type
					curType = colTypeHash.get(key);
					
					// fix UNKNOWN's as they become known
					if (curType.equals(MIXED)) {
						// do nothing if cell is already MIXED
					}
					else if (curType.equals(UNKNOWN)) {
						colTypeHash.put(key, valueDataType);
						colTypesForNewTable.set(colNumHash.get(key), valueDataType);
					
					// insert MIXED if types are coming back funny
					} else if (!curType.equals(valueDataType)) {
						colTypeHash.put(key, MIXED);
						colTypesForNewTable.set(colNumHash.get(key), MIXED);
					}	
				}
			}
			rowsForNewTable.add(rowForNewTable); // add the row to the set of rows
		}
		
		String[] colsForNewTableArray = colsForNewTable.toArray(new String[0]);
		String[] colTypesForNewTableArray = colTypesForNewTable.toArray(new String[0]);
		// create JSON to return 
		Table table = new Table(colsForNewTableArray, colTypesForNewTableArray, rowsForNewTable);  
		
		JSONObject tableJson = table.toJson();
		return tableJson;
	}	

	public abstract SparqlEndpointInterface copy() throws Exception;
}
