#version 460 core

in vec2 fragTextureCoordinates;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out ivec4 intPos;

uniform sampler2D dayTexture;
uniform sampler2D nightTexture;
uniform float time;

void main() {
    vec4 color1 = texture(dayTexture, fragTextureCoordinates);
    vec4 color2 = texture(nightTexture, fragTextureCoordinates);

    float absTime = abs(time);
    fragColor = color1 * absTime + color2 * (1 - absTime);
    intPos = ivec4(0, 0, 0, 6); // Prevents AO on the sky
}
