/**
 ** Copyright 2016-2020 General Electric Company
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
package com.ge.research.semtk.edcquerygen.client;

import com.ge.research.semtk.services.client.RestClientConfig;

/**
 * Client for the RDBTimeCoherentTimeSeriesQueryGenService
 * (Extending without adding functionality so that it can be changed under the hood without modifying EDC mnemonic)
 */
public class RDBTimeCoherentTimeSeriesQueryGenClient extends EdcQueryGenClient {

	public RDBTimeCoherentTimeSeriesQueryGenClient(RestClientConfig conf) {
		super(conf);
	}		

}

