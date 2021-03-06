/**
 * Copyright (c) 2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.common;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;

/**
 * @author semancik
 *
 */
@Component
public class ConstantsManager {

	@Autowired
	private MidpointConfiguration midpointConfiguration;

	private Configuration constConfig;

	public ConstantsManager() {
	}

	/**
	 * For testing.
	 */
	public ConstantsManager(Configuration config) {
		this.constConfig = config;
	}

	private Configuration getConstConfig() {
		if (constConfig == null) {
			constConfig = midpointConfiguration.getConfiguration(MidpointConfiguration.CONSTANTS_CONFIGURATION);
		}
		return constConfig;
	}

	public String getConstantValue(String constName) {
		String val = getConstConfig().getString(constName);
		return val;
	}

}
