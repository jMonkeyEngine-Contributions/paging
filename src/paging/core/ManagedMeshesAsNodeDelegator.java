package paging.core;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Node;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import paging.core.PagingManager.LOD;
import paging.core.spatials.ManagedMesh;
import paging.core.spatials.ManagedNode;
import paging.core.tasks.DelegatorTask;
import paging.core.tasks.DelegatorTask.STAGE;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author t0neg0d
 */
public abstract class ManagedMeshesAsNodeDelegator extends Delegator {
	
	protected abstract ManagedNode createNode(Vector3f position, ManagedNode dependantNode, Object customData);
	
	@Override
	protected void initialize() {
		prepopulated = false;
	}
	
	@Override
	public void update(float tpf) {
		// Update removal queue
		delegateUpdateRemove(tpf);

		// Handle LOD updates
		if (manageLOD) {
			delegateUpdateLOD(tpf);
		}
		
		// Call to spatial specific delegator for handling tile creation
		// ManagedMeshDelegator = create new mesh and add to queue
		// ManagedNodeDelegator = add existing managed node to queue
		delegateTasks(tpf);

		if (!isDependant) {
			// Check for tile creation and LOD updates
			camVec = new Vector3f(
					((int)FastMath.floor(cam.getLocation().x/tileSize))*tileSize,
					((int)FastMath.floor(cam.getLocation().y/tileSize))*tileSize,
					((int)FastMath.floor(cam.getLocation().z/tileSize))*tileSize
				);
			if (disableYAxis) { camVec.setY(0f); }

			// Check all potential tile locations
			for (int x = 0; x < tilesPerColumn; x++) {
				for (int z = 0; z < tilesPerColumn; z++) {
					if (!disableYAxis) {
						for (int y = 0; y < tilesPerColumn; y++) {
							Vector3f testVec = camVec.subtract(maxDistance, maxDistance, maxDistance).add((float)x*tileSize, (float)y*tileSize, (float)z*tileSize);
							checkTile(testVec);
						}
					} else {
						Vector3f testVec = camVec.subtract(maxDistance, 0f, maxDistance).add((float)x*tileSize, 0f, (float)z*tileSize);
						checkTile(testVec);
					}
				}
			}
		}

		// Call to user extended delegators
		delegatorUpdate(tpf);
	}
	
	private void checkTile(Vector3f testVec) {
		if (cam.getLocation().distance(testVec) <= maxDistance) {
			if (!tiles.containsKey(testVec)) {
				if (tileCache.containsKey(testVec)) {
					if (!pm.tileAdd.contains(tileCache.get(testVec))) {
						if (!prepopulated) { tiles.put(testVec, tileCache.get(testVec)); }
						pm.tileAdd.add(tileCache.get(testVec));
					}
				} else {
					if (!prepopulated)  { tiles.put(testVec, new DelegatorTask(this, testVec, this.LODHigh, this.LODLow)); }
				}
			} else {
				if (prepopulated) {
					DelegatorTask task = tiles.get(testVec);
					if (!pm.tileAdd.contains(task)) {
						pm.tileAdd.add(task);
					}
				}
			}
		}
	}
	
	private void delegateUpdateRemove(float tpf) {
		// Update removal queue
		if (!exec.isTerminating() && !exec.isShutdown()) {
			if (fTaskRemove == null) {
				fTaskRemove = exec.submit(new Callable() {
					@Override
					public Void call() throws Exception {
						Set<Vector3f> keys = tiles.keySet();
						for (Vector3f key : keys) {
							if (cam.getLocation().distance(key) > maxDistance) {
								DelegatorTask task = tiles.get(key);
								if (task.getStage() == STAGE.COMPLETE) {
									// Queue for removal
									if (!pm.tileRemove.contains(task)) {
										pm.tileRemove.add(task);
									}
								}
							}
						}
						return null;
					}
				});
			} else if (fTaskRemove.isDone()) {
				fTaskRemove = null;
			}
		}
	}
	
