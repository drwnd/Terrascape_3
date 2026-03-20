#version 400 core
#define MAX_AMOUNT_OF_MATERIALS 256
#define HEAD_UNDER_WATER_BIT 1
#define DO_SHADOW_MAPPING_BIT 2
#define DO_GLASS_SHADOWS_BIT 4

flat in int textureData;
flat in vec3 normal;
in vec3 texturePosition;
in vec3 voxelPosition;

out vec4 fragColor;

uniform sampler2DArray textures;
uniform int[MAX_AMOUNT_OF_MATERIALS] textureSizes;
uniform int maxTextureSize;

vec2 getUVOffset(int side, int textureSize) {
    float invTextureSize = 1.0 / textureSize;
    vec3 textureCoordinate = fract(texturePosition * invTextureSize);
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
    int side = textureData >> 8 & 7;
    int material = textureData & 0xFF;
    int textureSize = textureSizes[material];
    vec3 textureCoord = vec3(getUVOffset(side, textureSize), material);

    fragColor = texture(textures, textureCoord) * vec4(0.5, 0.8, 1.2, 1);
    if (fragColor.a == 0) discard;
}