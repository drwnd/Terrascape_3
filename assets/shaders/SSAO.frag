#version 400 core

uniform sampler2D colorTexture;
uniform isampler2D intPosTexture;

uniform mat4 projectionViewMatrix;
uniform vec3 inChunkPosition;
uniform int samples;

in vec2 fragTextureCoordinate;

out vec4 fragColor;

const vec3[6] NORMALS = vec3[6](vec3(0, 0, 1), vec3(0, 1, 0), vec3(1, 0, 0), vec3(0, 0, -1), vec3(0, -1, 0), vec3(-1, 0, 0));
const float MAX_DISTANCE = 2000.0;
const int SAMPLE_COUNT = 34;
const vec3[SAMPLE_COUNT] SAMPLES = vec3[SAMPLE_COUNT](
vec3(-1, -1, -0.5),
vec3(-1, 0, -0.5),
vec3(-1, 1, -0.5),
vec3(0, -1, -0.5),
vec3(0, 0, -0.5),
vec3(0, 1, -0.5),
vec3(1, -1, -0.5),
vec3(1, 0, -0.5),
vec3(1, 1, -0.5),
vec3(-2, -2, 0.5),
vec3(-2, -1, 0.5),
vec3(-2, 0, 0.5),
vec3(-2, 1, 0.5),
vec3(-2, 2, 0.5),
vec3(-1, -2, 0.5),
vec3(-1, -1, 0.5),
vec3(-1, 0, 0.5),
vec3(-1, 1, 0.5),
vec3(-1, 2, 0.5),
vec3(0, -2, 0.5),
vec3(0, -1, 0.5),
vec3(0, 0, 0.5),
vec3(0, 1, 0.5),
vec3(0, 2, 0.5),
vec3(1, -2, 0.5),
vec3(1, -1, 0.5),
vec3(1, 0, 0.5),
vec3(1, 1, 0.5),
vec3(1, 2, 0.5),
vec3(2, -2, 0.5),
vec3(2, -1, 0.5),
vec3(2, 0, 0.5),
vec3(2, 1, 0.5),
vec3(2, 2, 0.5)
);

mat3 getSampleMatrix(int side) {
    switch (side) {
        case 0: return mat3(1, 0, 0, 0, 1, 0, 0, 0, 1);
        case 1: return mat3(1, 0, 0, 0, 0, 1, 0, 1, 0);
        case 2: return mat3(0, 0, 1, 0, 1, 0, 1, 0, 0);
        case 3: return mat3(1, 0, 0, 0, 1, 0, 0, 0, -1);
        case 4: return mat3(1, 0, 0, 0, 0, 1, 0, -1, 0);
        case 5: return mat3(0, 0, 1, 0, 1, 0, -1, 0, 0);
    }
    return mat3(0);
}

float computeVisibilityFactor() {
    ivec4 intPos = texture(intPosTexture, fragTextureCoordinate);
    int side = intPos.w;
    if (side < 0 || side >= 6) return 1;
    vec3 worldNormal = NORMALS[side].xyz;
    vec3 voxelPos = vec3(intPos.xyz) + 0.5;
    float currentDepth = length(voxelPos - inChunkPosition);

    mat3 sampleMatrix = getSampleMatrix(side);
    float occlusionFactor = 0.0;

    int count = clamp(samples, 0, SAMPLE_COUNT);
    for (int index = 0; index < count; index++) {
        vec3 samplePos = sampleMatrix * (SAMPLES[index] + vec3(0, 0, 1.5)) + voxelPos;

        vec4 offset = vec4(samplePos, 1.0);
        offset = projectionViewMatrix * offset;
        offset.xy /= offset.w;
        offset.xy = offset.xy * 0.5 + 0.5;

        ivec4 geometryIntPos = texture(intPosTexture, offset.xy);
        if (geometryIntPos.w < 0 || geometryIntPos.w >= 6) continue;

        float geometryDepth = length(geometryIntPos.xyz - inChunkPosition + 0.5);
        float expectedDepth = length(samplePos - inChunkPosition);
        float rangeCheck = float(abs(currentDepth - geometryDepth) < 7);
        occlusionFactor += rangeCheck * float(geometryDepth < expectedDepth - 0.01);
    }

    float distanceScale = 1.0 - smoothstep(0.0, MAX_DISTANCE, min(MAX_DISTANCE, currentDepth));
    float averageOcclusionFactor = occlusionFactor * distanceScale / count;
    float visibilityFactor = 1.0 - averageOcclusionFactor;
    visibilityFactor = pow(visibilityFactor, 5.0);

    return visibilityFactor;
}

void main() {
    vec4 color = texture(colorTexture, fragTextureCoordinate);
    if (color.a == 0) discard;

    float visibilityFactor = computeVisibilityFactor();
    visibilityFactor = max(visibilityFactor, 0.5);
    fragColor = vec4(color.rgb * visibilityFactor, color.a);
//        fragColor = vec4((texture(intPosTexture, fragTextureCoordinate).xyz - inChunkPosition + 1) / 32, 1);
    //    fragColor = vec4(vec3(length(texture(intPosTexture, fragTextureCoordinate).xyz - inChunkPosition + 1)) / 100, 1);
//    fragColor = vec4(abs(NORMALS[texture(intPosTexture, fragTextureCoordinate).w]), 1);
}