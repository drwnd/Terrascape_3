#version 400 core

uniform vec3 position;
uniform mat4 projectionViewMatrix;
uniform mat4[8] transformations;

layout (location = 0) in vec3 positionOffset;
layout (location = 1) in vec2 textureCoordinate;
layout (location = 2) in int intData;

out vec2 fragTextureCoordinate;
out vec3 offset;
out vec3 normal;
out mat4 transfromationMatrix;

const vec3[6] NORMALS = vec3[6](vec3(0, 0, 1), vec3(0, 1, 0), vec3(1, 0, 0), vec3(0, 0, -1), vec3(0, -1, 0), vec3(-1, 0, 0));

void main() {
    int transfromIndex = intData & 7;
    int side = intData >> 29 & 7;

    vec3 transformedPosition = (transformations[transfromIndex] * vec4(positionOffset, 1)).xyz;
    gl_Position = projectionViewMatrix * vec4(transformedPosition + position, 1);
    fragTextureCoordinate = textureCoordinate;
    offset = positionOffset;
    transfromationMatrix = transformations[transfromIndex];
    normal = NORMALS[side];
}