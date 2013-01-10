package com.evolveum.midpoint.provisioning.test.mock;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationListener;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

@Service(value = "syncServiceMock")
public class SynchornizationServiceMock implements ResourceObjectChangeListener, ResourceOperationListener {

	private static final Trace LOGGER = TraceManager.getTrace(SynchornizationServiceMock.class);

	private int callCount = 0;
	private ResourceObjectShadowChangeDescription lastChange = null;
	private ResourceOperationDescription lastOperationDescription = null;
	private ObjectChecker changeChecker;

	@Autowired(required=true)
	ChangeNotificationDispatcher notificationManager;
	@Autowired(required=true)
	RepositoryService repositoryService;
	
	@PostConstruct
	public void register() {
		notificationManager.registerNotificationListener((ResourceObjectChangeListener)this);
		notificationManager.registerNotificationListener((ResourceOperationListener)this);
	}

	@PreDestroy
	public void unregister() {
		notificationManager.unregisterNotificationListener((ResourceObjectChangeListener)this);
		notificationManager.unregisterNotificationListener((ResourceOperationListener)this);
	}
	
	public ObjectChecker getChangeChecker() {
		return changeChecker;
	}

	public void setChangeChecker(ObjectChecker changeChecker) {
		this.changeChecker = changeChecker;
	}

