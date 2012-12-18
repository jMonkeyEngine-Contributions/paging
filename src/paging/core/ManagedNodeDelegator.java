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
			delegateAddRemove(tpf);
			if (fTaskCreate == null) {
				if (!exec.isTerminating() && !exec.isShutdown()) {
					fTaskCreate = exec.submit(new Callable() {
						@Override
						public Void call() throws Exception {
							Set<Vector3f> keys = tiles.keySet();
							for (Vector3f key : keys) {
								DelegatorTask task = tiles.get(key);
								float distance = cam.getLocation().distance(task.getNode().getLocalTranslation());
								if (task.getNode().getParent() == ((Node)spatial)) {
									if (distance > maxDistance && !tileRemove.contains(task)) {
										// Remove node
										tileRemove.add(task);
									}
								} else {
									if (distance <= maxDistance && !tileAdd.contains(task)) {
										tileAdd.add(task);
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
	
	protected void delegateAddRemove(float tpf) {
		// Poll managed nodes and remove tile from scene
		if (!tileRemove.isEmpty()) {
			DelegatorTask task = tileRemove.poll();
			
			if (task.getNode() != null) {
				task.getNode().removeFromParent();
				
				// Physics
				if (pm.getManagePhysics() && managePhysics) {
					pm.getPhysicsSpace().remove(task.getPhysicsNode());
				}

				// Notify dependants


				// Notify listeners
				for (DelegatorListener l : listeners) {
					l.onRemoveFromScene(task.getNode());
				}
			}
		}
		// Poll managed nodes and add tile to scene
		if (!tileAdd.isEmpty()) {
			DelegatorTask task = tileAdd.poll();
			
		//	task.setStage(STAGE.COMPLETE);
			((Node)spatial).attachChild(task.getNode());
			
			// Physics
			if (pm.getManagePhysics() && managePhysics) {
				pm.getPhysicsSpace().add(task.getPhysicsNode());
			}
			
			// Notify dependants
			
			
			// Notify listeners
			for (DelegatorListener l : listeners) {
				l.onAddToScene(task.getNode());
			}
		}
	}
	
	protected void delegateTasks(float tpf) {
		
	}
	
	public synchronized void addManagedNode(ManagedNode node) {
		System.out.println("Adding node at : " + node.getLocalTranslation());
		DelegatorTask task = new DelegatorTask(this, node.getLocalTranslation(), LOD.LOD_1, LOD.LOD_1);
		task.setNode(node);
		task.setStage(STAGE.COMPLETE);
		tiles.put(node.getLocalTranslation(), task);
	}
	
	public void removeManagedNode(Vector3f location) {
		tiles.remove(location);
	}
	
	public void removeManagedNode(ManagedNode node) {
		tiles.remove(node.getLocalTranslation());
	}
	
	// Dependent delegator methods
	@Override
	public void onParentNotifyCreate(DelegatorTask task) {
		tileAdd.add(tiles.get(task.getPosition()));
	}
}
