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

package com.ge.research.semtk.test;

import java.io.File;
import java.util.UUID;

import com.ge.research.semtk.api.nodeGroupExecution.NodeGroupExecutor;
import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClient;
import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClientConfig;
import com.ge.research.semtk.auth.AuthorizationProperties;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.edc.client.OntologyInfoClient;
import com.ge.research.semtk.edc.client.OntologyInfoClientConfig;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.edc.client.StatusClientConfig;
import com.ge.research.semtk.load.client.IngestorClientConfig;
import com.ge.research.semtk.load.client.IngestorRestClient;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreConfig;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.properties.EndpointProperties;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.S3BucketConfig;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.client.SparqlQueryAuthClientConfig;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClient;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;
import com.ge.research.semtk.sparqlX.dispatch.client.DispatchClientConfig;
import com.ge.research.semtk.sparqlX.dispatch.client.DispatchRestClient;
import com.ge.research.semtk.utility.Utility;

/**
 * Utility class for configuring integration tests.
 * 
 * NOTE: This class cannot be put in under src/test/java because it must remain accessible to other projects.
 */
public class IntegrationTestUtility {
	
	// property file with integration test configurations
	public static final String INTEGRATION_TEST_PROPERTY_FILE = "src/test/resources/integrationtest.properties";

	public static String getAuthUsernameKey() throws Exception{
		return getIntegrationTestProperty("integrationtest.auth.usernamekey");
	}
	
	// protocol for all services
	public static String getServiceProtocol() throws Exception{
		return getIntegrationTestProperty("integrationtest.protocol");
	}
	
	public static String getDispatcherClassName() throws Exception{
		return getIntegrationTestProperty("integrationtest.dispatcherclassname");
	}
	
