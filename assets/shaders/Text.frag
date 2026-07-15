#version 460 core

in vec2 textureCoordinates;

out vec4 fragColor;

uniform sampler2D textAtlas;
uniform vec3 color;
uniform int addTransparentBackground;

const float backGroundBrightness = 0.2;
const float backGroundOpacity = 0.75;

void main() {
    fragColor = texture(textAtlas, textureCoordinates);
    if (fragColor.a != 1.0 && addTransparentBackground == 0) discard;
    if (fragColor.a != 1.0 && addTransparentBackground == 1) fragColor = vec4(backGroundBrightness, backGroundBrightness, backGroundBrightness, backGroundOpacity);
    if (fragColor.a == 1.0) fragColor *= vec4(color, 1.0);
}