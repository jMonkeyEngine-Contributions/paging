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
	
	public PagingManager(ScheduledThreadPoolExecutor exec, Camera cam) {
		this.exec = exec;
		this.cam = cam;
	}
	
	public void addPhysicsSupport(PhysicsSpace physics) {
		this.managePhysics = true;
		this.physics = physics;
	}
	
	public boolean getManagePhysics() {
		return this.managePhysics;
	}
	
	public PhysicsSpace getPhysicsSpace() {
		return this.physics;
	}
	
	public void registerDelegator(String UID, Delegator delegator, Spatial spatial, int cacheSize) {
		delegator.initDelegator(UID, exec, spatial, cam, this, cacheSize);
		delegators.put(UID, delegator);
		spatial.addControl(delegator);
	}
	
	public Delegator getDelegatorByUID(String UID) {
		return delegators.get(UID);
	}
}
