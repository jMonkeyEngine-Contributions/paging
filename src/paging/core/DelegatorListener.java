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
	public void onAddToScene(Node node);
	public void onRemoveFromScene(Node node);
}
