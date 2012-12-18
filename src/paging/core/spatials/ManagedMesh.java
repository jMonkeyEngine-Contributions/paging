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
	
	public abstract void build();
	public abstract void updateLOD(LOD lod);
	public LOD getCurrentLOD() {
		return this.currentLOD;
	}
	public void setCurrentLOD(LOD lod) {
		this.currentLOD = lod;
	}
}
