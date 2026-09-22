#version 460 core

uniform sampler2DArray image;
uniform int layer;

in vec2 fragTextureCoordinate;

out vec4 fragColor;

void main() {
    vec4 color = texture(image, vec3(fragTextureCoordinate, layer));
    if (color.a == 0.0) discard;
    fragColor = color;
}
