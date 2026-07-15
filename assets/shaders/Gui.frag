#version 460 core

uniform sampler2D image;

in vec2 fragTextureCoordinate;

out vec4 fragColor;

void main() {
    vec4 color = texture(image, fragTextureCoordinate);
    if (color.a == 0.0) discard;
    fragColor = color;
}
