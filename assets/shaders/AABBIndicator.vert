#version 400 core

out vec3 totalPosition;
flat out int side;

uniform mat4 projectionViewMatrix;
uniform ivec3 iCameraPosition;
uniform ivec3 minPosition;
uniform ivec3 maxPosition;

const vec3[6] NORMALS = vec3[6](vec3(0, 0, 1), vec3(0, 1, 0), vec3(1, 0, 0), vec3(0, 0, -1), vec3(0, -1, 0), vec3(-1, 0, 0));
const ivec2[6] FACE_POSITIONS = ivec2[6](ivec2(0, 0), ivec2(0, 1), ivec2(1, 0), ivec2(1, 1), ivec2(1, 0), ivec2(0, 1));
const int NORTH = 0;
const int TOP = 1;
const int WEST = 2;
const int SOUTH = 3;
const int BOTTOM = 4;
const int EAST = 5;

ivec3 getFacePositions(int side, int currentVertexId) {
    ivec3 currentVertexOffset = ivec3(FACE_POSITIONS[currentVertexId].xy, 0);

    switch (side) {
        case NORTH: return (1 - currentVertexOffset.yxz) * maxPosition + currentVertexOffset.yxz * minPosition;
        case TOP: return (1 - currentVertexOffset.xzy) * maxPosition + currentVertexOffset.xzy * minPosition;
        case WEST: return (1 - currentVertexOffset.zyx) * maxPosition + currentVertexOffset.zyx * minPosition;
        case SOUTH: return currentVertexOffset.xyz * maxPosition + (1 - currentVertexOffset.xyz) * minPosition;
        case BOTTOM: return currentVertexOffset.yzx * maxPosition + (1 - currentVertexOffset.yzx) * minPosition;
        case EAST: return currentVertexOffset.zxy * maxPosition + (1 - currentVertexOffset.zxy) * minPosition;
    }

    return ivec3(0, 0, 0);
}

void main() {
    side = gl_VertexID / 6;
    int currentVertexId = gl_VertexID % 6;

    totalPosition = getFacePositions(side, currentVertexId) - iCameraPosition;
    gl_Position = projectionViewMatrix * vec4(totalPosition, 1.0);
}