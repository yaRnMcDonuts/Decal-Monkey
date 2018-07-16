/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SpecialEffects.TextureEffects;

import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.terrain.Terrain;
import java.util.ArrayList;

/**
 *
 * @author ryan
 */
public interface DecalEnabledMap {
    
        public CollisionResults getCollisionResultsWith(Collidable collidable);
        public ArrayList<Terrain> getTerrains();
    
}
