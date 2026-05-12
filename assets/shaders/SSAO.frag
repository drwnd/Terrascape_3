#version 400 core

uniform sampler2D depthTexture;
uniform sampler2D colorTexture;
uniform isampler2D sideTexture;

uniform mat4 projectionMatrix;
uniform mat4 projectionInverse;
uniform mat3 viewMatrix;
uniform mat3 viewInverse;

uniform vec3 fractionPosition;
uniform int samples;

in vec2 fragTextureCoordinate;

out vec4 fragColor;

const vec3[6] NORMALS = vec3[6](vec3(0, 0, 1), vec3(0, 1, 0), vec3(1, 0, 0), vec3(0, 0, -1), vec3(0, -1, 0), vec3(-1, 0, 0));
const float MAX_DISTANCE = 2000.0;
const int SAMPLE_COUNT = 35;
const vec3[SAMPLE_COUNT] SAMPLES = vec3[SAMPLE_COUNT](
vec3(0, 0, -1.5),
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

// Code from https://medium.com/better-programming/depth-only-ssao-for-forward-renderers-1a3dcfa1873a
vec3 calcViewPosition(vec2 coords) {
    float fragmentDepth = texture(depthTexture, coords).r;

    vec4 ndc = vec4(coords * 2.0 - 1.0, fragmentDepth, 1.0);

    vec4 vs_pos = projectionInverse * ndc;
    vs_pos.xyz = vs_pos.xyz / vs_pos.w;

    return vs_pos.xyz;
}

mat3 getSampleMatrix(int side) {
    switch (side) {
        case 0: return mat3(1, 0, 0, 0, 1, 0, 0, 0, 1);
        case 1: return mat3(1, 0, 0, 0, 0, 1, 0, 1, 0);
        case 2: return mat3(0, 0, 1, 0, 1, 0, 1, 0, 0);
        case 3: return mat3(1, 0, 0, 0, 1, 0, 0, 0, -1);
        case 4: return mat3(1, 0, 0, 0, 0, -1, 0, 1, 0);
        case 5: return mat3(0, 0, -1, 0, 1, 0, 1, 0, 0);
    }
    return mat3(0);
}

float computeVisibilityFactor() {
    int side = texture(sideTexture, fragTextureCoordinate).r;
    if (side < 0 || side >= 6) return 1;
    vec3 worldNormal = NORMALS[side].xyz;
    vec3 viewPos = viewInverse * calcViewPosition(fragTextureCoordinate);
    viewPos.xyz = floor(viewPos.xyz + fractionPosition + worldNormal * 1.5);
    viewPos = viewMatrix * viewPos;

    mat3 sampleMatrix = viewMatrix * getSampleMatrix(side);
    float occlusionFactor = 0.0;

    int count = clamp(samples, 0, SAMPLE_COUNT);
    for (int index = 0; index < count; index++) {
        vec3 samplePos = sampleMatrix * SAMPLES[index].xyz;
        samplePos = viewPos + samplePos;
        //        samplePos.xy = floor(samplePos.xy);

        vec4 offset = vec4(samplePos, 1.0);
        offset = projectionMatrix * offset;
        offset.xy /= offset.w;
        offset.xy = offset.xy * 0.5 + 0.5;

        float geometryDepth = calcViewPosition(offset.xy).z;
        float rangeCheck = float(abs(viewPos.z - geometryDepth) < length(SAMPLES[index]) * 2) / length(SAMPLES[index]);

        occlusionFactor += float(geometryDepth >= samplePos.z + 0.0001) * rangeCheck;
    }

    float distanceScale = max(0.05, 1.0 - smoothstep(0.0, MAX_DISTANCE, min(MAX_DISTANCE, abs(viewPos.z))));
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