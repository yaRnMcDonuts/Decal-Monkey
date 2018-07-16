/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SpecialEffects.TextureEffects;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingSphere;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import java.util.ArrayList;

/**
 *
 * @author ryan
 */
public class TextureEffect {
    
    
//vars that get passed into shader arrays:
    public Vector4f color = new Vector4f(1f,1f,1f,1f);
    public Vector4f getColor(){ return color; }
    public void setColor(Vector4f c){ color = c; }
    public void setOpacity(float op){ color.w = op;} 
    
    public float scale = 1;
    public void setScale(float s){ scale = s; }
    public float getScale(){return scale; }
   
    public boolean circleShape = true;
    public void toggleCircleShape(boolean boo){ circleShape = boo;}
    public boolean isCircleShape() { return circleShape; }
    
    public float edgeFade = .1f; //fades the texture/color at .1 distance from edge of the effects by default
    public float getEdgeFade(){ return edgeFade;}
    public void setEdgeFade(float ef){ edgeFade = ef; }

    public Vector3f location = new Vector3f(0,0,0); //location of the projection box for creating teh mesh
    public Vector3f getLocation(){ return location; }
    public void setLocation(Vector3f loc){ location = loc; }
    
    public Vector3f centerLoc; //center of the effect/texture in the shader
    public Vector3f getCenterLocation(){ return centerLoc; }
    public void setCenterLocation(Vector3f loc){ centerLoc = loc; }

    public float radius = 1; //radius for the mesh box.
    public float getRadius(){ return radius;}
    public void setRadius(float rad){ radius = rad; }
    
    public float visibleRadius = 1; //radius for the visible zone in shader
    public float getVisibleRadius(){ return visibleRadius;}
    public void setVisibleRadius(float vrad){ visibleRadius = vrad; }
    
    public float yDepth;
    public float getYDepth(){ return yDepth; }
    
    public boolean indefinite = false;
    public boolean isIndefinite(){ return indefinite; }
    public void setIndefinite(boolean b){ indefinite = b; }
    
    public Vector3f direction = new Vector3f(0, -1, 0);
    public Vector3f getDirection(){ return direction; }
    public void setDirection(Vector3f dir){ direction = dir; }

    
    //the distance from the edge that should use noise. Set this equal to the radius in order to cover the entire texture effect with nosie effects
    public float standardNoiseEdgeDistance = 0;
    public float perlinNoiseEdgeDistance = 0;
    public float perlinNoiseWaveLength = 0;
    public Vector3f noiseVector = new Vector3f();
    public float getStandardNoiseEdgeDistance(){ return standardNoiseEdgeDistance ; }
    public float getPerlinNoiseEdgeDistance(){ return perlinNoiseEdgeDistance; }
    public float getPerlinNoiseWaveLength(){ return perlinNoiseWaveLength; }
    public Vector3f getNoiseVector() {  return noiseVector; }
    
    //Both noise types can be used at the same time for a mixed effect!
    public void useStandardNoise(float distToCover){
        standardNoiseEdgeDistance = distToCover;
        noiseVector.setX(distToCover);
        
    }
    public void usePerlinNoise(float distToCover, float waveLength){
        perlinNoiseEdgeDistance = distToCover;
        perlinNoiseWaveLength = waveLength;
        noiseVector.setY(distToCover);
        noiseVector.setZ(waveLength);
    }

    
// vars for use in the texture effect manager 
    private int id;
    public int getId(){ return id; }
    public void setId(int i){id = i; }
    
    public boolean ready = false;
    public boolean isReady(){ return ready; }
    
    public float duration;
    public float getDuration(){ return duration; }
    public void setDuration(float dur){ duration = dur; }
    
    public int depthPriority;
    public int getDepthPriority(){ return depthPriority; }
    public void setDepthPriority(int dp){ depthPriority = dp ; }
    
//projector box    
    public Vector3f projectorDimensions;
    public Geometry projectorGeom;
    public Geometry getProjectorBox() {
        return projectorGeom;
    }           
     public Vector3f getDimensions(){
        return projectorDimensions;
    }   
     
//MAterial, Mesh and Geom for the actual decal / effect
         
    public Material decalMat;
    public Material getMaterial(){ return decalMat; }
    public void setMaterial(Material mat){ 
        decalMat = mat;
        geometry.setMaterial(decalMat);
    }
    
    private Geometry geometry;
    private Mesh mesh;
    public Mesh getMesh(){ return mesh;}
    public Geometry getGeometry(){ return geometry; }   
    void setGeometry(Geometry geo) {
        geometry = geo;
        mesh = geo.getMesh();
    }

    public void setYDepth(float yd) {
        yDepth = yd;
    }

  

    
    
