/**
 ** Copyright 2017 General Electric Company
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

package com.ge.research.semtk.load.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Hashtable;

import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.LocalLogger;

public class DataLoaderTest_IT {

	private static final String DELETE_URI_FMT = "delete { ?x ?y ?z.} where { ?x ?y ?z FILTER (?x = <%s> || ?z = <%s>).\n }";
	private static final String SELECT_URI_TRIPLES_FMT = "select distinct ?x ?y ?z where { ?x ?y ?z FILTER (?x = <%s> || ?z = <%s>).\n }";	
	private static final int DEFAULT_BATCH_SIZE = 32;
	@Test
	public void testOriginal() throws Exception {
		Dataset ds = new CSVDataset("src/test/resources/testTransforms.csv", false);

		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/testTransforms.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");

		// test
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			fail(err.toCSVString());
		}
		assertEquals(dl.getTotalRecordsProcessed(), 3);
	}	
	
	@Test
	public void testTransforms() throws Exception {
		// Paul
		// Test that a transform is applied properly to a column value but NOT
		// to the adjacent text

		// set up the data
		String contents = "cell,size in,lot,material,guy,treatment\n"
				+ "abcde_test,,,,,\n";
		Dataset ds = new CSVDataset(contents, true);

		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/testTransforms.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");

		// calculate expected uri after applying transform. Capitalize all the
		// "E"s in the column value but not the text "Cell_"
		String prefix = sgJson.getImportSpec().getUriPrefix();
		String uri = prefix + "Cell_abcdE_tEst";

		// delete uri if left over from previous tests
		deleteUri(uri);

		// import
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			fail(err.toCSVString());
		}

		// look for triples
		confirmUriExists(uri);
	}
	
	@Test
	public void testBadUri() throws Exception {
		// Paul
		// Test uri with spaces and <> to make sure it is escaped properly

		// set up the data
		String contents = "Battery,Cell,birthday,color\n"
				+ "<contains space and brackets>,cellA,01/01/1966,red\n";

		//String CORRECT = "Cell_%3Ccontains%20spacE%20and%20brackEts%3E";
		String owlPath = "src/test/resources/sampleBattery.owl";
		String jsonPath = "src/test/resources/sampleBattery.json";
		Dataset ds = new CSVDataset(contents, true);

		// get json
		TestGraph.clearGraph();
		TestGraph.uploadOwl(owlPath);
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile(jsonPath);

		// calculate expected uri after applying transform. Capitalize all the
		// "E"s in the column value but not the text "Cell_"
		//String prefix = sgJson.getImportSpec().getUriPrefix();

		// import
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());

		int records = dl.importData(true);
		assertTrue(records == 0);
		assertTrue(dl.getLoadingErrorReport().getRow(0).get(4).contains("ill-formed URI"));
	}	
	
	@Test
	public void testMessyString1() throws Exception {
		// Test string with \r and \n
		String uri = "testMessyString"; // avoid the uppercasing transform by using uppercase
		String pastelot = "This is a messy\n string \r\n line3\n";
		String contents = "cell,size in,lot,material,guy,treatment\n" + uri 	+ ",,\"" + pastelot + "\",,,\n";

		Dataset ds = new CSVDataset(contents, true);

		// get json
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");

		// calculate expected uri after applying transform. Capitalize all the "E"s in the column value but not the text "Cell_"
		String prefix = sgJson.getImportSpec().getUriPrefix();
		uri = prefix + "Cell_" + uri.replaceAll("e", "E");

		// delete uri if left over from previous tests
		deleteUri(uri);

		// import
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			fail(err.toCSVString());
		}

		// look for triples
		NodeGroup nodegroup = sgJson.getNodeGroup();
		constrainUri(nodegroup, "Cell", uri);
		returnProp(nodegroup, "Cell", "cellId");
		returnProp(nodegroup, "ScreenPrinting", "pasteLot");

		String query = nodegroup.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, null, null, false);

		Table tab = TestGraph.runQuery(query);
		if (tab.getNumRows() < 1) {
			throw new Exception("No triples were found for uri: " + uri);
		}

		// check first pasteLot
		String answer = tab.getRow(0).get(1);
		if (!answer.equals(pastelot)) {
			fail(String.format("Inserted wrong string: %s expecting %s ", answer, pastelot));
		}
	}
	
	
	@Test
	public void testMessyStringsInCsv() throws Exception {
		// Reads strings out of testStrings.csv
		//
		// Adding more test cases:
		// Fill in only the two columns already used
		// Make sure "cell" starts with "testStrings" and is unique
		// Then "lot" should be the string to be tested
		// get the csv
		Dataset ds = new CSVDataset("src/test/resources/testStrings.csv", false);
		CSVDataset csv = new CSVDataset("src/test/resources/testStrings.csv", false);

		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/testTransforms.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");

		// import
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			fail(err.toCSVString());
		}

		// get all the answers
		String query = "prefix testconfig:<http://research.ge.com/spcell#> \n"
				+ " \n" + "select distinct ?cellId ?pasteLot where { \n"
				+ "   ?Cell a testconfig:Cell. \n"
				+ "   ?Cell testconfig:cellId ?cellId . \n"
				+ "      FILTER regex(?cellId, \"^testStrings\") . \n" + " \n"
				+ "   ?Cell testconfig:screenPrinting ?ScreenPrinting. \n"
				+ "      ?ScreenPrinting testconfig:pasteLot ?pasteLot . \n"
				+ "}";

		Table tab = TestGraph.runQuery(query);
		if (tab.getNumRows() < 1) {
			throw new Exception("No triples were found for uri: "); // + uri);
		}

		// put all expected results into a hashtable
		Hashtable<String, String> correct = new Hashtable<String, String>();
		ArrayList<ArrayList<String>> rows = csv.getNextRecords(1000);
		for (int i = 0; i < rows.size(); i++) {
			correct.put(rows.get(i).get(0), rows.get(i).get(2));
		}

		// loop through actual results
		for (int i = 0; i < tab.getNumRows(); i++) {
			String key = tab.getRow(i).get(0);
			String actual = tab.getRow(i).get(1);
			String expect = correct.get(key);

			// these substitutions are expected
			expect = expect.replace("\\n", "\n");
			expect = expect.replace("\\t", "\t");

			if (!actual.equals(expect)) {
				fail(String.format(
						"Bad string retrieved.  Expecting '%s' Found '%s'",	expect, actual));
			}
		}
	}
	
	@Test
	public void testLoadData() throws Exception {
		// Pre changes:   19.5s 18.5s  18.64s  17.514
		// During changes:  
		// Bigger-ish test of many import spec features and timing
		Dataset ds = new CSVDataset("src/test/resources/loadTestData.csv", false);

		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/loadTest.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/loadTest.json");

		// import
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		
		LocalLogger.logToStdOut("Starting load");
		dl.importData(true);
		LocalLogger.logToStdOut("Finished with load");
		
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			LocalLogger.logToStdErr(err.toCSVString());
			fail();
		}

		TestGraph.queryAndCheckResults(sgJson.getNodeGroup(), this, "/loadTestResults.csv");
		
	}
	
	@Test
	public void testLoadDataDuraBattery() throws Exception {
		// Pre changes:   
		// During changes:  
		// Bigger-ish test of many import spec features and timing. 
		Dataset ds = new CSVDataset("src/test/resources/loadTestDuraBatteryData.csv", false);

		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/loadTestDuraBattery.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/loadTestDuraBattery.json");

		// import
		DataLoader dl = new DataLoader(sgJson, 8, ds, TestGraph.getUsername(), TestGraph.getPassword());
		
		LocalLogger.logToStdOut("Starting load");
		dl.importData(true);
		LocalLogger.logToStdOut("Finished with load");
		
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			LocalLogger.logToStdErr(err.toCSVString());
			fail();
		}

		TestGraph.queryAndCheckResults(sgJson.getNodeGroup(), this, "/loadTestDuraBatteryResults.csv");
		
	}
	
	@Test
	public void testLoadDataDuraBatteryEmptyCol() throws Exception {
		// Catch the error when a URI mappingItem column contains empty
		Dataset ds = new CSVDataset("src/test/resources/loadTestDuraBatteryData.csv", false);

		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/loadTestDuraBattery.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/loadTestDBattEmptyCol.json");

		// import
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		
		LocalLogger.logToStdOut("Starting load");
		dl.importData(true);
		LocalLogger.logToStdOut("Finished with load");
		
		Table err = dl.getLoadingErrorReport();
		assertEquals(1, err.getNumRows());
		assertTrue(err.toCSVString().contains("Empty values in"));
		assertTrue(err.toCSVString().contains("only colors"));
	}
	
	@Test
	public void testLookupBatteryIdAddDesc() throws Exception {
		// setup
		TestGraph.clearGraph();
				
		// ==== pre set some data =====
		TestGraph.uploadOwl("src/test/resources/loadTestDuraBattery.owl");
		SparqlGraphJson sgJson0 = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/loadTestDuraBattery.json");
		Dataset ds0 = new CSVDataset("src/test/resources/loadTestDuraBatteryData.csv", false);

		DataLoader dl0 = new DataLoader(sgJson0, DEFAULT_BATCH_SIZE, ds0, TestGraph.getUsername(), TestGraph.getPassword());
		dl0.importData(true);
		
		Table err0 = dl0.getLoadingErrorReport();
		if (err0.getNumRows() > 0) {
			fail(err0.toCSVString());
		}
				
				
		// Try URI lookup
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/lookupBatteryIdAddDesc.json");

		Dataset ds = new CSVDataset("src/test/resources/lookupBatteryIdAddDescData.csv", false);
		
		// import
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);


		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			LocalLogger.logToStdErr(err.toCSVString());
			fail();
		}
		
		TestGraph.queryAndCheckResults(sgJson.getNodeGroup(), this, "/lookupBatteryIdAddDescResults.csv");
		
	}
	
	@Test
	public void testLookupBatteryIdAddDescShort() throws Exception {
		// setup
		TestGraph.clearGraph();
				
		// ==== pre set some data =====
		TestGraph.uploadOwl("src/test/resources/loadTestDuraBattery.owl");
		SparqlGraphJson sgJson0 = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/loadTestDuraBattery.json");
		Dataset ds0 = new CSVDataset("src/test/resources/loadTestDuraBatteryShortData.csv", false);

		DataLoader dl0 = new DataLoader(sgJson0, DEFAULT_BATCH_SIZE, ds0, TestGraph.getUsername(), TestGraph.getPassword());
		dl0.importData(true);
		
		Table err0 = dl0.getLoadingErrorReport();
		if (err0.getNumRows() > 0) {
			fail(err0.toCSVString());
		}
				
		LocalLogger.logToStdErr("------ done import 1 -------");		
		// Try URI lookup
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/lookupBatteryIdAddDesc.json");

		Dataset ds = new CSVDataset("src/test/resources/lookupBatteryIdAddDescShortData.csv", false);
		
		// import the actual test: lookup URI and add description
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);


		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			LocalLogger.logToStdErr(err.toCSVString());
			fail();
		}
		
		TestGraph.queryAndCheckResults(sgJson.getNodeGroup(), this, "/lookupBatteryIdAddDescShortResults.csv");
		
	}
	
	@Test
	public void testLookupBatteryId_lookupFails() throws Exception {
		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/loadTestDuraBattery.owl");
	
		// Try URI lookup
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/lookupBatteryIdAddDesc.json");
		Dataset ds = new CSVDataset("src/test/resources/lookupBatteryIdAddDescShortData.csv", false);
		
		// import
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);


		Table err = dl.getLoadingErrorReport();
		assertEquals(20, err.getNumRows());		
		assertTrue(err.getRowAsCSVString(5).contains("URI lookup failed"));
	}
	
	@Test
	public void testLoadLookupFailTwoFound() throws Exception {
		
		// Lookup fails because two matching URI's are found
		Dataset ds = new CSVDataset("src/test/resources/loadTestDuraBatteryShortData.csv", false);

		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/loadTestDuraBattery.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/loadTestDuraBattery.json");

		// import durabattery twice.  
		// Puts two copies of each cell (GUID URI)  on each battery (mapped URI)
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			LocalLogger.logToStdErr(err.toCSVString());
			fail();
		}
		
		// second load
		dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			LocalLogger.logToStdErr(err.toCSVString());
			fail();
		}

		// the real test
		sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/lookupCellDuraBattery.json");
		// real test's load
		dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		err = dl.getLoadingErrorReport();
		
		LocalLogger.logToStdErr("Expecting 20 rows, found: \n" + err.toCSVString());
		
		assertEquals(20, err.getNumRows());
		TestGraph.compareResults(err.toCSVString(), this, "/loadTestLookupFailTwoFoundResults.csv");
		
	}
	
	@Test
	public void testLoadLookXNodes() throws Exception {
		// Look for a battery with three cells A1, B1, C1
		// Add a fourth new cell, D100
		// Tests lookup across SNode connections since it looks up a battery by three cell's ids.
		// It also tests that the lookup nodegroup is pruned properly
		Dataset ds = new CSVDataset("src/test/resources/loadTestDuraBatteryFirst4Data.csv", false);

		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/loadTestDuraBattery.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/loadTestDuraBattery.json");

		// import durabattery first4.  
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			LocalLogger.logToStdErr(err.toCSVString());
			fail();
		}

		// the real test
		sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/loadTestDuraBatteryLookXNodes.json");
		ds = new CSVDataset("src/test/resources/loadTestDuraBatteryLookXNodesData.csv", false);
		dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			LocalLogger.logToStdErr(err.toCSVString());
			fail();
		}

		TestGraph.queryAndCheckResults(sgJson.getNodeGroup(), this, "/loadTestDuraBatteryLookXNodesResults.csv");
	}
	
	@Test
	public void testLoadConnectNodes() throws Exception {
		// Connect cell will id "D" to cell4 of batteries with ids: "nocells" and "three"
		Dataset ds = new CSVDataset("src/test/resources/loadTestDuraBatteryFirst4Data.csv", false);

		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/loadTestDuraBattery.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/loadTestDuraBattery.json");

		// import durabattery first4.  
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			LocalLogger.logToStdErr(err.toCSVString());
			fail();
		}

		// the real test
		sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/lookupBatteryConnectNodes.json");
		ds = new CSVDataset("src/test/resources/lookupBatteryConnectNodesData.csv", false);
		dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			LocalLogger.logToStdErr(err.toCSVString());
			fail();
		}

		TestGraph.queryAndCheckResults(sgJson.getNodeGroup(), this, "/lookupBatteryConnectNodesResults.csv");
	}
	
	@Test
	public void testLookupByEnum() throws Exception {
		// lookup battery with cell1 and cell2 both blue, and change the assembly Date to 3/23/66 8:00
		Dataset ds = new CSVDataset("src/test/resources/loadTestDuraBatteryFirst4Data.csv", false);

		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/loadTestDuraBattery.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/loadTestDuraBattery.json");

		// import durabattery first4.  
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			LocalLogger.logToStdErr(err.toCSVString());
			fail();
		}
		
		// the real test 
		sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/loadTestLookupByEnum.json");
		ds = new CSVDataset("src/test/resources/loadTestLookupByEnumData.csv", false);
		dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			LocalLogger.logToStdErr(err.toCSVString());
			fail();
		}

		TestGraph.queryAndCheckResults(sgJson.getNodeGroup(), this, "/loadTestLookupByEnumResults.csv");
	}
	
	@Test
	public void testLookupByEnumBlanks() throws Exception {
		// try looking up on two enums but one is blank.  throw error
		Dataset ds = new CSVDataset("src/test/resources/loadTestDuraBatteryFirst4Data.csv", false);

		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/loadTestDuraBattery.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/loadTestDuraBattery.json");

		// import durabattery first4.  
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			LocalLogger.logToStdErr(err.toCSVString());
			fail();
		}
		
		// the real test  
		sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/loadTestLookupByEnum.json");
		ds = new CSVDataset("src/test/resources/loadTestLookupByEnumBlanksData.csv", false);
		dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		err = dl.getLoadingErrorReport();
		if (err.getNumRows() != 1) {
			LocalLogger.logToStdErr(err.toCSVString());
			fail();
		}

		assertTrue(err.toCSVString().contains("URI Lookup field is empty for node ?Color_2"));
	}
	
	@Test
	public void testLookupCreate() throws Exception {
		//  lookup Fails:    create 4 new batteries with assemglyDate, batteryDesc, batteryId
		//  lookup Succeeds: add assemblyDate to battery with batteryId="onlycolor"
		//  leave alone:     batteryId=three should remain un-changed
		Dataset ds = new CSVDataset("src/test/resources/loadTestDuraBatteryFirst4Data.csv", false);

		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/loadTestDuraBattery.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/loadTestDuraBattery.json");

		// import durabattery first4.  
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			LocalLogger.logToStdErr(err.toCSVString());
			fail();
		}
		
		// the real test  
		sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/loadTestLookupCreate.json");
		ds = new CSVDataset("src/test/resources/loadTestLookupCreateData.csv", false);
		dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		err = dl.getLoadingErrorReport();
		if (err.getNumRows() != 0) {
			LocalLogger.logToStdErr(err.toCSVString());
			fail();
		}

		TestGraph.queryAndCheckResults(sgJson.getNodeGroup(), this, "/loadTestLookupCreateResults.csv");
	}
	
	@Test
	public void testLookupCreatePartial() throws Exception {
		Dataset ds = new CSVDataset("src/test/resources/loadTestDuraBatteryFirst4Data.csv", false);

		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/loadTestDuraBattery.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/loadTestDuraBattery.json");

		// import durabattery first4.  
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			LocalLogger.logToStdErr(err.toCSVString());
			fail();
		}
		
		// the real test  
		sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/loadTestLookupCreatePartial.json");
		ds = new CSVDataset("src/test/resources/loadTestLookupCreatePartialData.csv", false);
		dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		err = dl.getLoadingErrorReport();
		if (err.getNumRows() != 0) {
			LocalLogger.logToStdErr(err.toCSVString());
			fail();
		}
		
		// check that results match expected in csv
		TestGraph.queryAndCheckResults(sgJson.getNodeGroup(), this, "/loadTestLookupCreatePartialResults.csv");
		
		// Check that no new cells were created
		String query = "prefix generateSparqlInsert:<belmont/generateSparqlInsert#>\r\n" + 
				"prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#>\r\n" + 
				"prefix durabattery:<http://kdl.ge.com/durabattery#>\r\n" + 
				"SELECT (COUNT(*) as ?count) { \r\n" + 
				"select distinct ?Cell ?cellId where {\r\n" + 
				"	?Cell a durabattery:Cell .\r\n" + 
				"	?Cell durabattery:cellId ?cellId .\r\n" + 
				"}\r\n" + 
				"\r\n" + 
				"}";
		Table tab = TestGraph.runQuery(query);
		assertEquals(7, tab.getCellAsInt(0, 0));
	}
	@Test
	public void testCaseTransform() throws Exception {
		// Paul
		// Test string with \r and \n

		String uri = "testCaseTransform";
		String guy = "fred";
		String stuff = "STUFF";
		// set up the data
		String contents = "cell,size in,lot,material,guy,treatment\n" + String.format("%s,,,%s,%s,", uri, stuff, guy);

		Dataset ds = new CSVDataset(contents, true);

		// setup
		TestGraph.clearGraph();
		TestGraph.uploadOwl("src/test/resources/testTransforms.owl");
		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile("src/test/resources/testTransforms.json");

		// calculate expected uri after applying transform. Capitalize all the
		// "E"s in the column value but not the text "Cell_"
		String prefix = sgJson.getImportSpec().getUriPrefix();
		uri = prefix + "Cell_" + uri.replaceAll("e", "E");


		// import
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			fail(err.toCSVString());
		}

		// look for triples

		NodeGroup nodegroup = sgJson.getNodeGroup();
		returnProp(nodegroup, "Cell", "cellId");
		returnProp(nodegroup, "ScreenPrinting", "pasteVendor");
		returnProp(nodegroup, "ScreenPrinting", "pasteMaterial");
		constrainUri(nodegroup, "Cell", uri);

		String query = nodegroup.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, null, null, false);

		Table tab = TestGraph.runQuery(query);
		if (tab.getNumRows() < 1) {
			throw new Exception("No triples were found for uri: " + uri);
		}

		// check first pasteVendor
		String answer;
		guy = guy.toUpperCase();
		stuff = stuff.toLowerCase();
		answer = tab.getRow(0).get(tab.getColumnIndex("pasteVendor"));
		if (!answer.equals(guy)) {
			fail(String.format("Inserted wrong string: %s expecting %s ", answer, guy));
		}
		answer = tab.getRow(0).get(tab.getColumnIndex("pasteMaterial"));
		if (!answer.equals(stuff)) {
			fail(String.format("Inserted wrong string: %s expecting %s ", answer, stuff));
		}
	}	
	
	
	@Test
	public void testGraphLoadBattery() throws Exception {
		SparqlGraphJson sgJson = TestGraph.initGraphWithData("sampleBattery");
		CSVDataset csvDataset = new CSVDataset("src/test/resources/sampleBattery.csv", false);
		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, csvDataset, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		assertEquals(dl.getTotalRecordsProcessed(), 4);
	}
	

	@Test
	public void testGraphLoadBadEnum() throws Exception {
		String csvPath = "src/test/resources/sampleBatteryBadColor.csv";
		String jsonPath = "src/test/resources/sampleBattery.json";
		String owlPath = "src/test/resources/sampleBattery.owl";

		SparqlGraphJson sgJson = TestGraph.getSparqlGraphJsonFromFile(jsonPath);
		CSVDataset csvDataset = new CSVDataset(csvPath, false);
		TestGraph.clearGraph();
		TestGraph.uploadOwl(owlPath);

		DataLoader dl = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, csvDataset, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		String report = dl.getLoadingErrorReportBrief();

		// make sure there is a complaint about the bad color
		assertTrue(report.contains("whiteish-yellow"));
		assertTrue(dl.getTotalRecordsProcessed() == 0);
	}
	
	
	@Test
	public void test_TEMPORARY() throws Exception {
		// This test will load up to two owls
		// then a csv dataset
		// and ingest up to three templates against the csv
		String csvPath = "";
		String jsonPath1 = "";
		String jsonPath2 = "";
		String jsonPath3 = "";
		String owlPath1 = "";
		String owlPath2 = "";
		
		if (csvPath.isEmpty() || owlPath1.isEmpty()) {
			return;
		}
		
		// load csv
		CSVDataset csvDataset = new CSVDataset(csvPath, false);
		
		// owl1
		TestGraph.clearGraph();
		TestGraph.uploadOwl(owlPath1);
		
		// owl2
		if (!owlPath2.isEmpty()) {
			TestGraph.uploadOwl(owlPath2);
		}
		
		SparqlGraphJson sgJson = null;
		
        // load data 1
		if (!jsonPath1.isEmpty()) {
			sgJson = TestGraph.getSparqlGraphJsonFromFile(jsonPath1);
			DataLoader dl1 = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, csvDataset, TestGraph.getUsername(), TestGraph.getPassword());
			dl1.importData(true);
			
			Table err = dl1.getLoadingErrorReport();
			if (err.getNumRows() > 0) {
				LocalLogger.logToStdErr(err.toCSVString());
				fail();
			}
		}
		
        // load data 2
		if (!jsonPath2.isEmpty()) {
	
			sgJson = TestGraph.getSparqlGraphJsonFromFile(jsonPath2);
			csvDataset.reset();
			DataLoader dl2 = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, csvDataset, TestGraph.getUsername(), TestGraph.getPassword());
			dl2.importData(true);
			
			Table err = dl2.getLoadingErrorReport();
			if (err.getNumRows() > 0) {
				LocalLogger.logToStdErr(err.toCSVString());
				fail();
			}
		}
			
		// load data 3
		if (!jsonPath3.isEmpty()) {

			sgJson = TestGraph.getSparqlGraphJsonFromFile(jsonPath3);
			csvDataset.reset();
			DataLoader dl3 = new DataLoader(sgJson, DEFAULT_BATCH_SIZE, csvDataset, TestGraph.getUsername(), TestGraph.getPassword());
			dl3.importData(true);
			
			Table err = dl3.getLoadingErrorReport();
			if (err.getNumRows() > 0) {
				LocalLogger.logToStdErr(err.toCSVString());
				fail();
			}
		}
		
	}
	
	
	
	
	
	// TODO: fails if we don't set SparqlID
	// TODO: fails if SparqlID doesn't start with "?"
	private void returnProp(NodeGroup nodegroup, String node, String prop) {
		PropertyItem cellId = nodegroup.getNodeBySparqlID("?" + node).getPropertyByKeyname(prop);
		cellId.setSparqlID("?" + prop);
		cellId.setIsReturned(true);
	}

	private void constrainUri(NodeGroup nodegroup, String nodeUri, String constraintUri) {
		Node node = nodegroup.getNodeBySparqlID("?" + nodeUri);
		ValueConstraint v = new ValueConstraint(String.format("VALUES ?Cell { <%s> } ", constraintUri));
		node.setValueConstraint(v);
	}

	private void runDeleteQuery(String query) throws Exception {
		SparqlEndpointInterface sei = TestGraph.getSei();
		TableResultSet res = (TableResultSet) sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
		if (!res.getSuccess()) {
			throw new Exception("Failure running delete query: \n" + query + "\n rationale: " + res.getRationaleAsString(" "));
		}
	}

	private void deleteUri(String uri) throws Exception {
		String query = String.format(DELETE_URI_FMT, uri, uri);
		SparqlEndpointInterface sei = TestGraph.getSei();
		TableResultSet res = (TableResultSet) sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
		if (!res.getSuccess()) {
			throw new Exception("Failure deleting uri: " + uri + " rationale: "	+ res.getRationaleAsString(" "));
		}
	}

	private void confirmUriExists(String uri) throws Exception {
		String query = String.format(SELECT_URI_TRIPLES_FMT, uri, uri);
		SparqlEndpointInterface sei = TestGraph.getSei();
		TableResultSet res = (TableResultSet) sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
		if (!res.getSuccess()) {
			throw new Exception("Failure querying uri: " + uri + " rationale: "	+ res.getRationaleAsString(" "));
		}
		if (res.getTable().getNumRows() < 1) {
			throw new Exception("No triples were found for uri: " + uri);
		}
	}	
	
}
