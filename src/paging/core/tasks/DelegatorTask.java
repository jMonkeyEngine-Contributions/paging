/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package paging.core.tasks;

import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import java.util.concurrent.Future;
import paging.core.Delegator;
import paging.core.PagingManager.LOD;
import paging.core.spatials.ManagedMesh;
import paging.core.spatials.ManagedNode;

/**
 *
 * @author t0neg0d
 */
public class DelegatorTask {
	public static enum STAGE {
		BEGIN,
		CACHED,
		PHYSICS,
		COMPLETE
	}
	
	private Delegator delegator;
	private Vector3f position;
	private ManagedNode node = null;
	private ManagedNode dependentNode = null;
	private ManagedMesh mesh = null;
	private PhysicsControl physicsNode = null;
	private Geometry geom = null;
	private STAGE stage = STAGE.BEGIN;
	private Future future = null;
	private LOD detailHigh, detailLow;
	private LOD nextLOD = LOD.LOD_1;
	
	public DelegatorTask(Delegator delegator, final Vector3f position, LOD detailHigh, LOD detailLow) {
		this.delegator = delegator;
		this.position = position;
		this.detailHigh = detailHigh;
		this.detailLow = detailLow;
		this.nextLOD = detailLow;
	}
	
	public Vector3f getPosition() {
		return this.position;
	}
	
	public void finialize() {
		String name =	delegator.getUID() + ":" + 
						position.getX() + ":" + 
						position.getY() + ":" + 
						position.getZ() + ":";
		node = new ManagedNode();
		node.setName(name + "Node");
		geom = delegator.getGeometry();
		geom.setName(name + "Geom");
		geom.setMesh(mesh);
		node.attachChild(geom);
		node.setLocalTranslation(position);
	}
	
	public ManagedNode getNode() {
		return this.node;
	}
	
	public void setNode(ManagedNode node) {
		this.node = node;
	}
	
	public ManagedMesh getMesh() {
		return this.mesh;
	}
	
	public void setMesh(ManagedMesh mesh) {
		this.mesh = mesh;
	}
	
	public Geometry getGeometry() {
		return this.geom;
	}
	
	public void setGeometry(Geometry geom) {
		this.geom = geom;
	}
	
	public void createPhysicsNode() {
		mesh.updateLOD(detailHigh);
		this.physicsNode = new RigidBodyControl(CollisionShapeFactory.createMeshShape(node), 0);
		mesh.updateLOD(detailLow);
		node.addControl(physicsNode);
	}
	
	public PhysicsControl getPhysicsNode() {
		return this.physicsNode;
	}
	
	public STAGE getStage() {
		return this.stage;
	}
	
	public void setStage(STAGE stage) {
		this.stage = stage;
	}
	
	public Future getFuture() {
		return this.future;
	}
	
	public void setFuture(Future future) {
		this.future = future;
	}
	
	public void setNextLOD(LOD lod) {
		this.nextLOD = lod;
	}
	
	public LOD getNextLOD() {
		return this.nextLOD;
	}
	
	public ManagedNode getDependentNode() {
		return this.dependentNode;
	}
	
	public void setDependentNode(ManagedNode node) {
		this.dependentNode = node;
	}
	
	
}