    //Constructor for an effect with no texture, only a solid color
    public TextureEffect(Material mat, Vector3f loc, float rad, float dur){
     //   color = col.toVector4f();
        setLocation(loc);
        duration = dur;
        radius = rad;
        visibleRadius = rad;
        
        decalMat = mat;
        
        newRadius = radius;
        newVisibleRadius = visibleRadius;
    }
    
  
     
     
    
    public void init(SimpleApplication app){
        ready = true;
        if(yDepth == 0){
            yDepth = radius;
        }
        
        projectorDimensions = new Vector3f(radius, yDepth, radius);
        
        Box boxMesh = new Box(radius, yDepth, radius); 
        projectorGeom = new Geometry("ColoredBox", boxMesh); 
        Material boxMat = app.getAssetManager().loadMaterial("Materials/almostInvis_mat_1.j3m");
        projectorGeom.setMaterial(boxMat); 
      //   app.getRootNode().attachChild(boxGeo);
         projectorGeom.setQueueBucket(RenderQueue.Bucket.Translucent);
         projectorGeom.updateModelBound();

         projectorGeom.setLocalTranslation(getLocation());
         
         
    }
    public void finished(){
        
    }
    
  
    
    //vars for effects and modifications
    public float newRadius;
    public float morphSpeed;
    
    public float newVisibleRadius;
    public float morphSpeedVis;
    
    public float newStandardNoiseEdgeDistance;
    public float newPerlinNoiseEdgeDistance;
    public float newPerlinNoiseWaveLength;
    public float standardNoiseMorphSpeed, perlinNoiseMorphSpeedWl, perlinNoiseMorphSpeedDist;
    
    public float newEdgeFade;
    public float edgeFadeMorphSpeed;
    
    
    public boolean smoothFadeTo = false;
    public Vector4f newColor;
    public Vector4f colorDiff;
    public float fadeSpeed;
    public float fadeDelay;
    
    public Vector3f lastLoc = null;
    public Vector3f getLastLoc(){ return lastLoc; }
    
    public void update(float tpf){
        duration -= tpf;
        if(projectorGeom != null){
            lastLoc = projectorGeom.getWorldTranslation().clone();
            
            projectorGeom.setLocalTranslation(location);
            
            projectorDimensions.setX(visibleRadius);
        }
        
        float rDiff = radius - newRadius;
        if(rDiff < -.15f || rDiff >.15f){
            float sizeMorphAmt = tpf * morphSpeed;
            if(newRadius > radius){ //grow
                radius += sizeMorphAmt;
                 if(radius > newRadius){ //make sure values dont pass the destination value
                    radius = newRadius;
                }
            }else{ //shrink
                radius -= sizeMorphAmt;
                if(radius < newRadius){ //make sure values dont pass the destination value
                    radius = newRadius;
                }
                if(radius <= 0){
                    duration = 0; //ends the effect if it has a radius of 0 (duration won't effect effects set as indefinite)
                }
            }
        }
        
        float vrDiff = visibleRadius - newVisibleRadius;
        if(vrDiff < -.15f || vrDiff >.15f){
           
            float sizeMorphAmt = tpf * morphSpeedVis;
            if(newVisibleRadius > visibleRadius){ //grow
                visibleRadius += sizeMorphAmt;
                if(visibleRadius > newVisibleRadius){ //make sure values dont pass the destination value
                    visibleRadius = newVisibleRadius;
                }
            }else{ //shrink
                visibleRadius -= sizeMorphAmt;
                 if(visibleRadius < newVisibleRadius){ //make sure values dont pass the destination value
                    visibleRadius = newVisibleRadius;
                }
            }
        }
       
        float edgeDiff = edgeFade - newEdgeFade;
        if(edgeDiff < -.15f || edgeDiff >.15f){
            float edgeMorphAmt = tpf * edgeFadeMorphSpeed;
            if(newEdgeFade > edgeFade){ //grow
                edgeFade += edgeMorphAmt;
                if(edgeFade > newEdgeFade){ //make sure values dont pass the destination value
                    edgeFade = newEdgeFade;
                }
            }else{ //shrink
                edgeFade -= edgeMorphAmt;
                  if(edgeFade < newEdgeFade){ //make sure values dont pass the destination value
                    edgeFade = newEdgeFade;
                }
            }
        }
         if(fadeDelay <=0){
            if(newColor != null){
                if(newColor != color){
                    if(!smoothFadeTo){
                        Vector4f diff = newColor.subtract(color);
                        if(colorDiff != null && diff.length() < colorDiff.length()){
                            color = newColor;
                        }
                        else{
                            colorDiff = diff;
                            colorDiff.normalizeLocal();
                            colorDiff.multLocal(fadeSpeed/100);
                            color.addLocal(colorDiff.mult(tpf));
                        }
                    }
                    else{
                        color.interpolateLocal(newColor, tpf * fadeSpeed);
                        if(color.w < .0001f){
                            color = newColor;
                        }
                    }
                }
                else{
                    newColor = null; //done fading
                }
            }
        }
        else{
            fadeDelay -= tpf;
        }
        
        float sNoiseDiff = newStandardNoiseEdgeDistance - standardNoiseEdgeDistance;
        if(sNoiseDiff < -.05f || sNoiseDiff >.05f){
            float noiseMorphAmt = tpf * standardNoiseMorphSpeed;
            if(newStandardNoiseEdgeDistance > standardNoiseEdgeDistance){ //grow
                standardNoiseEdgeDistance += noiseMorphAmt;
            }else{ //shrink
                standardNoiseEdgeDistance -= noiseMorphAmt;
            }
            noiseVector.setX(standardNoiseEdgeDistance);
        }
        
        float pNoiseDiff = newPerlinNoiseEdgeDistance - perlinNoiseEdgeDistance;
        if(pNoiseDiff < -.05f || pNoiseDiff >.05f){
            float noiseMorphAmt = tpf * perlinNoiseMorphSpeedDist;
            if(newPerlinNoiseEdgeDistance > perlinNoiseEdgeDistance){ //grow
                perlinNoiseEdgeDistance += noiseMorphAmt;
            }else{ //shrink
                perlinNoiseEdgeDistance -= noiseMorphAmt;
            }
            noiseVector.setY(perlinNoiseEdgeDistance);
        }
        pNoiseDiff = newPerlinNoiseWaveLength - perlinNoiseWaveLength;
        if(pNoiseDiff < -.05f || pNoiseDiff >.05f){
            float noiseMorphAmt = tpf * perlinNoiseMorphSpeedWl;
            if(newPerlinNoiseWaveLength > perlinNoiseWaveLength){ //grow
                perlinNoiseWaveLength += noiseMorphAmt;
            }else{ //shrink
                perlinNoiseWaveLength -= noiseMorphAmt;
            }
            noiseVector.setZ(perlinNoiseWaveLength);
        }
       
    }
    
        
//effect methods for altering size, opacity, color, noise, etc...

    
    public void morphToSize(float newRad, float spd){
        morphSpeed = spd;
        newRadius = newRad;
    }
    
