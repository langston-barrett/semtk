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


package com.ge.research.semtk.belmont;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.belmont.BelmontUtil;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.NodeItem;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.Returnable;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.ontologyTools.OntologyClass;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.OntologyName;
import com.ge.research.semtk.ontologyTools.OntologyProperty;
import com.ge.research.semtk.ontologyTools.OntologyRange;
import com.ge.research.semtk.ontologyTools.ValidationException;

//the nodes which represent any given entity the user/caller intends to manipulate. 

public class Node extends Returnable {
	
	private XSDSupportedType nodeType = XSDSupportedType.NODE_URI;
	
	// keeps track of our properties and collection 	
	private ArrayList<PropertyItem> props = new ArrayList<PropertyItem>();
	private ArrayList<NodeItem> nodes = new ArrayList<NodeItem>();
	
	// basic information required to be a node
	private String nodeName = null;
	private String fullURIname = null;
	private String instanceValue = null;
	private boolean instanceLookedUp = false;
	private NodeGroup nodeGroup = null;
	
	private NodeDeletionTypes deletionMode = NodeDeletionTypes.NO_DELETE;
	
	// Left-over from confused port from javascript
	public Node(String name, ArrayList<PropertyItem> p, ArrayList<NodeItem> n, String URI, NodeGroup ng){
		// just create the basic node.
		this.nodeName = name;
		this.fullURIname = URI;
		if(n != null){ this.nodes = n;}
		if(p != null){ this.props = p;}
		this.nodeGroup = ng;
		
		// add code to get the sparqlID
		this.sparqlID = BelmontUtil.generateSparqlID(name, this.nodeGroup.getSparqlNameHash());
	}
	
	public Node(String jsonStr, NodeGroup ng) throws Exception{
		// create the JSON Object we need and then call the other constructor. 
		this((JSONObject)(new JSONParser()).parse(jsonStr), ng);
	}
	
	public Node(JSONObject nodeEncoded, NodeGroup ng) throws Exception{
		// create a new node from JSON, assuming everything is sane. 
		this.nodeGroup = ng;
		
		this.updateFromJson(nodeEncoded);
	}
	
