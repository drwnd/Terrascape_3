#version 400 core

flat in int textureData;
flat in vec3 normal;
in vec3 totalPosition;

out vec4 fragColor;

uniform sampler2DArray textures;
uniform sampler2DArray propertiesTextures;

uniform int flags;
uniform float nightBrightness;
uniform float time;
uniform vec3 sunDirection;
uniform vec3 cameraPosition;

const int HEAD_UNDER_WATER_BIT = 1;
const int DO_SHADOW_MAPPING_BIT = 2;

vec2 getUVOffset(int side) {
    switch (side) {
        case 0: return vec2(fract(totalPosition.x * 0.0625), 1 - fract(totalPosition.y * 0.0625));
        case 1: return fract(totalPosition.xz * 0.0625);
        case 2: return 1 - fract(totalPosition.zy * 0.0625);
        case 3: return 1 - fract(totalPosition.xy * 0.0625);
        case 4: return fract(totalPosition.xz * 0.0625);
        case 5: return vec2(fract(totalPosition.z * 0.0625), 1 - fract(totalPosition.y * 0.0625));
    }

    return fract(totalPosition.zx);
}

float easeInOutQuart(float x) {
    float inValue = 8.0 * x * x * x * x;
    float outValue = 1.0 - pow(-2.0 * x + 2.0, 4.0) * 0.5;
    return step(inValue, 0.5) * inValue + step(0.5, outValue) * outValue;
}

float getSkyLight(vec3 position, vec3 normal) {
    return 1.0;
}

float getBlockLight(vec3 position, vec3 normal) {
    return 0.0;
}

int isFlag(int bit) {
    return int((flags & bit) != 0);
}

vec3 getColor(vec3 color, vec3 textureCoord) {
    float emissivness = texture(propertiesTextures, textureCoord).r;
    float absTime = abs(time);
    float skyLight = getSkyLight(totalPosition, normal);
    float blockLight = getBlockLight(totalPosition, normal);

    float sunIllumination = dot(normal, sunDirection) * 0.2 * skyLight * absTime;
    float timeLight = max(nightBrightness, easeInOutQuart(absTime));
    float nightLight = 0.6 * (1 - absTime) * (1 - absTime);
    float light = max(blockLight + nightBrightness, max(nightBrightness, skyLight) * timeLight + sunIllumination);
    float distance = length(cameraPosition - totalPosition);
    float waterFogMultiplier = min(1, isFlag(HEAD_UNDER_WATER_BIT) * max(0.5, distance * 0.000625));

    vec3 fragLight = max(vec3(emissivness), vec3(light, light, max(nightBrightness, max(nightBrightness, skyLight + nightLight) * timeLight + sunIllumination)));
    vec3 baseColor = color * fragLight * (1 - waterFogMultiplier);
    vec3 waterColor = vec3(0.0, 0.098, 0.643) * waterFogMultiplier * timeLight;

    return baseColor + waterColor;
}

void main() {
    vec3 textureCoord = vec3(getUVOffset(textureData >> 8 & 7), textureData & 0xFF);
    vec4 color = texture(textures, textureCoord);
    if (color.a == 0) discard;

    color.rgb = getColor(color.rgb, textureCoord);
    fragColor = vec4(color);
}
