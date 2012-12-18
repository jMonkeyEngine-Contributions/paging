/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package paging.core;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import java.util.HashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 *
 * @author t0neg0d
 */
public class PagingManager {
	public static enum LOD {
		LOD_10,
		LOD_9,
		LOD_8,
		LOD_7,
		LOD_6,
		LOD_5,
		LOD_4,
		LOD_3,
		LOD_2,
		LOD_1
	};
	
	private ScheduledThreadPoolExecutor exec;
	private Camera cam;
	private boolean managePhysics = false;
	private PhysicsSpace physics = null;
	
	protected HashMap<String, Delegator> delegators = new HashMap();
	/**
	 * Creates a new instance of the PagingManager
	 * @param exec The ScheduledThreadPoolExecutor used by the user's application
	 * @param cam The default camera used by the scene
	 */
	public PagingManager(ScheduledThreadPoolExecutor exec, Camera cam) {
		this.exec = exec;
		this.cam = cam;
	}
	/**
	 * Enable support for basic physics
	 * @param physics The PhysicsSpace used by the user's application
	 */
	public void addPhysicsSupport(PhysicsSpace physics) {
		this.managePhysics = true;
		this.physics = physics;
	}
	/**
	 * Returns if the PagingManager supports physics management
	 * @return 
	 */
	public boolean getManagePhysics() {
		return this.managePhysics;
	}
	/**
	 * Returns the registered PhysicsSpace
	 * @return 
	 */
	public PhysicsSpace getPhysicsSpace() {
		return this.physics;
	}
	/**
	 * Registers a delegator with the PagingManager
	 * @param UID The unique String friendly name of the delegator (i.e. Terrain, Vegeation, etc)
	 * @param delegator The Delegator to register
	 * @param spatial The spatial to apply the delegator to
	 * @param cacheSize The cache limit of the delegator
	 */
	public void registerDelegator(String UID, Delegator delegator, Spatial spatial, int cacheSize) {
		delegator.initDelegator(UID, exec, spatial, cam, this, cacheSize);
		delegators.put(UID, delegator);
		spatial.addControl(delegator);
	}
	/**
	 * Returns the delegator registered with the specified UID
	 * @param UID The unique String friendly name of the delegator
	 * @return 
	 */
	public Delegator getDelegatorByUID(String UID) {
		return delegators.get(UID);
	}
}
