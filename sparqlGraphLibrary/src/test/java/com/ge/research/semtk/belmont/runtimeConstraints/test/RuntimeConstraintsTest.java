package com.ge.research.semtk.belmont.runtimeConstraints.test;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;

import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstrainedItems;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstrainedObject;
import com.ge.research.semtk.belmont.runtimeConstraints.SupportedOperations;
import com.ge.research.semtk.load.utility.SparqlGraphJson;

public class RuntimeConstraintsTest {

	public static final String testJsonString = "{ \"sparqlConn\": { \"name\": \"pop music test\", \"type\": \"virtuoso\", \"dsURL\": \"http://fakeserver:2420\", \"dsKsURL\": \"\", \"dsDataset\": \"http://research.ge.com/test/popmusic/data\", \"domain\": \"http://\", \"onDataset\": \"http://research.ge.com/test/popmusic/model\" }, \"sNodeGroup\": { \"version\": 1, \"sNodeList\": [ { \"propList\": [ { \"KeyName\": \"name\", \"ValueType\": \"string\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#string\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#name\", \"Constraints\": \"FILTER regex(%id, \\\"the Beatles\\\")\", \"fullURIName\": \"\", \"SparqlID\": \"?name_0\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] } ], \"nodeList\": [ { \"SnodeSparqlIDs\": [], \"KeyName\": \"member\", \"ValueType\": \"Artist\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"ConnectBy\": \"\", \"Connected\": false, \"UriConnectBy\": \"\", \"isOptional\": 0 } ], \"NodeName\": \"Band\", \"fullURIName\": \"http://com.ge.research/knowledge/test/popMusic#Band\", \"subClassNames\": [], \"SparqlID\": \"?OriginalBand\", \"isReturned\": true, \"isRuntimeConstrained\": false, \"valueConstraint\": \"\", \"instanceValue\": null }, { \"propList\": [ { \"KeyName\": \"songTitle\", \"ValueType\": \"string\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#string\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#songTitle\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"?songTitle\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] } ], \"nodeList\": [ { \"SnodeSparqlIDs\": [], \"KeyName\": \"composer\", \"ValueType\": \"Artist\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"ConnectBy\": \"\", \"Connected\": false, \"UriConnectBy\": \"\", \"isOptional\": 0 }, { \"SnodeSparqlIDs\": [ \"?OriginalBand\" ], \"KeyName\": \"originalArtrist\", \"ValueType\": \"Artist\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"ConnectBy\": \"originalArtrist\", \"Connected\": true, \"UriConnectBy\": \"http://com.ge.research/knowledge/test/popMusic#originalArtrist\", \"isOptional\": 0 } ], \"NodeName\": \"Song\", \"fullURIName\": \"http://com.ge.research/knowledge/test/popMusic#Song\", \"subClassNames\": [], \"SparqlID\": \"?Song\", \"isReturned\": false, \"isRuntimeConstrained\": false, \"valueConstraint\": \"\", \"instanceValue\": null }, { \"propList\": [ { \"KeyName\": \"durationInSeconds\", \"ValueType\": \"int\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#int\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#durationInSeconds\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"?durationInSeconds\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": true, \"instanceValues\": [] }, { \"KeyName\": \"recordingDate\", \"ValueType\": \"date\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#date\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#recordingDate\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"\", \"isReturned\": false, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] }, { \"KeyName\": \"trackNumber\", \"ValueType\": \"int\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#int\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#trackNumber\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"\", \"isReturned\": false, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] } ], \"nodeList\": [ { \"SnodeSparqlIDs\": [], \"KeyName\": \"recordingArtist\", \"ValueType\": \"Artist\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"ConnectBy\": \"\", \"Connected\": false, \"UriConnectBy\": \"\", \"isOptional\": 0 }, { \"SnodeSparqlIDs\": [ \"?Song\" ], \"KeyName\": \"song\", \"ValueType\": \"Song\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Song\", \"ConnectBy\": \"song\", \"Connected\": true, \"UriConnectBy\": \"http://com.ge.research/knowledge/test/popMusic#song\", \"isOptional\": 0 } ], \"NodeName\": \"AlbumTrack\", \"fullURIName\": \"http://com.ge.research/knowledge/test/popMusic#AlbumTrack\", \"subClassNames\": [], \"SparqlID\": \"?AlbumTrack\", \"isReturned\": false, \"isRuntimeConstrained\": false, \"valueConstraint\": \"\", \"instanceValue\": null }, { \"propList\": [ { \"KeyName\": \"style\", \"ValueType\": \"string\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#string\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#style\", \"Constraints\": \"FILTER regex(%id, \\\"Rock and Roll\\\")\", \"fullURIName\": \"\", \"SparqlID\": \"?style\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] } ], \"nodeList\": [], \"NodeName\": \"Genre\", \"fullURIName\": \"http://com.ge.research/knowledge/test/popMusic#Genre\", \"subClassNames\": [], \"SparqlID\": \"?Genre\", \"isReturned\": false, \"isRuntimeConstrained\": false, \"valueConstraint\": \"\", \"instanceValue\": null }, { \"propList\": [ { \"KeyName\": \"name\", \"ValueType\": \"string\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#string\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#name\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"?name\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": true, \"instanceValues\": [] } ], \"nodeList\": [ { \"SnodeSparqlIDs\": [], \"KeyName\": \"member\", \"ValueType\": \"Artist\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"ConnectBy\": \"\", \"Connected\": false, \"UriConnectBy\": \"\", \"isOptional\": 0 } ], \"NodeName\": \"Band\", \"fullURIName\": \"http://com.ge.research/knowledge/test/popMusic#Band\", \"subClassNames\": [], \"SparqlID\": \"?RelaeaseBand\", \"isReturned\": true, \"isRuntimeConstrained\": false, \"valueConstraint\": \"\", \"instanceValue\": null }, { \"propList\": [ { \"KeyName\": \"albumTtitle\", \"ValueType\": \"string\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#string\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#albumTtitle\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"?albumTtitle\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] }, { \"KeyName\": \"releaseDate\", \"ValueType\": \"date\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#date\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#releaseDate\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"\", \"isReturned\": false, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] } ], \"nodeList\": [ { \"SnodeSparqlIDs\": [ \"?RelaeaseBand\" ], \"KeyName\": \"band\", \"ValueType\": \"Band\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Band\", \"ConnectBy\": \"band\", \"Connected\": true, \"UriConnectBy\": \"http://com.ge.research/knowledge/test/popMusic#band\", \"isOptional\": 0 }, { \"SnodeSparqlIDs\": [ \"?Genre\" ], \"KeyName\": \"genre\", \"ValueType\": \"Genre\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Genre\", \"ConnectBy\": \"genre\", \"Connected\": true, \"UriConnectBy\": \"http://com.ge.research/knowledge/test/popMusic#genre\", \"isOptional\": 0 }, { \"SnodeSparqlIDs\": [], \"KeyName\": \"producer\", \"ValueType\": \"Artist\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"ConnectBy\": \"producer\", \"Connected\": false, \"UriConnectBy\": \"http://com.ge.research/knowledge/test/popMusic#producer\", \"isOptional\": 0 }, { \"SnodeSparqlIDs\": [ \"?AlbumTrack\" ], \"KeyName\": \"track\", \"ValueType\": \"AlbumTrack\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#AlbumTrack\", \"ConnectBy\": \"track\", \"Connected\": true, \"UriConnectBy\": \"http://com.ge.research/knowledge/test/popMusic#track\", \"isOptional\": 0 } ], \"NodeName\": \"Album\", \"fullURIName\": \"http://com.ge.research/knowledge/test/popMusic#Album\", \"subClassNames\": [], \"SparqlID\": \"?Album\", \"isReturned\": false, \"isRuntimeConstrained\": false, \"valueConstraint\": \"\", \"instanceValue\": null } ] }, \"importSpec\": { \"version\": \"1\", \"baseURI\": \"\", \"columns\": [], \"texts\": [], \"transforms\": [], \"nodes\": [] }, \"RuntimeConstraints\" :[ { \"SparqlID\" : \"?durationInSeconds\", \"Operator\" : \"GREATERTHANOREQUALS\", \"Operands\" : [ 300 ] }, { \"SparqlID\" : \"?name\", \"Operator\" : \"MATCHES\", \"Operands\" : [ \"AFI\" ] } ] }";
	public NodeGroup ng;
	public JSONObject nodeGroupJSON = null;
	public JSONArray runtimeConstraintsJSON = null;
	
	private void setup() throws Exception{
		SparqlGraphJson sparqlGraphJson = new SparqlGraphJson(testJsonString);
		ng = sparqlGraphJson.getNodeGroup();	
		runtimeConstraintsJSON = sparqlGraphJson.getRuntimeConstraintsJson();
	}
	
	@Test 
	public void getRuntimeConstraintsFromNodeGroup() throws Exception{
		this.setup();
		
		// actually try getting the rt constraints. 
		HashMap<String, RuntimeConstrainedObject> retval = this.ng.getConstrainedItems();
		
		// check for the actual values we expect
		
		if( retval.containsKey("?durationInSeconds") && retval.containsKey("?name")){
			// do nothing
			System.err.println("getRuntimeConstraintsFromNodeGroup() :: Both expected runtime constraints found: ( ?durationInSeconds & ?name )" );
		}
		else{
			fail();
		}		
	}
	
	@Test
	public void getRuntimeConstraintsFromJson() throws Exception{
		// simple test to confirm given json format is as expected. not a funcitonal code test.
		setup();
		
		// check size
		if(runtimeConstraintsJSON.size() == 2){
			System.err.println("getRuntimeConstraintsFromJson() :: Both expected runtime constraints found" );			
		}
		else{ fail(); }
		//check sparqlIDs
		int count = 0;
		for(Object jCurr : runtimeConstraintsJSON){
			JSONObject inUse = (JSONObject) jCurr;
			
			if(count == 0){
				if(inUse.get("SparqlID").toString().equals("?durationInSeconds") && inUse.get("Operator").toString().equals("GREATERTHANOREQUALS")){
					System.err.println("getRuntimeConstraintsFromJson() :: first constraint sparqlId and operation as expected" );		
				}
				else{
					fail();
				}
			}
			else{
				if(inUse.get("SparqlID").toString().equals("?name") && inUse.get("Operator").toString().equals("MATCHES")){
					System.err.println("getRuntimeConstraintsFromJson() :: second constraint sparqlId and operation as expected" );		
				}
				else{
					fail();
				}
			}
			count++;
		}
	}
	
	@Test
	public void attemptOverrideUsingRuntimeConstraints() throws Exception{
		// check that runtime constraints are applied to nodegroup without generating an exception.
		this.setup();
		
		RuntimeConstrainedItems rtci = new RuntimeConstrainedItems(ng);
		rtci.applyConstraintJson(runtimeConstraintsJSON);
		
		// if we got this far, let's check the constraints.
		
		// check the ?name constraint.
		String c0 = rtci.getValueConstraint("?name").getConstraint();
		String c1 = rtci.getValueConstraint("?durationInSeconds").getConstraint();
		
		if( c0.equals("VALUES ?name { 'AFI'^^<http://www.w3.org/2001/XMLSchema#string>  }")){
			System.err.println("attemptOverrideUsingRuntimeConstraints() :: constraint for ?name was as expected") ;
		}
		else{ fail(); }
		
		if( c1.equals("FILTER (?durationInSeconds >= 300.0)")){
			System.err.println("attemptOverrideUsingRuntimeConstraints() :: constraint for ?durationInSeconds was as expected") ;
		}
		else{ fail(); }
	}
	
	@Test 
	public void getSparqlUsingRuntimeConstraints() throws Exception{
		// check that the generated sparql select contains the expected constraints.
		this.setup();
		
		RuntimeConstrainedItems rtci = new RuntimeConstrainedItems(ng);
		rtci.applyConstraintJson(runtimeConstraintsJSON);
		
		// check for the constraints in generated sparql.
		String sparql = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 10000000, null);
		
		if(sparql.contains("VALUES ?name { 'AFI'^^<http://www.w3.org/2001/XMLSchema#string>  }")){
			System.err.println("getSparqlUsingRuntimeConstraints() :: constraint for ?name was as expected") ;
				
		}
		else{ fail(); }
		
		if(sparql.contains("FILTER (?durationInSeconds >= 300.0)")){
			System.err.println("getSparqlUsingRuntimeConstraints() :: constraint for ?durationInSeconds was as expected") ;
				
		}
		else{ fail(); }		
	}
	
	// easy functional tests after this point. these are mostly to make sure we have good coverage
	
	@Test
	public void testContraintsSetByType() throws Exception{
		setup();
		
		// alter the node group for more RT constraints so we have good coverage.
		// there is already a numeric rtc and a string rtc. we need date and URI for basic coverage.
		
		// add a constraint to releaseDate in album.
		ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").setIsRuntimeConstrained(true);
		ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").setSparqlID("?releaseDate"); // otherwise, no sparqlID
		ng.getNodeBySparqlID("?Album").setIsRuntimeConstrained(true);
		
		// build the runtimeConstrainedItems so we get the new ones.
		RuntimeConstrainedItems rtci = new RuntimeConstrainedItems(ng);
		
		// check all constraints listed.
		if( rtci.getConstrainedItemIds().size() == 4 ){
			System.err.println("testContraintsSetByType() :: expected number of runtime constraints found (4)") ;
			int x = 1;
			for(String i :  rtci.getConstrainedItemIds()){
				System.err.println("testContraintsSetByType() :: " +  i + " (" +  x + ")") ;
				x++;
			}
		}
		else{ fail(); }
		
		
		// attempt to set various constraints directly. 
		ArrayList<String> names = new ArrayList<String>();
		
		// numeric
			names.add("23");
			names.add("37");
		
			// set matches
			rtci.selectAndSetConstraint("?durationInSeconds", SupportedOperations.MATCHES.name(), ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueType(), names);

			// check matches
			if(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint().getConstraint().equals("VALUES ?durationInSeconds { '23'^^<http://www.w3.org/2001/XMLSchema#int> '37'^^<http://www.w3.org/2001/XMLSchema#int>  }")){
				System.err.println("testContraintsSetByType() :: expected value for ?durationInSeconds uri with Matches clause") ;
			}
			else{ 
				System.err.println(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint());
				fail(); }
				
			// set gt
			rtci.selectAndSetConstraint("?durationInSeconds", SupportedOperations.GREATERTHAN.name(), ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueType(), names);
		
			// check gt
			if(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint().getConstraint().equals("FILTER (?durationInSeconds > 23.0)")){
				System.err.println("testContraintsSetByType() :: expected value for ?durationInSeconds uri with GreaterThan clause") ;
			}
			else{ 
				System.err.println(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint());
				fail(); }
		
			// set lt
			rtci.selectAndSetConstraint("?durationInSeconds", SupportedOperations.LESSTHAN.name(), ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueType(), names);
					
			// check lt
			if(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint().getConstraint().equals("FILTER (?durationInSeconds < 23.0)")){
				System.err.println("testContraintsSetByType() :: expected value for ?durationInSeconds uri with LessThan clause") ;
			}
			else{ 
				System.err.println(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint());
				fail(); }
			
			// set gte
				rtci.selectAndSetConstraint("?durationInSeconds", SupportedOperations.GREATERTHANOREQUALS.name(), ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueType(), names);
					
			// check gte
				if(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint().getConstraint().equals("FILTER (?durationInSeconds >= 23.0)")){
					System.err.println("testContraintsSetByType() :: expected value for ?durationInSeconds uri with GreaterThanOrEquals clause") ;
				}
				else{ 
					System.err.println(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint());
					fail(); }
		
			// set lte
				rtci.selectAndSetConstraint("?durationInSeconds", SupportedOperations.LESSTHANOREQUALS.name(), ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueType(), names);
						
			// check lte
				if(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint().getConstraint().equals("FILTER (?durationInSeconds <= 23.0)")){
					System.err.println("testContraintsSetByType() :: expected value for ?durationInSeconds uri with LessThanOrEquals clause") ;
				}
				else{ 
					System.err.println(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint());
					fail(); }
				
			// set value between
				rtci.selectAndSetConstraint("?durationInSeconds", SupportedOperations.VALUEBETWEEN.name(), ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueType(), names);
				
			// check value between
				if(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint().getConstraint().equals("FILTER ( ?durationInSeconds >= 23.0  &&  ?durationInSeconds <= 37.0 )")){
					System.err.println("testContraintsSetByType() :: expected value for ?durationInSeconds uri with ValueBetween clause") ;
				}
				else{ 
					System.err.println(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint());
					fail(); }
				
			// set value between uninclusive
				rtci.selectAndSetConstraint("?durationInSeconds", SupportedOperations.VALUEBETWEENUNINCLUSIVE.name(), ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueType(), names);
		
			// check value between uniclusive
				if(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint().getConstraint().equals("FILTER ( ?durationInSeconds > 23.0  &&  ?durationInSeconds < 37.0 )")){
					System.err.println("testContraintsSetByType() :: expected value for ?durationInSeconds uri with ValueBetweenUninclusive clause") ;
				}
				else{ 
					System.err.println(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint());
					fail(); }
		
		// date
			names = new ArrayList<String>();	
			names.add("2017-01-17T00:00");  // 2014-05-23T10:20:13+05:30
			names.add("1955-11-05T22:04");
				
			// set matches
				rtci.selectAndSetConstraint("?releaseDate", SupportedOperations.MATCHES.name(), ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueType(), names);
			// check matches
				if(ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint().getConstraint().equals("VALUES ?releaseDate { '2017-01-17T00:00'^^<http://www.w3.org/2001/XMLSchema#date> '1955-11-05T22:04'^^<http://www.w3.org/2001/XMLSchema#date>  }")){
					System.err.println("testContraintsSetByType() :: expected value for ?releaseDate uri with Matches clause") ;
				}
				else{ 
					System.err.println(ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint());
					fail(); }
			// check greater than
				rtci.selectAndSetConstraint("?releaseDate", SupportedOperations.GREATERTHAN.name(), ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueType(), names);
				if(ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint().getConstraint().equals("FILTER (?releaseDate > 2017-01-17T00:00^^<http://www.w3.org/2001/XMLSchema#date>)")){
					System.err.println("testContraintsSetByType() :: expected value for ?releaseDate uri with greater than clause") ;
				}
				else{ 
					System.err.println("greater : "  + ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint());
					fail(); }
			// check less than
				rtci.selectAndSetConstraint("?releaseDate", SupportedOperations.LESSTHAN.name(), ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueType(), names);
				if(ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint().getConstraint().equals("FILTER (?releaseDate < 2017-01-17T00:00^^<http://www.w3.org/2001/XMLSchema#date>)")){
					System.err.println("testContraintsSetByType() :: expected value for ?releaseDate uri with less than clause") ;
				}
				else{ 
					System.err.println("lesser : "  + ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint());
					fail(); }
			// check interval
				rtci.selectAndSetConstraint("?releaseDate", SupportedOperations.VALUEBETWEEN.name(), ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueType(), names);
				if(ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint().getConstraint().equals("FILTER ( ?releaseDate >= 2017-01-17T00:00^^<http://www.w3.org/2001/XMLSchema#date>  &&  ?releaseDate <= 1955-11-05T22:04^^<http://www.w3.org/2001/XMLSchema#date> )")){
					System.err.println("testContraintsSetByType() :: expected value for ?releaseDate uri with interval clause") ;
				}
				else{ 
					System.err.println("between : "  + ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint());
					fail(); }
				rtci.selectAndSetConstraint("?releaseDate", SupportedOperations.VALUEBETWEENUNINCLUSIVE.name(), ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueType(), names);
				if(ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint().getConstraint().equals("FILTER ( ?releaseDate > 2017-01-17T00:00^^<http://www.w3.org/2001/XMLSchema#date>  &&  ?releaseDate < 1955-11-05T22:04^^<http://www.w3.org/2001/XMLSchema#date> )")){
					System.err.println("testContraintsSetByType() :: expected value for ?releaseDate uri with interval clause") ;
				}
				else{ 
					System.err.println("between : "  + ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint());
					fail(); }

				
		// string

			names = new ArrayList<String>();
			names.add("30 seconds to mars");
			names.add("parabelle");
			names.add("soundgarden");

			// set matches
			rtci.selectAndSetConstraint("?name", SupportedOperations.MATCHES.name(), ng.getNodeBySparqlID("?RelaeaseBand").getPropertyByKeyname("name").getValueType(), names);
		
			// check matches
			if(ng.getNodeBySparqlID("?RelaeaseBand").getPropertyByKeyname("name").getValueConstraint().getConstraint().equals("VALUES ?name { '30 seconds to mars'^^<http://www.w3.org/2001/XMLSchema#string> 'parabelle'^^<http://www.w3.org/2001/XMLSchema#string> 'soundgarden'^^<http://www.w3.org/2001/XMLSchema#string>  }")){
				System.err.println("testContraintsSetByType() :: expected value for ?name uri with Matches clause") ;
			}
			else{ 
				System.err.println(ng.getNodeBySparqlID("?RelaeaseBand").getPropertyByKeyname("name").getValueConstraint());
				fail(); }
			
			// set regex
			rtci.selectAndSetConstraint("?name", SupportedOperations.REGEX.name(), ng.getNodeBySparqlID("?RelaeaseBand").getPropertyByKeyname("name").getValueType(), names);
		
			// check regex
			if(ng.getNodeBySparqlID("?RelaeaseBand").getPropertyByKeyname("name").getValueConstraint().getConstraint().equals("FILTER regex(?name ,\"30 seconds to mars\")")){
				System.err.println("testContraintsSetByType() :: expected value for ?name uri with Regex clause") ;
			}
			else{ 
				System.err.println(ng.getNodeBySparqlID("?RelaeaseBand").getPropertyByKeyname("name").getValueConstraint());
				fail(); }
			
			
		// NODE_URI


			// no prefix, no angle brackets.
			names = new ArrayList<String>();
			names.add("test://music#decemberunderground");

			// set matches
			rtci.selectAndSetConstraint("?Album", SupportedOperations.MATCHES.name(), ng.getNodeBySparqlID("?Album").getValueType(), names);
			// check matches
			if(ng.getNodeBySparqlID("?Album").getValueConstraintStr().equals("VALUES ?Album { <test://music#decemberunderground>  }")){
				System.err.println("testContraintsSetByType() :: expected value for ?Album uri with Matches clause (no angle brackets)") ;
			}
			else { fail(); }

			// no prefix, angle brackets included
			names = new ArrayList<String>();
			names.add("<test://music#decemberunderground>");

			// set matches
			rtci.selectAndSetConstraint("?Album", SupportedOperations.MATCHES.name(), ng.getNodeBySparqlID("?Album").getValueType(), names);
			// check matches
			if(ng.getNodeBySparqlID("?Album").getValueConstraintStr().equals("VALUES ?Album { <test://music#decemberunderground>  }")){
				System.err.println("testContraintsSetByType() :: expected value for ?Album uri with Matches clause (angle brackets included)") ;
			}
			else { fail(); }

			// prefixed
			names = new ArrayList<String>();
			names.add("music:decemberunderground");

			// set matches
			rtci.selectAndSetConstraint("?Album", SupportedOperations.MATCHES.name(), ng.getNodeBySparqlID("?Album").getValueType(), names);
			// check matches
			if(ng.getNodeBySparqlID("?Album").getValueConstraintStr().equals("VALUES ?Album { music:decemberunderground  }")){
				System.err.println("testContraintsSetByType() :: expected value for ?Album uri with Matches clause (prefixed)") ;
			}
			else { fail(); }
	}
	
	
}
