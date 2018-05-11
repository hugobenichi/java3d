#version 400 core

in vec2 out_uv;

out vec4 out_color; // RGBA

uniform sampler2D sampler;

void main(void) {
  out_color = texture(sampler, out_uv);
}
