#version 460 core

uniform vec2 position;
uniform vec2 size;

layout(location = 0) in vec2 positionOffset;
layout(location = 1) in vec2 textureCoordinate;

out vec2 fragTextureCoordinate;

void main() {
    gl_Position = vec4(position + positionOffset * size, 0.5, 0.5);
    fragTextureCoordinate = textureCoordinate;
}
