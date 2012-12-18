/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package paging.core.spatials;

import com.jme3.scene.Mesh;
import paging.core.PagingManager.LOD;

/**
 *
 * @author t0neg0d
 */
public abstract class ManagedMesh extends Mesh {
	private LOD currentLOD;
	/**
	 * The abstracted build method is not enforced by the paging system, instead
	 * buildMesh() is supplied via the Delegator class.  This is for continuity's
	 * sake by standardizing the build method of custom meshes.  Use if you like.
	 */
	public abstract void build();
	/**
	 * A mechanism used by Delegators for handling updates to LOD with the user's
	 * custom ManagedMeshs
	 * @param lod The PagingManager.LOD provided by the Delegator
	 */
	public abstract void updateLOD(LOD lod);
	/**
	 * Returns the current LOD of the custom mesh
	 * @return PagingManager.LOD
	 */
	public LOD getCurrentLOD() {
		return this.currentLOD;
	}
	/**
	 * Ensure that this method is called by updateLOD as the Delegator's LOD management
	 * depends on checks against the mesh's current LOD
	 * @param lod 
	 */
	public void setCurrentLOD(LOD lod) {
		this.currentLOD = lod;
	}
}
