#version 400 core

out vec4 fragColor;
in vec3 totalPosition;

void main() {
    float sum = floor(totalPosition.x + 0.5) + floor(totalPosition.y + 0.5) + floor(totalPosition.z + 0.5);
    fragColor = vec4(sin(sum * 6.283185307 * 0.125), 0, 0, 0.25);
}