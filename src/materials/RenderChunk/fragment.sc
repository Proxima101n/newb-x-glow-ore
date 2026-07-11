$input v_color0, v_texcoord0, v_lightmapUV, v_extra

#include <bgfx_shader.sh>
#include <newb/main.sh>

SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_LightMapTexture);

void main() {
  #if defined(DEPTH_ONLY_OPAQUE) || defined(DEPTH_ONLY) || defined(INSTANCING)
    gl_FragColor = vec4(1.0,1.0,1.0,1.0);
    return;
  #endif

  vec4 diffuse = texture2D(s_MatTexture, v_texcoord0);
  vec4 color = v_color0;

  #ifdef ALPHA_TEST
    if (diffuse.a < 0.6) {
      discard;
    }
  #endif

  vec3 glow = nlGlow(s_MatTexture, v_texcoord0, v_extra.a);

  // vanilla: texture * vertex color * lightmap sample. no tonemap, no squaring, no fog (fog removed per earlier config)
  diffuse.rgb *= color.rgb;
  diffuse.rgb *= texture2D(s_LightMapTexture, v_lightmapUV).rgb;
  diffuse.rgb += glow;

  gl_FragColor = diffuse;
}