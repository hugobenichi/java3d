#version 400 core

in vec3 position;
in vec2 uv;

out vec2 out_uv;
out float z;

uniform float base_s;
uniform float base_z;
uniform vec2 world_xy;
uniform vec3 translation; // TODO: apply projection matrix scaling so that translation can be specified in world tiles !
uniform mat4 projection;

void main(void) {
  vec4 p = vec4(position.xyz, 1.0);
  p.xy += world_xy;
  p.z += base_z;
  p = projection * p;
  p.xyz += translation * p.w;           // post-projection translation: don't forget the w scaling
  p.w *= base_s;

  gl_Position = p;
  out_uv = uv;
  z = position.z;
}
