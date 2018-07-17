#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/PBR.glsllib"
#import "Common/ShaderLib/Parallax.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"


float rand(float n){return fract(sin(n) * 43758.5453123);}
float rand(vec2 n) { 
	return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);
}

float noise(vec2 n) {
	const vec2 d = vec2(0.0, 1.0);
  vec2 b = floor(n), f = smoothstep(vec2(0.0), vec2(1.0), fract(n));
	return mix(mix(rand(b), rand(b + d.yx), f.x), mix(rand(b + d.xy), rand(b + d.yy), f.x), f.y);
}


float prand(vec2 c){
	return fract(sin(dot(c.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

float pnoise(vec2 p, float freqPct){
	//float unit = circ/freq;
        float unit = freqPct;

	vec2 ij = floor(p/unit);
	vec2 xy = mod(p,unit)/unit;
	//xy = 3.*xy*xy-2.*xy*xy*xy;
	xy = .5*(1.-cos(3.14159*xy));
	float a = prand((ij+vec2(0.,0.)));
	float b = prand((ij+vec2(1.,0.)));
	float c = prand((ij+vec2(0.,1.)));
	float d = prand((ij+vec2(1.,1.)));
	float x1 = mix(a, b, xy.x);
	float x2 = mix(c, d, xy.x);
	return mix(x1, x2, xy.y);
}



// - - - - - - - - 

float rand3D(in vec3 co){
    return fract(sin(dot(co.xyz ,vec3(12.9898,78.233,144.7272))) * 43758.5453);
}
    
float simple_interpolate(in float a, in float b, in float x)
{
   return a + smoothstep(0.0,1.0,x) * (b-a);
}
float interpolatedNoise3D(in float x, in float y, in float z)
{
    float integer_x = x - fract(x);
    float fractional_x = x - integer_x;

    float integer_y = y - fract(y);
    float fractional_y = y - integer_y;

    float integer_z = z - fract(z);
    float fractional_z = z - integer_z;

    float v1 = rand3D(vec3(integer_x, integer_y, integer_z));
    float v2 = rand3D(vec3(integer_x+1.0, integer_y, integer_z));
    float v3 = rand3D(vec3(integer_x, integer_y+1.0, integer_z));
    float v4 = rand3D(vec3(integer_x+1.0, integer_y +1.0, integer_z));

    float v5 = rand3D(vec3(integer_x, integer_y, integer_z+1.0));
    float v6 = rand3D(vec3(integer_x+1.0, integer_y, integer_z+1.0));
    float v7 = rand3D(vec3(integer_x, integer_y+1.0, integer_z+1.0));
    float v8 = rand3D(vec3(integer_x+1.0, integer_y +1.0, integer_z+1.0));

    float i1 = simple_interpolate(v1,v5, fractional_z);
    float i2 = simple_interpolate(v2,v6, fractional_z);
    float i3 = simple_interpolate(v3,v7, fractional_z);
    float i4 = simple_interpolate(v4,v8, fractional_z);

    float ii1 = simple_interpolate(i1,i2,fractional_x);
    float ii2 = simple_interpolate(i3,i4,fractional_x);

    return simple_interpolate(ii1 , ii2 , fractional_y);
}

float Noise3D(in vec3 coord, in float wavelength)
{
   return interpolatedNoise3D(coord.x/wavelength, coord.y/wavelength, coord.z/wavelength);
}




    uniform float m_Scale;
    uniform float m_Radius;
    uniform vec3 m_Noise;


varying vec2 texCoord;
#ifdef SEPARATE_TEXCOORD
  varying vec2 texCoord2;
#endif

varying vec4 Color;

uniform vec4 g_LightData[NB_LIGHTS];

uniform vec3 g_CameraPosition;

uniform float m_Roughness;
uniform float m_Metallic;

varying vec3 wPosition;    




#ifdef INDIRECT_LIGHTING
//  uniform sampler2D m_IntegrateBRDF;
  uniform samplerCube g_PrefEnvMap;
  uniform vec3 g_ShCoeffs[9];
  uniform vec4 g_LightProbeData;
#endif

#ifdef BASECOLORMAP
  uniform sampler2D m_BaseColorMap;
#endif

#ifdef USE_PACKED_MR
     uniform sampler2D m_MetallicRoughnessMap;
#else
    #ifdef METALLICMAP
      uniform sampler2D m_MetallicMap;
    #endif
    #ifdef ROUGHNESSMAP
      uniform sampler2D m_RoughnessMap;
    #endif
#endif

#ifdef EMISSIVE
    uniform vec4 m_Emissive;
#endif
#ifdef EMISSIVEMAP
    uniform sampler2D m_EmissiveMap;
#endif
#if defined(EMISSIVE) || defined(EMISSIVEMAP)
    uniform float m_EmissivePower;
    uniform float m_EmissiveIntensity;
#endif 

#ifdef SPECGLOSSPIPELINE

  uniform vec4 m_Specular;
  uniform float m_Glossiness;
  #ifdef USE_PACKED_SG
    uniform sampler2D m_SpecularGlossinessMap;
  #else
    uniform sampler2D m_SpecularMap;
    uniform sampler2D m_GlossinessMap;
  #endif
#endif

#ifdef PARALLAXMAP
  uniform sampler2D m_ParallaxMap;  
#endif
#if (defined(PARALLAXMAP) || (defined(NORMALMAP_PARALLAX) && defined(NORMALMAP)))
    uniform float m_ParallaxHeight;
#endif

#ifdef LIGHTMAP
  uniform sampler2D m_LightMap;
#endif
  
#if defined(NORMALMAP) || defined(PARALLAXMAP)
  uniform sampler2D m_NormalMap;   
  varying vec4 wTangent;
#endif
varying vec3 wNormal;

#ifdef DISCARD_ALPHA
uniform float m_AlphaDiscardThreshold;
#endif

uniform vec3 m_CenterPoint;
uniform vec3 m_ProjectorDimensions;

uniform float m_EdgeFadeDistance;

uniform bool m_CircleShape;

varying vec3 mPos;
varying vec3 mNorm;

void main(){
    
  //modify tex coords if this decal / effect has a different scale


    float scale = m_Scale; 

    
    vec2 scaledTexCoord = texCoord * scale;

    float texX = scaledTexCoord.x;
    float texZ = scaledTexCoord.y;

    texX = mod(texX, 1);
    texZ = mod(texZ, 1);

    scaledTexCoord = vec2(texX, texZ);


    vec2 newTexCoord;
    vec3 viewDir = normalize(g_CameraPosition - wPosition);

    vec3 norm = normalize(wNormal);
    #if defined(NORMALMAP) || defined(PARALLAXMAP)
        vec3 tan = normalize(wTangent.xyz);
        mat3 tbnMat = mat3(tan, wTangent.w * cross( (norm), (tan)), norm);
    #endif

    #if (defined(PARALLAXMAP) || (defined(NORMALMAP_PARALLAX) && defined(NORMALMAP)))
       vec3 vViewDir =  viewDir * tbnMat;  
       #ifdef STEEP_PARALLAX
           #ifdef NORMALMAP_PARALLAX
               //parallax map is stored in the alpha channel of the normal map         
               newTexCoord = steepParallaxOffset(m_NormalMap, vViewDir, scaledTexCoord, m_ParallaxHeight);
           #else
               //parallax map is a texture
               newTexCoord = steepParallaxOffset(m_ParallaxMap, vViewDir, scaledTexCoord, m_ParallaxHeight);         
           #endif
       #else
           #ifdef NORMALMAP_PARALLAX
               //parallax map is stored in the alpha channel of the normal map         
               newTexCoord = classicParallaxOffset(m_NormalMap, vViewDir, scaledTexCoord, m_ParallaxHeight);
           #else
               //parallax map is a texture
               newTexCoord = classicParallaxOffset(m_ParallaxMap, vViewDir, scaledTexCoord, m_ParallaxHeight);
           #endif
       #endif
    #else
       newTexCoord = scaledTexCoord;    
    #endif
    
    #ifdef TRIPLANAR



  vec3 blending = abs( wNormal );
  blending = normalize(max(blending, 0.00001)); // Force weights to sum to 1.0
  float b = (blending.x + blending.y + blending.z);
  blending /= vec3(b, b, b);

        #ifdef BASECOLORMAP
            vec4 xaxis = texture2D( m_BaseColorMap, mod(wPosition.yz* scale *.05, 1));
            vec4 yaxis = texture2D( m_BaseColorMap, mod(wPosition.xz* scale *.05, 1));
            vec4 zaxis = texture2D( m_BaseColorMap, mod(wPosition.xy* scale *.05, 1));


  //          vec4 col1 = texture2D( m_BaseColorMap, mod(mPos.yz * scale*.05, 1));
  //          vec4 col2 = texture2D( m_BaseColorMap, mod(mPos.xz  * scale*.05, 1));
  //          vec4 col3 = texture2D( m_BaseColorMap, mod(mPos.xy  * scale *.05, 1)); 
            // blend the results of the 3 planar projections.
  //          vec4 albedo = (col1 * blending.x) + (col2 * blending.y) + (col3 * blending.z);
            vec4 albedo = xaxis * blending.x + yaxis * blending.y + zaxis * blending.z;

        //    albedo.a = (texture2D(m_BaseColorMap, newTexCoord) * Color).a;

//albedo.rgb = norm;
        #else
            vec4 albedo = Color;
        #endif
    #else
        #ifdef BASECOLORMAP
            vec4 albedo = texture2D(m_BaseColorMap, newTexCoord) * Color;
        #else
            vec4 albedo = Color;
        #endif
    #endif

    #ifdef USE_PACKED_MR
        vec2 rm = texture2D(m_MetallicRoughnessMap, newTexCoord).gb;
        float Roughness = rm.x * max(m_Roughness, 1e-4);
        float Metallic = rm.y * max(m_Metallic, 0.0);
    #else
        #ifdef ROUGHNESSMAP
            float Roughness = texture2D(m_RoughnessMap, newTexCoord).r * max(m_Roughness, 1e-4);
        #else
            float Roughness =  max(m_Roughness, 1e-4);
        #endif
        #ifdef METALLICMAP
            float Metallic = texture2D(m_MetallicMap, newTexCoord).r * max(m_Metallic, 0.0);
        #else
            float Metallic =  max(m_Metallic, 0.0);
        #endif
    #endif
 
    float alpha = albedo.a;

    #ifdef DISCARD_ALPHA
        if(alpha < m_AlphaDiscardThreshold){
            discard;
        }
        
    #endif

 
    // ***********************
    // Read from textures
    // ***********************
    #if defined(NORMALMAP)
      vec4 normalHeight = texture2D(m_NormalMap, newTexCoord);
      //Note the -2.0 and -1.0. We invert the green channel of the normal map, 
      //as it's complient with normal maps generated with blender.
      //see http://hub.jmonkeyengine.org/forum/topic/parallax-mapping-fundamental-bug/#post-256898
      //for more explanation.
      vec3 normal = normalize((normalHeight.xyz * vec3(2.0, NORMAL_TYPE * 2.0, 2.0) - vec3(1.0, NORMAL_TYPE * 1.0, 1.0)));
      normal = normalize(tbnMat * normal);
      //normal = normalize(normal * inverse(tbnMat));
    #else
      vec3 normal = norm;
    #endif

// ____________________ \/ TEXTURE EFFECTS \/

    float perlinNoise = 0; 
    float standardNoise = 0;
    float totalNoise = 0;
//    #ifdef USENOISE
    if(m_Noise.x > 0){
        standardNoise = interpolatedNoise3D(wPosition.x, wPosition.y, wPosition.z) * m_Noise.x;
    }
    if(m_Noise.y > 0 || m_Noise.z > 0){
       perlinNoise = Noise3D(wPosition.xyz, m_Noise.y) * m_Noise.z;
    }
        
 //       float weightTotal = m_Noise.x + m_Noise.z;
        totalNoise = (perlinNoise + standardNoise) * .5;// (perlinNoise *(m_Noise.z / weightTotal)) + (standardNoise *(m_Noise.x / weightTotal)) * .5;
//    #endif
    
    #ifdef USECIRCLESHAPE
        float fragDist = distance(wPosition, m_CenterPoint);
        float originalFragDist = fragDist;
        fragDist += totalNoise;
        float radius = m_ProjectorDimensions.x; ;

        #ifdef RADIUS
            radius = m_Radius;
//        #else
//            radius = m_ProjectorDimensions.x; 
        #endif


        float edgeFadeLimit = radius - m_EdgeFadeDistance;

         if(fragDist > radius){
            discard;
        }
        
        else if(fragDist > edgeFadeLimit){
      //      float slerp = max( m_EdgeFadeDistance /edgeFadeLimit, 0 );
     //       alpha = mix(alpha, 0.0, slerp);
 alpha *= pow(((radius - fragDist) / m_EdgeFadeDistance), 2);

         }
        

        if(alpha <= 0 ){
             discard;
        }
        else{
            alpha = min(alpha, 1.0);
        }
    #else
// 'crop' the square here using transparency, until you do so in the mesh creation
          vec3 dist = (wPosition - m_CenterPoint);
          if(dist.x < 0){
            dist.x *= -1;
          }
          if(dist.z < 0 ){
            dist.z *= -1;
          }
         if(dist.y < 0 ){
            dist.y *= -1;
          }
          
          dist.z += totalNoise;
          dist.x += totalNoise;


          float edgeFadeLimitX = m_ProjectorDimensions.x - m_EdgeFadeDistance;
          float edgeFadeLimitZ = m_ProjectorDimensions.z - m_EdgeFadeDistance;
          float edgeFadeLimitY = m_ProjectorDimensions.y - m_EdgeFadeDistance;

          if(dist.x > m_ProjectorDimensions.x || dist.z > m_ProjectorDimensions.z || dist.y > m_ProjectorDimensions.y){
            discard;
          }
          else{
            if(dist.x > edgeFadeLimitX){
                alpha *= (m_ProjectorDimensions.x - dist.x) / m_EdgeFadeDistance;
            }
            if(dist.z > edgeFadeLimitZ){
                alpha *= (m_ProjectorDimensions.z - dist.z) / m_EdgeFadeDistance;
            }
            if(dist.y > edgeFadeLimitY){
                alpha *= (m_ProjectorDimensions.y - dist.y) / m_EdgeFadeDistance;
            }
          }

    #endif




    float specular = 0.5;
    #ifdef SPECGLOSSPIPELINE

        #ifdef USE_PACKED_SG
            vec4 specularColor = texture2D(m_SpecularGlossinessMap, newTexCoord);
            float glossiness = specularColor.a * m_Glossiness;
            specularColor *= m_Specular;
        #else
            #ifdef SPECULARMAP
                vec4 specularColor = texture2D(m_SpecularMap, newTexCoord);
            #else
                vec4 specularColor = vec4(1.0);
            #endif
            #ifdef GLOSSINESSMAP
                float glossiness = texture2D(m_GlossinesMap, newTexCoord).r * m_Glossiness;
            #else
                float glossiness = m_Glossiness;
            #endif
            specularColor *= m_Specular;
        #endif
        vec4 diffuseColor = albedo;// * (1.0 - max(max(specularColor.r, specularColor.g), specularColor.b));
        Roughness = 1.0 - glossiness;
    #else      
        float nonMetalSpec = 0.08 * specular;
        vec4 specularColor = (nonMetalSpec - nonMetalSpec * Metallic) + albedo * Metallic;
        vec4 diffuseColor = albedo - albedo * Metallic;
    #endif

    #ifdef LIGHTMAP
       vec3 lightMapColor;
       #ifdef SEPARATE_TEXCOORD
          lightMapColor = texture2D(m_LightMap, texCoord2).rgb;
       #else
          lightMapColor = texture2D(m_LightMap, scaledTexCoord).rgb;
       #endif
       #ifdef AO_MAP
         lightMapColor.gb = lightMapColor.rr;
       #endif
       specularColor.rgb *= lightMapColor;
       albedo.rgb  *= lightMapColor;
    #endif

    gl_FragColor.rgb = vec3(0.0);
    float ndotv = max( dot( normal, viewDir ),0.0);
    for( int i = 0;i < NB_LIGHTS; i+=3){
        vec4 lightColor = g_LightData[i];
        vec4 lightData1 = g_LightData[i+1];                
        vec4 lightDir;
        vec3 lightVec;            
        lightComputeDir(wPosition, lightColor.w, lightData1, lightDir, lightVec);

        float fallOff = 1.0;
        #if __VERSION__ >= 110
            // allow use of control flow
        if(lightColor.w > 1.0){
        #endif
            fallOff =  computeSpotFalloff(g_LightData[i+2], lightVec);
        #if __VERSION__ >= 110
        }
        #endif
        //point light attenuation
        fallOff *= lightDir.w;

        lightDir.xyz = normalize(lightDir.xyz);            
        vec3 directDiffuse;
        vec3 directSpecular;
        
        PBR_ComputeDirectLight(normal, lightDir.xyz, viewDir,
                            lightColor.rgb,specular, Roughness, ndotv,
                            directDiffuse,  directSpecular);

        vec3 directLighting = diffuseColor.rgb *directDiffuse + directSpecular;
        
        gl_FragColor.rgb += directLighting * fallOff;
    }

    #ifdef INDIRECT_LIGHTING
        vec3 rv = reflect(-viewDir.xyz, normal.xyz);
        //prallax fix for spherical bounds from https://seblagarde.wordpress.com/2012/09/29/image-based-lighting-approaches-and-parallax-corrected-cubemap/
        // g_LightProbeData.w is 1/probe radius + nbMipMaps, g_LightProbeData.xyz is the position of the lightProbe.
        float invRadius = fract( g_LightProbeData.w);
        float nbMipMaps = g_LightProbeData.w - invRadius;
        rv = invRadius * (wPosition - g_LightProbeData.xyz) +rv;

         //horizon fade from http://marmosetco.tumblr.com/post/81245981087
        float horiz = dot(rv, norm);
        float horizFadePower = 1.0 - Roughness;
        horiz = clamp( 1.0 + horizFadePower * horiz, 0.0, 1.0 );
        horiz *= horiz;

        vec3 indirectDiffuse = vec3(0.0);
        vec3 indirectSpecular = vec3(0.0);
        indirectDiffuse = sphericalHarmonics(normal.xyz, g_ShCoeffs) * diffuseColor.rgb;
        vec3 dominantR = getSpecularDominantDir( normal, rv.xyz, Roughness*Roughness );
        indirectSpecular = ApproximateSpecularIBLPolynomial(g_PrefEnvMap, specularColor.rgb, Roughness, ndotv, dominantR, nbMipMaps);
        indirectSpecular *= vec3(horiz);

        vec3 indirectLighting =  indirectDiffuse + indirectSpecular;

        gl_FragColor.rgb = gl_FragColor.rgb + indirectLighting * step( 0.0, g_LightProbeData.w);
    #endif
 
    #if defined(EMISSIVE) || defined (EMISSIVEMAP)
        #ifdef EMISSIVEMAP
            vec4 emissive = texture2D(m_EmissiveMap, newTexCoord);
        #else
            vec4 emissive = m_Emissive;
        #endif
        gl_FragColor += emissive * pow(emissive.a, m_EmissivePower) * m_EmissiveIntensity;
    #endif
    gl_FragColor.a = alpha;

   
}
