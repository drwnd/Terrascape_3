#version 400 core
#define MAX_AMOUNT_OF_MATERIALS 256

flat in int textureData;
flat in vec3 normal;
in vec3 texturePosition;
in vec3 voxelPosition;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out int side;

uniform sampler2DArray textures;
uniform sampler2DArray propertiesTextures;
uniform sampler2D shadowMap;
uniform mat4 sunMatrix;

uniform int[MAX_AMOUNT_OF_MATERIALS] textureSizes;
uniform int maxTextureSize;

uniform int flags;
uniform float nightBrightness;
uniform float time;
uniform vec3 sunDirection;
uniform vec3 cameraPosition;

const int HEAD_UNDER_WATER_BIT = 1;
const int DO_SHADOW_MAPPING_BIT = 2;

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

int isFlag(int bit) {
    return int((flags & bit) != 0);
}

float easeInOutQuart(float x) {
    float inValue = 8.0 * x * x * x * x;
    float outValue = 1.0 - pow(-2.0 * x + 2.0, 4.0) * 0.5;
    return step(inValue, 0.5) * inValue + step(0.5, outValue) * outValue;
}

float getSkyLight(vec3 position, vec3 normal) {
    if (isFlag(DO_SHADOW_MAPPING_BIT) == 0) return 1.0;
//    if (dot(normal, sunDirection) < 0.0) return 0.0;
    vec4 shadowCoord = sunMatrix * vec4(floor(position + normal * 2.5), 1);
    shadowCoord.xyz /= shadowCoord.w;
    shadowCoord = shadowCoord * 0.5 + 0.5;

    float closestDepth = texture(shadowMap, shadowCoord.xy).r;
    if (closestDepth == 1.0) return 1.0;
    float currentDepth = shadowCoord.z;

    return currentDepth - 0.005 > closestDepth ? 0.5 : 1.0;
}

float getBlockLight(vec3 position, vec3 normal) {
    return 0.0;
}

vec3 getColor(vec3 color, vec3 textureCoord) {
    float emissivness = texture(propertiesTextures, textureCoord).r;
    float absTime = abs(time);
    float skyLight = getSkyLight(voxelPosition, normal);
    float blockLight = getBlockLight(texturePosition, normal);

    float sunIllumination = dot(normal, sunDirection) * 0.2 * skyLight * absTime;
    float timeLight = max(nightBrightness, easeInOutQuart(absTime));
    float nightLight = 0.6 * (1 - absTime) * (1 - absTime);
    float light = max(blockLight + nightBrightness, max(nightBrightness, skyLight) * timeLight + sunIllumination);
    float distance = length(cameraPosition - texturePosition);
    float waterFogMultiplier = min(1, isFlag(HEAD_UNDER_WATER_BIT) * max(0.5, distance * 0.000625));
    float fogMultiplier = 1 - exp(-distance * 0.000005);

    vec3 fragLight = max(vec3(emissivness), vec3(light, light, max(nightBrightness, max(nightBrightness, skyLight + nightLight) * timeLight + sunIllumination)));
    vec3 baseColor = color * fragLight * (1 - waterFogMultiplier) * (1 - fogMultiplier);
    vec3 waterColor = vec3(0.0, 0.098, 0.643) * waterFogMultiplier * timeLight;
    vec3 fogColor = vec3(0.46, 0.63, 0.79) * fogMultiplier * timeLight;

    return baseColor + waterColor + fogColor;
}

void main() {
    side = textureData >> 8 & 7;
    int material = textureData & 0xFF;
    int textureSize = textureSizes[material];

    vec3 textureCoord = vec3(getUVOffset(side, textureSize), material);
    vec4 color = texture(textures, textureCoord);
    if (color.a == 0) discard;

    color.rgb = getColor(color.rgb, textureCoord);
    fragColor = vec4(color);
}
