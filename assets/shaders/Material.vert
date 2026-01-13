#version 460 core

out vec3 texturePosition;
out vec3 voxelPosition;
flat out vec3 normal;
flat out int textureData;

struct Vertex {
    int x, y, z;
    int textureData;
};

layout (std430, binding = 0) restrict readonly buffer vertexBuffer {
    Vertex[] vertices;
};

uniform mat4 projectionViewMatrix;
uniform int lodSize;
uniform ivec3 iCameraPosition;

const vec3[6] NORMALS = vec3[6](vec3(0, 0, 1), vec3(0, 1, 0), vec3(1, 0, 0), vec3(0, 0, -1), vec3(0, -1, 0), vec3(-1, 0, 0));
const vec2[6] FACE_POSITIONS = vec2[6](vec2(0, 0), vec2(0, 1), vec2(1, 0), vec2(1, 1), vec2(1, 0), vec2(0, 1));
const int NORTH = 0;
const int TOP = 1;
const int WEST = 2;
const int SOUTH = 3;
const int BOTTOM = 4;
const int EAST = 5;

vec3 getFacePositions(int side, int currentVertexId, int faceSize1, int faceSize2) {
    vec3 currentVertexOffset = vec3(FACE_POSITIONS[currentVertexId].xy, 0);

    switch (side) {
        case NORTH: return currentVertexOffset.yxz * vec3(faceSize2, faceSize1, 1) + vec3(0, 0, 1);
        case TOP: return currentVertexOffset.xzy * vec3(faceSize1, 1, faceSize2) + vec3(0, 1, 0);
        case WEST: return currentVertexOffset.zyx * vec3(1, faceSize1, faceSize2) + vec3(1, 0, 0);
        case SOUTH: return currentVertexOffset.xyz * vec3(faceSize2, faceSize1, 1);
        case BOTTOM: return currentVertexOffset.yzx * vec3(faceSize1, 1, faceSize2);
        case EAST: return currentVertexOffset.zxy * vec3(1, faceSize1, faceSize2);
    }

    return vec3(0, 0, 0);
}

void main() {
    Vertex currentVertex = vertices[gl_VertexID / 6];
    int currentVertexId = gl_VertexID % 6;

    int x = currentVertex.x;
    int y = currentVertex.y;
    int z = currentVertex.z;
    int side = currentVertex.textureData >> 8 & 7;

    int faceSize1 = (currentVertex.textureData >> 17 & 63) + 1;
    int faceSize2 = (currentVertex.textureData >> 11 & 63) + 1;
    vec3 inChunkPosition = (getFacePositions(side, currentVertexId, faceSize1, faceSize2)) * lodSize;
    texturePosition = ivec3(x, y, z) * lodSize - iCameraPosition + ivec3(0, -lodSize + 1, 0) + inChunkPosition;
    voxelPosition = texturePosition;

    gl_Position = projectionViewMatrix * vec4(texturePosition, 1.0);

    textureData = currentVertex.textureData;
    normal = NORMALS[side];
}