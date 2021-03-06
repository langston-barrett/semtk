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


package com.ge.research.semtk.standaloneExecutables;

import com.opencsv.CSVReader;
import org.apache.commons.lang.ArrayUtils;
import org.json.simple.JSONObject;

import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreConfig;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlConnection;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Utility to load nodegroups from local disk to the semantic store.
 */
public class StoreNodeGroup {

	private static final String[] headers= {"Context","ID","comments","creationDate","creator","jsonFile"};

	/**
	 * Main method
	 */
	public static void main(String[] args) throws Exception{

		// get arguments
		if(args.length != 2 && args.length != 3) {
			System.out.println("\nUsage: http://endpoint:port inputFile.csv [sparqlConnectionOverride.json]\n");
			System.exit(0);
		}
		String endpointUrlWithPort = args[0]; //e.g. http://localhost:12056
		String csvFile = args[1];
		String sparqlConnOverrideFile = (args.length == 3) ? args[2] : null;

		try {
			processCSVFile(endpointUrlWithPort, csvFile, sparqlConnOverrideFile);
		} catch(Exception e){
			LocalLogger.printStackTrace(e);
			System.exit(1);  // need this to catch errors in the calling script
		}

		System.exit(0);
	}


	public static void processCSVFile(String endpointUrlWithPort, String csvFile) throws Exception {
		processCSVFile(endpointUrlWithPort, csvFile, null);
	}

	public static void processCSVFile(String endpointUrlWithPort, String csvFile, String sparqlConnOverrideFile) throws Exception {

		CSVReader br = new CSVReader(new FileReader(csvFile));

		String[] parsedLine = br.readNext(); // header line
		if (parsedLine.length != headers.length) {
			throw new Exception("Wrong number of columns on header: "+Arrays.toString(parsedLine));
		}

		// check first line column names
		int col=0;
		for (String headerName: parsedLine) {
			if (! headerName.trim().equalsIgnoreCase(headers[col].trim())) {
				LocalLogger.logToStdErr("Wrong column name: "+headerName+". Was expecting: "+headers[col]);
			}
			col++;
		}

		int lineNumber=1; // header is line #1
		while ((parsedLine = br.readNext()) != null) {
			lineNumber++;
			if (parsedLine.length == 0) {
				LocalLogger.logToStdOut("Ignoring blank line number: "+ lineNumber);
			} else  if (parsedLine.length < headers.length) {
				LocalLogger.logToStdOut("Ignoring! Missing column in line: "+Arrays.toString(parsedLine));
			} else if (parsedLine.length > headers.length ) {
				LocalLogger.logToStdOut("Ignoring! Found Too many: "+parsedLine.length+" columns in line: "+Arrays.toString(parsedLine));

			} else {
				//String context = parsedLine[0];
				String ngId = parsedLine[1]; // e.g. "AMP Design Curve"
				String ngComments = parsedLine[2]; // e.g. "Retrieve an AMP design curve"
				// ignore parsedLine[3]...
				String ngOwner = parsedLine[4]; // e.g. sso as "20000588"
				String ngFilePath = parsedLine[5]; // system full path of the file with json representation of the nodegroup
				String endpointPart[] = endpointUrlWithPort.split(":/*");

				// if nodegroup json path is bad, try same directory as csv file
				if (!(new File(ngFilePath).exists())) {
					String parent = (Paths.get(csvFile)).getParent().toString();
					String fname =  (Paths.get(ngFilePath)).getFileName().toString();
					ngFilePath = (Paths.get(parent, fname)).toString();
				}

				if (endpointUrlWithPort != null && !endpointUrlWithPort.trim().isEmpty()) {
					storeSingleNodeGroup(endpointUrlWithPort, ngId, ngComments, ngOwner, ngFilePath, endpointPart, sparqlConnOverrideFile);
				} else {
					LocalLogger.logToStdOut("Ignoring line: "+Arrays.toString(parsedLine));
				}
			}
		}
		LocalLogger.logToStdOut("Finished processing file: "+csvFile);
	}


	private static void storeSingleNodeGroup(String endpointUrlWithPort, String ngId, String ngComments, String ngOwner, String ngFilePath, String[] endpointPart, String sparqlConnOverrideFile) throws Exception {

		// validate nodegroup file
		if(!ngFilePath.endsWith(".json")){
			throw new Exception("Error: Nodegroup file " + ngFilePath + " is not a JSON file");
		}
		JSONObject ngJson = Utility.getJSONObjectFromFilePath(ngFilePath);
		
		// if a SPARQL connection override is provided, use it
		if(sparqlConnOverrideFile != null){
			if(!sparqlConnOverrideFile.endsWith(".json")){
				throw new Exception("Error: SPARQL connection override file " + sparqlConnOverrideFile + " is not a JSON file");
			}
			SparqlConnection sparqlConnOverride = new SparqlConnection(Utility.getJSONObjectFromFilePath(sparqlConnOverrideFile).toJSONString());
			LocalLogger.logToStdOut("Overriding SPARQL connection");
			SparqlGraphJson sgJson = new SparqlGraphJson(ngJson);
			sgJson.setSparqlConn(sparqlConnOverride);
			ngJson = sgJson.getJson();
		}

		// set up client
		NodeGroupStoreConfig config = new NodeGroupStoreConfig(endpointPart[0], endpointPart[1], Integer.parseInt(endpointPart[2]));
		NodeGroupStoreRestClient client = new NodeGroupStoreRestClient(config);

		// check whether id already exists
		TableResultSet res = client.executeGetNodeGroupMetadata();
		res.throwExceptionIfUnsuccessful("Error while checking of nodegroup Id already exists");

		// delete old copy if it does
		if (ArrayUtils.contains(res.getTable().getColumn("ID"), ngId)) {
			SimpleResultSet r = client.deleteStoredNodeGroup(ngId);
			r.throwExceptionIfUnsuccessful("Error while removing preview version of nodegroup");
		}

		// store it
		SimpleResultSet r = client.executeStoreNodeGroup(ngId, ngComments, ngOwner, ngJson);
		r.throwExceptionIfUnsuccessful("Error while storing nodegroup");

		LocalLogger.logToStdOut("Successfully stored " + ngId);
	}

}
