/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package paging.core;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import paging.core.PagingManager.LOD;
import paging.core.tasks.DelegatorTask;

/**
 *
 * @author t0neg0d
 */
public abstract class Delegator implements Control {
	
	protected String UID;
	protected ScheduledThreadPoolExecutor exec;
	protected Spatial spatial;
	protected Camera cam;
	protected Vector3f camVec = Vector3f.ZERO;
	protected boolean isDependant = false;
	protected int cacheSize = 30;
	protected PagingManager pm;
	protected boolean prepopulated = false;
	
	// Listeners & dependant delegators
	protected Set<DelegatorListener> listeners = new HashSet<DelegatorListener>();
	protected Set<Delegator> dependents = new HashSet<Delegator>();
	
	// Tasks & directives
	protected LRUCache<Vector3f, DelegatorTask> tileCache;
	protected ConcurrentHashMap<Vector3f, DelegatorTask> tiles = new ConcurrentHashMap();
	protected ConcurrentLinkedQueue<DelegatorTask> tileAdd = new ConcurrentLinkedQueue();
	protected ConcurrentLinkedQueue<DelegatorTask> tileRemove = new ConcurrentLinkedQueue();
	protected ConcurrentLinkedQueue<DelegatorTask> LODUpdates = new ConcurrentLinkedQueue();
	//	protected ConcurrentLinkedQueue<DelegatorDirective> directives = new ConcurrentLinkedQueue();
//	protected Set<Vector3f> keys;
	
	// Tile info
	protected float tileSize;
	protected int tilesPerColumn;
	protected float maxDistance;
	protected boolean disableYAxis = true;
	
	// LOD info
	protected ConcurrentHashMap<PagingManager.LOD, Float> LODDistances = new ConcurrentHashMap();
	protected boolean manageLOD;
	protected LOD LODLow, LODHigh;
	
	// Physics
	protected boolean managePhysics = false;
	
	// Futures
	Future fTaskLOD;
	Future fTaskRemove;
	Future fTaskCreate;
	
	// extended initialize
	protected abstract void initialize();
	// User extended update loop
	public abstract void delegatorUpdate(float tpf);
	public abstract Geometry getGeometry();
	// Dependent delegator methods
	public abstract void onParentNotifyCreate(DelegatorTask task);
	
	protected void initDelegator(String UID, ScheduledThreadPoolExecutor exec, Spatial spatial, Camera cam, PagingManager pm, int cacheSize) {
		this.UID = UID;
		this.exec = exec;
		this.spatial = spatial;
		this.cam = cam;
		this.cacheSize = cacheSize;
		this.pm = pm;
		System.out.println(UID + ": " + cacheSize);
		this.tileCache = new LRUCache(cacheSize);
		initialize();
	}
	
	public void setTile(float tileSize, int tilesPerColumn, boolean disableYAxis) {
		this.tileSize = tileSize;
		this.tilesPerColumn = tilesPerColumn;
		this.maxDistance = tileSize*((float)tilesPerColumn/2f);
		this.disableYAxis = disableYAxis;
	}
	
	public void addListener(DelegatorListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(DelegatorListener listener) {
        this.listeners.remove(listener);
    }
	
	public void addDependantDelegator(String UID, Delegator delegator) {
		delegator.initDelegator(UID, exec, spatial, cam, pm, cacheSize);
		delegator.setTile(tileSize, tilesPerColumn, disableYAxis);
		delegator.setIsDependant(true);
		spatial.addControl(delegator);
		this.dependents.add(delegator);
	}
	
	protected void setIsDependant(boolean isDependant) {
		this.isDependant = isDependant;
	}
	
//	protected void enqueueDirective(DelegatorDirective directive) {
//		this.directives.add(directive);
//	}
	
	public boolean tileExists(Vector3f position) {
		return tiles.contains(position);
	}
	
	public Spatial getTileAtLocation(Vector3f position) {
		return tiles.get(position).getNode();
	}
	
	public Spatial getTileContainingLocation(Vector3f position) {
		Spatial ret = null;
		Set<Vector3f> keys = tiles.keySet();
		for (Vector3f key : keys) {
			if (tiles.get(key).getNode().getWorldBound().contains(position)) {
				ret = tiles.get(key).getNode();
				break;
			}
		}
		return ret;
	}
	
	public float getTileSize() {
		return this.tileSize;
	}
	
	public int getTilesPerColumn() {
		return this.tilesPerColumn;
	}
	
	public float getMaxDistance() {
		return this.maxDistance;
	}
	
	public boolean getDisableYAxis() {
		return this.disableYAxis;
	}
	
	public void setManagePhysics(boolean managePhysics) {
		this.managePhysics = managePhysics;
	}
	
	public boolean getManagePhysics() {
		return this.managePhysics;
	}
	
	public void setManageLOD(boolean manageLOD) {
		this.manageLOD = manageLOD;
	}
	
	public boolean getManageLOD() {
		return this.manageLOD;
	}
	
	public void addLOD(LOD lod, float distance) {
		if (!LODDistances.containsKey(lod)) {
			LODDistances.put(lod, distance);
			if (LODLow == null) LODLow  = lod;
			else if (lod.ordinal() < LODLow.ordinal()) LODLow = lod;
			if (LODHigh == null) LODHigh  = lod;
			else if (lod.ordinal() > LODHigh.ordinal()) LODHigh = lod;
		}
	}
	
	public void removeLOD(LOD lod) {
		if (LODDistances.containsKey(lod))
			LODDistances.remove(lod);
	}
	
	public float getLOD(LOD lod) {
		return LODDistances.get(lod);
	}
	
	public LOD getLowDetail() {
		return this.LODLow;
	}
	
	public LOD getHighDetail() {
		return this.LODHigh;
	}
	
	public ConcurrentHashMap<LOD, Float> getLODs() {
		return LODDistances;
	}
	
	public String getUID() {
		return this.UID;
	}
}
