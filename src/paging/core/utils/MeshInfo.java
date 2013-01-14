/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package paging.core.utils;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 *
 * @author t0neg0d
 */
public class MeshInfo {
	Vector3f position;
	Quaternion rotation;
	float scale;
	public MeshInfo(Vector3f position, Quaternion rotation, float scale) {
		this.position = position;
		this.rotation = rotation;
		this.scale = scale;
	}
	public Vector3f getPosition() {
		return this.position;
	}
	public Quaternion getRotation() {
		return this.rotation;
	}
	public float getScale() {
		return this.scale;
	}
}
