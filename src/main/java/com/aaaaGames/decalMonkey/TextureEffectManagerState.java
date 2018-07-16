/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SpecialEffects.TextureEffects;

import CoreAppClasses.AppController;
import Maps.Map;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Transform;
import com.jme3.math.Triangle;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.scene.shape.Box;
import com.jme3.shader.VarType;
import static com.jme3.shader.VarType.TextureArray;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.TerrainPatch;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import com.jme3.util.BufferUtils;
import com.jme3.util.SafeArrayList;
import com.jme3.util.TangentBinormalGenerator;
import com.sun.prism.impl.BufferUtil;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

/**
 *
 * @author ryan
 */
public class TextureEffectManagerState extends BaseAppState{
    public SimpleApplication app;
    

    private ArrayList registeredEffects = new ArrayList();
    private HashMap<String, Integer> registeredTextures = new HashMap();
    
    
    
    private Node collisionNode = null;
    public void setCollisionNode(Node decalCollisionNode){ collisionNode = decalCollisionNode; } 
    
    // OPTIONAL - implement the ' Map ' interface and set an active map, and write your own method to overide the "getCollisionResultsWith" method in order to return collision results filtered however you would like for your map!
    // active map takes precedence over collision node
    private DecalEnabledMap activeMap;
    public void setMap(DecalEnabledMap map){ activeMap = map; }
     

    //if 'decalUdString' is not null, then decals will only be applied to objects that have any non-null string value as the value assigned to 
    private String decalUdStringKey = null;
    public void setUserDataString(String udString){ decalUdStringKey = udString; }
    

    private ArrayList<Terrain> terrains;
    
    int frame = 0;
    
    float depthPriority = 0;
    
    @Override
    public void update(float tpf){
        if(frame > 15){
            for(int i = 0; i < registeredEffects.size(); i++){
                TextureEffect textureEffect = (TextureEffect) registeredEffects.get(i);
                if(textureEffect != null){
                    textureEffect.update(tpf);
                    if(!textureEffect.isReady()){
                        textureEffect.init(app);
                    }
                    else{
                        depthPriority = textureEffect.getDepthPriority();
                        depthPriority = ((float)(i)/200) +  depthPriority;
                        updateAffectedObjects(textureEffect);
                    }
                      
                    
                    if(textureEffect.getDuration() <= 0 && !textureEffect.isIndefinite()){
                        textureEffect.finished();
                        app.getRootNode().detachChild(textureEffect.getGeometry());
                        registeredEffects.remove(textureEffect);
                    }
                    
                }
            }
        }
        else{
            frame++;
        }
        
       super.update(tpf);
    }
    
    private void updateAffectedObjects(TextureEffect textureEffect){
        Geometry projectorBox = textureEffect.getProjectorBox();
        
        updateGeom(projectorBox, textureEffect);
      
    }
    
    public void registerTextureEffect(TextureEffect textureEffect){
        registeredEffects.add(textureEffect);        
      
    }
    
      Random chance = new Random();
      
