#version 400 core
#define MAX_AMOUNT_OF_MATERIALS 256

in vec3 totalPosition;
flat in vec3 normal;
flat in int textureData;
layout(early_fragment_tests) in;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out int side;

uniform sampler2DArray textures;

uniform int[MAX_AMOUNT_OF_MATERIALS] textureSizes;
uniform int maxTextureSize;

uniform int flags;
uniform float nightBrightness;
uniform float time;
uniform vec3 sunDirection;
uniform vec3 cameraPosition;

const int HEAD_UNDER_WATER_BIT = 1;
const int CALCULATE_SHADOWS_BIT = 2;

int isFlag(int bit) {
    return int((flags & bit) != 0);
}

float easeInOutQuart(float x) {
    //x < 0.5 ? 8 * x * x * x * x : 1 - pow(-2 * x + 2, 4) / 2;
    float inValue = 8.0 * x * x * x * x;
    float outValue = 1.0 - pow(-2.0 * x + 2.0, 4.0) / 2.0;
    return step(inValue, 0.5) * inValue + step(0.5, outValue) * outValue;
}

float getSkyLight() {
    return 1.0;
}

float getBlockLight() {
    return 0.0;
}

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

    float distance = length(cameraPosition - totalPosition);
    float angle = abs(dot((totalPosition - cameraPosition) / distance, normal));


    float absTime = abs(time);
    float skyLight = getSkyLight();
    float blockLight = getBlockLight();

    float sunIllumination = dot(normal, sunDirection) * nightBrightness * skyLight * absTime;
    float timeLight = max(nightBrightness, easeInOutQuart(absTime));
    float nightLight = -0.6 * (1 - absTime) * (1 - absTime);
    float light = max(blockLight + nightBrightness, max(nightBrightness, skyLight) * timeLight + sunIllumination);
    float waterFogMultiplier = min(1, isFlag(HEAD_UNDER_WATER_BIT) * max(0.5, distance * 0.000625));
    float fogMultiplier = 1 - exp(-distance * 0.000005);

    vec3 fragLight = vec3(light, light, max(blockLight + nightBrightness, max(nightBrightness, skyLight + nightLight) * timeLight + sunIllumination));
    vec3 fogColor = vec3(0.46, 0.63, 0.79) * fogMultiplier * timeLight * (1 - waterFogMultiplier);
    vec3 waterColor = (color.rgb + angle * vec3(0.0, 0.4, 0.15)) * fragLight * (1 - fogMultiplier);
    vec3 waterFog = vec3(0.0, 0.098, 0.643) * waterFogMultiplier * timeLight;
    fragColor = vec4((waterColor + waterFog + fogColor), color.a - angle * 0.3);
}