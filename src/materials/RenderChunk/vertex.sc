$input a_color0, a_position, a_texcoord0, a_texcoord1
#ifdef INSTANCING
  $input i_data0, i_data1, i_data2, i_data3
#endif
$output v_color0, v_color1, v_fog, v_texcoord0, v_lightmapUV, v_extra

#include <bgfx_shader.sh>
#include <newb/main.sh>

uniform vec4 RenderChunkFogAlpha;
uniform vec4 FogAndDistanceControl;
uniform vec4 ViewPositionAndTime;
uniform vec4 FogColor;
uniform vec4 DimensionID;
uniform vec4 TimeOfDay;
uniform vec4 Day;
uniform vec4 CameraPosition;

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
    vec3 modelCamPos = ViewPositionAndTime.xyz - worldPos;
    float camDis = length(modelCamPos);
    vec3 viewDir = modelCamPos / camDis;

    vec4 color = a_color0;
  #endif

  float relativeDist = camDis / FogAndDistanceControl.z;

  vec3 cPos = a_position.xyz;
  vec3 bPos = fract(cPos);

  vec2 uv0 = 2.0*a_texcoord0.xy;
  uv0 = fract(uv0) + ((floor(uv0)-0.5)/16384.0);

  vec2 uv1 = fract(a_texcoord1.y*vec2(256.0, 4096.0));
  vec2 lit = uv1*uv1;

  bool isColored = color.r != color.g || color.r != color.b;
  float shade = isColored ? color.g*1.5 : color.g;

  #if defined(ALPHA_TEST) && !defined(RENDER_AS_BILLBOARDS)
    bool isTree = (isColored && (bPos.x+bPos.y+bPos.z < 0.001)) || color.a == 0.0;
  #else
    bool isTree = false;
  #endif

  nl_environment env = nlDetectEnvironment(DimensionID.x, TimeOfDay.x, Day.x, FogColor.rgb, FogAndDistanceControl.xyz);
  nl_skycolor skycol = nlSkyColors(env);

  highp float t = ViewPositionAndTime.w;

  #ifdef SEASONS
    isTree = true;
    color.w *= color.w;
    color = vec4(color.www, 1.0);
  #else
    if (isColored) {
      color.rgb *= color.rgb*1.2;
    }
  #endif

  vec3 torchColor;
  vec3 light = nlLighting(s_LightMapTexture, skycol, env, worldPos, torchColor, a_color0.rgb, uv1, lit, isTree, shade, t, FogAndDistanceControl.z, TimeOfDay.x, CameraPosition.xyz);

  relativeDist += RenderChunkFogAlpha.x;

  vec4 fogColor;
  fogColor.rgb = skycol.horizon;
  fogColor.a = nlRenderFogFade(relativeDist, FogColor.rgb, FogAndDistanceControl.xy);

  if (env.nether) {
    fogColor.rgb = colorCorrectionInv(FogColor.rgb);
  }

  color.rgb *= light;

  #if defined(NL_GLOW_SHIMMER) && !(defined(RENDER_AS_BILLBOARDS) || defined(SEASONS))
    float shimmer = nlGlowShimmer(cPos, t);
  #else
    float shimmer = 1.0;
  #endif

  v_extra = vec4(shade, worldPos.y, 0.0, shimmer);
  v_texcoord0 = uv0;
  v_lightmapUV = uv1;
  v_color0 = color;
  v_color1 = a_color0;
  v_fog = fogColor;

  #endif

  vec4 pos = mul(u_viewProj, vec4(worldPos, 1.0));
  gl_Position = pos;
}