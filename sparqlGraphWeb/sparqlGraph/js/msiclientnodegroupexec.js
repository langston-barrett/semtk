/**
 ** Copyright 2016-17 General Electric Company
 **
 ** Authors:  Paul Cuddihy, Justin McHugh
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

define([	// properly require.config'ed   bootstrap-modal
        	'sparqlgraph/js/microserviceinterface',
        	'sparqlgraph/js/msiresultset',
            'sparqlgraph/js/msiclientstatus',
            'sparqlgraph/js/msiclientresults'

			// shimmed

		],

	function(MicroServiceInterface, MsiResultSet, MsiClientStatus, MsiClientResults) {


		var MsiClientNodeGroupExec = function (serviceURL, optTimeout) {

			this.msi = new MicroServiceInterface(serviceURL);
            this.optTimeout = optTimeout;
		};

        MsiClientNodeGroupExec.scaleProgress = function(progressCallback, min, max, val, optMsgOverride) {
            progressCallback(min + Math.floor((max - min) * val / 100), optMsgOverride);
        };

        MsiClientNodeGroupExec.USE_NODEGROUP_CONN = '{"name": "%NODEGROUP%","domain": "%NODEGROUP%","model": [],"data": []}';

        /*
         * Create a jobIdCallback suitable for the execAsync* functions, that
         *      handles the status and results clients
         *      sends any errors to failureCallback(html)
         *      updates progress with progressCallback(percent)
         *      calls tableResCallback(fullCsvUrl, tableResults)
         *
         * Given these:
         *      maxRows - max rows in tableResults
         *
         *      tableResCallback(csvFilename, fullCsvUrl, tableResults)
         *          this - will be the document not any object
         *          fullCsvUrl - url of full results in csv form
         *          tableResults - MsiResultSet where isTableResults() == true
         *
         *      failureCallback(html)
         *
         *      progressCallback(progressPercentInteger)
         *
         *      statusUrl - url of status service
         *
         *      resultUrl - url of results service
         */
		MsiClientNodeGroupExec.buildCsvUrlSampleJsonCallback = function(maxRows, csvUrlSampleJsonCallback, failureCallback, progressCallback, checkForCancelCallback, statusUrl, resultUrl) {

            // callback for the nodegroup execution service to send jobId
            var ngExecJobIdCallback = function(maxRows, csvUrlSampleJsonCallback0, failureCallback0, progressCallback0, checkForCancelCallback0, statusUrl, resultUrl, jobId) {

                // callback for status service after job successfully finishes
                var ngStatusSuccessCallback = function(jobId, maxRows, csvUrlSampleJsonCallback1, failureCallback1, progressCallback1, checkForCancelCallback1, resultUrl) {

                    // callback for results service
                    var ngResultsSuccessCallback = function (csvUrlSampleJsonCallback2, progressCallback2, csvFilename, fullURL, results) {
                        progressCallback2(99);
                        csvUrlSampleJsonCallback2(csvFilename, fullURL, results);
                    };

                    // get csv url
                    var resultsClient = new MsiClientResults(resultUrl, jobId, failureCallback1);
                    var fullURL = resultsClient.getTableResultsCsvDownloadUrl();
                    var csvFilename = jobId + ".csv";

                    // ask for json results and give csvUrlSampleJsonCallback with the csv Url bound
                    resultsClient.execGetTableResultsJsonTableRes(maxRows,
                                                                  ngResultsSuccessCallback.bind(this, csvUrlSampleJsonCallback1, progressCallback1, csvFilename, fullURL));

                }.bind(this, jobId, maxRows, csvUrlSampleJsonCallback0, failureCallback0, progressCallback0, checkForCancelCallback0, resultUrl);

                progressCallback0(5); // got jobId
                var sProgress = MsiClientNodeGroupExec.scaleProgress.bind(this, progressCallback0, 10, 90);

                // call status service loop
                var statusClient = new MsiClientStatus(statusUrl, jobId, failureCallback0);
                statusClient.execAsyncWaitUntilDone(ngStatusSuccessCallback, checkForCancelCallback0, sProgress);

            }.bind(this, maxRows, csvUrlSampleJsonCallback, failureCallback, progressCallback, checkForCancelCallback, statusUrl, resultUrl);

            return ngExecJobIdCallback;
        };

        /*
         * just like buildCsvUrlSampleJsonCallback
         * EXCEPT:
         *    no max rows or csv URL
         *    tableResCallback(tableRes)
         */
        MsiClientNodeGroupExec.buildFullJsonCallback = function(tableResCallback, failureCallback, progressCallback, checkForCancelCallback, statusUrl, resultUrl) {

            // callback for the nodegroup execution service to send jobId
            var ngExecJobIdCallback = function(tableResCallback0, failureCallback0, progressCallback0, checkForCancelCallback0, statusUrl, resultUrl, jobId) {

                // callback for status service after job successfully finishes
                var ngStatusSuccessCallback = function(jobId, tableResCallback1, failureCallback1, progressCallback1, checkForCancelCallback1, resultUrl) {

                    // callback for results service
                    var ngResultsSuccessCallback = function (tableResCallback2, progressCallback2, results) {
                        progressCallback2(99);
                        tableResCallback2(results);
                    };

                    // send json results to tableResCallback
                    var resultsClient = new MsiClientResults(resultUrl, jobId, failureCallback1);
                    resultsClient.execGetTableResultsJsonTableRes(null,
                                                                  ngResultsSuccessCallback.bind(this, tableResCallback1, progressCallback1));

                }.bind(this, jobId, tableResCallback0, failureCallback0, progressCallback0, checkForCancelCallback0, resultUrl);

                progressCallback0(5); // got jobId
                var sProgress = MsiClientNodeGroupExec.scaleProgress.bind(this, progressCallback0, 10, 90);

                // call status service loop
                var statusClient = new MsiClientStatus(statusUrl, jobId, failureCallback0);
                statusClient.execAsyncWaitUntilDone(ngStatusSuccessCallback, checkForCancelCallback0, sProgress);

            }.bind(this, tableResCallback, failureCallback, progressCallback, checkForCancelCallback, statusUrl, resultUrl);

            return ngExecJobIdCallback;
        };


        /*
         * just like buildCsvUrlSampleJsonCallback
         * EXCEPT:
         *    no max rows or csv URL
         *    jsonLdCallback(jsonLdRes)
         */
        MsiClientNodeGroupExec.buildJsonLdCallback = function(jsonLdCallback, failureCallback, progressCallback, checkForCancelCallback, statusUrl, resultUrl) {

            // callback for the nodegroup execution service to send jobId
            var ngExecJobIdCallback = function(jsonLdCallback0, failureCallback0, progressCallback0, checkForCancelCallback0, statusUrl, resultUrl, jobId) {

                // callback for status service after job successfully finishes
                var ngStatusSuccessCallback = function(jobId, jsonLdCallback1, failureCallback1, progressCallback1, checkForCancelCallback1, resultUrl) {

                    // callback for results service
                    var ngResultsSuccessCallback = function (jsonLdCallback2, progressCallback2, results) {
                        progressCallback2(99);
                        jsonLdCallback2(results);
                    };

                    // send json results to jsonLdCallback
                    var resultsClient = new MsiClientResults(resultUrl, jobId, failureCallback1);
                    resultsClient.execGetJsonLdRes( ngResultsSuccessCallback.bind(this, jsonLdCallback1, progressCallback1) );

                }.bind(this, jobId, jsonLdCallback0, failureCallback0, progressCallback0, checkForCancelCallback0, resultUrl);

                progressCallback0(5); // got jobId
                var sProgress = MsiClientNodeGroupExec.scaleProgress.bind(this, progressCallback0, 10, 90);

                // call status service loop
                var statusClient = new MsiClientStatus(statusUrl, jobId, failureCallback0);
                statusClient.execAsyncWaitUntilDone(ngStatusSuccessCallback, checkForCancelCallback0, sProgress);

            }.bind(this, jsonLdCallback, failureCallback, progressCallback, checkForCancelCallback, statusUrl, resultUrl);

            return ngExecJobIdCallback;
        };


		MsiClientNodeGroupExec.prototype = {


            execGetJobCompletionPercentage : function(jobId, percentCallback, failureCallback) {

                // Callback checks for success and gets a percent int before calling percentCallback
                var successCallback = function(percCallback, fCallback, resultSet) {
                    if (resultSet.isSuccess()) {
                        var thisPercent = resultSet.getSimpleResultField("percent");
                        if (thisPercent == null) {
                            fCallback(resultSet.getFailureHtml("did not return a percent."));
                        } else {
                            percCallback(parseInt(thisPercent));
                        }
                    } else {
                        fCallback(resultSet.getFailureHtml());
                    }
                }.bind(this, percentCallback, failureCallback);

                this.execJobId("getJobCompletionPercentage", jobId, successCallback, failureCallback);
            },


            execJobStatus :  function(jobId, successBoolCallback, failureCallback) {

                var successCallback = function(successBoolCallback0, failureCallback0, resultSet) {

                    // job is finished
                    if (resultSet.isSuccess()) {
                        var status = resultSet.getSimpleResultField("status");

                        if ( status == null) {
                            failureCallback0(resultSet.getFailureHtml("did not return a status."));
                        } else if (status == "Success") {
                            successBoolCallback0(true);
                        } else {
                            successBoolCallback0(false);
                        }
                    } else {
                        failureCallback0(resultSet.getFailureHtml());
                    }
                }.bind(this, successBoolCallback, failureCallback);

                this.execJobId("jobStatus", jobId, successCallback, failureCallback);
            },

            execJobStatusMessage :  function(jobId, messageCallback, failureCallback) {

                var successCallback = function(messageCallback0, failureCallback0, resultSet) {

                    // job is finished
                    if (resultSet.isSuccess()) {
                        var message = resultSet.getSimpleResultField("message");

                        if ( message == null) {
                            failureCallback0(resultSet.getFailureHtml("did not return a message."));
                        } else {
                            messageCallback0(message);
                        }
                    } else {
                        failureCallback0(resultSet.getFailureHtml());
                    }
                }.bind(this, messageCallback, failureCallback);

                this.execJobId("jobStatusMessage", jobId, successCallback, failureCallback);
            },

            /*==========  functions with jobIdCallback ============*/
            execAsyncDispatchConstructFromNodeGroup : function(nodegroup, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback) {
                this.runAsyncNodegroup("dispatchConstructFromNodegroup",
                                        nodegroup, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback);
            },
            execAsyncDispatchSelectFromNodeGroup : function(nodegroup, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback) {
                this.runAsyncNodegroup("dispatchSelectFromNodegroup",
                                        nodegroup, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback);
            },

            execAsyncDispatchDeleteFromNodeGroup : function(nodegroup, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback) {
                this.runAsyncNodegroup("dispatchDeleteFromNodegroup",
                                        nodegroup, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback);
            },

            execAsyncDispatchCountFromNodeGroup : function(nodegroup, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback) {
                this.runAsyncNodegroup("dispatchCountFromNodegroup",
                                        nodegroup, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback);
            },

            execAsyncDispatchFilterFromNodeGroup : function(nodegroup, conn, sparqlId, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback) {
                this.runAsyncNodegroupSparqlId("dispatchFilterFromNodegroup",
                                        nodegroup, sparqlId, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback);
            },

            execAsyncDispatchSelectInstanceData : function(conn, classValues, predicateValues, limitOverride, offsetOverride, countOnly, jobIdCallback, failureCallback) {
                this.runAsyncNodegroupInstanceData("dispatchSelectInstanceData",
                                         conn, classValues, predicateValues, limitOverride, offsetOverride, countOnly, jobIdCallback, failureCallback);
            },
            execAsyncDispatchSelectInstanceDataSubjects : function(conn, classValues, limitOverride, offsetOverride, countOnly, jobIdCallback, failureCallback) {
                this.runAsyncNodegroupInstanceDataSubjects("dispatchSelectInstanceDataSubjects",
                                         conn, classValues, limitOverride, offsetOverride, countOnly, jobIdCallback, failureCallback);
            },
            execAsyncDispatchSelectInstanceDataPredicates : function(conn, predicateList, limitOverride, offsetOverride, countOnly, jobIdCallback, failureCallback) {
                this.runAsyncNodegroupInstanceDataPredicates("dispatchSelectInstanceDataPredicates",
                                         conn, predicateList, limitOverride, offsetOverride, countOnly, jobIdCallback, failureCallback);
            },

            execAsyncDispatchRawSparql : function(sparql, conn, jobIdCallback, failureCallback) {
                this.runAsyncSparql("dispatchRawSparql",
                                        sparql, conn, jobIdCallback, failureCallback);
            },

            execAsyncDispatchSelectById : function(nodegroupId, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback) {
                this.runAsyncNodegroupId("dispatchSelectById",
                                         nodegroupId, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback);
            },

            execAsyncDispatchClearGraph : function(sei, jobIdCallback, failureCallback) {
                this.runAsyncSei(   "dispatchClearGraph",
                                    sei, jobIdCallback, failureCallback);
            },

            /* ===================================================================================== */

            /*
             * successCallback(resultSet)
             * @private
             */
            execJobId : function (endpoint, jobId, successCallback, failureCallback) {
                var data = JSON.stringify ({
                    "jobID": jobId.toString()
                });

                this.msi.postToEndpoint(endpoint, data, "application/json",
                                        successCallback,
                                        failureCallback,
                                        this.optTimeout);
            },

            runAsyncSei : function (endpoint, sei, jobIdCallback, failureCallback) {
                var data = JSON.stringify ({
                    "graph":           sei.getGraph(),
                    "serverAndPort":   sei.getServerURL(),
                    "serverType" :     sei.getServerType()
                });

				this.runAsync(endpoint, data, jobIdCallback, failureCallback);
            },

            runAsyncSparql : function (endpoint, sparql, conn, jobIdCallback, failureCallback) {

				var data = JSON.stringify ({
                    "sparql":           sparql,
                    "sparqlConnection": JSON.stringify(conn.toJson()),
                });

				this.runAsync(endpoint, data, jobIdCallback, failureCallback);
			},

            /**
              * Package data for endpoint requiring nodegroup, conn, edcConstraints, runtimeconstraints
              * and call runAsync()
              *
              * csvUrlSampleJsonCallback(table)
              * failureCallback(html)
              * statusCallback(percentCompleteInt)     hint for statusCallback: setStatus.bind(this, "Running an XYZ")
              *
              * @private
              */
			runAsyncNodegroup : function (endpoint, nodegroup, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback) {

				var data = JSON.stringify ({
                    "jsonRenderedNodeGroup": JSON.stringify(nodegroup.toJson()),
                    "sparqlConnection":      JSON.stringify(conn.toJson()),
                    "runtimeConstraints":    (typeof runtimeConstraints == "undefined" || runtimeConstraints == null) ? "" : JSON.stringify(runtimeConstraints.toJson()),
                    "externalDataConnectionConstraints": (typeof edcConstraints == "undefined" || edcConstraints == null) ? "" : JSON.stringify(edcConstraints.toJson())
                });

				this.runAsync(endpoint, data, jobIdCallback, failureCallback);
			},

            runAsyncNodegroupId : function (endpoint, nodegroupId, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback) {

				var data = JSON.stringify ({
                    "nodeGroupId": nodegroupId,
                    "sparqlConnection":      JSON.stringify(conn.toJson()),
                    "runtimeConstraints":    (typeof runtimeConstraints == "undefined" || runtimeConstraints == null) ? "" : JSON.stringify(runtimeConstraints.toJson()),
                    "externalDataConnectionConstraints": (typeof edcConstraints == "undefined" || edcConstraints == null) ? "" : JSON.stringify(edcConstraints.toJson())
                });

				this.runAsync(endpoint, data, jobIdCallback, failureCallback);
			},

            runAsyncNodegroupSparqlId : function (endpoint, nodegroup, sparqlId, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback) {

				var data = JSON.stringify ({
                    "jsonRenderedNodeGroup": JSON.stringify(nodegroup.toJson()),
                    "sparqlConnection":      JSON.stringify(conn.toJson()),
                    "targetObjectSparqlId":  sparqlId,
                    "runtimeConstraints":    (typeof runtimeConstraints == "undefined" || runtimeConstraints == null) ? "" : JSON.stringify(runtimeConstraints.toJson()),
                    "externalDataConnectionConstraints": (typeof edcConstraints == "undefined" || edcConstraints == null) ? "" : JSON.stringify(edcConstraints.toJson())
                });

				this.runAsync(endpoint, data, jobIdCallback, failureCallback);
			},

            runAsyncNodegroupInstanceData : function (endpoint, conn, classValues, predicateValues, limit, offset, countOnly, jobIdCallback, failureCallback) {

                var data = {}
				data.conn = JSON.stringify(conn.toJson());

                if (typeof classValues != "undefined" && classValues.length > 0) {
                    data.classValues = classValues;
                }
                if (typeof predicateValues != "undefined" && predicateValues.length > 0) {
                    data.predicateValues = predicateValues;
                }
                if (typeof limit != "undefined") {
                    data.limitOverride = limit;
                }
                if (typeof offset != "undefined") {
                    data.offsetOverride = offset;
                }
                if (typeof countOnly != "undefined") {
                    data.countOnly = countOnly;
                }


				this.runAsync(endpoint, JSON.stringify(data), jobIdCallback, failureCallback);
			},

            runAsyncNodegroupInstanceDataSubjects : function (endpoint, conn, classValues, limit, offset, countOnly, jobIdCallback, failureCallback) {

                var data = {}
				data.conn = JSON.stringify(conn.toJson());

                if (typeof classValues != "undefined" && classValues.length > 0) {
                    data.classValues = classValues;
                }
                if (typeof limit != "undefined") {
                    data.limitOverride = limit;
                }
                if (typeof offset != "undefined") {
                    data.offsetOverride = offset;
                }
                if (typeof countOnly != "undefined") {
                    data.countOnly = countOnly;
                }

				this.runAsync(endpoint, JSON.stringify(data), jobIdCallback, failureCallback);
			},

            runAsyncNodegroupInstanceDataPredicates : function (endpoint, conn, predicateList, limit, offset, countOnly, jobIdCallback, failureCallback) {

                var data = {}
				data.conn = JSON.stringify(conn.toJson());

                if (typeof predicateList != "undefined" && predicateList.length > 0) {
                    var sendPredList = [];
                    for (var pair of predicateList) {
                        sendPredList.push({domainURI: pair[0], predicateURI: pair[1]});
                    }
                    data.predicateList = sendPredList;
                }
                if (typeof limit != "undefined") {
                    data.limitOverride = limit;
                }
                if (typeof offset != "undefined") {
                    data.offsetOverride = offset;
                }
                if (typeof countOnly != "undefined") {
                    data.countOnly = countOnly;
                }

				this.runAsync(endpoint, JSON.stringify(data), jobIdCallback, failureCallback);
			},


            /*
             * start an async chain where results service is used.
             * @private
             */
            runAsync : function (endpoint, data, jobIdCallback, failureCallback) {

                this.msi.postToEndpoint(endpoint, data, "application/json",
                                        this.runAsyncJobIdCallback.bind(this, endpoint, jobIdCallback, failureCallback),
                                        failureCallback,
                                        this.optTimeout);
            },

            /*
             * first callback in runAsync chain
             * Expecting to receive the jobId
             * @private
             */
            runAsyncJobIdCallback : function (endpoint, jobIdCallback, failureCallback, resultSet) {
                if (resultSet.isSuccess()) {
                    // get the jobId
                    var jobId = resultSet.getSimpleResultField("JobId");
                    if (jobId) {
                        jobIdCallback(jobId);
                    } else {
                        failureCallback(resultSet.getFailureHtml("did not return a requestID."));
                    }
                } else {
                    failureCallback(resultSet.getFailureHtml());
                }
            },

            runAsynctableResCallback : function (tableRes) {
                this.tableResCallback(tableRes);
            },
		};

		return MsiClientNodeGroupExec;            // return the constructor
	}
);
