#version 400 core

in vec3 position;
in vec2 uv;

out vec2 out_uv;
out float z;

uniform vec3 translation;
uniform mat4 projection;

void main(void) {
  gl_Position = projection * (vec4(0,0,-13,0) + vec4(translation.xyz, 0) + vec4(position.xyz, 1.0));
  //gl_Position = vec4(translation.xyz, 0) + projection * vec4(position.xyz, 1.0);
  out_uv = uv;
  z = position.z;
}
