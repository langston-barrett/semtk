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


package com.ge.research.semtk.edc.client.test;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.properties.ResultsServiceProperties;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.utility.Utility;

public class ResultsClientTest_IT {
	
	private static String SERVICE_PROTOCOL;
	private static String SERVICE_SERVER;
	private static int SERVICE_PORT;
	
	private static ResultsClient client = null;
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		SERVICE_PROTOCOL = IntegrationTestUtility.get("protocol");
		SERVICE_SERVER = IntegrationTestUtility.get("resultsservice.server");
		SERVICE_PORT = IntegrationTestUtility.getInt("resultsservice.port");
		client = new ResultsClient(new ResultsClientConfig(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT));
	}

	@Test
	public void testStoreTable() throws Exception {

		String jobId = "test_jobid_" + UUID.randomUUID();
		
		String [] cols = {"col1", "col2"};
		String [] types = {"String", "String"};
		ArrayList<String> row = new ArrayList<String>();
		row.add("one");
		row.add("two");
		ArrayList<String> row2 = new ArrayList<String>();
		row2.add("three");
		row2.add("four");
		
		try {
			Table table = new Table(cols, types, null);
			table.addRow(row);
			table.addRow(row2);
			
			client.execStoreTableResults(jobId, table);

			String expectedJsonString = "{\"col_names\":[\"col1\",\"col2\"],\"rows\":[[\"one\",\"two\"],[\"three\",\"four\"]],\"type\":\"TABLE\",\"col_type\":[\"String\",\"String\"],\"col_count\":2,\"row_count\":2}";		
			String expectedCSVString = "col1,col2\n\"one\",\"two\"\n\"three\",\"four\"\n";

			
			Table tbl = client.getTableResultsJson(jobId, null);
			String resultJSONString = tbl.toJson().toJSONString();
			
			
			assertEquals(resultJSONString, expectedJsonString); 		// check the JSON results
			
			CSVDataset data = client.getTableResultsCSV(jobId, null);
			CSVDataset compare = new CSVDataset(expectedCSVString, true);
			
			
			ArrayList<String> compareColumnNames = compare.getColumnNamesinOrder();
			ArrayList<String> resultColumnNames  = data.getColumnNamesinOrder();
					
			assertEquals(compareColumnNames.get(0), resultColumnNames.get(0));
			assertEquals(compareColumnNames.get(1), resultColumnNames.get(1));
						
			// check getting results as json
			Table jTable = client.getTableResultsJson(jobId, null);
			assertEquals(jTable.toJson().toString(), expectedJsonString);
			
			// check getting results as csv
			CSVDataset resCsvDataset = client.getTableResultsCSV(jobId, null);
			assertEquals(resCsvDataset.getColumnNamesinOrder().get(0), "col1");
			assertEquals(resCsvDataset.getColumnNamesinOrder().get(1), "col2");
			ArrayList<ArrayList<String>> resCsvRows = resCsvDataset.getNextRecords(5);
			assertEquals(resCsvRows.size(), 2);				// 2 rows
			assertEquals(resCsvRows.get(0).size(), 2);		// 2 columns
			assertEquals(resCsvRows.get(0).get(0), "one");
			assertEquals(resCsvRows.get(0).get(1), "two");
			assertEquals(resCsvRows.get(1).get(0), "three");
			assertEquals(resCsvRows.get(1).get(1), "four");
			
		} finally {
			cleanup(client, jobId);
		}
	}
	
	@Test
	/**
	 * Make sure the UTF-8 ish micron character survives the results service
	 * @throws Exception
	 */
	public void testStoreTableMicronChar() throws Exception {

		String jobId = "test_jobid_" + UUID.randomUUID();
		
		String [] cols = {"col1", "col2"};
		String [] types = {"String", "String"};
		ArrayList<String> row = new ArrayList<String>();
		row.add("one-µ");
		row.add("two");
		ArrayList<String> row2 = new ArrayList<String>();
		row2.add("three");
		row2.add("four");
		
		try {
			Table table = new Table(cols, types, null);
			table.addRow(row);
			table.addRow(row2);
			
			client.execStoreTableResults(jobId, table);

			String expectedJsonString = "{\"col_names\":[\"col1\",\"col2\"],\"rows\":[[\"one-µ\",\"two\"],[\"three\",\"four\"]],\"type\":\"TABLE\",\"col_type\":[\"String\",\"String\"],\"col_count\":2,\"row_count\":2}";		
			
			Table tbl = client.getTableResultsJson(jobId, null);
			String resultJSONString = tbl.toJson().toJSONString();		
			
			assertEquals(resultJSONString, expectedJsonString); 		// check the JSON results
						
			// check getting results as json
			Table jTable = client.getTableResultsJson(jobId, null);
			assertEquals(jTable.toJson().toString(), expectedJsonString);
			
			// check getting results as csv
			CSVDataset resCsvDataset = client.getTableResultsCSV(jobId, null);
			ArrayList<ArrayList<String>> resCsvRows = resCsvDataset.getNextRecords(5);
			assertEquals(resCsvRows.get(0).get(0), "one-µ");
			
			
		} finally {
			cleanup(client, jobId);
		}
	}
	
	
	@Test
	public void testStoreTable_WithQuotesAndCommas() throws Exception {
		
		String jobId = "test_jobid_" + UUID.randomUUID();
		
		String [] cols = {"colA", "colB","colC","colD"};
		String [] types = {"String", "String", "String","String"};
		ArrayList<String> row = new ArrayList<String>();
		row.add("apple,ant");  					// this element has a comma
		row.add("bench");
		row.add("\"cabana\"");					// this element has quotes
		row.add("Dan declared \"hi, dear\"");	// this element has quotes and a comma
		
		try {
			Table table = new Table(cols, types, null);
			table.addRow(row);
			client.execStoreTableResults(jobId, table);	
			
			// check the JSON results
			Table tbl = client.getTableResultsJson(jobId, null);
			String resultJSONString = tbl.toJson().toJSONString();
			
			String expectedJSONString = "{\"col_names\":[\"colA\",\"colB\",\"colC\",\"colD\"],\"rows\":[[\"apple,ant\",\"bench\",\"\\\"cabana\\\"\",\"Dan declared \\\"hi, dear\\\"\"]],\"type\":\"TABLE\",\"col_type\":[\"String\",\"String\",\"String\",\"String\"],\"col_count\":4,\"row_count\":1}";  // validated json
			//assertEquals(expectedJSONString, resultJSONString);
			assertEquals(expectedJSONString.length(), resultJSONString.length());
			
			for(int i = 0; i < expectedJSONString.length(); i += 1){
				assertEquals(expectedJSONString.charAt(i), resultJSONString.charAt(i));
			}		
			
			// check the CSV results
			CSVDataset data = client.getTableResultsCSV(jobId, null);
			String expectedCSVString = "colA,colB,colC,colD\n\"apple,ant\",bench,\"\"\"cabana\"\"\",\"Dan declared \"\"hi, dear\"\"\"\n";  // validated by opening in Excel
			CSVDataset compare = new CSVDataset(expectedCSVString, true);			
			ArrayList<String> compareColumnNames = compare.getColumnNamesinOrder();
			ArrayList<String> resultColumnNames  = data.getColumnNamesinOrder();
			assertEquals(compareColumnNames.get(0), resultColumnNames.get(0));
			assertEquals(compareColumnNames.get(1), resultColumnNames.get(1));
			assertEquals(compareColumnNames.get(2), resultColumnNames.get(2));
			assertEquals(compareColumnNames.get(3), resultColumnNames.get(3));
			
		} finally {
			cleanup(client, jobId);
		}
	}
	
	@Test
	public void testStoreTable_WithBackslash() throws Exception {
		// try backslash and quotes
		//
		// PEC 9/28/17  I don't understand why 
		// 		this works in Java but not in the browser or python, which both choke on the backslash
		// 		this works in Java when the RestClient TableFormatter is fixing double quotes but not backslashes
		// 		we're allowed to put TableFormatter in ResultsClient.java
		
		String jobId = "test_jobid_" + UUID.randomUUID();
		
		String [] cols = {"colA"};
		String [] types = {"String"};
		ArrayList<String> row = new ArrayList<String>();
		row.add("\"Tor\" likes blue\\purple jello");  	
		
		try {
			Table table = new Table(cols, types, null);
			table.addRow(row);
			String tableJsonStrOrig = table.toJson().toJSONString();
			client.execStoreTableResults(jobId, table);	
			String tableJsonStrPostStore = table.toJson().toJSONString();
			
			// check the JSON results
			Table tbl = client.getTableResultsJson(jobId, null);
			String tableJsonStrRetrieved = tbl.toJson().toJSONString();

			System.out.println("jobId: " + jobId);
			System.out.println("json orig:      " + tableJsonStrOrig);
			System.out.println("json post_send: " + tableJsonStrPostStore);
			System.out.println("json retrieved: " + tableJsonStrRetrieved);
			
			assert(tableJsonStrRetrieved.equals(tableJsonStrOrig));

			
		} finally {
			cleanup(client, jobId);
		}
	}
	
	
	/**
	 * Test a row with quotes but no commas (in the past this triggered different logic in the ResultsClient)
	 */
	@Test
	public void testStoreTable_WithQuotesNoCommas() throws Exception {
		
		String jobId = "test_jobid_" + UUID.randomUUID();
		
		String [] cols = {"colA", "colB","colC","colD"};
		String [] types = {"String", "String", "String","String"};
		ArrayList<String> row = new ArrayList<String>();
		row.add("apple");  				
		row.add("bench");
		row.add("\"cabana\"");			// this element has internal quotes and no commas
		row.add("Dan declared \"hi\"");	// this element has internal quotes and no commas
		
		try {
			Table table = new Table(cols, types, null);
			table.addRow(row);
			client.execStoreTableResults(jobId, table);	
			
			// check the JSON results
			Table tbl = client.getTableResultsJson(jobId, null);
			String resultJSONString = tbl.toJson().toJSONString();
			String expectedJSONString = "{\"col_names\":[\"colA\",\"colB\",\"colC\",\"colD\"],\"rows\":[[\"apple\",\"bench\",\"\\\"cabana\\\"\",\"Dan declared \\\"hi\\\"\"]],\"type\":\"TABLE\",\"col_type\":[\"String\",\"String\",\"String\",\"String\"],\"col_count\":4,\"row_count\":1}";  // validated json		
			System.err.println(expectedJSONString);
			System.err.println(resultJSONString);
			
			assertEquals(expectedJSONString.length(), resultJSONString.length());
			
			for(int i = 0; i < expectedJSONString.length(); i += 1){
				assertEquals(expectedJSONString.charAt(i), resultJSONString.charAt(i));
			}
			
			// check the CSV results
			CSVDataset data = client.getTableResultsCSV(jobId, null);

			String expectedCSVString = "colA,colB,colC,colD\napple,bench,\"\"\"cabana\"\"\",\"Dan declared \"\"hi\"\"\"\n";  // validated by opening in Excel
			CSVDataset compare = new CSVDataset(expectedCSVString, true);
			
			
			ArrayList<String> compareColumnNames = compare.getColumnNamesinOrder();
			ArrayList<String> resultColumnNames  = data.getColumnNamesinOrder();
					
			assertEquals(compareColumnNames.get(0), resultColumnNames.get(0));
			assertEquals(compareColumnNames.get(1), resultColumnNames.get(1));
			assertEquals(compareColumnNames.get(2), resultColumnNames.get(2));
			assertEquals(compareColumnNames.get(3), resultColumnNames.get(3));
			
			
		} finally {
			cleanup(client, jobId);
		}
	}
	
	
	/**
	 * TODO: fix this comment.  SOH passes through 
	 * Test that SOH is stripped when writing results.
	 * If SOH is not stripped, the ResultsClient would succeed - but the browser would choke 
	 * (org.simple.JSONParser is less strict than Chrome/Firefox which uses the JSON standard)
	 */
	@Test
	public void testStoreTable_WithSOH() throws Exception {
		
		String jobId = "test_jobid_" + UUID.randomUUID();
		
		String [] cols = {"colA", "colB"};
		String [] types = {"String", "String"};
		ArrayList<String> row = new ArrayList<String>();
		row.add("apple\u0001ant");
		row.add("bench");

		try {
			Table table = new Table(cols, types, null);
			table.addRow(row);	
			client.execStoreTableResults(jobId, table);	
			
			// check the JSON results:   SOH comes back as an escape sequence
			Table tbl = client.getTableResultsJson(jobId, null);
			String resultJSONString = tbl.toJson().toJSONString();
			String expectedJSONString = "{\"col_names\":[\"colA\",\"colB\"],\"rows\":[[\"apple\\u0001ant\",\"bench\"]],\"type\":\"TABLE\",\"col_type\":[\"String\",\"String\"],\"col_count\":2,\"row_count\":1}";  // validated json
			assertEquals(expectedJSONString, resultJSONString);
	
			// check the CSV results:   SOH is in the string
			CSVDataset data = client.getTableResultsCSV(jobId, null);
			String expectedCSVString = "colA,colB\napple\u0001ant,bench\n";  
			CSVDataset compare = new CSVDataset(expectedCSVString, true);
			ArrayList<String> compareColumnNames = compare.getColumnNamesinOrder();
			ArrayList<String> resultColumnNames  = data.getColumnNamesinOrder();					
			assertEquals(compareColumnNames.get(0), resultColumnNames.get(0));
			assertEquals(compareColumnNames.get(1), resultColumnNames.get(1));
						
		} finally {
			cleanup(client, jobId);
		}
	}
	
	/**
	 * Test a storing a medium-size dataset
	 */
	@Test
	public void testStoreTable_Medium() throws Exception {

		String jobId = "test_jobid_" + UUID.randomUUID();
		
		try {
			long startTime = System.nanoTime();
			
			JSONObject jsonObj = Utility.getJSONObjectFromFilePath("src/test/resources/table9000.json");
			Table table = Table.fromJson(jsonObj);
			
			long endTime = System.nanoTime();
			double elapsed = ((endTime - startTime) / 1000000000.0);
			System.err.println(String.format(">>> Utility.getJSONEObjectFromFilePath()=%.2f sec", elapsed));

			// --- store results ----
			startTime = System.nanoTime();		
			client.execStoreTableResults(jobId, table);
			endTime = System.nanoTime();
			elapsed = ((endTime - startTime) / 1000000000.0);
			System.err.println(String.format(">>> client.execStoreTableResults()=%.2f sec", elapsed));
			
			// --- test results ---
			Table res = client.getTableResultsJson(jobId, 200);
			assertEquals(res.getNumRows(), 200);	
			
			// test that we got full 9000 rows of CSV 
			CSVDataset csv = client.getTableResultsCSV(jobId, 10000);
			csv.getColumnNamesinOrder().contains("comments");
			ArrayList<ArrayList<String>> lines = csv.getNextRecords(10000);
			assertEquals(9000, lines.size(), 9000);
			
		} finally {
			cleanup(client, jobId);
		}
	}
	
	/**
	 * Test a storing a large dataset
	 * Adjust NUM_COLS and NUM_ROWS to make this bigger when performance testing.  
	 * (Committing them on the smaller size)
	 */
	@Test
	public void testStoreTable_Huge() throws Exception {

		String jobId = "test_jobid_" + UUID.randomUUID();
		
		try {
			
			// construct a huge table
			final int NUM_COLS = 100;
			final int NUM_ROWS = 1000;
			String[] cols = new String[NUM_COLS];
			String[] colTypes = new String[NUM_COLS];
			ArrayList<String> row = new ArrayList<String>();
			for(int i = 0; i < NUM_COLS; i++){
				cols[i] = "col" + i;
				colTypes[i] = "String";
				row.add(UUID.randomUUID().toString().substring(0,10));	// non-repetitive to realistically test compression
			}
			Table table = new Table(cols, colTypes, null);
			for(int i = 0; i < NUM_ROWS; i++){
				table.addRow((ArrayList<String>) row.clone());  // need to clone, otherwise formatter recursively formats the same row
			}
			
			long startTime, endTime;
			double elapsed;
			
			// --- store results ----
			startTime = System.nanoTime();		
			client.execStoreTableResults(jobId, table);
			endTime = System.nanoTime();
			elapsed = ((endTime - startTime) / 1000000000.0);
			System.err.println(String.format(">>> client.execStoreTableResults()=%.2f sec (%s columns, %s rows)", elapsed, NUM_COLS, NUM_ROWS));
			
			// --- test retrieving json results ---
			startTime = System.nanoTime();
			Table res = client.getTableResultsJson(jobId, null);
			endTime = System.nanoTime();
			elapsed = ((endTime - startTime) / 1000000000.0);
			System.err.println(String.format(">>> client.execTableResultsJson()=%.2f sec (%s columns, %s rows)", elapsed, NUM_COLS, NUM_ROWS));
			assertEquals(res.getNumRows(), NUM_ROWS);
			assertEquals(res.getNumColumns(), NUM_COLS);
			assertEquals(res.getCell(0,0).length(), 10);
			
			// --- test retrieving csv results ---
			startTime = System.nanoTime();
			CSVDataset resultCsv = client.getTableResultsCSV(jobId, null);
			endTime = System.nanoTime();
			elapsed = ((endTime - startTime) / 1000000000.0);
			System.err.println(String.format(">>> client.execTableResultsCsv()=%.2f sec (%s columns, %s rows)", elapsed, NUM_COLS, NUM_ROWS));
		} catch (Exception e){
			e.printStackTrace();
			fail();
		} finally {
			cleanup(client, jobId);
		}
	}
		
	@Test
	public void test_delete() throws Exception {
		
		String jobId = "test_jobid_" + UUID.randomUUID();
		String [] cols = {"col1", "col2"};
		String [] types = {"String", "String"};
		ArrayList<String> row = new ArrayList<String>();
		row.add("one");
		row.add("two");
		
		try {			
			Table table = new Table(cols, types, null);
			table.addRow(row);
			table.addRow(row);
			
			client.execStoreTableResults(jobId, table);
			URL urls[] = client.execGetResults(jobId);				// this should succeed
			client.execDeleteJob(jobId);
			urls = client.execGetResults(jobId); 	// this should throw an exception, because the results are deleted
			
			fail();  // expect it to not get here
		} catch (Exception e) {
			// success			
		} 
		
		
	}

	@Test
	public void testStoreAndRetrieveBinaryFile() throws Exception {


		// Happy path
		File testFile = new File("src/test/resources/test.csv");
		File testFile2 = new File("src/test/resources/test2.csv");
		String jobId = "test_jobid_" + UUID.randomUUID();
		
		try {
			final String fileName = "test.csv";
			final String fileName2 = "test2.csv";
			SimpleResultSet res = client.execStoreBinaryFile(jobId, testFile);
			SimpleResultSet res2 = client.execStoreBinaryFile(jobId, testFile2);

			String fileId = (String) res.getResult("fileId");
			String fileContent = client.execReadBinaryFile(fileId);
			
			assertNotNull(fileContent);
			
			// check getting resultsURLs
			Table urlTab = client.getResultsFiles(jobId);
			assertEquals(2, urlTab.getNumRows());
			assertTrue(urlTab.getCell(0, "name").equals(fileName) || urlTab.getCell(1, "name").equals(fileName));  		// entries may appear in any order
			assertTrue(urlTab.getCell(0, "name").equals(fileName2) || urlTab.getCell(1, "name").equals(fileName2));  	// entries may appear in any order
		} finally {
			cleanup(client, jobId);
		}

	}
	
	@Test
	public void testBinaryFileSecurity() throws Exception {


		// Happy path
		File testFile = new File("src/test/resources/test.csv");
		File testFile2 = new File("src/test/resources/test2.csv");
		String jobId1 = "../../important.txt";
		String jobId2 = "${cd}my/$%var*)(/important.txt";
		try {
			// fail due to ".." in path
			SimpleResultSet res = client.execStoreBinaryFile(jobId1, testFile);
			fail("unexpected success with jobid=" + jobId1);
		} catch (Exception e) {
		}finally {
			cleanup(client, jobId1);
		}
		
		try {
			// fail due to wonky characters in path
			SimpleResultSet res = client.execStoreBinaryFile(jobId2, testFile);
			fail("unexpected success with jobid=" + jobId2);
		} catch (Exception e) {
		}finally {
			cleanup(client, jobId2);
		}
		
	}
	
	/**
	 * Don't know how to test success because
	 * how do we get a file onto the results service machine reliably?
	 * @throws Exception
	 */
	@Test
	public void testStoreBinaryFilePath() throws Exception {

		String path;
		String jobId = JobTracker.generateJobId();
		
		// try a non-white-listed file
		try {
			path = "C:\\Users\\200001934\\Desktop\\Temp\\working.txt";
			client.execStoreBinaryFilePath(jobId, path, "file1");
			fail("unexpected success with path=" + path);
		} catch (Exception e) {
			assertTrue(e.getMessage() + " does not contain 'white-listed'", e.getMessage().contains("white-listed"));
		}

		String fileLocation = IntegrationTestUtility.get("integrationtest.resultsservice.fileLocation");
		String additionalFileLocations = IntegrationTestUtility.get("integrationtest.resultsservice.additionalFileLocations");
		
		// try a white-listed file in fileLocation
		try {
			path = Paths.get(fileLocation, "test.txt").toString();
			client.execStoreBinaryFilePath(jobId, path, "file1");
			fail("unexpected success with path=" + path);
		} catch (Exception e) {
			// not readable error instead of white-list error
			assertTrue(e.getMessage() + " does not contain 'not readable'", e.getMessage().contains("not readable"));
		}
		
		// try white-listed file additionalFileLocations
		for (String loc : additionalFileLocations.split(",")) {
			try {
				path = Paths.get(loc, "test.txt").toString();
				client.execStoreBinaryFilePath(jobId, path, "file1");
				fail("unexpected success with path=" + path);
			} catch (Exception e) {
				// not readable error instead of white-list error
				assertTrue(e.getMessage() + " does not contain 'not readable'", e.getMessage().contains("not readable"));
			}
		}
		
		// there is no way to predict a successful location on the server
		// so leave "success" test to the fileStagingService Client test
	}
	
	@Test
	public void testAuthenticationFailures() throws Exception {
		// moved to AuthorizationTest_IT
	}
	@Test
	public void testStoreAndRetrieveWrongId() throws Exception {

		// Error case
		try {
			String fileContent = client.execReadBinaryFile("wrongId");
			fail("Missing exception for bad fileId");
		} catch (Exception e) {
		}
	}


	private void cleanup(ResultsClient client, String jobId) {
		try {
			client.execDeleteJob(jobId);
		} catch (Exception e) {
			System.err.println("cleanup failed: " + jobId);
		}
	}

}
