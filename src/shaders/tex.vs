#version 400 core

in vec3 position;
in vec2 uv;

out vec2 out_uv;

void main(void) {
  gl_Position = vec4(position.xyz, 1.0);
  out_uv = uv;
}
