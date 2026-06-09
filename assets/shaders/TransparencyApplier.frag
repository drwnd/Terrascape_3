#version 400 core

uniform sampler2D accumulationTexture;
uniform sampler2D revealTexture;

out vec4 fragColor;

in vec2 fragTextureCoordinate;

const float EPSILON = 0.00001f;

bool isApproximatelyEqual(float a, float b) {
    return abs(a - b) <= (abs(a) < abs(b) ? abs(b) : abs(a)) * EPSILON;
}

float max3(vec3 v) {
    return max(max(v.x, v.y), v.z);
}

void main() {

    float revealage = texture(revealTexture, fragTextureCoordinate, 0).r;

    if (isApproximatelyEqual(revealage, 1.0)) discard;

    vec4 accumulation = texture(accumulationTexture, fragTextureCoordinate, 0);

    if (isinf(max3(abs(accumulation.rgb)))) accumulation.rgb = vec3(accumulation.a);

    vec3 average_color = accumulation.rgb / max(accumulation.a, EPSILON);

    fragColor = vec4(average_color, 1.0 - revealage);
}