      private Geometry updateGeom(Geometry projectorGeom, TextureEffect textureEffect){
          
          BoundingBox bounds = (BoundingBox) projectorGeom.getWorldBound();
          float xLength = bounds.getXExtent(), zLength  = bounds.getZExtent(), yLength = bounds.getYExtent();
          
          Vector3f decalCenter = bounds.getCenter();

          Geometry decalGeom = textureEffect.getGeometry();
          Mesh decalMesh = textureEffect.getMesh();         
          Material decalMat;
          if(decalMesh == null){
              decalMesh = new Mesh();
              decalMesh = updateMesh(bounds, decalMesh, null, textureEffect);
              
               decalGeom = new Geometry("decalGeom",decalMesh);
               app.getRootNode().attachChild(decalGeom);
               
               decalGeom.setUserData("decal", "t");
            
               decalMat = textureEffect.getMaterial();
                
                              
                
                
                textureEffect.setGeometry(decalGeom);
                decalGeom.setMaterial(decalMat);  
                decalGeom.setShadowMode(RenderQueue.ShadowMode.Receive);
                decalGeom.setQueueBucket(RenderQueue.Bucket.Translucent);
                
                
       //         if(textureEffect.isStackable())
       //             decalGeom.setUserData("hybridItem", "t"); //makes existent effects collide for extra depth sorting (most practical when using an effect that fades and can be re-applied at a fast rate)
                
          }
          else{
              Vector3f lastLoc = textureEffect.getLastLoc();
              if(!lastLoc.equals(textureEffect.getLocation())){
                decalMesh = updateMesh(bounds, decalMesh, decalGeom, textureEffect);
              }
              decalMat = decalGeom.getMaterial();
          }
            
            decalMat.setParam("CenterPoint", VarType.Vector3, bounds.getCenter());
            decalMat.setParam("EdgeFadeDistance", VarType.Float, textureEffect.getEdgeFade());
            decalMat.setParam("Scale", VarType.Float, textureEffect.getScale());
            decalMat.setParam("CircleShape", VarType.Boolean, textureEffect.isCircleShape());
            decalMat.setParam("ProjectorDimensions", VarType.Vector3, textureEffect.getDimensions());
            decalMat.setParam("Noise", VarType.Vector3, textureEffect.getNoiseVector());
            decalMat.setParam("BaseColor", VarType.Vector4, textureEffect.getColor());
            
            
            decalGeom.setLocalTranslation(decalCenter);
            
   //         mat.getAdditionalRenderState().setWireframe(true);
            
            return decalGeom;
          
            
            
      }
      
      private HashMap<Vector3f, Integer> decalVerts;
      private HashMap<Vector3f, Vector3f> decalNrmls;
      
