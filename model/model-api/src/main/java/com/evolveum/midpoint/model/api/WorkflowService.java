package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

import java.util.List;

/**
 * @author mederly
 */
public interface WorkflowService {

    /**
     * Approves or rejects a work item (without supplying any further information).
	 * @param taskId identifier of activiti task backing the work item
	 * @param decision true = approve, false = reject
	 * @param comment
	 * @param additionalDelta
	 * @param parentResult
	 */
    void approveOrRejectWorkItem(String workItemId, boolean decision, String comment, ObjectDelta additionalDelta,
			OperationResult parentResult) throws SecurityViolationException, SchemaException;

    void stopProcessInstance(String instanceId, String username, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, SecurityViolationException;

    void claimWorkItem(String workItemId, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException;

    void releaseWorkItem(String workItemId, OperationResult parentResult) throws ObjectNotFoundException, SecurityViolationException;

	// TODO check authority in wf manager
    void delegateWorkItem(String workItemId, List<PrismReferenceValue> delegates, OperationResult parentResult) throws ObjectNotFoundException, SecurityViolationException;
}
