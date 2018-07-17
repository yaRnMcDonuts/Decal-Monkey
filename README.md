# Decal-Monkey
App state and shaders for projecting and managing decals and texture effects.

How to apply a Decal:

Initiate the TextureEffectManagerState -
1. Create a new "TextureEffectManagerAppState" object; attach and enable the state.
2. Set the node and terrain that you want to apply decals to ( or alternatively, set a "DecalEnabledMap" and implement the "DecalEnabledMap" in your own class. This is useful for paged worlds/maps, or worlds/maps that change dynamically)

        TextureEffectManagerState textureEffectManagerState = new TextureEffectManagerState();
        app.getStateManager().attach(textureEffectManagerState);
        textureEffectManagerState.setEnabled(true);
        


Create and register a new Decal / Texture Effect -
1. Create a new decal object
2. Set a material that uses the "TextureEffect.j3md" MaterialDefiniation.
3. Register the decal with the TextureEffectManagerAppState

       Material decalMat = app.getAssetManager().loadMaterial("Effects/plasmaCratorEffect.j3m");
       
       float radius = 7f;
       float duration = 10f;  
       Vector3f location = new Vector3f(10, 0, 0);
       cratorTextureEffect = new TextureEffect(decalMat, location, radius, duration);
       
       //customize the effect to your needs
       cratorTextureEffect.setVisibleRadius(4);
       cratorTextureEffect.setEdgeFade(3);
       cratorTextureEffect.setScale(2); 
       cratorTextureEffect.setDepthPriority(1); 
       cratorTextureEffect.morphToVisibleSize(8.5f, 6.9f); //8.5 is the morph speed, 6.9 is the final radius
       cratorTextureEffect.usePerlinNoise(3, 7.39f);  // 3 is the distance from the edge of the effect to apply noise to. 7.39 is the wavelength
       cratorTextureEffect.setIndefinite(); // causes the decal to ignore its duration, and last indefinitely, or until manually removed

       gameState.registerTextureEffect(cratorTextureEffect);




Methods for applying transitions and effects to a Decal --

- wip