    public void morphToVisibleSize(float newVisRad, float  spdv) {
        morphSpeedVis = spdv;
        newVisibleRadius = newVisRad;
    }
    
    
    //'linear' fades at a constant rate
    public void fadeToOpacityLinear(float newOp, float spd){
        fadeToColorLinear(new Vector4f(color.getX(), color.getY(), color.getZ(), newOp), spd);
    }
    public void fadeToColorLinear(Vector4f finalColor , float spd){
        fadeSpeed = spd * 10;
        newColor = finalColor;
        smoothFadeTo = false;
    }
    
    //'smooth' fades based on a percentage of current value. This is useful when fading at very low values. (i.e: despite being smaller mathematically, the difference from .01 -> .02 is more visually apparent than a jump from .7 -> .6 with colors
    public void fadeToOpacitySmooth(float newOp, float spd){
        fadeToColorSmooth(new Vector4f(color.getX(), color.getY(), color.getZ(), newOp), spd);
        smoothFadeTo = true;
    }
    public void fadeDelay(float fd){
        fadeDelay = fd;
    }
    
    public void fadeToColorSmooth(Vector4f finalColor , float spd){
        fadeSpeed = spd;
        newColor = finalColor;
        smoothFadeTo = true;
    }
    
    public void morphEdgeFadeToDistance(float newEfd, float spd){
        newEdgeFade = newEfd;
        edgeFadeMorphSpeed = spd;
        
    }
    
    public void morphToStandardNoise(float newDist, float spd){
        standardNoiseMorphSpeed = spd;
        newStandardNoiseEdgeDistance = newDist;
    }
    
    public void morphToPerlinNoise(float newDist, float newWl, float spd){
        perlinNoiseMorphSpeedWl = spd;
        perlinNoiseMorphSpeedDist = spd;
        newPerlinNoiseEdgeDistance = newDist;
        newPerlinNoiseWaveLength = newWl;
    }
    
    public void morphToPerlinDistance(float newDist, float spd){
        perlinNoiseMorphSpeedDist = spd;
        newPerlinNoiseEdgeDistance = newDist;
    }
    
    public void morphToPerlinWl(float newWl, float spd){
        perlinNoiseMorphSpeedWl = spd;
        newPerlinNoiseWaveLength = newWl;
    }


    
    
    
}