      private Mesh updateMesh(BoundingBox bounds, Mesh decalMesh, Geometry decalGeom, TextureEffect textureEffect){
          
          
          CollisionResults results = new CollisionResults(); 
          try{
              
              
            if(activeMap != null){
                results = activeMap.getCollisionResultsWith(bounds);
            }
            else{
                if(collisionNode == null){
                    System.out.println(" IMPORTANT: The TextureEffectManagerState cannot have a null collisionNode.");
                }
                
                bounds.collideWith(collisionNode, results);
                
            }
                
            
            
            ArrayList<TerrainPatch> terrainPatches = getTerrainPatches(bounds);
            
            int patchCount = 0;
            if(terrainPatches != null){
                patchCount = terrainPatches.size();
            }
                
            Vector3f decalCenter = bounds.getCenter();
            float xLength = bounds.getXExtent(), zLength  = bounds.getZExtent(), yLength = bounds.getYExtent();
            decalVerts = new HashMap();
            decalNrmls = new HashMap();
  //          ArrayList normals = new ArrayList();
  //          ArrayList texCoords = new ArrayList();
  //     
              FloatBuffer positionBuff;
              FloatBuffer texBuffer;
              FloatBuffer normalBuffer;
              IntBuffer indexBuffer;

              int size = 4 * ((patchCount*4126) + results.size());

              Vector3f[] newPositions = new Vector3f[size];
              Vector3f[] newNormals = new Vector3f[size];
              Vector2f[] newTexCoords = new Vector2f[size];
              int[] newIndexes = new int[size];
              
              int posCount = 0;
              int indCount = 0;
              int texCount = 0;

              Triangle tri = null;
              Vector3f side0 = new Vector3f(), side1;

              for(int t = 0; t < results.size();t++){
                  Geometry geo = results.getCollision(t).getGeometry();
         
                      if(( decalUdStringKey == null || traverseNodeHierarchyForUD(decalUdStringKey, geo)) && !geo.equals(decalGeom)){ 

                            Transform transform = new Transform();
                            transform = geo.getWorldTransform();

                            tri = new Triangle();

                            tri = results.getCollision(t).getTriangle(tri);
                            Vector3f normal, nrmlOffset; //used for avoiding z figthing during testing
                            normal = tri.getNormal();
                            nrmlOffset = normal.mult(.02f * depthPriority).divide(geo.getWorldScale());

                            for(int m = 0; m < 3; m++){
                                Vector3f vertPos = tri.get(m);

                                Vector3f projectedLoc = new Vector3f();

                                transform.transformVector(vertPos, projectedLoc);
                                vertPos.addLocal(nrmlOffset);
                                Vector3f mapKey;
                               if(!decalVerts.containsKey(projectedLoc)){
                                   mapKey = projectedLoc.clone();


                                    decalVerts.put(mapKey, posCount); // store verts to check for doubles before storing the vert's location with normal offset applied


                                     //add nrmlOffset and calculate position
                                    transform.transformVector(vertPos, projectedLoc); 
                                    decalNrmls.put(mapKey, nrmlOffset); //store this vert's position after nrmlOffset is applied, in order to properly combine with another vert later on if it is a double


                                    projectedLoc.subtractLocal(decalCenter);

                                     //calculate texCoords
                                      float xDist, zDist, yDist;
                                       xDist = (projectedLoc.getX() + xLength) / (xLength*2f);
                                       zDist = (projectedLoc.getZ() + zLength) / (zLength*2f);
                                       
                                       yDist = (projectedLoc.getY() + yLength) / (yLength*2f);

   
//                                       Vector3f projDir = new Vector3f(0f, 0f, -1f);
//                                 //      Vector3f projDir = tri.getNormal().negate();
//                                
//                                        projDir.normalizeLocal();
//                                        
//                                        Quaternion direction = new Quaternion();
//                                        direction.lookAt(projDir, new Vector3f(0,1,0));
//                                       
//                                       Vector3f leftVec = direction.getRotationColumn(0).normalize().mult(xLength * 2);
//                                       
//                                       Vector3f topVec = direction.getRotationColumn(2).normalize().mult(zLength*2);
//                                       
//                                       xDist = projectedLoc.distance(leftVec) / leftVec.length();
//                                       zDist = projectedLoc.distance(topVec) / topVec.length();
//                                       
                                       
                                  
                                       Vector2f coord = new Vector2f(xDist, zDist);
                                       
                                       
                                       

                                       
                                       
                                       newTexCoords[texCount++] = coord;


                                        newIndexes[indCount++] = posCount;
                                        newNormals[posCount] = new Vector3f(0,1,0); 
        
                                        newPositions[posCount++] = projectedLoc;



                               }
                               else{ //add nrmlOffset to any existant verts / doubles
                                   mapKey = projectedLoc;
                                   int pos = decalVerts.get(projectedLoc);
                                   newIndexes[indCount++] = pos;

                                   Vector3f existantNormal = decalNrmls.get(mapKey);
                                   vertPos.addLocal(existantNormal);
                                   transform.transformVector(vertPos, projectedLoc); 

                                   projectedLoc.subtractLocal(decalCenter);

                                   
                                   newPositions[pos].set(projectedLoc);
                                   
//                                   
//                                  float xDist, zDist, yDist;
//                                       xDist = (projectedLoc.getX() + xLength) / (xLength*2f);
//                                       zDist = (projectedLoc.getZ() + zLength) / (zLength*2f);
//                                       
//                                       yDist = (projectedLoc.getY() + yLength) / (yLength*2f);
//
//   
//                                  //     Vector3f projDir = new Vector3f(-.1f, -1, -.4f);
//                                       Vector3f projDir = tri.getNormal().negate();
//                                
//                                       
//                                        projDir.normalizeLocal();
////                                        projDir.crossLocal(existantNormal.normalize());
////                                        projDir.normalizeLocal();
//                                        
//                                        Quaternion direction = new Quaternion();
//                                        direction.lookAt(projDir, new Vector3f(0,1,0));
//                                       
//                                       Vector3f leftVec = direction.getRotationColumn(0).normalize().mult(xLength * 2);
//                                       
//                                       Vector3f topVec = direction.getRotationColumn(2).normalize().mult(zLength*2);
//                                       
//                                       xDist = projectedLoc.distance(leftVec) / leftVec.length();
//                                       zDist = projectedLoc.distance(topVec) / topVec.length();
//                                       
//                                       
//                                  
//                                       Vector2f coord = new Vector2f(xDist, zDist);
//                                       
//                                       newTexCoords[pos] = coord;
                                   
                             //      newNormals[pos] = tri.getNormal().add(newNormals[pos]).divide(2).normalize();
                             //       newNormals[pos] = decalNrmls.get(mapKey);

                               }

                    //        if(!bounds.contains(worldLoc)){ 
                    //             eventually crop verts that aren't in the projection zone
                    //         }


                           }


                        
                      }
              }
              
              int indexOffset = indCount;
                 
            if(terrainPatches != null){
                for(TerrainPatch patch : terrainPatches){

                    Geometry newGeo = patch.clone();
           //         app.getRootNode().attachChild(newGeo);
                    newGeo.setLocalTransform(patch.getWorldTransform());
                    newGeo.move(0,.05f,0);
                    newGeo.setMaterial(textureEffect.getMaterial());

                    Mesh newMesh = newGeo.getMesh();

    //                VertexBuffer positions = newMesh.getBuffer(Type.Position);
    //                 IndexBuffer indexes = newMesh.getIndexBuffer();
    //                
    //               Vector3f[] terrainPositions =  BufferUtils.getVector3Array((FloatBuffer) positions.getData());
    ////               
    //                for(int i = 0; i < indexes.size(); i++){
    //                    int terrainIndex = indexes.get(i) + indexOffset;
    //                    
    //                    newIndexes[indCount++] = terrainIndex;
    //                }
    //                
    //                
    //               for(int p = 0; p < terrainPositions.length; p++){
    //                   Vector3f terrainVertPos = terrainPositions[p];
    //                   
    //                   newPositions[posCount++] = terrainVertPos;
    //               }



                    for(int i = 0; i < newMesh.getTriangleCount(); i++){
                        Triangle terrainTri = new Triangle();
                        newMesh.getTriangle(i, terrainTri);

               //        if(bounds.contains(tri.get(0))){ //implement eventually when you add mesh cropping
    //                      Transform transform = new Transform();
    //                            transform = newGeo.getWorldTransform();
    //
    //                            Vector3f normal, nrmlOffset; //used for avoiding z figthing during testing
    //                            normal = terrainTri.getNormal();
    //                //            nrmlOffset = normal.mult(.02f * depthPriority).divide(newGeo.getWorldScale());
    //                nrmlOffset = new Vector3f(0,0,0);

                                for(int m = 0; m < 3; m++){
                                    Vector3f vertPos = terrainTri.get(m);
                       //             System.out.println(vertPos);

                                    Vector3f projectedLoc = new Vector3f();
    //
    //                                transform.transformVector(vertPos, projectedLoc);
    //                                vertPos.addLocal(nrmlOffset);
    //                                Vector3f mapKey;
    //                               if(!decalVerts.containsKey(projectedLoc)){
                         //              mapKey = projectedLoc.clone();


                     //                   decalVerts.put(mapKey, posCount); // store verts to check for doubles before storing the vert's location with normal offset applied


                                         //add nrmlOffset and calculate position
                  //                      transform.transformVector(vertPos, projectedLoc); 
                  //                      decalNrmls.put(mapKey, nrmlOffset); //store this vert's position after nrmlOffset is applied, in order to properly combine with another vert later on if it is a double


                                        newGeo.getWorldTransform().transformVector(vertPos, projectedLoc);


                                         //calculate texCoords
                                          float xDist, zDist, yDist;
                                           xDist = (projectedLoc.getX() + xLength) / (xLength*2f);
                                           zDist = (projectedLoc.getZ() + zLength) / (zLength*2f);

                                           yDist = (projectedLoc.getY() + yLength) / (yLength*2f);


                                           float xFloat, yFloat;



                                           Vector3f projDir = new Vector3f(0f, 0f, -1f);
    //                                 //      Vector3f projDir = tri.getNormal().negate();
    //                                
    //                                        projDir.normalizeLocal();
    //                                        
    //                                        Quaternion direction = new Quaternion();
    //                                        direction.lookAt(projDir, new Vector3f(0,1,0));
    //                                       
    //                                       Vector3f leftVec = direction.getRotationColumn(0).normalize().mult(xLength * 2);
    //                                       
    //                                       Vector3f topVec = direction.getRotationColumn(2).normalize().mult(zLength*2);
    //                                       
    //                                       xDist = projectedLoc.distance(leftVec) / leftVec.length();
    //                                       zDist = projectedLoc.distance(topVec) / topVec.length();
    //                                       
    //                                       
    //                                  
                                           Vector2f coord = new Vector2f(xDist, zDist);


                                           projectedLoc.subtractLocal(decalCenter);



                                           newTexCoords[texCount++] = coord;


                                            newIndexes[indCount++] = posCount;
                                            newNormals[posCount] = new Vector3f(0,1,0); 

                                            newPositions[posCount++] = projectedLoc;
       //                            }
    //                                else{ //add nrmlOffset to any existant verts / doubles
    //                                   mapKey = projectedLoc;
    //                                   int pos = decalVerts.get(projectedLoc);
    //                                   newIndexes[indCount++] = pos;
    //
    //                                   Vector3f existantNormal = decalNrmls.get(mapKey);
    //                                   vertPos.addLocal(existantNormal);
    //                                   transform.transformVector(vertPos, projectedLoc); 
    //
    //                                   projectedLoc.subtractLocal(decalCenter);
    //
    //                                   
    //                                   newPositions[pos].set(projectedLoc);
    //                            }
                           }


                    }
                }
            }



                  positionBuff = BufferUtils.createFloatBuffer(newPositions);
                  normalBuffer = (BufferUtils.createFloatBuffer(newNormals));
                  texBuffer = BufferUtils.createFloatBuffer(newTexCoords);
                  indexBuffer = BufferUtils.createIntBuffer(newIndexes);

                  decalMesh.setBuffer(Type.Position, 3, positionBuff);
                  decalMesh.setBuffer(Type.Index, 3, indexBuffer);
                  decalMesh.setBuffer(Type.Normal, 3, normalBuffer);
                  decalMesh.setBuffer(Type.TexCoord, 2, texBuffer);

                  decalMesh.updateBound();

    //              for(int i = 0; i < decalMesh.getTriangleCount(); i++){
    //                  Triangle newTri = new Triangle();
    //                  decalMesh.getTriangle(i, newTri);
    //                  newTri.calculateNormal();
    //              }

              }
            catch(Exception e){
       //         e.printStackTrace();

                //error sometimes thrown : 'java.lang.IllegalStateException: Scene graph must be updated before checking collision'
             // when collisions with terrain are attempted while a light probe is rendering 

           }
          
          
            return decalMesh;
            
      }
 
       
       
