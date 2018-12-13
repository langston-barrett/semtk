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

package com.ge.research.semtk.belmont.test;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.datanucleus.store.rdbms.schema.PostgresqlTypeInfo;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeDeletionTypes;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.NodeItem;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class DeletionTest_IT {

	public String jsonNodegroupPath = "src/test/resources/ConsolidatedMusicTestDeletionNodegroup.json";
	public String ttlGraphContentsPath = "src/test/resources/musicTestDataset_2017.q2.ttl";
	public String domain = "http://";
	public NodeGroup ng = null;
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
	}
	
	public void setupTest() throws Exception{
		// load the dataset we want to use.
		
		ng = TestGraph.getNodeGroup(jsonNodegroupPath);
		
		// flush and reload graph.
		TestGraph.clearGraph();
		TestGraph.uploadTurtle(ttlGraphContentsPath);
	}

	
	@Test
	public void testDeletionOfTypeInformationOnly() throws Exception{
		
		setupTest();
		
		NodeGroup testgrp = modifyNodeGroupToDeleteForAGivenNodeSparlId(ng, NodeDeletionTypes.TYPE_INFO_ONLY, "?Band");
		
		// get a basis number...
		String basisSparql = testgrp.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 50000, null);
		int basis = TestGraph.execTableSelect(basisSparql).getNumRows();
		
		// modify the nodegroup to omit everything BUT the bands...
		ArrayList<Node> nodes = new ArrayList<Node>();
	
		for( Node node : testgrp.getNodeList() ){
			nodes.add(node);
		}
		for(Node nd : nodes){
			if(!nd.getSparqlID().equals("?Band")){
				testgrp.deleteNode(nd, false);
			}
		}
		
		String sparqlDeleteQuery = testgrp.generateSparqlDelete(null);
		String sparqlInitialQuery = testgrp.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 50000, null);
		
		int preDeletionRows = TestGraph.execTableSelect(sparqlInitialQuery).getNumRows();
		TestGraph.execDeletionQuery(sparqlDeleteQuery);
			
		String sparqlCompareQuery = testgrp.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 50000, null);
		int postDeletionRows = TestGraph.execTableSelect(sparqlCompareQuery).getNumRows();
	
		assertTrue(preDeletionRows > postDeletionRows); // check that we can no longer find the Bands.
		
		// check that the original query still returns all the same number of rows (since type info is ignored in it)
		int postDel =  TestGraph.execTableSelect(basisSparql).getNumRows();
		assertTrue(basis == postDel);
	}
	@Test
	public void testDeletionOfAllInScope() throws Exception{
		
		setupTest();
		
		NodeGroup testgrp = modifyNodeGroupToDeleteForAGivenNodeSparlId(ng, NodeDeletionTypes.FULL_DELETE, "?Band");
		
		// get a basis number...
		String basisSparql = testgrp.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 50000, null);
		int basis = TestGraph.execTableSelect(basisSparql).getNumRows();
		
		// modify the nodegroup to omit everything BUT the bands...
		ArrayList<Node> nodes = new ArrayList<Node>();
	
		for( Node node : testgrp.getNodeList() ){
			nodes.add(node);
		}
		for(Node nd : nodes){
			if(!nd.getSparqlID().equals("?Band")){
				testgrp.deleteNode(nd, false);
			}
		}
		
		String sparqlDeleteQuery = testgrp.generateSparqlDelete(null);
		String sparqlInitialQuery = testgrp.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 50000, null);
		
		int preDeletionRows = TestGraph.execTableSelect(sparqlInitialQuery).getNumRows();
		TestGraph.execDeletionQuery(sparqlDeleteQuery);
			
		String sparqlCompareQuery = testgrp.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 50000, null);
		int postDeletionRows = TestGraph.execTableSelect(sparqlCompareQuery).getNumRows();
	
		assertTrue(preDeletionRows > postDeletionRows); // check that we can no longer find the Bands.
		
		// check that the original query returns more rows
		int postDel =  TestGraph.execTableSelect(basisSparql).getNumRows();
		assertTrue(basis > postDel);
	}
	
	@Test
	public void testDeletionOfAllInScopeWithPropertyConstraint() throws Exception{
		
		setupTest();
		
		NodeGroup testgrp = modifyNodeGroupToDeleteForAGivenNodeSparlId(ng, NodeDeletionTypes.FULL_DELETE, "?Band");
		
		// get a basis number...
		String basisSparql = testgrp.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 50000, null);
		int basis = TestGraph.execTableSelect(basisSparql).getNumRows();
		
		// modify the nodegroup to omit everything BUT the bands...
		ArrayList<Node> nodes = new ArrayList<Node>();
	
		for( Node node : testgrp.getNodeList() ){
			nodes.add(node);
		}
		for(Node nd : nodes){
			if(!nd.getSparqlID().equals("?Band")){
				testgrp.deleteNode(nd, false);
			}
		}
		
		String sparqlInitialQuery = testgrp.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 50000, null);
		
		int preDeletionRows = TestGraph.execTableSelect(sparqlInitialQuery).getNumRows();

		// add the property-based deletion

		testgrp.getPropertyItemBySparqlID("?name").setValueConstraint(new ValueConstraint("VALUES ?name { 'Kansas'^^XMLSchema:string } "));;
		
		String sparqlDeleteQuery = testgrp.generateSparqlDelete(null);

		
		TestGraph.execDeletionQuery(sparqlDeleteQuery);
			
		int postDeletionRows = TestGraph.execTableSelect(sparqlInitialQuery).getNumRows();
	
		assertTrue(preDeletionRows == 4); // check that we can no longer find the Bands.
		assertTrue(postDeletionRows == 3);
		
		// check that the original query returns more rows
		int postDel =  TestGraph.execTableSelect(basisSparql).getNumRows();
		assertTrue(basis > postDel);
	}
	
	
	@Test
	public void testDeletionOfNodeGroupScope() throws Exception{
		
		setupTest();
		
		NodeGroup testgrp = modifyNodeGroupToDeleteForAGivenNodeSparlId(ng, NodeDeletionTypes.LIMITED_TO_NODEGROUP, "?Band");
		
		// get a basis number...
		String basisSparql = testgrp.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 50000, null);
		String sparqlCompareQuery = testgrp.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 50000, null);
		int basis = TestGraph.execTableSelect(basisSparql).getNumRows();
		
		
		String sparqlInitialQuery = testgrp.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 50000, null);
		
		int preDeletionRows = TestGraph.execTableSelect(sparqlInitialQuery).getNumRows();
		
		// add target info to remove a given band. 
		
		// add the property-based deletion

		testgrp.getPropertyItemBySparqlID("?name").setValueConstraint(new ValueConstraint("VALUES ?name { 'Kansas'^^XMLSchema:string } "));;
		
		String sparqlDeleteQuery = testgrp.generateSparqlDelete(null);
		TestGraph.execDeletionQuery(sparqlDeleteQuery);
			
		int postDeletionRows = TestGraph.execTableSelect(sparqlCompareQuery).getNumRows();
	
		assertTrue(preDeletionRows > postDeletionRows); // check that we can no longer find the Bands.
		
		// check that the original query returns more rows
		int postDel =  TestGraph.execTableSelect(basisSparql).getNumRows();
		assertTrue(basis > postDel);
	}	
	
	@Test
	public void testDeletionOfTypeInfoOnlyWithPropertyConstraint() throws Exception{
		
		setupTest();
		
		NodeGroup testgrp = modifyNodeGroupToDeleteForAGivenNodeSparlId(ng, NodeDeletionTypes.TYPE_INFO_ONLY, "?Band");
		
		// get a basis number...
		String basisSparql = testgrp.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 50000, null);
		int basis = TestGraph.execTableSelect(basisSparql).getNumRows();
		
		// modify the nodegroup to omit everything BUT the bands...
		ArrayList<Node> nodes = new ArrayList<Node>();
	
		for( Node node : testgrp.getNodeList() ){
			nodes.add(node);
		}
		for(Node nd : nodes){
			if(!nd.getSparqlID().equals("?Band")){
				testgrp.deleteNode(nd, false);
			}
		}
		
		String sparqlInitialQuery = testgrp.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 50000, null);
		
		int preDeletionRows = TestGraph.execTableSelect(sparqlInitialQuery).getNumRows();

		// add the property-based deletion

		testgrp.getPropertyItemBySparqlID("?name").setValueConstraint(new ValueConstraint("VALUES ?name { 'Kansas'^^XMLSchema:string } "));;
		
		String sparqlDeleteQuery = testgrp.generateSparqlDelete(null);

		TestGraph.execDeletionQuery(sparqlDeleteQuery);
			
		int postDeletionRows = TestGraph.execTableSelect(sparqlInitialQuery).getNumRows();
	
		assertTrue(preDeletionRows == 4); // check that we can no longer find the Bands.
		assertTrue(postDeletionRows == 3);
		
		// check that the original query returns more rows
		int postDel =  TestGraph.execTableSelect(basisSparql).getNumRows();
		assertTrue(basis == postDel);
	}
	
	@Test
	public void testDeletionOfSpecificPropertyInstancesByContraints() throws Exception {
		setupTest();
		
		NodeGroup testgrp = modifyNodeGroupToDeleteForAGivenNodeSparlId(ng, NodeDeletionTypes.TYPE_INFO_ONLY, "?Band");
		// modify the nodegroup to omit everything BUT the albumTracks and songs...
		ArrayList<Node> nodes = new ArrayList<Node>();
		
		for( Node node : testgrp.getNodeList() ){
			nodes.add(node);
		}
		for(Node nd : nodes){
			if(!nd.getSparqlID().equals("?Song") && !nd.getSparqlID().equals("?AlbumTrack")){
				testgrp.deleteNode(nd, false);
			}
		}
		
		String sparqlInitialQuery = testgrp.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 50000, null);
		int preDeletionRows = TestGraph.execTableSelect(sparqlInitialQuery).getNumRows();
		
		// add a constraint to use to delete.
		testgrp.getPropertyItemBySparqlID("?durationInSeconds").setValueConstraint(new ValueConstraint("VALUES ?durationInSeconds { '290'^^XMLSchema:int } "));;
		testgrp.getPropertyItemBySparqlID("?durationInSeconds").setIsMarkedForDeletion(true);
		
		String sparqlDeleteQuery = testgrp.generateSparqlDelete(null);
		TestGraph.execDeletionQuery(sparqlDeleteQuery);
		
		int postDeletionRows = TestGraph.execTableSelect(sparqlInitialQuery).getNumRows();
		
		assertTrue(preDeletionRows == 12);
		assertTrue(postDeletionRows == 10);	
	}
	
	@Test
	public void testDeletionOfSpecificNodeItemInstanceBySomeRelatedConstriant() throws Exception {
	setupTest();
		
		NodeGroup testgrp = modifyNodeGroupToDeleteForAGivenNodeSparlId(ng, NodeDeletionTypes.TYPE_INFO_ONLY, "?Band");
		// modify the nodegroup to omit everything BUT the albumTracks and songs...
		ArrayList<Node> nodes = new ArrayList<Node>();
		
		for( Node node : testgrp.getNodeList() ){
			nodes.add(node);
		}
		for(Node nd : nodes){
			if(!nd.getSparqlID().equals("?Song") && !nd.getSparqlID().equals("?AlbumTrack")){
				testgrp.deleteNode(nd, false);
			}
		}
		
		String sparqlInitialQuery = testgrp.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, false, 50000, null);
		int preDeletionRows = TestGraph.execTableSelect(sparqlInitialQuery).getNumRows();
	
		// add a constraint to use to delete.
		testgrp.getPropertyItemBySparqlID("?durationInSeconds").setValueConstraint(new ValueConstraint("VALUES ?durationInSeconds { '290'^^XMLSchema:int } "));;
	
		for(NodeItem ni : testgrp.getNodeBySparqlID("?AlbumTrack").getNodeItemList()){
			if(ni.getUriConnectBy().equals("http://com.ge.research/knowledge/test/popMusic#song")){
				// we have our contrived example...
				for(Node nTarget : ni.getNodeList() ){
					if(nTarget.getSparqlID().equals("?Song")){
						ni.setSnodeDeletionMarker(nTarget, true);
					}

				}
			}
		}
		
		String sparqlDeleteQuery = testgrp.generateSparqlDelete(null);
		TestGraph.execDeletionQuery(sparqlDeleteQuery);
		
		int postDeletionRows = TestGraph.execTableSelect(sparqlInitialQuery).getNumRows();
		
		assertTrue(preDeletionRows == 12);
		assertTrue(postDeletionRows == 10);	
	}
	
	public NodeGroup modifyNodeGroupToDeleteForAGivenNodeSparlId(NodeGroup ng, NodeDeletionTypes del, String sparqlID) throws Exception{
		setupTest();
		
		NodeGroup retval = ng.deepCopy(ng);
		
		// change the ?Band node to have a deletion mode set
		Node nd = retval.getNodeBySparqlID(sparqlID);
		nd.setDeletionMode(del);
		
		// ship out the copy
		return retval;
	}
	
	public NodeGroup modifyNodeGroupToIncludePropertyDeletion(NodeGroup inputNg, String propSparqlID) throws Exception{
		
	// alter the sparqlID's constraints
	PropertyItem ofInterest = inputNg.getPropertyItemBySparqlID(propSparqlID);
	if(ofInterest != null){
		// let the modification commence.
		ofInterest.setIsMarkedForDeletion(true);;
	}
	else{
		throw new Exception("passed Property Item ID (" + propSparqlID + ") was not related to a known property!");
	}
	
	return inputNg;
		
	}
	
	public NodeGroup modifyNodeGroupToIncludePropertyContraints(NodeGroup inputNg, String propSparqlID, String constraint) throws Exception{
			
		// alter the sparqlID's constraints
		PropertyItem ofInterest = inputNg.getPropertyItemBySparqlID(propSparqlID);
		if(ofInterest != null){
			// let the modification commence.
			ofInterest.setValueConstraint(new ValueConstraint(constraint));
		}
		else{
			throw new Exception("passed Property Item ID (" + propSparqlID + ") was not related to a known property!");
		}
		
		return inputNg;
	}

}