	@Override
	public void notifyChange(ResourceObjectShadowChangeDescription change, Task task,
			OperationResult parentResult) {
		LOGGER.debug("Notify change mock called with {}", change);

		// Some basic sanity checks
		assertNotNull("No change", change);
		assertNotNull("No task", task);
		assertNotNull("No resource", change.getResource());
		assertNotNull("No parent result", parentResult);

		assertTrue("Either current shadow or delta must be present", change.getCurrentShadow() != null
				|| change.getObjectDelta() != null);
		if (change.getCurrentShadow() != null) {
			ResourceObjectShadowType currentShadowType = change.getCurrentShadow().asObjectable();
			if (currentShadowType != null) {
				// not a useful check..the current shadow could be null
				assertNotNull("Current shadow does not have an OID", change.getCurrentShadow().getOid());
				assertNotNull("Current shadow does not have resourceRef", currentShadowType.getResourceRef());
				assertNotNull("Current shadow has null attributes", currentShadowType.getAttributes());
				assertFalse("Current shadow has empty attributes", ResourceObjectShadowUtil
						.getAttributesContainer(currentShadowType).isEmpty());

				// Check if the shadow is already present in repo
				try {
					repositoryService.getObject(currentShadowType.getClass(), currentShadowType.getOid(), new OperationResult("mockSyncService.notifyChange"));
				} catch (Exception e) {
					AssertJUnit.fail("Got exception while trying to read current shadow "+currentShadowType+
							": "+e.getCause()+": "+e.getMessage());
				}			
				// Check resource
				String resourceOid = ResourceObjectShadowUtil.getResourceOid(currentShadowType);
				assertFalse("No resource OID in current shadow "+currentShadowType, StringUtils.isBlank(resourceOid));
				try {
					repositoryService.getObject(ResourceType.class, resourceOid, new OperationResult("mockSyncService.notifyChange"));
				} catch (Exception e) {
					AssertJUnit.fail("Got exception while trying to read resource "+resourceOid+" as specified in current shadow "+currentShadowType+
							": "+e.getCause()+": "+e.getMessage());
				}

				if (change.getCurrentShadow().asObjectable() instanceof AccountShadowType) {
					AccountShadowType account = (AccountShadowType) change.getCurrentShadow().asObjectable();
					assertNotNull("Current shadow does not have activation", account.getActivation());
					assertNotNull("Current shadow activation/enabled is null", account.getActivation()
							.isEnabled());
				} else {
					// We don't support other types now
					AssertJUnit.fail("Unexpected type of shadow " + change.getCurrentShadow().getClass());
				}
			}
		}
		if (change.getOldShadow() != null) {
			assertNotNull("Old shadow does not have an OID", change.getOldShadow().getOid());
			assertNotNull("Old shadow does not have an resourceRef", change.getOldShadow().asObjectable()
					.getResourceRef());
		}
		if (change.getObjectDelta() != null) {
			assertNotNull("Delta has null OID", change.getObjectDelta().getOid());
		}
		
		if (changeChecker != null) {
			changeChecker.check(change);
		}

		// remember ...
		callCount++;
		lastChange = change;
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#notifyFailure(com.evolveum.midpoint.provisioning.api.ResourceObjectShadowFailureDescription, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public void notifySuccess(ResourceOperationDescription opDescription,
			Task task, OperationResult parentResult) {
		notifyOp("success", opDescription, task, parentResult);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#notifyFailure(com.evolveum.midpoint.provisioning.api.ResourceObjectShadowFailureDescription, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public void notifyFailure(ResourceOperationDescription opDescription,
			Task task, OperationResult parentResult) {
		notifyOp("failure", opDescription, task, parentResult);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#notifyFailure(com.evolveum.midpoint.provisioning.api.ResourceObjectShadowFailureDescription, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public void notifyInProgress(ResourceOperationDescription opDescription,
			Task task, OperationResult parentResult) {
		notifyOp("in-progress", opDescription, task, parentResult);
	}

		
	private void notifyOp(String notificationDesc, ResourceOperationDescription opDescription,
			Task task, OperationResult parentResult) {
		LOGGER.debug("Notify "+notificationDesc+" mock called with {}", opDescription);

		// Some basic sanity checks
		assertNotNull("No op description", opDescription);
		assertNotNull("No task", task);
		assertNotNull("No result", opDescription.getResult());
		assertNotNull("No resource", opDescription.getResource());
		assertNotNull("No parent result", parentResult);

		assertNotNull("Current shadow not present", opDescription.getCurrentShadow());
		assertNotNull("Delta not present", opDescription.getObjectDelta());
		if (opDescription.getCurrentShadow() != null) {
			ResourceObjectShadowType currentShadowType = opDescription.getCurrentShadow().asObjectable();
			if (currentShadowType != null) {
				// not a useful check..the current shadow could be null
				assertNotNull("Current shadow does not have an OID", opDescription.getCurrentShadow().getOid());
				assertNotNull("Current shadow does not have resourceRef", currentShadowType.getResourceRef());
				assertNotNull("Current shadow has null attributes", currentShadowType.getAttributes());
				assertFalse("Current shadow has empty attributes", ResourceObjectShadowUtil
						.getAttributesContainer(currentShadowType).isEmpty());

				// Check if the shadow is already present in repo
				try {
					repositoryService.getObject(currentShadowType.getClass(), currentShadowType.getOid(), new OperationResult("mockSyncService."+notificationDesc));
				} catch (Exception e) {
					AssertJUnit.fail("Got exception while trying to read current shadow "+currentShadowType+
							": "+e.getCause()+": "+e.getMessage());
				}			
				// Check resource
				String resourceOid = ResourceObjectShadowUtil.getResourceOid(currentShadowType);
				assertFalse("No resource OID in current shadow "+currentShadowType, StringUtils.isBlank(resourceOid));
				try {
					repositoryService.getObject(ResourceType.class, resourceOid, new OperationResult("mockSyncService."+notificationDesc));
				} catch (Exception e) {
					AssertJUnit.fail("Got exception while trying to read resource "+resourceOid+" as specified in current shadow "+currentShadowType+
							": "+e.getCause()+": "+e.getMessage());
				}

				if (opDescription.getCurrentShadow().asObjectable() instanceof AccountShadowType) {
					AccountShadowType account = (AccountShadowType) opDescription.getCurrentShadow().asObjectable();
					assertNotNull("Current shadow does not have activation", account.getActivation());
					assertNotNull("Current shadow activation/enabled is null", account.getActivation()
							.isEnabled());
				} else {
					// We don't support other types now
					AssertJUnit.fail("Unexpected type of shadow " + opDescription.getCurrentShadow().getClass());
				}
			}
		}
		if (opDescription.getObjectDelta() != null) {
			assertNotNull("Delta has null OID", opDescription.getObjectDelta().getOid());
		}
		
		if (changeChecker != null) {
			changeChecker.check(opDescription);
		}

		// remember ...
		callCount++;
		lastOperationDescription = opDescription;
	}

	public boolean wasCalled() {
		return (callCount > 0);
	}

	public void reset() {
		callCount = 0;
		lastChange = null;
	}

	public ResourceObjectShadowChangeDescription getLastChange() {
		return lastChange;
	}

	public void setLastChange(ResourceObjectShadowChangeDescription lastChange) {
		this.lastChange = lastChange;
	}

	public int getCallCount() {
		return callCount;
	}

	public void setCallCount(int callCount) {
		this.callCount = callCount;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#getName()
	 */
	@Override
	public String getName() {
		return "synchronization service mock";
	}

}