    private boolean traverseNodeHierarchyForUD(String userDataString, Spatial item){
        boolean found = false;
        if(item != null){
            while(item.getParent() != null && !found){
                String string = item.getUserData(userDataString);
                if(string != null){
                    found = true;
                }
                item = item.getParent();
            }
        }
      return found;
    }
    
    
     public ArrayList getTerrainPatches(BoundingBox bounds) {
         if(activeMap != null){
             terrains = activeMap.getTerrains();
         }
         
         
         if(terrains != null){
            ArrayList terrainPatches = new ArrayList();

            for(Terrain terrain : terrains){
                Node terrainNode = (Node) terrain;

                if(bounds.intersects(terrainNode.getWorldBound())){
                    findTerrainPatches(terrainNode, terrainPatches, bounds);
                }

            }
            return terrainPatches;
         }
         else{
             return null;
         }
    }
     
     
     private void findTerrainPatches(Node node, ArrayList<TerrainPatch> store, BoundingVolume bounds){
        SafeArrayList<Spatial> children = (SafeArrayList<Spatial>) node.getChildren();
        
        for(Spatial spatial : children){
            if(spatial instanceof TerrainPatch){
                if(((TerrainPatch)spatial).getWorldBound().intersects(bounds)){
                    store.add((TerrainPatch) spatial);
                }
            }
            else if(spatial instanceof Node || spatial instanceof TerrainQuad){
                findTerrainPatches((Node) spatial, store, bounds);
            }
        }
    }

     
     
    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
    }

    @Override
    protected void cleanup(Application app) {
        
    }

    @Override
    protected void onEnable() {
        
    }

    @Override
    protected void onDisable() {
        
    }
    
}
