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

package com.ge.research.semtk.services.nodeGroupExecution;

import java.util.TreeMap;

import com.ge.research.semtk.services.nodeGroupExecution.NodegroupExecutionAuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.utility.Utility;

@Component
public class NodegroupExecutionServiceStartup implements ApplicationListener<ApplicationReadyEvent> {

	@Autowired
	private NodegroupExecutionAuthProperties auth_prop;
	
  /**
   * Code to run after the service starts up.
   */
	@Override
	public void onApplicationEvent(final ApplicationReadyEvent event) {

		// print and validate properties - and exit if invalid
		String[] propertyNames = {
				"ssl.enabled",
				"node-group-execution.ngStoreProtocol",
				"node-group-execution.ngStoreServer",
				"node-group-execution.ngStorePort",
				"node-group-execution.dispatchProtocol",
				"node-group-execution.dispatchServer",
				"node-group-execution.dispatchPort",
				"node-group-execution.resultsProtocol",
				"node-group-execution.resultsServer",
				"node-group-execution.resultsPort",
				"node-group-execution.statusProtocol",
				"node-group-execution.statusServer",
				"node-group-execution.statusPort",
				"node-group-execution.edc.services.jobEndpointType",
				"node-group-execution.edc.services.jobEndpointDomain",
				"node-group-execution.edc.services.jobEndpointServerUrl",
				"node-group-execution.edc.services.jobEndpointDataset",
				//"node-group-execution.edc.services.jobEndpointUsername",
				//"node-group-execution.edc.services.jobEndpointPassword"
		};
		TreeMap<String,String> properties = new TreeMap<String,String>();
		for(String propertyName : propertyNames){
			properties.put(propertyName, event.getApplicationContext().getEnvironment().getProperty(propertyName));
		}
		Utility.validatePropertiesAndExitOnFailure(properties); 

		try {
			AuthorizationManager.authorize(auth_prop);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return;
	}
 
}
