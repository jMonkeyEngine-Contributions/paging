/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package paging.core.spatials;

import com.jme3.asset.AssetManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import paging.core.PagingManager.LOD;
import paging.core.utils.MeshInfo;

/**
 *
 * @author t0neg0d
 */
public class ManagedMeshFromTemplate extends ManagedMesh {
	private AssetManager assetManager;
	
	private FloatBuffer	templateVerts;
	private FloatBuffer	templateCoords;
	private IndexBuffer	templateIndexes;
	private FloatBuffer	templateNormals;
	
	private FloatBuffer	finVerts;
	private FloatBuffer	finCoords;
	private IntBuffer	finIndexes;
	private FloatBuffer	finNormals;
	
	private List<MeshInfo> positions;
	
	public ManagedMeshFromTemplate(AssetManager assetManager, FloatBuffer templateVerts, FloatBuffer templateCoords, IndexBuffer templateIndexes, FloatBuffer templateNormals) {
		this.assetManager = assetManager;
		this.templateVerts = templateVerts;
		this.templateCoords = templateCoords;
		this.templateIndexes = templateIndexes;
		this.templateNormals = templateNormals;
	}
	
	public void setMeshInfo(List<MeshInfo> positions) {
		this.positions = positions;
		build();
	}
	
	@Override
	public void build() {
		// Create final buffers
		this.finVerts = BufferUtils.createFloatBuffer(templateVerts.capacity()*positions.size());
		this.finCoords = BufferUtils.createFloatBuffer(templateCoords.capacity()*positions.size());
		this.finIndexes = BufferUtils.createIntBuffer(templateIndexes.size()*positions.size());
		this.finNormals = BufferUtils.createFloatBuffer(templateNormals.capacity()*positions.size());
		
	//	System.out.println(templateVerts.capacity());
		// Create new vector3f for altering position/rotation of existing buffer data
		Vector3f tempVec = new Vector3f();
		
		int index = 0, index2 = 0, index3 = 0, index4 = 0;
		int indexOffset = 0;
		
		for (int i = 0; i < positions.size(); i++) {
			templateVerts.rewind();
			for (int v = 0; v < templateVerts.capacity(); v += 3) {
				tempVec.set(templateVerts.get(v), templateVerts.get(v+1), templateVerts.get(v+2));
				positions.get(i).getRotation().mult(tempVec, tempVec);
				tempVec.multLocal(positions.get(i).getScale());
				tempVec.addLocal(positions.get(i).getPosition());
				finVerts.put(index, tempVec.getX());
				index++;
				finVerts.put(index, tempVec.getY());
				index++;
				finVerts.put(index, tempVec.getZ());
				index++;
			}
			
			templateCoords.rewind();
			for (int v = 0; v < templateCoords.capacity(); v++) {
				finCoords.put(index2, templateCoords.get(v));
				index2++;
			}
			
			for (int v = 0; v < templateIndexes.size(); v++) {
				finIndexes.put(index3, templateIndexes.get(v)+indexOffset);
				index3++;
			}
			indexOffset += templateVerts.capacity()/3;
			
			templateNormals.rewind();
			for (int v = 0; v < templateNormals.capacity(); v++) {
				finNormals.put(index4, templateNormals.get(v));
				index4++;
			}
		}
		
		// Help GC
		tempVec = null;
	//	templateVerts = null;
	//	templateCoords = null;
	//	templateIndexes = null;
	//	templateNormals = null;
		
		// Clear & ssign buffers
		this.clearBuffer(Type.Position);
		this.setBuffer(Type.Position,	3, finVerts);
		this.clearBuffer(Type.TexCoord);
		this.setBuffer(Type.TexCoord,	2, finCoords);
		this.clearBuffer(Type.Index);
		this.setBuffer(Type.Index,		3, finIndexes);
		this.clearBuffer(Type.Normal);
		this.setBuffer(Type.Normal,		3, finNormals);
		this.updateBound();
	}

	@Override
	public void updateLOD(LOD lod) {
		
	}
}
