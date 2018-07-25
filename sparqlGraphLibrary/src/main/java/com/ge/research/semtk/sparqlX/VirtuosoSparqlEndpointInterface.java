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

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;

import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

/**
 * Interface to Virtuoso SPARQL endpoint
 */
public class VirtuosoSparqlEndpointInterface extends SparqlEndpointInterface {
	
	/**
	 * Constructor
	 */
	public VirtuosoSparqlEndpointInterface(String server, String graph)	throws Exception {
		super(server, graph);
		// TODO Auto-generated constructor stub
	}
 
	/**
	 * Constructor used for authenticated connections... like insert/update/delete/clear
	 */
	public VirtuosoSparqlEndpointInterface(String server, String graph, String user, String pass)	throws Exception {
		super(server, graph, user, pass);
		// TODO Auto-generated constructor stub
	}
	
	public String getServerType() {
		return "virtuoso";
	}
	
	/**
	 * Build a GET URL
	 */
	public String getGetURL(){
		if(this.userName != null && this.userName.length() > 0 && this.password != null && this.password.length() > 0){
			return String.format("%s:%s/sparql-auth/?default-graph-uri=%s&format=json&query=", server, this.port, this.graph);  
		}else{
			return String.format("%s:%s/sparql/?default-graph-uri=%s&format=json&query=", this.server, this.port, this.graph); 
		}
	}
	
	/**
	 * Build a POST URL
	 */
	public String getPostURL(){
		if(this.userName != null && this.userName.length() > 0 && this.password != null && this.password.length() > 0){
			return String.format("%s:%s/sparql-auth", this.server, this.port);
		}else{
			return String.format("%s:%s/sparql", this.server, this.port);
		}
	}

	/**
	 * Build a upload URL
	 */
	public String getUploadURL() throws Exception{
		if(this.userName != null && this.userName.length() > 0 && this.password != null && this.password.length() > 0){
			return this.server + "/sparql-graph-crud-auth";
		}else{
			throw new Exception("Virtuoso requires authentication for file upload");	
		}
	}
	
	/**
	 * Execute an auth query using POST, and using AUTH if this.userName != null
	 * @return a JSONObject wrapping the results. in the event the results were tabular, they can be obtained in the JsonArray "@Table". if the results were a graph, use "@Graph" for json-ld
	 * @throws Exception
	 */
	public JSONObject executeUpload(byte[] owl) throws Exception{
		
        CloseableHttpClient httpclient = this.buildHttpClient();
		HttpHost targetHost = this.buildHttpHost();
		BasicHttpContext localcontext = this.buildHttpContext(targetHost);
		HttpPost httppost = new HttpPost(this.getUploadURL());
		
		String resultsFormat = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
		this.addHeaders(httppost, resultsFormat);
		 
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();    
	
		builder.addTextBody("graph-uri", this.graph);
		builder.addBinaryBody("res-file", owl);
		HttpEntity entity = builder.build();
		httppost.setEntity(entity);
		
		/*  THIS IS THE MULTIPART FORMAT WE NEED TO SEND.
		
		Content-Type: multipart/form-data; boundary=---------------------------32932166721282
		Content-Length: 234
		
		-----------------------------32932166721282
		Content-Disposition: form-data; name="graph-uri"
		
		http://www.kdl.ge.com/changeme
		-----------------------------32932166721282
		Content-Disposition: form-data; name="res-file"; filename="employee.owl"
		Content-Type: application/octet-stream
		
		<rdf:RDF
		    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
		    xmlns:owl="http://www.w3.org/2002/07/owl#"
		    xmlns="http://kdl.ge.com/pd/employee#"
		    xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
		  .
		  .
		  .
		</rdf:RDF>
		
		-----------------------------32932166721282--

		 */
		
		executeTestQuery();

		HttpResponse response_http = httpclient.execute(targetHost, httppost, localcontext);
		HttpEntity resp_entity = response_http.getEntity();
		// get response with HTML tags removed
		String responseTxt = EntityUtils.toString(resp_entity, "UTF-8").replaceAll("\\<.*?>"," ");

		SimpleResultSet ret = new SimpleResultSet();
		
		if(responseTxt.trim().isEmpty()){
			// success or bad login :-(
			ret.setSuccess(true);
		} else {
			ret.setSuccess(false);
			ret.addRationaleMessage("SparqlEndpointInterface.executeAuthUploadOwl", responseTxt);
		}
		resp_entity.getContent().close();
		return ret.toJson();
	}
	/**
	 * Handle an empty response
	 * (if a Virtuoso response is empty, then something is wrong)
	 * @throws Exception 
	 */
	@Override
	public void handleEmptyResponse() throws Exception {
		throw new Exception("Virtuoso returning empty response (could be wrong username/password)");	
	}

	@Override
	public SparqlEndpointInterface copy() throws Exception {
		VirtuosoSparqlEndpointInterface retval = null;
		
		retval = new VirtuosoSparqlEndpointInterface(this.getServerAndPort(), this.graph, this.userName, this.password);
		
		return (SparqlEndpointInterface) retval;
	}
	
	/**
	 * Get explanations of known triple store errors, given responseTxt contains "error"
	 * @param responseTxt
	 * @return
	 */
	@Override
	protected String getResposeTextExplanation(String responseTxt) {
		if ( responseTxt.contains("Error SP031")) {
			return "SemTk says: Virtuoso query may be too large or complex.\n";
		} else {
			return "Non-JSON error was returned from Virtuoso.\n";
		}
	}
}
