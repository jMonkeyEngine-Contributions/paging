/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package paging.core.spatials;

import com.jme3.scene.Node;

/**
 *
 * @author t0neg0d
 */
public class ManagedNode extends Node {
	private Node node;
	private boolean fade = true;
	private float maxAlpha = 1f;
	private float maxDistance = 80f;
	private float fadeStartDistance = 60f;
	
	public void setNode(Node node) { this.node = node; }
	public Node getNode() { return this.node; }
	public void setFade(boolean fade) { this.fade = fade; }
	public boolean getFade() { return this.fade; }
	public void setMaxDistance(float maxDistance) { this.maxDistance = maxDistance; }
	public float getMaxDistance() { return this.maxDistance; }
	public void setFadeStartDistance(float fadeStartDistance) { this.fadeStartDistance = fadeStartDistance; }
	public float getFadeStartDistance() { return this.fadeStartDistance; }
	public float getCalculatedAlpha(float distance) {
		return 1.0f-(distance-fadeStartDistance)/(maxDistance-fadeStartDistance);
	}
}
