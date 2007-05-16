/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.servletservice.postable.components;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.excalibur.source.SourceException;
import org.apache.excalibur.source.impl.AbstractSource;

public class ServletServiceConsumerSource extends AbstractSource {
	
	private Log logger = LogFactory.getLog(getClass());
	
	private InputStream requestBody;
	
	public ServletServiceConsumerSource(HttpServletRequest request) {
		try {
			requestBody = request.getInputStream();
		} catch (Exception e) {
			logger.error("Error during obtaning request's body (POST data)", e);
		}
	}

	public boolean exists() {
		return requestBody != null;
	}
	
	public InputStream getInputStream() throws IOException, SourceException {
		if (!exists()) throw new SourceException("POST data does not exists for request. Make sure you are processing service call.");
		return requestBody;
	}

}
