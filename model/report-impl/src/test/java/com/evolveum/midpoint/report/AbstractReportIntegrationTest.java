/**
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.report;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.report.impl.ReportJasperCreateTaskHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class AbstractReportIntegrationTest extends AbstractModelIntegrationTest {

	protected final static File TEST_DIR_COMMON = new File("src/test/resources/common");
	protected final static File EXPORT_DIR = new File("target/midpoint-home/export");

	protected final static File TEST_REPOSTS_DIR = new File("src/test/resources/reports");
		
	protected final static File REPORT_USER_LIST_FILE = new File(TEST_REPOSTS_DIR, "report-user-list.xml"); 
	protected final static String REPORT_USER_LIST_OID = "00000000-0000-0000-0000-000000000110";
	
	protected final static File REPORT_USER_LIST_EXPRESSIONS_CSV_FILE = new File(TEST_REPOSTS_DIR, "report-user-list-expressions-csv.xml"); 
	protected final static String REPORT_USER_LIST_EXPRESSIONS_CSV_OID = "8fa48180-4f17-11e9-9eed-3fb4721a135e";
	
	protected final static File REPORT_USER_LIST_EXPRESSIONS_POISONOUS_QUERY_CSV_FILE = new File(TEST_REPOSTS_DIR, "report-user-list-expressions-poisonous-query-csv.xml"); 
	protected final static String REPORT_USER_LIST_EXPRESSIONS_POISONOUS_QUERY_CSV_OID = "5c5af02a-4fe9-11e9-bb07-7b4e52fe05cd";
	
	protected final static File REPORT_USER_LIST_EXPRESSIONS_POISONOUS_FIELD_CSV_FILE = new File(TEST_REPOSTS_DIR, "report-user-list-expressions-poisonous-field-csv.xml"); 
	protected final static String REPORT_USER_LIST_EXPRESSIONS_POISONOUS_FIELD_CSV_OID = "76c58132-4fe9-11e9-86fe-ff36d221f673";
	
	protected final static File REPORT_USER_LIST_SCRIPT_FILE = new File(TEST_REPOSTS_DIR, "report-user-list-script.xml"); 
	protected final static String REPORT_USER_LIST_SCRIPT_OID = "222bf2b8-c89b-11e7-bf36-ebd4e4d45a80";

	protected final static File USER_JACK_FILE = new File(TEST_DIR_COMMON, "user-jack.xml"); 
	protected final static String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	
	protected final static File USERS_MONKEY_ISLAND_FILE = new File(TEST_DIR_COMMON, "users-monkey-island.xml");
	
	protected final static File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR_COMMON, "system-configuration.xml");
	protected final static File SYSTEM_CONFIGURATION_SAFE_FILE = new File(TEST_DIR_COMMON, "system-configuration-safe.xml");

	protected final static File RESOURCE_OPENDJ_FILE = new File(TEST_DIR_COMMON, "resource-opendj.xml");
	protected final static String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

	protected final static File CONNECTOR_DUMMY_FILE = new File(TEST_DIR_COMMON, "connector-ldap.xml");
	protected final static String CONNECTOR_DUMMY_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3eedd";

	protected final static File ROLE_SUPERUSER_FILE = new File(TEST_DIR_COMMON, "role-superuser.xml");

	protected final static File USER_ADMINISTRATOR_FILE = new File(TEST_DIR_COMMON, "user-administrator.xml");

	@Autowired protected ReportManager reportManager;
	@Autowired
	@Qualifier("reportJasperCreateTaskHandler")
	protected ReportJasperCreateTaskHandler reportTaskHandler;
	
	protected PrismObject<UserType> userAdministrator;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		repoAddObjectFromFile(USER_JACK_FILE, true, initResult).asObjectable();
		repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
		// System Configuration
		modelService.postInit(initResult);
		try {
			repoAddObjectFromFile(getSystemConfigurationFile(), initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}

		// User administrator
		userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);

		login(userAdministrator);

		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
	}

	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_FILE;
	}

	@Override
	protected PrismObject<UserType> getDefaultActor() {
		return userAdministrator;
	}
}
