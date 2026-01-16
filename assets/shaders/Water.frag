#version 400 core
#define MAX_AMOUNT_OF_MATERIALS 256
#define HEAD_UNDER_WATER_BIT 1
#define DO_SHADOW_MAPPING_BIT 2
#define DO_GLASS_SHADOWS_BIT 4

in vec3 texturePosition;
in vec3 voxelPosition;
flat in vec3 normal;
flat in int textureData;
layout(early_fragment_tests) in;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out int side;

uniform sampler2DArray textures;
uniform sampler2D shadowMap;
uniform sampler2D shadowColor;
uniform mat4 sunMatrix;

uniform int[MAX_AMOUNT_OF_MATERIALS] textureSizes;
uniform int maxTextureSize;

uniform int flags;
uniform float nightBrightness;
uniform float time;
uniform vec3 sunDirection;
uniform vec3 cameraPosition;

int isFlag(int bit) {
    return int((flags & bit) != 0);
}

float easeInOutQuart(float x) {
    //x < 0.5 ? 8 * x * x * x * x : 1 - pow(-2 * x + 2, 4) / 2;
    float inValue = 8.0 * x * x * x * x;
    float outValue = 1.0 - pow(-2.0 * x + 2.0, 4.0) / 2.0;
    return step(inValue, 0.5) * inValue + step(0.5, outValue) * outValue;
}

vec3 getLightColor(vec2 shadowCoord) {
    if (isFlag(DO_GLASS_SHADOWS_BIT) == 0) return vec3(1.0);
    return max(texture(shadowColor, shadowCoord).rgb, vec3(0.5));
}

vec3 getSkyLight(vec3 position, vec3 normal) {
    if (isFlag(DO_SHADOW_MAPPING_BIT) == 0) return vec3(1.0);
    //    if (dot(normal, sunDirection) < 0.0) return 0.0;
    vec4 shadowCoord = sunMatrix * vec4(floor(position + normal * 2.5), 1);
    shadowCoord.xyz /= shadowCoord.w;
    shadowCoord = shadowCoord * 0.5 + 0.5;

    float closestDepth = texture(shadowMap, shadowCoord.xy).r;
    if (closestDepth == 1.0) return getLightColor(shadowCoord.xy);
    float currentDepth = shadowCoord.z;

    return currentDepth - 0.005 > closestDepth ? vec3(0.5) : getLightColor(shadowCoord.xy);
}

float getBlockLight() {
    return 0.0;
}

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
    side = textureData >> 8 & 7;
    int material = textureData & 0xFF;
    int textureSize = textureSizes[material];

    vec4 color = texture(textures, vec3(getUVOffset(side, textureSize), textureData & 0xFF));

    float distance = length(cameraPosition - texturePosition);
    float angle = abs(dot((texturePosition - cameraPosition) / distance, normal));


    float absTime = abs(time);
    float blockLight = getBlockLight();
    float timeLight = max(nightBrightness, easeInOutQuart(absTime));
    float nightLight = -0.6 * (1 - absTime) * (1 - absTime);
    float waterFogMultiplier = min(1, isFlag(HEAD_UNDER_WATER_BIT) * max(0.5, distance * 0.000625));
    float fogMultiplier = 1 - exp(-distance * 0.000005);

    vec3 nightBrightnessVec = vec3(nightBrightness);
    vec3 skyLight = getSkyLight(voxelPosition, normal);
    vec3 sunIllumination = dot(normal, sunDirection) * 0.2 * absTime * skyLight;
    vec3 light = max(nightBrightnessVec, skyLight) * timeLight + sunIllumination;

    light = max(nightBrightnessVec, light);
    light.b = max(nightBrightness, max(nightBrightness, skyLight.b + nightLight) * timeLight + sunIllumination.b);

    vec3 fogColor = vec3(0.46, 0.63, 0.79) * fogMultiplier * timeLight * (1 - waterFogMultiplier);
    vec3 waterColor = (color.rgb + angle * vec3(0.0, 0.4, 0.15)) * light * (1 - fogMultiplier);
    vec3 waterFog = vec3(0.0, 0.098, 0.643) * waterFogMultiplier * timeLight;
    fragColor = vec4((waterColor + waterFog + fogColor), color.a - angle * 0.3);
}