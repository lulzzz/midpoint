/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.wf.impl.activiti.dao;

import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.activiti.engine.FormService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;

/**
 * @author mederly
 */

@Component
public class WorkItemManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(WorkItemManager.class);

    @Autowired
    private ActivitiEngine activitiEngine;

    @Autowired
    private MiscDataUtil miscDataUtil;
    
    @Autowired
    private SecurityEnforcer securityEnforcer;
    
    @Autowired
	private SystemObjectCache systemObjectCache;

    @Autowired
	private PrismContext prismContext;

    @Autowired
	private WorkItemProvider workItemProvider;

    private static final String DOT_INTERFACE = WorkflowManager.class.getName() + ".";

    private static final String OPERATION_COMPLETE_WORK_ITEM = DOT_INTERFACE + "completeWorkItem";
    private static final String OPERATION_CLAIM_WORK_ITEM = DOT_INTERFACE + "claimWorkItem";
    private static final String OPERATION_RELEASE_WORK_ITEM = DOT_INTERFACE + "releaseWorkItem";
    private static final String OPERATION_DELEGATE_WORK_ITEM = DOT_INTERFACE + "delegateWorkItem";

    public void completeWorkItem(String workItemId, String decision, String comment, ObjectDelta additionalDelta,
			OperationResult parentResult) throws SecurityViolationException, SchemaException {

        OperationResult result = parentResult.createSubresult(OPERATION_COMPLETE_WORK_ITEM);
        result.addParams(new String[] { "workItemId", "decision", "comment" }, workItemId, decision, comment);

		try {
			final String userDecription = toShortString(securityEnforcer.getPrincipal().getUser());
			result.addContext("user", userDecription);

			LOGGER.trace("Completing work item {} with decision of {} ['{}'] by {}", workItemId, decision, comment, userDecription);

			FormService formService = activitiEngine.getFormService();
			TaskFormData data = activitiEngine.getFormService().getTaskFormData(workItemId);

			String assigneeOid = data.getTask().getAssignee();
			if (!miscDataUtil.isAuthorizedToSubmit(workItemId, assigneeOid, systemObjectCache, result)) {
				throw new SecurityViolationException("You are not authorized to complete this work item.");
			}

			final Map<String, String> propertiesToSubmit = new HashMap<>();
			propertiesToSubmit.put(CommonProcessVariableNames.FORM_FIELD_DECISION, decision);
			propertiesToSubmit.put(CommonProcessVariableNames.FORM_FIELD_COMMENT, comment);
			if (additionalDelta != null) {
				ObjectDeltaType objectDeltaType = DeltaConvertor.toObjectDeltaType(additionalDelta);
				String xmlDelta = prismContext.xmlSerializer()
						.serializeRealValue(objectDeltaType, SchemaConstants.T_OBJECT_DELTA);
				propertiesToSubmit.put(CommonProcessVariableNames.FORM_FIELD_ADDITIONAL_DELTA, xmlDelta);
			}

			// we also fill-in the corresponding 'button' property (if there's one that corresponds to the decision)
			for (FormProperty formProperty : data.getFormProperties()) {
				if (formProperty.getId().startsWith(CommonProcessVariableNames.FORM_BUTTON_PREFIX)) {
					boolean value = formProperty.getId().equals(CommonProcessVariableNames.FORM_BUTTON_PREFIX + decision);
					LOGGER.trace("Setting the value of {} to writable property {}", value, formProperty.getId());
					propertiesToSubmit.put(formProperty.getId(), Boolean.toString(value));
				}
			}
			LOGGER.trace("Submitting {} properties", propertiesToSubmit.size());
			formService.submitTaskFormData(workItemId, propertiesToSubmit);
		} catch (SecurityViolationException|SchemaException|RuntimeException e) {
			result.recordFatalError("Couldn't complete the work item " + workItemId + ": " + e.getMessage(), e);
			throw e;
		} finally {
			result.computeStatusIfUnknown();
		}
    }

    public void claimWorkItem(String workItemId, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OPERATION_CLAIM_WORK_ITEM);
        result.addParam("workItemId", workItemId);
		try {
			MidPointPrincipal principal = securityEnforcer.getPrincipal();
			result.addContext("user", toShortString(principal.getUser()));

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Claiming work item {} by {}", workItemId, toShortString(principal.getUser()));
			}

			TaskService taskService = activitiEngine.getTaskService();
			Task task = taskService.createTaskQuery().taskId(workItemId).singleResult();
			if (task == null) {
				throw new ObjectNotFoundException("The work item does not exist");
			}
			if (task.getAssignee() != null) {
				String desc = task.getAssignee().equals(principal.getOid()) ? "the current" : "another";
				throw new SystemException("The work item is already assigned to "+desc+" user");
			}
			if (!miscDataUtil.isAuthorizedToClaim(task.getId())) {
				throw new SecurityViolationException("You are not authorized to claim the selected work item.");
			}
			taskService.claim(workItemId, principal.getOid());
		} catch (ObjectNotFoundException|SecurityViolationException|RuntimeException e) {
			result.recordFatalError("Couldn't claim the work item " + workItemId + ": " + e.getMessage(), e);
			throw e;
		} finally {
			result.computeStatusIfUnknown();
		}
	}

    public void releaseWorkItem(String workItemId, OperationResult parentResult) throws ObjectNotFoundException, SecurityViolationException {
        OperationResult result = parentResult.createSubresult(OPERATION_RELEASE_WORK_ITEM);
        result.addParam("workItemId", workItemId);
		try {
			MidPointPrincipal principal = securityEnforcer.getPrincipal();
			result.addContext("user", toShortString(principal.getUser()));

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Releasing work item {} by {}", workItemId, toShortString(principal.getUser()));
			}

            TaskService taskService = activitiEngine.getTaskService();
            Task task = taskService.createTaskQuery().taskId(workItemId).singleResult();
            if (task == null) {
				throw new ObjectNotFoundException("The work item does not exist");
            }
            if (task.getAssignee() == null) {
				throw new SystemException("The work item is not assigned to a user");
            }
            if (!task.getAssignee().equals(principal.getOid())) {
                throw new SystemException("The work item is not assigned to the current user");
            }
            boolean candidateFound = false;
            for (IdentityLink link : taskService.getIdentityLinksForTask(workItemId)) {
                if (IdentityLinkType.CANDIDATE.equals(link.getType())) {
                    candidateFound = true;
                    break;
                }
            }
            if (!candidateFound) {
                throw new SystemException("It has no candidates to be offered to");
            }
            taskService.unclaim(workItemId);
        } catch (ObjectNotFoundException|SecurityViolationException|RuntimeException e) {
            result.recordFatalError("Couldn't release work item " + workItemId + ": " + e.getMessage(), e);
			throw e;
        } finally {
			result.computeStatusIfUnknown();
		}
    }

	public void delegateWorkItem(String workItemId, List<PrismReferenceValue> delegates, OperationResult parentResult)
			throws ObjectNotFoundException, SecurityViolationException {
		OperationResult result = parentResult.createSubresult(OPERATION_DELEGATE_WORK_ITEM);
		result.addParam("workItemId", workItemId);
		result.addCollectionOfSerializablesAsParam("delegates", delegates);
		try {
			MidPointPrincipal principal = securityEnforcer.getPrincipal();
			result.addContext("user", toShortString(principal.getUser()));

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Delegating work item {} to {}", workItemId, delegates);
			}

			TaskService taskService = activitiEngine.getTaskService();
			Task task = taskService.createTaskQuery().taskId(workItemId).singleResult();
			// TODO check authority

			List<PrismReferenceValue> currentDelegates = new ArrayList<>(workItemProvider.getDelegates(task));
			for (PrismReferenceValue delegate : delegates) {
				if (delegate.getTargetType() != null && !QNameUtil.match(UserType.COMPLEX_TYPE, delegate.getTargetType())) {
					throw new IllegalArgumentException("Couldn't add non-user reference as a delegate: " + delegate);
				}
				if (delegate.getOid() == null) {
					throw new IllegalArgumentException("Couldn't add no-OID reference as a delegate: " + delegate);
				}
				if (!PrismReferenceValue.containsOid(currentDelegates, delegate.getOid())) {
					currentDelegates.add(delegate);
				}
			}
			String delegatesAsString = currentDelegates.stream()
					.map(d -> "[" + d.getOid() + "]")
					.collect(Collectors.joining(WorkItemProvider.DELEGATE_SEPARATOR));
			taskService.setVariableLocal(task.getId(), WorkItemProvider.DELEGATE_VARIABLE_NAME, delegatesAsString);
		} catch (SecurityViolationException|RuntimeException e) {
			result.recordFatalError("Couldn't delegate work item " + workItemId + ": " + e.getMessage(), e);
			throw e;
		} finally {
			result.computeStatusIfUnknown();
		}
	}

}