	private void delegateUpdateLOD(float tpf) {
		// Poll LODUpdates and apply lod change
		if (!LODUpdates.isEmpty()) {
			DelegatorTask task = LODUpdates.poll();
			if (task.getMesh() != null) {
				task.getMesh().updateLOD(task.getNextLOD());
			}
		}
		
		// Evaluate needed lod updates
		if (!exec.isTerminating() && !exec.isShutdown()) {
			if (fTaskLOD == null) {
				fTaskLOD = exec.submit(new Callable() {
					@Override
					public Void call() throws Exception {
						Vector3f camVec = cam.getLocation().clone();
						Set<Vector3f> keys = tiles.keySet();
						for (Vector3f key : keys) {
							if (!LODUpdates.contains(tiles.get(key))) {
								for (LOD lod : LOD.values()) {
									if (LODDistances.containsKey(lod)) {
										float distance = camVec.distance(key);
										if (distance >= LODDistances.get(lod)) {
											DelegatorTask task = tiles.get(key);
											if (task != null) {
												if (task.getStage() == STAGE.COMPLETE) {
													if (task.getNextLOD() != lod) {
														task.setNextLOD(lod);
														LODUpdates.add(task);
													}
												}
											}
											break;
										}
									}
								}
							}
						}
						return null;
					}
				});
			} else if (fTaskLOD.isDone()) {
				fTaskLOD = null;
			}
		}
	}
	
	private void delegateTasks(float tpf) {
		if (!exec.isTerminating() && !exec.isShutdown()) {
			Set<Vector3f> keys = tiles.keySet();
			for (final Vector3f key : keys) {
				DelegatorTask task = tiles.get(key);
				if (task != null) {
					if (task.getStage() != STAGE.COMPLETE) {
						switch (task.getStage()) {
							case CUSTOM:
								delegatorTaskCustomData(tpf, task);
								task.setStage(STAGE.BEGIN);
								break;
							case BEGIN:
								if (task.getFuture() == null) {
									final Object customData = task.getCustomData();
									if (this.isDependant) {
										final ManagedNode node = task.getDependentNode();
										task.setFuture(exec.submit(new Callable() {
											@Override
											public ManagedNode call() throws Exception {
												return createNode(key, node, customData);
											}
										}));
									} else {
										task.setFuture(exec.submit(new Callable() {
											@Override
											public ManagedNode call() throws Exception {
												return createNode(key, null, customData);
											}
										}));
									}
								} else if (task.getFuture().isDone()) {
									try {
										task.setNode((ManagedNode) task.getFuture().get());
									//	task.setGeometry(getGeometry());
									//	task.finialize();
										if (managePhysics) {
											task.setStage(STAGE.PHYSICS);
											task.setFuture(null);
										} else {
											task.setStage(STAGE.COMPLETE);
											task.setFuture(null);
											if (!pm.tileAdd.contains(task)) {
												pm.tileAdd.add(task);
											}
										}
										// Notify dependents
										for (Delegator d : dependents) {
											d.onParentNotifyCreate(task);
										}
									} catch (InterruptedException ex) {
										Logger.getLogger(ManagedMeshesAsNodeDelegator.class.getName()).log(Level.SEVERE, null, ex);
									} catch (ExecutionException ex) {
										Logger.getLogger(ManagedMeshesAsNodeDelegator.class.getName()).log(Level.SEVERE, null, ex);
									}
								}
								break;
							case CACHED:
								task.setStage(STAGE.COMPLETE);
								// Notify dependents
								for (Delegator d : dependents) {
									d.onParentNotifyCreate(task);
								}
								break;
							case PHYSICS:
								// TODO: Look into PhysicsControl definition for delegator
								if (task.getFuture() == null) {
									task.setFuture(exec.submit(new Callable() {
										@Override
										public Void call() throws Exception {
											tiles.get(key).createPhysicsNode();
											return null;
										}
									}));
								} else if (task.getFuture().isDone()) {
									task.setStage(STAGE.COMPLETE);
									task.setFuture(null);
									if (!pm.tileAdd.contains(task)) {
										pm.tileAdd.add(task);
									}
								}
								break;
						}
					}
				}
			}
		}
	}
	
	/**
	 * Dependent delegator notification.  Standard delegators check for tile 
	 * addition/creation based on distance from camera.  In the case of dependent
	 * delegators, normal distance checks are bypassed and onParentNotifyCreate
	 * is called instead.
	 * @param task The most recent task created by the parent delegator
	 */
	@Override
	protected void onParentNotifyCreate(DelegatorTask task) {
		if (tileCache.containsKey(task.getPosition())) {
			// Check cache for stored tile at current position
			if (!prepopulated) { tiles.put(task.getPosition(), tileCache.get(task.getPosition())); }
			pm.tileAdd.add(tileCache.get(task.getPosition()));
		} else {
			// Create new task
			DelegatorTask newTask = new DelegatorTask(this, task.getPosition(), this.LODHigh, this.LODLow);
			newTask.setDependentNode(task.getNode());
			tiles.put(task.getPosition(), newTask);
		}
	}
}
