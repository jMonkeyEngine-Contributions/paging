/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package paging.core;

import com.jme3.scene.Node;

/**
 *
 * @author t0neg0d
 */
public interface DelegatorListener {
	/**
	 * Callback to listener on add tile to scene event
	 * @param node The node added to the scene
	 */
	public void onAddToScene(Node node);
	/**
	 * Callback to listener on remove tile from scene event
	 * @param node The node removed from the scene
	 */
	public void onRemoveFromScene(Node node);
}
