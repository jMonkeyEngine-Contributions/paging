/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package paging.core;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.Set;
import java.util.concurrent.Callable;
import paging.core.PagingManager.LOD;
import paging.core.spatials.ManagedNode;
import paging.core.tasks.DelegatorTask;
import paging.core.tasks.DelegatorTask.STAGE;

/**
 *
 * @author t0neg0d
 */
public abstract class ManagedNodeDelegator extends Delegator {
	
	@Override
	protected void initialize() {
		prepopulated = true;
	}
	
	@Override
	public void update(float tpf) {
		// Handle Managed Nodes
		if (!tiles.isEmpty()) {
			if (fTaskCreate == null) {
				if (!exec.isTerminating() && !exec.isShutdown()) {
					fTaskCreate = exec.submit(new Callable() {
						// Check existing nodes and queue add/remove of tiles
						@Override
						public Void call() throws Exception {
							Set<Vector3f> keys = tiles.keySet();
							for (Vector3f key : keys) {
								DelegatorTask task = tiles.get(key);
								float distance = cam.getLocation().distance(task.getNode().getLocalTranslation());
								if (task.getNode().getParent() == ((Node)spatial)) {
									if (distance > maxDistance && !pm.tileRemove.contains(task)) {
										// Remove node
										pm.tileRemove.add(task);
									}
								} else {
									if (distance <= maxDistance && !pm.tileAdd.contains(task)) {
										pm.tileAdd.add(task);
									}
								}
							}
							return null;
						}
					});
				}
			} else if (fTaskCreate.isDone()) {
				fTaskCreate = null;
			}
		}
	}
	
	/**
	 * Public method for adding ManagedNode(s) to the pool of nodes this delegator
	 * is responsible for
	 * @param node The ManagedNode to add to the pool
	 */
	public void addManagedNode(ManagedNode node) {
		DelegatorTask task = new DelegatorTask(this, node.getLocalTranslation(), LOD.LOD_1, LOD.LOD_1);
		task.setNode(node);
		task.setStage(STAGE.COMPLETE);
		tiles.put(node.getLocalTranslation(), task);
	}
	/**
	 * Public method for removing and ManagedNode by location from the pool 
	 * of nodes this delegator is responsible for
	 * @param location The Vector3f the ManagedNode was registered at
	 */
	public void removeManagedNode(Vector3f location) {
		tiles.remove(location);
	}
	/**
	 * Public method for removing and ManagedNode by instance from the pool 
	 * of nodes this delegator is responsible for
	 * @param node The ManagedNode instance to remove
	 */
	public void removeManagedNode(ManagedNode node) {
		tiles.remove(node.getLocalTranslation());
	}
	
	/**
	 * Dependent delegator notification.  Standard delegators check for tile 
	 * addition/creation based on distance from camera.  In the case of dependent
	 * delegators, normal distance checks are bypassed and onParentNotifyCreate
	 * is called instead.
	 * @param task The most recent task created/added by the parent delegator
	 */
	@Override
	protected void onParentNotifyCreate(DelegatorTask task) {
		pm.tileAdd.add(tiles.get(task.getPosition()));
	}
}
