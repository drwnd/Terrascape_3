#version 400 core
#define MAX_AMOUNT_OF_MATERIALS 256

flat in int textureData;
in vec3 totalPosition;
layout(early_fragment_tests) in;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out int side;

uniform sampler2DArray textures;

uniform int[MAX_AMOUNT_OF_MATERIALS] textureSizes;
uniform int maxTextureSize;

vec2 getUVOffset(int side, int textureSize) {
    float invTextureSize = 1.0 / textureSize;
    vec3 textureCoordinate = fract(totalPosition * invTextureSize);
    float normalizer = float(textureSize) / maxTextureSize;

    switch (side) {
        case 0: return vec2(textureCoordinate.x, 1 - textureCoordinate.y) * normalizer;
        case 1: return textureCoordinate.xz * normalizer;
        case 2: return (1 - textureCoordinate.zy) * normalizer;
        case 3: return (1 - textureCoordinate.xy) * normalizer;
        case 4: return textureCoordinate.xz * normalizer;
        case 5: return vec2(textureCoordinate.z, 1 - textureCoordinate.y) * normalizer;
    }

    return fract(textureCoordinate.zx);
}

void main() {
    side = textureData >> 8 & 7;
    int material = textureData & 0xFF;
    int textureSize = textureSizes[material];

    vec4 color = texture(textures, vec3(getUVOffset(side, textureSize), textureData & 0xFF));
    if (color.a == 0) discard;

    fragColor = color;
}