	public Node(JSONObject nodeEncoded, NodeGroup ng, OntologyInfo inflateOInfo) throws Exception{
		// create a new node from JSON, assuming everything is sane. 
		this.nodeGroup = ng;
		
		this.updateFromJson(nodeEncoded);
		this.inflateAndValidate(inflateOInfo);

	}
	
	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		return this.toJson(null);
	}
	
	/**
	 *
	 * @param dontDeflatePropItems - list of PropertyItems which should not be deflated
	 * @return
	 */
	public JSONObject toJson(ArrayList<PropertyItem> dontDeflatePropItems) {
		// return a JSON object of things needed to serialize
		JSONObject ret = new JSONObject();
		JSONArray jPropList = new JSONArray();
		JSONArray jNodeList = new JSONArray();
		
		// NOTE removed optional subclassNames
		
		// add properties
		for (int i = 0; i < this.props.size(); i++) { 
			PropertyItem p = this.props.get(i);
			// if compressFlag, then only add property if returned or constrained
			if (dontDeflatePropItems == null || p.isUsed() || dontDeflatePropItems.contains(p)) {
				jPropList.add(p.toJson());
			}
		}
		
		// add nodes
		for (int i = 0; i < this.nodes.size(); i++) {
			// if we're deflating, only add connected nodes
			if (dontDeflatePropItems == null || this.nodes.get(i).getConnected()) {
				jNodeList.add(this.nodes.get(i).toJson());
			}
		}
				
		ret.put("propList", jPropList);
		ret.put("nodeList", jNodeList);
		ret.put("NodeName", this.nodeName);
		ret.put("fullURIName", this.fullURIname);;
		ret.put("SparqlID", this.sparqlID);
		ret.put("isReturned", this.isReturned);
		ret.put("valueConstraint", this.getValueConstraintStr());
		ret.put("instanceValue", this.getInstanceValue());
		ret.put("isRuntimeConstrained", this.getIsRuntimeConstrained());
		ret.put("deletionMode", this.deletionMode.name());
		
		// optional things
		if (this.isTypeReturned) {
			ret.put("isTypeReturned", true);
		}
		
		return ret;
	}
	
	/**
	 * Instance was looked up and found
	 * @return
	 */
	public boolean isInstanceLookedUp() {
		return instanceLookedUp;
	}

	public void setInstanceLookedUp(boolean instanceLookedUp) {
		this.instanceLookedUp = instanceLookedUp;
	}

	/**
	 * Expand props to full set of properties for this classURI in oInfo 
	 * and validates all props
	 * @param classURI
	 * @param props
	 * @param oInfo - if non-null then inflate and validate
	 * @return
	 * @throws Exception
	 */
	public void inflateAndValidate(OntologyInfo oInfo) throws Exception {
		if (oInfo == null) { return; }
		
		ArrayList<PropertyItem> newProps = new ArrayList<PropertyItem>();
		ArrayList<NodeItem> newNodes = new ArrayList<NodeItem>();
		
		// build hash of suggested properties for this class
		HashMap<String, PropertyItem> propItemHash = new HashMap<>();
		for (PropertyItem p : this.props) {
			propItemHash.put(p.getUriRelationship(), p);
		}
		
		// build hash of suggested nodes for this class
		HashMap<String, NodeItem> nodeItemHash = new HashMap<>();
		for (NodeItem n : this.nodes) {
			nodeItemHash.put(n.getKeyName(), n);
		}
		
		// get oInfo's version of the property list
		OntologyClass ontClass = oInfo.getClass(this.getFullUriName());
		if (ontClass == null) {
			throw new ValidationException("Class does not exist in the model:  " + this.getFullUriName() + "   existing classes: " + oInfo.getClassNames().toString());
		}
		ArrayList<OntologyProperty> ontProps = oInfo.getInheritedProperties(ontClass);
		
		// loop through oInfo's version	
		for (OntologyProperty oProp : ontProps) {
			String oPropURI = oProp.getNameStr();
			String oPropKeyname = oProp.getNameStr(true);
			
			// if ontology property is one of the prop parameters, then check it over
			if (propItemHash.containsKey(oPropURI)) {
				
				// has range changed
				PropertyItem propItem = propItemHash.get(oPropURI);
				if (! propItem.getValueTypeURI().equals(oProp.getRangeStr())) {
					throw new ValidationException(String.format("Property %s range of %s doesn't match model range of %s",
														oPropURI, 
														propItem.getValueTypeURI(), 
														oProp.getRangeStr() ));
				}
				
				// all is ok: add the propItem
				newProps.add(propItem);
				
				propItemHash.remove(oPropURI);
				
			// else ontology property is not in this Node.  AND its range is outside the model (it's a Property)  
		    // Inflate (create) it.
			} else if (!oInfo.containsClass(oProp.getRangeStr())) {
				
				if (nodeItemHash.containsKey(oPropKeyname)) {
					throw new ValidationException(String.format("Node property %s has range %s in the nodegroup, which can't be found in model.", oPropURI, oProp.getRangeStr()));
				}
				
				PropertyItem propItem = new PropertyItem(	oProp.getNameStr(true), 
															oProp.getRangeStr(true), 
															oProp.getRangeStr(false),
															oProp.getNameStr(false));
				newProps.add(propItem);
				
			// node, in hash
			} else if (nodeItemHash.containsKey(oPropKeyname)) {
				// regardless of connection, check range
				NodeItem nodeItem = nodeItemHash.get(oPropKeyname);
				String nRangeStr = nodeItem.getUriValueType();
				String nRangeAbbr = nodeItem.getValueType();
				
				if (!nRangeStr.equals(oProp.getRangeStr())) {
					throw new ValidationException("Node property " + oPropURI + " range of " + nRangeStr + " doesn't match model range of " + oProp.getRangeStr());
				}
				if (!nRangeAbbr.equals(oProp.getRangeStr(true))) {
					throw new ValidationException("Node property " + oPropURI + " range abbreviation of " + nRangeAbbr + " doesn't match model range of " + oProp.getRangeStr(true));
				}
				
				// if connected 
				if (nodeItem.getConnected()) {
					
					// check full domain
					String nDomainStr = nodeItem.getUriConnectBy();
					if (!nDomainStr.equals(oProp.getNameStr())) {
						throw new ValidationException("Node property " + oPropURI + " domain of " + nDomainStr + " doesn't match model domain of " + oProp.getNameStr());
					}
					
					// check all connected snode classes
					OntologyClass nRangeClass = oInfo.getClass(nRangeStr);
					
					ArrayList<Node> snodeList = nodeItem.getNodeList();
					for (int j=0; j < snodeList.size(); j++) {
						String snodeURI = snodeList.get(j).getUri();
						OntologyClass snodeClass = oInfo.getClass(snodeURI);
						
						if (snodeClass == null) {
							throw new ValidationException("Node property " + oPropURI + " is connected to node with class " + snodeURI + " which can't be found in model");
						}
						
						if (!oInfo.classIsA(snodeClass, nRangeClass)) {
							throw new ValidationException("Node property " + oPropURI + " is connected to node with class " + snodeURI + " which is not a type of " + nRangeStr + " in model");

						}
					}
				}
				// all is ok: add the propItem
				newNodes.add(nodeItem);
				
				nodeItemHash.remove(oPropKeyname);
				
			// new node
			} else {
				NodeItem nodeItem = new NodeItem(	oProp.getNameStr(false), 
													oProp.getRangeStr(true),
													oProp.getRangeStr(false)
													);
				newNodes.add(nodeItem);
			}
		}
		
		if (!propItemHash.isEmpty()) {
			throw new ValidationException("Property does not exist in the model: " + propItemHash.keySet().toString());
		}
		if (!nodeItemHash.isEmpty()) {
			throw new ValidationException("Node property does not exist in the model: " + nodeItemHash.keySet().toString());
		}
		
		this.props = newProps;
		this.nodes = newNodes;
		
	}
	
	/**
	 * Validates that all nodeItems and propertyItems exist in the model
	 * @param oInfo
	 * @throws Exception
	 */
	public void validateAgainstModel(OntologyInfo oInfo) throws Exception {
		OntologyClass oClass = oInfo.getClass(this.fullURIname);
		
		if (oClass == null) {
			throw new ValidationException("Class URI does not exist in the model: " + this.fullURIname);
		}
		
		// build hash of ontology properties for this class
		HashMap<String, OntologyProperty> oPropHash = new HashMap<>();
		for (OntologyProperty op : oInfo.getInheritedProperties(oClass)) {
			oPropHash.put(op.getNameStr(), op);
		}
		
		// check each property's URI and range
		for (PropertyItem myPropItem : this.props) {
			// domain
			if (! oPropHash.containsKey(myPropItem.getUriRelationship())) {
				throw new ValidationException(String.format("Node %s contains property %s which does not exist in the model",
									this.getSparqlID(), myPropItem.getUriRelationship()));
			}
			
			// range
			OntologyRange oRange = oPropHash.get(myPropItem.getUriRelationship()).getRange();
			if (! oRange.getFullName().equals(myPropItem.getValueTypeURI())) {
				throw new ValidationException(String.format("Node %s, property %s has type %s which doesn't match %s in model", 
									this.getSparqlID(), myPropItem.getUriRelationship(), myPropItem.getValueTypeURI(), oRange.getFullName()));
			}
		}
		
		// check node items
		for (NodeItem myNodeItem : this.nodes) {
			if (myNodeItem.getConnected()) {
				// domain
				if (! oPropHash.containsKey(myNodeItem.getUriConnectBy())) {
					throw new ValidationException(String.format("Node %s contains node connection %s which does not exist in the model",
										this.getSparqlID(), myNodeItem.getUriConnectBy()));
				}
				
				// range
				// Raghava's bug is right here
				OntologyProperty oProp = oPropHash.get(myNodeItem.getUriConnectBy());
				OntologyRange oRange = oProp.getRange();
				if (! myNodeItem.getUriValueType().equals(oRange.getFullName())) {
					ArrayList<OntologyProperty> d = oInfo.getInheritedProperties(oClass);
					throw new ValidationException(String.format("Node %s contains node connection %s with type %s which doesn't match %s in model", 
										this.getSparqlID(), myNodeItem.getUriConnectBy(), myNodeItem.getUriValueType(), oRange.getFullName()));
				}
				
				// connected node types
				for (Node n : myNodeItem.getNodeList()) {
					OntologyClass rangeClass = oInfo.getClass(oRange.getFullName());
					OntologyClass myNodeClass = oInfo.getClass(n.getFullUriName());
					if (!oInfo.classIsA(myNodeClass, rangeClass)) {
						throw new ValidationException(String.format("Node %s, node connection %s connects to node %s with type %s which isn't a type of %s in model", 
								this.getSparqlID(), myNodeItem.getUriConnectBy(), n.getSparqlID(), n.getFullUriName(), oRange.getFullName()));
					}
				}
			}
		}
	}
	
	/** 
	 * clear everything except names and connections between nodes
	 */
	public void reset() {
		this.instanceValue = null;
		this.isReturned = false;
		this.deletionMode = NodeDeletionTypes.NO_DELETE;
		this.constraints = null;
		this.isRuntimeConstrained = false;
		
		for (PropertyItem p : this.props) {
			p.reset();
		}
		
		for (NodeItem n : this.nodes) {
			n.reset();
		}
	}
	
	public void setSparqlID(String ID){
		if (this.constraints != null) {
			this.constraints.changeSparqlID(this.sparqlID, ID);
		}
		this.sparqlID = ID;
	}
	
	public void updateFromJson(JSONObject nodeEncoded) throws Exception{
		// blank existing 
		props = new ArrayList<PropertyItem>();
		nodes = new ArrayList<NodeItem>();
		nodeName = null;
		fullURIname = null;
		instanceValue = null;		
		
		// build all the parts we need from this incoming JSON Object...
		this.nodeName = nodeEncoded.get("NodeName").toString();
		this.fullURIname = nodeEncoded.get("fullURIName").toString();
		this.sparqlID = nodeEncoded.get("SparqlID").toString();
				
		// NOTE: removed OPTIONAL array of subclass names.
		
		this.isReturned = (Boolean)nodeEncoded.get("isReturned");
		
		try{
			this.instanceValue = nodeEncoded.get("instanceValue").toString();
		}
		catch(Exception E){ // the value was missing
			this.instanceValue = null;
		}
		try{
			String vc = nodeEncoded.get("valueConstraint").toString();
			if (vc.length() < 1) {  // change blank constraints to null
				this.constraints = null;
			} else {
				this.constraints = new ValueConstraint(vc);
			}
		}
		catch(Exception E){ // the value was not set
			this.constraints = null;
		}
		try{
			this.setIsRuntimeConstrained((Boolean)nodeEncoded.get("isRuntimeConstrained"));
		}
		catch(Exception E){
			this.setIsRuntimeConstrained(false);
		}
		try{
			this.setDeletionMode(NodeDeletionTypes.valueOf((String)nodeEncoded.get("deletionMode")));
		}
		catch(IllegalArgumentException iae) {
			throw iae;   // known bad enum exception
		}
		catch(NullPointerException enpe){
			this.setDeletionMode(NodeDeletionTypes.NO_DELETE);  // deletionMode is missing
		}
		catch(Exception ee){
			throw ee;   // other unexpected exception
		}
		
		
		// create the node items and property items.
		// nodeItems 
		JSONArray nodesToProcess = (JSONArray)nodeEncoded.get("nodeList");
		Iterator<JSONObject> nIt = nodesToProcess.iterator();
		while(nIt.hasNext()){
			this.nodes.add(new NodeItem(nIt.next(), this.nodeGroup));
		}
		
		// propertyItems
		JSONArray propertiesToProcess = (JSONArray)nodeEncoded.get("propList");
		Iterator<JSONObject> pIt = propertiesToProcess.iterator();
		while(pIt.hasNext()){
			this.props.add(new PropertyItem(pIt.next()));
		}
		
		// optional things
		if (nodeEncoded.containsKey("isTypeReturned")) {
			this.isTypeReturned = (Boolean)nodeEncoded.get("isTypeReturned");
		} else {
			this.isTypeReturned = false;
		}
		
	}
	
	public NodeItem setConnection(Node curr, String connectionURI) throws Exception {
		return this.setConnection(curr,  connectionURI, NodeItem.OPTIONAL_FALSE, false);
	}
	
	public NodeItem setConnection(Node curr, String connectionURI, int opt) throws Exception {
		return this.setConnection(curr,  connectionURI, opt, false);
	}
	
	public NodeItem setConnection(Node curr, String connectionURI, Boolean markForDeletion) throws Exception {
		return this.setConnection(curr,  connectionURI, NodeItem.OPTIONAL_FALSE, markForDeletion);
	}
	
	public NodeItem setConnection(Node curr, String connectionURI, int opt, Boolean markedForDeletion) throws Exception {
		// create a display name. 
		String connectionLocal = new OntologyName(connectionURI).getLocalName();

		// actually set the connection. 
		for(int i = 0; i < this.nodes.size(); i += 1){
			NodeItem nd = this.nodes.get(i);
			// did it match?
			if(nd.getKeyName().equals(connectionLocal)){
				nd.setConnected(true);
				nd.setConnectBy(connectionLocal);
				nd.setUriConnectBy(connectionURI);
				nd.pushNode(curr, opt, markedForDeletion);
				
				return nd;
			}
		}
		throw new Exception("Internal error in SemanticNode.setConnection().  Couldn't find node item connection: " + this.getSparqlID() + "->" + connectionURI);
	}

	public ArrayList<PropertyItem> getReturnedPropertyItems() {
		ArrayList<PropertyItem> retval = new ArrayList<PropertyItem>();
		// spin through the list of values and add teh correct ones. 
		for(int i = 0; i < this.props.size(); i += 1){
			PropertyItem pi = this.props.get(i);
			if(pi.getIsReturned()){		// we are returning this one. add it to the list. 
				retval.add(pi);
			}
		}
		// return the list. 
		return retval;
	}	
	
	public ArrayList<PropertyItem> getPropsForSparql(Returnable forceReturn, AutoGeneratedQueryTypes qt) {
		ArrayList<PropertyItem> retval = new ArrayList<PropertyItem>();
		// spin through the list of values and add teh correct ones. 
		for(int i = 0; i < this.props.size(); i += 1){
			PropertyItem pi = this.props.get(i);
			if(pi.getIsReturned()){		// we are returning this one. add it to the list. 
				retval.add(pi);
			}
			else if(pi.getIsMarkedForDeletion() && qt != null && qt == AutoGeneratedQueryTypes.QUERY_DELETE_WHERE){
				retval.add(pi);		   // 
			}
			else if( pi.getConstraints() != null && pi.getConstraints() != "" && !pi.getConstraints().isEmpty() ){
				retval.add(pi);
			}
			else if(pi.equals(forceReturn)){
				retval.add(pi);
			}
		}
		// return the list. 
		return retval;
	}	
		
	
	
	public boolean checkConnectedTo(Node nodeToCheck) {
		for (NodeItem n : nodes) {
			if (n.getConnected()) {
				ArrayList<Node> nodeList = n.getNodeList();
				for (Node o : nodeList) {
					if (o.getSparqlID().equals(nodeToCheck.getSparqlID())) {
						return true;
					}
				}
			}
		}	
		
		return false;
	}
	
	public ArrayList<Node> getConnectedNodes() {
		
		ArrayList<Node> connectedNodes = new ArrayList<Node>();
		
		for (NodeItem ni : this.nodes) {
			if (ni.getConnected()) {
				connectedNodes.addAll(ni.getNodeList());
			}
		}
		
		return connectedNodes;
	}

	/**
	 * 
	 * @return value constraint string, which might be "" but not null
	 */
	public String getValueConstraintStr() {
		return this.constraints != null ? this.constraints.toString() : "";
	}
	
	public void setValueConstraint(ValueConstraint v) {
		this.constraints = v;
	}
	
	public void addValueConstraint(String vc) {
		if (this.constraints == null) {
			this.constraints = new ValueConstraint(vc);
		} else {
			this.constraints.addConstraint(vc);
		}
	}
	
	public String getUri(boolean localFlag) {
		if (localFlag) {
			return new OntologyName(this.getFullUriName()).getLocalName();
		} else {
			return this.getFullUriName();
		}
	}
	public String getUri() {
		return this.getFullUriName();
	}
	
	public String getFullUriName() {
		return fullURIname;
	}

	public boolean isUsed(boolean instanceOnly) {
		if (instanceOnly) {
			return this.hasInstanceData();
		} else {
			return this.isUsed();
		}
	}
	
	public boolean isUsed() {
		if (this.isReturned || this.isTypeReturned || this.constraints != null || this.instanceValue != null || this.isRuntimeConstrained || this.deletionMode != NodeDeletionTypes.NO_DELETE) {
			return true;
		}
		for (PropertyItem item : this.props) {
			if (item.isReturned || item.getConstraints() != null || item.getInstanceValues().size() > 0 || item.getIsRuntimeConstrained() || item.getIsMarkedForDeletion()) {
				return true;
			}
		}
		return false;	
	}
	
	public boolean hasInstanceData() {
		if (this.instanceValue != null) {
			return true;
		}
		for (PropertyItem item : this.props) {
			if (item.getInstanceValues().size() > 0) {
				return true;
			}
		}
		return false;	
	}
	 
	public ArrayList<String> getSparqlIDList() {
		// list all sparqlID's in use by this node
		ArrayList<String> ret = new ArrayList<String>();
		ret.add(this.getSparqlID());
		
		for (PropertyItem p : this.props) {
			String s = p.getSparqlID();
			if (s != null && ! s.isEmpty()) {
				ret.add(s);
			}
		}
		return ret;
	}
	
	public void removeFromNodeList(Node node) {
		for (NodeItem item : this.nodes) {
			item.removeNode(node);
		}
	}
	
	/**
	 * Find all nodeItems that connect to otherNode
	 * @param otherNode
	 * @return
	 */
	public ArrayList<NodeItem> getConnectingNodeItems(Node otherNode) {
		ArrayList<NodeItem> ret = new ArrayList<NodeItem>();
		
		// look through all my nodeItems
		for (NodeItem item : this.nodes) {
			if (item.getConnected()) {
				ArrayList<Node> nodeList = item.getNodeList();
				
				// does my connection point to otherNode
				for (Node node : nodeList) {
					if (node.getSparqlID().equals(otherNode.getSparqlID())) {
						ret.add(item);
					}
				}
			}
		}
		
		return ret;
	}

	/**
	 * Get all nodeItems that are connected
	 * @param otherNode
	 * @return
	 */
	public ArrayList<NodeItem> getConnectedNodeItems() {
		ArrayList<NodeItem> ret = new ArrayList<NodeItem>();
		
		// look through all my nodeItems
		for (NodeItem item : this.nodes) {
			if (item.getConnected()) {
				ret.add(item);
			}
		}
		
		return ret;
	}
	
	/**
	 * Get node item by predicateURI
	 * @param predicateURI
	 * @return might be null
	 */
	public NodeItem getNodeItem(String predicateURI) {
		for (NodeItem n : this.nodes) {
			if (n.getUriConnectBy().equals(predicateURI)) {
				return n;
			}
		}
		return null;
	}

	public ArrayList<PropertyItem> getPropertyItems() {
		return this.props;
	}
	
	public PropertyItem getPropertyItemBySparqlID(String currID){
		/* 
		 * return the given property item, if we can find it. if not, 
		 * just return null. 
		 * if an ID not prefixed with ? is passed, we are just going to add it.
		 */
		
		int i = this.getPropertyItemIndexBySparqlID(currID);
		if (i == -1) 
			return null;
		else
			return this.props.get(i);
	}
	
	public int getPropertyItemIndexBySparqlID(String currID) {
		if(currID != null && !currID.isEmpty() && !currID.startsWith("?")){
			currID = "?" + currID;
		}
		for (int i = 0; i < this.props.size(); i++) {
			if (this.props.get(i).getSparqlID().equals(currID)) {
				return i;
			}
		}
		return -1;
	}
	
	public PropertyItem getPropertyByKeyname(String keyname) {
		for (int i = 0; i < this.props.size(); i++) {
			if (this.props.get(i).getKeyName().equals(keyname)) {
				return this.props.get(i);
			}
		}
		return null;
	}
	
	public PropertyItem getPropertyItem(int i) {
		return this.props.get(i);
	}
	
	public int getPropertyIndexByURIRelation(String uriRel) {
		for (int i = 0; i < this.props.size(); i++) {
			if (this.props.get(i).getUriRelationship().equals(uriRel)) {
				return i;
			}
		}
		return -1;
	}

	public PropertyItem getPropertyByURIRelation(String uriRel) {
		for (int i = 0; i < this.props.size(); i++) {
			if (this.props.get(i).getUriRelationship().equals(uriRel)) {
				return this.props.get(i);
			}
		}
		return null;
	}
	
	public boolean ownsNodeItem(NodeItem nodeItem) {
		return this.nodes.contains(nodeItem);
	}
	
	public String getInstanceValue() {
		return this.instanceValue;
	}
	
	public void setInstanceValue(String value) {
		this.instanceValue = value;
	}
	
	public void setProperties(ArrayList<PropertyItem> p){
		if(p != null){
			this.props = p;
		}
	}
	
	public void setNodeItems(ArrayList<NodeItem> n){
		if(n!= null){
			this.nodes = n;
		}
	}
	
	public ArrayList<NodeItem> getNodeItemList() {
		return this.nodes;
	}
	
	public int countReturns () {
		int ret = this.getIsReturned() ? 1 : 0;
		
		for (PropertyItem p : this.props) {
			ret += (p.getIsReturned() ? 1 : 0);
		}
		
		return ret;
	}
	
	public int countUnconstrainedReturns () {
		int ret = 0;
		
		if (this.getIsReturned() && this.getValueConstraint() == null) {
			ret ++;
		}
		
		for (PropertyItem p : this.props) {
			if (p.getIsReturned() && p.getValueConstraint() == null) {
				ret ++;
			}
		}
		
		return ret;
	}
	
	public int countConstrainedReturns () {
		int ret = 0;
		
		if (this.getIsReturned() && this.getValueConstraint() != null) {
			ret ++;
		}
		
		for (PropertyItem p : this.props) {
			if (p.getIsReturned() && p.getValueConstraint() != null) {
				ret ++;
			}
		}
		
		return ret;
	}
	
	public int getReturnedCount () {
		int ret = 0;
		if (this.getIsReturned()) {
			ret += 1;
		}
		if (this.getIsTypeReturned()) {
			ret += 1;
		}
		for (PropertyItem p : this.getPropertyItems()) {
			if (p.getIsReturned()) {
				ret += 1;
			}
		}
		return ret;
	}
	
	public ArrayList<PropertyItem> getConstrainedPropertyObjects() {
		ArrayList<PropertyItem> constrainedProperties = new ArrayList<PropertyItem>();
		
		for (PropertyItem pi : props) {
			if (pi.getConstraints() != null && !pi.getConstraints().isEmpty()) {
				constrainedProperties.add(pi);
			}
		}
		
		return constrainedProperties;
	}
	public XSDSupportedType getValueType(){
		return this.nodeType;
	}
	
	public void setDeletionMode(NodeDeletionTypes ndt){
		this.deletionMode = ndt;
	}
	
	public NodeDeletionTypes getDeletionMode(){
		return this.deletionMode;
	}

}
