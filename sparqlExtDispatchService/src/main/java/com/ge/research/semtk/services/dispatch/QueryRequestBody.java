/**
 ** Copyright 2018 General Electric Company
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

package com.ge.research.semtk.services.dispatch;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.utility.Utility;

public class QueryRequestBody extends NodegroupRequestBody {

	private String constraintSet;	
	private String flags;			// a string parseable to a JSONArray

	public void setConstraintSet(String constraintSet){
		this.constraintSet = constraintSet;
	}
	public String getConstraintSet(){
		return this.constraintSet;
	}
	
	public String getFlags() {
		return this.flags;
	}
	public void setFlags(String flagsJsonArrayStr) {
		this.flags = flagsJsonArrayStr;
	}
	
	public JSONObject getConstraintSetJson() throws Exception{
		if(this.constraintSet != null && !this.constraintSet.trim().isEmpty()){
			return Utility.getJsonObjectFromString(this.constraintSet);
		}
		return null;
	}

	public JSONArray getFlagsJsonArray() throws Exception{
		if(this.flags != null && !this.flags.trim().isEmpty()){
			return Utility.getJsonArrayFromString(this.flags);
		}
		return null;
	}
	
}
