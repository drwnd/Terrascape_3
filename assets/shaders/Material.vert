#version 460 core

out vec3 totalPosition;
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

int getWrappedPosition(int actualPosition, int reference) {
    if (actualPosition - reference > 1 << 30) return actualPosition - (1 << 31);
    if (reference - actualPosition > 1 << 30) return actualPosition + (1 << 31);
    return actualPosition;
}

ivec3 getWrappedPosition(ivec3 worldPos) {
    return ivec3(
    getWrappedPosition(worldPos.x, iCameraPosition.x),
    getWrappedPosition(worldPos.y, iCameraPosition.y),
    getWrappedPosition(worldPos.z, iCameraPosition.z)
    ) - iCameraPosition;
}

void main() {
    Vertex currentVertex = vertices[gl_VertexID / 6];
    int currentVertexId = gl_VertexID % 6;

    int x = currentVertex.x;
    int y = currentVertex.y;
    int z = currentVertex.z;
    int side = currentVertex.textureData >> 8 & 7;

    int faceSize1 = (currentVertex.textureData >> 24 & 63) + 1;
    int faceSize2 = (currentVertex.textureData >> 18 & 63) + 1;
    vec3 inChunkPosition = (getFacePositions(side, currentVertexId, faceSize1, faceSize2)) * lodSize;
    totalPosition = getWrappedPosition(ivec3(x, y, z) * lodSize) + ivec3(0, -lodSize + 1, 0) + inChunkPosition;

    gl_Position = projectionViewMatrix * vec4(totalPosition, 1.0);

    textureData = currentVertex.textureData;
    normal = NORMALS[side];
}