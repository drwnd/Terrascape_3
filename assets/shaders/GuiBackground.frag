#version 460 core

uniform sampler2D image;
uniform float rimWidth;
uniform float aspectRatio;
uniform vec2 size;

in vec2 fragTextureCoordinate;

out vec4 fragColor;

float getTextureCoord(float fragTextureCoord, float size, float thicknessMultiplier) {
    fragTextureCoord = min(fragTextureCoord, 1.0 - fragTextureCoord) * size;

    float thickness = rimWidth * thicknessMultiplier;

    if (fragTextureCoord >= thickness) return 0.5;
    return fragTextureCoord * (0.5 / thickness);
}

void main() {
    vec2 textureCoord = vec2(
    getTextureCoord(fragTextureCoordinate.x, size.x, 1),
    getTextureCoord(fragTextureCoordinate.y, size.y, aspectRatio)
    );

    vec4 color = texture(image, textureCoord);
    if (color.a == 0.0) discard;
    fragColor = color;
}