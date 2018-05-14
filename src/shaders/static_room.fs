#version 400 core

in vec2 out_uv;
in float z;

out vec4 out_color; // RGBA

uniform sampler2D sampler;

void main(void) {

  vec2 uv = vec2(out_uv.x + floor(z) * 0.5, out_uv.y);

  out_color = texture(sampler, uv);
}
