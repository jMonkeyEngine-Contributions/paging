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
	
	/**
	 * This is a passthrough initializer method for classes extending
	 * either ManagedMeshDelegator or ManagedNodeDelegator.  Upon regustering
	 * the extended class with either the PagingManager or a parent delegator,
	 * initDelegator is called and subsequently calls initialize.
	 */
	protected abstract void initialize();
	/**
	 * An abstracted control update loop call for use with classes extending
	 * either ManagedMeshDelegator or ManagedNodeDelegator
	 * @param tpf Standard JME time/ticks per frame
	 */
	public abstract void delegatorUpdate(float tpf);
	/**
	 * Allows classes extending spatial delegators to apply texturing to
	 * custom ManagedMesh classes managed by the extended delegator.
	 * @return The empty textured geometry that the delegator should wrap the custom ManagedMesh in.
	 */
	public abstract Geometry getGeometry();
	/**
	 * Abstracted classes used by ManagedMeshDelegator and ManagedNodeDelegator
	 * @param task The delegator task (see DelegatorTask.java)
	 */
	public abstract void onParentNotifyCreate(DelegatorTask task);
	/**
	 * Initialization method called by either the paging manager or parent
	 * delegator when registering the new delegator with the system.  See
	 * initialize() for custom delegator usage.
	 * @param UID Unique String name of delegator
	 * @param exec The user created ScheduledThreadPoolExecutor the system is using
	 * @param spatial The spatial the delegator control is to be added to
	 * @param cam The default camera used in the scene
	 * @param pm Pointer to the paging manager (see PagingManager.getDelegatorByUID()
	 * @param cacheSize The number of entries the delegator cache should handle
	 */
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
	/**
	 * 
	 * @param tileSize The x/y or x/y/z float size of a single tile or sector
	 * @param tilesPerColumn The number of tile along the x/y/z axis to manage/display
	 * @param disableYAxis true = tile, false = sector
	 */
	public void setTile(float tileSize, int tilesPerColumn, boolean disableYAxis) {
		this.tileSize = tileSize;
		this.tilesPerColumn = tilesPerColumn;
		this.maxDistance = tileSize*((float)tilesPerColumn/2f);
		this.disableYAxis = disableYAxis;
	}
	/**
	 * Any class that implements the DelegatorListener interface which requires
	 * notification when a tile is added to or removed from the scene
	 * @param listener The class implementing the DelegatorLister interface
	 */
	public void addListener(DelegatorListener listener) {
        this.listeners.add(listener);
    }
	/**
	 * Remove a listener that is registered with the delegator
	 * @param listener The class that will no longer be listening
	 */
    public void removeListener(DelegatorListener listener) {
        this.listeners.remove(listener);
    }
	/**
	 * Dependent delegators bypass the camera distance evaluation and are instead
	 * notified when their parent delegator creates/adds a new tile.  Depending on
	 * the type of delegator being registered as a dependent, this either fires off
	 * the ManagedMesh creation process or adds a ManagedNode to the scene.
	 * NOTE: Dependent delegators inherit their parent delegator tile information.
	 * NOTE: Dependent delegators only need be registered with their parent delegator.
	 * @param UID The unique String name of the delegator
	 * @param delegator The delegator being added as a dependent
	 */
	public void addDependantDelegator(String UID, Delegator delegator) {
		delegator.initDelegator(UID, exec, spatial, cam, pm, cacheSize);
		delegator.setTile(tileSize, tilesPerColumn, disableYAxis);
		delegator.setIsDependant(true);
		spatial.addControl(delegator);
		this.dependents.add(delegator);
	}
	/**
	 * Flags the delegator as a dependent. Called by addDependent delegator
	 * @param isDependant boolean
	 */
	protected void setIsDependant(boolean isDependant) {
		this.isDependant = isDependant;
	}
	/**
	 * Method for external classes to use in locating tiles
	 * @param position Vector3f location of the tile to find
	 * @return boolean exists
	 */
	public boolean tileExists(Vector3f position) {
		return tiles.contains(position);
	}
	/**
	 * Method for external classes to use in locating tiles
	 * @param position Vector3f location of tile to find
	 * @return Returns the spatial (tile) at the provided location
	 */
	public Spatial getTileAtLocation(Vector3f position) {
		return tiles.get(position).getNode();
	}
	/**
	 * Method for external classes to use in locating tiles
	 * @param position Vector3f location
	 * @return The spatial (tile) containing the location provided
	 */
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
	/**
	 * Returns the float value of the size of the tile
	 * @return float tileSize
	 */
	public float getTileSize() {
		return this.tileSize;
	}
	/**
	 * Returns the number of tiles managed/added along any one axis
	 * @return int tilesPerColumn
	 */
	public int getTilesPerColumn() {
		return this.tilesPerColumn;
	}
	/**
	 * Returns the maximum distance any given tile can reach before being
	 * removed from the scene
	 * @return float maxDistance (tileSize*tilesPerColumn/2)
	 */
	public float getMaxDistance() {
		return this.maxDistance;
	}
	/**
	 * Returns if the delegator is handling tiles or sectors
	 * @return boolean disableYAxis
	 */
	public boolean getDisableYAxis() {
		return this.disableYAxis;
	}
	/**
	 * Sets if the delegator should create/add/remove RigidBodyControls
	 * per tile to the specified PhysicsSpace
	 * @param managePhysics boolean
	 */
	public void setManagePhysics(boolean managePhysics) {
		this.managePhysics = managePhysics;
	}
	/**
	 * Gets if the delegator is creating/adding/removing RigidBodyControls
	 * per tile to the specified PhysicsSpace
	 * @return boolean managePhysics
	 */
	public boolean getManagePhysics() {
		return this.managePhysics;
	}
	/**
	 * Sets if the delegator should be handling LOD updates
	 * @param manageLOD 
	 */
	public void setManageLOD(boolean manageLOD) {
		this.manageLOD = manageLOD;
	}
	/**
	 * Gets if the delegator is handling LOD updates
	 * @return boolean manageLOD
	 */
	public boolean getManageLOD() {
		return this.manageLOD;
	}
	/**
	 * Adds a level of detail and LODLow or LODHigh if applicable
	 * @param lod PagingManager.LOD level
	 * @param distance Start distance for level of detail
	 */
	public void addLOD(LOD lod, float distance) {
		if (!LODDistances.containsKey(lod)) {
			LODDistances.put(lod, distance);
			if (LODLow == null) LODLow  = lod;
			else if (lod.ordinal() < LODLow.ordinal()) LODLow = lod;
			if (LODHigh == null) LODHigh  = lod;
			else if (lod.ordinal() > LODHigh.ordinal()) LODHigh = lod;
		}
	}
	/**
	 * Removes an existing level of detail
	 * @param lod PagingManager.LOD
	 */
	public void removeLOD(LOD lod) {
		if (LODDistances.containsKey(lod))
			LODDistances.remove(lod);
	}
	/**
	 * Returns the float value of the start distance for the specified level of detail
	 * @param lod PagingManager.LOD
	 * @return float Level of detail start distance
	 */
	public float getLOD(LOD lod) {
		return LODDistances.get(lod);
	}
	/**
	 * Returns the furthest level of detail handled by the delegator
	 * @return PagingManager.LOD
	 */
	public LOD getLowDetail() {
		return this.LODLow;
	}
	/**
	 * Returns the closest level of detail handled by the delegator
	 * @return PagingManager.LOD
	 */
	public LOD getHighDetail() {
		return this.LODHigh;
	}
	/**
	 * Returns the entire hash map of registered levels of detail
	 * @return ConcurrentHashMap<PagingManager.LOD, Float>
	 */
	public ConcurrentHashMap<LOD, Float> getLODs() {
		return LODDistances;
	}
	/**
	 * Returns the unique string identifier for the delegator
	 * @return String UID
	 */
	public String getUID() {
		return this.UID;
	}
}
