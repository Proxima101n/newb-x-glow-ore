$input a_color0, a_position, a_texcoord0, a_texcoord1
#ifdef INSTANCING
  $input i_data0, i_data1, i_data2, i_data3
#endif
$output v_color0, v_texcoord0, v_lightmapUV, v_extra

#include <bgfx_shader.sh>
#include <newb/main.sh>

uniform vec4 ViewPositionAndTime;

SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_LightMapTexture);

void main() {
  #ifdef INSTANCING
    mat4 model = mtxFromCols(i_data0, i_data1, i_data2, i_data3);
  #else
    mat4 model = u_model[0];
  #endif

  vec3 worldPos = mul(model, vec4(a_position, 1.0)).xyz;

  #if !(defined(DEPTH_ONLY_OPAQUE) || defined(DEPTH_ONLY) || defined(INSTANCING))

  vec3 cPos = a_position.xyz;

  #ifdef RENDER_AS_BILLBOARDS
    worldPos += vec3(0.5,0.5,0.5);
    vec3 modelCamPos = ViewPositionAndTime.xyz - worldPos;
    float camDis = length(modelCamPos);
    vec3 viewDir = modelCamPos / camDis;
    vec3 boardPlane = normalize(vec3(-viewDir.z, 0.0, viewDir.x));
    worldPos -= (((viewDir.zxy * boardPlane.yzx) - (viewDir.yzx * boardPlane.zxy)) *
                 (a_color0.z - 0.5)) +
                 (boardPlane * (a_color0.x - 0.5));
    vec4 color = vec4(1.0,1.0,1.0,1.0);
  #else
    vec4 color = a_color0;
  #endif

  vec2 uv0 = 2.0*a_texcoord0.xy;
  uv0 = fract(uv0) + ((floor(uv0)-0.5)/16384.0);

  vec2 uv1 = fract(a_texcoord1.y*vec2(256.0, 4096.0));

  // secret tint-mask flag: bit 8 of the packed texcoord1.y value (matches vanilla's v_ditheringAndMaskTinting.y)
  float tintMaskFlag = mod(floor(a_texcoord1.y * 256.0), 2.0);

  highp float t = ViewPositionAndTime.w;

  #if defined(NL_GLOW_SHIMMER) && !defined(RENDER_AS_BILLBOARDS)
    float shimmer = nlGlowShimmer(cPos, t);
  #else
    float shimmer = 1.0;
  #endif

  v_extra = vec4(0.0, tintMaskFlag, 0.0, shimmer);
  v_texcoord0 = uv0;
  v_lightmapUV = uv1;
  v_color0 = color;

  #endif

  vec4 pos = mul(u_viewProj, vec4(worldPos, 1.0));
  gl_Position = pos;
}