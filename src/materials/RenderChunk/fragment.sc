$input v_color0, v_color1, v_fog, v_texcoord0, v_lightmapUV, v_extra

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

  diffuse.rgb *= diffuse.rgb;
  diffuse.a = 1.0;

  diffuse.rgb *= color.rgb;
  diffuse.rgb += glow;

  diffuse.rgb = mix(diffuse.rgb, v_fog.rgb, v_fog.a);
  diffuse.rgb = colorCorrection(diffuse.rgb);

  gl_FragColor = diffuse;
}