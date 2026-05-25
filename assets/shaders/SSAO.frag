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
const int SAMPLE_COUNT = 51;
const vec3[SAMPLE_COUNT] SAMPLES = vec3[SAMPLE_COUNT](
vec3(1, 1, 1.1), vec3(-1, -2, 1.1), vec3(1, 2, 1.1), vec3(0, 2, 1.1),
vec3(1, 0, 1.1), vec3(0, 0, 1.1), vec3(2, -1, 1.1), vec3(1, -2, 1.1),
vec3(2, 1, 1.1), vec3(1, -1, 1.1), vec3(0, 1, 1.1), vec3(-1, 0, 1.1),
vec3(-1, 1, 1.1), vec3(2, 0, 1.1), vec3(-2, -1, 1.1), vec3(-2, 0, 1.1),
vec3(0, -2, 1.1), vec3(-2, 1, 1.1), vec3(0, -1, 1.1), vec3(-1, 2, 1.1),
vec3(-1, -1, 1.1), vec3(2, 1, 2.1), vec3(0, 0, 2.1), vec3(-2, -1, 2.1),
vec3(0, 2, 2.1), vec3(0, -2, 2.1), vec3(-1, 1, 2.1), vec3(1, 1, 2.1),
vec3(-1, -1, 2.1), vec3(1, -1, 2.1), vec3(2, -1, 2.1), vec3(-1, -2, 2.1),
vec3(0, -1, 2.1), vec3(2, 0, 2.1), vec3(-1, 2, 2.1), vec3(1, 2, 2.1),
vec3(1, -2, 2.1), vec3(1, 0, 2.1), vec3(0, 1, 2.1), vec3(-1, 0, 2.1),
vec3(-2, 1, 2.1), vec3(-2, 0, 2.1), vec3(1, 0, 3.1), vec3(-1, 0, 3.1),
vec3(-1, -1, 3.1), vec3(-1, 1, 3.1), vec3(0, 0, 3.1), vec3(1, 1, 3.1),
vec3(0, -1, 3.1), vec3(1, -1, 3.1), vec3(0, 1, 3.1));

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
    vec3 voxelPos = vec3(intPos.xyz) + 0.5;
    float currentDepth = length(voxelPos - inChunkPosition);
    float distanceScale = 1.0 - smoothstep(0.0, MAX_DISTANCE, min(MAX_DISTANCE, currentDepth));
    if (distanceScale == 0) return 1;

    mat3 sampleMatrix = getSampleMatrix(side);
    float occlusionFactor = 0.0;

    int count = clamp(samples, 0, SAMPLE_COUNT);
    for (int index = 0; index < count; index++) {
        vec3 samplePos = sampleMatrix * SAMPLES[index] + voxelPos;

        vec4 offset = vec4(samplePos, 1.0);
        offset = projectionViewMatrix * offset;
        offset.xy /= offset.w;
        offset.xy = offset.xy * 0.5 + 0.5;

        ivec4 geometryIntPos = texture(intPosTexture, offset.xy);
        vec3 relativePosition = vec3(geometryIntPos.xyz) - inChunkPosition + 0.5;
        if (geometryIntPos.w < 0 || geometryIntPos.w >= 6) continue;

        float geometryDepth = length(relativePosition);
        float expectedDepth = length(samplePos - inChunkPosition);

        float angleScale = abs(dot(NORMALS[geometryIntPos.w], normalize(relativePosition)));
        float rangeCheck = float(abs(currentDepth - geometryDepth) < 7) / length(SAMPLES[index]);
        occlusionFactor += rangeCheck * float(geometryDepth < expectedDepth + 1.1 - angleScale);
    }

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
}