	// sparql endpoint
	public static String getSparqlServerOnly() throws Exception {
		String stripProtocol = getSparqlServer().split("://")[1];
		String stripPort = stripProtocol.split(":")[0];
		return stripPort;
				
	}
	public static int getSparqlServerPort() throws Exception {
		String [] splitByColon = getSparqlServer().split(":");
		String portMaybeEndpoint = splitByColon[2];
		return Integer.valueOf(portMaybeEndpoint.split("/")[0]);
	}
	public static String getSparqlServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.sparqlendpoint.server");
	}
	public static String getSparqlServerType() throws Exception{
		return getIntegrationTestProperty("integrationtest.sparqlendpoint.type");
	}
	public static String getSparqlServerUsername() throws Exception{
		return getIntegrationTestProperty("integrationtest.sparqlendpoint.username");
	}
	public static String getSparqlServerPassword() throws Exception{
		return getIntegrationTestProperty("integrationtest.sparqlendpoint.password");
	}
	
	/**
	 * Get JobEndpointProperties without domain or dataset
	 * @return
	 * @throws Exception
	 */
	public static EndpointProperties getEndpointProperties() throws Exception {
		EndpointProperties ret = new EndpointProperties();
		
		ret.setEndpointType(getSparqlServerType());
		ret.setEndpointServerUrl(getSparqlServer());
		ret.setEndpointUsername(getSparqlServerUsername());
		ret.setEndpointPassword(getSparqlServerPassword());
		
		return ret;
	}
	
	public static AuthorizationProperties getAuthorizationProperties() {
		AuthorizationProperties ret = new AuthorizationProperties();
		try {
			ret.setRefreshFreqSeconds(Integer.parseInt(getIntegrationTestProperty("auth.refreshFreqSeconds")));
		} catch (Exception e) {
			// ok. optional property
		}
		return ret;
	}
	
	// sparql query service
	public static String getSparqlQueryServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.sparqlqueryservice.server");
	}
	public static int getSparqlQueryServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.sparqlqueryservice.port")).intValue();	
	}	
	
	// ingestion service
	public static String getIngestionServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.ingestionservice.server");
	}
	public static int getIngestionServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.ingestionservice.port")).intValue();	
	}	
		
	// status service
	public static String getStatusServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.statusservice.server");
	}
	public static int getStatusServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.statusservice.port")).intValue();
	}
	
	// results service
	public static String getResultsServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.resultsservice.server");
	}
	public static int getResultsServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.resultsservice.port")).intValue();
	}
	public static int getFdcSampleServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.fdcsampleservice.port")).intValue();
	}
	public static String getFdcSampleServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.fdcsampleservice.server");
	}
	
	// dispatch service
	public static String getDispatchServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.dispatchservice.server");
	}
	public static int getDispatchServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.dispatchservice.port")).intValue();
	}	
		
	// Hive service
	public static String getHiveServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.hiveservice.server");
	}
	public static int getHiveServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.hiveservice.port")).intValue();
	}
	// nodegroup store service
	public static String getNodegroupServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.nodegroupservice.server");
	}
	public static int getNodegroupServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.nodegroupservice.port")).intValue();
	}
	// nodegroup store service
	public static String getNodegroupStoreServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.nodegroupstoreservice.server");
	}
	public static int getNodegroupStoreServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.nodegroupstoreservice.port")).intValue();
	}
	
	// nodegroup execution service
	public static String getNodegroupExecutionServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.nodegroupexecution.server");
	}
	public static int getNodegroupExecutionServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.nodegroupexecution.port")).intValue();
	}
	
	// oInfo store service
	public static String getOntologyInfoServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.ontologyinfoservice.server");
	}
	public static int getOntologyInfoServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.ontologyinfoservice.port")).intValue();
	}
	// Hive
	public static String getHiveServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.hive.server");
	}
	public static int getHivePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.hive.port")).intValue();
	}
	public static String getHiveUsername() throws Exception{
		return getIntegrationTestProperty("integrationtest.hive.username");
	}
	public static String getHivePassword() throws Exception{
		return getIntegrationTestProperty("integrationtest.hive.password");
	}
	public static String getHiveDatabase() throws Exception{
		return getIntegrationTestProperty("integrationtest.hive.database");
	}
	
	// ArangoDB
	public static String getArangoDbServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.arangodb.server");
	}
	public static int getArangoDbPort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.arangodb.port")).intValue();
	}
	public static String getArangoDbUsername() throws Exception{
		return getIntegrationTestProperty("integrationtest.arangodb.username");
	}
	public static String getArangoDbPassword() throws Exception{
		return getIntegrationTestProperty("integrationtest.arangodb.password");
	}
	
	/**
	 * Get a ResultsClient using the integration test properties.
	 */
	public static ResultsClient getResultsClient() throws Exception{
		return new ResultsClient(getResultsClientConfig());
	}
	
	public static ResultsClientConfig getResultsClientConfig() throws Exception{
		return new ResultsClientConfig(getServiceProtocol(), getResultsServiceServer(), getResultsServicePort());
	}
	
	/**
	 * Get a StatusClient using the integration test properties.
	 */
	public static StatusClient getStatusClient(String jobId) throws Exception{
		return new StatusClient(new StatusClientConfig(getServiceProtocol(), getStatusServiceServer(), getStatusServicePort(), jobId));
	}	
	
	public static SparqlEndpointInterface getServicesSei() throws Exception {
		return SparqlEndpointInterface.getInstance(
				getSparqlServerType(), 
				getSparqlServer(),
				getServicesGraph(), 
				getSparqlServerUsername(), 
				getSparqlServerPassword());
	}
	
	public static String getServicesGraph() throws Exception {
		return getIntegrationTestProperty("integrationtest.services.graph");
	}
	/**
	 * Get a SparqlQueryClient using the integration test properties.
	 */
	public static SparqlQueryClient getSparqlQueryClient(String serviceEndpoint, String sparqlServer, String dataset) throws Exception{
		return new SparqlQueryClient(new SparqlQueryClientConfig(getServiceProtocol(), getSparqlQueryServiceServer(), getSparqlQueryServicePort(), serviceEndpoint, sparqlServer, getSparqlServerType(), dataset));
	}
	
	/**
	 * Get a SparqlQueryClient using the integration test properties.
	 */
	public static SparqlQueryClient getSparqlQueryAuthClient(String serviceEndpoint, String sparqlServer, String dataset) throws Exception{
		return new SparqlQueryClient(new SparqlQueryAuthClientConfig(getServiceProtocol(), getSparqlQueryServiceServer(), getSparqlQueryServicePort(), serviceEndpoint, sparqlServer, getSparqlServerType(), dataset, getSparqlServerUsername(), getSparqlServerPassword()));
	}
	
	public static SparqlQueryClient getSparqlQueryAuthClient() throws Exception{
		return new SparqlQueryClient(new SparqlQueryAuthClientConfig(getServiceProtocol(), getSparqlQueryServiceServer(), getSparqlQueryServicePort(), "/sparqlQueryService/query", getSparqlServer(), getSparqlServerType(), TestGraph.getDataset(), getSparqlServerUsername(), getSparqlServerPassword()));
	}

	public static OntologyInfoClient getOntologyInfoClient() throws Exception{
		return new OntologyInfoClient(new OntologyInfoClientConfig(getServiceProtocol(), getOntologyInfoServiceServer(), getOntologyInfoServicePort()));
	}
	
	/**
	 * Get a NodeGroupStoreRestClient using the integration test properties.
	 */
	public static NodeGroupStoreRestClient getNodeGroupStoreRestClient() throws Exception{
		return new NodeGroupStoreRestClient(new NodeGroupStoreConfig(getServiceProtocol(), getNodegroupStoreServiceServer(),  getNodegroupStoreServicePort()));
	}
	
	/**
	 * Get a NodeGroupStoreRestClient using the integration test properties.
	 */
	public static NodeGroupExecutionClient getNodeGroupExecutionRestClient() throws Exception{
		return new NodeGroupExecutionClient(new NodeGroupExecutionClientConfig(getServiceProtocol(), getNodegroupExecutionServiceServer(), getNodegroupExecutionServicePort(), getSparqlServerUsername(), getSparqlServerPassword()));
	}
	
	/**
	 * Get a NodeGroupExecutor using the integration test properties.
	 */
	public static NodeGroupExecutor getNodegroupExecutor() throws Exception{
		NodeGroupStoreRestClient ngsrc = getNodeGroupStoreRestClient();
		DispatchRestClient drc = new DispatchRestClient(new DispatchClientConfig(getServiceProtocol(), getDispatchServiceServer(), getDispatchServicePort()));
		StatusClient stc = new StatusClient(new StatusClientConfig(getServiceProtocol(), getStatusServiceServer(), getStatusServicePort(), "totally fake"));
		ResultsClient rc  = new ResultsClient(new ResultsClientConfig(getServiceProtocol(), getResultsServiceServer(), getResultsServicePort()));
		IngestorRestClient ic = new IngestorRestClient(new IngestorClientConfig(getServiceProtocol(), getIngestionServiceServer(), getIngestionServicePort()));		
		SparqlEndpointInterface sei = getServicesSei();
		return new NodeGroupExecutor(ngsrc, drc, rc, sei, ic);
	}
	
	public static String getIntegrationTestProperty(String key) throws Exception{
		if (!Utility.ENV_TEST) {
			throw new Exception(Utility.ENV_TEST_EXCEPTION_STRING);
		}
		if (key.startsWith("integrationtest")) {
			return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, key);
		} else {
			return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest." + key);
		}
	}
	
	public static File getSampleFile(Object caller) throws Exception {
		return Utility.getResourceAsFile(caller, "/annotationBattery.owl");
	}
	
	public static String getSampleJsonBlob(Object caller) throws Exception {
		return Utility.getResourceAsString(caller, "/annotationBatteryOInfo.json");
	}
	
	public static Table getSampleTable() throws Exception {
		Table table = new Table(new String [] {"col1", "col2"}, new String [] {"string", "int"});
		table.addRow(new String [] {"value1", "2"});
		return table;
	}

	public static void authenticateJunit() {
		ThreadAuthenticator.authenticateThisThread("junit");
	}
	
	public static String generateUser(String testName, String suffix) {
		return "junit_" + testName + "_" + suffix;
	}
	
	public static String generateJobId(String testName) {
		return "junit_" + UUID.randomUUID().toString();
	}
	
	/**
	 * return an un-verified S3config.  It could be empty/garbage.
	 * @return
	 * @throws Exception
	 */
	public static S3BucketConfig getS3Config() throws Exception {
		// these should exist, but may be blank
		String region = getIntegrationTestProperty("integrationtest.neptuneupload.s3ClientRegion");
		String iamRoleArn = getIntegrationTestProperty("integrationtest.neptuneupload.awsIamRoleArn");
		String name =   getIntegrationTestProperty("integrationtest.neptuneupload.s3BucketName");
        
        S3BucketConfig config = new S3BucketConfig(region, name, iamRoleArn);
        
        return config;
	}
	
	public static void cleanupNodegroupStore(String creator) throws Exception {
		cleanupNodegroupStore(getNodeGroupStoreRestClient(), creator);
	}

	public static void cleanupNodegroupStore(NodeGroupStoreRestClient nodeGroupStoreClient, String creator) throws Exception {
		// Clean up old nodegroups.   Shouldn't happen but it seems to.
		// So as not to interfere with others' testing, don't delete if creation date is today
		TableResultSet tabRes = nodeGroupStoreClient.executeGetNodeGroupMetadata();
		tabRes.throwExceptionIfUnsuccessful();
		Table storeTab = tabRes.getTable();
		String today = Utility.getSPARQLCurrentDateString();
		for (int i=0; i < storeTab.getNumRows(); i++) {
			if (storeTab.getCell(i,  "creator").equals(creator) && 
					! storeTab.getCell(i, "creationDate").equals(today)) {
				String id = storeTab.getCell(i, "ID");
				nodeGroupStoreClient.deleteStoredNodeGroup(id);
			}
		}
	}
}
